package com.mitar.dipl.service.implementation;

import com.mitar.dipl.mapper.ReservationMapper;
import com.mitar.dipl.model.dto.reservation.ReservationCreateDto;
import com.mitar.dipl.model.entity.Reservation;
import com.mitar.dipl.repository.ReservationRepository;
import com.mitar.dipl.service.ReservationService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@AllArgsConstructor
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationMapper reservationMapper;
    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);

    // Business hours constants
    private static final LocalTime OPENING_TIME = LocalTime.of(10, 0);
    private static final LocalTime CLOSING_TIME = LocalTime.of(22, 0);
    private static final Duration BUFFER_DURATION = Duration.ofMinutes(30);
    private static final Duration RESERVATION_DURATION = Duration.ofHours(2);

    @Override
    public ResponseEntity<?> getAllReservations() {
        return ResponseEntity.status(HttpStatus.OK).body(reservationRepository.findAll());
    }

    @Override
    public ResponseEntity<?> getAllIncludingDeleted() {
        return ResponseEntity.status(HttpStatus.OK).body(reservationRepository.findAllIncludingDeleted());
    }

    @Override
    public ResponseEntity<?> getReservationById(String reservationId) {
        Optional<Reservation> reservation = reservationRepository.findById(UUID.fromString(reservationId));
        if (reservation.isEmpty()) {
            logger.warn("Reservation not found with ID: {}", reservationId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Reservation not found.");
        }
        return ResponseEntity.status(HttpStatus.OK).body(reservationMapper.toDto(reservation.get()));
    }

    @Override
    public ResponseEntity<?> getReservationsByUserId(String userId) {
        return ResponseEntity.status(HttpStatus.OK).body(reservationRepository.findAllByUser_Id(UUID.fromString(userId)));
    }

    @Override
    public ResponseEntity<?> getReservationsByTableId(String tableId) {
        return ResponseEntity.status(HttpStatus.OK).body(reservationRepository.findAllByTable_Id(UUID.fromString(tableId)));
    }

    @Override
    public ResponseEntity<?> getReservationsByGuestName(String guestName) {
        if (isNullOrEmpty(guestName)) {
            logger.warn("Guest name is empty or null.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Guest name cannot be empty.");
        }
        return ResponseEntity.status(HttpStatus.OK).body(reservationRepository.findAllByGuestName(guestName));
    }

    @Override
    public ResponseEntity<?> getReservationsByGuestEmail(String guestEmail) {
        if (isNullOrEmpty(guestEmail) || isInvalidEmail(guestEmail)) {
            logger.warn("Invalid guest email: {}", guestEmail);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Valid guest email is required.");
        }
        return ResponseEntity.status(HttpStatus.OK).body(reservationRepository.findAllByGuestEmail(guestEmail));
    }

    @Override
    public ResponseEntity<?> getReservationsByGuestPhone(String guestPhone) {
        if (isNullOrEmpty(guestPhone) || isInvalidPhone(guestPhone)) {
            logger.warn("Invalid guest phone: {}", guestPhone);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Valid guest phone number is required.");
        }
        return ResponseEntity.status(HttpStatus.OK).body(reservationRepository.findAllByGuestPhone(guestPhone));
    }

    @Override
    public ResponseEntity<?> createReservation(ReservationCreateDto reservationCreateDto) {
        logger.info("Attempting to create reservation with data: {}", reservationCreateDto);

        Reservation reservation = reservationMapper.toEntity(reservationCreateDto);

        String validationError = validateReservationData(reservation);
        if (validationError != null) {
            logger.warn("Validation failed: {}", validationError);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationError);
        }

        if (isOverlappingReservation(reservation)) {
            logger.warn("Table is already reserved at: {}", reservation.getReservationTime());
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Table is already reserved at that time.");
        }

        if (reservation.getUser() != null) {
            if (hasUserExistingReservation(reservation)) {
                logger.warn("User already has a reservation at: {}", reservation.getReservationTime());
                return ResponseEntity.status(HttpStatus.CONFLICT).body("User already has a reservation at this time.");
            }
        } else {
            if (hasGuestExistingReservation(reservation)) {
                logger.warn("Guest with email {} already has a reservation at: {}", reservation.getGuestEmail(), reservation.getReservationTime());
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Guest with this email already has a reservation at this time.");
            }
        }

        Reservation savedReservation = reservationRepository.save(reservation);
        logger.info("Reservation created successfully: {}", savedReservation.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(reservationMapper.toDto(savedReservation));
    }

    @Override
    public ResponseEntity<?> deleteReservation(String reservationId) {
        Optional<Reservation> reservation = reservationRepository.findById(UUID.fromString(reservationId));
        if (reservation.isEmpty()) {
            logger.warn("Reservation not found with ID: {}", reservationId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Reservation not found.");
        }
        reservationRepository.delete(reservation.get());
        logger.info("Reservation deleted successfully: {}", reservationId);
        return ResponseEntity.status(HttpStatus.OK).body("Reservation deleted successfully.");
    }

    @Override
    public ResponseEntity<?> updateReservation(String reservationId, ReservationCreateDto reservationCreateDto) {
        logger.info("Attempting to update reservation with ID: {} using data: {}", reservationId, reservationCreateDto);

        UUID uuid;
        try {
            uuid = UUID.fromString(reservationId);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid reservation ID format: {}", reservationId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid reservation ID format.");
        }

        Optional<Reservation> optionalReservation = reservationRepository.findById(uuid);
        if (optionalReservation.isEmpty()) {
            logger.warn("Reservation not found with ID: {}", reservationId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Reservation not found.");
        }

        Reservation existingReservation = optionalReservation.get();

        reservationMapper.updateEntityFromDto(reservationCreateDto, existingReservation);

        String validationError = validateReservationData(existingReservation);
        if (validationError != null) {
            logger.warn("Validation failed during update: {}", validationError);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationError);
        }

        if (isOverlappingReservation(existingReservation, uuid)) {
            logger.warn("Table is already reserved at: {}", existingReservation.getReservationTime());
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Table is already reserved at that time.");
        }

        if (existingReservation.getUser() != null) {
            if (hasUserExistingReservation(existingReservation, uuid)) {
                logger.warn("User already has a reservation at: {}", existingReservation.getReservationTime());
                return ResponseEntity.status(HttpStatus.CONFLICT).body("User already has a reservation at this time.");
            }
        } else {
            if (hasGuestExistingReservation(existingReservation, uuid)) {
                logger.warn("Guest with email {} already has a reservation at: {}", existingReservation.getGuestEmail(), existingReservation.getReservationTime());
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Guest with this email already has a reservation at this time.");
            }
        }

        Reservation updatedReservation = reservationRepository.save(existingReservation);
        logger.info("Reservation updated successfully: {}", updatedReservation.getId());

        return ResponseEntity.status(HttpStatus.OK).body(reservationMapper.toDto(updatedReservation));
    }

    // ------------------- Private Validation Methods -------------------

    /**
     * Validates the reservation data.
     *
     * @param reservation the reservation entity
     * @return error message if validation fails, otherwise null
     */
    private String validateReservationData(Reservation reservation) {
        LocalDate reservationDate = reservation.getReservationTime().toLocalDate();
        LocalDate today = LocalDate.now();

        if (!reservationDate.isAfter(today)) {
            return "Reservation time must be at least the next day.";
        }

        LocalTime reservationStart = reservation.getReservationTime().toLocalTime();
        LocalTime reservationEnd = reservationStart.plus(RESERVATION_DURATION).plusMinutes(BUFFER_DURATION.toMinutes());

        if (reservationStart.isBefore(OPENING_TIME) || reservationEnd.isAfter(CLOSING_TIME)) {
            return "Reservation time is outside business hours.";
        }

        // Additional validations can be added here (e.g., maximum capacity of the table)

        return null;
    }

    /**
     * Checks if the reservation overlaps with existing reservations.
     *
     * @param reservation the reservation to check
     * @return true if overlapping exists, otherwise false
     */
    private boolean isOverlappingReservation(Reservation reservation) {
        UUID tableId = reservation.getTable().getId();
        LocalDateTime requestedStart = reservation.getReservationTime();
        LocalDateTime requestedEnd = requestedStart.plus(RESERVATION_DURATION).plus(BUFFER_DURATION);

        // Check for overlapping reservations within the reservation duration and buffer
        return reservationRepository.existsByTable_IdAndReservationTimeBetween(
                tableId,
                requestedStart.minus(RESERVATION_DURATION).minus(BUFFER_DURATION),
                requestedEnd
        );
    }

    /**
     * Checks if the reservation overlaps with existing reservations, excluding a specific reservation ID.
     *
     * @param reservation the reservation to check
     * @param excludeResId the reservation ID to exclude from the check
     * @return true if overlapping exists, otherwise false
     */
    private boolean isOverlappingReservation(Reservation reservation, UUID excludeResId) {
        UUID tableId = reservation.getTable().getId();
        LocalDateTime requestedStart = reservation.getReservationTime();
        LocalDateTime requestedEnd = requestedStart.plus(RESERVATION_DURATION).plus(BUFFER_DURATION);

        // Check for overlapping reservations within the reservation duration and buffer, excluding current reservation
        return reservationRepository.existsByTable_IdAndReservationTimeBetweenAndIdNot(
                tableId,
                requestedStart.minus(RESERVATION_DURATION).minus(BUFFER_DURATION),
                requestedEnd,
                excludeResId
        );
    }

    /**
     * Checks if the user already has a reservation at the specified time.
     *
     * @param reservation the reservation entity
     * @return true if the user has an existing reservation at the time, otherwise false
     */
    private boolean hasUserExistingReservation(Reservation reservation) {
        UUID userId = reservation.getUser().getId();
        LocalDateTime reservationTime = reservation.getReservationTime();
        return reservationRepository.existsByUser_IdAndReservationTime(userId, reservationTime);
    }

    /**
     * Checks if the user already has a reservation at the specified time, excluding a specific reservation ID.
     *
     * @param reservation the reservation entity
     * @param excludeResId the reservation ID to exclude from the check
     * @return true if the user has an existing reservation at the time, otherwise false
     */
    private boolean hasUserExistingReservation(Reservation reservation, UUID excludeResId) {
        UUID userId = reservation.getUser().getId();
        LocalDateTime reservationTime = reservation.getReservationTime();
        return reservationRepository.existsByUser_IdAndReservationTimeAndIdNot(userId, reservationTime, excludeResId);
    }

    /**
     * Checks if the guest already has a reservation at the specified time.
     *
     * @param reservation the reservation entity
     * @return true if the guest has an existing reservation at the time, otherwise false
     */
    private boolean hasGuestExistingReservation(Reservation reservation) {
        String guestEmail = reservation.getGuestEmail();
        LocalDateTime reservationTime = reservation.getReservationTime();
        return reservationRepository.existsByGuestEmailAndReservationTime(guestEmail, reservationTime);
    }

    /**
     * Checks if the guest already has a reservation at the specified time, excluding a specific reservation ID.
     *
     * @param reservation the reservation entity
     * @param excludeResId the reservation ID to exclude from the check
     * @return true if the guest has an existing reservation at the time, otherwise false
     */
    private boolean hasGuestExistingReservation(Reservation reservation, UUID excludeResId) {
        String guestEmail = reservation.getGuestEmail();
        LocalDateTime reservationTime = reservation.getReservationTime();
        return reservationRepository.existsByGuestEmailAndReservationTimeAndIdNot(guestEmail, reservationTime, excludeResId);
    }

    // ------------------- Private Helper Methods -------------------

    /**
     * Checks if a string is null or empty after trimming.
     *
     * @param value the string to check
     * @return true if null or empty, otherwise false
     */
    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Validates the format of an email address.
     *
     * @param email the email to validate
     * @return true if invalid, otherwise false
     */
    private boolean isInvalidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return !Pattern.compile(emailRegex).matcher(email).matches();
    }

    /**
     * Validates the format of a phone number.
     *
     * @param phone the phone number to validate
     * @return true if invalid, otherwise false
     */
    private boolean isInvalidPhone(String phone) {
        String phoneRegex = "^\\+?[0-9]{7,15}$";
        return !Pattern.compile(phoneRegex).matcher(phone).matches();
    }
}
