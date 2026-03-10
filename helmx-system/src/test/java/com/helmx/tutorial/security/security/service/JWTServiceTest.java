package com.helmx.tutorial.security.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JWTServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private JwtDecoder refreshJwtDecoder;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    private JWTService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JWTService();
        ReflectionTestUtils.setField(jwtService, "jwtEncoder", jwtEncoder);
        ReflectionTestUtils.setField(jwtService, "jwtDecoder", jwtDecoder);
        ReflectionTestUtils.setField(jwtService, "refreshJwtDecoder", refreshJwtDecoder);
        ReflectionTestUtils.setField(jwtService, "userDetailsService", userDetailsService);
        ReflectionTestUtils.setField(jwtService, "expirationHours", 24L);
    }

    @Test
    void refreshToken_nonExpiredTokenForEnabledUser_returnsNewToken() {
        Jwt expiredJwt = Jwt.withTokenValue("expired-token")
                .header("alg", "RS256")
                .subject("alice")
                .issuedAt(Instant.now().minusSeconds(3600))
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("scope", "ROLE_USER")
                .build();
        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L,
                "alice",
                "alice@example.com",
                "encoded-password",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        Jwt newJwt = Jwt.withTokenValue("new-token")
                .header("alg", "RS256")
                .subject("alice")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("scope", "ROLE_USER")
                .build();

        when(refreshJwtDecoder.decode("expired-token")).thenReturn(expiredJwt);
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);
        when(jwtEncoder.encode(any())).thenReturn(newJwt);

        String refreshedToken = jwtService.refreshToken("expired-token");

        assertEquals("new-token", refreshedToken);
    }

    @Test
    void refreshToken_expiredToken_throwsIllegalArgumentException() {
        Jwt activeJwt = Jwt.withTokenValue("active-token")
                .header("alg", "RS256")
                .subject("alice")
                .issuedAt(Instant.now().minusSeconds(3600))
                .expiresAt(Instant.now().minusSeconds(60))
                .claim("scope", "ROLE_USER")
                .build();
        when(refreshJwtDecoder.decode("expired-token")).thenReturn(activeJwt);

        assertThrows(IllegalArgumentException.class, () -> jwtService.refreshToken("expired-token"));
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void refreshToken_disabledUser_throwsIllegalArgumentException() {
        Jwt expiredJwt = Jwt.withTokenValue("expired-token")
                .header("alg", "RS256")
                .subject("alice")
                .issuedAt(Instant.now().minusSeconds(3600))
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("scope", "ROLE_USER")
                .build();
        UserDetailsImpl disabledUser = new UserDetailsImpl(
                1L,
                "alice",
                "alice@example.com",
                "encoded-password",
                false,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        when(refreshJwtDecoder.decode("expired-token")).thenReturn(expiredJwt);
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(disabledUser);

        assertThrows(IllegalArgumentException.class, () -> jwtService.refreshToken("expired-token"));
    }

    @Test
    void refreshToken_invalidSignature_throwsIllegalArgumentException() {
        when(refreshJwtDecoder.decode("bad-token")).thenThrow(new JwtException("bad token"));

        assertThrows(IllegalArgumentException.class, () -> jwtService.refreshToken("bad-token"));
    }

    @Test
    void refreshToken_unknownUser_throwsIllegalArgumentException() {
        Jwt expiredJwt = Jwt.withTokenValue("expired-token")
                .header("alg", "RS256")
                .subject("missing-user")
                .issuedAt(Instant.now().minusSeconds(3600))
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("scope", "ROLE_USER")
                .build();
        when(refreshJwtDecoder.decode("expired-token")).thenReturn(expiredJwt);
        when(userDetailsService.loadUserByUsername("missing-user"))
                .thenThrow(new UsernameNotFoundException("missing"));

        assertThrows(IllegalArgumentException.class, () -> jwtService.refreshToken("expired-token"));
    }

    @Test
    void generateToken_whitespaceUsername_throwsIllegalArgumentException() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("   ", null, List.of());

        assertThrows(IllegalArgumentException.class, () -> jwtService.generateToken(authentication));
    }

    @Test
    void generateToken_negativeExpirationHours_throwsIllegalStateException() {
        ReflectionTestUtils.setField(jwtService, "expirationHours", -1L);
        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        assertThrows(IllegalStateException.class, () -> jwtService.generateToken(authentication));
    }

    @Test
    void generateToken_validUser_returnsToken() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        Jwt newJwt = Jwt.withTokenValue("generated-token")
                .header("alg", "RS256")
                .subject("alice")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("scope", "ROLE_USER")
                .build();

        when(jwtEncoder.encode(any())).thenReturn(newJwt);

        String token = jwtService.generateToken(authentication);

        assertNotNull(token);
        assertEquals("generated-token", token);
    }
}
