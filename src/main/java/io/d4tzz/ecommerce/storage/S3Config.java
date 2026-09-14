package io.d4tzz.ecommerce.storage;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

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
}
