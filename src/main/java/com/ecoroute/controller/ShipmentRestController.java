package com.ecoroute.controller;

import com.ecoroute.security.EcoUserDetails;
import com.ecoroute.service.DistanceService;
import com.ecoroute.service.ShipmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Legacy REST controller — preserved for backwards compatibility.
 * All new integrations should use /api/v1/ instead.
 */
@RestController
@RequestMapping("/api")
public class ShipmentRestController {

    private final ShipmentService shipmentService;
    private final DistanceService distanceService;

    public ShipmentRestController(ShipmentService shipmentService,
                                  DistanceService distanceService) {
        this.shipmentService = shipmentService;
        this.distanceService = distanceService;
    }

    @GetMapping("/shipments")
    public ResponseEntity<?> listShipments(@AuthenticationPrincipal EcoUserDetails principal) {
        return ResponseEntity.ok(shipmentService.getShipmentsByCompany(principal.getCompanyId()));
    }

    @PostMapping("/shipments")
    public ResponseEntity<?> createShipment(@AuthenticationPrincipal EcoUserDetails principal,
                                            @RequestBody Map<String, Object> body) {
        try {
            var s = shipmentService.createShipment(
                    (String) body.get("trackingNumber"),
                    Double.parseDouble(body.get("weightTons").toString()),
                    (String) body.get("transportMode"),
                    (String) body.get("originCity"),
                    (String) body.get("destinationCity"),
                    principal.getCompanyId()
            );
            return ResponseEntity.ok(Map.of("status", "created", "id", s.getId(),
                    "co2EmissionsKg", s.getCo2Emissions()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/routes/distance")
    public ResponseEntity<?> getDistance(@RequestParam String origin,
                                         @RequestParam String destination) {
        try {
            return ResponseEntity.ok(Map.of(
                    "origin", origin, "destination", destination,
                    "distanceKm", distanceService.getDistanceBetween(origin, destination)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
