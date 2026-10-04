package com.microservices.inventory.repository;

import com.microservices.inventory.model.InventoryItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository test backed by a real embedded H2 database with Flyway running
 * the actual migrations, including the full seed data set.
 *
 * The expected row counts are computed dynamically from product-service's
 * own migration files (the same files V2__seed_inventory.sql's generator
 * script read its SKU list from), rather than hard-coded, so this test stays
 * correct if product-service's catalog ever changes.
 */
@DataJpaTest
class InventoryRepositoryTest {

    // sku_code is always the last quoted field in a product-service INSERT
    // tuple, immediately followed by a closing paren and a comma/semicolon -
    // the same pattern used by the seed-data generator script.
    private static final Pattern SKU_PATTERN = Pattern.compile("'([A-Z0-9][A-Z0-9-]*)'\\)\\s*[,;]");

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryAuditLogRepository inventoryAuditLogRepository;

    private static Set<String> productServiceSkus() {
        Path baseDir = Paths.get("").toAbsolutePath();
        Path migrationDir = baseDir.resolve("../product-service/src/main/resources/db/migration").normalize();

        Set<String> skus = new LinkedHashSet<>();
        skus.addAll(extractSkus(migrationDir.resolve("V1__create_products_table.sql")));
        skus.addAll(extractSkus(migrationDir.resolve("V2__seed_products.sql")));
        return skus;
    }

    private static Set<String> extractSkus(Path file) {
        Set<String> skus = new LinkedHashSet<>();
        try {
            String content = Files.readString(file);
            Matcher matcher = SKU_PATTERN.matcher(content);
            while (matcher.find()) {
                skus.add(matcher.group(1));
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read product-service migration file: " + file, e);
        }
        return skus;
    }

    @Test
    void count_equalsDistinctSkuCountInProductServiceCatalog() {
        Set<String> expectedSkus = productServiceSkus();

        assertThat(expectedSkus).hasSize(5010);
        assertThat(inventoryRepository.count()).isEqualTo(expectedSkus.size());
    }

    @Test
    void auditLogCount_equalsDistinctSkuCountInProductServiceCatalog() {
        Set<String> expectedSkus = productServiceSkus();

        assertThat(inventoryAuditLogRepository.count()).isEqualTo(expectedSkus.size());
    }

    @Test
    void findBySkuCode_returnsSensibleRow_forOriginalV1Sku() {
        var result = inventoryRepository.findBySkuCode("APPLE-IP15P-128");

        assertThat(result).isPresent();
        InventoryItem item = result.get();
        assertThat(item.getSkuCode()).isEqualTo("APPLE-IP15P-128");
        assertThat(item.getQuantityOnHand()).isNotNull();
        assertThat(item.getQuantityOnHand()).isGreaterThanOrEqualTo(0);
        assertThat(item.getReorderThreshold()).isEqualTo(10);
    }

    @Test
    void findBySkuCode_returnsSensibleRow_forAnotherOriginalV1Sku() {
        var result = inventoryRepository.findBySkuCode("DYSON-V15-DETECT");

        assertThat(result).isPresent();
        assertThat(result.get().getQuantityOnHand()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void findBySkuCode_returnsSensibleRow_forBulkGeneratedV2Sku() {
        var result = inventoryRepository.findBySkuCode("ELEC-00001");

        assertThat(result).isPresent();
        assertThat(result.get().getQuantityOnHand()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void existsBySkuCode_trueForSeededSku_falseForUnknown() {
        assertThat(inventoryRepository.existsBySkuCode("APPLE-IP15P-128")).isTrue();
        assertThat(inventoryRepository.existsBySkuCode("DOES-NOT-EXIST")).isFalse();
    }

    @Test
    void findBySkuCodeIn_returnsOnlyMatchingSkus_silentlyOmittingUnknownOnes() {
        var results = inventoryRepository.findBySkuCodeIn(
                List.of("APPLE-IP15P-128", "ELEC-00001", "TOTALLY-FAKE-SKU"));

        assertThat(results).hasSize(2);
        assertThat(results).extracting(InventoryItem::getSkuCode)
                .containsExactlyInAnyOrder("APPLE-IP15P-128", "ELEC-00001");
    }
}
