package com.example.orderservice;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderProducer orderProducer;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public OrderProducer orderProducer() {
            return new OrderProducer(null) {
                @Override
                public void sendOrder(Order order) {
                    // no-op for controller test stub
                }
            };
        }
    }

    @Test
    void testPlaceOrder() throws Exception {
        String orderJson = "{\"id\":\"100\",\"product\":\"Book\",\"quantity\":1}";

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().isOk())
                .andExpect(content().string("Order placed and sent to Kafka topic."));
    }
}
