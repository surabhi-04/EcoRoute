package com.ecoroute.security;

import com.ecoroute.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Custom UserDetails that carries the tenant context (companyId, companyName)
 * throughout the security lifecycle. This prevents any controller or service
 * from having to perform a separate DB lookup for the current user's company.
 */
public class EcoUserDetails implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final String role;
    private final Long companyId;
    private final String companyName;
    private final List<GrantedAuthority> authorities;

    public EcoUserDetails(User user) {
        this.userId      = user.getId();
        this.username    = user.getUsername();
        this.password    = user.getPassword();
        this.role        = user.getRole().name();
        this.companyId   = user.getCompany().getId();
        this.companyName = user.getCompany().getName();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + this.role));
    }

    // ── Tenant accessors ──────────────────────────────────────────────────────
    public Long getUserId()      { return userId;      }
    public Long getCompanyId()   { return companyId;   }
    public String getCompanyName() { return companyName; }
    public String getRole()      { return role;        }

    // ── UserDetails ───────────────────────────────────────────────────────────
    @Override public String getUsername()  { return username;    }
    @Override public String getPassword()  { return password;    }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public boolean isAccountNonExpired()  { return true; }
    @Override public boolean isAccountNonLocked()   { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()            { return true; }
}
