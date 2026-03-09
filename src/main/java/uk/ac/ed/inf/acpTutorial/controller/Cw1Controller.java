package uk.ac.ed.inf.acpTutorial.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.S3Object;
import uk.ac.ed.inf.acpTutorial.configuration.DynamoDbConfiguration;
import uk.ac.ed.inf.acpTutorial.configuration.S3Configuration;
import uk.ac.ed.inf.acpTutorial.configuration.SystemEnvironment;
import uk.ac.ed.inf.acpTutorial.dto.DroneFromService;
import uk.ac.ed.inf.acpTutorial.dto.ProcessBodyData;
import uk.ac.ed.inf.acpTutorial.service.DynamoDbService;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController()
@RequestMapping("/api/v1/acp")
public class Cw1Controller {

    private final S3Configuration s3Configuration;
    private final SystemEnvironment systemEnvironment;
    private final DynamoDbConfiguration dynamoDbConfiguration;
    private final DynamoDbService dynamoDbService;

    public Cw1Controller(S3Configuration s3Configuration, SystemEnvironment systemEnvironment, DynamoDbConfiguration dynamoDbConfiguration, DynamoDbService dynamoDbService) {
        this.s3Configuration = s3Configuration;
        this.systemEnvironment = systemEnvironment;
        this.dynamoDbConfiguration = dynamoDbConfiguration;
        this.dynamoDbService = dynamoDbService;
    }


    @GetMapping("/endpoint")
    public String getS3Endpoint() {
        return s3Configuration.getS3Endpoint();
    }

    @GetMapping("/buckets")
    public List<String> listBuckets() {
        return getS3Client().listBuckets().buckets().stream().map(Bucket::name).toList();
    }

    @GetMapping("/list-objects/{bucket}")
    public List<String> listBucketObjects(@PathVariable String bucket) {
        return getS3Client().listObjectsV2(b -> b.bucket(bucket)).contents().stream().map(S3Object::key).toList();
    }


    @PutMapping("/create-bucket/{bucket}")
    public void createBucket(@PathVariable String bucket) {
        getS3Client().createBucket(b -> b.bucket(bucket));
    }

    @PutMapping("/create-object/{bucket}/{s3Object}")
    public void createBucket(@PathVariable String bucket, @PathVariable String s3Object, @RequestBody String objectContent) {
        getS3Client().putObject(b -> b.bucket(bucket).key(s3Object), software.amazon.awssdk.core.sync.RequestBody.fromString(objectContent));
    }

    private S3Client getS3Client() {
        return S3Client.builder()
                    .endpointOverride(URI.create(s3Configuration.getS3Endpoint()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(systemEnvironment.getAwsUser(), systemEnvironment.getAwsSecret())))
                    .region(systemEnvironment.getAwsRegion())
                    .build();
    }

    /*
     * CW1
     */

    @GetMapping("/all/s3/{bucket}")
    public List<String> listBucketObjectContents(@PathVariable String bucket) {
        S3Client s3Client = getS3Client();
        return s3Client.listObjectsV2(b -> b.bucket(bucket)).contents().stream()
                .map(
                        obj -> handleBytesToString(s3Client.getObjectAsBytes(req -> req.bucket(bucket).key(obj.key())).asByteArray())
                ).toList();
    }

    @GetMapping("/single/s3/{bucket}/{key}")
    public String listBucketObjectContents(@PathVariable String bucket, @PathVariable String key) {
        S3Client s3Client = getS3Client();
        return "\"" + handleBytesToString(s3Client.getObjectAsBytes(req -> req.bucket(bucket).key(key)).asByteArray()) + "\"" ;
    }

    private String handleBytesToString(byte[] bytes) {
        try {
            return new String(bytes);
        } catch (Exception e) {
            // Fall through to byte array representation
        }
        return Arrays.toString(bytes);
    }

    @GetMapping("/all/dynamo/{table}")
    public List<Map<String, String>> listTableObjectContents(@PathVariable String table) {
        return dynamoDbService.listTableObjectsDirectly(table);
    }

    @GetMapping("/single/dynamo/{table}/{key}")
    public Map<String, String> listTableObjectForKeyContents(@PathVariable String table, @PathVariable String key) {
        return dynamoDbService.listTableObjectsForKeyDirectly(table, key);
    }

    @PostMapping("/process/dump")
    public List<DroneFromService> readDronesAndDump(@RequestBody ProcessBodyData bodyData) {
        try (var serviceClient = HttpClient.newHttpClient()){
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(bodyData.urlPath()))
                    .GET()
                    .build();
            var response = serviceClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to read drones and dump, status code: " + response.statusCode());
            }

            var json = response.body();
            var drones = new ObjectMapper().readValue(json, DroneFromService[].class);
            var result = new ArrayList<DroneFromService>();

            for (var drone : drones) {
                result.add(getDroneFromService(drone));
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read drones and dump", e);
        }
    }

    private static @NonNull DroneFromService getDroneFromService(DroneFromService drone) {
        BigDecimal costInitial = drone.capability().costInitial() == null ? BigDecimal.ZERO : drone.capability().costInitial();
        BigDecimal costFinal = drone.capability().costFinal() == null ? BigDecimal.ZERO : drone.capability().costFinal();
        BigDecimal costPerMove = drone.capability().costPerMove() == null ? BigDecimal.ZERO : drone.capability().costPerMove();

        return new DroneFromService(drone.id(), drone.name(),
                costInitial.add(costFinal).add(costPerMove.multiply(BigDecimal.valueOf(100))), drone.capability());
    }


    @PostMapping("/process/dynamo")
    public void readDronesAndWriteDynamoDb(@RequestBody ProcessBodyData bodyData) {
        try (var serviceClient = HttpClient.newHttpClient()){
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(bodyData.urlPath()))
                    .GET()
                    .build();
            var response = serviceClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to read drones and dump, status code: " + response.statusCode());
            }

            var json = response.body();
            var drones = new ObjectMapper().readValue(json, DroneFromService[].class);
            var result = new ArrayList<DroneFromService>();

            for (var drone : drones) {
                result.add(getDroneFromService(drone));
            }

            dynamoDbService.createTable("s9999999");
        } catch (Exception e) {
            throw new RuntimeException("Failed to read drones and dump", e);
        }
    }

}
