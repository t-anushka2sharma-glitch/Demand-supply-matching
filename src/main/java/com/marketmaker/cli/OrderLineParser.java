package com.marketmaker.cli;

import com.marketmaker.model.Order;
import com.marketmaker.model.OrderType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Parses a single line of standard-input order data into an {@link Order}.
 *
 * Expected format: {@code <order-id> <time> <produce> <price/kg> <quantity in kg>}
 * e.g. {@code s1 09:45 tomato 24/kg 100kg}
 *
 * Order ids starting with 's' (or 'S') are supply orders; ids starting with
 * 'd' (or 'D') are demand orders.
 */
public final class OrderLineParser {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private OrderLineParser() {
        // utility class
    }

    public static Order parse(String line, long sequence) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Blank line cannot be parsed as an order");
        }

        String[] tokens = trimmed.split("\\s+");
        if (tokens.length != 5) {
            throw new IllegalArgumentException(
                    "Expected 5 fields '<order-id> <time> <produce> <price/kg> <quantity in kg>' but got: " + line);
        }

        String orderId = tokens[0];
        LocalTime time = LocalTime.parse(tokens[1], TIME_FORMAT);
        String produce = tokens[2].toLowerCase();
        BigDecimal price = parsePrice(tokens[3]);
        int quantity = parseQuantity(tokens[4]);

        OrderType type = resolveType(orderId);
        LocalDateTime orderDateTime = LocalDateTime.of(LocalDate.now(), time);

        return new Order(orderId, type, produce, price, quantity, orderDateTime, sequence);
    }

    private static OrderType resolveType(String orderId) {
        char firstChar = Character.toLowerCase(orderId.charAt(0));
        if (firstChar == 's') {
            return OrderType.SUPPLY;
        } else if (firstChar == 'd') {
            return OrderType.DEMAND;
        }
        throw new IllegalArgumentException("Order id must start with 's' (supply) or 'd' (demand): " + orderId);
    }

    private static BigDecimal parsePrice(String token) {
        // e.g. "24/kg" -> 24
        String numeric = token.replaceAll("(?i)/kg", "");
        return new BigDecimal(numeric);
    }

    private static int parseQuantity(String token) {
        // e.g. "100kg" -> 100
        String numeric = token.replaceAll("(?i)kg", "");
        return Integer.parseInt(numeric);
    }
}
