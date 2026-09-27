package com.portshipping.config;

import com.portshipping.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;
import java.util.List;

@Configuration
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins:http://localhost:5500,http://127.0.0.1:5500,http://localhost:3000,http://localhost:5173,http://localhost:8080,http://127.0.0.1:8080,http://localhost:8000,http://127.0.0.1:8000}")
    private String rawAllowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository repo) {
        return email -> repo.findByEmailIgnoreCase(email)
                .map(u -> User.withUsername(u.getEmail())
                        .password(u.getPassword())
                        .roles(u.getRole())
                        .disabled(!u.isEnabled())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService details, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(details);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(requestHandler)
                .ignoringRequestMatchers("/api/auth/login", "/api/auth/register", "/api/health")
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/*.html", "/css/**", "/js/**", "/assets/**", "/favicon.ico", "/api/auth/**", "/api/health", "/api/help/**", "/error").permitAll()
                .requestMatchers("/api/users/me/**").authenticated()
                .requestMatchers("/api/users/**", "/api/audit-logs/**").hasRole("ADMIN")
                .requestMatchers("/api/notifications/**", "/api/search/**", "/api/support/**").authenticated()
                .requestMatchers("/api/admin/support/**").hasAnyRole("ADMIN", "PORT_MANAGER", "SHIPPING_AGENT")
                .requestMatchers("/api/invoices/**", "/api/payments/**", "/api/financial/**", "/api/reports/financial-summary", "/api/reports/revenue-trends").hasAnyRole("ADMIN", "PORT_MANAGER", "FINANCE_OFFICER")
                .requestMatchers("/api/ports/**", "/api/terminals/**", "/api/berths/**").hasAnyRole("ADMIN", "PORT_MANAGER")
                .requestMatchers("/api/containers/**", "/api/cargo/**", "/api/shipments/**", "/api/vessels/**", "/api/berth-allocations/**", "/api/schedules/**", "/api/customs/**", "/api/customs-clearances/**", "/api/documents/**").hasAnyRole("ADMIN", "PORT_MANAGER", "SHIPPING_AGENT")
                .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("ADMIN", "PORT_MANAGER", "SHIPPING_AGENT", "VIEWER", "FINANCE_OFFICER")
                .anyRequest().authenticated()
            )
            .httpBasic(h -> {});

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        List<String> origins = Arrays.stream(rawAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        c.setAllowedOrigins(origins);
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        c.setAllowedHeaders(List.of("*"));
        c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }
}
