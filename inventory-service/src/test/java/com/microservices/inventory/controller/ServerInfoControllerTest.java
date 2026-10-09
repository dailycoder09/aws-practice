package com.microservices.inventory.controller;

import com.microservices.inventory.dto.response.CloudMetadata;
import com.microservices.inventory.dto.response.ServerInfoResponse;
import com.microservices.inventory.dto.response.ServerInfoResponse.ApplicationInfo;
import com.microservices.inventory.dto.response.ServerInfoResponse.HostInfo;
import com.microservices.inventory.dto.response.ServerInfoResponse.NetworkAddress;
import com.microservices.inventory.dto.response.ServerInfoResponse.RequestInfo;
import com.microservices.inventory.dto.response.ServerInfoResponse.RuntimeInfo;
import com.microservices.inventory.service.InventoryService;
import com.microservices.inventory.service.ServerInfoService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// InventoryController is loaded too so the test can prove /server-info wins over its /{id} mapping
@WebMvcTest({ServerInfoController.class, InventoryController.class})
class ServerInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServerInfoService serverInfoService;

    @MockBean
    private InventoryService inventoryService;

    private ServerInfoResponse.ServerInfoResponseBuilder responseBuilder() {
        return ServerInfoResponse.builder()
                .application(ApplicationInfo.builder()
                        .name("inventory-service")
                        .version("1.0.0")
                        .profiles(List.of("dev"))
                        .port("8082")
                        .build())
                .host(HostInfo.builder()
                        .hostname("ip-10-0-1-23")
                        .addresses(List.of(NetworkAddress.builder()
                                .interfaceName("eth0")
                                .address("10.0.1.23")
                                .build()))
                        .container(true)
                        .build())
                .request(RequestInfo.builder()
                        .hostHeader("shop.example.com")
                        .serverAddress("10.0.1.23")
                        .serverPort(8082)
                        .clientAddress("10.0.0.5")
                        .forwardedFor("203.0.113.7")
                        .build())
                .runtime(RuntimeInfo.builder()
                        .javaVersion("17.0.9")
                        .javaVendor("OpenLogic")
                        .osName("Linux")
                        .osVersion("6.1")
                        .osArch("amd64")
                        .cpus(4)
                        .heapUsedMb(120)
                        .heapMaxMb(512)
                        .heapUsedPercent(23)
                        .pid(1234)
                        .startedAt("2026-10-09T10:00:00Z")
                        .startedAtEpochMs(1791540000000L)
                        .uptime("00:05:00")
                        .build());
    }

    private CloudMetadata cloudMetadata() {
        return CloudMetadata.builder()
                .instanceId("i-0abc123def456")
                .instanceType("t3.micro")
                .availabilityZone("eu-west-1a")
                .region("eu-west-1")
                .privateIp("10.0.1.23")
                .publicIp("52.0.0.1")
                .amiId("ami-0123456789abcdef0")
                .build();
    }

    @Test
    void getServerInfo_returnsServerDetailsAsUncachedJson() throws Exception {
        when(serverInfoService.describe(any(HttpServletRequest.class))).thenReturn(responseBuilder().build());

        mockMvc.perform(get("/api/inventory/server-info"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.host.hostname").value("ip-10-0-1-23"))
                .andExpect(jsonPath("$.host.addresses[0].address").value("10.0.1.23"))
                .andExpect(jsonPath("$.request.serverAddress").value("10.0.1.23"))
                .andExpect(jsonPath("$.runtime.cpus").value(4))
                .andExpect(jsonPath("$.application.name").value("inventory-service"));
    }

    @Test
    void getServerInfo_includesCloudDetails_whenRunningOnEc2() throws Exception {
        when(serverInfoService.describe(any(HttpServletRequest.class)))
                .thenReturn(responseBuilder().cloud(cloudMetadata()).build());

        mockMvc.perform(get("/api/inventory/server-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cloud.instanceId").value("i-0abc123def456"));
    }

    @Test
    void getServerInfo_omitsCloudDetails_whenNotRunningOnEc2() throws Exception {
        when(serverInfoService.describe(any(HttpServletRequest.class))).thenReturn(responseBuilder().build());

        mockMvc.perform(get("/api/inventory/server-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cloud").doesNotExist());
    }

    @Test
    void getServerInfo_isAnsweredByThisController_notTreatedAsAnInventoryId() throws Exception {
        when(serverInfoService.describe(any(HttpServletRequest.class))).thenReturn(responseBuilder().build());

        mockMvc.perform(get("/api/inventory/server-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application.name").value("inventory-service"));

        verify(serverInfoService).describe(any(HttpServletRequest.class));
        // InventoryController#getById(/{id}) would have failed to parse "server-info" as a Long (400) or hit the service
        verifyNoInteractions(inventoryService);
    }
}
