package com.microservices.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.inventory.dto.request.InventoryRequest;
import com.microservices.inventory.dto.response.InventoryAuditLogResponse;
import com.microservices.inventory.dto.response.InventoryResponse;
import com.microservices.inventory.exception.DuplicateInventoryException;
import com.microservices.inventory.exception.InventoryNotFoundException;
import com.microservices.inventory.exception.ProductServiceUnavailableException;
import com.microservices.inventory.exception.SkuNotFoundInCatalogException;
import com.microservices.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService inventoryService;

    private InventoryRequest validRequest() {
        return InventoryRequest.builder()
                .skuCode("APPLE-IP15P-128")
                .quantityOnHand(50)
                .build();
    }

    private InventoryResponse response() {
        return InventoryResponse.builder()
                .id(1L)
                .skuCode("APPLE-IP15P-128")
                .quantityOnHand(50)
                .reorderThreshold(10)
                .build();
    }

    @Test
    void createInventory_returns201_whenRequestIsValid() throws Exception {
        when(inventoryService.createInventory(any(InventoryRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.skuCode").value("APPLE-IP15P-128"));
    }

    @Test
    void createInventory_returns400_whenSkuCodeIsBlank() throws Exception {
        InventoryRequest invalid = InventoryRequest.builder()
                .skuCode("")
                .quantityOnHand(10)
                .build();

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.skuCode").exists());
    }

    @Test
    void createInventory_returns400_whenQuantityIsNegative() throws Exception {
        InventoryRequest invalid = InventoryRequest.builder()
                .skuCode("APPLE-IP15P-128")
                .quantityOnHand(-5)
                .build();

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createInventory_returns422_whenSkuNotInCatalog() throws Exception {
        when(inventoryService.createInventory(any(InventoryRequest.class)))
                .thenThrow(new SkuNotFoundInCatalogException("UNKNOWN-SKU"));

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createInventory_returns409_whenDuplicateInventory() throws Exception {
        when(inventoryService.createInventory(any(InventoryRequest.class)))
                .thenThrow(new DuplicateInventoryException("APPLE-IP15P-128"));

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict());
    }

    @Test
    void createInventory_returns503_whenProductServiceUnavailable() throws Exception {
        when(inventoryService.createInventory(any(InventoryRequest.class)))
                .thenThrow(new ProductServiceUnavailableException());

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void getById_returns200_whenRecordExists() throws Exception {
        when(inventoryService.getById(1L)).thenReturn(response());

        mockMvc.perform(get("/api/inventory/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getById_returns404_whenRecordMissing() throws Exception {
        when(inventoryService.getById(99L)).thenThrow(new InventoryNotFoundException(99L));

        mockMvc.perform(get("/api/inventory/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBySkuCode_returns200_whenRecordExists() throws Exception {
        when(inventoryService.getBySkuCode("APPLE-IP15P-128")).thenReturn(response());

        mockMvc.perform(get("/api/inventory/sku/{skuCode}", "APPLE-IP15P-128"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuCode").value("APPLE-IP15P-128"));
    }

    @Test
    void getBySkuCode_returns404_whenSkuMissing() throws Exception {
        when(inventoryService.getBySkuCode("UNKNOWN")).thenThrow(new InventoryNotFoundException("UNKNOWN"));

        mockMvc.perform(get("/api/inventory/sku/{skuCode}", "UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAll_returns200_withPagedBody() throws Exception {
        Page<InventoryResponse> page = new PageImpl<>(List.of(response()), PageRequest.of(0, 20), 1);
        when(inventoryService.getAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].skuCode").value("APPLE-IP15P-128"));
    }

    @Test
    void update_returns200_whenRequestIsValid() throws Exception {
        when(inventoryService.update(anyLong(), any(InventoryRequest.class))).thenReturn(response());

        mockMvc.perform(put("/api/inventory/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk());
    }

    @Test
    void update_returns404_whenRecordMissing() throws Exception {
        when(inventoryService.update(anyLong(), any(InventoryRequest.class)))
                .thenThrow(new InventoryNotFoundException(99L));

        mockMvc.perform(put("/api/inventory/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void adjustStock_returns200_whenAdjustmentSucceeds() throws Exception {
        when(inventoryService.adjustStock(anyString(), anyInt())).thenReturn(response());

        mockMvc.perform(patch("/api/inventory/sku/{skuCode}/adjust", "APPLE-IP15P-128")
                        .param("delta", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void adjustStock_returns400_whenResultingQuantityWouldBeNegative() throws Exception {
        when(inventoryService.adjustStock(anyString(), anyInt()))
                .thenThrow(new IllegalArgumentException("Resulting quantity cannot be negative"));

        mockMvc.perform(patch("/api/inventory/sku/{skuCode}/adjust", "APPLE-IP15P-128")
                        .param("delta", "-1000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adjustStock_returns404_whenSkuMissing() throws Exception {
        when(inventoryService.adjustStock(anyString(), anyInt()))
                .thenThrow(new InventoryNotFoundException("UNKNOWN"));

        mockMvc.perform(patch("/api/inventory/sku/{skuCode}/adjust", "UNKNOWN")
                        .param("delta", "10"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204_whenRecordExists() throws Exception {
        mockMvc.perform(delete("/api/inventory/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns404_whenRecordMissing() throws Exception {
        doThrow(new InventoryNotFoundException(99L)).when(inventoryService).delete(99L);

        mockMvc.perform(delete("/api/inventory/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAuditHistory_returns200_withPagedBody() throws Exception {
        InventoryAuditLogResponse auditResponse = InventoryAuditLogResponse.builder()
                .id(1L)
                .skuCode("APPLE-IP15P-128")
                .changeType("CREATED")
                .newQuantity(50)
                .build();
        Page<InventoryAuditLogResponse> page = new PageImpl<>(List.of(auditResponse), PageRequest.of(0, 20), 1);
        when(inventoryService.getAuditHistory(anyString(), any())).thenReturn(page);

        mockMvc.perform(get("/api/inventory/sku/{skuCode}/audit", "APPLE-IP15P-128"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].changeType").value("CREATED"));
    }

    @Test
    void getBatchStock_returns200_withMapOfRequestedSkus() throws Exception {
        when(inventoryService.getStockForSkus(List.of("A", "B")))
                .thenReturn(Map.of("A", 10, "B", 20));

        mockMvc.perform(get("/api/inventory/batch").param("skuCodes", "A,B"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.A").value(10))
                .andExpect(jsonPath("$.B").value(20));
    }

    @Test
    void getBatchStock_returns200_withEmptyBody_whenSkuCodesParamIsAbsent() throws Exception {
        mockMvc.perform(get("/api/inventory/batch"))
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));
    }

    @Test
    void unmappedPath_returns404_notInternalServerError() throws Exception {
        mockMvc.perform(get("/this-route-does-not-exist"))
                .andExpect(status().isNotFound());
    }
}
