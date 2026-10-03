package com.waste.power.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.waste.power.common.ApiResponse;
import com.waste.power.dto.PowerPayload;
import com.waste.power.service.PowerService;

/**
 * 余热锅炉与汽轮发电接口：蒸汽工况、发电量与上网电量登记（含理论上限校核）。
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

    @PostMapping("/boilers/{boilerNo}/adjust")
    public ApiResponse<PowerPayload.BoilerVO> adjustBoiler(@PathVariable String boilerNo,
            @RequestBody PowerPayload.BoilerRequest request) {
        return ApiResponse.success("锅炉蒸汽工况已更新", powerService.adjustBoiler(boilerNo, request));
    }

    @PostMapping("/turbines/{unitNo}/generation")
    public ApiResponse<PowerPayload.TurbineVO> register(@PathVariable String unitNo,
            @RequestBody PowerPayload.GenerationRequest request) {
        return ApiResponse.success("发电量与上网电量已登记", powerService.register(unitNo, request));
    }

    @GetMapping("/dict")
    public ApiResponse<Map<String, Object>> dict() {
        return ApiResponse.success(powerService.dict());
    }
}
