package com.marketmaker.spring.dto;

import java.util.List;

/**
 * Response returned by {@code GET /api/orders/{orderId}/status}.
 *
 * {@code status} is one of {@code PENDING} (nothing matched yet),
 * {@code PARTIALLY_FILLED} (some quantity matched, some still open), or
 * {@code FILLED} (fully matched / traded).
 */
public class StatusResponse {

    private String orderId;
    private String type;
    private String status;
    private int originalQuantity;
    private int remainingQuantity;
    private List<String> trades;

    public StatusResponse() {
    }

    public StatusResponse(String orderId, String type, String status,
                           int originalQuantity, int remainingQuantity, List<String> trades) {
        this.orderId = orderId;
        this.type = type;
        this.status = status;
        this.originalQuantity = originalQuantity;
        this.remainingQuantity = remainingQuantity;
        this.trades = trades;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getOriginalQuantity() {
        return originalQuantity;
    }

    public void setOriginalQuantity(int originalQuantity) {
        this.originalQuantity = originalQuantity;
    }

    public int getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(int remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }

    public List<String> getTrades() {
        return trades;
    }

    public void setTrades(List<String> trades) {
        this.trades = trades;
    }
}
