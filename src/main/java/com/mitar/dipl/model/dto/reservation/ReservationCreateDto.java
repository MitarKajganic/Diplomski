package com.mitar.dipl.model.dto.reservation;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservationCreateDto {

    @NotNull(message = "Table ID is required.")
    @NotEmpty(message = "Table ID cannot be empty.")
    private String tableId;

    @NotNull(message = "Reservation time is required.")
    @Future(message = "Reservation time must be in the future.")
    private LocalDateTime reservationTime;

    @NotNull(message = "Number of guests is required.")
    @Min(value = 1, message = "There must be at least one guest.")
    private Integer numberOfGuests;

    @NotNull(message = "User ID is required.")
    @NotEmpty(message = "User ID cannot be empty.")
    private String userId;

    @NotNull(message = "Guest name is required.")
    @NotEmpty(message = "Guest name cannot be empty.")
    @Size(min = 2, message = "Guest name must be at least 2 characters long.")
    private String guestName;

    @Email(message = "Invalid guest email format.")
    private String guestEmail;

    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid guest phone number format.")
    private String guestPhone;
}
