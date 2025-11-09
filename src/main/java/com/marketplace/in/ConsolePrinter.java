package com.marketplace.in;


import com.marketplace.model.AuditEvent;
import com.marketplace.model.Product;
import com.marketplace.service.MetricsService;

import java.util.List;

public interface ConsolePrinter {
    void printProducts(List<Product> products);
    void printProduct(Product product);
    void printAuditLog(List<AuditEvent> events);
    void printMessage(String message);
    void printMetrics(MetricsService metricsService);
}

