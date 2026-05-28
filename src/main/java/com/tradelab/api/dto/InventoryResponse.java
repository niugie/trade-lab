package com.tradelab.api.dto;

import com.tradelab.domain.inventory.InventoryLedger;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryResponse {

    private Long skuId;
    private Integer available;
    private Integer reserved;
    private Long version;

    public static InventoryResponse from(InventoryLedger ledger) {
        return InventoryResponse.builder()
                .skuId(ledger.getSkuId())
                .available(ledger.getAvailable())
                .reserved(ledger.getReserved())
                .version(ledger.getVersion())
                .build();
    }
}
