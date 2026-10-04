package com.waste.power.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.waste.power.common.BizException;
import com.waste.power.domain.GenerationDaily;
import com.waste.power.domain.MeterEvent;
import com.waste.power.domain.TurbineGenerator;
import com.waste.power.dto.PowerPayload;
import com.waste.power.repository.GenerationDailyRepository;
import com.waste.power.repository.MeterEventRepository;
import com.waste.power.repository.TurbineGeneratorRepository;

/**
 * 发电日结集成测试（H2，MODE=MySQL，行锁 + 唯一索引行为与 MySQL 一致）：
 * 跨日增量、表计回拨/更换两种选择、并发同日日结、重复请求与重新打开页面幂等。
 */
@SpringBootTest
class PowerServiceSettleTest {

    private static final LocalDate DAY0 = LocalDate.of(2026, 10, 1);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private PowerService powerService;
    @Autowired
    private TurbineGeneratorRepository turbineRepository;
    @Autowired
    private GenerationDailyRepository dailyRepository;
    @Autowired
    private MeterEventRepository meterEventRepository;

    private Clock fixed;

    @BeforeEach
    void clean() {
        dailyRepository.deleteAll();
        meterEventRepository.deleteAll();
        turbineRepository.deleteAll();
        fixed = Clock.fixed(DAY0.atStartOfDay(ZONE).toInstant(), ZONE);
        powerService.setClock(fixed);
    }

    private TurbineGenerator newUnit(String no, BigDecimal meter, BigDecimal gridMeter) {
        TurbineGenerator u = new TurbineGenerator();
        u.setUnitNo(no);
        u.setUnitName(no + " 机组");
        u.setCapacityMw(new BigDecimal("25.00"));
        u.setMeterReadingKwh(meter);
        u.setGridMeterReadingKwh(gridMeter);
        u.setMeterSource("PREV_DAY");
        u.setMeterStatus("NORMAL");
        u.setStatus("ON_GRID");
        return turbineRepository.save(u);
    }

    private PowerPayload.GenerationRequest settleReq(String date, String meter, String gridMeter,
            String hours, String requestId) {
        PowerPayload.GenerationRequest req = new PowerPayload.GenerationRequest();
        req.setStatDate(date == null ? null : LocalDate.parse(date));
        req.setMeterReadingKwh(new BigDecimal(meter));
        req.setGridMeterReadingKwh(gridMeter == null ? null : new BigDecimal(gridMeter));
        req.setRunningHours(hours == null ? null : new BigDecimal(hours));
        req.setOperator("测试日结员");
        req.setRequestId(requestId);
        return req;
    }

    private void advanceClock(int days) {
        fixed = Clock.fixed(DAY0.plusDays(days).atStartOfDay(ZONE).toInstant(), ZONE);
        powerService.setClock(fixed);
    }

    @Test
    @DisplayName("日结按相邻表码增量计算，绝不把累计读数当成单日发电量")
    void settleUsesIncrementNotCumulative() {
        newUnit("TG-T1", new BigDecimal("1000.00"), new BigDecimal("800.00"));

        PowerPayload.DailyVO d1 = powerService.settle("TG-T1",
                settleReq("2026-10-01", "1500.00", "1220.00", "24.0", "r1"));
        assertThat(d1.getBaseReadingKwh()).isEqualByComparingTo("1000.00");
        assertThat(d1.getClosingReadingKwh()).isEqualByComparingTo("1500.00");
        assertThat(d1.getGenerationKwh()).isEqualByComparingTo("500.00");
        assertThat(d1.getGridKwh()).isEqualByComparingTo("420.00");
        // 厂用电率 = (500 − 420) / 500 × 100 = 16.00%
        assertThat(d1.getAuxiliaryRate()).isEqualByComparingTo("16.00");
        assertThat(d1.getBaseTypeName()).contains("上日封账表码");

        TurbineGenerator u = turbineRepository.findByUnitNo("TG-T1").orElseThrow();
        // 机组表保存的是当前累计表码，不是发电量
        assertThat(u.getMeterReadingKwh()).isEqualByComparingTo("1500.00");
        assertThat(u.getGenerationKwh()).isEqualByComparingTo("500.00");
        assertThat(u.getStatDate()).isEqualTo(LocalDate.parse("2026-10-01"));
    }

    @Test
    @DisplayName("次日按新基数继续做差；缺省上网电量按 85% 估算")
    void nextDayUsesAdjacentBaseAndDefaultGridFactor() {
        newUnit("TG-T2", new BigDecimal("2000.00"), null);
        powerService.settle("TG-T2", settleReq("2026-10-01", "2600.00", null, "24.0", "r2a"));
        advanceClock(1);
        PowerPayload.DailyVO d2 = powerService.settle("TG-T2",
                settleReq("2026-10-02", "3200.00", null, "24.0", "r2b"));
        assertThat(d2.getBaseReadingKwh()).isEqualByComparingTo("2600.00");
        assertThat(d2.getGenerationKwh()).isEqualByComparingTo("600.00");
        assertThat(d2.getGridKwh()).isEqualByComparingTo("510.00"); // 600 × 0.85
        assertThat(d2.getAuxiliaryRate()).isEqualByComparingTo("15.00");
    }

