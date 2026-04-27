package com.demo.resortslite;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Spring Session configuration for distributed session management.
 * Enables Redis-backed HTTP sessions for container portability and horizontal scaling.
 * 
 * This configuration addresses containerization blockers:
 * - cz-java-0063: Server-side Sessions
 * - cz-java-0069: In-Memory Session Storage
 * 
 * Sessions are now stored in Amazon ElastiCache for Redis, enabling:
 * - Session persistence across container restarts
 * - Session sharing across horizontally scaled instances
 * - Stateless application architecture
 */
@Configuration
@EnableRedisHttpSession
public class SessionConfig {
    // Spring Session automatically configures Redis session repository
    // using properties from application.properties:
    // - spring.redis.host
    // - spring.redis.port
    // - spring.redis.password
}
