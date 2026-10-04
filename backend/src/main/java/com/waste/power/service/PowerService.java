package com.waste.power.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.waste.power.common.BizException;
import com.waste.power.common.Dict;
import com.waste.power.domain.Boiler;
import com.waste.power.domain.GenerationDaily;
import com.waste.power.domain.MeterEvent;
import com.waste.power.domain.TurbineGenerator;
import com.waste.power.dto.PowerPayload;
import com.waste.power.repository.BoilerRepository;
import com.waste.power.repository.GenerationDailyRepository;
import com.waste.power.repository.MeterEventRepository;
import com.waste.power.repository.TurbineGeneratorRepository;

/**
 * 余热锅炉与汽轮发电业务：蒸汽工况维护、发电机组「日结」。
 *
 * 日结口径（修正把累计电表读数误当成单日发电量的问题）：
 * 按机组、统计日取「期末表码 − 相邻上日封账表码（基数）」得到当日发电增量，
 * 上网电量同理取关口表增量，厂用电率 = (发电增量 − 上网增量)/发电增量；
 * 每天的基数、期末表码、增量与依据逐日落库（generation_daily）。
 *
 * 表计回拨 / 更换（表码不增反降）：
 * - SETTLED_NEW_BASE：旧表封账并启用新基数，当日增量 = 旧表尾段 + 新表首段，留痕；
 * - HELD_PENDING_CALIBRATION：暂停日结等待校表（机组 METER_HELD），校表确认新基数后恢复。
 *
 * 并发口径：
 * - 同机组行锁 + (unit_no, stat_date) 唯一索引：重复请求不得产生两份日结；
 * - 机组 @Version 乐观锁 + 终态校验：较早页面的过期请求不得改掉较新的日结结果；
 * - requestId 幂等：同凭据重发（网络重试 / 重新打开页面再点）直接返回原日结。
 */
@Service
@Transactional
public class PowerService {

    private static final DateTimeFormatter EVENT_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final BoilerRepository boilerRepository;
    private final TurbineGeneratorRepository turbineRepository;
    private final GenerationDailyRepository dailyRepository;
    private final MeterEventRepository meterEventRepository;

    /** 可注入时钟，便于验收跨日场景 */
    private Clock clock = Clock.systemDefaultZone();

