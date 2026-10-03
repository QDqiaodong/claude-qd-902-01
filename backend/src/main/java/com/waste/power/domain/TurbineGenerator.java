package com.waste.power.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 汽轮发电机组：装机容量、发电量、上网电量、厂用电率、运行小时、统计日期。
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

    /** 发电量 kWh */
    @Column(name = "generation_kwh", precision = 14, scale = 2)
    private BigDecimal generationKwh;

    /** 上网电量 kWh */
    @Column(name = "grid_kwh", precision = 14, scale = 2)
    private BigDecimal gridKwh;

    /** 厂用电率 % */
    @Column(name = "auxiliary_rate", precision = 5, scale = 2)
    private BigDecimal auxiliaryRate;

    /** 运行小时 h */
    @Column(name = "running_hours", precision = 10, scale = 1)
    private BigDecimal runningHours;

    @Column(name = "stat_date")
    private LocalDate statDate;

    @Column(name = "status", length = 16)
    private String status;

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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
