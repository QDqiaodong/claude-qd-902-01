package com.waste.power.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.waste.power.common.BizException;
import com.waste.power.common.Dict;
import com.waste.power.domain.Boiler;
import com.waste.power.domain.TurbineGenerator;
import com.waste.power.dto.PowerPayload;
import com.waste.power.repository.BoilerRepository;
import com.waste.power.repository.TurbineGeneratorRepository;

/**
 * 余热锅炉与汽轮发电业务：蒸汽工况维护、发电量登记（不得超过 装机容量 × 运行小时 的理论上限）。
 */
@Service
@Transactional
public class PowerService {

    private final BoilerRepository boilerRepository;
    private final TurbineGeneratorRepository turbineRepository;

    public PowerService(BoilerRepository boilerRepository, TurbineGeneratorRepository turbineRepository) {
        this.boilerRepository = boilerRepository;
        this.turbineRepository = turbineRepository;
    }

    @Transactional(readOnly = true)
    public List<PowerPayload.BoilerVO> boilers() {
        return boilerRepository.findAll().stream().map(PowerPayload.BoilerVO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PowerPayload.TurbineVO> turbines() {
        return turbineRepository.findAll().stream().map(this::toTurbineVO).toList();
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
     * 发电量登记：不得超过 装机容量(MW) × 1000 × 运行小时(h) 的理论上限
     */
    public PowerPayload.TurbineVO register(String unitNo, PowerPayload.GenerationRequest request) {
        TurbineGenerator unit = turbineRepository.findByUnitNo(unitNo)
                .orElseThrow(() -> new BizException("汽轮发电机组不存在：" + unitNo));
        BigDecimal generation = request.getGenerationKwh();
        BigDecimal grid = request.getGridKwh();
        BigDecimal hours = request.getRunningHours();

        if (generation == null || generation.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("发电量必须大于 0");
        }
        if (hours != null && hours.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("运行小时不能为负");
        }
        BigDecimal runningHours = hours == null ? nvl(unit.getRunningHours()) : hours;
        if (runningHours.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("运行小时必须大于 0，无法校核发电量上限");
        }
        // 理论上限 = 装机容量 MW × 1000 kW/MW × 运行小时 h
        BigDecimal maxKwh = nvl(unit.getCapacityMw())
                .multiply(BigDecimal.valueOf(1000))
                .multiply(runningHours)
                .setScale(2, RoundingMode.HALF_UP);
        if (generation.compareTo(maxKwh) > 0) {
            throw new BizException("发电量 " + generation.setScale(2, RoundingMode.HALF_UP)
                    + " kWh 超过理论上限 " + maxKwh + " kWh（装机 " + unit.getCapacityMw() + " MW × 运行 "
                    + runningHours + " h），请核对表码");
        }
        if (grid != null) {
            if (grid.compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("上网电量不能为负");
            }
            if (grid.compareTo(generation) > 0) {
                throw new BizException("上网电量 " + grid + " kWh 不得大于发电量 " + generation + " kWh");
            }
        }
        BigDecimal gridKwh = grid == null ? generation.multiply(BigDecimal.valueOf(0.85))
                .setScale(2, RoundingMode.HALF_UP) : grid;
        // 厂用电率 = (发电量 − 上网电量) / 发电量 × 100
        BigDecimal auxiliaryRate = generation.subtract(gridKwh)
                .divide(generation, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        unit.setGenerationKwh(generation);
        unit.setGridKwh(gridKwh);
        unit.setRunningHours(runningHours);
        unit.setAuxiliaryRate(auxiliaryRate);
        unit.setStatDate(LocalDate.now());
        unit.setStatus("ON_GRID");
        return toTurbineVO(turbineRepository.save(unit));
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
        dict.put("maxAuxiliaryRate", Dict.MAX_AUXILIARY_RATE);
        return dict;
    }

    // ---------------- 内部方法 ----------------

    private PowerPayload.TurbineVO toTurbineVO(TurbineGenerator unit) {
        BigDecimal capacity = nvl(unit.getCapacityMw());
        BigDecimal hours = nvl(unit.getRunningHours());
        BigDecimal maxKwh = capacity.multiply(BigDecimal.valueOf(1000)).multiply(hours)
                .setScale(2, RoundingMode.HALF_UP);
        double factor = maxKwh.compareTo(BigDecimal.ZERO) == 0 ? 0D
                : nvl(unit.getGenerationKwh()).doubleValue() / maxKwh.doubleValue();
        return PowerPayload.TurbineVO.from(unit, maxKwh, BigDecimal.valueOf(factor)
                .setScale(4, RoundingMode.HALF_UP).doubleValue());
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
