package repository;

import domain.dto.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

public class OrderRepository {

  // Metodo principal que será chamado quando a mensagem do Gcp chegar
  public void saveOrderData(OrderPayloadDTO payload) throws SQLException {

    try (Connection conn = DatabaseConnection.getConnection()) {

      // FASE 1: Dados de Referência (Rodam com AutoCommit = TRUE).
      // Os locks são liberados instantaneamente linha a linha! Fim do Deadlock.
      insertCustomer(conn, payload.customer());
      insertSeller(conn, payload.seller());
      for (ItemDTO item : payload.items()) {
        insertCategoryAndProduct(conn, item);
      }

      // FASE 2: Transação Estrita do Pedido (AutoCommit = FALSE)
      // Aqui sse falhar um item ou o envio, o pedido inteiro da o rollback.
      conn.setAutoCommit(false);
      try {
        insertOrder(conn, payload);
        for (ItemDTO item : payload.items()) {
          insertOrderItem(conn, payload.uuid(), item);
        }
        insertShipment(conn, payload.uuid(), payload.shipment());
        insertPayment(conn, payload.uuid(), payload.payment());
        insertMetadata(conn, payload.uuid(), payload.metadata());

        conn.commit();
        System.out.println("✅ Pedido " + payload.uuid() + " salvo com sucesso no Supabase!");
      } catch (SQLException e) {
        conn.rollback();
        System.err.println("❌ Erro ao salvar o pedido. Rollback executado.");
        throw e;
      } finally {
        conn.setAutoCommit(true);
      }
    }
  }

  // PreparedStatement =  Query parametrizada pra evitar sql injection
  private void insertCustomer(Connection conn, CustomerDTO customer) throws SQLException {
    String sql = "INSERT INTO cliente (id, nome, email, documento) VALUES (?, ?, ?, ?) ON CONFLICT (id) DO NOTHING";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, customer.id());
      stmt.setString(2, customer.name());
      stmt.setString(3, customer.email());
      stmt.setString(4, customer.document());
      stmt.executeUpdate();
    }
  }

  private void insertSeller(Connection conn, SellerDTO seller) throws SQLException {
    String sql = "INSERT INTO seller (id, nome, cidade, estado) VALUES (?, ?, ?, ?) ON CONFLICT (id) DO NOTHING";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, seller.id());
      stmt.setString(2, seller.name());
      stmt.setString(3, seller.city());
      stmt.setString(4, seller.state());
      stmt.executeUpdate();
    }
  }

  private void insertCategoryAndProduct(Connection conn, ItemDTO item) throws SQLException {
    String sqlCat = "INSERT INTO categoria (id, nome) VALUES (?, ?) ON CONFLICT (id) DO NOTHING";
    try (PreparedStatement stmt = conn.prepareStatement(sqlCat)) {
      stmt.setString(1, item.category().id());
      stmt.setString(2, item.category().name());
      stmt.executeUpdate();
    }

    String sqlSub = "INSERT INTO subcategoria (id, nome, categoria_id) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING";
    try (PreparedStatement stmt = conn.prepareStatement(sqlSub)) {
      stmt.setString(1, item.category().subCategory().id());
      stmt.setString(2, item.category().subCategory().name());
      stmt.setString(3, item.category().id());
      stmt.executeUpdate();
    }

    String sqlProd = "INSERT INTO produto (id, nome, subcategoria_id) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING";
    try (PreparedStatement stmt = conn.prepareStatement(sqlProd)) {
      stmt.setInt(1, item.productId());
      stmt.setString(2, item.productName());
      stmt.setString(3, item.category().subCategory().id());
      stmt.executeUpdate();
    }
  }

  private void insertOrder(Connection conn, OrderPayloadDTO payload) throws SQLException {
    String sql = "INSERT INTO pedido (uuid, cliente_id, seller_id, created_at, channel, status) VALUES (?, ?, ?, ?, ?, ?)";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, UUID.fromString(payload.uuid()));
      stmt.setInt(2, payload.customer().id());
      stmt.setInt(3, payload.seller().id());
      stmt.setTimestamp(4, Timestamp.from(java.time.Instant.parse(payload.createdAt())));
      stmt.setString(5, payload.channel());
      stmt.setString(6, payload.status());
      stmt.executeUpdate();
    }
  }

  private void insertOrderItem(Connection conn, String orderUuid, ItemDTO item) throws SQLException {
    String sql = "INSERT INTO item_pedido (id, pedido_uuid, produto_id, unit_price, quantity) VALUES (?, ?, ?, ?, ?)";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, item.id());
      stmt.setObject(2, UUID.fromString(orderUuid));
      stmt.setInt(3, item.productId());
      stmt.setDouble(4, item.unitPrice());
      stmt.setInt(5, item.quantity());
      stmt.executeUpdate();
    }
  }

  private void insertShipment(Connection conn, String orderUuid, ShipmentDTO shipment) throws SQLException {
    String sql = "INSERT INTO envio (pedido_uuid, carrier, service, status, tracking_code) VALUES (?, ?, ?, ?, ?)";
    try(PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, UUID.fromString(orderUuid));
      stmt.setString(2, shipment.carrier());
      stmt.setString(3, shipment.service());
      stmt.setString(4, shipment.status());
      stmt.setString(5, shipment.trackingCode());
      stmt.executeUpdate();
    }
  }

  private void insertPayment(Connection conn, String orderUuid, PaymentDTO payment) throws SQLException {
    String sql = "INSERT INTO pagamento (pedido_uuid, method, status, transaction_id) VALUES (?, ?, ?, ?)";
    try(PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, UUID.fromString(orderUuid));
      stmt.setString(2, payment.method());
      stmt.setString(3, payment.status());
      stmt.setString(4, payment.transactionId());
      stmt.executeUpdate();
    }
  }

  private void insertMetadata(Connection conn, String orderUuid, MetadataDTO metadata) throws SQLException {
    String sql = "INSERT INTO metadata (pedido_uuid, source, user_agent, ip_address) VALUES (?, ?, ?, ?)";
    try(PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, UUID.fromString(orderUuid));
      stmt.setString(2, metadata.source());
      stmt.setString(3, metadata.userAgent());
      stmt.setString(4, metadata.ipAddress());
      stmt.executeUpdate();
    }
  }
}
