package com.marketplace.in;

import com.marketplace.model.AuditEvent;
import com.marketplace.model.Product;
import com.marketplace.service.MetricsService;

import java.util.List;
import java.util.Map;

public class ConsolePrinterImpl implements ConsolePrinter {

    @Override
    public void printProducts(List<Product> products) {
        if (products.isEmpty()) {
            System.out.println("Товары не найдены.");
        } else {
            for (Product p : products) {
                printProduct(p);
            }
        }
    }

    @Override
    public void printProduct(Product product) {
        System.out.println(product);
    }

    @Override
    public void printAuditLog(List<AuditEvent> events) {
        System.out.println("\n=== Журнал аудита ===");
        for (AuditEvent e : events) {
            System.out.println(e);
        }
    }

    @Override
    public void printMessage(String message) {
        System.out.println(message);
    }

    @Override
    public void printMetrics(MetricsService metricsService) {
        System.out.println("\n=== Метрики ===");

        System.out.println("Gauges:");
        for (Map.Entry<String, Long> e : metricsService.getGauges().entrySet()) {
            System.out.printf("  %s = %d%n", e.getKey(), e.getValue());
        }

        System.out.println("Operations:");
        for (Map.Entry<String, Long> e : metricsService.getOpCounts().entrySet()) {
            String op = e.getKey();
            long cnt = e.getValue();
            double avg = metricsService.getAverageMillis(op);
            System.out.printf("  %s : count=%d, avg=%.3f ms%n", op, cnt, avg);
        }

        System.out.println("Counters:");
        for (Map.Entry<String, Long> e : metricsService.getCounters().entrySet()) {
            System.out.printf("  %s = %d%n", e.getKey(), e.getValue());
        }
        System.out.println("===================");
    }
}