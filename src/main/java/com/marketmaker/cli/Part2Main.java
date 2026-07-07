package com.marketmaker.cli;

import com.marketmaker.db.OrderDao;
import com.marketmaker.db.TradeDao;
import com.marketmaker.engine.MatchingEngine;
import com.marketmaker.model.Order;
import com.marketmaker.model.Trade;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

/**
 * Java Exercise 1 - Part 2 [MySQL]: Persisting the Data.
 *
 * Behaves like Part 1 (input still comes from standard input, matching still
 * happens in-memory via {@link MatchingEngine}), but every order and trade is
 * now persisted to MySQL through plain JDBC, and any still-open orders from
 * previous runs are reloaded into the ledger on startup.
 *
 * Prerequisite: run src/main/resources/schema.sql against your MySQL server,
 * and adjust src/main/resources/db.properties with your credentials.
 *
 * Usage:
 *   mvn compile exec:java -Dexec.mainClass="com.marketmaker.cli.Part2Main" &lt; input.txt
 */
public final class Part2Main {

    private Part2Main() {
    }

    public static void main(String[] args) throws IOException {
        OrderDao orderDao = new OrderDao();
        TradeDao tradeDao = new TradeDao();
        MatchingEngine engine = new MatchingEngine();

        long sequence;
        try {
            // Re-hydrate the ledger with anything still open from a previous run,
            // and continue the sequence counter from where it left off.
            List<Order> openOrders = orderDao.loadAllOpenOrders();
            for (Order order : openOrders) {
                engine.loadExistingOpenOrder(order);
            }
            sequence = orderDao.getMaxSequence() + 1;
            System.err.println("Loaded " + openOrders.size() + " open order(s) from DB. Resuming at sequence " + sequence + ".");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load existing orders from DB on startup", e);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    Order order = OrderLineParser.parse(line, sequence++);

                    // Persist the order in its initial (unmatched) state first...
                    orderDao.insert(order);

                    // ...then match it, which may mutate this order and/or
                    // existing orders already in the ledger.
                    List<Trade> trades = engine.submit(order);

                    // Persist this order's post-match state.
                    orderDao.updateRemainingQuantity(order);

                    for (Trade trade : trades) {
                        tradeDao.insert(trade);
                        // Persist the counterpart orders' updated remaining quantities too.
                        persistCounterpart(orderDao, engine, trade.getDemandOrderId());
                        persistCounterpart(orderDao, engine, trade.getSupplyOrderId());
                        System.out.println(trade);
                    }
                } catch (IllegalArgumentException badLine) {
                    System.err.println("Skipping invalid line: " + line + " (" + badLine.getMessage() + ")");
                } catch (SQLException dbError) {
                    System.err.println("DB error while processing line '" + line + "': " + dbError.getMessage());
                }
            }
        }
    }

    private static void persistCounterpart(OrderDao orderDao, MatchingEngine engine, String orderId) throws SQLException {
        Order order = engine.getOrder(orderId);
        if (order != null) {
            orderDao.updateRemainingQuantity(order);
        }
    }
}
