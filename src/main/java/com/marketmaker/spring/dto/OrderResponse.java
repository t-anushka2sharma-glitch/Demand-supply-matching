package com.marketmaker.spring.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response returned after successfully submitting a supply or demand order.
 */
public class OrderResponse {

    private String orderId;
    private String type;
    private String produce;
    private BigDecimal price;
    private int quantity;
    private String status;
    private List<String> tradesGenerated;

    public OrderResponse() {
    }

    public OrderResponse(String orderId, String type, String produce, BigDecimal price,
                          int quantity, String status, List<String> tradesGenerated) {
        this.orderId = orderId;
        this.type = type;
        this.produce = produce;
        this.price = price;
        this.quantity = quantity;
        this.status = status;
        this.tradesGenerated = tradesGenerated;
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

    public String getProduce() {
        return produce;
    }

    public void setProduce(String produce) {
        this.produce = produce;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getTradesGenerated() {
        return tradesGenerated;
    }

    public void setTradesGenerated(List<String> tradesGenerated) {
        this.tradesGenerated = tradesGenerated;
    }
}
