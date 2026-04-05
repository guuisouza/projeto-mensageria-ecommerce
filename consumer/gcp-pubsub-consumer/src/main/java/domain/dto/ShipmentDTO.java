package domain.dto;

import com.google.gson.annotations.SerializedName;

public record ShipmentDTO(
        String carrier,
        String service,
        String status,
        @SerializedName("tracking_code") String trackingCode
) {}