    @Test
    @DisplayName("重复请求不得产生两份日结：无凭据重复提交被拒")
    void duplicateSettleRejected() {
        newUnit("TG-T3", new BigDecimal("1000.00"), new BigDecimal("800.00"));
        powerService.settle("TG-T3", settleReq("2026-10-01", "1500.00", "1200.00", "24.0", null));
        assertThatThrownBy(() -> powerService.settle("TG-T3",
                settleReq("2026-10-01", "1500.00", "1200.00", "24.0", null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已存在日结")
                .hasMessageContaining("不产生第二份");
        assertThat(dailyRepository.findByUnitNoOrderByStatDateDesc("TG-T3")).hasSize(1);
    }

    @Test
    @DisplayName("较早页面的过期请求不得改掉较新的日结结果")
    void stalePageCannotOverwriteNewer() {
        newUnit("TG-T4", new BigDecimal("1000.00"), new BigDecimal("800.00"));
        powerService.settle("TG-T4", settleReq("2026-10-01", "1500.00", "1200.00", "24.0", "r4a"));
        advanceClock(1);
        // 新页面先完成 10-02 日结
        powerService.settle("TG-T4", settleReq("2026-10-02", "2100.00", "1710.00", "24.0", "r4b"));
        // 较早页面（仍停在 10-01）重提 10-01：拒绝
        assertThatThrownBy(() -> powerService.settle("TG-T4",
                settleReq("2026-10-01", "9999.00", "8888.00", "24.0", "r4-old")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已存在日结");
        // 较早页面提交更早的 09-30：也拒绝，不得回改
        assertThatThrownBy(() -> powerService.settle("TG-T4",
                settleReq("2026-09-30", "1400.00", "1100.00", "24.0", "r4-older")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不能重复/回改");
        // 较新结果保持不变
        GenerationDaily d2 = dailyRepository
                .findByUnitNoAndStatDate("TG-T4", LocalDate.parse("2026-10-02")).orElseThrow();
        assertThat(d2.getGenerationKwh()).isEqualByComparingTo("600.00");
        TurbineGenerator u = turbineRepository.findByUnitNo("TG-T4").orElseThrow();
        assertThat(u.getMeterReadingKwh()).isEqualByComparingTo("2100.00");
        assertThat(u.getStatDate()).isEqualTo(LocalDate.parse("2026-10-02"));
    }

    @Test
    @DisplayName("跨日不得跳日：缺日必须逐日补齐；不能提前日结未来日期")
    void crossDayNoGapNoFuture() {
        newUnit("TG-T5", new BigDecimal("1000.00"), new BigDecimal("800.00"));
        powerService.settle("TG-T5", settleReq("2026-10-01", "1500.00", "1200.00", "24.0", "r5a"));
        advanceClock(2);
        // 直接日结 10-03：跨日缺口被拒
        assertThatThrownBy(() -> powerService.settle("TG-T5",
                settleReq("2026-10-03", "2700.00", "2200.00", "24.0", "r5gap")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("先补日结")
                .hasMessageContaining("2026-10-02");
        // 未来日期被拒
        assertThatThrownBy(() -> powerService.settle("TG-T5",
                settleReq("2026-10-04", "3000.00", "2400.00", "24.0", "r5future")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不能提前日结");
        // 逐日补齐
        powerService.settle("TG-T5", settleReq("2026-10-02", "2100.00", "1700.00", "24.0", "r5b"));
        PowerPayload.DailyVO d3 = powerService.settle("TG-T5",
                settleReq("2026-10-03", "2700.00", "2200.00", "24.0", "r5c"));
        assertThat(d3.getGenerationKwh()).isEqualByComparingTo("600.00");
    }

    @Test
    @DisplayName("表码回拨：普通日结拒绝负增量，必须走表计异常处置")
    void rollbackNegativeIncrementRejected() {
        newUnit("TG-T6", new BigDecimal("1000.00"), new BigDecimal("800.00"));
        assertThatThrownBy(() -> powerService.settle("TG-T6",
                settleReq("2026-10-01", "900.00", "750.00", "24.0", "r6neg")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("表码回拨或已换表");
        assertThat(dailyRepository.findByUnitNoOrderByStatDateDesc("TG-T6")).isEmpty();
    }

    @Test
    @DisplayName("选择封账启用新基数：当日增量 = 旧表尾段 + 新表首段，并留痕")
    void rollbackSettledWithNewBase() {
        newUnit("TG-T7", new BigDecimal("1000.00"), new BigDecimal("800.00"));
        PowerPayload.MeterEventRequest req = new PowerPayload.MeterEventRequest();
        req.setStatDate(LocalDate.parse("2026-10-01"));
        req.setEventType("ROLLBACK");
        req.setObservedReadingKwh(new BigDecimal("200.00")); // 回拨后新读数
        req.setOldMeterFinalReadingKwh(new BigDecimal("1200.00")); // 旧表止码
        req.setNewMeterStartReadingKwh(new BigDecimal("0.00"));
        req.setRunningHours(new BigDecimal("24.0"));
        req.setResolution("SETTLED_NEW_BASE");
        req.setOperator("校表员");
        req.setNote("电表回拨，旧表封账");
        req.setRequestId("me7");

        PowerPayload.MeterEventVO vo = powerService.handleMeterEvent("TG-T7", req);
        assertThat(vo.getEventStatus()).isEqualTo("RESOLVED");
        assertThat(vo.getResolutionName()).contains("封账");

        GenerationDaily d = dailyRepository
                .findByUnitNoAndStatDate("TG-T7", LocalDate.parse("2026-10-01")).orElseThrow();
        // 尾段 1200−1000=200，首段 200−0=200，合计 400
        assertThat(d.getGenerationKwh()).isEqualByComparingTo("400.00");
        assertThat(d.getBaseType()).isEqualTo("RESET_NEW_BASE");
        assertThat(d.getAbnormalStatus()).isEqualTo("ROLLBACK_RESET");
        assertThat(d.getMeterEventId()).isNotNull();
        assertThat(d.getRemark()).contains("旧表尾段").contains("新表首段");

        TurbineGenerator u = turbineRepository.findByUnitNo("TG-T7").orElseThrow();
        assertThat(u.getMeterReadingKwh()).isEqualByComparingTo("200.00");
        assertThat(u.getMeterSource()).isEqualTo("RESET_NEW_BASE");
        assertThat(u.getMeterStatus()).isEqualTo("NORMAL");

        // 次日起按新基数做差
        advanceClock(1);
        PowerPayload.DailyVO next = powerService.settle("TG-T7",
                settleReq("2026-10-02", "700.00", null, "24.0", "r7next"));
        assertThat(next.getGenerationKwh()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("选择暂停日结等待校表：挂起期间拒绝日结，校表确认新基数后恢复")
    void holdPendingCalibrationThenRelease() {
        newUnit("TG-T8", new BigDecimal("1000.00"), new BigDecimal("800.00"));
        PowerPayload.MeterEventRequest hold = new PowerPayload.MeterEventRequest();
        hold.setStatDate(LocalDate.parse("2026-10-01"));
        hold.setEventType("ROLLBACK");
        hold.setObservedReadingKwh(new BigDecimal("100.00"));
        hold.setResolution("HELD_PENDING_CALIBRATION");
        hold.setOperator("校表员");
        hold.setNote("读数异常，待校表");
        PowerPayload.MeterEventVO held = powerService.handleMeterEvent("TG-T8", hold);
        assertThat(held.getEventStatus()).isEqualTo("OPEN");

        TurbineGenerator u = turbineRepository.findByUnitNo("TG-T8").orElseThrow();
        assertThat(u.getMeterStatus()).isEqualTo("METER_HELD");
        // 基数未被污染
        assertThat(u.getMeterReadingKwh()).isEqualByComparingTo("1000.00");
        // 挂起期间没有日结
        assertThat(dailyRepository.findByUnitNoOrderByStatDateDesc("TG-T8")).isEmpty();

        // 日结被拒
        assertThatThrownBy(() -> powerService.settle("TG-T8",
                settleReq("2026-10-01", "1500.00", "1200.00", "24.0", "r8x")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("暂停日结等待校表");

        // 校表确认新基数
        Long eventId = meterEventRepository
                .findByUnitNoAndEventStatusOrderByCreateTimeDesc("TG-T8", "OPEN").get(0).getId();
        PowerPayload.MeterReleaseRequest release = new PowerPayload.MeterReleaseRequest();
        release.setReleaseReadingKwh(new BigDecimal("50.00"));
        release.setOperator("计量专责");
        release.setNote("校表完成，确认新基数 50");
        PowerPayload.MeterEventVO released = powerService.releaseMeterEvent(eventId, release);
        assertThat(released.getEventStatus()).isEqualTo("RESOLVED");

        advanceClock(1);
        PowerPayload.DailyVO d = powerService.settle("TG-T8",
                settleReq("2026-10-02", "550.00", null, "24.0", "r8next"));
        // 暂停日结不补结 10-01；10-02 增量 = 550 − 50
        assertThat(d.getGenerationKwh()).isEqualByComparingTo("500.00");
        assertThat(turbineRepository.findByUnitNo("TG-T8").orElseThrow().getMeterStatus())
                .isEqualTo("NORMAL");
    }

    @Test
    @DisplayName("换表封账：旧表尾段 + 新表首段，异常状态标记 REPLACE_RESET")
    void replaceMeterSettled() {
        newUnit("TG-T9", new BigDecimal("9000.00"), new BigDecimal("7600.00"));
        PowerPayload.MeterEventRequest req = new PowerPayload.MeterEventRequest();
        req.setStatDate(LocalDate.parse("2026-10-01"));
        req.setEventType("REPLACE");
        req.setObservedReadingKwh(new BigDecimal("300.00")); // 新表当日读数
        req.setOldMeterFinalReadingKwh(new BigDecimal("9200.00"));
        req.setNewMeterStartReadingKwh(new BigDecimal("0.00"));
        req.setRunningHours(new BigDecimal("24.0"));
        req.setResolution("SETTLED_NEW_BASE");
        req.setOperator("计量专责");
        powerService.handleMeterEvent("TG-T9", req);

        GenerationDaily d = dailyRepository
                .findByUnitNoAndStatDate("TG-T9", LocalDate.parse("2026-10-01")).orElseThrow();
        // 尾段 200 + 首段 300 = 500
        assertThat(d.getGenerationKwh()).isEqualByComparingTo("500.00");
        assertThat(d.getAbnormalStatus()).isEqualTo("REPLACE_RESET");
        MeterEvent event = meterEventRepository.findById(d.getMeterEventId()).orElseThrow();
        assertThat(event.getEventType()).isEqualTo("REPLACE");
        assertThat(event.getOldMeterFinalReadingKwh()).isEqualByComparingTo("9200.00");
        assertThat(event.getNewMeterStartReadingKwh()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("多人同时操作：同机组同日并发日结只成功一份")
    void concurrentSettleOnlyOneWins() throws Exception {
        newUnit("TG-TC", new BigDecimal("5000.00"), new BigDecimal("4000.00"));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<String> task = () -> {
            PowerPayload.GenerationRequest req = settleReq("2026-10-01", "5600.00",
                    "4500.00", "24.0", null);
            req.setOperator(Thread.currentThread().getName());
            powerService.settle("TG-TC", req);
            return "OK";
        };
        Future<String> f1 = pool.submit(task);
        Future<String> f2 = pool.submit(task);
        int ok = 0;
        int rejected = 0;
        for (Future<String> f : List.of(f1, f2)) {
            try {
                f.get();
                ok++;
            } catch (Exception e) {
                rejected++;
                assertThat(e.getCause()).isInstanceOf(BizException.class);
            }
        }
        pool.shutdown();
        assertThat(ok).as("仅一份日结成功").isEqualTo(1);
        assertThat(rejected).isEqualTo(1);
        assertThat(dailyRepository.findByUnitNoOrderByStatDateDesc("TG-TC")).hasSize(1);
        // 增量只被计算一次：5600 − 5000 = 600，而不是 1200
        assertThat(turbineRepository.findByUnitNo("TG-TC").orElseThrow().getMeterReadingKwh())
                .isEqualByComparingTo("5600.00");
    }

    @Test
    @DisplayName("重新打开页面后重复点击：同 requestId 幂等返回原日结，不新增、基数不二次推进")
    void sameRequestIdIdempotentAcrossPages() {
        newUnit("TG-TI", new BigDecimal("3000.00"), new BigDecimal("2500.00"));
        PowerPayload.DailyVO first = powerService.settle("TG-TI",
                settleReq("2026-10-01", "3500.00", "2900.00", "24.0", "page-uuid-1"));
        // 页面重开/网络重试，同一凭据再次提交（哪怕数值被改过）
        PowerPayload.DailyVO again = powerService.settle("TG-TI",
                settleReq("2026-10-01", "9999.00", "8888.00", "24.0", "page-uuid-1"));
        assertThat(again.getId()).isEqualTo(first.getId());
        assertThat(again.getGenerationKwh()).isEqualByComparingTo("500.00");
        assertThat(dailyRepository.findByUnitNoOrderByStatDateDesc("TG-TI")).hasSize(1);
        assertThat(turbineRepository.findByUnitNo("TG-TI").orElseThrow().getMeterReadingKwh())
                .isEqualByComparingTo("3500.00");
    }

    @Test
    @DisplayName("当日增量超过装机×运行小时理论上限被拒")
    void incrementOverCapacityRejected() {
        newUnit("TG-TX", new BigDecimal("0.00"), null);
        // 25 MW × 24h = 600000 kWh 上限；申报 800000 增量
        assertThatThrownBy(() -> powerService.settle("TG-TX",
                settleReq("2026-10-01", "800000.00", null, "24.0", "rx")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("理论上限");
    }
}
