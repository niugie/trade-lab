package com.tradelab;

import com.tradelab.domain.order.OrderStatus;
import com.tradelab.infrastructure.persistence.InventoryLedgerRepository;
import com.tradelab.infrastructure.persistence.TradeOrderRepository;
import com.tradelab.infrastructure.persistence.UserCouponRepository;
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

    @Autowired
    private UserCouponRepository userCouponRepository;

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

        String orderIdStr = com.jayway.jsonpath.JsonPath.read(createResp, "$.data.orderId").toString();
        long orderId = Long.parseLong(orderIdStr);

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

        long id1 = Long.parseLong(com.jayway.jsonpath.JsonPath.read(r1, "$.data.orderId").toString());
        long id2 = Long.parseLong(com.jayway.jsonpath.JsonPath.read(r2, "$.data.orderId").toString());
        assertThat(id1).isEqualTo(id2);
    }

    @Test
    void closeOrderReleasesInventoryAndCoupon() throws Exception {
        mockMvc.perform(post("/api/v1/demo/reset")).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":10001,"skuId":1,"quantity":1,"userCouponId":1,"idempotencyKey":"close-test-1"}
                                """))
                .andExpect(status().isOk());

        var afterCreate = inventoryLedgerRepository.findById(1L).orElseThrow();
        assertThat(afterCreate.getReserved()).isGreaterThan(0);

        String listResp = mockMvc.perform(get("/api/v1/orders?userId=10001&limit=5"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String orderIdStr = com.jayway.jsonpath.JsonPath.read(listResp, "$.data[0].orderId").toString();

        mockMvc.perform(post("/api/v1/orders/" + orderIdStr + "/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        var coupon = userCouponRepository.findById(1L).orElseThrow();
        assertThat(coupon.getStatus().name()).isEqualTo("AVAILABLE");
    }

    @Test
    void demoResetRestoresSeedState() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":10001,"skuId":1,"quantity":3,"userCouponId":1,"idempotencyKey":"reset-test-1"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/demo/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inventory.available").value(1000))
                .andExpect(jsonPath("$.data.inventory.reserved").value(0))
                .andExpect(jsonPath("$.data.coupon.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.pendingOrderCount").value(0));
    }

    @Test
    void getInventory() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.skuId").value(1));
    }

    @Test
    void getDemoState() throws Exception {
        mockMvc.perform(get("/api/v1/demo/state?userId=10001&couponId=1&skuId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inventory.skuId").value(1))
                .andExpect(jsonPath("$.data.coupon.couponId").value(1));
    }
}
