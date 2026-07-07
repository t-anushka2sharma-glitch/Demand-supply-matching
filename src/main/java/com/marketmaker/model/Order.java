package com.marketmaker.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a single supply or demand order published to the market maker.
 *
 * An order tracks both its original quantity (as published) and its remaining
 * (unmatched) quantity, so it can be partially filled over time while it sits
 * in the ledger.
 */
public class Order {

    private final String id;
    private final OrderType type;
    private final String produce;
    private final BigDecimal price;
    private final int originalQuantity;
    private int remainingQuantity;
    private final LocalDateTime orderTime;

    /**
     * Monotonically increasing sequence number assigned at ingestion time.
     * Used as the tie-breaker for the "first-in-first-out on time" rule,
     * since two orders can otherwise share the same clock time (e.g. "09:45").
     */
    private final long sequence;

    public Order(String id, OrderType type, String produce, BigDecimal price,
                 int quantity, LocalDateTime orderTime, long sequence) {
        this.id = id;
        this.type = type;
        this.produce = produce;
        this.price = price;
        this.originalQuantity = quantity;
        this.remainingQuantity = quantity;
        this.orderTime = orderTime;
        this.sequence = sequence;
    }

    /**
     * Reconstructs an {@link Order} from persisted state (used when reloading
     * still-open orders from the database on application startup), where the
     * order may already be partially matched.
     */
    public static Order rehydrate(String id, OrderType type, String produce, BigDecimal price,
                                   int originalQuantity, int remainingQuantity,
                                   LocalDateTime orderTime, long sequence) {
        Order order = new Order(id, type, produce, price, originalQuantity, orderTime, sequence);
        int alreadyMatched = originalQuantity - remainingQuantity;
        if (alreadyMatched > 0) {
            order.reduceQuantity(alreadyMatched);
        }
        return order;
    }

    /** Reduces the remaining quantity by the given matched amount. */

    public void reduceQuantity(int matchedQuantity) {
        if (matchedQuantity > this.remainingQuantity) {
            throw new IllegalArgumentException(
                    "Cannot match " + matchedQuantity + "kg; only " + this.remainingQuantity + "kg remaining on order " + id);
        }
        this.remainingQuantity -= matchedQuantity;
    }

    public boolean isFullyMatched() {
        return remainingQuantity == 0;
    }

    public OrderStatus getStatus() {
        if (remainingQuantity == originalQuantity) {
            return OrderStatus.PENDING;
        } else if (remainingQuantity == 0) {
            return OrderStatus.FILLED;
        } else {
            return OrderStatus.PARTIALLY_FILLED;
        }
    }

    public String getId() {
        return id;
    }

    public OrderType getType() {
        return type;
    }

    public String getProduce() {
        return produce;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getOriginalQuantity() {
        return originalQuantity;
    }

    public int getRemainingQuantity() {
        return remainingQuantity;
    }

    public LocalDateTime getOrderTime() {
        return orderTime;
    }

    public long getSequence() {
        return sequence;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", produce='" + produce + '\'' +
                ", price=" + price +
                ", remaining=" + remainingQuantity + "/" + originalQuantity +
                ", time=" + orderTime +
                '}';
    }
}
