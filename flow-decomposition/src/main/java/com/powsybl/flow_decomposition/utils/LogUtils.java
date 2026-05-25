package com.powsybl.flow_decomposition.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Utility for timed logging around a block of work.
 * <p>
 * Each call logs a "started" message before the work begins and a "completed" message
 * with elapsed time in milliseconds once it finishes. This class is a wrapper around
 * arbitrary code that may throw any {@link Throwable}; checked exceptions are rethrown
 * without wrapping (sneaky-throw pattern).
 *
 * @author Georg Haider {@literal <georg.haider at artelys.es>}
 *
 */
public final class LogUtils {

    private LogUtils() {
        // empty
    }

    /**
     * A {@link java.util.function.Supplier}-like functional interface whose {@code get}
     * method is allowed to throw any {@link Throwable}.
     *
     * @param <T> the type of value produced
     */
    @FunctionalInterface
    public interface ThrowingSupplier<T> {

        /**
         * Gets a result, potentially throwing a checked exception.
         *
         * @return the produced value
         * @throws Throwable any exception or error
         */
        @SuppressWarnings("java:S112")
        T get() throws Throwable;
    }

    /**
     * A {@link Runnable}-like functional interface whose {@code run} method is allowed
     * to throw any {@link Throwable}.
     */
    @FunctionalInterface
    public interface ThrowingRunnable {

        /**
         * Performs the operation, potentially throwing a checked exception.
         *
         * @throws Throwable any exception or error
         */
        @SuppressWarnings("java:S112")
        void run() throws Throwable;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(LogUtils.class);

    /**
     * Executes {@code suppl}, surrounding it with INFO log messages that include the
     * step name and, on completion, the elapsed time.
     *
     * @param <T>   the return type of the supplier
     * @param s     human-readable name of the step, used in the log messages
     * @param suppl the work to execute and time
     * @return the value returned by {@code suppl}
     */
    public static <T> T info(String s, ThrowingSupplier<T> suppl) {
        return timed(s, suppl, LOGGER::isInfoEnabled, LOGGER::info);
    }

    /**
     * Executes {@code runn}, surrounding it with INFO log messages that include the
     * step name and, on completion, the elapsed time.
     *
     * @param s    human-readable name of the step, used in the log messages
     * @param runn the work to execute and time
     */
    public static void info(String s, ThrowingRunnable runn) {
        timed(s, runn, LOGGER::isInfoEnabled, LOGGER::info);
    }

    /**
     * Executes {@code suppl}, surrounding it with TRACE log messages that include the
     * step name and, on completion, the elapsed time.
     *
     * @param <T>   the return type of the supplier
     * @param s     human-readable name of the step, used in the log messages
     * @param suppl the work to execute and time
     * @return the value returned by {@code suppl}
     */
    public static <T> T trace(String s, ThrowingSupplier<T> suppl) {
        return timed(s, suppl, LOGGER::isTraceEnabled, LOGGER::trace);
    }

    /**
     * Executes {@code runn}, surrounding it with TRACE log messages that include the
     * step name and, on completion, the elapsed time.
     *
     * @param s    human-readable name of the step, used in the log messages
     * @param runn the work to execute and time
     */
    public static void trace(String s, ThrowingRunnable runn) {
        timed(s, runn, LOGGER::isTraceEnabled, LOGGER::trace);
    }

    private static <T> T timed(String s, ThrowingSupplier<T> run, BooleanSupplier isLogEnabled, Consumer<String> logger) {
        if (!isLogEnabled.getAsBoolean()) {
            return sneakyCall(run);
        }
        long start = System.currentTimeMillis();
        logger.accept(s + " started");
        T result = sneakyCall(run);
        logger.accept(s + " completed. Time=" + (System.currentTimeMillis() - start) + " ms");
        return result;
    }

    private static void timed(String s, ThrowingRunnable run, BooleanSupplier isLogEnabled, Consumer<String> logger) {
        timed(s, () -> {
            run.run(); return null;
        }, isLogEnabled, logger);
    }

    private static <T> T sneakyCall(ThrowingSupplier<T> run) {
        try {
            return run.get();
        } catch (Throwable t) {
            sneakyThrow(t);
            return null; // unreachable
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable t) throws E {
        throw (E) t;
    }

}
