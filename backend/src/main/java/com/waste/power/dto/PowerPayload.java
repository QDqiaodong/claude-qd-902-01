package com.waste.power.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.waste.power.common.Dict;
import com.waste.power.domain.Boiler;
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

    /** 汽轮发电机视图（含理论发电上限与校核结果） */
    public static class TurbineVO {

        private Long id;
        private String unitNo;
        private String unitName;
        private BigDecimal capacityMw;
        private BigDecimal generationKwh;
        private BigDecimal gridKwh;
        private BigDecimal auxiliaryRate;
        private BigDecimal runningHours;
        private LocalDate statDate;
        private String status;
        private String statusName;
        private String remark;
        /** 理论发电上限 = 装机 MW × 1000 × 运行小时 */
        private BigDecimal theoreticalMaxKwh;
        /** 容量因子 = 发电量 / 理论上限 */
        private Double capacityFactor;

        public static TurbineVO from(TurbineGenerator unit, BigDecimal maxKwh, Double factor) {
            TurbineVO vo = new TurbineVO();
            vo.id = unit.getId();
            vo.unitNo = unit.getUnitNo();
            vo.unitName = unit.getUnitName();
            vo.capacityMw = unit.getCapacityMw();
            vo.generationKwh = unit.getGenerationKwh();
            vo.gridKwh = unit.getGridKwh();
            vo.auxiliaryRate = unit.getAuxiliaryRate();
            vo.runningHours = unit.getRunningHours();
            vo.statDate = unit.getStatDate();
            vo.status = unit.getStatus();
            vo.statusName = Dict.unitStatusName(unit.getStatus());
            vo.remark = unit.getRemark();
            vo.theoreticalMaxKwh = maxKwh;
            vo.capacityFactor = factor;
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
    }

    /** 发电量登记请求 */
    public static class GenerationRequest {

        private BigDecimal generationKwh;
        private BigDecimal gridKwh;
        private BigDecimal runningHours;
        private String operator;

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
