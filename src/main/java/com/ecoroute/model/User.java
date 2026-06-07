package com.ecoroute.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "email", nullable = true, length = 100)
    private String email;

    /** Multi-tenant FK — every user belongs to exactly one Company (Tenant). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // ── Enum ──────────────────────────────────────────────────────────────────
    public enum Role {
        LOGISTICS_MANAGER,
        AUDITOR,
        SYSTEM_ADMIN
    }

    // ── Constructors ─────────────────────────────────────────────────────────
    public User() {}

    public User(String username, String password, Role role, Company company) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.company = company;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
