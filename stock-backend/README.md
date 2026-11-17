# stock-backend

Spring Boot (Java 21) service for the **React → Meteor migration demo**. It serves
two NYSE stock lists over REST — one consumed by the React + Redux app, the other
by the Meteor app.

Standalone. Mirrors the build and `FinnhubClient` pattern of
`../../Claude-Certified-Architect---Foundations/01-agentic-orchestration/marketsdata_backend`,
minus the Claude/Anthropic parts (there is no LLM here).

## Endpoints

| Method | Path | Name | Consumer |
|---|---|---|---|
| GET | `/api/stocks/nyse` | `GetTotalStockOnNyse` | React + Redux app |
| GET | `/api/stocks/top-winners` | `GetTenTopWinnersOnNyse` | Meteor app |

Both return a JSON array of rows:

```json
[ { "symbol": "JPM", "name": "JPMorgan Chase & Co.", "price": 215.3, "changePercent": 1.42 } ]
```

- **`/nyse`** — 20 NYSE stocks. Roster comes live from Finnhub's symbol endpoint
  when a key is set, otherwise from a curated list of real NYSE names.
- **`/top-winners`** — a curated universe (~30 liquid NYSE names) priced, sorted by
  day %-change descending, top 20. (Neither Finnhub nor EODHD has a *free* ranked
  "gainers" endpoint, so winners are computed by quote-and-sort.)

## Run

```bash
export FINNHUB_API_KEY=...        # optional — omit for synthetic data
./mvnw spring-boot:run            # http://localhost:8080
```

First run downloads Maven via the `./mvnw` wrapper — no system Maven needed.

Quick check:

```bash
curl -s http://localhost:8080/api/stocks/nyse | head
curl -s http://localhost:8080/api/stocks/top-winners | head
```

## Data source

Live data needs a free **Finnhub** key in `FINNHUB_API_KEY` (see `.env.example`).
Without a key the service returns **deterministic synthetic** data (seeded per
symbol, stable across restarts) so the demo always renders and "top winners" still
has something to rank. `GET`-only CORS is open to the two dev origins
(`:5173` React, `:3000` Meteor) in `config/CorsConfig.java`.

## Test

```bash
./mvnw test
```

`NyseStockServiceTest` is a plain unit test (no Spring context, no network): it
asserts both lists return 20 rows, winners are sorted descending by change, and —
in the no-key path — that the synthetic data is deterministic.

## Layout

```
src/main/java/com/stockdemo/
├── StockBackendApplication.java   # entry point
├── model/StockRow.java            # wire contract (symbol, name, price, changePercent)
├── client/FinnhubClient.java      # quote + NYSE symbol listing; synthetic-safe
├── service/NyseStockService.java  # the two lists; live-or-synthetic; winners = sort
├── controller/StockController.java# GET /api/stocks/nyse, /top-winners
└── config/CorsConfig.java         # dev-origin CORS
```
