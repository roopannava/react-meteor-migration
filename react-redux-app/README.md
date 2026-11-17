# react-redux-app

React 18 + **Redux Toolkit** app for the **React → Meteor migration demo**. It
shows **`GetTotalStockOnNyse`** — the NYSE list served by the sibling Spring Boot
`stock-backend` at `GET /api/stocks/nyse`.

This is **setup #2 of 3**:

| Setup | App | Shows |
|---|---|---|
| 1 | `../stock-backend` (Spring Boot) | serves both lists |
| **2** | **this app** (React + Redux) | **`GetTotalStockOnNyse`** |
| 3 | `../meteor-app` (Meteor) | `GetTenTopWinnersOnNyse` |

## How the data flows (the Redux pattern)

```
StockList (component)
  └─ dispatch(fetchTotalStockOnNyse())      ← async thunk
        └─ fetch GET :8080/api/stocks/nyse  ← Spring Boot micro-service
              └─ pending / fulfilled / rejected actions
                    └─ stocksSlice reducer updates state.stocks
                          └─ useSelector re-renders the table
```

The Meteor migration later replaces this thunk-plus-slice round-trip with
Meteor's reactive data (a Method + Tracker), which is the whole point of the demo.

## Run

The backend must be running first:

```bash
cd ../stock-backend && ./mvnw spring-boot:run     # :8080
```

Then, in another terminal:

```bash
npm install
npm run dev                                        # http://localhost:5173
```

The backend's `CorsConfig` already allows `http://localhost:5173`, so the
browser's cross-origin GET to `:8080` succeeds. Point the app at a different
backend with `VITE_API_BASE` (see `.env.example`).

## Layout

```
src/
├── main.jsx                        # mounts React, wraps tree in <Provider store>
├── store.js                        # configureStore({ stocks })
├── App.jsx                         # header + <StockList>
├── styles.css
└── features/stocks/
    ├── stocksSlice.js              # fetchTotalStockOnNyse thunk + slice + selectors
    └── StockList.jsx               # reads the store, renders the 20-row table
```
