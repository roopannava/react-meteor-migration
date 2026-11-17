import { useEffect } from 'react'
import { useAppDispatch, useAppSelector } from '../../app/hooks'
import {
  fetchTotalStockOnNyse,
  selectStockRows,
  selectStockStatus,
  selectStockError,
} from './stocksSlice'

// Renders GetTotalStockOnNyse. Reads everything from the Redux store via the
// typed hooks and kicks off the fetch on mount by dispatching the thunk.
export default function StockList() {
  const dispatch = useAppDispatch()
  const rows = useAppSelector(selectStockRows)
  const status = useAppSelector(selectStockStatus)
  const error = useAppSelector(selectStockError)

  useEffect(() => {
    if (status === 'idle') {
      dispatch(fetchTotalStockOnNyse())
    }
  }, [status, dispatch])

  if (status === 'loading' || status === 'idle') {
    return <p className="state">Loading NYSE stocks…</p>
  }
  if (status === 'failed') {
    return (
      <div className="state error">
        <p>Could not load stocks: {error}</p>
        <button onClick={() => dispatch(fetchTotalStockOnNyse())}>Retry</button>
      </div>
    )
  }

  return (
    <table>
      <thead>
        <tr>
          <th>#</th>
          <th>Symbol</th>
          <th>Name</th>
          <th className="num">Price</th>
          <th className="num">Change %</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((row, i) => (
          <tr key={row.symbol}>
            <td className="muted">{i + 1}</td>
            <td className="sym">{row.symbol}</td>
            <td>{row.name}</td>
            <td className="num">${row.price.toFixed(2)}</td>
            <td className={`num ${row.changePercent >= 0 ? 'up' : 'down'}`}>
              {row.changePercent >= 0 ? '+' : ''}
              {row.changePercent.toFixed(2)}%
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
