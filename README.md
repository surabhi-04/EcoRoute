# EcoRoute — Green Logistics & Multi-Tenant Compliance Tracking System

EcoRoute is a Java Spring Boot B2B SaaS web application for tracking shipments, auditing greenhouse gas emissions, and issuing print-ready Environmental Compliance Certificates. It is designed as an enterprise-grade multi-tenant compliance platform with three separate role-based workflows — System Administrators, Logistics Managers, and Compliance Auditors.

The platform isolates all data at the tenant level. Logistics Managers log transit shipments between cities, and the system automatically calculates the CO₂ footprint using cargo weight, distance, and transport mode. Compliance Auditors can audit their company's emission records and print certificates. System Administrators manage tenant onboardings and track security and operational events via a central Audit Trail.

---

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.2.5
- **Frontend:** HTML5, Thymeleaf, Tailwind CSS (dynamic configurations & class-based light/dark transitions)
- **Database:** MySQL 8.0
- **ORM:** Spring Data JPA / Hibernate 6
- **Direct Queries:** Spring JdbcTemplate (bypasses Hibernate L2 cache for distance lookups)
- **Build Tool:** Apache Maven 3.9.6 (bundled locally)
- **Authentication:** Form-based login with BCrypt password hashing (Spring Security 6)
- **REST API:** Spring `@RestController` (JSON endpoints for integration)
- **Tests:** JUnit 5, Mockito, Spring MockMvc

---

## Project Location

```text
C:\Users\surab\OneDrive\Desktop\EcoRoute
```

---

## Main Modules

### 1. System Administrator Module
System Administrators (`ROLE_SYSTEM_ADMIN`) manage the central infrastructure terminal at `/admin/dashboard` to:
- Monitor corporate metrics (Pending Registrations, Active Tenants, Rejected Entities).
- Approve or reject pending tenant company applications.
- Track real-time events across all tenants using the **Platform Audit Trail**.

### 2. Logistics Manager Module
Logistics Managers (`ROLE_LOGISTICS_MANAGER`) control operations for their specific company:
- View live tenant-isolated KPIs (Total Shipments, Gross CO₂ Emitted, Net Carbon Savings).
- Log new transit shipments using auto-calculating transport mode forms.
- Provision new sub-user accounts (Managers or Auditors) bound to their exact `company_id`.

### 3. Compliance Auditor Module
Compliance Auditors (`ROLE_AUDITOR`) have read-only access to their company's workspace:
- Audit company emission logs on the dashboard.
- Generate and download print-ready **Environmental Compliance Certificates**.
- Print certificates directly using native browser print styles.

---

## Emission Factors & Formulas

| Transport Mode | Factor (kg CO₂ per Ton·km) |
|---|---|
| AIR | 0.50 |
| ROAD | 0.12 |
| RAIL | 0.03 |
| SEA | 0.01 |

### CO₂ Emissions
```text
CO₂ Emissions (kg) = Weight (Tons) × Distance (km) × Emission Factor
```

### Net Carbon Savings
Savings are computed by comparing actual emissions against an equivalent AIR transport baseline:
```text
Savings (kg) = (Weight × Distance × 0.50) − Actual CO₂ Emissions
```
*Note: If the transport mode is AIR itself, savings are recorded as 0.*

---

## Distance Resolution Logic

The `distance_lookups` table stores direct city-pair distances. When a route has no direct entry, the system runs **Dijkstra's shortest-path algorithm** in memory over the full city graph to resolve the optimal route.

*Example:*
`Chennai → Mysuru` has no direct entry. 
The system resolves: `Chennai → Bengaluru` (350.2 km) + `Bengaluru → Mysuru` (145.0 km) = `495.2 km`.

To ensure fresh, real-time results, distance queries bypass Hibernate's Level-2 cache using `JdbcTemplate`.

---

## Folder Structure

```text
EcoRoute/
├── pom.xml
├── Dockerfile
├── README.md
├── apache-maven-3.9.6/              ← bundled Maven binary
└── src/
    ├── main/
    │   ├── java/com/ecoroute/
    │   │   ├── EcoRouteApplication.java
    │   │   ├── config/
    │   │   │   └── SecurityConfig.java
    │   │   ├── controller/
    │   │   │   ├── AdminController.java
    │   │   │   ├── AuthController.java
    │   │   │   ├── ShipmentController.java
    │   │   │   ├── ShipmentRestController.java
    │   │   │   ├── TeamManagementController.java
    │   │   │   └── TenantRegistrationController.java
    │   │   ├── model/
    │   │   │   ├── AuditLog.java        ← [NEW]
    │   │   │   ├── Company.java
    │   │   │   ├── DistanceLookup.java
    │   │   │   ├── Shipment.java
    │   │   │   └── User.java
    │   │   ├── repository/
    │   │   │   ├── AuditLogRepository.java ← [NEW]
    │   │   │   ├── CompanyRepository.java
    │   │   │   ├── ShipmentRepository.java
    │   │   │   └── UserRepository.java
    │   │   └── service/
    │   │       ├── AuditLoggingService.java ← [NEW]
    │   │       ├── CustomUserDetailsService.java
    │   │       ├── DistanceService.java
    │   │       └── ShipmentService.java
    │   └── resources/
    │       ├── application.properties   ← preserves data (ddl-auto=update)
    │       ├── data.sql                 ← seeds with INSERT IGNORE
    │       └── templates/
    │           ├── admin-dashboard.html
    │           ├── admin-login.html
    │           ├── certificate.html
    │           ├── dashboard.html       ← tabbed manager/auditor views
    │           ├── home.html
    │           ├── login.html
    │           ├── register.html
    │           └── team_management.html
    └── test/
        └── java/com/ecoroute/
            ├── controller/
            │   ├── ShipmentRestControllerTest.java
            │   └── TenantOnboardingTest.java
            └── service/
                └── DistanceServiceTest.java
```

