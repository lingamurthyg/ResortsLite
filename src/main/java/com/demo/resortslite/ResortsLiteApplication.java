package com.demo.resortslite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * ResortsLite Spring Boot application entry point.
 *
 * <p>Cloud-readiness annotations:</p>
 * <ul>
 *   <li>{@code @EnableRedisHttpSession} — activates Spring Session backed by
 *       Amazon ElastiCache for Redis, replacing in-process HttpSession storage
 *       and enabling stateless, horizontally scalable instances on AWS
 *       (blockers 13-17 / cr-java-0065).</li>
 *   <li>{@code @EnableCaching} — activates the Spring Cache abstraction backed
 *       by Amazon ElastiCache for Redis with TTL, replacing the in-memory
 *       HashMap cache and preventing unbounded memory growth across instances
 *       (blocker-20 / cr-java-0067).</li>
 * </ul>
 */
@SpringBootApplication
@EnableCaching
@EnableRedisHttpSession
public class ResortsLiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResortsLiteApplication.class, args);
    }
}
