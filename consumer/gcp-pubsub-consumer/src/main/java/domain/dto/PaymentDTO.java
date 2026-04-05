package domain.dto;

import com.google.gson.annotations.SerializedName;

public record PaymentDTO(
        String method,
        String status,
        @SerializedName("transaction_id") String transactionId
) {}
