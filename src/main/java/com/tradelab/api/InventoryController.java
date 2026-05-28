package com.tradelab.api;

import com.tradelab.api.dto.InventoryResponse;
import com.tradelab.common.ApiResponse;
import com.tradelab.common.BusinessException;
import com.tradelab.common.ErrorCode;
import com.tradelab.infrastructure.persistence.InventoryLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryLedgerRepository inventoryLedgerRepository;

    @GetMapping("/{skuId}")
    public ApiResponse<InventoryResponse> get(@PathVariable Long skuId) {
        var ledger = inventoryLedgerRepository.findById(skuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Inventory not found"));
        return ApiResponse.ok(InventoryResponse.from(ledger));
    }
}
