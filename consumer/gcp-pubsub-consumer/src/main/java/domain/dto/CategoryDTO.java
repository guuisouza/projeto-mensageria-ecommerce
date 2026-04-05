package domain.dto;

import com.google.gson.annotations.SerializedName;

public record CategoryDTO(
        String id,
        String name,
        @SerializedName("sub_category") SubCategoryDTO subCategory
) {}