package com.mitar.dipl.security;

import com.mitar.dipl.exception.JwtAccessDeniedHandler;
import com.mitar.dipl.exception.JwtAuthenticationEntryPoint;
import com.mitar.dipl.security.oauth2.CustomOAuth2UserService;
import com.mitar.dipl.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.mitar.dipl.security.oauth2.OAuth2AuthenticationSuccessHandler;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.DispatcherServlet;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@AllArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, DispatcherServlet dispatcherServlet) throws Exception {
        http
                // Enable CORS using the CorsConfigurationSource bean defined in CorsConfig
                .cors(Customizer.withDefaults())

                // Disable CSRF as we are using JWT
                .csrf(AbstractHttpConfigurer::disable)

                // Set session management to stateless
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Set unauthorized requests exception handler
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )

                // Set permissions on endpoints
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()// Allow access to authentication endpoints
                        .requestMatchers("/users/create").permitAll() // Allow access to user endpoints
                        .anyRequest().authenticated() // Require authentication for all other endpoints
                )

                // OAuth2 Login Configuration
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/api/auth/login") // Custom login page (optional)
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService) // Injected CustomOAuth2UserService
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler) // Injected success handler
                        .failureHandler(oAuth2AuthenticationFailureHandler) // Injected failure handler
                )

                // Add JWT token filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .authenticationProvider(authProvider)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}