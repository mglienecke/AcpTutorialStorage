package uk.ac.ed.inf.acpTutorial.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import uk.ac.ed.inf.acpTutorial.configuration.SqsConfiguration;
import uk.ac.ed.inf.acpTutorial.configuration.SystemConfiguration;

import java.net.URI;
import java.util.List;

@Slf4j
@Service
public class KafkaService {

    private final SqsConfiguration sqsConfiguration;
    private final SystemConfiguration systemConfiguration;

    public KafkaService(SqsConfiguration sqsConfiguration, SystemConfiguration systemConfiguration) {
        this.sqsConfiguration = sqsConfiguration;
        this.systemConfiguration = systemConfiguration;
    }

    public List<String> listQueues() {
        return getSqsClient().listQueues().queueUrls();
    }

    public String createQueue(String queueName) {
        CreateQueueResponse response = getSqsClient().createQueue(CreateQueueRequest.builder()
                .queueName(queueName)
                .build());
        return response.queueUrl();
    }

    public void deleteQueue(String queueUrl) {
        getSqsClient().deleteQueue(DeleteQueueRequest.builder()
                .queueUrl(queueUrl)
                .build());
    }

    public void sendMessage(String queueUrl, String messageBody) {
        getSqsClient().sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody)
                .build());
    }

    public List<Message> receiveMessages(String queueUrl) {
        ReceiveMessageResponse response = getSqsClient().receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .build());

        for (Message msg : response.messages()) {
            getSqsClient().deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(msg.receiptHandle())
                    .build());
        }

        return response.messages();
    }

    private SqsClient getSqsClient() {
        return SqsClient.builder()
                .endpointOverride(URI.create(sqsConfiguration.getSqsEndpoint()))
                .region(systemConfiguration.getAwsRegion())
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(systemConfiguration.getAwsUser(), systemConfiguration.getAwsSecret())))
                .build();
    }
}
