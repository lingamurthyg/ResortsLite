package com.demo.resortslite;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Configuration for Spring Session with Redis.
 * Enables distributed session management across multiple container instances.
 * Sessions are stored in Amazon ElastiCache for Redis instead of in-memory.
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 3600)
public class SessionConfig {
    // Spring Session automatically configures Redis-backed session storage
    // Session data is now shared across all container instances
}
