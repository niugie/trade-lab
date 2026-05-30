package com.tradelab.infrastructure.persistence;

import com.tradelab.domain.inventory.InventoryLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryLedgerRepository extends JpaRepository<InventoryLedger, Long> {

    @Modifying
    @Query("""
            UPDATE InventoryLedger l SET
            l.available = l.available - :qty,
            l.reserved = l.reserved + :qty
            WHERE l.skuId = :skuId AND l.available >= :qty
            """)
    int reserve(@Param("skuId") Long skuId, @Param("qty") int qty);

    @Modifying
    @Query("""
            UPDATE InventoryLedger l SET
            l.reserved = l.reserved - :qty
            WHERE l.skuId = :skuId AND l.reserved >= :qty
            """)
    int confirm(@Param("skuId") Long skuId, @Param("qty") int qty);

    @Modifying
    @Query("""
            UPDATE InventoryLedger l SET
            l.available = l.available + :qty,
            l.reserved = l.reserved - :qty
            WHERE l.skuId = :skuId AND l.reserved >= :qty
            """)
    int release(@Param("skuId") Long skuId, @Param("qty") int qty);

    @Modifying
    @Query("""
            UPDATE InventoryLedger l SET l.available = :available, l.reserved = :reserved
            WHERE l.skuId = :skuId
            """)
    void resetForDemo(@Param("skuId") Long skuId, @Param("available") int available, @Param("reserved") int reserved);
}
