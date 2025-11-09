package com.marketplace.service;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Сервис для сбора и хранения метрик производительности и статистики.
 * <p>
 * Поддерживает:
 * <ul>
 *     <li>Счетчики операций (counters)</li>
 *     <li>Текущее состояние ресурсов (gauges)</li>
 *     <li>Измерение времени выполнения операций (timers)</li>
 *     <li>Подсчет общего времени и количества операций для анализа производительности</li>
 * </ul>
 */
public class MetricsService {

    private final Map<String, Long> counters = new HashMap<>();
    private final Map<String, Long> gauges = new HashMap<>();
    private final Map<String, Long> totalTimeNs = new HashMap<>();
    private final Map<String, Long> opCount = new HashMap<>();

    /**
     * Увеличивает счетчик для указанной операции на 1.
     *
     * @param key название счетчика
     */
    public void increment(String key) {
        counters.put(key, counters.getOrDefault(key, 0L) + 1);
    }


    /**
     * Получает текущее значение счетчика по ключу.
     *
     * @param key название счетчика
     * @return текущее значение счетчика
     */
    public long getCounter(String key) {
        return counters.getOrDefault(key, 0L);
    }

    /**
     * Устанавливает текущее значение датчика (например, количество элементов в хранилище).
     *
     * @param key   название датчика
     * @param value значение
     */
    public void setGauge(String key, long value) {
        gauges.put(key, value);
    }

    /**
     * Получает текущее значение датчика.
     *
     * @param key название датчика
     * @return значение
     */
    public long getGauge(String key) {
        return gauges.getOrDefault(key, 0L);
    }

    /**
     * Запускает таймер для измерения времени выполнения операции.
     *
     * @return текущее время в наносекундах
     */
    public long startTimer() {
        return System.nanoTime();
    }

    /**
     * Останавливает таймер и сохраняет время выполнения операции.
     *
     * @param operationName имя операции
     * @param startTimeNs   время старта в наносекундах
     * @return продолжительность выполнения в наносекундах
     */
    public long stopTimer(String operationName, long startTimeNs) {
        long duration = System.nanoTime() - startTimeNs;
        totalTimeNs.put(operationName, totalTimeNs.getOrDefault(operationName, 0L) + duration);
        opCount.put(operationName, opCount.getOrDefault(operationName, 0L) + 1);
        return duration;
    }

    /**
     * Возвращает количество выполненных операций с указанным именем.
     *
     * @param operationName имя операции
     * @return количество выполнений
     */
    public long getOpCount(String operationName) {
        return opCount.getOrDefault(operationName, 0L);
    }


    /**
     * Возвращает суммарное время выполнения операций с указанным именем в наносекундах.
     *
     * @param operationName имя операции
     * @return суммарное время выполнения
     */
    public long getTotalTimeNs(String operationName) {
        return totalTimeNs.getOrDefault(operationName, 0L);
    }


    /**
     * Получает среднее время выполнения операции в миллисекундах.
     *
     * @param operationName имя операции
     * @return среднее время выполнения в миллисекундах
     */
    public double getAverageMillis(String operationName) {
        long count = getOpCount(operationName);
        if (count == 0) return 0.0;
        return (getTotalTimeNs(operationName) / (double) count) / 1_000_000.0;
    }

    /**
     * Получает все счетчики в виде неизменяемой карты.
     *
     * @return карта счетчиков
     */
    public Map<String, Long> getCounters() {
        return Collections.unmodifiableMap(new HashMap<>(counters));
    }

    /**
     * Получает все датчики в виде неизменяемой карты.
     *
     * @return карта датчики
     */
    public Map<String, Long> getGauges() {
        return Collections.unmodifiableMap(new HashMap<>(gauges));
    }

    /**
     * Получает количество операций для всех операций в виде неизменяемой карты.
     *
     * @return карта с количеством выполненных операций
     */
    public Map<String, Long> getOpCounts() {
        return Collections.unmodifiableMap(new HashMap<>(opCount));
    }
}
