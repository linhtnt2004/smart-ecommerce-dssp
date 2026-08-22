package com.example.secdsp.modules.ai.tool;

import com.example.secdsp.modules.product.dto.response.ProductDetailResponse;
import com.example.secdsp.modules.product.dto.response.ProductResponse;
import com.example.secdsp.modules.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductAiTool {

    private static final int MAX_RESULTS = 10;
    private static final int FETCH_SIZE = 50;

    private final ProductService productService;

    public List<ProductResponse> searchProducts(
        String keyword,
        BigDecimal minPrice,
        BigDecimal maxPrice
    ) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        if (kw == null && minPrice == null && maxPrice == null) {
            return List.of();
        }

        int fetchSize = (minPrice != null || maxPrice != null) ? FETCH_SIZE : MAX_RESULTS;

        List<ProductResponse> products = productService
            .getProducts(
                kw,
                null,
                null,
                null,
                PageRequest.of(0, fetchSize)
            )
            .getContent();

        return products.stream()
            .filter(p -> minPrice == null || p.getPrice().compareTo(minPrice) >= 0)
            .filter(p -> maxPrice == null || p.getPrice().compareTo(maxPrice) <= 0)
            .limit(MAX_RESULTS)
            .toList();
    }

    public ProductDetailResponse getProductDetail(Long productId) {
        return productService.getProductById(productId);
    }
}