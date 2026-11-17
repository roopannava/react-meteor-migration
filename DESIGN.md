# Before-Migration Design — NYSE Stocks Demo

How the two front-ends work **today**, before any migration. One Spring Boot backend
serves two lists; a **React + Redux** app shows one, a **Meteor** app shows the other.
The point of writing this down: the two apps fetch and store data in fundamentally
different places, and seeing that clearly is what makes the upcoming migration
(moving the React list into Meteor) obvious rather than scary.

> Status: this is the **"before migration"** baseline. The React app has just been
> converted to TypeScript so it faithfully mirrors the real application being rehearsed.

---

## 1. System overview

Three tiers, each on its own port:

| Port | Process | Role |
|------|---------|------|
| `8080` | Spring Boot | The backend. Serves both stock lists over REST. |
| `5173` | React + Redux (Vite dev server) | Shows **GetTotalStockOnNyse**. |
| `3000` | Meteor app | Shows **GetTenTopWinnersOnNyse**. |
| `3001` | Meteor's server-side MongoDB | Where Meteor stores its data on the **server**. |
| `8081` | Meteor's rspack dev-server | Builds/serves the Meteor client bundle (dev only). |

Both apps hang off the **same** backend, but each consumes a **different endpoint**:

```
                          ┌────────────────────────────┐
                          │   Spring Boot  (:8080)      │
                          │   @RequestMapping           │
                          │     /api/stocks             │
                          ├────────────────────────────┤
          GET /nyse ─────▶│  GetTotalStockOnNyse        │◀───── GET /top-winners
          (20 rows)       │  GetTenTopWinnersOnNyse     │       (20 rows)
                          └────────────────────────────┘
                 ▲                                              ▲
                 │ browser fetch                   server fetch │
                 │ (in the user's tab)             (at startup) │
      ┌──────────┴───────────┐                       ┌──────────┴───────────┐
      │  React + Redux        │                       │  Meteor app          │
      │  (:5173)              │                       │  (:3000)             │
      │  GetTotalStockOnNyse  │                       │  GetTenTopWinnersOnNyse│
      └───────────────────────┘                       └──────────────────────┘
```

The single most important difference, visible above: **React fetches from the browser;
Meteor fetches from its server.** Everything else follows from that.

---

## 2. The shared backend (Spring Boot)

Identical for both apps — only the path differs. Key files
(`stock-backend/src/main/java/com/stockdemo/`):

| File | Responsibility |
|------|----------------|
| `controller/StockController.java` | Two endpoints: `GET /api/stocks/nyse` → `GetTotalStockOnNyse`, `GET /api/stocks/top-winners` → `GetTenTopWinnersOnNyse`. |
| `service/NyseStockService.java` | Produces each list. |
| `client/FinnhubClient.java` | Calls the Finnhub API; falls back to **deterministic synthetic data** when no `FINNHUB_API_KEY` is set (so the demo always returns rows). |
| `model/StockRow.java` | The wire contract: `{ symbol, name, price, changePercent }`. |
| `config/CorsConfig.java` | Allows origins `:5173` and `:3000`, **GET only**. |

Both lists return the same `StockRow` shape, so the two front-ends are solving the
*same* display problem in two *different* architectures.

---

## 3. Flow A — Spring Boot → React + Redux (the fetch happens in the **browser**)

The browser itself calls the backend. Redux holds the result in an **in-browser** store.

```
  Browser tab (:5173)
  ───────────────────
  main.tsx
    └─ <Provider store={store}>          ← store.ts: single Redux store
         └─ <StockList/>  mounts
               │
               │ useEffect → status === 'idle'
               ▼
            dispatch(fetchTotalStockOnNyse())      ← createAsyncThunk (stocksSlice.ts)
               │
               │  ┌─ pending   → state.status = 'loading'
               │  │
               ├──┤   fetch('http://localhost:8080/api/stocks/nyse')  ──────▶ Spring Boot :8080
               │  │                                                   ◀────── JSON StockRow[]
               │  └─ fulfilled → state.rows = payload, status='succeeded'
               │     (rejected → state.error = ...,  status='failed')
               ▼
            Redux store updated  (state.stocks.rows)
               │
               │ useAppSelector(selectStockRows)    ← app/hooks.ts (typed useSelector)
               ▼
            <StockList/> re-renders → <table> of 20 rows
```

Hop by hop:

