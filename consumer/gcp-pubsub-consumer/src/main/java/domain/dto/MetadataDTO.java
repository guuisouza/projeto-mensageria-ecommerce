package domain.dto;

import com.google.gson.annotations.SerializedName;

public record MetadataDTO(
        String source,
        @SerializedName("user_agent") String userAgent,
        @SerializedName("ip_address") String ipAddress
) {}
