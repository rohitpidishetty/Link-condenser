package com.linkCondenser.LinkCondenser.Filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

class TokenBucketRateLimiter {
    final int requests;
    final long interval;
    volatile long lastRefillTime;
    AtomicInteger tokens;

    TokenBucketRateLimiter(int requests, int interval) {
        this.requests = requests;
        this.interval = interval;
        this.lastRefillTime = System.currentTimeMillis();
        this.tokens = new AtomicInteger(requests);
    }

    //    Thread safety added
    protected synchronized boolean allowed() {
        long currentMilliSeconds = System.currentTimeMillis();
        if (currentMilliSeconds - this.lastRefillTime > 60000) {
            this.lastRefillTime = currentMilliSeconds;
            this.tokens.set(requests);
            return true;
        }
        if (this.tokens.get() > 0) {
            this.tokens.decrementAndGet();
            return true;
        }
        return false;
    }

}

@Component
public class Filter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, TokenBucketRateLimiter> limiter = new ConcurrentHashMap<>();
    // Each IP will be allowed maximum 5 requests in a minute.
    private final int REQUESTS = 5;
    private final int INTERVAL = 12000; // 12 seconds, 1 Request per 12 seconds

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setHeader("Access-Control-Allow-Origin", "https://url-az.web.app");
            response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String clientIP = request.getHeader("X-Forwarded-For");
        clientIP = (clientIP != null && !clientIP.isEmpty()) ? clientIP.split(",")[0].trim() : request.getRemoteAddr();

        TokenBucketRateLimiter bucket;
        if (!limiter.containsKey(clientIP)) {
            bucket = new TokenBucketRateLimiter(this.REQUESTS, this.INTERVAL);
            limiter.put(clientIP, bucket);
        } else
            bucket = limiter.get(clientIP);

        if (!bucket.allowed()) {
            response.setHeader("Access-Control-Allow-Origin", "https://url-az.web.app");
            response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setStatus(429);
            response.setContentType("text/plain");
            response.getWriter().write("Too many requests. Please try again after one minute");
            response.getWriter().flush();
            return;
        }

        filterChain.doFilter(request, response);
    }
}


