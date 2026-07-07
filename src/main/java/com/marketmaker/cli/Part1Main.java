package com.marketmaker.cli;

import com.marketmaker.engine.MatchingEngine;
import com.marketmaker.model.Order;
import com.marketmaker.model.Trade;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Java Exercise 1 - Part 1: Demand-Supply Matching.
 *
 * Reads supply/demand orders from standard input (one per line) and writes
 * generated trades to standard output as soon as they occur. Everything is
 * held in memory for the lifetime of the process; nothing is persisted.
 *
 * Usage:
 *   mvn compile exec:java -Dexec.mainClass="com.marketmaker.cli.Part1Main" &lt; input.txt
 */
public final class Part1Main {

    private Part1Main() {
    }

    public static void main(String[] args) throws IOException {
        MatchingEngine engine = new MatchingEngine();
        long sequence = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    Order order = OrderLineParser.parse(line, sequence++);
                    List<Trade> trades = engine.submit(order);
                    for (Trade trade : trades) {
                        System.out.println(trade);
                    }
                } catch (IllegalArgumentException badLine) {
                    System.err.println("Skipping invalid line: " + line + " (" + badLine.getMessage() + ")");
                }
            }
        }
    }
}
