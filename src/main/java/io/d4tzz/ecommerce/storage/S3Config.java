package io.d4tzz.ecommerce.storage;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    private static final Logger log = LoggerFactory.getLogger(S3Config.class);

    private final StorageProperties properties;

    public S3Config(StorageProperties properties) {
        this.properties = properties;
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
            .region(Region.of(properties.region()))
            .endpointOverride(URI.create(properties.endpoint()))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.accessKey().trim(), properties.secretKey().trim())
                )
            )
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            .region(Region.of(properties.region()))
            .endpointOverride(URI.create(properties.endpoint()))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.accessKey().trim(), properties.secretKey().trim())
                )
            )
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();
    }

    @Bean
    public ApplicationRunner s3BucketInitializer(S3Client s3Client) {
        return args -> {
            String bucket = properties.bucket();
            try {
                s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
                log.info("S3 Bucket '{}' already exists.", bucket);
            } catch (NoSuchBucketException e) {
                log.info("S3 Bucket '{}' not found. Creating it...", bucket);
                s3Client.createBucket(b -> b.bucket(bucket));
                log.info("S3 Bucket '{}' created successfully.", bucket);
            } catch (S3Exception e) {
                if (e.statusCode() == 404) {
                    log.info("S3 Bucket '{}' not found (status 404). Creating it...", bucket);
                    s3Client.createBucket(b -> b.bucket(bucket));
                    log.info("S3 Bucket '{}' created successfully.", bucket);
                } else {
                    log.error("S3 error while checking bucket '{}': {}", bucket, e.getMessage(), e);
                }
            } catch (Exception e) {
                log.error("Unexpected error initializing S3 bucket '{}': {}", bucket, e.getMessage(), e);
            }
        };
    }
}

