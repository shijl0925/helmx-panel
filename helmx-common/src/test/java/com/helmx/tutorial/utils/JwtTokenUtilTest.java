package com.helmx.tutorial.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenUtilTest {

    @Mock
    private JwtDecoder jwtDecoder;

    private JwtTokenUtil jwtTokenUtil;

    @BeforeEach
    void setUp() {
        jwtTokenUtil = new JwtTokenUtil(jwtDecoder);
    }

    @Test
    void validateToken_thenGetUserId_reusesDecodedJwtForSameToken() {
        Jwt jwt = Jwt.withTokenValue("token-1")
                .header("alg", "RS256")
                .subject("alice")
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("userId", 7L)
                .build();
        when(jwtDecoder.decode("token-1")).thenReturn(jwt);

        assertEquals(true, jwtTokenUtil.validateToken("token-1"));
        assertEquals(7L, jwtTokenUtil.getUserIdFromToken("token-1"));

        verify(jwtDecoder, times(1)).decode("token-1");
    }

    @Test
    void getUsernameFromToken_differentTokens_decodeSeparately() {
        Jwt aliceJwt = Jwt.withTokenValue("token-a")
                .header("alg", "RS256")
                .subject("alice")
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        Jwt bobJwt = Jwt.withTokenValue("token-b")
                .header("alg", "RS256")
                .subject("bob")
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode("token-a")).thenReturn(aliceJwt);
        when(jwtDecoder.decode("token-b")).thenReturn(bobJwt);

        assertEquals("alice", jwtTokenUtil.getUsernameFromToken("token-a"));
        assertEquals("bob", jwtTokenUtil.getUsernameFromToken("token-b"));

        verify(jwtDecoder).decode("token-a");
        verify(jwtDecoder).decode("token-b");
    }

    @Test
    void validateToken_invalidToken_returnsFalseWithoutReusingFailedCache() {
        when(jwtDecoder.decode("bad-token")).thenThrow(new IllegalArgumentException("bad token"));

        assertFalse(jwtTokenUtil.validateToken("bad-token"));
        assertThrows(RuntimeException.class, () -> jwtTokenUtil.parseToken("bad-token"));

        verify(jwtDecoder, times(2)).decode("bad-token");
    }
}
