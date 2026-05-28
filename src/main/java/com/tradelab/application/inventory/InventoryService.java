package com.tradelab.application.inventory;

import com.tradelab.common.BusinessException;
import com.tradelab.common.ErrorCode;
import com.tradelab.infrastructure.persistence.InventoryLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryLedgerRepository inventoryLedgerRepository;

    @Transactional
    public void reserve(Long skuId, int quantity) {
        int updated = inventoryLedgerRepository.reserve(skuId, quantity);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }

    @Transactional
    public void confirm(Long skuId, int quantity) {
        int updated = inventoryLedgerRepository.confirm(skuId, quantity);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to confirm inventory");
        }
    }

    @Transactional
    public void release(Long skuId, int quantity) {
        int updated = inventoryLedgerRepository.release(skuId, quantity);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to release inventory");
        }
    }
}
