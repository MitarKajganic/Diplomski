package com.mitar.dipl.model.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bills")
@Data
public class Bill {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    private BigDecimal totalAmount;
    private BigDecimal tax;
    private BigDecimal discount;
    private BigDecimal finalAmount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false)
    @JsonManagedReference
    private Order order;

    @PrePersist
    public void calculateFinalAmount() {
        this.finalAmount = this.totalAmount.add(this.tax).subtract(this.discount);
    }

}
