package com.microservices.inventory.client;

import com.microservices.inventory.dto.response.CloudMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Verifies the EC2 metadata calls against a mocked HTTP server - no AWS needed.
 */
class CloudMetadataClientImplTest {

    private static final String BASE_URL = "http://169.254.169.254";
    private static final String TOKEN = "TOKEN-123";

    private void expectToken(MockRestServiceServer server) {
        server.expect(requestTo(BASE_URL + "/latest/api/token"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("X-aws-ec2-metadata-token-ttl-seconds", "60"))
                .andRespond(withSuccess(TOKEN, MediaType.TEXT_PLAIN));
    }

    private void expectField(MockRestServiceServer server, String path, String value) {
        server.expect(requestTo(BASE_URL + "/latest/meta-data/" + path))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-aws-ec2-metadata-token", TOKEN))
                .andRespond(withSuccess(value, MediaType.TEXT_PLAIN));
    }

    @Test
    void fetch_returnsInstanceDetails_whenTheMetadataServiceAnswers() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        CloudMetadataClientImpl client = new CloudMetadataClientImpl(builder.build(), true);

        expectToken(server);
        expectField(server, "instance-id", "i-0abc123def456");
        expectField(server, "instance-type", "t3.micro");
        expectField(server, "ami-id", "ami-0123456789abcdef0");
        expectField(server, "placement/availability-zone", "eu-west-1a");
        expectField(server, "placement/region", "eu-west-1");
        expectField(server, "local-ipv4", "10.0.1.23");
        expectField(server, "public-ipv4", "52.0.0.1");

        Optional<CloudMetadata> result = client.fetch();

        assertThat(result).isPresent();
        CloudMetadata cloud = result.get();
        assertThat(cloud.getInstanceId()).isEqualTo("i-0abc123def456");
        assertThat(cloud.getInstanceType()).isEqualTo("t3.micro");
        assertThat(cloud.getAmiId()).isEqualTo("ami-0123456789abcdef0");
        assertThat(cloud.getAvailabilityZone()).isEqualTo("eu-west-1a");
        assertThat(cloud.getRegion()).isEqualTo("eu-west-1");
        assertThat(cloud.getPrivateIp()).isEqualTo("10.0.1.23");
        assertThat(cloud.getPublicIp()).isEqualTo("52.0.0.1");
        server.verify();
    }

    @Test
    void fetch_skipsOnlyTheMissingField_whenTheInstanceHasNoPublicIp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        CloudMetadataClientImpl client = new CloudMetadataClientImpl(builder.build(), true);

        expectToken(server);
        expectField(server, "instance-id", "i-0abc123def456");
        expectField(server, "instance-type", "t3.micro");
        expectField(server, "ami-id", "ami-0123456789abcdef0");
        expectField(server, "placement/availability-zone", "eu-west-1a");
        expectField(server, "placement/region", "eu-west-1");
        expectField(server, "local-ipv4", "10.0.1.23");
        server.expect(requestTo(BASE_URL + "/latest/meta-data/public-ipv4"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        Optional<CloudMetadata> result = client.fetch();

        assertThat(result).isPresent();
        assertThat(result.get().getInstanceId()).isEqualTo("i-0abc123def456");
        assertThat(result.get().getPublicIp()).isNull();
        server.verify();
    }

    @Test
    void fetch_returnsEmpty_whenTheTokenRequestFails() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CloudMetadataClientImpl client = new CloudMetadataClientImpl(builder.build(), true);

        server.expect(requestTo(BASE_URL + "/latest/api/token"))
                .andRespond(request -> {
                    throw new IOException("Connection timed out");
                });

        assertThat(client.fetch()).isEmpty();
        server.verify();
    }

    @Test
    void fetch_returnsEmpty_whenTheMetadataServiceReturnsAnError() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CloudMetadataClientImpl client = new CloudMetadataClientImpl(builder.build(), true);

        server.expect(requestTo(BASE_URL + "/latest/api/token"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThat(client.fetch()).isEmpty();
        server.verify();
    }

    @Test
    void fetch_returnsEmpty_whenTheTokenComesBackEmpty() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CloudMetadataClientImpl client = new CloudMetadataClientImpl(builder.build(), true);

        server.expect(requestTo(BASE_URL + "/latest/api/token"))
                .andRespond(withSuccess("", MediaType.TEXT_PLAIN));

        assertThat(client.fetch()).isEmpty();
        server.verify();
    }

    @Test
    void fetch_makesNoRequestAtAll_whenDisabled() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        // No expectations registered: any request would fail the test
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CloudMetadataClientImpl client = new CloudMetadataClientImpl(builder.build(), false);

        assertThat(client.fetch()).isEmpty();
        server.verify();
    }

    @Test
    void fetch_returnsEmptyQuickly_whenNothingIsListeningAtTheMetadataAddress() {
        // Uses the real constructor and a real HTTP client against a closed local port
        CloudMetadataClientImpl client = new CloudMetadataClientImpl("http://localhost:1", true, 200);

        assertThat(client.fetch()).isEmpty();
    }
}
