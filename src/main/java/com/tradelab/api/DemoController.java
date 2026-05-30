package com.tradelab.api;

import com.tradelab.api.dto.DemoStateResponse;
import com.tradelab.application.demo.DemoResetService;
import com.tradelab.application.demo.DemoStateService;
import com.tradelab.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Demo")
@RestController
@RequestMapping("/api/v1/demo")
@RequiredArgsConstructor
public class DemoController {

    private final DemoResetService demoResetService;
    private final DemoStateService demoStateService;

    @Operation(summary = "聚合状态")
    @GetMapping("/state")
    public ApiResponse<DemoStateResponse> state(
            @RequestParam(defaultValue = "10001") long userId,
            @RequestParam(defaultValue = "1") long couponId,
            @RequestParam(defaultValue = "1") long skuId,
            @RequestParam(defaultValue = "5") int orderLimit) {
        return ApiResponse.ok(demoStateService.getState(userId, couponId, skuId, orderLimit));
    }

    @Operation(summary = "重置测试数据")
    @PostMapping("/reset")
    public ApiResponse<DemoStateResponse> reset() {
        return ApiResponse.ok(demoResetService.reset());
    }
}
