package domain.dto;

import com.google.gson.annotations.SerializedName;

public record ItemDTO(
        int id,
        @SerializedName("product_id") int productId,
        @SerializedName("product_name") String productName,
        @SerializedName("unit_price") double unitPrice,
        int quantity,
        CategoryDTO category
) {}
