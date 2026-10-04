package com.microservices.product.controller;

import com.microservices.product.dto.response.CloudMetadata;
import com.microservices.product.dto.response.ProductResponse;
import com.microservices.product.dto.response.ServerInfoResponse;
import com.microservices.product.dto.response.ServerInfoResponse.ApplicationInfo;
import com.microservices.product.dto.response.ServerInfoResponse.HostInfo;
import com.microservices.product.dto.response.ServerInfoResponse.NetworkAddress;
import com.microservices.product.dto.response.ServerInfoResponse.RequestInfo;
import com.microservices.product.dto.response.ServerInfoResponse.RuntimeInfo;
import com.microservices.product.service.ProductService;
import com.microservices.product.service.ServerInfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HomeController.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private ServerInfoService serverInfoService;

    @BeforeEach
    void setUp() {
        Page<ProductResponse> onlyTheCount = new PageImpl<>(List.of(), PageRequest.of(0, 1), 5010);
        when(productService.getAllProducts(any(Pageable.class))).thenReturn(onlyTheCount);
        when(productService.getAllCategories()).thenReturn(List.of(
                "Automotive", "Beauty and Personal Care", "Books", "Clothing", "Electronics",
                "Furniture", "Groceries", "Home Appliances", "Sports and Outdoors", "Toys and Games"));
    }

    private ServerInfoResponse serverInfo(CloudMetadata cloud, String forwardedFor) {
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
                        .clientAddress("203.0.113.7").forwardedFor(forwardedFor).build())
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
    void home_rendersHostnameRequestPathAndCatalogSummary() throws Exception {
        when(serverInfoService.describe(any())).thenReturn(serverInfo(null, "198.51.100.4"));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("server"))
                .andExpect(model().attribute("productCount", 5010L))
                .andExpect(model().attribute("categoryCount", 10))
                .andExpect(content().string(containsString("ip-10-0-1-23.ec2.internal")))
                .andExpect(content().string(containsString("10.0.1.23:8081")))
                .andExpect(content().string(containsString("203.0.113.7")))
                .andExpect(content().string(containsString("198.51.100.4")))
                .andExpect(content().string(containsString("shop.example.com")))
                .andExpect(content().string(containsString("5,010")))
                .andExpect(content().string(containsString("17.0.14")));
    }

    @Test
    void home_showsEc2Details_whenCloudMetadataIsPresent() throws Exception {
        when(serverInfoService.describe(any())).thenReturn(serverInfo(ec2(), null));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("i-0abc123def456")))
                .andExpect(content().string(containsString("t3.micro")))
                .andExpect(content().string(containsString("eu-west-1a")))
                .andExpect(content().string(containsString("52.0.0.1")))
                .andExpect(content().string(not(containsString("Not running on AWS EC2"))));
    }

    @Test
    void home_saysItIsNotOnEc2_whenCloudMetadataIsAbsent() throws Exception {
        when(serverInfoService.describe(any())).thenReturn(serverInfo(null, null));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Not running on AWS EC2")))
                .andExpect(content().string(not(containsString("Instance ID"))));
    }

    @Test
    void home_leavesOutForwardedFor_whenHeaderWasNotSent() throws Exception {
        when(serverInfoService.describe(any())).thenReturn(serverInfo(null, null));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Forwarded for"))));
    }
}
