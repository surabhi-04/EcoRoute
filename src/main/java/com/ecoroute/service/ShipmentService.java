package com.ecoroute.service;

import com.ecoroute.model.Company;
import com.ecoroute.model.Shipment;
import com.ecoroute.model.Shipment.TransportMode;
import com.ecoroute.repository.CompanyRepository;
import com.ecoroute.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Core business logic service.
 * All public methods are tenant-scoped: they accept a companyId parameter
 * extracted from EcoUserDetails in the security context, never from client input.
 */
@Service
public class ShipmentService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentService.class);

    /** AIR emission factor — used as the baseline for net-savings calculations. */
    private static final double AIR_FACTOR = 0.50;

    private final ShipmentRepository shipmentRepository;
    private final CompanyRepository  companyRepository;
    private final DistanceService    distanceService;

    public ShipmentService(ShipmentRepository shipmentRepository,
                           CompanyRepository companyRepository,
                           DistanceService distanceService) {
        this.shipmentRepository = shipmentRepository;
        this.companyRepository  = companyRepository;
        this.distanceService    = distanceService;
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Creates and persists a new shipment scoped to the given tenant.
     * Distance is resolved via JdbcTemplate (Dijkstra fallback).
     * CO₂ = weightTons × distanceKm × transportModeFactor.
     */
    @Transactional
    public Shipment createShipment(String trackingNumber,
                                   Double weightTons,
                                   String transportModeStr,
                                   String originCity,
                                   String destinationCity,
                                   Long   companyId) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));

        TransportMode mode = TransportMode.valueOf(transportModeStr.toUpperCase());

        // JdbcTemplate-backed distance lookup (Dijkstra multi-hop fallback)
        double distanceKm = distanceService.getDistanceBetween(originCity, destinationCity);

        // Core emission formula
        double co2 = weightTons * distanceKm * mode.getFactor();

        log.info("Shipment [{}] | Company: {} | Route: {} → {} | {}km | {}T | Mode: {} | CO₂: {:.2f}kg",
                trackingNumber, company.getName(), originCity, destinationCity,
                distanceKm, weightTons, mode, co2);

        Shipment shipment = new Shipment();
        shipment.setTrackingNumber(trackingNumber);
        shipment.setWeightTons(weightTons);
        shipment.setTransportMode(mode);
        shipment.setOriginCity(originCity);
        shipment.setDestinationCity(destinationCity);
        shipment.setCo2Emissions(co2);
        shipment.setCompany(company);

        return shipmentRepository.save(shipment);
    }

    // ── Read (tenant-isolated) ────────────────────────────────────────────────

    /** Returns all shipments for a tenant, newest first. */
    @Transactional(readOnly = true)
    public List<Shipment> getShipmentsByCompany(Long companyId) {
        return shipmentRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
    }

    /** Sums gross CO₂ (kg → metric tons) emitted by a tenant. */
    @Transactional(readOnly = true)
    public double getTotalEmissionsTonsByCompany(Long companyId) {
        Double totalKg = shipmentRepository.sumCo2EmissionsByCompanyId(companyId);
        return (totalKg == null ? 0.0 : totalKg) / 1000.0;
    }

    /**
     * Calculates net carbon savings vs. equivalent AIR transport
     * for all shipments belonging to a tenant.
     * Savings = Σ max(0, (weight × dist × AIR_FACTOR) − actual_co2)
     */
    @Transactional(readOnly = true)
    public double getNetSavingsTonsByCompany(Long companyId) {
        List<Shipment> all = shipmentRepository.findAllByCompanyId(companyId);
        double savingsKg = all.stream().mapToDouble(s -> {
            double distKm = distanceService.getDistanceBetween(s.getOriginCity(), s.getDestinationCity());
            double airCo2 = s.getWeightTons() * distKm * AIR_FACTOR;
            return Math.max(0.0, airCo2 - s.getCo2Emissions());
        }).sum();
        return savingsKg / 1000.0;
    }

    /** Returns total shipment count for a tenant. */
    @Transactional(readOnly = true)
    public long getShipmentCountByCompany(Long companyId) {
        return shipmentRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).size();
    }

    /** Fetches a single shipment by id — used for certificate generation. */
    @Transactional(readOnly = true)
    public Optional<Shipment> getShipmentById(Long id) {
        return shipmentRepository.findById(id);
    }

    /** Helper: exposed for certificate CO₂ display. */
    public double getDistanceKm(String origin, String dest) {
        return distanceService.getDistanceBetween(origin, dest);
    }
}
