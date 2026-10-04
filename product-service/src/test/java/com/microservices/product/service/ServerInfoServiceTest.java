package com.microservices.product.service;

import com.microservices.product.client.CloudMetadataClient;
import com.microservices.product.dto.response.CloudMetadata;
import com.microservices.product.dto.response.ServerInfoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServerInfoServiceTest {

    private CloudMetadataClient cloudMetadataClient;
    private ServerInfoService service;

    @BeforeEach
    void setUp() {
        cloudMetadataClient = mock(CloudMetadataClient.class);
        when(cloudMetadataClient.fetch()).thenReturn(Optional.empty());

        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.application.name", "product-service")
                .withProperty("application.version", "1.0.0")
                .withProperty("server.port", "8081");
        environment.setActiveProfiles("dev");

        service = new ServerInfoService(environment, cloudMetadataClient);
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setLocalAddr("10.0.1.23");
        request.setLocalPort(8081);
        request.setRemoteAddr("203.0.113.7");
        request.addHeader("Host", "shop.example.com:8081");
        return request;
    }

    @Test
    void describe_reportsApplicationDetails() {
        ServerInfoResponse info = service.describe(request());

        assertThat(info.getApplication().getName()).isEqualTo("product-service");
        assertThat(info.getApplication().getVersion()).isEqualTo("1.0.0");
        assertThat(info.getApplication().getProfiles()).containsExactly("dev");
        assertThat(info.getApplication().getPort()).isEqualTo("8081");
    }

    @Test
    void describe_fallsBackToTheDefaultProfile_whenNoneIsActive() {
        ServerInfoService withoutProfiles = new ServerInfoService(new MockEnvironment(), cloudMetadataClient);

        ServerInfoResponse info = withoutProfiles.describe(request());

        assertThat(info.getApplication().getProfiles()).containsExactly("default");
    }

    @Test
    void describe_reportsHowTheRequestArrived() {
        MockHttpServletRequest request = request();
        request.addHeader("X-Forwarded-For", "198.51.100.4");

        ServerInfoResponse.RequestInfo requestInfo = service.describe(request).getRequest();

        assertThat(requestInfo.getHostHeader()).isEqualTo("shop.example.com:8081");
        assertThat(requestInfo.getServerAddress()).isEqualTo("10.0.1.23");
        assertThat(requestInfo.getServerPort()).isEqualTo(8081);
        assertThat(requestInfo.getClientAddress()).isEqualTo("203.0.113.7");
        assertThat(requestInfo.getForwardedFor()).isEqualTo("198.51.100.4");
    }

    @Test
    void describe_leavesForwardedForNull_whenHeaderIsAbsent() {
        assertThat(service.describe(request()).getRequest().getForwardedFor()).isNull();
    }

    @Test
    void describe_truncatesAVeryLongForwardedForHeader() {
        MockHttpServletRequest request = request();
        request.addHeader("X-Forwarded-For", "9".repeat(500));

        String forwardedFor = service.describe(request).getRequest().getForwardedFor();

        assertThat(forwardedFor).hasSize(201).endsWith("…");
    }

    @Test
    void describe_reportsHostAndRuntimeDetails() {
        ServerInfoResponse info = service.describe(request());

        assertThat(info.getHost().getHostname()).isNotBlank();
        assertThat(info.getHost().getAddresses()).isNotNull();

        ServerInfoResponse.RuntimeInfo runtime = info.getRuntime();
        assertThat(runtime.getJavaVersion()).isNotBlank();
        assertThat(runtime.getCpus()).isGreaterThan(0);
        assertThat(runtime.getHeapMaxMb()).isGreaterThan(0);
        assertThat(runtime.getHeapUsedPercent()).isBetween(0, 100);
        assertThat(runtime.getPid()).isGreaterThan(0);
        assertThat(runtime.getUptime()).matches("(\\d+d )?\\d{2}:\\d{2}:\\d{2}");
        assertThat(runtime.getStartedAt()).endsWith("Z");
    }

    @Test
    void describe_includesCloudMetadata_whenTheClientFindsIt() {
        when(cloudMetadataClient.fetch()).thenReturn(Optional.of(
                CloudMetadata.builder().instanceId("i-0abc123def456").availabilityZone("eu-west-1a").build()));

        CloudMetadata cloud = service.describe(request()).getCloud();

        assertThat(cloud.getInstanceId()).isEqualTo("i-0abc123def456");
        assertThat(cloud.getAvailabilityZone()).isEqualTo("eu-west-1a");
    }

    @Test
    void describe_hasNoCloudDetails_whenTheClientFindsNothing() {
        assertThat(service.describe(request()).getCloud()).isNull();
    }

    @Test
    void describe_looksUpCloudMetadataOnlyOnce_acrossRequests() {
        service.describe(request());
        service.describe(request());
        service.describe(request());

        verify(cloudMetadataClient, times(1)).fetch();
    }

    @Test
    void formatUptime_showsHoursMinutesSeconds() {
        assertThat(ServerInfoService.formatUptime(42)).isEqualTo("00:00:42");
        assertThat(ServerInfoService.formatUptime(3661)).isEqualTo("01:01:01");
    }

    @Test
    void formatUptime_showsDaysOnceUptimePassesADay() {
        assertThat(ServerInfoService.formatUptime(90061)).isEqualTo("1d 01:01:01");
    }
}
