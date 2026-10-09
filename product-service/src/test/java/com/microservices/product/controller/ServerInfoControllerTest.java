package com.microservices.product.controller;

import com.microservices.product.dto.response.CloudMetadata;
import com.microservices.product.dto.response.ServerInfoResponse;
import com.microservices.product.dto.response.ServerInfoResponse.ApplicationInfo;
import com.microservices.product.dto.response.ServerInfoResponse.HostInfo;
import com.microservices.product.dto.response.ServerInfoResponse.NetworkAddress;
import com.microservices.product.dto.response.ServerInfoResponse.RequestInfo;
import com.microservices.product.dto.response.ServerInfoResponse.RuntimeInfo;
import com.microservices.product.service.ProductService;
import com.microservices.product.service.ServerInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ProductController is included in the slice on purpose: its GET /api/products/{id} mapping is the
// one that could swallow /api/products/server-info, so the routing test needs both controllers loaded.
@WebMvcTest({ServerInfoController.class, ProductController.class})
class ServerInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServerInfoService serverInfoService;

    @MockBean
    private ProductService productService;

    private ServerInfoResponse serverInfo(CloudMetadata cloud) {
        return ServerInfoResponse.builder()
                .application(ApplicationInfo.builder()
                        .name("product-service").version("1.0.0").profiles(List.of("dev")).port("8081").build())
                .host(HostInfo.builder()
                        .hostname("ip-10-0-1-23.ec2.internal")
                        .addresses(List.of(NetworkAddress.builder().interfaceName("eth0").address("10.0.1.23").build()))
                        .container(true)
                        .build())
                .request(RequestInfo.builder()
                        .hostHeader("shop.example.com").serverAddress("10.0.1.23").serverPort(8081)
                        .clientAddress("203.0.113.7").forwardedFor("198.51.100.4").build())
                .runtime(RuntimeInfo.builder()
                        .javaVersion("17.0.14").javaVendor("OpenLogic")
                        .osName("Linux").osVersion("6.1").osArch("amd64")
                        .cpus(4).heapUsedMb(200).heapMaxMb(512).heapUsedPercent(39).pid(4121)
                        .startedAt("2026-10-04T08:43:44Z").startedAtEpochMs(1791103424000L).uptime("00:12:44")
                        .build())
                .cloud(cloud)
                .build();
    }

    private CloudMetadata ec2() {
        return CloudMetadata.builder()
                .instanceId("i-0abc123def456").instanceType("t3.micro")
                .availabilityZone("eu-west-1a").region("eu-west-1")
                .privateIp("10.0.1.23").publicIp("52.0.0.1").amiId("ami-0123456789abcdef0")
                .build();
    }

    @Test
    void getServerInfo_returnsUncachedJsonWithHostRequestRuntimeAndApplication() throws Exception {
        when(serverInfoService.describe(any())).thenReturn(serverInfo(null));

        mockMvc.perform(get("/api/products/server-info"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.host.hostname").value("ip-10-0-1-23.ec2.internal"))
                .andExpect(jsonPath("$.host.addresses[0].address").value("10.0.1.23"))
                .andExpect(jsonPath("$.request.serverAddress").value("10.0.1.23"))
                .andExpect(jsonPath("$.runtime.cpus").value(4))
                .andExpect(jsonPath("$.application.name").value("product-service"));
    }

    @Test
    void getServerInfo_includesCloudDetails_whenRunningOnEc2() throws Exception {
        when(serverInfoService.describe(any())).thenReturn(serverInfo(ec2()));

        mockMvc.perform(get("/api/products/server-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cloud.instanceId").value("i-0abc123def456"))
                .andExpect(jsonPath("$.cloud.availabilityZone").value("eu-west-1a"));
    }

    @Test
    void getServerInfo_leavesOutCloud_whenNotRunningOnEc2() throws Exception {
        when(serverInfoService.describe(any())).thenReturn(serverInfo(null));

        mockMvc.perform(get("/api/products/server-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cloud").doesNotExist());
    }

    @Test
    void getServerInfo_isRoutedToServerInfoController_notToTheProductIdMapping() throws Exception {
        when(serverInfoService.describe(any())).thenReturn(serverInfo(null));

        // If GET /api/products/{id} won this request, "server-info" would fail Long conversion (400)
        // and the product service would be asked for a product instead of the server info.
        mockMvc.perform(get("/api/products/server-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.host.hostname").value("ip-10-0-1-23.ec2.internal"))
                .andExpect(jsonPath("$.application.name").value("product-service"));

        verifyNoInteractions(productService);
    }

    @Test
    void getRoot_returns404_nowThatTheServerRenderedHomePageIsGone() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isNotFound());
    }
}
