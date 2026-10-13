package in.shivashahi.travel.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@RestController
@RequestMapping("/api")
public class TravelApi {
  private final JdbcTemplate db;
  public TravelApi(JdbcTemplate db) { this.db = db; }

  @GetMapping("/trips/search")
  public List<Map<String,Object>> search(@RequestParam String from, @RequestParam String to, @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date) {
    return db.queryForList("SELECT t.id, r.origin, r.destination, b.operator_name operator, b.bus_number, b.bus_type, b.amenities, t.departure_at, t.arrival_at, t.fare, (SELECT COUNT(*) FROM trip_seats s WHERE s.trip_id=t.id AND s.status='AVAILABLE') available FROM trips t JOIN routes r ON r.id=t.route_id JOIN buses b ON b.id=t.bus_id WHERE LOWER(r.origin)=LOWER(?) AND LOWER(r.destination)=LOWER(?) AND DATE(t.departure_at)=? ORDER BY t.departure_at", from.trim(), to.trim(), date);
  }

  @GetMapping("/trips/{id}/seats")
  public List<Map<String,Object>> seats(@PathVariable long id) {
    return db.queryForList("SELECT seat_code code, seat_type type, status FROM trip_seats WHERE trip_id=? ORDER BY seat_code", id);
  }

  public record BookingRequest(@Positive long tripId, @NotBlank @Size(max=120) String passengerName, @Pattern(regexp="[+0-9() -]{7,20}") String phone, @Email @NotBlank String email, @NotEmpty @Size(max=6) List<@NotBlank String> seats) {}
  @PostMapping("/bookings") @Transactional
  public Map<String,Object> book(@Valid @RequestBody BookingRequest request, org.springframework.security.core.Authentication authentication) {
    var trip = db.query("SELECT fare FROM trips WHERE id=?", rs -> rs.next() ? rs.getBigDecimal(1) : null, request.tripId());
    if (trip == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found");
    var unique = new HashSet<>(request.seats());
    if (unique.size() != request.seats().size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate seat selected");
    int changed = db.update("UPDATE trip_seats SET status='BOOKED' WHERE trip_id=? AND seat_code IN (" + String.join(",", Collections.nCopies(unique.size(), "?")) + ") AND status='AVAILABLE'", prepend(request.tripId(), unique.toArray()));
    if (changed != unique.size()) throw new ResponseStatusException(HttpStatus.CONFLICT, "One or more seats are no longer available");
    String pnr = "SS" + UUID.randomUUID().toString().replace("-", "").substring(0,10).toUpperCase();
    BigDecimal amount = trip.multiply(BigDecimal.valueOf(unique.size()));
    Long userId = authentication != null && authentication.getPrincipal() instanceof Long id ? id : null;
    db.update("INSERT INTO bookings(pnr,trip_id,passenger_name,phone,email,seat_codes,amount,status,user_id) VALUES(?,?,?,?,?,?,?,'CONFIRMED',?)", pnr, request.tripId(), request.passengerName().trim(), request.phone(), request.email().trim(), String.join(",", unique), amount, userId);
    return Map.of("pnr", pnr, "status", "CONFIRMED", "amount", amount, "seats", unique);
  }

  @GetMapping("/bookings/mine")
  public List<Map<String,Object>> myBookings(org.springframework.security.core.Authentication authentication) {
    long userId=(Long)authentication.getPrincipal();
    return db.queryForList("SELECT b.pnr,b.status,b.seat_codes,b.amount,b.created_at,t.departure_at,t.arrival_at,r.origin,r.destination,bus.operator_name,bus.bus_type FROM bookings b JOIN trips t ON t.id=b.trip_id JOIN routes r ON r.id=t.route_id JOIN buses bus ON bus.id=t.bus_id WHERE b.user_id=? ORDER BY b.created_at DESC", userId);
  }
  private Object[] prepend(Object first, Object[] rest) { Object[] all = new Object[rest.length+1]; all[0]=first; System.arraycopy(rest,0,all,1,rest.length); return all; }
}
