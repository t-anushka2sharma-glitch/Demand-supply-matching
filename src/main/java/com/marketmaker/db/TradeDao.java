package com.marketmaker.db;

import com.marketmaker.model.Trade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Plain-JDBC data access object for the {@code trades} table.
 */
public class TradeDao {

    public void insert(Trade trade) throws SQLException {
        String sql = "INSERT INTO trades (demand_order_id, supply_order_id, price, quantity, trade_time) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trade.getDemandOrderId());
            ps.setString(2, trade.getSupplyOrderId());
            ps.setBigDecimal(3, trade.getPrice());
            ps.setInt(4, trade.getQuantity());
            ps.setTimestamp(5, Timestamp.valueOf(trade.getTradeTime()));
            ps.executeUpdate();
        }
    }

    /** Returns every trade involving the given order id, on either side of the trade. */
    public List<Trade> findByOrderId(String orderId) throws SQLException {
        String sql = "SELECT * FROM trades WHERE demand_order_id = ? OR supply_order_id = ? ORDER BY trade_time ASC";
        List<Trade> trades = new ArrayList<>();
        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            ps.setString(2, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    trades.add(new Trade(
                            rs.getString("demand_order_id"),
                            rs.getString("supply_order_id"),
                            rs.getBigDecimal("price"),
                            rs.getInt("quantity"),
                            rs.getTimestamp("trade_time").toLocalDateTime()));
                }
            }
        }
        return trades;
    }
}
