package com.sa.promotion.domain.sku.service;

import com.sa.promotion.domain.sku.entity.Sku;
import com.sa.promotion.domain.sku.repository.SkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("SKU管理域服务测试")
class SkuDomainServiceTest {

    private SkuDomainService skuDomainService;

    @Mock
    private SkuRepository skuRepository;

    // 模拟内存存储
    private final Map<String, Sku> mockStore = new HashMap<>();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        skuDomainService = new SkuDomainService(skuRepository);
        mockStore.clear();

        // save
        doAnswer(inv -> {
            Sku s = inv.getArgument(0);
            mockStore.put(s.getSkuId(), s);
            return null;
        }).when(skuRepository).save(any(Sku.class));

        // update
        doAnswer(inv -> {
            Sku s = inv.getArgument(0);
            mockStore.put(s.getSkuId(), s);
            return null;
        }).when(skuRepository).update(any(Sku.class));

        // delete
        doAnswer(inv -> {
            mockStore.remove(inv.getArgument(0).toString());
            return null;
        }).when(skuRepository).delete(anyString());

        // findBySkuId
        when(skuRepository.findBySkuId(anyString())).thenAnswer(inv ->
            Optional.ofNullable(mockStore.get(inv.getArgument(0).toString())));

        // findAll
        when(skuRepository.findAll()).thenAnswer(inv -> new ArrayList<>(mockStore.values()));

        // findBySkuIds
        when(skuRepository.findBySkuIds(anyList())).thenAnswer(inv -> {
            List<String> ids = inv.getArgument(0);
            List<Sku> result = new ArrayList<>();
            for (String id : ids) {
                Sku s = mockStore.get(id);
                if (s != null) result.add(s);
            }
            return result;
        });

        // exists
        when(skuRepository.exists(anyString())).thenAnswer(inv ->
            mockStore.containsKey(inv.getArgument(0).toString()));
    }

    @Test
    @DisplayName("createSku - 成功创建SKU")
    void testCreateSkuValid() {
        Sku sku = skuDomainService.createSku("Test SKU", new BigDecimal("100.00"));
        assertNotNull(sku);
        assertNotNull(sku.getSkuId());
        assertEquals("Test SKU", sku.getSkuName());
    }

    @Test
    @DisplayName("createSku - 空名称抛出异常")
    void testCreateSkuEmptyName() {
        assertThrows(IllegalArgumentException.class,
            () -> skuDomainService.createSku("", new BigDecimal("100.00")));
    }

    @Test
    @DisplayName("updateSku - 成功更新")
    void testUpdateSku() {
        Sku sku = skuDomainService.createSku("Test SKU", new BigDecimal("100.00"));
        Sku updated = skuDomainService.updateSku(sku.getSkuId(), "Updated", null);
        assertEquals("Updated", updated.getSkuName());
    }

    @Test
    @DisplayName("deleteSku - 成功删除")
    void testDeleteSku() {
        Sku sku = skuDomainService.createSku("Test SKU", new BigDecimal("100.00"));
        skuDomainService.deleteSku(sku.getSkuId());
        assertNull(skuDomainService.findBySkuId(sku.getSkuId()));
    }

    @Test
    @DisplayName("deleteSku - 不存在抛出异常")
    void testDeleteSkuNotFound() {
        assertThrows(com.sa.promotion.domain.exception.ResourceNotFoundException.class,
            () -> skuDomainService.deleteSku("non-existent"));
    }

    @Test
    @DisplayName("findBySkuId - 找到")
    void testFindBySkuIdFound() {
        Sku sku = skuDomainService.createSku("Test", new BigDecimal("100.00"));
        assertNotNull(skuDomainService.findBySkuId(sku.getSkuId()));
    }

    @Test
    @DisplayName("findBySkuId - 未找到返回null")
    void testFindBySkuIdNotFound() {
        assertNull(skuDomainService.findBySkuId("non-existent"));
    }

    @Test
    @DisplayName("listAllSkus - 返回列表")
    void testListAllSkus() {
        skuDomainService.createSku("S1", new BigDecimal("100"));
        skuDomainService.createSku("S2", new BigDecimal("200"));
        assertEquals(2, skuDomainService.listAllSkus().size());
    }

    @Test
    @DisplayName("batchQuerySkus - 批量查询")
    void testBatchQuerySkus() {
        Sku s1 = skuDomainService.createSku("S1", new BigDecimal("100"));
        Sku s2 = skuDomainService.createSku("S2", new BigDecimal("200"));
        List<Sku> result = skuDomainService.batchQuerySkus(Arrays.asList(s1.getSkuId(), s2.getSkuId()));
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("exists - 存在返回true")
    void testExists() {
        Sku sku = skuDomainService.createSku("Test", new BigDecimal("100"));
        assertTrue(skuDomainService.exists(sku.getSkuId()));
        assertFalse(skuDomainService.exists("non-existent"));
    }
}
