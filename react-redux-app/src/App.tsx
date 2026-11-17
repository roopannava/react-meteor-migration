import StockList from './features/stocks/StockList'

export default function App() {
  return (
    <main className="app">
      <header>
        <h1>NYSE Stocks</h1>
        <p className="subtitle">
          <code>GetTotalStockOnNyse</code> · React + Redux (TypeScript) · data from
          Spring Boot <code>/api/stocks/nyse</code>
        </p>
      </header>
      <StockList />
    </main>
  )
}