    public PowerService(BoilerRepository boilerRepository,
            TurbineGeneratorRepository turbineRepository,
            GenerationDailyRepository dailyRepository,
            MeterEventRepository meterEventRepository) {
        this.boilerRepository = boilerRepository;
        this.turbineRepository = turbineRepository;
        this.dailyRepository = dailyRepository;
        this.meterEventRepository = meterEventRepository;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PowerPayload.BoilerVO> boilers() {
        return boilerRepository.findAll().stream().map(PowerPayload.BoilerVO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PowerPayload.TurbineVO> turbines() {
        return turbineRepository.findAll().stream().map(this::toTurbineVO).toList();
    }

    @Transactional(readOnly = true)
    public List<PowerPayload.DailyVO> dailyList(String unitNo) {
        List<GenerationDaily> items = unitNo == null || unitNo.isBlank()
                ? dailyRepository.findTop50ByOrderByStatDateDescIdDesc()
                : dailyRepository.findByUnitNoOrderByStatDateDesc(unitNo);
        return items.stream().map(PowerPayload.DailyVO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PowerPayload.MeterEventVO> meterEvents(String unitNo) {
        List<MeterEvent> items = meterEventRepository.findTop50ByOrderByCreateTimeDesc();
        return items.stream()
                .filter(e -> unitNo == null || unitNo.isBlank() || unitNo.equals(e.getUnitNo()))
                .map(PowerPayload.MeterEventVO::from)
                .toList();
    }

    /**
     * 锅炉蒸汽工况调整
     */
    public PowerPayload.BoilerVO adjustBoiler(String boilerNo, PowerPayload.BoilerRequest request) {
        Boiler boiler = boilerRepository.findByBoilerNo(boilerNo)
                .orElseThrow(() -> new BizException("余热锅炉不存在：" + boilerNo));
        if (request.getSteamPressureMpa() != null) {
            boiler.setSteamPressureMpa(request.getSteamPressureMpa());
        }
        if (request.getSteamTempC() != null) {
            boiler.setSteamTempC(request.getSteamTempC());
        }
        if (request.getEvaporationTph() != null) {
            if (request.getEvaporationTph().compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("蒸发量不能为负");
            }
            boiler.setEvaporationTph(request.getEvaporationTph());
        }
        if (request.getStatus() != null) {
            boiler.setStatus(request.getStatus());
        }
        return PowerPayload.BoilerVO.from(boilerRepository.save(boiler));
    }

    /**
     * 发电日结：申报期末累计表码，按相邻表码计算当日发电增量。
     */
    public PowerPayload.DailyVO settle(String unitNo, PowerPayload.GenerationRequest request) {
        LocalDate statDate = request.getStatDate() != null ? request.getStatDate() : today();
        BigDecimal closing = request.getMeterReadingKwh();
        BigDecimal gridClosing = request.getGridMeterReadingKwh();
        BigDecimal hours = resolveHours(request.getRunningHours());

        if (closing == null) {
            throw new BizException("请填报发电电能表期末累计表码（不是发电量）");
        }
        if (closing.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("表码不能为负，请核对电能表读数");
        }
        if (gridClosing != null) {
            if (gridClosing.compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("上网表码不能为负，请核对关口表读数");
            }
        }

        // 行锁串行化同机组日结，保证两人同时操作时基数一致、只有一份日结
        TurbineGenerator unit = turbineRepository.findWithLockingByUnitNo(unitNo)
                .orElseThrow(() -> new BizException("汽轮发电机组不存在：" + unitNo));

        // 同凭据重发（锁内复查）：重复请求幂等返回原日结，绝不产生第二份
        String requestId = trimToNull(request.getRequestId());
        if (requestId != null) {
            GenerationDaily idem = dailyRepository.findByRequestId(requestId).orElse(null);
            if (idem != null) {
                if (!idem.getUnitNo().equals(unitNo) || !idem.getStatDate().equals(statDate)) {
                    throw new BizException("该请求凭据已用于 " + idem.getUnitNo() + " "
                            + idem.getStatDate() + " 的日结，不能改用于其他机组/日期");
                }
                return PowerPayload.DailyVO.from(idem);
            }
        }

        // 已存在日结：重复请求不得产生两份；较早页面的过期请求不得改掉较新结果
        GenerationDaily existed = dailyRepository.findByUnitNoAndStatDate(unitNo, statDate).orElse(null);
        if (existed != null) {
            throw new BizException(unitNo + " " + statDate + " 已存在日结（当日发电增量 "
                    + existed.getGenerationKwh().setScale(2, RoundingMode.HALF_UP)
                    + " kWh），重复请求不产生第二份；如需更正请走表计回拨/更换流程");
        }
        validateStatDate(statDate, unit);
        assertNotHeld(unit);

        BigDecimal base = nvl(unit.getMeterReadingKwh());
        BigDecimal gridBase = unit.getGridMeterReadingKwh();
        BigDecimal genIncrement = closing.subtract(base);
        if (genIncrement.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("期末表码 " + closing + " 小于日结基数 " + base
                    + "（表码回拨或已换表），不得按负值日结。请在「表计异常处置」中选择："
                    + "旧表封账并启用新基数，或暂停日结等待校表");
        }
        if (genIncrement.compareTo(BigDecimal.ZERO) == 0) {
            throw new BizException("当日发电增量为 0（期末表码与基数相同），请核对表码后再日结");
        }
        if (gridClosing != null && gridBase != null
                && gridClosing.subtract(gridBase).compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("上网表期末码 " + gridClosing + " 小于基数 " + gridBase
                    + "，关口表疑似回拨/更换，请走表计异常处置");
        }
        assertIncrementWithinCapacity(unit, genIncrement, hours);

        BigDecimal gridIncrement = resolveGridIncrement(genIncrement, gridBase, gridClosing);

        GenerationDaily daily = buildDaily(unitNo, statDate, base, closing, genIncrement,
                gridBase, gridClosing, gridIncrement, hours,
                unit.getMeterSource(), null, null, "NORMAL",
                request.getOperator(), requestId,
                "相邻表码增量：发电 " + closing + " − 基数 " + base
                        + (gridClosing != null && gridBase != null
                                ? "；上网 " + gridClosing + " − " + gridBase
                                : "；上网电量按 85% 估算"));
        daily = saveDaily(daily);

        unit.setMeterReadingKwh(closing);
        if (gridClosing != null) {
            unit.setGridMeterReadingKwh(gridClosing);
        } else {
            unit.setGridMeterReadingKwh(baseOf(unit.getGridMeterReadingKwh(), gridIncrement));
        }
        unit.setMeterSource("PREV_DAY");
        applyDailyToUnit(unit, daily);
        saveUnit(unit);
        return PowerPayload.DailyVO.from(daily);
    }

    /**
     * 表计回拨 / 更换处置：
     * SETTLED_NEW_BASE 旧表封账并启用新基数（当日日结 = 旧表尾段 + 新表首段）；
     * HELD_PENDING_CALIBRATION 暂停日结等待校表（不产生日结，恢复前机组日结一律拒绝）。
     */
    public PowerPayload.MeterEventVO handleMeterEvent(String unitNo, PowerPayload.MeterEventRequest request) {
        LocalDate statDate = request.getStatDate() != null ? request.getStatDate() : today();
        String eventType = request.getEventType();
        String resolution = request.getResolution();
        if (!"ROLLBACK".equals(eventType) && !"REPLACE".equals(eventType)) {
            throw new BizException("表计异常类型必须为 ROLLBACK（回拨）或 REPLACE（更换）");
        }
        if (!"SETTLED_NEW_BASE".equals(resolution)
                && !"HELD_PENDING_CALIBRATION".equals(resolution)) {
            throw new BizException("请明确选择：SETTLED_NEW_BASE 旧表封账并启用新基数，"
                    + "或 HELD_PENDING_CALIBRATION 暂停日结等待校表");
        }
        BigDecimal observed = request.getObservedReadingKwh();
        if (observed == null || observed.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("请填报本次表码（回拨后读数或新表读数），不能为负");
        }

        String requestId = trimToNull(request.getRequestId());

        TurbineGenerator unit = turbineRepository.findWithLockingByUnitNo(unitNo)
                .orElseThrow(() -> new BizException("汽轮发电机组不存在：" + unitNo));

        // 已有待校表事件：不重复留痕，幂等返回
        MeterEvent openEvent = meterEventRepository
                .findByUnitNoAndEventStatusOrderByCreateTimeDesc(unitNo, "OPEN").stream()
                .findFirst().orElse(null);

        if ("HELD_PENDING_CALIBRATION".equals(resolution)) {
            validateStatDate(statDate, unit);
            if (openEvent != null) {
                return PowerPayload.MeterEventVO.from(openEvent);
            }
            assertNoDaily(unitNo, statDate);
            MeterEvent event = newMeterEvent(unitNo, statDate, eventType, unit, observed,
                    resolution, request.getOperator(), request.getNote());
            event.setEventStatus("OPEN");
            // 暂停日结：机组表码维持原基数不动，仅置挂起标志
            unit.setMeterStatus("METER_HELD");
            meterEventRepository.save(event);
            saveUnit(unit);
            return PowerPayload.MeterEventVO.from(event);
        }

        // SETTLED_NEW_BASE：旧表封账并启用新基数
        validateStatDate(statDate, unit);
        if ("METER_HELD".equals(unit.getMeterStatus()) && openEvent == null) {
            throw new BizException("机组处于暂停日结状态但找不到挂起事件，请联系管理员");
        }
        assertNoDaily(unitNo, statDate);

        BigDecimal oldFinal = request.getOldMeterFinalReadingKwh() != null
                ? request.getOldMeterFinalReadingKwh()
                : nvl(unit.getMeterReadingKwh());
        BigDecimal newStart = request.getNewMeterStartReadingKwh() != null
                ? request.getNewMeterStartReadingKwh()
                : (eventType.equals("ROLLBACK") ? observed : BigDecimal.ZERO);
        if (oldFinal.compareTo(nvl(unit.getMeterReadingKwh())) < 0) {
            throw new BizException("旧表封账止码 " + oldFinal + " 小于日结基数 "
                    + unit.getMeterReadingKwh() + "，旧表尾段不能为负");
        }
        if (observed.compareTo(newStart) < 0) {
            throw new BizException("本次表码 " + observed + " 小于新表起码 " + newStart
                    + "，新表首段不能为负");
        }

        BigDecimal hours = resolveHours(request.getRunningHours());
        BigDecimal tail = oldFinal.subtract(nvl(unit.getMeterReadingKwh()));
        BigDecimal head = observed.subtract(newStart);
        BigDecimal genIncrement = tail.add(head);
        if (genIncrement.compareTo(BigDecimal.ZERO) == 0) {
            throw new BizException("旧表尾段 + 新表首段为 0，请核对旧表止码与新表读数");
        }
        assertIncrementWithinCapacity(unit, genIncrement, hours);

        BigDecimal gridClosing = request.getGridMeterReadingKwh();
        BigDecimal gridBase = unit.getGridMeterReadingKwh();
        // 发电表与上网关口表同时更换时，上网增量无法跨表做差，回退按 85% 估算并在依据中说明
        boolean gridMeterAlsoReset = gridClosing != null && gridBase != null
                && gridClosing.compareTo(gridBase) < 0;
        if (gridMeterAlsoReset) {
            gridClosing = null;
        }
        BigDecimal gridIncrement = resolveGridIncrement(genIncrement, gridBase, gridClosing);

        String abnormalStatus = "ROLLBACK".equals(eventType) ? "ROLLBACK_RESET" : "REPLACE_RESET";
        String basis = "旧表尾段 " + tail.setScale(2, RoundingMode.HALF_UP)
                + "（止码 " + oldFinal + " − 基数 " + nvl(unit.getMeterReadingKwh())
                + "）+ 新表首段 " + head.setScale(2, RoundingMode.HALF_UP)
                + "（读数 " + observed + " − 起码 " + newStart + "）= "
                + genIncrement.setScale(2, RoundingMode.HALF_UP);

        MeterEvent event = openEvent;
        if (event == null) {
            event = newMeterEvent(unitNo, statDate, eventType, unit, observed,
                    resolution, request.getOperator(), request.getNote());
        } else {
            event.setEventType(eventType);
            event.setResolution("SETTLED_NEW_BASE");
            event.setOperator(trimToNull(request.getOperator()));
            event.setNote(trimToNull(request.getNote()));
        }
        event.setOldMeterFinalReadingKwh(oldFinal);
        event.setNewMeterStartReadingKwh(newStart);
        event.setEventStatus("RESOLVED");
        event.setResolveTime(now());
        event.setReleaseReadingKwh(observed);
        event.setReleaseOperator(trimToNull(request.getOperator()));
        event.setReleaseNote(trimToNull(request.getNote()));
        event = meterEventRepository.save(event);

        GenerationDaily daily = buildDaily(unitNo, statDate, nvl(unit.getMeterReadingKwh()), observed,
                genIncrement, gridBase, gridClosing, gridIncrement, hours,
                "RESET_NEW_BASE", eventType, "SETTLED_NEW_BASE", abnormalStatus,
                request.getOperator(), requestId, basis);
        daily.setMeterEventId(event.getId());
        daily = saveDaily(daily);
        event.setDailyId(daily.getId());
        meterEventRepository.save(event);

        // 封账后新基数：新表读数；后续日结按「读数 − observed」取增量
        unit.setMeterReadingKwh(observed);
        if (gridClosing != null) {
            unit.setGridMeterReadingKwh(gridClosing);
        } else {
            unit.setGridMeterReadingKwh(baseOf(unit.getGridMeterReadingKwh(), gridIncrement));
        }
        unit.setMeterSource("RESET_NEW_BASE");
        unit.setMeterStatus("NORMAL");
        applyDailyToUnit(unit, daily);
        saveUnit(unit);
        return PowerPayload.MeterEventVO.from(event);
    }

    /**
     * 校表恢复：暂停日结期间完成校表，确认新基数后恢复正常日结（不补结算账）。
     */
    public PowerPayload.MeterEventVO releaseMeterEvent(Long eventId, PowerPayload.MeterReleaseRequest request) {
        MeterEvent event = meterEventRepository.findById(eventId)
                .orElseThrow(() -> new BizException("表计事件不存在：" + eventId));
        if (!"OPEN".equals(event.getEventStatus())) {
            throw new BizException("事件 " + event.getEventNo() + " 已校表恢复，无需重复操作");
        }
        BigDecimal release = request.getReleaseReadingKwh();
        if (release == null || release.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("请填报校表后确认的新基数，不能为负");
        }
        TurbineGenerator unit = turbineRepository.findWithLockingByUnitNo(event.getUnitNo())
                .orElseThrow(() -> new BizException("汽轮发电机组不存在：" + event.getUnitNo()));
        if (!"METER_HELD".equals(unit.getMeterStatus())) {
            throw new BizException("机组当前并非暂停日结状态，不能校表恢复");
        }

        unit.setMeterReadingKwh(release);
        if (request.getGridReleaseReadingKwh() != null) {
            unit.setGridMeterReadingKwh(request.getGridReleaseReadingKwh());
        }
        unit.setMeterSource("RESET_NEW_BASE");
        unit.setMeterStatus("NORMAL");
        saveUnit(unit);

        event.setEventStatus("RESOLVED");
        event.setResolution("HELD_PENDING_CALIBRATION");
        event.setReleaseReadingKwh(release);
        event.setReleaseOperator(trimToNull(request.getOperator()));
        event.setReleaseNote(trimToNull(request.getNote()));
        event.setResolveTime(now());
        return PowerPayload.MeterEventVO.from(meterEventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dict() {
        Map<String, Object> dict = new LinkedHashMap<>();
        dict.put("boilerStatuses", Dict.BOILER_STATUS.stream()
                .map(code -> (Object) Map.of("code", code, "name", Dict.boilerStatusName(code)))
                .toList());
        dict.put("unitStatuses", Dict.UNIT_STATUS.stream()
                .map(code -> (Object) Map.of("code", code, "name", Dict.unitStatusName(code)))
                .toList());
        dict.put("meterSources", Dict.METER_SOURCES.stream()
                .map(code -> (Object) Map.of("code", code, "name", Dict.meterSourceName(code)))
                .toList());
        dict.put("meterStatuses", Dict.METER_STATUSES.stream()
                .map(code -> (Object) Map.of("code", code, "name", Dict.meterStatusName(code)))
                .toList());
        dict.put("meterEventTypes", Dict.METER_EVENT_TYPES.stream()
                .map(code -> (Object) Map.of("code", code, "name", Dict.meterEventTypeName(code)))
                .toList());
        dict.put("meterResolutions", Dict.METER_RESOLUTIONS.stream()
                .map(code -> (Object) Map.of("code", code, "name", Dict.meterResolutionName(code)))
                .toList());
        dict.put("maxAuxiliaryRate", Dict.MAX_AUXILIARY_RATE);
        return dict;
    }

    // ---------------- 内部方法 ----------------

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private void validateStatDate(LocalDate statDate, TurbineGenerator unit) {
        LocalDate today = today();
        if (statDate.isAfter(today)) {
            throw new BizException("统计日 " + statDate + " 晚于今天，不能提前日结");
        }
        LocalDate last = unit.getStatDate();
        if (last != null) {
            LocalDate expected = last.plusDays(1);
            if (statDate.isBefore(expected)) {
                throw new BizException(unit.getUnitNo() + " 已日结到 " + last
                        + "，不能重复/回改 " + statDate + "（较早页面的过期请求不得改掉较新的日结）");
            }
            if (statDate.isAfter(expected) && !"METER_HELD".equals(unit.getMeterStatus())) {
                throw new BizException("上一个已日结日为 " + last + "，请先补日结 " + expected
                        + "，不得跨日跳日（跨日缺口需逐日日结）");
            }
        }
    }

    private void assertNotHeld(TurbineGenerator unit) {
        if ("METER_HELD".equals(unit.getMeterStatus())) {
            throw new BizException(unit.getUnitNo()
                    + " 已暂停日结等待校表，校表确认新基数前不得日结");
        }
    }

    private void assertNoDaily(String unitNo, LocalDate statDate) {
        if (dailyRepository.existsByUnitNoAndStatDate(unitNo, statDate)) {
            throw new BizException(unitNo + " " + statDate + " 已存在日结，不能再登记表计异常");
        }
    }

    private BigDecimal resolveHours(BigDecimal hours) {
        if (hours == null) {
            return BigDecimal.valueOf(24);
        }
        if (hours.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("运行小时不能为负");
        }
        if (hours.compareTo(BigDecimal.ZERO) == 0) {
            throw new BizException("运行小时必须大于 0，无法校核发电增量上限");
        }
        return hours;
    }

    private void assertIncrementWithinCapacity(TurbineGenerator unit, BigDecimal increment, BigDecimal hours) {
        // 理论上限 = 装机容量 MW × 1000 kW/MW × 当日运行小时 h（校核的是当日增量）
        BigDecimal maxKwh = nvl(unit.getCapacityMw())
                .multiply(BigDecimal.valueOf(1000))
                .multiply(hours)
                .setScale(2, RoundingMode.HALF_UP);
        if (increment.compareTo(maxKwh) > 0) {
            throw new BizException("当日发电增量 " + increment.setScale(2, RoundingMode.HALF_UP)
                    + " kWh 超过理论上限 " + maxKwh + " kWh（装机 " + unit.getCapacityMw()
                    + " MW × 运行 " + hours + " h），请核对表码");
        }
    }

    /**
     * 上网电量增量：有上网表码按相邻表码做差；缺省（或上网表同时更换无法跨表做差）按 85% 估算。
     */
    private BigDecimal resolveGridIncrement(BigDecimal genIncrement,
            BigDecimal gridBase, BigDecimal gridClosing) {
        BigDecimal gridIncrement;
        if (gridClosing != null && gridBase != null) {
            gridIncrement = gridClosing.subtract(gridBase);
            if (gridIncrement.compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("上网电量增量为负，请核对关口表读数或走表计异常处置");
            }
        } else {
            gridIncrement = genIncrement.multiply(Dict.DEFAULT_GRID_FACTOR)
                    .setScale(2, RoundingMode.HALF_UP);
        }
        if (gridIncrement.compareTo(genIncrement) > 0) {
            throw new BizException("上网电量增量 " + gridIncrement + " kWh 不得大于发电增量 "
                    + genIncrement + " kWh");
        }
        return gridIncrement;
    }

    private GenerationDaily buildDaily(String unitNo, LocalDate statDate,
            BigDecimal base, BigDecimal closing, BigDecimal genIncrement,
            BigDecimal gridBase, BigDecimal gridClosing, BigDecimal gridIncrement,
            BigDecimal hours, String baseType, String abnormalType, String resolution,
            String abnormalStatus, String operator, String requestId, String basis) {
        BigDecimal auxiliaryRate = genIncrement.subtract(gridIncrement)
                .divide(genIncrement, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        GenerationDaily daily = new GenerationDaily();
        daily.setUnitNo(unitNo);
        daily.setStatDate(statDate);
        daily.setBaseReadingKwh(base.setScale(2, RoundingMode.HALF_UP));
        daily.setClosingReadingKwh(closing.setScale(2, RoundingMode.HALF_UP));
        daily.setGenerationKwh(genIncrement.setScale(2, RoundingMode.HALF_UP));
        if (gridBase != null) {
            daily.setGridBaseReadingKwh(gridBase.setScale(2, RoundingMode.HALF_UP));
        }
        if (gridClosing != null) {
            daily.setGridClosingReadingKwh(gridClosing.setScale(2, RoundingMode.HALF_UP));
        }
        daily.setGridKwh(gridIncrement.setScale(2, RoundingMode.HALF_UP));
        daily.setAuxiliaryRate(auxiliaryRate);
        daily.setRunningHours(hours);
        daily.setBaseType(baseType);
        daily.setAbnormalType(abnormalType);
        daily.setResolution(resolution);
        daily.setAbnormalStatus(abnormalStatus);
        daily.setOperator(trimToNull(operator));
        daily.setRequestId(trimToNull(requestId));
        daily.setRemark(basis);
        daily.setCreateTime(now());
        return daily;
    }

    private GenerationDaily saveDaily(GenerationDaily daily) {
        try {
            return dailyRepository.saveAndFlush(daily);
        } catch (DataIntegrityViolationException e) {
            // 唯一索引兜底：并发穿透时重复日结仍被库级拒绝
            throw new BizException(daily.getUnitNo() + " " + daily.getStatDate()
                    + " 的日结已存在（并发重复请求已拦截），未产生第二份日结");
        }
    }

    private void saveUnit(TurbineGenerator unit) {
        try {
            turbineRepository.saveAndFlush(unit);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BizException("机组日结状态已被更新的页面操作刷新，本请求基于过期数据，"
                    + "未改动较新的日结结果，请重新打开页面后再操作");
        }
    }

    private MeterEvent newMeterEvent(String unitNo, LocalDate statDate, String eventType,
            TurbineGenerator unit, BigDecimal observed, String resolution,
            String operator, String note) {
        MeterEvent event = new MeterEvent();
        event.setEventNo("ME" + now().format(EVENT_NO_FMT) + "-" + unitNo.replaceAll("[^A-Za-z0-9]", ""));
        event.setUnitNo(unitNo);
        event.setStatDate(statDate);
        event.setEventType(eventType);
        event.setPreviousBaseReadingKwh(nvl(unit.getMeterReadingKwh()));
        event.setObservedReadingKwh(observed);
        event.setResolution(resolution);
        event.setEventStatus("OPEN");
        event.setOperator(trimToNull(operator));
        event.setNote(trimToNull(note));
        event.setCreateTime(now());
        return event;
    }

    private void applyDailyToUnit(TurbineGenerator unit, GenerationDaily daily) {
        unit.setGenerationKwh(daily.getGenerationKwh());
        unit.setGridKwh(daily.getGridKwh());
        unit.setAuxiliaryRate(daily.getAuxiliaryRate());
        unit.setRunningHours(daily.getRunningHours());
        unit.setStatDate(daily.getStatDate());
        if (!"OVERHAUL".equals(unit.getStatus())) {
            unit.setStatus("ON_GRID");
        }
    }

    private PowerPayload.TurbineVO toTurbineVO(TurbineGenerator unit) {
        GenerationDaily lastDaily = unit.getStatDate() == null ? null
                : dailyRepository.findByUnitNoAndStatDate(unit.getUnitNo(), unit.getStatDate())
                        .orElse(null);
        BigDecimal hours = lastDaily != null ? nvl(lastDaily.getRunningHours())
                : nvl(unit.getRunningHours());
        BigDecimal maxKwh = nvl(unit.getCapacityMw()).multiply(BigDecimal.valueOf(1000))
                .multiply(hours.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.valueOf(24) : hours)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal generation = lastDaily != null ? nvl(lastDaily.getGenerationKwh())
                : nvl(unit.getGenerationKwh());
        double factor = maxKwh.compareTo(BigDecimal.ZERO) == 0 ? 0D
                : generation.doubleValue() / maxKwh.doubleValue();
        return PowerPayload.TurbineVO.from(unit, lastDaily, maxKwh,
                BigDecimal.valueOf(factor).setScale(4, RoundingMode.HALF_UP).doubleValue());
    }

    private BigDecimal baseOf(BigDecimal currentBase, BigDecimal increment) {
        // 未提供上网表码时，把估算增量累加到基数，保证下次仍有相邻基数
        return nvl(currentBase).add(increment).setScale(2, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
