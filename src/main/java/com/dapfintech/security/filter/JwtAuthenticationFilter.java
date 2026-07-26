package com.dapfintech.security.filter;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.dapfintech.security.jwt.JwtService;
import com.dapfintech.security.service.CustomUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final CustomUserDetailsService
            userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader =
                request.getHeader("Authorization");

        // =============================================
        // NO JWT TOKEN
        // Continue the request.
        // Spring Security will decide whether the
        // endpoint requires authentication.
        // =============================================

        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        try {

            // =========================================
            // EXTRACT JWT
            // =========================================

            String jwt =
                    authHeader.substring(7);

            String mobileNumber =
                    jwtService.extractUsername(jwt);

            // =========================================
            // AUTHENTICATE USER
            // =========================================

            if (mobileNumber != null &&
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication() == null) {

                UserDetails userDetails =
                        userDetailsService
                                .loadUserByUsername(
                                        mobileNumber
                                );

                if (jwtService.isTokenValid(jwt)) {

                    UsernamePasswordAuthenticationToken
                            authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(
                                    authToken
                            );

                    System.out.println(
                            "JWT AUTH SUCCESS: "
                                    + mobileNumber
                                    + " | "
                                    + request.getRequestURI()
                    );
                }
            }

        } catch (Exception e) {

            System.err.println(
                    "JWT authentication failed for request: "
                            + request.getRequestURI()
            );

            System.err.println(
                    "Error type: "
                            + e.getClass().getSimpleName()
            );

            System.err.println(
                    "Error message: "
                            + e.getMessage()
            );
        }

        // =============================================
        // CRITICAL:
        // ALWAYS CONTINUE THE FILTER CHAIN
        // =============================================

        filterChain.doFilter(
                request,
                response
        );
    }
}