package com.waste.power.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.waste.power.common.ApiResponse;
import com.waste.power.dto.PowerPayload;
import com.waste.power.service.PowerService;

/**
 * 余热锅炉与汽轮发电接口：蒸汽工况、发电日结（相邻表码增量口径）、表计回拨/更换处置。
 */
@RestController
@RequestMapping("/api/power")
public class PowerController {

    private final PowerService powerService;

    public PowerController(PowerService powerService) {
        this.powerService = powerService;
    }

    @GetMapping("/boilers")
    public ApiResponse<List<PowerPayload.BoilerVO>> boilers() {
        return ApiResponse.success(powerService.boilers());
    }

    @GetMapping("/turbines")
    public ApiResponse<List<PowerPayload.TurbineVO>> turbines() {
        return ApiResponse.success(powerService.turbines());
    }

    /** 日结列表：保留每天的计算依据（基数、期末表码、增量、基数来源、异常状态） */
    @GetMapping("/daily")
    public ApiResponse<List<PowerPayload.DailyVO>> daily(@RequestParam(required = false) String unitNo) {
        return ApiResponse.success(powerService.dailyList(unitNo));
    }

    /** 表计回拨/更换事件留痕列表 */
    @GetMapping("/meter-events")
    public ApiResponse<List<PowerPayload.MeterEventVO>> meterEvents(
            @RequestParam(required = false) String unitNo) {
        return ApiResponse.success(powerService.meterEvents(unitNo));
    }

    @PostMapping("/boilers/{boilerNo}/adjust")
    public ApiResponse<PowerPayload.BoilerVO> adjustBoiler(@PathVariable String boilerNo,
            @RequestBody PowerPayload.BoilerRequest request) {
        return ApiResponse.success("锅炉蒸汽工况已更新", powerService.adjustBoiler(boilerNo, request));
    }

    /** 发电日结：填报期末累计表码，后端按相邻表码计算当日增量 */
    @PostMapping("/turbines/{unitNo}/daily-settle")
    public ApiResponse<PowerPayload.DailyVO> settle(@PathVariable String unitNo,
            @RequestBody PowerPayload.GenerationRequest request) {
        return ApiResponse.success("日结完成：已按相邻表码计算当日发电增量",
                powerService.settle(unitNo, request));
    }

    /** 表计回拨/更换：旧表封账并启用新基数，或暂停日结等待校表 */
    @PostMapping("/turbines/{unitNo}/meter-event")
    public ApiResponse<PowerPayload.MeterEventVO> meterEvent(@PathVariable String unitNo,
            @RequestBody PowerPayload.MeterEventRequest request) {
        return ApiResponse.success("表计异常处置已记录留痕",
                powerService.handleMeterEvent(unitNo, request));
    }

    /** 校表恢复：确认新基数后解除暂停日结 */
    @PostMapping("/meter-events/{eventId}/release")
    public ApiResponse<PowerPayload.MeterEventVO> releaseMeter(@PathVariable Long eventId,
            @RequestBody PowerPayload.MeterReleaseRequest request) {
        return ApiResponse.success("校表完成，已按新基数恢复日结",
                powerService.releaseMeterEvent(eventId, request));
    }

    @GetMapping("/dict")
    public ApiResponse<Map<String, Object>> dict() {
        return ApiResponse.success(powerService.dict());
    }
}
