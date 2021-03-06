package de.coronavirus.imis.config;

import java.util.Base64;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {
	private static final String BEARER = "Bearer ";
	private static final String AUTHORIZATION = "Authorization";
	private static final String ROLES = "roles";
	private final UserDetailsService userDetailsService;
	private String secretKey;
	@Value("${security.jwt.token.expire-length:86400000}")
	private long validityInMilliseconds;


	@PostConstruct
	protected void init() {
		//This is okay since it is a single so it will be created with the first request either
		//we generate this on the server and secure it somewhere or we let it random but I DON'T feel comfortable having
		//static secrets in the source code
		var randomSecret = RandomStringUtils.randomAlphabetic(12);
		secretKey = Base64.getEncoder().encodeToString(randomSecret.getBytes());
	}

	public String createToken(String username, List<String> role) {
		Claims claims = Jwts.claims().setSubject(username);
		claims.put(ROLES, role);
		Date now = new Date();
		Date validity = new Date(now.getTime() + validityInMilliseconds);
		return Jwts.builder()
				.setClaims(claims)
				.setIssuedAt(now)
				.setExpiration(validity)
				.signWith(SignatureAlgorithm.HS256, secretKey)
				.compact();
	}

	// TODO: MAYBE do not load from the db -> store roles in token since tokens are
	public Authentication getAuthentication(String token) {
		UserDetails userDetails = userDetailsService.loadUserByUsername(getUsername(token));
		return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
	}

	public String getUsername(String token) {
		return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody().getSubject();
	}

	public String resolveToken(HttpServletRequest req) {
		String bearerToken = req.getHeader(AUTHORIZATION);
		if (bearerToken != null && bearerToken.startsWith(BEARER)) {
			return bearerToken.replaceFirst(BEARER, "");
		}
		return null;
	}

	public boolean validateToken(String token) {
		try {
			Jws<Claims> claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
			if (claims.getBody().getExpiration().before(new Date())) {
				return false;
			}
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			log.error("error parsing tokens {}", token, e);
		}
		return false;
	}
}
