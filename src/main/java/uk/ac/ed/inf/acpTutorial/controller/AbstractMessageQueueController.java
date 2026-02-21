package uk.ac.ed.inf.acpTutorial.controller;

import jakarta.jms.JMSException;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.inf.acpTutorial.service.MessageQueueService;

import java.util.List;

public abstract class AbstractMessageQueueController {

    protected final MessageQueueService messageQueueService;

    protected AbstractMessageQueueController(MessageQueueService messageQueueService) {
        this.messageQueueService = messageQueueService;
    }

    @GetMapping("/queues")
    public List<String> listQueues() {
        return messageQueueService.listQueues();
    }

    @PutMapping("/create-queue/{queueName}")
    public String createQueue(@PathVariable String queueName) {
        return messageQueueService.createQueue(queueName);
    }

    @DeleteMapping("/delete-queue")
    public void deleteQueue(@RequestParam String queueUrl) {
        messageQueueService.deleteQueue(queueUrl);
    }

    @PostMapping("/send-message/{queueUrl}")
    public void sendMessage(@PathVariable String queueUrl, @RequestBody String messageBody) {
        messageQueueService.sendMessage(queueUrl, messageBody);
    }

    @GetMapping("/receive-messages/{queueUrl}")
    public List<String> receiveMessages(@PathVariable String queueUrl) {
        return messageQueueService.receiveMessages(queueUrl);
    }

    @PostMapping("/jms/send-message")
    public void sendMessageJms(@RequestParam String queueName, @RequestBody String messageBody) {
        messageQueueService.sendMessageJms(queueName, messageBody);
    }

    @GetMapping("/jms/receive-message")
    public String receiveMessageJms(@RequestParam String queueName) throws JMSException {
        return messageQueueService.receiveMessageJms(queueName);
    }
}
