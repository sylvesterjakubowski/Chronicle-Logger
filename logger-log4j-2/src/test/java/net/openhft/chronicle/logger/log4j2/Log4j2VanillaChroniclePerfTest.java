/*
 * Copyright 2014 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.logger.log4j2;

import net.openhft.lang.io.IOTools;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Ignore
public class Log4j2VanillaChroniclePerfTest extends Log4j2TestBase {

    // *************************************************************************
    // Single Thread
    // *************************************************************************

    @Test
    public void testSingleThreadLogging1() throws IOException {
        Thread.currentThread().setName("perf-plain-vanilla");

        final String testId = "perf-binary-vanilla-chronicle";
        final Logger clogger = LoggerFactory.getLogger(testId);
        final Logger plogger = LoggerFactory.getLogger("perf-plain-vanilla");
        final long items = 1000000;

        warmup(clogger);
        warmup(plogger);

        for (int s = 64; s <= 1024; s += 64) {
            final String staticStr = StringUtils.leftPad("", s, 'X');

            long cStart1 = System.nanoTime();

            for (int i = 1; i <= items; i++) {
                clogger.info(staticStr);
            }

            long cEnd1 = System.nanoTime();

            long pStart1 = System.nanoTime();

            for (int i = 1; i <= items; i++) {
                plogger.info(staticStr);
            }

            long pEnd1 = System.nanoTime();

            System.out.printf("items=%03d size=%04d => chronology=%.3f ms, chronology-average=%.3f us, plain=%d, plain-average=%.3f us\n",
                    items,
                    staticStr.length(),
                    (cEnd1 - cStart1) / 1e6,
                    (cEnd1 - cStart1) / items / 1e3,
                    (pEnd1 - pStart1),
                    (pEnd1 - pStart1) / items / 1e3);
        }

        IOTools.deleteDir(basePath(testId));
    }

    @Test
    public void testSingleThreadLogging2() throws IOException {
        Thread.currentThread().setName("perf-plain-vanilla");

        final String testId = "perf-binary-vanilla-chronicle";
        final Logger clogger = LoggerFactory.getLogger(testId);
        final Logger plogger = LoggerFactory.getLogger("perf-plain-vanilla");
        final long items = 1000000;
        final String strFmt = StringUtils.leftPad("> v1={}, v2={}, v3={}", 32, 'X');

        warmup(clogger);
        warmup(plogger);

        for (int n = 0; n < 10; n++) {
            long cStart1 = System.nanoTime();

            for (int i = 1; i <= items; i++) {
                clogger.info(strFmt, i, i * 7, i / 16);
            }

            long cEnd1 = System.nanoTime();

            long pStart1 = System.nanoTime();

            for (int i = 1; i <= items; i++) {
                plogger.info(strFmt, i, i * 7, i / 16);
            }

            long pEnd1 = System.nanoTime();

            System.out.printf("items=%03d => chronology=%.3f ms, chronology-average=%.3f us, plain=%d, plain-average=%.3f us\n",
                    items,
                    (cEnd1 - cStart1) / 1e6,
                    (cEnd1 - cStart1) / items / 1e3,
                    (pEnd1 - pStart1),
                    (pEnd1 - pStart1) / items / 1e3);
        }

        IOTools.deleteDir(basePath(testId));
    }

    @Ignore
    @Test
    public void testSingleThreadLoggin3() throws IOException {
        Thread.currentThread().setName("perf-plain-vanilla");

        final String testId = "perf-binary-vanilla-chronicle";
        final Logger clogger = LoggerFactory.getLogger(testId);
        final Logger plogger = LoggerFactory.getLogger("perf-plain-vanilla-async");
        final long items = 1000000;
        final String strFmt = StringUtils.leftPad("> v1={}, v2={}, v3={}", 32, 'X');

        warmup(clogger);
        warmup(plogger);

        for (int n = 0; n < 10; n++) {
            long cStart1 = System.nanoTime();

            for (int i = 1; i <= items; i++) {
                clogger.info(strFmt, i, i * 7, i / 16);
            }

            long cEnd1 = System.nanoTime();

            long pStart1 = System.nanoTime();

            for (int i = 1; i <= items; i++) {
                plogger.info(strFmt, i, i * 7, i / 16);
            }

            long pEnd1 = System.nanoTime();

            System.out.printf("items=%03d => chronology=%.3f ms, chronology-average=%.3f us, plain=%d, plain-average=%.3f us\n",
                    items,
                    (cEnd1 - cStart1) / 1e6,
                    (cEnd1 - cStart1) / items / 1e3,
                    (pEnd1 - pStart1),
                    (pEnd1 - pStart1) / items / 1e3);
        }

        IOTools.deleteDir(basePath(testId));
    }

    // *************************************************************************
    // Multi Thread
    // *************************************************************************

    @Test
    public void testMultiThreadLogging() throws IOException, InterruptedException {

        warmup(LoggerFactory.getLogger("perf-binary-vanilla-chronicle"));
        warmup(LoggerFactory.getLogger("perf-plain-vanilla"));

        final int RUNS = 250000; // ~10s
        final int THREADS = Runtime.getRuntime().availableProcessors();

        for (int size : new int[]{64, 128, 256}) {
            {
                final long start = System.nanoTime();

                ExecutorService es = Executors.newFixedThreadPool(THREADS);
                for (int t = 0; t < THREADS; t++) {
                    es.submit(new RunnableLogger(RUNS, size, "perf-binary-vanilla-chronicle"));
                }

                es.shutdown();
                es.awaitTermination(2, TimeUnit.MINUTES);

                final long time = System.nanoTime() - start;

                System.out.printf("ChronicleLog.MT (runs=%d, min size=%03d, elapsed=%.3f ms) took an average of %.3f us per entry\n",
                        RUNS,
                        size,
                        time / 1e6,
                        time / 1e3 / (RUNS * THREADS)
                );
            }

            {
                final long start = System.nanoTime();

                ExecutorService es = Executors.newFixedThreadPool(THREADS);
                for (int t = 0; t < THREADS; t++) {
                    es.submit(new RunnableLogger(RUNS, size, "perf-plain-vanilla"));
                }

                es.shutdown();
                es.awaitTermination(10, TimeUnit.MINUTES);

                final long time = System.nanoTime() - start;

                System.out.printf("Plain.MT (runs=%d, min size=%03d, elapsed=%.3f ms)): took an average of %.3f us per entry\n",
                        RUNS,
                        size,
                        time / 1e6,
                        time / 1e3 / (RUNS * THREADS)
                );
            }
        }

        IOTools.deleteDir(basePath("perf-binary-vanilla-chronicle"));
    }
}
