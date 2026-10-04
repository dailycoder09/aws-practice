package com.microservices.product.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Inventory Client Implementation Tests
 *
 * Verifies request construction and response/error mapping against a mocked HTTP server
 * (Spring's MockRestServiceServer) - no live inventory-service required
 *
 * @author Microservices Team
 * @version 1.0
 */
class InventoryClientImplTest {

    private static final String BASE_URL = "http://localhost:8082";

    @Test
    void findStockBySkuCode_returnsQuantity_whenInventoryServiceRespondsWithRecord() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        InventoryClientImpl client = new InventoryClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/api/inventory/sku/APPLE-IP15P-128"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"quantityOnHand\": 42}", MediaType.APPLICATION_JSON));

        Optional<Integer> result = client.findStockBySkuCode("APPLE-IP15P-128");

        assertThat(result).contains(42);
        server.verify();
    }

    @Test
    void findStockBySkuCode_returnsEmpty_whenInventoryServiceRespondsWith404() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        InventoryClientImpl client = new InventoryClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/api/inventory/sku/UNKNOWN-SKU"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        Optional<Integer> result = client.findStockBySkuCode("UNKNOWN-SKU");

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    void findStockBySkuCode_returnsEmpty_whenInventoryServiceRespondsWithServerError() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        InventoryClientImpl client = new InventoryClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/api/inventory/sku/APPLE-IP15P-128"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        Optional<Integer> result = client.findStockBySkuCode("APPLE-IP15P-128");

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    void findStockBySkuCode_returnsEmpty_whenInventoryServiceIsUnreachable() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        InventoryClientImpl client = new InventoryClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/api/inventory/sku/APPLE-IP15P-128"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(request -> {
                    throw new IOException("Connection refused");
                });

        Optional<Integer> result = client.findStockBySkuCode("APPLE-IP15P-128");

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    void findStockForSkus_returnsMap_whenInventoryServiceRespondsWithRecords() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        InventoryClientImpl client = new InventoryClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/api/inventory/batch?skuCodes=APPLE-IP15P-128,SAMSUNG-S24-256"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"APPLE-IP15P-128\": 42, \"SAMSUNG-S24-256\": 7}", MediaType.APPLICATION_JSON));

        Map<String, Integer> result = client.findStockForSkus(List.of("APPLE-IP15P-128", "SAMSUNG-S24-256"));

        assertThat(result).containsEntry("APPLE-IP15P-128", 42).containsEntry("SAMSUNG-S24-256", 7);
        server.verify();
    }

    @Test
    void findStockForSkus_returnsEmptyMap_whenInventoryServiceRespondsWithServerError() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        InventoryClientImpl client = new InventoryClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/api/inventory/batch?skuCodes=APPLE-IP15P-128"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        Map<String, Integer> result = client.findStockForSkus(List.of("APPLE-IP15P-128"));

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    void findStockForSkus_returnsEmptyMap_whenInventoryServiceIsUnreachable() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        InventoryClientImpl client = new InventoryClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/api/inventory/batch?skuCodes=APPLE-IP15P-128"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(request -> {
                    throw new IOException("Connection refused");
                });

        Map<String, Integer> result = client.findStockForSkus(List.of("APPLE-IP15P-128"));

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    void findStockForSkus_returnsEmptyMap_whenInventoryServiceRespondsWithMalformedBody() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        InventoryClientImpl client = new InventoryClientImpl(builder.build());

        server.expect(requestTo(BASE_URL + "/api/inventory/batch?skuCodes=APPLE-IP15P-128"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("this is not json", MediaType.APPLICATION_JSON));

        Map<String, Integer> result = client.findStockForSkus(List.of("APPLE-IP15P-128"));

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    void findStockForSkus_returnsEmptyMap_andMakesNoRequest_whenSkuCodesIsEmpty() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        // Deliberately no server.expect(...) registered: MockRestServiceServer throws
        // AssertionError on any unexpected request, so if the implementation fails to
        // short-circuit on an empty list, this test fails loudly rather than silently passing.
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        InventoryClientImpl client = new InventoryClientImpl(builder.build());

        Map<String, Integer> result = client.findStockForSkus(List.of());

        assertThat(result).isEmpty();
        server.verify();
    }
}
