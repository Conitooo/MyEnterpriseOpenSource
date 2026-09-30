package com.myenterpriseos.myenterpriseopensource.security;

import com.myenterpriseos.myenterpriseopensource.entity.AppUser;
import com.myenterpriseos.myenterpriseopensource.repository.AppUserRepository;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    public static final String AUDIENCE = "myenterpriseos-api";

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    KeyPair jwtKeyPair(@Value("${app.security.jwt.private-key-location}") Resource privateResource,
                       @Value("${app.security.jwt.public-key-location}") Resource publicResource) throws Exception {
        KeyFactory factory = KeyFactory.getInstance("RSA");
        RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) factory.generatePrivate(
                new PKCS8EncodedKeySpec(pemBytes(privateResource)));
        RSAPublicKey publicKey = (RSAPublicKey) factory.generatePublic(
                new X509EncodedKeySpec(pemBytes(publicResource)));
        if (publicKey.getModulus().bitLength() < 3072 || !publicKey.getModulus().equals(privateKey.getModulus()))
            throw new IllegalStateException("JWT keys must be a matching RSA pair of at least 3072 bits");
        return new KeyPair(publicKey, privateKey);
    }

    private byte[] pemBytes(Resource resource) throws Exception {
        String pem = new String(resource.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
        String data = pem.replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(data);
    }

    @Bean
    JwtEncoder jwtEncoder(KeyPair jwtKeyPair) {
        RSAKey jwk = new RSAKey.Builder((RSAPublicKey) jwtKeyPair.getPublic())
                .privateKey(jwtKeyPair.getPrivate()).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    @Bean
    JwtDecoder jwtDecoder(KeyPair jwtKeyPair, @Value("${app.security.jwt.issuer}") String issuer) {
        URI issuerUri = URI.create(issuer);
        if (!"https".equalsIgnoreCase(issuerUri.getScheme()) || issuerUri.getHost() == null)
            throw new IllegalStateException("JWT issuer must be an HTTPS URI");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) jwtKeyPair.getPublic()).build();
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience().contains(AUDIENCE)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));
        OAuth2TokenValidator<Jwt> lifetime = jwt -> {
            Instant issued = jwt.getIssuedAt();
            Instant expires = jwt.getExpiresAt();
            if (issued == null || expires == null || jwt.getId() == null || jwt.getId().isBlank() ||
                    issued.isAfter(Instant.now().plusSeconds(60)) ||
                    Duration.between(issued, expires).toSeconds() > 900 || !expires.isAfter(issued))
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid token lifetime", null));
            return OAuth2TokenValidatorResult.success();
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), audience, lifetime));
        return decoder;
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter(AppUserRepository users) {
        return jwt -> {
            Long id;
            try { id = Long.valueOf(jwt.getSubject()); }
            catch (RuntimeException ex) { throw invalidToken(); }
            AppUser user = users.findWithCompanyById(id).orElseThrow(this::invalidToken);
            Object companyClaim = jwt.getClaim("company_id");
            Object versionClaim = jwt.getClaim("token_version");
            if (!user.isActive() || !(companyClaim instanceof Number companyId) ||
                    !(versionClaim instanceof Number version) ||
                    user.getCompany().getId().longValue() != companyId.longValue() ||
                    user.getTokenVersion() != version.intValue()) throw invalidToken();
            return new JwtAuthenticationToken(jwt,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())), user.getUsername());
        };
    }

    private OAuth2AuthenticationException invalidToken() {
        return new OAuth2AuthenticationException(new OAuth2Error("invalid_token", "Invalid token", null));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            Converter<Jwt, AbstractAuthenticationToken> converter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));
        return http.build();
    }
}
