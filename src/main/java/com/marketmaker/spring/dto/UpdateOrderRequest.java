package com.marketmaker.spring.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Request body for {@code PUT /api/orders/{orderId}}.
 *
 * Only price and quantity are editable — produce, order type, and time are
 * fixed at creation. Editing is only allowed while the order is still
 * {@code PENDING} (nothing matched yet); see {@code OrderService.updateOrder}.
 *
 * Example:
 * <pre>
 * { "price": 23, "quantity": 80 }
 * </pre>
 */
public class UpdateOrderRequest {

    @NotNull(message = "price is required")
    @Positive(message = "price must be positive")
    private BigDecimal price;

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be positive")
    private Integer quantity;

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
