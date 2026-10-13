package in.shivashahi.travel.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
  private final JwtAuthFilter jwtFilter;
  @Value("${FRONTEND_ORIGIN:http://localhost:5173}") private String frontendOrigin;
  public SecurityConfig(JwtAuthFilter jwtFilter) { this.jwtFilter = jwtFilter; }
  @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
  @Bean @Order(1) SecurityFilterChain publicAuthEndpoints(HttpSecurity http) throws Exception {
    return http.securityMatcher("/api/auth/register", "/api/auth/login")
      .csrf(csrf -> csrf.disable()).cors(cors -> cors.configurationSource(corsConfigurationSource()))
      .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(a -> a.anyRequest().permitAll())
      .build();
  }
  @Bean @Order(2) SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable()).cors(cors -> cors.configurationSource(corsConfigurationSource()))
      .httpBasic(basic -> basic.disable()).formLogin(form -> form.disable())
      .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(a -> a
        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
        .requestMatchers("/api/trips/**", "/api/bookings", "/api/charters").permitAll()
        .requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")
        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
        .anyRequest().authenticated())
      .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class).build();
  }
  @Bean CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    // Vite may choose 5174 (or another port) when its default port is occupied.
    // Permit local development origins while keeping production origins explicit.
    config.setAllowedOriginPatterns(java.util.List.of(
      frontendOrigin,
      "http://localhost:*",
      "http://127.0.0.1:*",
      "http://192.168.1.26:*"
    ));
    config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