---

## Default Login Credentials

The database is pre-seeded with these credentials (passwords are BCrypt hashed as `password`):

| Scope | Username | Password | Role |
|---|---|---|---|
| Central Platform | `admin` | `password` | SYSTEM_ADMIN |
| GreenFleet Logistics | `manager1` | `password` | LOGISTICS_MANAGER |
| GreenFleet Logistics | `auditor1` | `password` | AUDITOR |
| OceanCargo Int. | `manager2` | `password` | LOGISTICS_MANAGER |
| OceanCargo Int. | `auditor2` | `password` | AUDITOR |

---

## Core Security & Persistence Rules

1. **ddl-auto=update**: Set in `application.properties` to ensure MySQL tables and onboarding records (like custom registered tenants) are fully preserved across server restarts.
2. **INSERT IGNORE**: Used in `data.sql` to bypass duplicate constraint errors on startup when seeding the default database values.
3. **Data Isolation**: All dashboard parameters, KPIs, ledgers, and team provisionings extract the `companyId` directly from the Spring Security Principal (`EcoUserDetails`).
4. **Access Gates**: `/admin/**` is secured to `SYSTEM_ADMIN` only.

---

## Demo Flow For Evaluation

Use this flow to demonstrate the complete corporate lifecycle:

### Step 1: Tenant Workspace Registration (Public)
1. Navigate to `http://localhost:8080` and click **Register New Tenant**.
2. Input Company Name (e.g. `Company2`), Sector (e.g. `Tech`), Manager Username (e.g. `manager_test`), Email (`manager@company2.com`), and Password (`password`).
3. Submit the form. You are redirected to the login screen.
4. Try logging in as `manager_test`. Spring Security throws a **DisabledException** showing that approval is pending.

### Step 2: System Admin Review & Approval
1. Navigate to the admin terminal at `http://localhost:8080/admin/login` and log in with `admin` / `password`.
2. On the admin dashboard, find `Company2` under the **Pending Applications** queue.
3. Observe the **Platform Audit Trail** showing a `TENANT_REGISTER` row logged in real-time.
4. Click **Approve Access** for `Company2`. The application status changes to active and a `TENANT_APPROVE` event logs in the audit trail.

### Step 3: Logistics Log & Team Provisioning
1. Log out of the admin console, and log in to the tenant workspace with `manager_test` / `password`.
2. Select the **Manager Panel** tab. 
3. Provision a new compliance auditor user account: `auditor_test` / `password`.
4. Return to the **Dashboard** tab and log a transit from `Chennai` to `Mysuru` (10 tons, `RAIL`).
5. The system calculates the CO₂ footprint and updates the KPI cards.

### Step 4: Auditor Compliance Certificate Download
1. Log out, and log in with the new auditor account: `auditor_test` / `password`.
2. Observe that write forms are hidden. Go to the **Compliance Certificate** tab.
3. Click **Download Certificate** next to the logged shipment.
4. View the environmental certificate and click **Print Official Certificate** to trigger the print layout dialog.

---

## REST API Endpoints

| Method | URL | Role Required | Description |
|---|---|---|---|
| `GET` | `/api/shipments` | Authenticated | Return tenant-scoped shipments as JSON |
| `POST` | `/api/shipments` | `LOGISTICS_MANAGER` | Create a new shipment via JSON |
| `GET` | `/api/routes/distance?origin=X&destination=Y` | Authenticated | Get resolved Dijkstra distance between two cities |

---

## Test Results

```text
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

Run test classes using:
```bash
.\apache-maven-3.9.6\bin\mvn.cmd test
```

Breakdown:
- `DistanceServiceTest`: Verifies shortest paths, multihops, and Dijkstra algorithms.
- `ShipmentRestControllerTest`: Asserts security gates for REST API endpoints.
- `TenantOnboardingTest`: Asserts tenant self-registration redirects, admin review panels, approvals, and team provisionings.
