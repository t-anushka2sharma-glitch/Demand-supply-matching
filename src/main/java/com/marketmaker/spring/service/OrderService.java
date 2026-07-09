package com.marketmaker.spring.service;

import com.marketmaker.db.OrderDao;
import com.marketmaker.db.TradeDao;
import com.marketmaker.engine.MatchingEngine;
import com.marketmaker.model.Order;
import com.marketmaker.model.OrderStatus;
import com.marketmaker.model.OrderType;
import com.marketmaker.model.Trade;
import com.marketmaker.spring.dto.DemandOrderRequest;
import com.marketmaker.spring.dto.OrderResponse;
import com.marketmaker.spring.dto.StatusResponse;
import com.marketmaker.spring.dto.SupplyOrderRequest;
import com.marketmaker.spring.dto.UpdateOrderRequest;
import com.marketmaker.spring.exception.InvalidOrderStateException;
import com.marketmaker.spring.exception.OrderNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Application service for the REST layer: bridges incoming HTTP requests to
 * the in-memory {@link MatchingEngine} and the JDBC persistence layer from
 * Part 2, so every order/trade submitted through the API is durable and the
 * ledger survives an application restart.
 *
 * Part 4 additions:
 *  - order ids are auto-generated (never supplied by the client)
 *  - {@link #getOrderStatus(String)} looks up whether a given order id
 *    (supply or demand) is still pending or has been (partially/fully)
 *    matched, falling back to the database so it also works for orders
 *    fully filled before the current process started.
 */
@Service
public class OrderService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final MatchingEngine engine = new MatchingEngine();
    private final OrderDao orderDao = new OrderDao();
    private final TradeDao tradeDao = new TradeDao();
    private final AtomicLong sequenceCounter = new AtomicLong(0);

    // Auto-generated id counters (Part 4), e.g. next supply id = "s" + supplyIdCounter.incrementAndGet()
    private final AtomicInteger supplyIdCounter = new AtomicInteger(0);
    private final AtomicInteger demandIdCounter = new AtomicInteger(0);

    /** On startup, reload any still-open orders from the DB so the ledger survives restarts. */
    @PostConstruct
    public void init() {
        try {
            List<Order> openOrders = orderDao.loadAllOpenOrders();
            for (Order order : openOrders) {
                engine.loadExistingOpenOrder(order);
            }
            sequenceCounter.set(orderDao.getMaxSequence() + 1);
            supplyIdCounter.set(orderDao.getMaxIdSuffix("s"));
            demandIdCounter.set(orderDao.getMaxIdSuffix("d"));
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load existing orders from the database on startup", e);
        }
    }

    public OrderResponse addSupplyOrder(SupplyOrderRequest request) {
        String generatedId = "s" + supplyIdCounter.incrementAndGet();
        Order order = buildOrder(generatedId, OrderType.SUPPLY, request.getTime(),
                request.getProduce(), request.getPrice(), request.getQuantity());
        return processNewOrder(order);
    }

    public OrderResponse addDemandOrder(DemandOrderRequest request) {
        String generatedId = "d" + demandIdCounter.incrementAndGet();
        Order order = buildOrder(generatedId, OrderType.DEMAND, request.getTime(),
                request.getProduce(), request.getPrice(), request.getQuantity());
        return processNewOrder(order);
    }

    /**
     * Looks up the current status of an order (supply or demand) by id.
     * Checks the live in-memory ledger first (covers orders from the
     * current process, including ones already fully matched and evicted
     * from the ledger but still tracked by id), then falls back to the
     * database (covers orders from before a restart).
     */
    public StatusResponse getOrderStatus(String orderId) {
        Order order = engine.getOrder(orderId);
        if (order == null) {
            try {
                order = orderDao.findById(orderId);
            } catch (SQLException e) {
                throw new IllegalStateException("Database error while looking up order " + orderId, e);
            }
        }
        if (order == null) {
            throw new OrderNotFoundException(orderId);
        }

        try {
            List<String> tradeDescriptions = tradeDao.findByOrderId(orderId).stream()
                    .map(Trade::toString)
                    .collect(Collectors.toList());
            return new StatusResponse(order.getId(), order.getType().name(), order.getStatus().name(),
                    order.getOriginalQuantity(), order.getRemainingQuantity(), tradeDescriptions);
        } catch (SQLException e) {
            throw new IllegalStateException("Database error while looking up trades for order " + orderId, e);
        }
    }

    /**
     * Edits an order's price and/or quantity (PUT), then immediately
     * re-attempts matching under the new values. Only allowed while the
     * order is still {@code PENDING} — nothing about it has matched yet —
     * since rewriting price/quantity on a partially-matched order would
     * leave its trade history inconsistent with its own row.
     */
    public OrderResponse updateOrder(String orderId, UpdateOrderRequest request) {
        Order order = engine.getOrder(orderId);
        if (order == null) {
            order = loadFromDbOrThrow(orderId);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Cannot update order '" + orderId + "': it is " + order.getStatus() +
                            ", not PENDING. Only an order with no matched quantity yet can be edited.");
        }
        engine.withdrawForUpdate(orderId);
        order.updateForEdit(request.getPrice(), request.getQuantity());
        return applyMatchAndPersist(order, false);
    }

    /**
     * Cancels whatever quantity of an order is still unmatched (DELETE).
     * Quantity already locked into trades before the call stays as-is;
     * only the order's remaining/original quantity and status are updated.
     * Rejected (409) if the order is already {@code FILLED} or already
     * {@code CANCELLED} — there's nothing left to withdraw either way.
     */
    public StatusResponse cancelOrder(String orderId) {
        Order order = engine.getOrder(orderId);
        if (order == null) {
            order = loadFromDbOrThrow(orderId);
        }
        OrderStatus currentStatus = order.getStatus();
        if (currentStatus == OrderStatus.FILLED || currentStatus == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException(
                    "Cannot cancel order '" + orderId + "': it is already " + currentStatus + ".");
        }

        Order cancelled = engine.cancelOrder(orderId);
        try {
            orderDao.updateOrderDetails(cancelled);
            List<String> tradeDescriptions = tradeDao.findByOrderId(orderId).stream()
                    .map(Trade::toString)
                    .collect(Collectors.toList());
            return new StatusResponse(cancelled.getId(), cancelled.getType().name(), cancelled.getStatus().name(),
                    cancelled.getOriginalQuantity(), cancelled.getRemainingQuantity(), tradeDescriptions);
        } catch (SQLException e) {
            throw new IllegalStateException("Database error while cancelling order " + orderId, e);
        }
    }

    /** Loads an order straight from the DB, or throws a 404 if it truly doesn't exist anywhere. */
    private Order loadFromDbOrThrow(String orderId) {
        try {
            Order order = orderDao.findById(orderId);
            if (order == null) {
                throw new OrderNotFoundException(orderId);
            }
            return order;
        } catch (SQLException e) {
            throw new IllegalStateException("Database error while looking up order " + orderId, e);
        }
    }

    /**
     * Wipes every order and trade, both in the database and in the live
     * in-memory ledger, and restarts the auto-generated id/sequence counters
     * from zero — a one-call replacement for manually truncating both
     * tables and restarting the app. Used by {@code DELETE /api/orders}.
     */
    public void resetAll() {
        try {
            orderDao.deleteAllOrdersAndTrades();
        } catch (SQLException e) {
            throw new IllegalStateException("Database error while resetting orders and trades", e);
        }
        engine.reset();
        sequenceCounter.set(0);
        supplyIdCounter.set(0);
        demandIdCounter.set(0);
    }

    private Order buildOrder(String orderId, OrderType type, String time, String produce,
                              BigDecimal price, int quantity) {
        LocalTime localTime = LocalTime.parse(time, TIME_FORMAT);
        LocalDateTime orderTime = LocalDateTime.of(LocalDate.now(), localTime);
        return new Order(orderId, type, produce.toLowerCase(), price, quantity, orderTime, sequenceCounter.getAndIncrement());
    }

    /** Persists a brand-new order, matches it, then persists the outcome. */
    private OrderResponse processNewOrder(Order order) {
        return applyMatchAndPersist(order, true);
    }

    /**
     * Matches an order (new or edited) against the ledger, then persists the
     * resulting state of every order touched by the match (mirrors the flow
     * used by {@code Part2Main} so behaviour stays identical across the CLI
     * and API). For a brand-new order this inserts a fresh row; for an
     * edited (PUT) order the row already exists, so its details are updated
     * in place instead.
     */
    private OrderResponse applyMatchAndPersist(Order order, boolean isNewOrder) {
        try {
            if (isNewOrder) {
                orderDao.insert(order);
            }

            List<Trade> trades = engine.submit(order);

            if (isNewOrder) {
                orderDao.updateRemainingQuantity(order);
            } else {
                orderDao.updateOrderDetails(order);
            }

            for (Trade trade : trades) {
                tradeDao.insert(trade);
                persistCounterpart(trade.getDemandOrderId());
                persistCounterpart(trade.getSupplyOrderId());
            }

            List<String> tradeDescriptions = trades.stream().map(Trade::toString).collect(Collectors.toList());
            return new OrderResponse(order.getId(), order.getType().name(), order.getProduce(),
                    order.getPrice(), order.getOriginalQuantity(), order.getStatus().name(), tradeDescriptions);
        } catch (SQLException e) {
            throw new IllegalStateException("Database error while processing order " + order.getId(), e);
        }
    }

    private void persistCounterpart(String orderId) throws SQLException {
        Order order = engine.getOrder(orderId);
        if (order != null) {
            orderDao.updateRemainingQuantity(order);
        }
    }
}
