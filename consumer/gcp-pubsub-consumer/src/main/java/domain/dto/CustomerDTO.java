package domain.dto;

public record CustomerDTO(
        int id,
        String name,
        String email,
        String document
){}
