package in.shivashahi.travel.api;

import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import in.shivashahi.travel.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@RestController
@RequestMapping("/api/auth")
public class AuthApi {
  private final JdbcTemplate db;
  private final PasswordEncoder passwords;
  private final JwtService jwt;
  public AuthApi(JdbcTemplate db, PasswordEncoder passwords, JwtService jwt) { this.db=db; this.passwords=passwords; this.jwt=jwt; }

  public record RegisterRequest(@NotBlank @Size(max=120) String fullName, @NotBlank @Email @Size(max=180) String email,
      @NotBlank @Pattern(regexp="[+0-9() -]{7,20}") String phone, @NotBlank @Size(min=8,max=72) String password) {}
  public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

  @PostMapping("/register") @Transactional
  public Map<String,Object> register(@Valid @RequestBody RegisterRequest request) {
    String email=request.email().trim().toLowerCase();
    try { db.update("INSERT INTO users(full_name,email,phone,password_hash,role) VALUES(?,?,?,?, 'CUSTOMER')", request.fullName().trim(), email, request.phone().trim(), passwords.encode(request.password())); }
    catch (DuplicateKeyException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists"); }
    return loginResponse(email);
  }

  @PostMapping("/login")
  public Map<String,Object> login(@Valid @RequestBody LoginRequest request) {
    String email=request.email().trim().toLowerCase();
    var rows=db.queryForList("SELECT id,full_name,email,phone,password_hash,role FROM users WHERE email=?", email);
    if (rows.isEmpty() || !passwords.matches(request.password(), (String)rows.get(0).get("password_hash")))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect");
    return response(rows.get(0));
  }

  @GetMapping("/me")
  public Map<String,Object> me(org.springframework.security.core.Authentication auth) {
    long id=(Long)auth.getPrincipal();
    var rows=db.queryForList("SELECT id,full_name,email,phone,role FROM users WHERE id=?", id);
    if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
    return rows.get(0);
  }

  private Map<String,Object> loginResponse(String email) {
    return response(db.queryForMap("SELECT id,full_name,email,phone,password_hash,role FROM users WHERE email=?", email));
  }
  private Map<String,Object> response(Map<String,Object> row) {
    long id=((Number)row.get("id")).longValue(); String email=(String)row.get("email"); String role=(String)row.get("role");
    return Map.of("token",jwt.create(id,email,role), "tokenType","Bearer", "user",Map.of("id",id,"fullName",row.get("full_name"),"email",email,"phone",row.get("phone"),"role",role));
  }
}
