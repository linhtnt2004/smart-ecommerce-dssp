package com.example.secdsp.modules.ai.tool;

import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;

import java.util.List;
import java.util.Map;

public final class ProductToolDefinition {

    private ProductToolDefinition() {
    }

    public static FunctionDeclaration searchProducts() {
        return FunctionDeclaration.builder()
            .name("search_products")
            .description("""
                Search and filter products on the e-commerce platform.
                Use this tool when the user asks to find, search, browse,
                or recommend products.
                - To find products under a price: set maxPrice
                  (e.g. 1000000 for under 1 million VND).
                - To find products above a price: set minPrice.
                - If the user only filters by price (not by name),
                  pass keyword as empty string "".
                """)
            .parameters(
                Schema.builder()
                    .type("OBJECT")
                    .properties(
                        Map.of(
                            "keyword",
                            Schema.builder()
                                .type("STRING")
                                .description(
                                    "Product name or search keyword. "
                                        + "Use empty string \"\" when filtering by price only."
                                )
                                .build(),
                            "minPrice",
                            Schema.builder()
                                .type("NUMBER")
                                .description("Minimum price in VND (inclusive). Optional.")
                                .build(),
                            "maxPrice",
                            Schema.builder()
                                .type("NUMBER")
                                .description("Maximum price in VND (inclusive). Optional.")
                                .build()
                        )
                    )
                    .required(List.of("keyword"))
                    .build()
            )
            .build();
    }

    public static FunctionDeclaration getProductDetail() {
        return FunctionDeclaration.builder()
            .name("get_product_detail")
            .description("""
                Get detailed information about a specific product.
                Use this tool when the user asks about a product's
                price, description, stock, seller, images, or attributes.
                """)
            .parameters(
                Schema.builder()
                    .type("OBJECT")
                    .properties(
                        Map.of(
                            "productId",
                            Schema.builder()
                                .type("INTEGER")
                                .description("Product identifier")
                                .build()
                        )
                    )
                    .required(List.of("productId"))
                    .build()
            )
            .build();
    }
}