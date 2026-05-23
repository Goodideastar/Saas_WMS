package com.wms.security;

import com.wms.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_BLACKLIST_PREFIX = "auth:blacklist:";

    private static final List<String> SKIP_PATHS = List.of(
            "/auth/login",
            "/auth/register",
            "/doc.html",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/webjars/**",
            "/actuator/health");

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final StringRedisTemplate redisTemplate;
    private final AntPathMatcher pathMatcher;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService,
            StringRedisTemplate redisTemplate) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
        this.redisTemplate = redisTemplate;
        this.pathMatcher = new AntPathMatcher();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return SKIP_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            try {
                if (Boolean.TRUE.equals(
                        redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + token))) {
                    filterChain.doFilter(request, response);
                    return;
                }

                String username = jwtUtils.getUsernameFromToken(token);

                if (StringUtils.hasText(username)
                        && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    if (!jwtUtils.isTokenExpired(token)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());
                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (Exception e) {
                logger.error("JWT authentication failed: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
