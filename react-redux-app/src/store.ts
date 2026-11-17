import { configureStore } from '@reduxjs/toolkit'
import stocksReducer from './features/stocks/stocksSlice'

// Single Redux store. `state.stocks` holds the GetTotalStockOnNyse list,
// its load status, and any error.
export const store = configureStore({
  reducer: {
    stocks: stocksReducer,
  },
})

// Types inferred from the store itself — consumed by the typed hooks in app/hooks.ts.
export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch
