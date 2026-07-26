package com.dapfintech.security.config;

import java.util.List;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import jakarta.servlet.DispatcherType;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.dapfintech.security.filter.JwtAuthenticationFilter;
import com.dapfintech.security.handler.JwtAuthenticationEntryPoint;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationEntryPoint entryPoint;
	private final JwtAuthenticationFilter authenticationFilter;



	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception{
		return configuration.getAuthenticationManager();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
		http.csrf(csrf -> csrf.disable())
		.cors(cors -> cors.configurationSource(corsConfigurationSource()))
		.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
		.exceptionHandling(exception -> exception.authenticationEntryPoint(entryPoint))
		.authorizeHttpRequests(auth -> auth
				.dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
				.requestMatchers( "/api/v1/auth/**",
			    "/swagger-ui/**",
			    "/swagger-ui.html",
			    "/v3/api-docs/**",
			    "/v3/api-docs",
			    "/swagger-resources/**",
			    "/webjars/**","/api/v1/files/view/**", "/error")
		.permitAll().anyRequest().authenticated()).addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
	
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {

	    CorsConfiguration configuration =
	            new CorsConfiguration();

	    configuration.setAllowedOriginPatterns(
	            List.of("*")
	    );

	    configuration.setAllowedMethods(
	            List.of(
	                    "GET",
	                    "POST",
	                    "PUT",
	                    "DELETE",
	                    "OPTIONS"
	            )
	    );

	    configuration.setAllowedHeaders(
	            List.of("*")
	    );

	    configuration.setAllowCredentials(true);

	    UrlBasedCorsConfigurationSource source =
	            new UrlBasedCorsConfigurationSource();

	    source.registerCorsConfiguration(
	            "/**",
	            configuration
	    );

	    return source;
	}
}
