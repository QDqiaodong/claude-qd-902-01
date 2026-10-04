package com.waste.power.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.waste.power.common.Dict;
import com.waste.power.domain.Boiler;
import com.waste.power.domain.GenerationDaily;
import com.waste.power.domain.MeterEvent;
import com.waste.power.domain.TurbineGenerator;

/**
 * 余热锅炉与汽轮发电模块的请求与视图对象。
 */
public class PowerPayload {

    /** 锅炉视图 */
    public static class BoilerVO {

        private Long id;
        private String boilerNo;
        private String boilerName;
        private String furnaceCode;
        private BigDecimal steamPressureMpa;
        private BigDecimal steamTempC;
        private BigDecimal evaporationTph;
        private BigDecimal feedWaterTempC;
        private BigDecimal flueGasTempC;
        private BigDecimal ratedEvaporationTph;
        private String status;
        private String statusName;
        private String remark;
        /** 蒸发率 = 蒸发量 / 额定蒸发量 */
        private Double loadRate;

        public static BoilerVO from(Boiler boiler) {
            BoilerVO vo = new BoilerVO();
            vo.id = boiler.getId();
            vo.boilerNo = boiler.getBoilerNo();
            vo.boilerName = boiler.getBoilerName();
            vo.furnaceCode = boiler.getFurnaceCode();
            vo.steamPressureMpa = boiler.getSteamPressureMpa();
            vo.steamTempC = boiler.getSteamTempC();
            vo.evaporationTph = boiler.getEvaporationTph();
            vo.feedWaterTempC = boiler.getFeedWaterTempC();
            vo.flueGasTempC = boiler.getFlueGasTempC();
            vo.ratedEvaporationTph = boiler.getRatedEvaporationTph();
            vo.status = boiler.getStatus();
            vo.statusName = Dict.boilerStatusName(boiler.getStatus());
            vo.remark = boiler.getRemark();
            if (boiler.getRatedEvaporationTph() != null && boiler.getEvaporationTph() != null
                    && boiler.getRatedEvaporationTph().compareTo(BigDecimal.ZERO) > 0) {
                vo.loadRate = boiler.getEvaporationTph().doubleValue()
                        / boiler.getRatedEvaporationTph().doubleValue();
            } else {
                vo.loadRate = 0D;
            }
            return vo;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getBoilerNo() {
            return boilerNo;
        }

        public void setBoilerNo(String boilerNo) {
            this.boilerNo = boilerNo;
        }

        public String getBoilerName() {
            return boilerName;
        }

        public void setBoilerName(String boilerName) {
            this.boilerName = boilerName;
        }

        public String getFurnaceCode() {
            return furnaceCode;
        }

        public void setFurnaceCode(String furnaceCode) {
            this.furnaceCode = furnaceCode;
        }

        public BigDecimal getSteamPressureMpa() {
            return steamPressureMpa;
        }

        public void setSteamPressureMpa(BigDecimal steamPressureMpa) {
            this.steamPressureMpa = steamPressureMpa;
        }

        public BigDecimal getSteamTempC() {
            return steamTempC;
        }

        public void setSteamTempC(BigDecimal steamTempC) {
            this.steamTempC = steamTempC;
        }

        public BigDecimal getEvaporationTph() {
            return evaporationTph;
        }

        public void setEvaporationTph(BigDecimal evaporationTph) {
            this.evaporationTph = evaporationTph;
        }

        public BigDecimal getFeedWaterTempC() {
            return feedWaterTempC;
        }

        public void setFeedWaterTempC(BigDecimal feedWaterTempC) {
            this.feedWaterTempC = feedWaterTempC;
        }

        public BigDecimal getFlueGasTempC() {
            return flueGasTempC;
        }

        public void setFlueGasTempC(BigDecimal flueGasTempC) {
            this.flueGasTempC = flueGasTempC;
        }

        public BigDecimal getRatedEvaporationTph() {
            return ratedEvaporationTph;
        }

        public void setRatedEvaporationTph(BigDecimal ratedEvaporationTph) {
            this.ratedEvaporationTph = ratedEvaporationTph;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getStatusName() {
            return statusName;
        }

        public void setStatusName(String statusName) {
            this.statusName = statusName;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public Double getLoadRate() {
            return loadRate;
        }

        public void setLoadRate(Double loadRate) {
            this.loadRate = loadRate;
        }
    }

    /**
     * 汽轮发电机视图：当前表码 + 最近日结增量。
     * generationKwh/gridKwh/auxiliaryRate 为「最近日结」的当日值，绝不是累计表码。
     */
    public static class TurbineVO {

        private Long id;
        private String unitNo;
        private String unitName;
        private BigDecimal capacityMw;
        /** 发电表当前累计表码 */
        private BigDecimal meterReadingKwh;
        /** 上网表当前累计表码 */
        private BigDecimal gridMeterReadingKwh;
        /** 基数来源代码 / 名称 */
        private String meterSource;
        private String meterSourceName;
        /** 表计状态代码 / 名称 */
        private String meterStatus;
        private String meterStatusName;
        /** 最近日结：当日发电增量 */
        private BigDecimal generationKwh;
        /** 最近日结：当日上网电量增量 */
        private BigDecimal gridKwh;
        /** 最近日结：厂用电率 */
        private BigDecimal auxiliaryRate;
        private BigDecimal runningHours;
        private LocalDate statDate;
        /** 最近日结异常状态代码 / 名称 */
        private String abnormalStatus;
        private String abnormalStatusName;
        private String abnormalType;
        private String abnormalTypeName;
        private String status;
        private String statusName;
        private String remark;
        /** 理论发电上限 = 装机 MW × 1000 × 当日运行小时（用增量校核，不用累计读数） */
        private BigDecimal theoreticalMaxKwh;
        /** 容量因子 = 当日发电增量 / 理论上限 */
        private Double capacityFactor;
        private Integer version;

        public static TurbineVO from(TurbineGenerator unit, GenerationDaily lastDaily,
                BigDecimal maxKwh, Double factor) {
            TurbineVO vo = new TurbineVO();
            vo.id = unit.getId();
            vo.unitNo = unit.getUnitNo();
            vo.unitName = unit.getUnitName();
            vo.capacityMw = unit.getCapacityMw();
            vo.meterReadingKwh = unit.getMeterReadingKwh();
            vo.gridMeterReadingKwh = unit.getGridMeterReadingKwh();
            vo.meterSource = unit.getMeterSource();
            vo.meterSourceName = Dict.meterSourceName(unit.getMeterSource());
            vo.meterStatus = unit.getMeterStatus();
            vo.meterStatusName = Dict.meterStatusName(unit.getMeterStatus());
            vo.status = unit.getStatus();
            vo.statusName = Dict.unitStatusName(unit.getStatus());
            vo.remark = unit.getRemark();
            vo.theoreticalMaxKwh = maxKwh;
            vo.capacityFactor = factor;
            vo.version = unit.getVersion();
            if (lastDaily != null) {
                vo.generationKwh = lastDaily.getGenerationKwh();
                vo.gridKwh = lastDaily.getGridKwh();
                vo.auxiliaryRate = lastDaily.getAuxiliaryRate();
                vo.runningHours = lastDaily.getRunningHours();
                vo.statDate = lastDaily.getStatDate();
                vo.abnormalStatus = lastDaily.getAbnormalStatus();
                vo.abnormalStatusName = Dict.abnormalStatusName(lastDaily.getAbnormalStatus());
                vo.abnormalType = lastDaily.getAbnormalType();
                vo.abnormalTypeName = Dict.meterEventTypeName(lastDaily.getAbnormalType());
            }
            return vo;
        }

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

        public String getMeterSourceName() {
            return meterSourceName;
        }

        public void setMeterSourceName(String meterSourceName) {
            this.meterSourceName = meterSourceName;
        }

        public String getMeterStatus() {
            return meterStatus;
        }

        public void setMeterStatus(String meterStatus) {
            this.meterStatus = meterStatus;
        }

        public String getMeterStatusName() {
            return meterStatusName;
        }

        public void setMeterStatusName(String meterStatusName) {
            this.meterStatusName = meterStatusName;
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

        public String getAbnormalStatus() {
            return abnormalStatus;
        }

        public void setAbnormalStatus(String abnormalStatus) {
            this.abnormalStatus = abnormalStatus;
        }

        public String getAbnormalStatusName() {
            return abnormalStatusName;
        }

        public void setAbnormalStatusName(String abnormalStatusName) {
            this.abnormalStatusName = abnormalStatusName;
        }

        public String getAbnormalType() {
            return abnormalType;
        }

        public void setAbnormalType(String abnormalType) {
            this.abnormalType = abnormalType;
        }

        public String getAbnormalTypeName() {
            return abnormalTypeName;
        }

        public void setAbnormalTypeName(String abnormalTypeName) {
            this.abnormalTypeName = abnormalTypeName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getStatusName() {
            return statusName;
        }

        public void setStatusName(String statusName) {
            this.statusName = statusName;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public BigDecimal getTheoreticalMaxKwh() {
            return theoreticalMaxKwh;
        }

        public void setTheoreticalMaxKwh(BigDecimal theoreticalMaxKwh) {
            this.theoreticalMaxKwh = theoreticalMaxKwh;
        }

        public Double getCapacityFactor() {
            return capacityFactor;
        }

        public void setCapacityFactor(Double capacityFactor) {
            this.capacityFactor = capacityFactor;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }
    }

    /** 日结视图：保留每天的表码与计算依据 */
    public static class DailyVO {

        private Long id;
        private String unitNo;
        private LocalDate statDate;
        private BigDecimal baseReadingKwh;
        private BigDecimal closingReadingKwh;
        private BigDecimal generationKwh;
        private BigDecimal gridBaseReadingKwh;
        private BigDecimal gridClosingReadingKwh;
        private BigDecimal gridKwh;
        private BigDecimal auxiliaryRate;
        private BigDecimal runningHours;
        private String baseType;
        private String baseTypeName;
        private String abnormalType;
        private String abnormalTypeName;
        private String resolution;
        private String resolutionName;
        private String abnormalStatus;
        private String abnormalStatusName;
        private Long meterEventId;
        private String operator;
        private String requestId;
        private String remark;
        private LocalDateTime createTime;

        public static DailyVO from(GenerationDaily d) {
            DailyVO vo = new DailyVO();
            vo.id = d.getId();
            vo.unitNo = d.getUnitNo();
            vo.statDate = d.getStatDate();
            vo.baseReadingKwh = d.getBaseReadingKwh();
            vo.closingReadingKwh = d.getClosingReadingKwh();
            vo.generationKwh = d.getGenerationKwh();
            vo.gridBaseReadingKwh = d.getGridBaseReadingKwh();
            vo.gridClosingReadingKwh = d.getGridClosingReadingKwh();
            vo.gridKwh = d.getGridKwh();
            vo.auxiliaryRate = d.getAuxiliaryRate();
            vo.runningHours = d.getRunningHours();
            vo.baseType = d.getBaseType();
            vo.baseTypeName = Dict.meterSourceName(d.getBaseType());
            vo.abnormalType = d.getAbnormalType();
            vo.abnormalTypeName = Dict.meterEventTypeName(d.getAbnormalType());
            vo.resolution = d.getResolution();
            vo.resolutionName = Dict.meterResolutionName(d.getResolution());
            vo.abnormalStatus = d.getAbnormalStatus();
            vo.abnormalStatusName = Dict.abnormalStatusName(d.getAbnormalStatus());
            vo.meterEventId = d.getMeterEventId();
            vo.operator = d.getOperator();
            vo.requestId = d.getRequestId();
            vo.remark = d.getRemark();
            vo.createTime = d.getCreateTime();
            return vo;
        }

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

        public String getBaseTypeName() {
            return baseTypeName;
        }

        public void setBaseTypeName(String baseTypeName) {
            this.baseTypeName = baseTypeName;
        }

        public String getAbnormalType() {
            return abnormalType;
        }

        public void setAbnormalType(String abnormalType) {
            this.abnormalType = abnormalType;
        }

        public String getAbnormalTypeName() {
            return abnormalTypeName;
        }

        public void setAbnormalTypeName(String abnormalTypeName) {
            this.abnormalTypeName = abnormalTypeName;
        }

        public String getResolution() {
            return resolution;
        }

        public void setResolution(String resolution) {
            this.resolution = resolution;
        }

        public String getResolutionName() {
            return resolutionName;
        }

        public void setResolutionName(String resolutionName) {
            this.resolutionName = resolutionName;
        }

        public String getAbnormalStatus() {
            return abnormalStatus;
        }

        public void setAbnormalStatus(String abnormalStatus) {
            this.abnormalStatus = abnormalStatus;
        }

        public String getAbnormalStatusName() {
            return abnormalStatusName;
        }

        public void setAbnormalStatusName(String abnormalStatusName) {
            this.abnormalStatusName = abnormalStatusName;
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

    /** 表计回拨/更换事件视图 */
    public static class MeterEventVO {

        private Long id;
        private String eventNo;
        private String unitNo;
        private LocalDate statDate;
        private String eventType;
        private String eventTypeName;
        private BigDecimal previousBaseReadingKwh;
        private BigDecimal observedReadingKwh;
        private BigDecimal oldMeterFinalReadingKwh;
        private BigDecimal newMeterStartReadingKwh;
        private String resolution;
        private String resolutionName;
        private String eventStatus;
        private String eventStatusName;
        private Long dailyId;
        private BigDecimal releaseReadingKwh;
        private String operator;
        private String note;
        private String releaseOperator;
        private String releaseNote;
        private LocalDateTime createTime;
        private LocalDateTime resolveTime;

        public static MeterEventVO from(MeterEvent e) {
            MeterEventVO vo = new MeterEventVO();
            vo.id = e.getId();
            vo.eventNo = e.getEventNo();
            vo.unitNo = e.getUnitNo();
            vo.statDate = e.getStatDate();
            vo.eventType = e.getEventType();
            vo.eventTypeName = Dict.meterEventTypeName(e.getEventType());
            vo.previousBaseReadingKwh = e.getPreviousBaseReadingKwh();
            vo.observedReadingKwh = e.getObservedReadingKwh();
            vo.oldMeterFinalReadingKwh = e.getOldMeterFinalReadingKwh();
            vo.newMeterStartReadingKwh = e.getNewMeterStartReadingKwh();
            vo.resolution = e.getResolution();
            vo.resolutionName = Dict.meterResolutionName(e.getResolution());
            vo.eventStatus = e.getEventStatus();
            vo.eventStatusName = Dict.meterEventStatusName(e.getEventStatus());
            vo.dailyId = e.getDailyId();
            vo.releaseReadingKwh = e.getReleaseReadingKwh();
            vo.operator = e.getOperator();
            vo.note = e.getNote();
            vo.releaseOperator = e.getReleaseOperator();
            vo.releaseNote = e.getReleaseNote();
            vo.createTime = e.getCreateTime();
            vo.resolveTime = e.getResolveTime();
            return vo;
        }

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

        public String getEventTypeName() {
            return eventTypeName;
        }

        public void setEventTypeName(String eventTypeName) {
            this.eventTypeName = eventTypeName;
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

        public String getResolutionName() {
            return resolutionName;
        }

        public void setResolutionName(String resolutionName) {
            this.resolutionName = resolutionName;
        }

        public String getEventStatus() {
            return eventStatus;
        }

        public void setEventStatus(String eventStatus) {
            this.eventStatus = eventStatus;
        }

        public String getEventStatusName() {
            return eventStatusName;
        }

        public void setEventStatusName(String eventStatusName) {
            this.eventStatusName = eventStatusName;
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

    /**
     * 日结登记请求：申报当日期末表码（累计读数），后端按相邻表码计算增量。
     * 不再接受「发电量」直接填报，避免把累计电表读数误当成单日发电量。
     */
    public static class GenerationRequest {

        /** 统计日，缺省为今天；只允许日结今天或尚未日结的相邻日期 */
        private LocalDate statDate;
        /** 发电电能表当日期末累计表码 kWh */
        private BigDecimal meterReadingKwh;
        /** 上网关口表当日期末累计表码 kWh（缺省按 85% 估算上网电量） */
        private BigDecimal gridMeterReadingKwh;
        /** 当日运行小时 h（缺省 24） */
        private BigDecimal runningHours;
        private String operator;
        /** 页面请求凭据：重复请求（同凭据）幂等返回原日结，不产生第二份 */
        private String requestId;

        public LocalDate getStatDate() {
            return statDate;
        }

        public void setStatDate(LocalDate statDate) {
            this.statDate = statDate;
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

        public BigDecimal getRunningHours() {
            return runningHours;
        }

        public void setRunningHours(BigDecimal runningHours) {
            this.runningHours = runningHours;
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
    }

    /** 表计回拨/更换处置请求：自行决定封账启用新基数，还是暂停日结等待校表 */
    public static class MeterEventRequest {

        private LocalDate statDate;
        /** ROLLBACK 回拨 / REPLACE 更换 */
        private String eventType;
        /** 本次申报表码：回拨后的读数，或新表读数 */
        private BigDecimal observedReadingKwh;
        /** 旧表封账止码（封账选择必填；不填时取上日封账表码） */
        private BigDecimal oldMeterFinalReadingKwh;
        /** 新表起码（换表封账时填写，如 0） */
        private BigDecimal newMeterStartReadingKwh;
        /** 上网表本次读数（可选） */
        private BigDecimal gridMeterReadingKwh;
        /** SETTLED_NEW_BASE 旧表封账并启用新基数 / HELD_PENDING_CALIBRATION 暂停日结等待校表 */
        private String resolution;
        private BigDecimal runningHours;
        private String operator;
        private String note;
        private String requestId;

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

        public BigDecimal getGridMeterReadingKwh() {
            return gridMeterReadingKwh;
        }

        public void setGridMeterReadingKwh(BigDecimal gridMeterReadingKwh) {
            this.gridMeterReadingKwh = gridMeterReadingKwh;
        }

        public String getResolution() {
            return resolution;
        }

        public void setResolution(String resolution) {
            this.resolution = resolution;
        }

        public BigDecimal getRunningHours() {
            return runningHours;
        }

        public void setRunningHours(BigDecimal runningHours) {
            this.runningHours = runningHours;
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

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }
    }

    /** 校表恢复请求：确认新基数后恢复日结 */
    public static class MeterReleaseRequest {

        /** 校表后确认的发电表新基数 */
        private BigDecimal releaseReadingKwh;
        /** 校表后确认的上网表新基数（可选） */
        private BigDecimal gridReleaseReadingKwh;
        private String operator;
        private String note;

        public BigDecimal getReleaseReadingKwh() {
            return releaseReadingKwh;
        }

        public void setReleaseReadingKwh(BigDecimal releaseReadingKwh) {
            this.releaseReadingKwh = releaseReadingKwh;
        }

        public BigDecimal getGridReleaseReadingKwh() {
            return gridReleaseReadingKwh;
        }

        public void setGridReleaseReadingKwh(BigDecimal gridReleaseReadingKwh) {
            this.gridReleaseReadingKwh = gridReleaseReadingKwh;
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
    }

    /** 锅炉工况调整请求 */
    public static class BoilerRequest {

        private BigDecimal steamPressureMpa;
        private BigDecimal steamTempC;
        private BigDecimal evaporationTph;
        private String status;

        public BigDecimal getSteamPressureMpa() {
            return steamPressureMpa;
        }

        public void setSteamPressureMpa(BigDecimal steamPressureMpa) {
            this.steamPressureMpa = steamPressureMpa;
        }

        public BigDecimal getSteamTempC() {
            return steamTempC;
        }

        public void setSteamTempC(BigDecimal steamTempC) {
            this.steamTempC = steamTempC;
        }

        public BigDecimal getEvaporationTph() {
            return evaporationTph;
        }

        public void setEvaporationTph(BigDecimal evaporationTph) {
            this.evaporationTph = evaporationTph;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
