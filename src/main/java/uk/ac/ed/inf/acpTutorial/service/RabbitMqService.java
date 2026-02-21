package uk.ac.ed.inf.acpTutorial.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.ac.ed.inf.acpTutorial.configuration.JmsConfiguration;
import uk.ac.ed.inf.acpTutorial.configuration.RabbitMqConfiguration;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class RabbitMqService implements MessageQueueService {

    private final JmsConfiguration jmsConfiguration;
    private final RabbitMqConfiguration rabbitMqConfiguration;
    private Connection persistedConnection;
    private Channel persistedChannel;

    public RabbitMqService(JmsConfiguration jmsConfiguration, RabbitMqConfiguration rabbitMqConfiguration) {
        this.jmsConfiguration = jmsConfiguration;
        this.rabbitMqConfiguration = rabbitMqConfiguration;
    }

    @Override
    @SneakyThrows
    public List<String> listQueues() {
        String scheme = rabbitMqConfiguration.getManagementScheme();
        String host = rabbitMqConfiguration.getHost();
        int mgmtPort = rabbitMqConfiguration.getManagementPort();
        String username = rabbitMqConfiguration.getUsername();
        String password = rabbitMqConfiguration.getPassword();

        String url = String.format("%s://%s:%d/api/queues", scheme, host, mgmtPort);

        HttpClient client = HttpClient.newHttpClient();
        String basicAuth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Basic " + basicAuth)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("RabbitMQ Management API returned non-2xx status: {} - body: {}", response.statusCode(), response.body());
            return List.of();
        }

        JsonArray arr = JsonParser.parseString(response.body()).getAsJsonArray();
        List<String> names = new ArrayList<>();
        for (JsonElement el : arr) {
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("name")) {
                names.add(obj.get("name").getAsString());
            }
        }
        return names;
    }

    @Override
    @SneakyThrows
    public String createQueue(String queueName) {
        Channel channel = getPersistedChannel();
        channel.queueDeclare(queueName, true, false, false, null);
        return queueName;
    }

    @Override
    @SneakyThrows
    public void deleteQueue(String queueNameOrUrl) {
        Channel channel = getPersistedChannel();
        channel.queueDelete(queueNameOrUrl);
    }

    @Override
    @SneakyThrows
    public void sendMessage(String queueNameOrUrl, String messageBody) {
        Channel channel = getPersistedChannel();
        channel.queueDeclare(queueNameOrUrl, true, false, false, null);
        channel.basicPublish("", queueNameOrUrl, null, messageBody.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    @SneakyThrows
    public List<String> receiveMessages(String queueNameOrUrl) {
        List<String> messages = new ArrayList<>();
        Channel channel = getPersistedChannel();
        channel.queueDeclare(queueNameOrUrl, true, false, false, null);
        for (int i = 0; i < 10; i++) {
            GetResponse response = channel.basicGet(queueNameOrUrl, true);
            if (response != null) {
                messages.add(new String(response.getBody(), StandardCharsets.UTF_8));
            } else {
                break;
            }
        }
        return messages;
    }

    @Override
    public void sendMessageJms(String queueName, String messageBody) {
        jmsConfiguration.getJmsTemplate("rabbitmq").convertAndSend(queueName, messageBody);
    }

    @Override
    public String receiveMessageJms(String queueName) {
        Object message = jmsConfiguration.getJmsTemplate("rabbitmq").receiveAndConvert(queueName);
        return message != null ? message.toString() : null;
    }

    private synchronized Channel getPersistedChannel() throws IOException, TimeoutException {
        if (persistedConnection == null || !persistedConnection.isOpen()) {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(rabbitMqConfiguration.getHost());
            factory.setPort(rabbitMqConfiguration.getPort());
            factory.setUsername(rabbitMqConfiguration.getUsername());
            factory.setPassword(rabbitMqConfiguration.getPassword());
            persistedConnection = factory.newConnection();
            persistedChannel = null;
        }
        if (persistedChannel == null || !persistedChannel.isOpen()) {
            persistedChannel = persistedConnection.createChannel();
        }
        return persistedChannel;
    }

    @PreDestroy
    @SneakyThrows
    public void cleanup() {
        if (persistedChannel != null && persistedChannel.isOpen()) {
            persistedChannel.close();
        }
        if (persistedConnection != null && persistedConnection.isOpen()) {
            persistedConnection.close();
        }
    }
}
