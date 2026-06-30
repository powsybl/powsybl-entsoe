package com.powsybl.flow_decomposition.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility for timed logging around a block of work.
 * <p>
 * Each call logs a "started" message before the work begins and a "completed" message
 * with elapsed time in milliseconds once it finishes.
 *
 * @author Georg Haider {@literal <georg.haider at artelys.com>}
 */
public final class LogUtils {

    private LogUtils() {
        // empty
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(LogUtils.class);

    /**
     * Executes {@code supplier}, surrounding it with INFO log messages that include the
     * step name and, on completion, the elapsed time.
     *
     * @param <T>      the return type of the supplier
     * @param stepName human-readable name of the step, used in the log messages
     * @param supplier the work to execute and time
     * @return the value returned by {@code supplier}
     */
    public static <T> T info(String stepName, Supplier<T> supplier) {
        return timed(stepName, supplier, LOGGER::isInfoEnabled, LOGGER::info);
    }

    /**
     * Executes {@code runnable}, surrounding it with INFO log messages that include the
     * step name and, on completion, the elapsed time.
     *
     * @param stepName human-readable name of the step, used in the log messages
     * @param runnable the work to execute and time
     */
    public static void info(String stepName, Runnable runnable) {
        timed(stepName, runnable, LOGGER::isInfoEnabled, LOGGER::info);
    }

    /**
     * Executes {@code supplier}, surrounding it with TRACE log messages that include the
     * step name and, on completion, the elapsed time.
     *
     * @param <T>      the return type of the supplier
     * @param stepName human-readable name of the step, used in the log messages
     * @param supplier the work to execute and time
     * @return the value returned by {@code supplier}
     */
    public static <T> T trace(String stepName, Supplier<T> supplier) {
        return timed(stepName, supplier, LOGGER::isTraceEnabled, LOGGER::trace);
    }

    /**
     * Executes {@code runnable}, surrounding it with TRACE log messages that include the
     * step name and, on completion, the elapsed time.
     *
     * @param stepName human-readable name of the step, used in the log messages
     * @param runnable the work to execute and time
     */
    public static void trace(String stepName, Runnable runnable) {
        timed(stepName, runnable, LOGGER::isTraceEnabled, LOGGER::trace);
    }

    private static <T> T timed(String stepName, Supplier<T> supplier, BooleanSupplier isLogEnabled, Consumer<String> logger) {
        Objects.requireNonNull(stepName, "stepName is null");
        Objects.requireNonNull(supplier, "supplier is null");
        Objects.requireNonNull(logger, "logger is null");
        if (!isLogEnabled.getAsBoolean()) {
            return supplier.get();
        }
        long start = System.nanoTime();
        logger.accept(stepName + " started");
        try {
            T result = supplier.get();
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            logger.accept(stepName + " completed. Time=" + elapsedMillis + " ms");
            return result;
        } catch (Exception e) {
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            logger.accept(stepName + " failed. Time=" + elapsedMillis + " ms");
            throw e;
        }
    }

    private static void timed(String stepName, Runnable runnable, BooleanSupplier isLogEnabled, Consumer<String> logger) {
        timed(stepName, () -> {
            runnable.run();
            return null;
        }, isLogEnabled, logger);
    }
}
