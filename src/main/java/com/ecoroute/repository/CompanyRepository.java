package com.ecoroute.repository;

import com.ecoroute.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByName(String name);
    boolean existsByName(String name);
    java.util.List<Company> findByOnboardingStatus(Company.OnboardingStatus onboardingStatus);
}
