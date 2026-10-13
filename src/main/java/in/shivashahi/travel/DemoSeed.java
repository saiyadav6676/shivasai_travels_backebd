package in.shivashahi.travel;

import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DemoSeed implements CommandLineRunner {
  private final JdbcTemplate db;
  private final Environment env;
  public DemoSeed(JdbcTemplate db, Environment env) { this.db=db; this.env=env; }
  @Override public void run(String... args) {
    if (!env.getProperty("app.seed-demo", Boolean.class, false)) return;
    if (db.queryForObject("SELECT COUNT(*) FROM trips", Integer.class) > 0) return;
    db.update("INSERT INTO routes(origin,destination) VALUES('Mumbai','Pune')");
    db.update("INSERT INTO buses(operator_name,bus_number,bus_type,amenities) VALUES('Shivasai Travels','MH-12 SS 1042','Volvo AC Sleeper (2+1)','AC,WiFi,Charging point')");
    var date=LocalDate.now().plusDays(1);
    db.update("INSERT INTO trips(route_id,bus_id,departure_at,arrival_at,fare) VALUES(1,1,?, ?,899.00)", date+" 21:30:00", date.plusDays(1)+" 06:15:00");
    for (int row=1; row<=8; row++) for (char col='A'; col<='F'; col++) {
      String code=""+row+col;
      db.update("INSERT INTO trip_seats(trip_id,seat_code,seat_type,status) VALUES(1,?,?,'AVAILABLE')", code, col=='A'||col=='D' ? "SLEEPER":"SEATER");
    }
  }
}
