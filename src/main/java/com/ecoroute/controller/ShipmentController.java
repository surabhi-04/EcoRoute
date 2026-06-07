package com.ecoroute.controller;

import com.ecoroute.dto.TeamMemberForm;
import com.ecoroute.security.EcoUserDetails;
import com.ecoroute.service.DistanceService;
import com.ecoroute.service.ShipmentService;
import com.ecoroute.model.Shipment;
import com.ecoroute.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Thymeleaf web controller — all data served here is scoped to the
 * authenticated user's company_id extracted from EcoUserDetails.
 * No cross-tenant data leakage is possible.
 */
@Controller
public class ShipmentController {

    private final ShipmentService  shipmentService;
    private final DistanceService  distanceService;
    private final UserRepository   userRepository;
    private final com.ecoroute.service.AuditLoggingService auditLoggingService;

    public ShipmentController(ShipmentService shipmentService,
                              DistanceService distanceService,
                              UserRepository userRepository,
                              com.ecoroute.service.AuditLoggingService auditLoggingService) {
        this.shipmentService = shipmentService;
        this.distanceService = distanceService;
        this.userRepository  = userRepository;
        this.auditLoggingService = auditLoggingService;
    }

    // ── Landing Page ──────────────────────────────────────────────────────────

    /**
      * GET / — public landing/home page.
      */
    @GetMapping("/")
    public String home() {
        return "home";
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    /**
      * GET /dashboard — main dashboard.
      * Populates Thymeleaf model with tenant-scoped data only.
      */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal EcoUserDetails principal,
                            @RequestParam(value = "tab", required = false) String tab,
                            Model model) {
        Long   companyId   = principal.getCompanyId();
        String companyName = principal.getCompanyName();

        model.addAttribute("activeUser",    principal.getUsername());
        model.addAttribute("activeCompany", companyName);
        model.addAttribute("role",          principal.getRole());
        model.addAttribute("username",      principal.getUsername());
        model.addAttribute("teamForm",      new TeamMemberForm());

        // Determine which tab index to open on load
        int activeTab = 0;
        if ("team".equals(tab))        activeTab = 1;
        if ("compliance".equals(tab))  activeTab = 2;
        model.addAttribute("activeTab", activeTab);

        // Tenant-isolated KPIs
        model.addAttribute("totalShipments",   shipmentService.getShipmentCountByCompany(companyId));
        model.addAttribute("totalCo2",         shipmentService.getTotalEmissionsTonsByCompany(companyId));
        model.addAttribute("totalEmissionsTons", String.format("%.2f", shipmentService.getTotalEmissionsTonsByCompany(companyId)));
        model.addAttribute("totalSavingsTons", String.format("%.2f", shipmentService.getNetSavingsTonsByCompany(companyId)));

        // Tenant-filtered shipment log
        model.addAttribute("shipments", shipmentService.getShipmentsByCompany(companyId));

        // Team members — used by the inline Manager Panel tab
        model.addAttribute("teamMembers", userRepository.findByCompanyId(companyId));

        // City dropdowns
        model.addAttribute("cities", distanceService.getAllUniqueCities());

        return "dashboard";
    }

    // ── Shipment Create (LOGISTICS_MANAGER only) ──────────────────────────────

    @PostMapping("/shipments/add")
    public String addShipment(@AuthenticationPrincipal EcoUserDetails principal,
                              @RequestParam String trackingNumber,
                              @RequestParam Double weightTons,
                              @RequestParam String transportMode,
                              @RequestParam String originCity,
                              @RequestParam String destinationCity,
                              RedirectAttributes redirectAttributes) {
        try {
            shipmentService.createShipment(
                    trackingNumber, weightTons, transportMode,
                    originCity, destinationCity,
                    principal.getCompanyId()   // ← companyId from security context, never from request
            );

            auditLoggingService.log("SHIPMENT_LOG", 
                "Logged transit '" + trackingNumber + "' (Cargo: " + weightTons + " T, Mode: " + transportMode + ") from " + originCity + " to " + destinationCity + ".", 
                principal.getUsername(), principal.getCompanyName());

            redirectAttributes.addFlashAttribute("success",
                    "Shipment " + trackingNumber + " logged for " + principal.getCompanyName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }

    // ── Compliance Certificate (AUDITOR only) ─────────────────────────────────

    @GetMapping("/certificate/{id}")
    public String certificate(@PathVariable Long id,
                              @AuthenticationPrincipal EcoUserDetails principal,
                              Model model) {
        Optional<Shipment> opt = shipmentService.getShipmentById(id);
        if (opt.isEmpty()) {
            return "redirect:/dashboard";
        }
        Shipment s = opt.get();

        // Tenant guard — auditor cannot view another tenant's certificate
        if (!s.getCompany().getId().equals(principal.getCompanyId())) {
            return "redirect:/dashboard?error=Access+Denied";
        }

        double distKm     = distanceService.getDistanceBetween(s.getOriginCity(), s.getDestinationCity());
        double emissKg    = s.getCo2Emissions();
        double airCo2Kg   = s.getWeightTons() * distKm * 0.50;
        double savingsKg  = Math.max(0, airCo2Kg - emissKg);

        model.addAttribute("shipment",      s);
        model.addAttribute("activeCompany", principal.getCompanyName());
        model.addAttribute("distanceKm",    String.format("%.1f", distKm));
        model.addAttribute("emissionsKg",   String.format("%.2f", emissKg));
        model.addAttribute("savingsKg",     String.format("%.2f", savingsKg));
        model.addAttribute("certId",        "CERT-" + s.getCreatedAt().getYear()
                                             + "-" + s.getTrackingNumber());

        return "certificate";
    }
}
