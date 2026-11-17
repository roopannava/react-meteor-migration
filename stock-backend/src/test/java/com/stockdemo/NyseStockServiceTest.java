package com.stockdemo;

import com.stockdemo.client.FinnhubClient;
import com.stockdemo.model.StockRow;
import com.stockdemo.service.NyseStockService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain unit tests for {@link NyseStockService} — no Spring context, no network.
 * Properties asserted here hold whether or not a FINNHUB_API_KEY is present;
 * the determinism check is gated on the synthetic (no-key) path.
 */
class NyseStockServiceTest {

    private final FinnhubClient finnhub = new FinnhubClient();
    private final NyseStockService service = new NyseStockService(finnhub);

    @Test
    void nyseListReturnsTwentyRows() {
        List<StockRow> rows = service.getTotalStockOnNyse();
        assertEquals(20, rows.size());
        rows.forEach(r -> {
            assertTrue(r.symbol() != null && !r.symbol().isBlank(), "symbol present");
            assertTrue(r.name() != null && !r.name().isBlank(), "name present");
        });
    }

    @Test
    void topWinnersAreTwentyRowsSortedDescendingByChange() {
        List<StockRow> winners = service.getTenTopWinnersOnNyse();
        assertEquals(20, winners.size());
        for (int i = 1; i < winners.size(); i++) {
            assertTrue(
                    winners.get(i - 1).changePercent() >= winners.get(i).changePercent(),
                    "winners must be sorted by changePercent descending"
            );
        }
    }

    @Test
    void syntheticDataIsDeterministicAcrossCalls() {
        // Only meaningful without a live key; live quotes legitimately change.
        if (finnhub.hasKey()) {
            return;
        }
        assertEquals(service.getTenTopWinnersOnNyse(), service.getTenTopWinnersOnNyse());
        assertEquals(service.getTotalStockOnNyse(), service.getTotalStockOnNyse());
    }
}
