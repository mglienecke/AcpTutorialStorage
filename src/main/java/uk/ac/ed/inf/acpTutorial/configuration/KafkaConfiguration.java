package uk.ac.ed.inf.acpTutorial.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "kafka")
public class KafkaConfiguration {

    private String bootstrapServers;
    private Producer producer = new Producer();
    private Security security = new Security();

    @Data
    public static class Producer {
        private String clientId;
        private String acks;
        private int retries;
        private int batchSize;
        private int lingerMs;
        private long bufferMemory;
        private String keySerializer;
        private String valueSerializer;
    }

    @Data
    public static class Security {
        private String protocol;
    }
}