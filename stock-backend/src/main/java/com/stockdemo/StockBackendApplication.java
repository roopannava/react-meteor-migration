package com.stockdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the stock-backend service.
 *
 * Exposes two read-only NYSE endpoints consumed by the React+Redux app
 * ({@code GET /api/stocks/nyse}) and the Meteor app
 * ({@code GET /api/stocks/top-winners}) in the React-to-Meteor migration demo.
 */
@SpringBootApplication
public class StockBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockBackendApplication.class, args);
    }
}
