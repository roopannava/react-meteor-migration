package com.stockdemo.model;

/**
 * One row of a stock list as sent to the frontends.
 *
 * This record is the wire contract for both endpoints; its field names become
 * the JSON keys ({@code symbol}, {@code name}, {@code price},
 * {@code changePercent}) that the React+Redux and Meteor apps render.
 */
public record StockRow(
        String symbol,
        String name,
        double price,
        double changePercent
) {
}
