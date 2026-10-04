package com.waste.power.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * 汽轮发电机组：保存装机容量与电能表「当前累计表码 / 基数状态」。
 *
 * 注意：meterReadingKwh 是累计表码（相邻日结做差的基数），不是单日发电量；
 * 单日发电量、上网电量、厂用电率逐日保存在 {@link GenerationDaily}。
 */
@Entity
@Table(name = "turbine_generator")
public class TurbineGenerator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unit_no", nullable = false, unique = true, length = 32)
    private String unitNo;

    @Column(name = "unit_name", length = 64)
    private String unitName;

    /** 装机容量 MW */
    @Column(name = "capacity_mw", precision = 8, scale = 2)
    private BigDecimal capacityMw;

    /** 发电电能表当前累计表码 kWh（日结基数，非当日发电量） */
    @Column(name = "meter_reading_kwh", precision = 16, scale = 2)
    private BigDecimal meterReadingKwh;

    /** 上网关口表当前累计表码 kWh */
    @Column(name = "grid_meter_reading_kwh", precision = 16, scale = 2)
    private BigDecimal gridMeterReadingKwh;

    /** 当前基数来源 SEED/PREV_DAY/RESET_NEW_BASE */
    @Column(name = "meter_source", nullable = false, length = 24)
    private String meterSource = "SEED";

    /** 表计状态 NORMAL/METER_HELD（暂停日结等待校表） */
    @Column(name = "meter_status", nullable = false, length = 24)
    private String meterStatus = "NORMAL";

    /** 最近一次日结的当日发电增量 kWh */
    @Column(name = "generation_kwh", precision = 14, scale = 2)
    private BigDecimal generationKwh;

    /** 最近一次日结的当日上网电量增量 kWh */
    @Column(name = "grid_kwh", precision = 14, scale = 2)
    private BigDecimal gridKwh;

    /** 最近一次日结厂用电率 % */
    @Column(name = "auxiliary_rate", precision = 5, scale = 2)
    private BigDecimal auxiliaryRate;

    /** 最近一次日结运行小时 h */
    @Column(name = "running_hours", precision = 10, scale = 1)
    private BigDecimal runningHours;

    /** 最近已日结统计日期 */
    @Column(name = "stat_date")
    private LocalDate statDate;

    @Column(name = "status", length = 16)
    private String status;

    /** 乐观锁：两人同时日结只成功一份 */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "remark", length = 128)
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUnitNo() {
        return unitNo;
    }

    public void setUnitNo(String unitNo) {
        this.unitNo = unitNo;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public BigDecimal getCapacityMw() {
        return capacityMw;
    }

    public void setCapacityMw(BigDecimal capacityMw) {
        this.capacityMw = capacityMw;
    }

    public BigDecimal getMeterReadingKwh() {
        return meterReadingKwh;
    }

    public void setMeterReadingKwh(BigDecimal meterReadingKwh) {
        this.meterReadingKwh = meterReadingKwh;
    }

    public BigDecimal getGridMeterReadingKwh() {
        return gridMeterReadingKwh;
    }

    public void setGridMeterReadingKwh(BigDecimal gridMeterReadingKwh) {
        this.gridMeterReadingKwh = gridMeterReadingKwh;
    }

    public String getMeterSource() {
        return meterSource;
    }

    public void setMeterSource(String meterSource) {
        this.meterSource = meterSource;
    }

    public String getMeterStatus() {
        return meterStatus;
    }

    public void setMeterStatus(String meterStatus) {
        this.meterStatus = meterStatus;
    }

    public BigDecimal getGenerationKwh() {
        return generationKwh;
    }

    public void setGenerationKwh(BigDecimal generationKwh) {
        this.generationKwh = generationKwh;
    }

    public BigDecimal getGridKwh() {
        return gridKwh;
    }

    public void setGridKwh(BigDecimal gridKwh) {
        this.gridKwh = gridKwh;
    }

    public BigDecimal getAuxiliaryRate() {
        return auxiliaryRate;
    }

    public void setAuxiliaryRate(BigDecimal auxiliaryRate) {
        this.auxiliaryRate = auxiliaryRate;
    }

    public BigDecimal getRunningHours() {
        return runningHours;
    }

    public void setRunningHours(BigDecimal runningHours) {
        this.runningHours = runningHours;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
