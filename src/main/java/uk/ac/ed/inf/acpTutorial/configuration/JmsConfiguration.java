package uk.ac.ed.inf.acpTutorial.configuration;

import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.rabbitmq.jms.admin.RMQConnectionFactory;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Session;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

@Configuration
@EnableJms
public class JmsConfiguration {

    private final SqsConfiguration sqsConfiguration;
    private final RabbitMqConfiguration rabbitMqConfiguration;
    private final SystemConfiguration systemConfiguration;

    public JmsConfiguration(SqsConfiguration sqsConfiguration, RabbitMqConfiguration rabbitMqConfiguration, SystemConfiguration systemConfiguration) {
        this.sqsConfiguration = sqsConfiguration;
        this.rabbitMqConfiguration = rabbitMqConfiguration;
        this.systemConfiguration = systemConfiguration;
    }

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .endpointOverride(URI.create(sqsConfiguration.getSqsEndpoint()))
                .region(systemConfiguration.getAwsRegion())
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(systemConfiguration.getAwsUser(), systemConfiguration.getAwsSecret())))
                .build();
    }

    @Bean
    public ConnectionFactory sqsConnectionFactory() {
        return new SQSConnectionFactory(
                new ProviderConfiguration(),
                sqsClient()
        );
    }

    @Bean
    public ConnectionFactory rabbitMqConnectionFactory() {
        RMQConnectionFactory factory = new RMQConnectionFactory();
        factory.setHost(rabbitMqConfiguration.getHost());
        factory.setPort(rabbitMqConfiguration.getPort());
        factory.setUsername(rabbitMqConfiguration.getUsername());
        factory.setPassword(rabbitMqConfiguration.getPassword());
        return factory;
    }

    @Bean(name = "sqsJmsTemplate")
    @Primary
    public JmsTemplate sqsJmsTemplate() {
        JmsTemplate jmsTemplate = new JmsTemplate(sqsConnectionFactory());
        jmsTemplate.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
        return jmsTemplate;
    }

    @Bean(name = "rabbitMqJmsTemplate")
    public JmsTemplate rabbitMqJmsTemplate() {
        JmsTemplate jmsTemplate = new JmsTemplate(rabbitMqConnectionFactory());
        return jmsTemplate;
    }

    public JmsTemplate getJmsTemplate(String provider) {
        if ("sqs".equalsIgnoreCase(provider)) {
            return sqsJmsTemplate();
        } else if ("rabbitmq".equalsIgnoreCase(provider)) {
            return rabbitMqJmsTemplate();
        }
        throw new IllegalArgumentException("Unknown JMS provider: " + provider);
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(sqsConnectionFactory());
        factory.setDestinationResolver(new DynamicDestinationResolver());
        factory.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
        return factory;
    }


}
