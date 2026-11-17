package com.stockdemo.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Thin wrapper over two free Finnhub endpoints:
 * <ul>
 *   <li>{@code GET /api/v1/quote?symbol=SYM&token=KEY} — live price + day % change</li>
 *   <li>{@code GET /api/v1/stock/symbol?exchange=US&token=KEY} — the US symbol
 *       universe, from which we keep only NYSE (MIC {@code XNYS}) listings</li>
 * </ul>
 *
 * <p>The key is read from the {@code FINNHUB_API_KEY} environment variable,
 * never hard-coded. When the key is absent (or a call fails) the methods degrade
 * gracefully: {@link #quote} returns the caller's fallback price, and
 * {@link #listNyseSymbols} returns an empty list so the service can fall back to
 * its curated universe. {@link #hasKey()} lets the service decide up-front
 * whether live data is available.
 */
@Component
public class FinnhubClient {

    private static final String QUOTE_URL = "https://finnhub.io/api/v1/quote";
    private static final String SYMBOL_URL = "https://finnhub.io/api/v1/stock/symbol";
    private static final String NYSE_MIC = "XNYS";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey = System.getenv("FINNHUB_API_KEY");

    /** Whether a Finnhub key is configured, i.e. whether live data is available. */
    public boolean hasKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Live price and day percent change for one symbol. Any failure mode --
     * missing key, non-200, network error, or a symbol Finnhub can't price
     * (returns c=0) -- degrades to {@code fallbackPrice}.
     */
    public Quote quote(String symbol, double fallbackPrice) {
        if (!hasKey()) {
            return new Quote(fallbackPrice, 0.0);
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(QUOTE_URL + "?symbol=" + symbol + "&token=" + apiKey))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return new Quote(fallbackPrice, 0.0);
            }
            JsonNode root = mapper.readTree(response.body());
            double price = root.path("c").asDouble(0.0);
            double changePercent = root.path("dp").asDouble(0.0);
            // Finnhub returns c=0 for symbols it can't quote; keep the fallback price.
            if (price <= 0.0) {
                return new Quote(fallbackPrice, changePercent);
            }
            return new Quote(price, changePercent);
        } catch (Exception e) {
            return new Quote(fallbackPrice, 0.0);
        }
    }

    /**
     * NYSE-listed common-stock symbols, up to {@code limit}. Finnhub groups all
     * US venues under {@code exchange=US}; we filter to NYSE by MIC ({@code XNYS}).
     * Returns an empty list when no key is set or on any error, so the caller can
     * fall back to a curated universe.
     */
    public List<Symbol> listNyseSymbols(int limit) {
        List<Symbol> out = new ArrayList<>();
        if (!hasKey()) {
            return out;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SYMBOL_URL + "?exchange=US&token=" + apiKey))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return out;
            }
            JsonNode root = mapper.readTree(response.body());
            if (!root.isArray()) {
                return out;
            }
            for (JsonNode node : root) {
                if (!NYSE_MIC.equals(node.path("mic").asText())) {
                    continue;
                }
                if (!"Common Stock".equalsIgnoreCase(node.path("type").asText())) {
                    continue;
                }
                String symbol = node.path("symbol").asText();
                String name = node.path("description").asText();
                if (symbol.isBlank() || name.isBlank()) {
                    continue;
                }
                out.add(new Symbol(symbol, name));
                if (out.size() >= limit) {
                    break;
                }
            }
            return out;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** Live price and day percent change for one symbol. */
    public record Quote(double price, double changePercent) {
    }

    /** A listed symbol and its company name. */
    public record Symbol(String symbol, String name) {
    }
}