1. **`main.tsx`** wraps the app in `<Provider store={store}>`. The store
   (`store.ts`) is a single `configureStore({ reducer: { stocks: stocksReducer } })`,
   living **entirely in the browser tab**.
2. **`StockList.tsx`** mounts and, in a `useEffect`, dispatches the thunk **once**
   (only when `status === 'idle'`).
3. **`fetchTotalStockOnNyse`** (`stocksSlice.ts`) is a `createAsyncThunk<StockRow[]>`.
   It runs a plain `fetch` to `:8080/api/stocks/nyse` **from the browser** — this is a
   real cross-origin call, which is why the backend's CORS config lists `:5173`.
4. The thunk's **lifecycle actions** drive the slice's `extraReducers`:
   `pending → loading`, `fulfilled → rows = payload`, `rejected → error`.
5. **`StockList`** reads everything back through the **typed hooks** in `app/hooks.ts`
   (`useAppSelector`, backed by `RootState` from `store.ts`) and renders the `<table>`.

Where the data lives: **the user's browser tab.** Close the tab, it's gone. Another
browser opening `:5173` does its own independent fetch into its own store.

---

## 4. Flow B — Spring Boot → Meteor (the fetch happens on the **server**)

The Meteor **server** calls the backend at startup, stores rows in **server-side Mongo**,
and *publishes* them. The browser never talks to Spring Boot — it receives rows over
Meteor's DDP protocol into an in-browser mirror database called **Minimongo**.

```
  Meteor SERVER (:3000 process)                         Spring Boot :8080
  ─────────────────────────────                         ─────────────────
  Meteor.startup()
    └─ loadTopWinners()  (server/main.js)
          │  fetch('http://localhost:8080/api/stocks/top-winners') ──────▶
          │                                                         ◀────── JSON StockRow[]
          │  for each row:
          │     StocksCollection.upsertAsync({symbol}, {$set: row})
          ▼
     ┌─────────────────────────────┐
     │  Server MongoDB (:3001)      │   ← StocksCollection ('stocks'), imports/api/stocks.js
     │  'stocks' collection         │
     └─────────────────────────────┘
          │
          │  Meteor.publish("topWinners",
          │     () => StocksCollection.find({},{sort:{changePercent:-1}}))
          │
          ▼   ── DDP over WebSocket ──▶   (pushes documents + live changes)
  ═══════════════════════════════════════════════════════════════════════
  Browser tab (:3000)
  ───────────────────
     ┌─────────────────────────────┐
     │  Minimongo (in-browser mirror)│  ← a client-side copy of the published docs
     │  'stocks'                     │
     └─────────────────────────────┘
          │
          │ TopWinners.jsx:
          │   const isLoading = useSubscribe("topWinners")   ← opens the pipe
          │   const stocks   = useFind(() => StocksCollection.find(..., sort))
          ▼
       <table> of 20 rows  (re-renders automatically when Minimongo changes)
```

Hop by hop:

1. **`loadTopWinners()`** (`server/main.js`) runs inside `Meteor.startup` — **on the
   server, once.** It `fetch`es `:8080/api/stocks/top-winners` and `upsertAsync`es each
   row into `StocksCollection` **keyed by `symbol`**, so re-runs update rows in place
   instead of duplicating.
2. **`StocksCollection`** (`imports/api/stocks.js`, `new Mongo.Collection('stocks')`) is
   *isomorphic*: the **same symbol** means the **server Mongo** collection on the server
   and the **Minimongo** mirror in the browser. Same code, two databases.
3. **`Meteor.publish("topWinners", ...)`** declares which documents subscribers may
   receive (sorted by `changePercent` desc).
4. In the browser, **`TopWinners.jsx`** calls `useSubscribe("topWinners")` to open the
   subscription and `useFind(() => StocksCollection.find(...))` to read from Minimongo.
5. Meteor streams the documents over **DDP** (a WebSocket protocol) and keeps them in
   sync. If the server later changes a row, every subscribed browser updates **with no
   extra fetch** — that's Meteor's built-in reactivity.

Where the data lives: **the server** (authoritative), mirrored into each browser. The
browser is a read-through view, not the owner.

---

## 5. Side-by-side comparison (the migration key)

This table is the whole point. Read it top to bottom and the migration plan writes itself.

