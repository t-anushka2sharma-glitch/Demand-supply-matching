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
    private BigDecimal price;
    private int originalQuantity;
    private int remainingQuantity;
    private final LocalDateTime orderTime;

    /**
     * True once the order's remaining (unmatched) quantity has been withdrawn
     * via DELETE, before it could be fully matched. Kept separate from the
     * quantity fields so a cancelled order can still report exactly how much
     * of it (if any) was matched into trades before cancellation.
     */
    private boolean cancelled = false;

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
                                   LocalDateTime orderTime, long sequence, String persistedStatus) {
        Order order = new Order(id, type, produce, price, originalQuantity, orderTime, sequence);
        int alreadyMatched = originalQuantity - remainingQuantity;
        if (alreadyMatched > 0) {
            order.reduceQuantity(alreadyMatched);
        }
        if (OrderStatus.CANCELLED.name().equals(persistedStatus)) {
            order.cancelled = true;
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

    /**
     * Withdraws whatever quantity is still unmatched (used by DELETE).
     * Quantity already locked into trades before this call is untouched.
     * Safe to call on an order with remainingQuantity == 0, though callers
     * should generally check {@link #getStatus()} first and reject that case
     * with a clearer error (nothing left to cancel).
     */
    public void cancelRemaining() {
        if (remainingQuantity > 0) {
            reduceQuantity(remainingQuantity);
        }
        this.cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Replaces this order's price and quantity (used by PUT). Only valid to
     * call while the order is still {@code PENDING} (nothing matched yet) —
     * callers must enforce that themselves before calling this, since this
     * method does not re-check it.
     */
    public void updateForEdit(BigDecimal newPrice, int newQuantity) {
        this.price = newPrice;
        this.originalQuantity = newQuantity;
        this.remainingQuantity = newQuantity;
    }

    public OrderStatus getStatus() {
        if (cancelled) {
            return OrderStatus.CANCELLED;
        } else if (remainingQuantity == originalQuantity) {
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
