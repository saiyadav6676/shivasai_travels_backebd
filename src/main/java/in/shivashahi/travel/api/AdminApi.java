package in.shivashahi.travel.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@RestController
@RequestMapping("/api/admin")
public class AdminApi {
  private final JdbcTemplate db;
  public AdminApi(JdbcTemplate db) { this.db = db; }

  public record FleetUpdate(@NotBlank @Pattern(regexp="ACTIVE|PARKED|MAINTENANCE") String fleetStatus,
      @Min(0) @Max(100) int fuelLevelPercent, @NotBlank @Size(max=100) String parkingLocation,
      LocalDate maintenanceDue, LocalDate lastService) {}
  public record CharterStatus(@NotBlank @Pattern(regexp="NEW|CONTACTED|CONFIRMED|CANCELLED") String status) {}

  @GetMapping("/summary")
  public Map<String,Object> summary() {
    long buses = count("SELECT COUNT(*) FROM buses");
    long trips = count("SELECT COUNT(*) FROM trips");
    long bookings = count("SELECT COUNT(*) FROM bookings");
    long customers = count("SELECT COUNT(*) FROM users WHERE role='CUSTOMER'");
    long parked = count("SELECT COUNT(*) FROM buses WHERE fleet_status='PARKED'");
    long maintenance = count("SELECT COUNT(*) FROM buses WHERE fleet_status='MAINTENANCE'");
    long lowFuel = count("SELECT COUNT(*) FROM buses WHERE fuel_level_percent<25");
    long charterRequests = count("SELECT COUNT(*) FROM charter_requests WHERE status='NEW'");
    return Map.of("buses",buses,"trips",trips,"bookings",bookings,"customers",customers,
      "parked",parked,"maintenance",maintenance,"lowFuel",lowFuel,"charterRequests",charterRequests);
  }

  @GetMapping("/buses")
  public List<Map<String,Object>> buses() {
    return db.queryForList("SELECT id,operator_name,bus_number,bus_type,amenities,fleet_status,fuel_level_percent,parking_location,maintenance_due,last_service FROM buses ORDER BY bus_number");
  }

  @PatchMapping("/buses/{id}")
  public Map<String,Object> updateBus(@PathVariable long id, @Valid @RequestBody FleetUpdate update) {
    int changed = db.update("UPDATE buses SET fleet_status=?,fuel_level_percent=?,parking_location=?,maintenance_due=?,last_service=? WHERE id=?",
      update.fleetStatus(),update.fuelLevelPercent(),update.parkingLocation().trim(),update.maintenanceDue(),update.lastService(),id);
    if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Bus not found");
    return Map.of("ok",true);
  }

  @GetMapping("/trips")
  public List<Map<String,Object>> trips() {
    return db.queryForList("SELECT t.id,r.origin,r.destination,b.bus_number,b.operator_name,b.bus_type,t.departure_at,t.arrival_at,t.fare FROM trips t JOIN routes r ON r.id=t.route_id JOIN buses b ON b.id=t.bus_id ORDER BY t.departure_at DESC");
  }

  @GetMapping("/bookings")
  public List<Map<String,Object>> bookings() {
    return db.queryForList("SELECT bk.id,bk.pnr,bk.passenger_name,bk.email,bk.phone,bk.seat_codes,bk.amount,bk.status,bk.created_at,r.origin,r.destination,t.departure_at FROM bookings bk JOIN trips t ON t.id=bk.trip_id JOIN routes r ON r.id=t.route_id ORDER BY bk.created_at DESC LIMIT 200");
  }

  @GetMapping("/customers")
  public List<Map<String,Object>> customers() {
    return db.queryForList("SELECT id,full_name,email,phone,created_at FROM users WHERE role='CUSTOMER' ORDER BY created_at DESC LIMIT 500");
  }

  @GetMapping("/charters")
  public List<Map<String,Object>> charters() {
    return db.queryForList("SELECT id,customer_name,email,phone,occasion,travel_date,pickup_location,destination,passenger_count,notes,status,created_at FROM charter_requests ORDER BY created_at DESC");
  }

  @PatchMapping("/charters/{id}")
  public Map<String,Object> updateCharter(@PathVariable long id, @Valid @RequestBody CharterStatus update) {
    int changed = db.update("UPDATE charter_requests SET status=? WHERE id=?",update.status(),id);
    if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Charter request not found");
    return Map.of("ok",true);
  }

  private long count(String sql) { Long n = db.queryForObject(sql, Long.class); return n == null ? 0 : n; }
}
