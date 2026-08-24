package com.holaho.intern.shared.security;

import com.holaho.intern.shared.config.SecurityConfig;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, CustomUserDetailsService uds) {
        this.tokenProvider = tokenProvider;
        this.customUserDetailsService = uds;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();

        // Allow filter to run for all paths so that @PreAuthorize works correctly
        // SecurityConfig handles the permitAll() for login/register
        if (path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        String token = null;
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        }

        if (token != null && tokenProvider.validateToken(token)) {
            // Check Redis Token Blacklist for revoked JWTs
            if (isTokenBlacklisted(token)) {
                logger.warn("Attempted use of revoked JWT token: " + token);
            } else {
                String username = tokenProvider.getUsernameFromJWT(token);
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails,
                        null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isTokenBlacklisted(String token) {
        try {
            org.springframework.data.redis.core.StringRedisTemplate redisTemplate =
                    com.holaho.intern.shared.config.ApplicationContextProvider.getBean(org.springframework.data.redis.core.StringRedisTemplate.class);
            return redisTemplate != null && Boolean.TRUE.equals(redisTemplate.hasKey("token:blacklist:" + token));
        } catch (Exception e) {
            return false;
        }
    }
}

