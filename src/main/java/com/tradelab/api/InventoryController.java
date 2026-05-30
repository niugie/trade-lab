package com.tradelab.api;

import com.tradelab.api.dto.InventoryResponse;
import com.tradelab.common.ApiResponse;
import com.tradelab.common.BusinessException;
import com.tradelab.common.ErrorCode;
import com.tradelab.infrastructure.persistence.InventoryLedgerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "库存")
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryLedgerRepository inventoryLedgerRepository;

    @Operation(summary = "查询 SKU 库存")
    @GetMapping("/{skuId}")
    public ApiResponse<InventoryResponse> get(@PathVariable Long skuId) {
        var ledger = inventoryLedgerRepository.findById(skuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Inventory not found"));
        return ApiResponse.ok(InventoryResponse.from(ledger));
    }
}
