package com.powsybl.flow_decomposition.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LogUtils {

  private LogUtils() {
    // empty
  }

  @FunctionalInterface
  public interface ThrowingSupplier<T> {
    T get() throws Throwable;
  }

  @FunctionalInterface
  public interface ThrowingRunnable {
    void run() throws Throwable;
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(LogUtils.class);

  public static <T> T info(String s, ThrowingSupplier<T> suppl) {
    long start = System.currentTimeMillis();
    LOGGER.info("{} started", s);
    T result = sneakyCall(suppl);
    LOGGER.info("{} completed. Time={}", s, System.currentTimeMillis() - start);
    return result;
  }

  public static void info(String s, ThrowingRunnable runn) {
    info(s, () -> {
      runn.run();
      return null;
    });
  }

  @SuppressWarnings("unchecked")
  private static <T> T sneakyCall(ThrowingSupplier<T> run) {
    try {
      return run.get();
    } catch (Throwable t) {
      return (T) sneakyThrow(t);
    }
  }

  @SuppressWarnings("unchecked")
  private static <E extends Throwable, R> R sneakyThrow(Throwable t) throws E {
    throw (E) t;
  }

}