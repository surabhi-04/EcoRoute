package com.ecoroute.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_number", nullable = false, unique = true, length = 50)
    private String trackingNumber;

    @Column(name = "weight_tons", nullable = false)
    private Double weightTons;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_mode", nullable = false, length = 15)
    private TransportMode transportMode;

    @Column(name = "origin_city", nullable = false, length = 100)
    private String originCity;

    @Column(name = "destination_city", nullable = false, length = 100)
    private String destinationCity;

    @Column(name = "co2_emissions", nullable = false)
    private Double co2Emissions;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Multi-tenant FK — every shipment is scoped to one Company (Tenant). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // ── Enum ──────────────────────────────────────────────────────────────────
    public enum TransportMode {
        AIR, SEA, RAIL, ROAD;

        /** Emission factor in kg CO₂ per Ton·km */
        public double getFactor() {
            return switch (this) {
                case AIR  -> 0.50;
                case ROAD -> 0.12;
                case RAIL -> 0.03;
                case SEA  -> 0.01;
            };
        }
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ── Constructors ─────────────────────────────────────────────────────────
    public Shipment() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public Double getWeightTons() { return weightTons; }
    public void setWeightTons(Double weightTons) { this.weightTons = weightTons; }

    public TransportMode getTransportMode() { return transportMode; }
    public void setTransportMode(TransportMode transportMode) { this.transportMode = transportMode; }

    public String getOriginCity() { return originCity; }
    public void setOriginCity(String originCity) { this.originCity = originCity; }

    public String getDestinationCity() { return destinationCity; }
    public void setDestinationCity(String destinationCity) { this.destinationCity = destinationCity; }

    public Double getCo2Emissions() { return co2Emissions; }
    public void setCo2Emissions(Double co2Emissions) { this.co2Emissions = co2Emissions; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
}
