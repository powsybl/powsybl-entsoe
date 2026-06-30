package com.powsybl.flow_decomposition.utils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Georg Haider {@literal <georg.haider at artelys.com>}
 */
class LogUtilsTest {

    @Nested
    class WhenLoggingDisabled {

        // Default situation: logback-test.xml sets the root level to ERROR

        @Test
        void supplierShouldRun() {
            assertEquals(42, LogUtils.info("step", () -> 42));
        }

        @Test
        void supplierShouldThrowException() {
            IllegalStateException cause = new IllegalStateException("testing");
            assertSame(cause, assertThrows(IllegalStateException.class,
                    () -> LogUtils.info("step", (Supplier<Void>) () -> {
                        throw cause;
                    })));
        }

        @Test
        void runnableShouldRun() {
            assertEquals("hello", LogUtils.trace("step", () -> "hello"));
        }

        @Test
        void runnableShouldThrowException() {
            IllegalStateException cause = new IllegalStateException("testing");
            assertSame(cause, assertThrows(IllegalStateException.class,
                    () -> LogUtils.info("step", (Runnable) () -> {
                        throw cause;
                    })));
        }
    }

    @Nested
    class WhenLoggingEnabled {

        // we enable logging, overwriting logback-test.xml

        private final Logger logbackLogger = (Logger) LoggerFactory.getLogger(LogUtils.class);
        private ListAppender<ILoggingEvent> listAppender;
        private Level originalLevel;

        @BeforeEach
        void enableLogging() {
            originalLevel = logbackLogger.getLevel();
            logbackLogger.setLevel(Level.TRACE);
            listAppender = new ListAppender<>();
            listAppender.start();
            logbackLogger.addAppender(listAppender);
        }

        @AfterEach
        void restoreLogging() {
            logbackLogger.detachAppender(listAppender);
            logbackLogger.setLevel(originalLevel);
        }

        private List<String> capturedMessages() {
            return listAppender.list.stream()
                    .map(ILoggingEvent::getMessage)
                    .toList();
        }

        @Test
        void supplierShouldRun() {
            assertEquals(7, LogUtils.info("step", () -> 7));
        }

        @Test
        void runnableShouldRun() {
            assertEquals("hello", LogUtils.trace("step", () -> "hello"));
        }

        @Test
        void infoShouldLogStartAndCompleted() {
            LogUtils.info("myStep", () -> "ignored");

            List<String> messages = capturedMessages();
            assertEquals(2, messages.size());
            assertEquals("myStep started", messages.get(0));
            assertTrue(messages.get(1).startsWith("myStep completed. Time="));
        }

        @Test
        void infoShouldLogStartAndFailed() {
            assertThrows(RuntimeException.class,
                    () -> LogUtils.info("myStep", (Supplier<Void>) () -> {
                        throw new RuntimeException("testing");
                    }));

            List<String> messages = capturedMessages();
            assertEquals(2, messages.size());
            assertEquals("myStep started", messages.get(0));
            assertTrue(messages.get(1).startsWith("myStep failed. Time="));
        }

        @Test
        void traceShouldLogStartAndCompleted() {
            LogUtils.trace("myStep", () -> "ignored");

            List<String> messages = capturedMessages();
            assertEquals(2, messages.size());
            assertEquals("myStep started", messages.get(0));
            assertTrue(messages.get(1).startsWith("myStep completed. Time="));
        }

        @Test
        void traceShouldLogStartAndFailed() {
            assertThrows(RuntimeException.class,
                    () -> LogUtils.trace("myStep", (Supplier<Void>) () -> {
                        throw new RuntimeException("testing");
                    }));

            List<String> messages = capturedMessages();
            assertEquals(2, messages.size());
            assertEquals("myStep started", messages.get(0));
            assertTrue(messages.get(1).startsWith("myStep failed. Time="));
        }
    }
}
