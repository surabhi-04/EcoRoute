package com.ecoroute.repository;

import com.ecoroute.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Fetches the user with its company in a single JOIN query.
     * Used by CustomUserDetailsService to populate EcoUserDetails.
     */
    @Query("SELECT u FROM User u JOIN FETCH u.company WHERE u.username = :username")
    Optional<User> findByUsernameWithCompany(@Param("username") String username);

    boolean existsByUsername(String username);
    java.util.List<User> findByCompanyId(Long companyId);
}
