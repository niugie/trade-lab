package com.tradelab;

import com.tradelab.domain.order.OrderStatus;
import com.tradelab.infrastructure.persistence.InventoryLedgerRepository;
import com.tradelab.infrastructure.persistence.TradeOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TradeOrderRepository tradeOrderRepository;

    @Autowired
    private InventoryLedgerRepository inventoryLedgerRepository;

    @Test
    void createPayAndCloseFlow() throws Exception {
        var before = inventoryLedgerRepository.findById(1L).orElseThrow();

        String createBody = """
                {
                  "userId": 10001,
                  "skuId": 1,
                  "quantity": 2,
                  "userCouponId": 1,
                  "idempotencyKey": "test-key-001"
                }
                """;

        String createResp = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAY"))
                .andReturn().getResponse().getContentAsString();

        long orderId = com.jayway.jsonpath.JsonPath.read(createResp, "$.data.orderId");

        var reserved = inventoryLedgerRepository.findById(1L).orElseThrow();
        assertThat(reserved.getAvailable()).isEqualTo(before.getAvailable() - 2);
        assertThat(reserved.getReserved()).isEqualTo(before.getReserved() + 2);

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payNo\":\"PAY-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));

        var afterPay = inventoryLedgerRepository.findById(1L).orElseThrow();
        assertThat(afterPay.getAvailable()).isEqualTo(before.getAvailable() - 2);
        assertThat(afterPay.getReserved()).isEqualTo(before.getReserved());

        var order = tradeOrderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void idempotentCreateReturnsSameOrder() throws Exception {
        String body = """
                {
                  "userId": 10002,
                  "skuId": 1,
                  "quantity": 1,
                  "idempotencyKey": "dup-key-001"
                }
                """;

        String r1 = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String r2 = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long id1 = com.jayway.jsonpath.JsonPath.read(r1, "$.data.orderId");
        long id2 = com.jayway.jsonpath.JsonPath.read(r2, "$.data.orderId");
        assertThat(id1).isEqualTo(id2);
    }

    @Test
    void getInventory() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.skuId").value(1));
    }
}
