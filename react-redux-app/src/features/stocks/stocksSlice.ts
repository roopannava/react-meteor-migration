import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import type { PayloadAction } from '@reduxjs/toolkit'
import type { RootState } from '../../store'

// Base URL of the Spring Boot micro-service. Configurable via VITE_API_BASE.
const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'

// One row of the NYSE list — mirrors the Spring Boot StockRow wire contract.
export interface StockRow {
  symbol: string
  name: string
  price: number
  changePercent: number
}

type Status = 'idle' | 'loading' | 'succeeded' | 'failed'

interface StocksState {
  rows: StockRow[]
  status: Status
  error: string | null
}

const initialState: StocksState = {
  rows: [],
  status: 'idle',
  error: null,
}

/**
 * GetTotalStockOnNyse — async thunk that fetches the NYSE list this app owns.
 * Typed to resolve with `StockRow[]`, so the fulfilled payload is type-checked.
 */
export const fetchTotalStockOnNyse = createAsyncThunk<StockRow[]>(
  'stocks/fetchTotalStockOnNyse',
  async () => {
    const res = await fetch(`${API_BASE}/api/stocks/nyse`)
    if (!res.ok) throw new Error(`Request failed: HTTP ${res.status}`)
    return (await res.json()) as StockRow[]
  }
)

const stocksSlice = createSlice({
  name: 'stocks',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchTotalStockOnNyse.pending, (state) => {
        state.status = 'loading'
        state.error = null
      })
      .addCase(
        fetchTotalStockOnNyse.fulfilled,
        (state, action: PayloadAction<StockRow[]>) => {
          state.status = 'succeeded'
          state.rows = action.payload
        }
      )
      .addCase(fetchTotalStockOnNyse.rejected, (state, action) => {
        state.status = 'failed'
        state.error = action.error.message ?? 'Unknown error'
      })
  },
})

// Selectors — typed against RootState.
export const selectStockRows = (state: RootState): StockRow[] => state.stocks.rows
export const selectStockStatus = (state: RootState): Status => state.stocks.status
export const selectStockError = (state: RootState): string | null => state.stocks.error

export default stocksSlice.reducer
