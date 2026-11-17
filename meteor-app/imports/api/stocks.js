import { Mongo } from 'meteor/mongo';

// Reactive store for the GetTenTopWinnersOnNyse rows.
// The server fills it from Spring Boot; the client reads it via a subscription.
// Each document mirrors the backend's StockRow: { symbol, name, price, changePercent }.
export const StocksCollection = new Mongo.Collection('stocks');