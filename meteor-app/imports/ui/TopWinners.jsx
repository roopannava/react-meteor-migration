import React from "react";
import { useSubscribe, useFind } from "meteor/react-meteor-data";
import { StocksCollection } from "/imports/api/stocks";

export const TopWinners = () => {
    // Open the pipe: subscribe to the server's "topWinners" publication.
    // isLoading() stays true until the first rows arrive in Minimongo.
    const isLoading = useSubscribe("topWinners");

    // Reactively read those rows from Minimongo, highest gainer first.
    const stocks = useFind(() =>
        StocksCollection.find({}, { sort: { changePercent: -1 } })
    );

    if (isLoading()) {
        return <p>Loading top winners…</p>;
    }
    return (
        <section>
            <h2>Top Winners on NYSE ({stocks.length})</h2>
    <table>
    <thead>
        <tr>
            <th>#</th><th>Symbol</th><th>Name</th><th>Price</th><th>Change %</th>
    </tr>
    </thead>
    <tbody>
    {stocks.map((s, i) => (
            <tr key={s._id}>
                <td>{i + 1}</td>
                <td>{s.symbol}</td>
                <td>{s.name}</td>
                <td>${s.price.toFixed(2)}</td>
    <td style={{ color: s.changePercent >= 0 ? "green" : "crimson" }}>
    {s.changePercent >= 0 ? "+" : ""}{s.changePercent.toFixed(2)}%
    </td>
    </tr>
))}
    </tbody>
    </table>
    </section>
);
};
