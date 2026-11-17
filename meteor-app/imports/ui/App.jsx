
import { Header } from "./Header.jsx";
import { TopWinners } from "./TopWinners.jsx";
import StockList from "/imports/features/stocks/StockList";

export const App = () => (
    <div className="page">
        <Header />
        <main className="main">
            <TopWinners />
            <StockList />
        </main>
    </div>
);
