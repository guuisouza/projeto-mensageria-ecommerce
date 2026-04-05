package service;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import domain.dto.OrderPayloadDTO;
import repository.OrderRepository;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PubSubConsumerService {
  private final Gson gson = new Gson();
  private final OrderRepository repository = new OrderRepository();

  public void startListening(String projectId, String subscriptionId) {
    ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId);

    MessageReceiver receiver = (PubsubMessage message, AckReplyConsumer consumer) -> {
      System.out.println("\n--- Nova Mensagem Capturada ---");
      System.out.println("ID da Mensagem: " + message.getMessageId());

      String jsonPayload = message.getData().toStringUtf8();

      try {
        OrderPayloadDTO order = gson.fromJson(jsonPayload, OrderPayloadDTO.class);

        if (order == null || order.uuid() == null) {
          throw new JsonSyntaxException("O JSON não possui a estrutura de um Pedido (UUID ausente).");
        }

        repository.saveOrderData(order);

        // Ack só em caso de sucesso, se der alguma exception reenvia a fila a mensagfem
         consumer.ack();

        System.out.println("✅ Processamento concluído! (ACK retornado ao GCP)");
      } catch (JsonSyntaxException e) {
        System.out.println("⚠ Mensagem ignorada (Lixo / Formato Inválido). Limpando da fila...");
        consumer.ack();
      } catch (SQLException e) {
        System.out.println("❌ Erro ao salvar no banco de dados. A mensagem retornará para a fila.");
        e.printStackTrace();
      } catch (Exception e) {
        System.out.println("❌ Erro inesperado: " + e.getMessage());
      }
    };

    Subscriber subscriber = null;
    try {
      subscriber = Subscriber.newBuilder(subscriptionName, receiver).build();
      subscriber.startAsync().awaitRunning();

      System.out.printf("🎧 Escutando mensagens na assinatura: %s...\n", subscriptionName.toString());

      subscriber.awaitTerminated(1, TimeUnit.HOURS);
    } catch (TimeoutException e) {
      if (subscriber != null) {
        subscriber.stopAsync();
      }
    }
  }
}
