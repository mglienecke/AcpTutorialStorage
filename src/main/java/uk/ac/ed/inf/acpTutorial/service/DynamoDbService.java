package uk.ac.ed.inf.acpTutorial.service;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import uk.ac.ed.inf.acpTutorial.configuration.DynamoDbConfiguration;
import uk.ac.ed.inf.acpTutorial.configuration.SystemConfiguration;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DynamoDbService {

    private final DynamoDbConfiguration dynamoDbConfiguration;
    private final SystemConfiguration systemConfiguration;
    private static final String KEY_COLUMN_NAME = "key";

    public DynamoDbService(DynamoDbConfiguration dynamoDbConfiguration, SystemConfiguration systemConfiguration) {
        this.dynamoDbConfiguration = dynamoDbConfiguration;
        this.systemConfiguration = systemConfiguration;
    }

    public List<String> listTables() {
        return getDynamoDbClient().listTables().tableNames();
    }

    public String getTableObject(@PathVariable String table, @PathVariable String key) {
        GetItemResponse response = getDynamoDbClient().getItem(GetItemRequest.builder()
                .tableName(table)
                .key(java.util.Map.of(KEY_COLUMN_NAME, software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(key).build()))
                .build());

        if (response.hasItem()) {
            return "{ \"key\": \"" + response.item().get("key").s() + " \", \"content\": \"" + response.item().get("content").s() + "\" } ";
        } else {
            throw new RuntimeException("Object with key " + key + " not found in table " + table);
        }
    }

    public List<String> listTableObjects(@PathVariable String table) {
        return getDynamoDbClient()
                .scanPaginator(ScanRequest.builder()
                        .tableName(table)
                        .build())
                .items()
                .stream()
                .map(e ->
                    "{ \"key\": \"" + e.get("key").s() + " \", \"content\": \"" + e.get("content").s() + "\" } "
                )
                .toList();
    }

    public void createTable(@PathVariable String table) {
        getDynamoDbClient().createTable(b -> b.tableName(table)
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeName(KEY_COLUMN_NAME)
                        .attributeType(ScalarAttributeType.S)
                        .build())
                .keySchema(KeySchemaElement.builder()
                        .attributeName(KEY_COLUMN_NAME)
                        .keyType(KeyType.HASH)
                        .build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(5L)
                        .writeCapacityUnits(5L)
                        .build())
        );
    }

    public void createObject(String table, String key, String objectContent) {
        getDynamoDbClient().putItem(b -> b.tableName(table).item(
                java.util.Map.of("key", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(key).build(),
                        "content", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(objectContent).build())
        ));
    }


    public void createObject(String table, String key, @RequestBody Map<String, String> attributesMap) {
        Map<String, AttributeValue> values = new HashMap<>();
        values.put("key", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(key).build());

        values.putAll(attributesMap.entrySet().stream().map(e ->
                java.util.Map.entry(e.getKey(), software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(e.getValue()).build())
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        getDynamoDbClient().putItem(b -> b.tableName(table).item(values));

    }

    public String getTablePrimaryKey(
            @Parameter(name = "table", description = "The name of the DynamoDB table")
            @PathVariable(required = true)
            String table) {

        DescribeTableRequest request = DescribeTableRequest.builder()
                .tableName(table)
                .build();

        DescribeTableResponse response = getDynamoDbClient().describeTable(request);

        return response.table().keySchema().stream()
                .filter(k -> k.keyType().toString().equals("HASH"))
                .map(KeySchemaElement::attributeName)
                .findFirst()
                .orElseThrow();
    }



    private DynamoDbClient getDynamoDbClient() {
        return DynamoDbClient.builder()
                .endpointOverride(URI.create(dynamoDbConfiguration.getDynamoDbEndpoint()))
                .region(systemConfiguration.getAwsRegion())
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(systemConfiguration.getAwsUser(), systemConfiguration.getAwsSecret())))
                .build();
    }

    public void saveMessageToDynamoDb(String sqsTableInDynamoDb, String key, String message) {
        if (! getDynamoDbClient().listTables().tableNames().contains(sqsTableInDynamoDb)) {
            createTable(sqsTableInDynamoDb);
        }
        createObject(sqsTableInDynamoDb, key, message);
    }
}
