package com.tradelab.infrastructure.persistence;

import com.tradelab.domain.product.Sku;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkuRepository extends JpaRepository<Sku, Long> {
}
