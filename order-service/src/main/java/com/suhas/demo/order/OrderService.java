package com.suhas.demo.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public List<Order> findAll() {
        log.info("Fetching all orders");
        return repository.findAllOrderedByCreatedAtDesc();
    }

    public List<Order> findByStatus(String status) {
        log.info("Fetching orders with status={}", status);
        return repository.findByStatusOrderByCreatedAtDesc(status);
    }

    public Order findById(String id) {
        log.info("Fetching order id={}", id);
        return repository.findById(id)
            .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional
    public Order create(CreateOrderRequest request) {
        log.info("Creating order item={} qty={}", request.item(), request.quantity());
        Order order = new Order();
        order.setItem(request.item());
        order.setQuantity(request.quantity());
        return repository.save(order);
    }

    @Transactional
    public Order updateStatus(String id, String status) {
        log.info("Updating order id={} status={}", id, status);
        Order order = findById(id);
        order.setStatus(status);
        return repository.save(order);
    }

    @Transactional
    public void delete(String id) {
        log.info("Deleting order id={}", id);
        Order order = findById(id);
        repository.delete(order);
    }
}