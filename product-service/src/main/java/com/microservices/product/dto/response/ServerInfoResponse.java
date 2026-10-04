package com.microservices.product.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Details about the server answering a request, shown on the home page.
 * {@code cloud} is null when the app is not running on AWS EC2.
 */
@Value
@Builder
public class ServerInfoResponse {

    ApplicationInfo application;
    HostInfo host;
    RequestInfo request;
    RuntimeInfo runtime;
    CloudMetadata cloud;

    @Value
    @Builder
    public static class ApplicationInfo {
        String name;
        String version;
        List<String> profiles;
        String port;
    }

    @Value
    @Builder
    public static class HostInfo {
        String hostname;
        List<NetworkAddress> addresses;
        boolean container;
    }

    @Value
    @Builder
    public static class NetworkAddress {
        String interfaceName;
        String address;
    }

    @Value
    @Builder
    public static class RequestInfo {
        String hostHeader;
        String serverAddress;
        int serverPort;
        String clientAddress;
        String forwardedFor;
    }

    @Value
    @Builder
    public static class RuntimeInfo {
        String javaVersion;
        String javaVendor;
        String osName;
        String osVersion;
        String osArch;
        int cpus;
        long heapUsedMb;
        long heapMaxMb;
        int heapUsedPercent;
        long pid;
        String startedAt;
        long startedAtEpochMs;
        String uptime;
    }
}
