package com.microservices.product.service;

import com.microservices.product.client.CloudMetadataClient;
import com.microservices.product.dto.response.CloudMetadata;
import com.microservices.product.dto.response.ServerInfoResponse;
import com.microservices.product.dto.response.ServerInfoResponse.ApplicationInfo;
import com.microservices.product.dto.response.ServerInfoResponse.HostInfo;
import com.microservices.product.dto.response.ServerInfoResponse.NetworkAddress;
import com.microservices.product.dto.response.ServerInfoResponse.RequestInfo;
import com.microservices.product.dto.response.ServerInfoResponse.RuntimeInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects details about the server answering a request (host, network, JVM, app, and AWS EC2 when available)
 * for the server info endpoint.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServerInfoService {

    private static final long MB = 1024 * 1024;
    private static final int MAX_HEADER_CHARS = 200;
    private static final long CLOUD_CACHE_MILLIS = Duration.ofMinutes(5).toMillis();

    private final Environment environment;
    private final CloudMetadataClient cloudMetadataClient;

    private String hostname;
    private CloudMetadata cloudMetadata;
    private long cloudCheckedAt;

    public ServerInfoResponse describe(HttpServletRequest request) {
        return ServerInfoResponse.builder()
                .application(applicationInfo())
                .host(hostInfo())
                .request(requestInfo(request))
                .runtime(runtimeInfo())
                .cloud(cloud())
                .build();
    }

    private ApplicationInfo applicationInfo() {
        String[] activeProfiles = environment.getActiveProfiles();
        List<String> profiles = List.of(activeProfiles.length > 0 ? activeProfiles : environment.getDefaultProfiles());

        return ApplicationInfo.builder()
                .name(environment.getProperty("spring.application.name", "unknown"))
                .version(environment.getProperty("application.version", "unknown"))
                .profiles(profiles)
                .port(environment.getProperty("local.server.port", environment.getProperty("server.port", "unknown")))
                .build();
    }

    private HostInfo hostInfo() {
        return HostInfo.builder()
                .hostname(hostname())
                .addresses(networkAddresses())
                .container(Files.exists(Path.of("/.dockerenv")))
                .build();
    }

    private RequestInfo requestInfo(HttpServletRequest request) {
        return RequestInfo.builder()
                .hostHeader(limit(request.getHeader("Host")))
                .serverAddress(request.getLocalAddr())
                .serverPort(request.getLocalPort())
                .clientAddress(request.getRemoteAddr())
                .forwardedFor(limit(request.getHeader("X-Forwarded-For")))
                .build();
    }

    private RuntimeInfo runtimeInfo() {
        Runtime jvm = Runtime.getRuntime();
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

        long usedMb = (jvm.totalMemory() - jvm.freeMemory()) / MB;
        long maxMb = jvm.maxMemory() / MB;
        int usedPercent = maxMb > 0 ? (int) Math.min(100, usedMb * 100 / maxMb) : 0;

        return RuntimeInfo.builder()
                .javaVersion(System.getProperty("java.version"))
                .javaVendor(System.getProperty("java.vendor"))
                .osName(System.getProperty("os.name"))
                .osVersion(System.getProperty("os.version"))
                .osArch(System.getProperty("os.arch"))
                .cpus(jvm.availableProcessors())
                .heapUsedMb(usedMb)
                .heapMaxMb(maxMb)
                .heapUsedPercent(usedPercent)
                .pid(ProcessHandle.current().pid())
                .startedAt(Instant.ofEpochMilli(runtimeBean.getStartTime()).truncatedTo(ChronoUnit.SECONDS).toString())
                .startedAtEpochMs(runtimeBean.getStartTime())
                .uptime(formatUptime(runtimeBean.getUptime() / 1000))
                .build();
    }

    // Looked up once and kept: reverse DNS for the hostname can be slow on some machines
    private synchronized String hostname() {
        if (hostname == null) {
            String fromEnvironment = System.getenv("HOSTNAME");
            if (fromEnvironment != null && !fromEnvironment.isBlank()) {
                hostname = fromEnvironment;
            } else {
                try {
                    hostname = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException ex) {
                    log.debug("Could not resolve local hostname: {}", ex.getMessage());
                    hostname = "unknown";
                }
            }
        }
        return hostname;
    }

    private List<NetworkAddress> networkAddresses() {
        List<NetworkAddress> addresses = new ArrayList<>();
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaces) {
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                    if (address instanceof Inet4Address) {
                        addresses.add(NetworkAddress.builder()
                                .interfaceName(networkInterface.getName())
                                .address(address.getHostAddress())
                                .build());
                    }
                }
            }
        } catch (SocketException ex) {
            log.debug("Could not list network interfaces: {}", ex.getMessage());
        }
        return addresses;
    }

    // The EC2 lookup is cached (also when it finds nothing) so refreshing the page does not retry it every time
    private synchronized CloudMetadata cloud() {
        long now = System.currentTimeMillis();
        if (cloudCheckedAt == 0 || now - cloudCheckedAt > CLOUD_CACHE_MILLIS) {
            cloudMetadata = cloudMetadataClient.fetch().orElse(null);
            cloudCheckedAt = now;
        }
        return cloudMetadata;
    }

    static String formatUptime(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        String clock = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        return days > 0 ? days + "d " + clock : clock;
    }

    // Headers are caller-controlled, so cap what gets displayed
    private static String limit(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > MAX_HEADER_CHARS ? value.substring(0, MAX_HEADER_CHARS) + "…" : value;
    }
}
