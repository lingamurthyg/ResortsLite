package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Distributed cache service using Amazon ElastiCache for Redis.
 * Replaces local in-memory caches for horizontal scalability.
 */
@Service
public class DistributedCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String BOOKING_CACHE_PREFIX = "booking:";
    private static final long DEFAULT_TTL_MINUTES = 60;

    /**
     * Store a booking in the distributed cache
     * @param bookingId The booking ID
     * @param booking The booking data
     */
    public void cacheBooking(String bookingId, Map<String, Object> booking) {
        String key = BOOKING_CACHE_PREFIX + bookingId;
        redisTemplate.opsForValue().set(key, booking, DEFAULT_TTL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Retrieve a booking from the distributed cache
     * @param bookingId The booking ID
     * @return The booking data, or null if not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCachedBooking(String bookingId) {
        String key = BOOKING_CACHE_PREFIX + bookingId;
        return (Map<String, Object>) redisTemplate.opsForValue().get(key);
    }

    /**
     * Remove a booking from the distributed cache
     * @param bookingId The booking ID
     */
    public void evictBooking(String bookingId) {
        String key = BOOKING_CACHE_PREFIX + bookingId;
        redisTemplate.delete(key);
    }

    /**
     * Store a value in the distributed cache with custom TTL
     * @param key The cache key
     * @param value The value to cache
     * @param ttlMinutes Time to live in minutes
     */
    public void put(String key, Object value, long ttlMinutes) {
        redisTemplate.opsForValue().set(key, value, ttlMinutes, TimeUnit.MINUTES);
    }

    /**
     * Retrieve a value from the distributed cache
     * @param key The cache key
     * @return The cached value, or null if not found
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }
}
