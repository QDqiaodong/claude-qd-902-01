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
 * 表计回拨 / 更换事件与处置留痕。
 *
 * 两种选择都会改变后续增量口径：
 * - SETTLED_NEW_BASE：旧表封账、启用新基数（当日增量 = 旧表尾段 + 新表首段）；
 * - HELD_PENDING_CALIBRATION：暂停日结等待校表，机组 meterStatus=METER_HELD，
 *   校表确认新基数后才能继续日结。
 */
@Entity
@Table(name = "meter_event")
public class MeterEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_no", nullable = false, unique = true, length = 40)
    private String eventNo;

    @Column(name = "unit_no", nullable = false, length = 32)
    private String unitNo;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    /** ROLLBACK 表计回拨 / REPLACE 表计更换 */
    @Column(name = "event_type", nullable = false, length = 16)
    private String eventType;

    /** 异常前日结基数 */
    @Column(name = "previous_base_reading_kwh", nullable = false, precision = 16, scale = 2)
    private BigDecimal previousBaseReadingKwh;

    /** 本次申报表码（回拨后读数 / 新表读数） */
    @Column(name = "observed_reading_kwh", nullable = false, precision = 16, scale = 2)
    private BigDecimal observedReadingKwh;

    /** 旧表封账止码 */
    @Column(name = "old_meter_final_reading_kwh", precision = 16, scale = 2)
    private BigDecimal oldMeterFinalReadingKwh;

    /** 新表起码 */
    @Column(name = "new_meter_start_reading_kwh", precision = 16, scale = 2)
    private BigDecimal newMeterStartReadingKwh;

    /** SETTLED_NEW_BASE / HELD_PENDING_CALIBRATION */
    @Column(name = "resolution", nullable = false, length = 32)
    private String resolution;

    /** OPEN 待校表 / RESOLVED 已校表恢复 */
    @Column(name = "event_status", nullable = false, length = 16)
    private String eventStatus = "OPEN";

    /** 封账选择对应的日结 id */
    @Column(name = "daily_id")
    private Long dailyId;

    /** 校表后确认的新基数 */
    @Column(name = "release_reading_kwh", precision = 16, scale = 2)
    private BigDecimal releaseReadingKwh;

    @Column(name = "operator", length = 32)
    private String operator;

    @Column(name = "note", length = 255)
    private String note;

    @Column(name = "release_operator", length = 32)
    private String releaseOperator;

    @Column(name = "release_note", length = 255)
    private String releaseNote;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "resolve_time")
    private LocalDateTime resolveTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventNo() {
        return eventNo;
    }

    public void setEventNo(String eventNo) {
        this.eventNo = eventNo;
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public BigDecimal getPreviousBaseReadingKwh() {
        return previousBaseReadingKwh;
    }

    public void setPreviousBaseReadingKwh(BigDecimal previousBaseReadingKwh) {
        this.previousBaseReadingKwh = previousBaseReadingKwh;
    }

    public BigDecimal getObservedReadingKwh() {
        return observedReadingKwh;
    }

    public void setObservedReadingKwh(BigDecimal observedReadingKwh) {
        this.observedReadingKwh = observedReadingKwh;
    }

    public BigDecimal getOldMeterFinalReadingKwh() {
        return oldMeterFinalReadingKwh;
    }

    public void setOldMeterFinalReadingKwh(BigDecimal oldMeterFinalReadingKwh) {
        this.oldMeterFinalReadingKwh = oldMeterFinalReadingKwh;
    }

    public BigDecimal getNewMeterStartReadingKwh() {
        return newMeterStartReadingKwh;
    }

    public void setNewMeterStartReadingKwh(BigDecimal newMeterStartReadingKwh) {
        this.newMeterStartReadingKwh = newMeterStartReadingKwh;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getEventStatus() {
        return eventStatus;
    }

    public void setEventStatus(String eventStatus) {
        this.eventStatus = eventStatus;
    }

    public Long getDailyId() {
        return dailyId;
    }

    public void setDailyId(Long dailyId) {
        this.dailyId = dailyId;
    }

    public BigDecimal getReleaseReadingKwh() {
        return releaseReadingKwh;
    }

    public void setReleaseReadingKwh(BigDecimal releaseReadingKwh) {
        this.releaseReadingKwh = releaseReadingKwh;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getReleaseOperator() {
        return releaseOperator;
    }

    public void setReleaseOperator(String releaseOperator) {
        this.releaseOperator = releaseOperator;
    }

    public String getReleaseNote() {
        return releaseNote;
    }

    public void setReleaseNote(String releaseNote) {
        this.releaseNote = releaseNote;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getResolveTime() {
        return resolveTime;
    }

    public void setResolveTime(LocalDateTime resolveTime) {
        this.resolveTime = resolveTime;
    }
}
