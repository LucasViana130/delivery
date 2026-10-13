package br.com.fiap.delivery.order.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class OrderRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(OrderRateLimitFilter.class);

    private final boolean enabled;
    private final int limit;
    private final TokenBucket bucket;

    public OrderRateLimitFilter(
            @Value("${delivery.rate-limit.enabled:true}") boolean enabled,
            @Value("${delivery.rate-limit.orders-per-second:20}") int ordersPerSecond) {
        this.enabled = enabled;
        this.limit = ordersPerSecond;
        this.bucket = new TokenBucket(ordersPerSecond);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        boolean ordersPath = "/orders".equals(uri) || "/orders/".equals(uri);
        return !enabled || !"POST".equals(request.getMethod()) || !ordersPath;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (bucket.tryAcquire()) {
            chain.doFilter(request, response);
            return;
        }

        log.warn("Rate limit exceeded on POST /orders (limit {} per second)", limit);
        response.setStatus(429);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", "1");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.getWriter().write(
                "{\"error\":\"Too many requests: the limit is " + limit + " orders per second\"}");
    }
}
