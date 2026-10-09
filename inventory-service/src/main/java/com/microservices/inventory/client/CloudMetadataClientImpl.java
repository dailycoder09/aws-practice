package com.microservices.inventory.client;

import com.microservices.inventory.dto.response.CloudMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/**
 * Reads EC2 instance details from the instance metadata service (IMDSv2: a short-lived token first,
 * then one request per field).
 *
 * Only a fixed list of harmless fields is read. The metadata service also serves IAM credentials;
 * this class must never request those paths.
 *
 * Fails fast (short timeout) and never throws, so running outside AWS just means "no cloud details".
 */
@Component
@Slf4j
public class CloudMetadataClientImpl implements CloudMetadataClient {

    private static final String TOKEN_HEADER = "X-aws-ec2-metadata-token";
    private static final String TOKEN_TTL_HEADER = "X-aws-ec2-metadata-token-ttl-seconds";

    private final RestClient restClient;
    private final boolean enabled;

    @Autowired
    public CloudMetadataClientImpl(
            @Value("${server-info.cloud-metadata.url:http://169.254.169.254}") String baseUrl,
            @Value("${server-info.cloud-metadata.enabled:true}") boolean enabled,
            @Value("${server-info.cloud-metadata.timeout-ms:500}") int timeoutMs) {
        this(buildRestClient(baseUrl, timeoutMs), enabled);
    }

    CloudMetadataClientImpl(RestClient restClient, boolean enabled) {
        this.restClient = restClient;
        this.enabled = enabled;
    }

    @Override
    public Optional<CloudMetadata> fetch() {
        if (!enabled) {
            return Optional.empty();
        }

        try {
            String token = restClient.put()
                    .uri("/latest/api/token")
                    .header(TOKEN_TTL_HEADER, "60")
                    .retrieve()
                    .body(String.class);

            if (token == null || token.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(CloudMetadata.builder()
                    .instanceId(read(token, "instance-id"))
                    .instanceType(read(token, "instance-type"))
                    .amiId(read(token, "ami-id"))
                    .availabilityZone(read(token, "placement/availability-zone"))
                    .region(read(token, "placement/region"))
                    .privateIp(read(token, "local-ipv4"))
                    .publicIp(read(token, "public-ipv4"))
                    .build());
        } catch (Exception ex) {
            log.debug("EC2 metadata service not available: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    // A missing field (for example public-ipv4 on an instance with no public address) is a 404 - skip just that field
    private String read(String token, String path) {
        try {
            return restClient.get()
                    .uri("/latest/meta-data/" + path)
                    .header(TOKEN_HEADER, token)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException ex) {
            log.debug("EC2 metadata field {} not available: {}", path, ex.getMessage());
            return null;
        }
    }

    private static RestClient buildRestClient(String baseUrl, int timeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
