package com.suhas.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    @GetMapping("/")
    public Map<String, String> root() {
        log.info("Root endpoint called");
        return Map.of(
            "service", "order-service",
            "status",  "running",
            "version", "1.0.0"
        );
    }

    @GetMapping("/api/orders")
    public List<Order> getOrders() {
        log.info("Fetching all orders");
        return List.of(
            new Order("ORD-001", "Widget A", "PENDING"),
            new Order("ORD-002", "Widget B", "SHIPPED")
        );
    }

    @GetMapping("/api/orders/{id}")
    public Order getOrder(@PathVariable String id) {
        log.info("Fetching order id={}", id);
        return new Order(id, "Widget A", "PENDING");
    }
}