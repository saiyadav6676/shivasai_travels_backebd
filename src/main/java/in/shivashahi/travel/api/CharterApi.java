package in.shivashahi.travel.api;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@RestController
@RequestMapping("/api/charters")
public class CharterApi {
  private final JdbcTemplate db;
  public CharterApi(JdbcTemplate db) { this.db = db; }

  public record CharterRequest(@NotBlank @Size(max=120) String customerName,
      @NotBlank @Email @Size(max=180) String email,
      @NotBlank @Pattern(regexp="[+0-9() -]{7,20}") String phone,
      @NotBlank @Size(max=80) String occasion, @NotNull @FutureOrPresent LocalDate travelDate,
      @NotBlank @Size(max=180) String pickupLocation,
      @NotBlank @Size(max=180) String destination,
      @Min(1) @Max(100) int passengerCount, @Size(max=1000) String notes) {}

  @PostMapping
  public Map<String,Object> create(@Valid @RequestBody CharterRequest request) {
    db.update("INSERT INTO charter_requests(customer_name,email,phone,occasion,travel_date,pickup_location,destination,passenger_count,notes) VALUES(?,?,?,?,?,?,?,?,?)",
      request.customerName().trim(), request.email().trim().toLowerCase(), request.phone().trim(),
      request.occasion().trim(), request.travelDate(), request.pickupLocation().trim(),
      request.destination().trim(), request.passengerCount(), request.notes() == null ? "" : request.notes().trim());
    Long id = db.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    return Map.of("id", id, "status", "NEW", "message", "Your charter request was received. Our team will contact you.");
  }
}
