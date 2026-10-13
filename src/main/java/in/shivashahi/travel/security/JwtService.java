package in.shivashahi.travel.security;

import java.time.Instant;
import java.util.Date;
import java.security.SecureRandom;
import javax.crypto.SecretKey;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecretKey key;
  public JwtService(@Value("${app.jwt-secret}") String base64Secret) {
    byte[] bytes;
    if (base64Secret == null || base64Secret.isBlank()) {
      // Local development convenience: avoid failing startup when a fresh
      // PowerShell session does not have JWT_SECRET set. Tokens expire on restart.
      bytes = new byte[32];
      new SecureRandom().nextBytes(bytes);
      System.getLogger(JwtService.class.getName()).log(System.Logger.Level.WARNING,
        "JWT_SECRET is unset; using a temporary key for this development run. Set JWT_SECRET to preserve tokens across restarts.");
    } else {
      bytes = Decoders.BASE64.decode(base64Secret);
    }
    if (bytes.length < 32) throw new IllegalArgumentException("JWT_SECRET must be base64 for at least 32 random bytes");
    key = Keys.hmacShaKeyFor(bytes);
  }
  public String create(long userId, String email, String role) {
    Instant now = Instant.now();
    return Jwts.builder().subject(Long.toString(userId)).claim("email", email).claim("role", role)
      .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(12 * 60 * 60)))
      .signWith(key).compact();
  }
  public io.jsonwebtoken.Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
