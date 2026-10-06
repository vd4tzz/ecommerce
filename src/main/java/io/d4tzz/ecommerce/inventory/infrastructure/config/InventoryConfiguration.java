package io.d4tzz.ecommerce.inventory.infrastructure.config;

import io.d4tzz.ecommerce.inventory.domain.StockAllocator;
import io.d4tzz.ecommerce.inventory.domain.StockReservationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InventoryConfiguration {

    @Bean
    public StockReservationService stockReservationService() {
        return new StockReservationService();
    }

    @Bean
    public StockAllocator stockAllocator() {
        return new StockAllocator();
    }
}
