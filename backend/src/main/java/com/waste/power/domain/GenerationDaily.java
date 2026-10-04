package com.waste.power.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 发电机组日结：按「机组 + 统计日」唯一。
 *
 * 当日发电增量 = 期末表码 closingReadingKwh − 期初基数 baseReadingKwh；
 * 表计回拨/更换封账时为「旧表尾段 + 新表首段」，在 remark 中保留每日计算依据。
 */
@Entity
@Table(name = "generation_daily")
public class GenerationDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unit_no", nullable = false, length = 32)
    private String unitNo;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    /** 发电表期初基数（相邻上日封账表码） */
    @Column(name = "base_reading_kwh", nullable = false, precision = 16, scale = 2)
    private BigDecimal baseReadingKwh;

    /** 发电表期末表码（当日当前表码） */
    @Column(name = "closing_reading_kwh", nullable = false, precision = 16, scale = 2)
    private BigDecimal closingReadingKwh;

    /** 当日发电增量 */
    @Column(name = "generation_kwh", nullable = false, precision = 14, scale = 2)
    private BigDecimal generationKwh;

    /** 上网表期初基数 */
    @Column(name = "grid_base_reading_kwh", precision = 16, scale = 2)
    private BigDecimal gridBaseReadingKwh;

    /** 上网表期末表码 */
    @Column(name = "grid_closing_reading_kwh", precision = 16, scale = 2)
    private BigDecimal gridClosingReadingKwh;

    /** 当日上网电量增量 */
    @Column(name = "grid_kwh", precision = 14, scale = 2)
    private BigDecimal gridKwh;

    /** 厂用电率 % */
    @Column(name = "auxiliary_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal auxiliaryRate;

    /** 当日运行小时 h */
    @Column(name = "running_hours", nullable = false, precision = 10, scale = 1)
    private BigDecimal runningHours;

    /** 基数来源 SEED/PREV_DAY/RESET_NEW_BASE */
    @Column(name = "base_type", nullable = false, length = 24)
    private String baseType;

    /** 异常类型 ROLLBACK 回拨 / REPLACE 更换 */
    @Column(name = "abnormal_type", length = 16)
    private String abnormalType;

    /** 异常处置 SETTLED_NEW_BASE */
    @Column(name = "resolution", length = 32)
    private String resolution;

    /** 异常状态 NORMAL/ROLLBACK_RESET/REPLACE_RESET */
    @Column(name = "abnormal_status", nullable = false, length = 24)
    private String abnormalStatus = "NORMAL";

    /** 关联表计事件 */
    @Column(name = "meter_event_id")
    private Long meterEventId;

    @Column(name = "operator", length = 32)
    private String operator;

    /** 页面请求凭据（同凭据重发幂等返回原日结） */
    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "remark", length = 255)
    private String remark;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

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

    public LocalDate getStatDate() {
        return statDate;
    }

    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }

    public BigDecimal getBaseReadingKwh() {
        return baseReadingKwh;
    }

    public void setBaseReadingKwh(BigDecimal baseReadingKwh) {
        this.baseReadingKwh = baseReadingKwh;
    }

    public BigDecimal getClosingReadingKwh() {
        return closingReadingKwh;
    }

    public void setClosingReadingKwh(BigDecimal closingReadingKwh) {
        this.closingReadingKwh = closingReadingKwh;
    }

    public BigDecimal getGenerationKwh() {
        return generationKwh;
    }

    public void setGenerationKwh(BigDecimal generationKwh) {
        this.generationKwh = generationKwh;
    }

    public BigDecimal getGridBaseReadingKwh() {
        return gridBaseReadingKwh;
    }

    public void setGridBaseReadingKwh(BigDecimal gridBaseReadingKwh) {
        this.gridBaseReadingKwh = gridBaseReadingKwh;
    }

    public BigDecimal getGridClosingReadingKwh() {
        return gridClosingReadingKwh;
    }

    public void setGridClosingReadingKwh(BigDecimal gridClosingReadingKwh) {
        this.gridClosingReadingKwh = gridClosingReadingKwh;
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

    public String getBaseType() {
        return baseType;
    }

    public void setBaseType(String baseType) {
        this.baseType = baseType;
    }

    public String getAbnormalType() {
        return abnormalType;
    }

    public void setAbnormalType(String abnormalType) {
        this.abnormalType = abnormalType;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getAbnormalStatus() {
        return abnormalStatus;
    }

    public void setAbnormalStatus(String abnormalStatus) {
        this.abnormalStatus = abnormalStatus;
    }

    public Long getMeterEventId() {
        return meterEventId;
    }

    public void setMeterEventId(Long meterEventId) {
        this.meterEventId = meterEventId;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
