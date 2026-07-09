package com.marketmaker.db;

import com.marketmaker.model.Order;
import com.marketmaker.model.OrderType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Plain-JDBC data access object for the {@code orders} table.
 * See {@code src/main/resources/schema.sql} for the table definition.
 */
public class OrderDao {

    /**
     * Wipes every row from both {@code trades} and {@code orders} — used by
     * the full-reset API ({@code DELETE /api/orders}), so testing doesn't
     * require manually running SQL. {@code trades} has a foreign key onto
     * {@code orders}, so MySQL normally refuses to truncate {@code orders}
     * while any row anywhere still references it; foreign key checks are
     * disabled just for this one operation and switched back on immediately
     * after.
     */
    public void deleteAllOrdersAndTrades() throws SQLException {
        try (Connection conn = DbConnectionManager.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS = 0");
            st.execute("TRUNCATE TABLE trades");
            st.execute("TRUNCATE TABLE orders");
            st.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    /** Inserts a brand-new order row. */
    public void insert(Order order) throws SQLException {
        String sql = "INSERT INTO orders " +
                "(id, order_type, produce, price, original_quantity, remaining_quantity, order_time, sequence_no, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, order.getId());
            ps.setString(2, order.getType().name());
            ps.setString(3, order.getProduce());
            ps.setBigDecimal(4, order.getPrice());
            ps.setInt(5, order.getOriginalQuantity());
            ps.setInt(6, order.getRemainingQuantity());
            ps.setTimestamp(7, Timestamp.valueOf(order.getOrderTime()));
            ps.setLong(8, order.getSequence());
            ps.setString(9, order.getStatus().name());
            ps.executeUpdate();
        }
    }

    /** Updates the remaining quantity/status of an order that has just been (partially) matched. */
    public void updateRemainingQuantity(Order order) throws SQLException {
        String sql = "UPDATE orders SET remaining_quantity = ?, status = ? WHERE id = ?";
        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, order.getRemainingQuantity());
            ps.setString(2, order.getStatus().name());
            ps.setString(3, order.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Updates every mutable field of an order: price, original/remaining
     * quantity, and status. Used after a PUT edit (which may change price
     * and/or quantity outright) has been re-matched, and after a DELETE
     * cancellation.
     */
    public void updateOrderDetails(Order order) throws SQLException {
        String sql = "UPDATE orders SET price = ?, original_quantity = ?, remaining_quantity = ?, status = ? WHERE id = ?";
        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, order.getPrice());
            ps.setInt(2, order.getOriginalQuantity());
            ps.setInt(3, order.getRemainingQuantity());
            ps.setString(4, order.getStatus().name());
            ps.setString(5, order.getId());
            ps.executeUpdate();
        }
    }

    /** Loads every order that is not yet fully matched (remaining_quantity > 0), oldest first. */
    public List<Order> loadAllOpenOrders() throws SQLException {
        String sql = "SELECT * FROM orders WHERE remaining_quantity > 0 ORDER BY sequence_no ASC";
        List<Order> orders = new ArrayList<>();
        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                orders.add(mapRow(rs));
            }
        }
        return orders;
    }

    /** Finds a single order (any status) by its id. Returns null if not found. */
    public Order findById(String orderId) throws SQLException {
        String sql = "SELECT * FROM orders WHERE id = ?";
        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** Returns the highest sequence number currently stored, or -1 if the table is empty. */
    public long getMaxSequence() throws SQLException {
        String sql = "SELECT MAX(sequence_no) AS max_seq FROM orders";
        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                long max = rs.getLong("max_seq");
                return rs.wasNull() ? -1 : max;
            }
            return -1;
        }
    }

    /**
     * Returns the highest numeric suffix used so far for a given id prefix
     * (e.g. "s" -> looks at s1, s2, s10... and returns 10). Used to
     * auto-generate the next order id (Part 4). Returns 0 if none exist yet.
     */
    public int getMaxIdSuffix(String prefix) throws SQLException {
        String sql = "SELECT id FROM orders WHERE order_type = ?";
        String type = prefix.equalsIgnoreCase("s") ? OrderType.SUPPLY.name() : OrderType.DEMAND.name();
        int max = 0;
        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String digits = id.replaceAll("(?i)^" + prefix, "");
                    try {
                        max = Math.max(max, Integer.parseInt(digits));
                    } catch (NumberFormatException ignored) {
                        // id didn't follow the "<prefix><number>" convention - skip it
                    }
                }
            }
        }
        return max;
    }

    private Order mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        OrderType type = OrderType.valueOf(rs.getString("order_type"));
        String produce = rs.getString("produce");
        BigDecimal price = rs.getBigDecimal("price");
        int originalQuantity = rs.getInt("original_quantity");
        int remainingQuantity = rs.getInt("remaining_quantity");
        LocalDateTime orderTime = rs.getTimestamp("order_time").toLocalDateTime();
        long sequence = rs.getLong("sequence_no");
        String status = rs.getString("status");
        return Order.rehydrate(id, type, produce, price, originalQuantity, remainingQuantity, orderTime, sequence, status);
    }
}
