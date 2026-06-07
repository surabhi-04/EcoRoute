package com.ecoroute.controller;

import com.ecoroute.model.Shipment;
import com.ecoroute.security.EcoUserDetails;
import com.ecoroute.service.ShipmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Tenant-Isolated REST API v1
 *
 * All responses are automatically filtered by the logged-in user's company_id.
 * Cross-tenant data access is structurally impossible — the companyId is never
 * accepted from the request body; it is always extracted from EcoUserDetails.
 *
 * Supports both session-based (form login) and HTTP Basic Auth for API clients.
 */
@RestController
@RequestMapping("/api/v1")
public class ShipmentApiController {

    private final ShipmentService shipmentService;

    public ShipmentApiController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    // ── GET /api/v1/shipments ─────────────────────────────────────────────────
    // Accessible by: LOGISTICS_MANAGER, AUDITOR
    // Returns: tenant-filtered shipment list + KPI metrics

    @GetMapping("/shipments")
    public ResponseEntity<Map<String, Object>> getShipments(
            @AuthenticationPrincipal EcoUserDetails principal) {

        Long companyId = principal.getCompanyId();

        List<Shipment> shipments = shipmentService.getShipmentsByCompany(companyId);

        // Map to lightweight DTO to avoid lazy-loading issues on JSON serialization
        List<Map<String, Object>> payload = shipments.stream().map(s -> Map.<String, Object>of(
                "id",               s.getId(),
                "trackingNumber",   s.getTrackingNumber(),
                "originCity",       s.getOriginCity(),
                "destinationCity",  s.getDestinationCity(),
                "transportMode",    s.getTransportMode().name(),
                "weightTons",       s.getWeightTons(),
                "co2EmissionsKg",   s.getCo2Emissions(),
                "createdAt",        s.getCreatedAt().toString()
        )).toList();

        return ResponseEntity.ok(Map.of(
                "company",          principal.getCompanyName(),
                "totalShipments",   shipments.size(),
                "totalCo2Tons",     shipmentService.getTotalEmissionsTonsByCompany(companyId),
                "netSavingsTons",   shipmentService.getNetSavingsTonsByCompany(companyId),
                "shipments",        payload
        ));
    }

    // ── POST /api/v1/shipments ────────────────────────────────────────────────
    // Accessible by: LOGISTICS_MANAGER only
    // The company_id is ALWAYS injected from the security principal — never trusted from the request body.

    @PostMapping("/shipments")
    public ResponseEntity<Map<String, Object>> createShipment(
            @AuthenticationPrincipal EcoUserDetails principal,
            @RequestBody Map<String, Object> body) {

        try {
            String trackingNumber  = (String)  body.get("trackingNumber");
            Double weightTons      = Double.parseDouble(body.get("weightTons").toString());
            String transportMode   = (String)  body.get("transportMode");
            String originCity      = (String)  body.get("originCity");
            String destinationCity = (String)  body.get("destinationCity");

            if (trackingNumber == null || transportMode == null
                    || originCity == null || destinationCity == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "trackingNumber, weightTons, transportMode, originCity, destinationCity are required"));
            }

            Shipment saved = shipmentService.createShipment(
                    trackingNumber, weightTons, transportMode,
                    originCity, destinationCity,
                    principal.getCompanyId()  // ← tenant isolation enforced here
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "status",          "created",
                    "company",         principal.getCompanyName(),
                    "id",              saved.getId(),
                    "trackingNumber",  saved.getTrackingNumber(),
                    "co2EmissionsKg",  saved.getCo2Emissions(),
                    "transportMode",   saved.getTransportMode().name(),
                    "createdAt",       saved.getCreatedAt().toString()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    // ── GET /api/v1/routes/distance ───────────────────────────────────────────
    // Accessible by: all authenticated users
    // Returns: resolved distance (JdbcTemplate + Dijkstra)

    @GetMapping("/routes/distance")
    public ResponseEntity<Map<String, Object>> getDistance(
            @RequestParam String origin,
            @RequestParam String destination,
            @AuthenticationPrincipal EcoUserDetails principal) {

        try {
            double km = shipmentService.getDistanceKm(origin, destination);
            return ResponseEntity.ok(Map.of(
                    "origin",      origin,
                    "destination", destination,
                    "distanceKm",  km,
                    "resolvedFor", principal.getCompanyName()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/v1/dashboard ─────────────────────────────────────────────────
    // Returns tenant KPI summary

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(
            @AuthenticationPrincipal EcoUserDetails principal) {

        Long companyId = principal.getCompanyId();
        return ResponseEntity.ok(Map.of(
                "activeUser",     principal.getUsername(),
                "activeCompany",  principal.getCompanyName(),
                "role",           principal.getRole(),
                "totalShipments", shipmentService.getShipmentCountByCompany(companyId),
                "totalCo2Tons",   shipmentService.getTotalEmissionsTonsByCompany(companyId),
                "netSavingsTons", shipmentService.getNetSavingsTonsByCompany(companyId)
        ));
    }
}
