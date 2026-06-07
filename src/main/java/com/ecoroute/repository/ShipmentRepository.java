package com.ecoroute.repository;

import com.ecoroute.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    /**
     * Tenant-isolated query: returns shipments ordered newest-first
     * for the given company only. Absolute data isolation.
     */
    List<Shipment> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    /**
     * Sums total CO2 (kg) for a specific tenant.
     */
    @Query("SELECT COALESCE(SUM(s.co2Emissions), 0.0) FROM Shipment s WHERE s.company.id = :companyId")
    Double sumCo2EmissionsByCompanyId(@Param("companyId") Long companyId);

    /**
     * Returns ALL shipments for a tenant (unordered) — used for savings calc.
     */
    @Query("SELECT s FROM Shipment s WHERE s.company.id = :companyId")
    List<Shipment> findAllByCompanyId(@Param("companyId") Long companyId);
}
