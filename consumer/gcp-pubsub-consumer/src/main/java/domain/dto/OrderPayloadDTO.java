package domain.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public record OrderPayloadDTO(
        String uuid,
        @SerializedName("created_at") String createdAt,
        String channel,
        String status,
        CustomerDTO customer,
        SellerDTO seller,
        List<ItemDTO> items,
        ShipmentDTO shipment,
        PaymentDTO payment,
        MetadataDTO metadata
) {}
