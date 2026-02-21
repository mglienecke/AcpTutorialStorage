package uk.ac.ed.inf.acpTutorial.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMqConfiguration {
    private String host;
    private int port;
    private String username;
    private String password;
    private String topic;
    // Management API settings (defaults align with standard RabbitMQ setup)
    private int managementPort = 15672;
    private String managementScheme = "http";
}
