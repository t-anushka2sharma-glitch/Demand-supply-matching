package com.marketmaker.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A trade generated when a demand order's price is greater than or equal to
 * a supply order's price. Per the exercise rules, a trade always settles at
 * the supply order's price, regardless of what the demand order offered.
 */
public class Trade {

    private final String demandOrderId;
    private final String supplyOrderId;
    private final BigDecimal price;
    private final int quantity;
    private final LocalDateTime tradeTime;

    public Trade(String demandOrderId, String supplyOrderId, BigDecimal price, int quantity, LocalDateTime tradeTime) {
        this.demandOrderId = demandOrderId;
        this.supplyOrderId = supplyOrderId;
        this.price = price;
        this.quantity = quantity;
        this.tradeTime = tradeTime;
    }

    public String getDemandOrderId() {
        return demandOrderId;
    }

    public String getSupplyOrderId() {
        return supplyOrderId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public LocalDateTime getTradeTime() {
        return tradeTime;
    }

    /** Formats the trade per the exercise's required output format:
     *  {@code <demand-order-id> <supply-order-id> <price/kg> <quantity in kg>} */
    @Override
    public String toString() {
        return demandOrderId + " " + supplyOrderId + " " + price.stripTrailingZeros().toPlainString() + "/kg " + quantity + "kg";
    }
}
