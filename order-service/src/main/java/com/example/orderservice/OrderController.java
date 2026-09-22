package com.example.orderservice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    // Giả lập cơ sở dữ liệu lưu trữ đơn hàng trong bộ nhớ
    private final Map<Long, Order> orderStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public OrderController() {
        // Khởi tạo dữ liệu mẫu
        orderStore.put(1L, new Order(1L, "Nguyễn Văn A", "Laptop Dell XPS 15", 1, new BigDecimal("35000000"), "PENDING"));
        orderStore.put(2L, new Order(2L, "Trần Thị B", "iPhone 15 Pro Max", 2, new BigDecimal("60000000"), "SHIPPED"));
        orderStore.put(3L, new Order(3L, "Lê Văn C", "Tai nghe Sony WH-1000XM5", 1, new BigDecimal("8500000"), "DELIVERED"));
        idGenerator.set(4L);
    }

    // 1. GET /api/orders - Lấy danh sách tất cả đơn hàng
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(new ArrayList<>(orderStore.values()));
    }

    // 2. GET /api/orders/{id} - Lấy thông tin đơn hàng chi tiết theo ID
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        Order order = orderStore.get(id);
        if (order != null) {
            return ResponseEntity.ok(order);
        }
        return ResponseEntity.notFound().build();
    }

    // 3. POST /api/orders - Tạo mới đơn hàng
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        Long newId = idGenerator.getAndIncrement();
        order.setId(newId);
        if (order.getStatus() == null || order.getStatus().isBlank()) {
            order.setStatus("PENDING");
        }
        orderStore.put(newId, order);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    // 4. PUT /api/orders/{id} - Cập nhật thông tin đơn hàng
    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable Long id, @RequestBody Order updatedOrder) {
        Order existingOrder = orderStore.get(id);
        if (existingOrder == null) {
            return ResponseEntity.notFound().build();
        }

        existingOrder.setCustomerName(updatedOrder.getCustomerName());
        existingOrder.setProductName(updatedOrder.getProductName());
        existingOrder.setQuantity(updatedOrder.getQuantity());
        existingOrder.setTotalPrice(updatedOrder.getTotalPrice());
        if (updatedOrder.getStatus() != null) {
            existingOrder.setStatus(updatedOrder.getStatus());
        }

        orderStore.put(id, existingOrder);
        return ResponseEntity.ok(existingOrder);
    }

    // 5. DELETE /api/orders/{id} - Xóa đơn hàng theo ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteOrder(@PathVariable Long id) {
        if (orderStore.containsKey(id)) {
            orderStore.remove(id);
            return ResponseEntity.ok(Map.of("message", "Xóa đơn hàng thành công với ID: " + id));
        }
        return ResponseEntity.notFound().build();
    }
}