| Concern | React + Redux (Flow A) | Meteor (Flow B) |
|---|---|---|
| Where the HTTP fetch runs | **Browser** | **Server** (at startup) |
| Who owns the data | The browser tab's Redux store | Server Mongo (mirrored to Minimongo) |
| Data store | `configureStore` (`store.ts`) | `new Mongo.Collection` (`imports/api/stocks.js`) |
| What triggers the fetch | Thunk dispatched on component mount | `loadTopWinners()` in `Meteor.startup` |
| Async mechanism | `createAsyncThunk` lifecycle (`pending`/`fulfilled`/`rejected`) | `publish` on the server + `useSubscribe` on the client |
| How the component reads data | `useAppSelector(selectStockRows)` | `useFind(() => Collection.find(...))` |
| Cross-client live sync | **No** — each tab fetches independently | **Yes** — DDP pushes changes to all subscribers |
| Loading state | `status === 'loading'` from the slice | `isLoading()` from `useSubscribe` |
| Language today | `.tsx` / `.ts`, strict `tsconfig` | `.jsx` / `.js` |

Conceptually: **Redux's store + slice + thunk == Meteor's collection + publication +
subscription.** They occupy the same slots in the architecture; they just put the
fetch (and the source of truth) on opposite sides of the network.

### A note on JSX vs TSX

Right now it's a mixed-language picture: the React app is TypeScript (`.tsx`/`.ts`),
the Meteor UI is JavaScript (`.jsx`/`.js`). That is **not** an obstacle to the migration:

- Meteor 3.6 **bundles the `typescript` package**, so its build compiles `.ts`/`.tsx`
  natively — the same way Vite's esbuild does for the React app. A migrated `.tsx`
  component can sit **right next to** the existing `.jsx` files; Meteor builds both.
- **Caveat:** Meteor *compiles* TypeScript but does **not** enforce a strict
  `tsconfig` (the React app's `strict`, `noUnusedLocals`, etc.) unless you add one to the
  Meteor app. So `.tsx` works, but type-checking is looser by default.
- **Net:** the migration changes the **data layer**, not the language — `.tsx` in,
  `.tsx` out. No need to convert the existing Meteor `.jsx` files either.

---

## 6. What the migration (next step) will do

To show **GetTotalStockOnNyse inside Meteor**, we replace Flow A's browser-centric
machinery with Flow B's server-centric machinery — reusing almost all of the UI:

| Before (React + Redux) | After (Meteor) |
|---|---|
| `store.ts` + `stocksSlice.ts` (store, slice, thunk) | **removed** — replaced by data-layer pieces below |
| Browser `fetch` in the thunk | `loadAllStocks()` server loader fetching `/api/stocks/nyse` |
| — | new `AllStocksCollection` (`new Mongo.Collection('allStocks')`) |
| — | `Meteor.publish("allStocks", ...)` |
| `useAppSelector` in `StockList.tsx` | `useSubscribe("allStocks")` + `useFind(...)` |
| `StockList.tsx` `<table>` JSX | moves **almost verbatim** into a Meteor `.tsx` component |

The `<table>` markup (symbol / name / price / change%, up-down coloring) barely changes.
What changes is *where the data comes from*: not a browser fetch into a Redux store, but
a server fetch into a Mongo collection that the component subscribes to.

> Building this is the next step and is **not** part of this document.

---

## Appendix — file map

```
react-meteor-migration/
├── stock-backend/            Spring Boot (:8080) — shared backend
│   └── src/main/java/com/stockdemo/
│       ├── controller/StockController.java      two endpoints
│       ├── service/NyseStockService.java
│       ├── client/FinnhubClient.java            Finnhub + synthetic fallback
│       ├── model/StockRow.java                  { symbol, name, price, changePercent }
│       └── config/CorsConfig.java               allows :5173 and :3000, GET only
│
├── react-redux-app/          React + Redux (:5173) — Flow A, GetTotalStockOnNyse
│   └── src/
│       ├── main.tsx                             <Provider store={store}>
│       ├── store.ts                             configureStore + RootState/AppDispatch
│       ├── app/hooks.ts                         typed useAppDispatch/useAppSelector
│       └── features/stocks/
│           ├── stocksSlice.ts                   StockRow, fetchTotalStockOnNyse thunk
│           └── StockList.tsx                     the <table>
│
└── meteor-app/               Meteor (:3000) — Flow B, GetTenTopWinnersOnNyse
    ├── server/main.js                           loadTopWinners() + publish("topWinners")
    ├── imports/api/stocks.js                    StocksCollection = new Mongo.Collection('stocks')
    └── imports/ui/TopWinners.jsx                useSubscribe + useFind → the <table>
```
