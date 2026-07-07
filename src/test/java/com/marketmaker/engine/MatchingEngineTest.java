package com.marketmaker.engine;

import com.marketmaker.cli.OrderLineParser;
import com.marketmaker.model.Order;
import com.marketmaker.model.Trade;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchingEngineTest {

    /** Reproduces "Example 1" from the Part 1 exercise spec. */
    @Test
    void example1FromSpec() {
        String[] lines = {
                "s1 09:45 tomato 24/kg 100kg",
                "s2 09:46 tomato 20/kg 90kg",
                "d1 09:47 tomato 22/kg 110kg",
                "d2 09:48 tomato 21/kg 10kg",
                "d3 09:49 tomato 21/kg 40kg",
                "s3 09:50 tomato 19/kg 50kg"
        };
        List<String> trades = runScenario(lines);

        assertEquals(
                java.util.Arrays.asList(
                        "d1 s2 20/kg 90kg",
                        "d1 s3 19/kg 20kg",
                        "d2 s3 19/kg 10kg",
                        "d3 s3 19/kg 20kg"),
                trades);
    }

    /** Reproduces "Example 2" from the Part 1 exercise spec. */
    @Test
    void example2FromSpec() {
        String[] lines = {
                "d1 09:47 tomato 110/kg 1kg",
                "d2 09:45 potato 110/kg 10kg",
                "d3 09:48 tomato 110/kg 10kg",
                "s1 09:45 potato 110/kg 1kg",
                "s2 09:45 potato 110/kg 7kg",
                "s3 09:45 potato 110/kg 2kg",
                "s4 09:45 tomato 110/kg 11kg"
        };
        List<String> trades = runScenario(lines);

        assertEquals(
                java.util.Arrays.asList(
                        "d2 s1 110/kg 1kg",
                        "d2 s2 110/kg 7kg",
                        "d2 s3 110/kg 2kg",
                        "d1 s4 110/kg 1kg",
                        "d3 s4 110/kg 10kg"),
                trades);
    }

    private List<String> runScenario(String[] lines) {
        MatchingEngine engine = new MatchingEngine();
        List<String> allTrades = new ArrayList<>();
        long sequence = 0;
        for (String line : lines) {
            Order order = OrderLineParser.parse(line, sequence++);
            for (Trade trade : engine.submit(order)) {
                allTrades.add(trade.toString());
            }
        }
        return allTrades;
    }
}
