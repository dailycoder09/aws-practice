package com.microservices.inventory.service;

import com.microservices.inventory.client.ProductClient;
import com.microservices.inventory.dto.request.InventoryRequest;
import com.microservices.inventory.dto.response.InventoryAuditLogResponse;
import com.microservices.inventory.dto.response.InventoryResponse;
import com.microservices.inventory.dto.response.ProductSummary;
import com.microservices.inventory.exception.DuplicateInventoryException;
import com.microservices.inventory.exception.InventoryNotFoundException;
import com.microservices.inventory.exception.ProductServiceUnavailableException;
import com.microservices.inventory.exception.SkuNotFoundInCatalogException;
import com.microservices.inventory.mapper.InventoryMapper;
import com.microservices.inventory.model.ChangeType;
import com.microservices.inventory.model.InventoryAuditLog;
import com.microservices.inventory.model.InventoryItem;
import com.microservices.inventory.repository.InventoryAuditLogRepository;
import com.microservices.inventory.repository.InventoryRepository;
import com.microservices.inventory.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryAuditLogRepository inventoryAuditLogRepository;

    @Mock
    private InventoryMapper inventoryMapper;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private InventoryItem inventoryItem;
    private InventoryRequest request;
    private InventoryResponse response;
    private ProductSummary productSummary;

    @BeforeEach
    void setUp() {
        inventoryItem = InventoryItem.builder()
                .id(1L)
                .skuCode("APPLE-IP15P-128")
                .quantityOnHand(50)
                .reorderThreshold(10)
                .build();

        request = InventoryRequest.builder()
                .skuCode("APPLE-IP15P-128")
                .quantityOnHand(50)
                .build();

        response = InventoryResponse.builder()
                .id(1L)
                .skuCode("APPLE-IP15P-128")
                .quantityOnHand(50)
                .reorderThreshold(10)
                .build();

        productSummary = ProductSummary.builder()
                .skuCode("APPLE-IP15P-128")
                .name("iPhone 15 Pro")
                .price(new BigDecimal("999.99"))
                .build();
    }

    @Test
    void createInventory_savesAndReturnsResponse_whenSkuExistsInCatalogAndIsUnique() {
        when(productClient.findBySkuCode("APPLE-IP15P-128")).thenReturn(Optional.of(productSummary));
        when(inventoryRepository.existsBySkuCode("APPLE-IP15P-128")).thenReturn(false);
        when(inventoryRepository.save(any(InventoryItem.class))).thenReturn(inventoryItem);
        when(inventoryMapper.toResponse(inventoryItem)).thenReturn(response);

        InventoryResponse result = inventoryService.createInventory(request);

        assertThat(result).isEqualTo(response);
        verify(inventoryRepository).save(any(InventoryItem.class));

        ArgumentCaptor<InventoryAuditLog> auditCaptor = ArgumentCaptor.forClass(InventoryAuditLog.class);
        verify(inventoryAuditLogRepository).save(auditCaptor.capture());

        InventoryAuditLog savedAudit = auditCaptor.getValue();
        assertThat(savedAudit.getSkuCode()).isEqualTo("APPLE-IP15P-128");
        assertThat(savedAudit.getChangeType()).isEqualTo(ChangeType.CREATED);
        assertThat(savedAudit.getPreviousQuantity()).isNull();
        assertThat(savedAudit.getNewQuantity()).isEqualTo(50);
    }

    @Test
    void createInventory_defaultsReorderThreshold_whenNotProvided() {
        when(productClient.findBySkuCode("APPLE-IP15P-128")).thenReturn(Optional.of(productSummary));
        when(inventoryRepository.existsBySkuCode("APPLE-IP15P-128")).thenReturn(false);
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryMapper.toResponse(any(InventoryItem.class))).thenReturn(response);

        inventoryService.createInventory(request);

        ArgumentCaptor<InventoryItem> itemCaptor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(inventoryRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getReorderThreshold()).isEqualTo(10);
    }

    @Test
    void createInventory_throwsSkuNotFoundInCatalogException_whenSkuNotInCatalog() {
        when(productClient.findBySkuCode("UNKNOWN-SKU")).thenReturn(Optional.empty());

        InventoryRequest unknownRequest = InventoryRequest.builder()
                .skuCode("UNKNOWN-SKU")
                .quantityOnHand(10)
                .build();

        assertThatThrownBy(() -> inventoryService.createInventory(unknownRequest))
                .isInstanceOf(SkuNotFoundInCatalogException.class);

        verify(inventoryRepository, never()).save(any());
        verify(inventoryAuditLogRepository, never()).save(any());
    }

    @Test
    void createInventory_propagatesProductServiceUnavailableException_whenCatalogUnreachable() {
        when(productClient.findBySkuCode("APPLE-IP15P-128"))
                .thenThrow(new ProductServiceUnavailableException());

        assertThatThrownBy(() -> inventoryService.createInventory(request))
                .isInstanceOf(ProductServiceUnavailableException.class);

        verify(inventoryRepository, never()).save(any());
        verify(inventoryAuditLogRepository, never()).save(any());
    }

    @Test
    void createInventory_throwsDuplicateInventoryException_whenInventoryAlreadyExists() {
        when(productClient.findBySkuCode("APPLE-IP15P-128")).thenReturn(Optional.of(productSummary));
        when(inventoryRepository.existsBySkuCode("APPLE-IP15P-128")).thenReturn(true);

        assertThatThrownBy(() -> inventoryService.createInventory(request))
                .isInstanceOf(DuplicateInventoryException.class);

        verify(inventoryRepository, never()).save(any());
        verify(inventoryAuditLogRepository, never()).save(any());
    }

    @Test
    void getById_returnsResponse_whenRecordExists() {
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventoryItem));
        when(inventoryMapper.toResponse(inventoryItem)).thenReturn(response);

        InventoryResponse result = inventoryService.getById(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getById_throwsInventoryNotFoundException_whenRecordMissing() {
        when(inventoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getById(99L))
                .isInstanceOf(InventoryNotFoundException.class);
    }

    @Test
    void getBySkuCode_returnsResponse_whenRecordExists() {
        when(inventoryRepository.findBySkuCode("APPLE-IP15P-128")).thenReturn(Optional.of(inventoryItem));
        when(inventoryMapper.toResponse(inventoryItem)).thenReturn(response);

        InventoryResponse result = inventoryService.getBySkuCode("APPLE-IP15P-128");

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getBySkuCode_throwsInventoryNotFoundException_whenSkuMissing() {
        when(inventoryRepository.findBySkuCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getBySkuCode("UNKNOWN"))
                .isInstanceOf(InventoryNotFoundException.class);
    }

    @Test
    void getAll_returnsMappedPage() {
        Page<InventoryItem> page = new PageImpl<>(List.of(inventoryItem));
        when(inventoryRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(inventoryMapper.toResponse(inventoryItem)).thenReturn(response);

        Page<InventoryResponse> result = inventoryService.getAll(PageRequest.of(0, 20));

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void update_updatesQuantityAndThresholdAndWritesAuditEntry_whenRecordExists() {
        InventoryRequest updateRequest = InventoryRequest.builder()
                .skuCode("APPLE-IP15P-128")
                .quantityOnHand(75)
                .reorderThreshold(20)
                .build();

        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventoryItem));
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryMapper.toResponse(any(InventoryItem.class))).thenReturn(response);

        inventoryService.update(1L, updateRequest);

        ArgumentCaptor<InventoryItem> itemCaptor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(inventoryRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getQuantityOnHand()).isEqualTo(75);
        assertThat(itemCaptor.getValue().getReorderThreshold()).isEqualTo(20);

        ArgumentCaptor<InventoryAuditLog> auditCaptor = ArgumentCaptor.forClass(InventoryAuditLog.class);
        verify(inventoryAuditLogRepository).save(auditCaptor.capture());
        InventoryAuditLog savedAudit = auditCaptor.getValue();
        assertThat(savedAudit.getChangeType()).isEqualTo(ChangeType.UPDATED);
        assertThat(savedAudit.getPreviousQuantity()).isEqualTo(50);
        assertThat(savedAudit.getNewQuantity()).isEqualTo(75);
    }

    @Test
    void update_throwsInventoryNotFoundException_whenRecordMissing() {
        when(inventoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.update(99L, request))
                .isInstanceOf(InventoryNotFoundException.class);

        verify(inventoryRepository, never()).save(any());
        verify(inventoryAuditLogRepository, never()).save(any());
    }

    @Test
    void adjustStock_increasesQuantityAndWritesAdjustedAuditEntry() {
        when(inventoryRepository.findBySkuCode("APPLE-IP15P-128")).thenReturn(Optional.of(inventoryItem));
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryMapper.toResponse(any(InventoryItem.class))).thenReturn(response);

        inventoryService.adjustStock("APPLE-IP15P-128", 25);

        ArgumentCaptor<InventoryItem> itemCaptor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(inventoryRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getQuantityOnHand()).isEqualTo(75);

        ArgumentCaptor<InventoryAuditLog> auditCaptor = ArgumentCaptor.forClass(InventoryAuditLog.class);
        verify(inventoryAuditLogRepository).save(auditCaptor.capture());
        InventoryAuditLog savedAudit = auditCaptor.getValue();
        assertThat(savedAudit.getChangeType()).isEqualTo(ChangeType.ADJUSTED);
        assertThat(savedAudit.getPreviousQuantity()).isEqualTo(50);
        assertThat(savedAudit.getNewQuantity()).isEqualTo(75);
    }

    @Test
    void adjustStock_decreasesQuantityAndWritesAdjustedAuditEntry() {
        when(inventoryRepository.findBySkuCode("APPLE-IP15P-128")).thenReturn(Optional.of(inventoryItem));
        when(inventoryRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryMapper.toResponse(any(InventoryItem.class))).thenReturn(response);

        inventoryService.adjustStock("APPLE-IP15P-128", -20);

        ArgumentCaptor<InventoryAuditLog> auditCaptor = ArgumentCaptor.forClass(InventoryAuditLog.class);
        verify(inventoryAuditLogRepository).save(auditCaptor.capture());
        InventoryAuditLog savedAudit = auditCaptor.getValue();
        assertThat(savedAudit.getChangeType()).isEqualTo(ChangeType.ADJUSTED);
        assertThat(savedAudit.getPreviousQuantity()).isEqualTo(50);
        assertThat(savedAudit.getNewQuantity()).isEqualTo(30);
    }

    @Test
    void adjustStock_throwsIllegalArgumentException_whenResultWouldBeNegative() {
        when(inventoryRepository.findBySkuCode("APPLE-IP15P-128")).thenReturn(Optional.of(inventoryItem));

        assertThatThrownBy(() -> inventoryService.adjustStock("APPLE-IP15P-128", -100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Resulting quantity cannot be negative");

        verify(inventoryRepository, never()).save(any());
        verify(inventoryAuditLogRepository, never()).save(any());
    }

    @Test
    void adjustStock_throwsInventoryNotFoundException_whenSkuMissing() {
        when(inventoryRepository.findBySkuCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.adjustStock("UNKNOWN", 10))
                .isInstanceOf(InventoryNotFoundException.class);

        verify(inventoryAuditLogRepository, never()).save(any());
    }

    @Test
    void delete_deletesRecordAndWritesDeletedAuditEntry_whenRecordExists() {
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventoryItem));

        inventoryService.delete(1L);

        verify(inventoryRepository).delete(inventoryItem);

        ArgumentCaptor<InventoryAuditLog> auditCaptor = ArgumentCaptor.forClass(InventoryAuditLog.class);
        verify(inventoryAuditLogRepository).save(auditCaptor.capture());
        InventoryAuditLog savedAudit = auditCaptor.getValue();
        assertThat(savedAudit.getSkuCode()).isEqualTo("APPLE-IP15P-128");
        assertThat(savedAudit.getChangeType()).isEqualTo(ChangeType.DELETED);
        assertThat(savedAudit.getPreviousQuantity()).isEqualTo(50);
        assertThat(savedAudit.getNewQuantity()).isNull();
    }

    @Test
    void delete_throwsInventoryNotFoundException_whenRecordMissing() {
        when(inventoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.delete(99L))
                .isInstanceOf(InventoryNotFoundException.class);

        verify(inventoryRepository, never()).delete(any());
        verify(inventoryAuditLogRepository, never()).save(any());
    }

    @Test
    void getAuditHistory_returnsMappedPage() {
        InventoryAuditLog auditLog = InventoryAuditLog.builder()
                .id(1L)
                .skuCode("APPLE-IP15P-128")
                .changeType(ChangeType.CREATED)
                .previousQuantity(null)
                .newQuantity(50)
                .build();

        InventoryAuditLogResponse auditResponse = InventoryAuditLogResponse.builder()
                .id(1L)
                .skuCode("APPLE-IP15P-128")
                .changeType("CREATED")
                .previousQuantity(null)
                .newQuantity(50)
                .build();

        Page<InventoryAuditLog> page = new PageImpl<>(List.of(auditLog));
        when(inventoryAuditLogRepository.findBySkuCodeOrderByChangedAtDesc(eq("APPLE-IP15P-128"), any(Pageable.class)))
                .thenReturn(page);
        when(inventoryMapper.toAuditResponse(auditLog)).thenReturn(auditResponse);

        Page<InventoryAuditLogResponse> result = inventoryService.getAuditHistory("APPLE-IP15P-128", PageRequest.of(0, 20));

        assertThat(result.getContent()).containsExactly(auditResponse);
    }

    @Test
    void getStockForSkus_returnsMapOfSkuCodeToQuantity_whenRepositoryReturnsItems() {
        InventoryItem secondItem = InventoryItem.builder()
                .id(2L)
                .skuCode("ELEC-00001")
                .quantityOnHand(200)
                .reorderThreshold(10)
                .build();

        when(inventoryRepository.findBySkuCodeIn(List.of("APPLE-IP15P-128", "ELEC-00001")))
                .thenReturn(List.of(inventoryItem, secondItem));

        Map<String, Integer> result = inventoryService.getStockForSkus(List.of("APPLE-IP15P-128", "ELEC-00001"));

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
                "APPLE-IP15P-128", 50,
                "ELEC-00001", 200));
    }

    @Test
    void getStockForSkus_returnsEmptyMapWithoutCallingRepository_whenInputIsEmpty() {
        Map<String, Integer> result = inventoryService.getStockForSkus(List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(inventoryRepository);
    }
}
