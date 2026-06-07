package com.ecoroute.model;

import jakarta.persistence.*;

@Entity
@Table(name = "distance_lookups", uniqueConstraints = {
    @UniqueConstraint(name = "uq_route", columnNames = {"city_a", "city_b"})
})
public class DistanceLookup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city_a", nullable = false, length = 100)
    private String cityA;

    @Column(name = "city_b", nullable = false, length = 100)
    private String cityB;

    @Column(name = "distance_km", nullable = false)
    private Double distanceKm;

    // Constructors
    public DistanceLookup() {}

    public DistanceLookup(String cityA, String cityB, Double distanceKm) {
        this.cityA = cityA;
        this.cityB = cityB;
        this.distanceKm = distanceKm;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCityA() {
        return cityA;
    }

    public void setCityA(String cityA) {
        this.cityA = cityA;
    }

    public String getCityB() {
        return cityB;
    }

    public void setCityB(String cityB) {
        this.cityB = cityB;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }
}
