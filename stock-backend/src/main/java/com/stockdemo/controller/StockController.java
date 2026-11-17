package com.stockdemo.controller;

import com.stockdemo.model.StockRow;
import com.stockdemo.service.NyseStockService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST surface for the two NYSE lists.
 *
 * <ul>
 *   <li>{@code GET /api/stocks/nyse} — GetTotalStockOnNyse (React+Redux app)</li>
 *   <li>{@code GET /api/stocks/top-winners} — GetTenTopWinnersOnNyse (Meteor app)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final NyseStockService service;

    public StockController(NyseStockService service) {
        this.service = service;
    }

    @GetMapping("/nyse")
    public List<StockRow> totalStockOnNyse() {
        return service.getTotalStockOnNyse();
    }

    @GetMapping("/top-winners")
    public List<StockRow> tenTopWinnersOnNyse() {
        return service.getTenTopWinnersOnNyse();
    }
}
