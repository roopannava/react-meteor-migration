package com.stockdemo.service;

import com.stockdemo.client.FinnhubClient;
import com.stockdemo.model.StockRow;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Builds the two NYSE lists the demo serves.
 *
 * <p>When a {@code FINNHUB_API_KEY} is configured, prices and day % changes are
 * live (the NYSE roster also comes live from Finnhub's symbol endpoint). Without
 * a key, the service falls back to a curated roster of real NYSE names and
 * <em>deterministic synthetic</em> prices/changes (seeded per symbol) so the
 * demo always renders and "top winners" still has something to rank.
 */
@Service
public class NyseStockService {

    private static final int LIST_SIZE = 20;

    /**
     * A curated universe of liquid, real NYSE-listed common stocks. Doubles as
     * (a) the fallback roster for {@code getTotalStockOnNyse} when no key is set,
     * and (b) the bounded universe that {@code getTenTopWinnersOnNyse} quotes and
     * ranks (there is no free "top gainers" endpoint, so winners are computed).
     */
    private static final List<Listing> UNIVERSE = List.of(
            new Listing("JPM", "JPMorgan Chase & Co.", 215.0),
            new Listing("BAC", "Bank of America Corp.", 42.0),
            new Listing("WMT", "Walmart Inc.", 92.0),
            new Listing("JNJ", "Johnson & Johnson", 158.0),
            new Listing("V", "Visa Inc.", 305.0),
            new Listing("PG", "Procter & Gamble Co.", 168.0),
            new Listing("XOM", "Exxon Mobil Corp.", 118.0),
            new Listing("CVX", "Chevron Corp.", 152.0),
            new Listing("KO", "Coca-Cola Co.", 62.0),
            new Listing("DIS", "Walt Disney Co.", 95.0),
            new Listing("MCD", "McDonald's Corp.", 295.0),
            new Listing("NKE", "Nike Inc.", 78.0),
            new Listing("HD", "Home Depot Inc.", 410.0),
            new Listing("IBM", "International Business Machines Corp.", 235.0),
            new Listing("GE", "GE Aerospace", 190.0),
            new Listing("CAT", "Caterpillar Inc.", 395.0),
            new Listing("BA", "Boeing Co.", 180.0),
            new Listing("GS", "Goldman Sachs Group Inc.", 560.0),
            new Listing("WFC", "Wells Fargo & Co.", 72.0),
            new Listing("C", "Citigroup Inc.", 68.0),
            new Listing("PFE", "Pfizer Inc.", 26.0),
            new Listing("MRK", "Merck & Co. Inc.", 100.0),
            new Listing("ABBV", "AbbVie Inc.", 198.0),
            new Listing("T", "AT&T Inc.", 22.0),
            new Listing("VZ", "Verizon Communications Inc.", 43.0),
            new Listing("UNH", "UnitedHealth Group Inc.", 585.0),
            new Listing("CRM", "Salesforce Inc.", 330.0),
            new Listing("ORCL", "Oracle Corp.", 185.0),
            new Listing("F", "Ford Motor Co.", 11.0),
            new Listing("GM", "General Motors Co.", 52.0)
    );

    private final FinnhubClient finnhub;

    public NyseStockService(FinnhubClient finnhub) {
        this.finnhub = finnhub;
    }

    /**
     * {@code GetTotalStockOnNyse} — the first {@value #LIST_SIZE} NYSE stocks.
     * Roster comes live from Finnhub when a key is set, otherwise from the
     * curated universe; each row is then priced (live or synthetic).
     */
    public List<StockRow> getTotalStockOnNyse() {
        List<Listing> roster = resolveRoster();
        List<StockRow> rows = new ArrayList<>(roster.size());
        for (Listing listing : roster) {
            rows.add(toRow(listing));
        }
        return rows;
    }

    /**
     * {@code GetTenTopWinnersOnNyse} — the curated universe priced (live or
     * synthetic), sorted by day % change descending, capped at
     * {@value #LIST_SIZE}. ("Ten" in the name, 20 shown, per the demo spec.)
     */
    public List<StockRow> getTenTopWinnersOnNyse() {
        List<StockRow> rows = new ArrayList<>(UNIVERSE.size());
        for (Listing listing : UNIVERSE) {
            rows.add(toRow(listing));
        }
        rows.sort(Comparator.comparingDouble(StockRow::changePercent).reversed());
        return rows.subList(0, Math.min(LIST_SIZE, rows.size()));
    }

    /** Live NYSE roster when a key is set; the curated universe otherwise. */
    private List<Listing> resolveRoster() {
        if (finnhub.hasKey()) {
            List<FinnhubClient.Symbol> live = finnhub.listNyseSymbols(LIST_SIZE);
            if (!live.isEmpty()) {
                List<Listing> roster = new ArrayList<>(live.size());
                for (FinnhubClient.Symbol s : live) {
                    roster.add(new Listing(s.symbol(), s.name(), 100.0));
                }
                return roster;
            }
        }
        return UNIVERSE.subList(0, Math.min(LIST_SIZE, UNIVERSE.size()));
    }

    /** Price one listing: live via Finnhub when a key is set, synthetic otherwise. */
    private StockRow toRow(Listing listing) {
        if (finnhub.hasKey()) {
            FinnhubClient.Quote q = finnhub.quote(listing.symbol(), listing.seedPrice());
            return new StockRow(listing.symbol(), listing.name(), round(q.price()), round(q.changePercent()));
        }
        return synthetic(listing);
    }

    /**
     * Deterministic synthetic row for a listing (seeded by symbol, so the demo is
     * stable across restarts): price within ~±12% of the seed, change in
     * roughly [-5%, +9%].
     */
    private StockRow synthetic(Listing listing) {
        Random rnd = new Random(listing.symbol().hashCode());
        double priceFactor = 0.88 + rnd.nextDouble() * 0.24;   // 0.88 .. 1.12
        double price = listing.seedPrice() * priceFactor;
        double change = -5.0 + rnd.nextDouble() * 14.0;         // -5.0 .. +9.0
        return new StockRow(listing.symbol(), listing.name(), round(price), round(change));
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /** A curated universe entry: symbol, company name, and a seed/fallback price. */
    private record Listing(String symbol, String name, double seedPrice) {
    }
}
