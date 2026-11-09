package com.duoc.storageservice;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class S3Config {

    @Bean
    S3Client s3Client(
            @Value("${aws.region:us-east-1}") String region,
            @Value("${aws.s3.endpoint:}") String endpoint) {

        var builder = S3Client.builder().region(Region.of(region));

        // Solo si se define un endpoint (LocalStack, MinIO, etc.)
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build());
        }

        // Sin credenciales explícitas: el SDK usa DefaultCredentialsProvider
        // (en EC2 toma el rol de IAM de la instancia automáticamente)
        return builder.build();
    }
}