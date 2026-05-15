package com.demo.resortslite;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Security configuration for Azure Active Directory authentication.
 * FIXED: cr-java-0090 - Replaced file-based authentication with Azure AD integration
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/api/bookings/**").authenticated()
                .antMatchers("/h2-console/**").permitAll()
                .anyRequest().permitAll()
            .and()
            .oauth2Login()
            .and()
            .oauth2ResourceServer()
                .jwt();
        
        // Allow H2 console in development
        http.csrf().disable();
        http.headers().frameOptions().disable();
    }
}
