package uk.ac.ed.inf.acpTutorial.service;

import java.util.List;

public interface MessageQueueService {
    List<String> listQueues();
    String createQueue(String queueName);
    void deleteQueue(String queueNameOrUrl);
    void sendMessage(String queueNameOrUrl, String messageBody);
    List<String> receiveMessages(String queueNameOrUrl);
    void sendMessageJms(String queueName, String messageBody);
    String receiveMessageJms(String queueName);
}
