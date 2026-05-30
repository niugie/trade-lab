package com.tradelab.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tradeLabOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Trade Lab API")
                        .description("交易域 REST API。预置数据：userId=10001, skuId=1, couponId=1。")
                        .version("1.0.0")
                        .license(new License().name("MIT")))
                .servers(List.of(new Server().url("/").description("Local")));
    }
}
