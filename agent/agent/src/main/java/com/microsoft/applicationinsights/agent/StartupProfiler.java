/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.agent;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

final class StartupProfiler {

  private static final String STACKTRACES = "stacktrace.txt";

  @SuppressWarnings("SystemOut")
  public static void start() {
    String tempDirectory = System.getProperty("java.io.tmpdir");
    File folder = new File(tempDirectory, "applicationinsights");
    if (!folder.exists()) {
      folder.mkdirs();
    }

    File dumpFile = new File(folder, STACKTRACES);
    System.out.println("Writing startup profiler to '" + dumpFile.getPath() + "'");

    PrintWriter printWriter = null;
    try (PrintWriter out =
        new PrintWriter(Files.newBufferedWriter(dumpFile.toPath(), Charset.defaultCharset()))) {
      printWriter = new PrintWriter(out);
    } catch (IOException e) {
      if (printWriter != null) {
        printWriter.close();
      }
      System.out.println("Error occurred when writing dump to " + dumpFile.getPath());
      e.printStackTrace();
      return;
    }

    start(printWriter);
  }

  private static void start(PrintWriter out) {
    Executors.newSingleThreadScheduledExecutor()
        .scheduleAtFixedRate(new ThreadDump(out), 50, 50, TimeUnit.MILLISECONDS);
  }

  private StartupProfiler() {}

  private static class ThreadDump implements Runnable {

    private final PrintWriter out;

    private ThreadDump(PrintWriter out) {
      this.out = out;
    }

    @Override
    public void run() {
      out.println("========================================");
      RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
      out.println(runtimeBean.getUptime());
      out.println();
      ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
      ThreadInfo[] threadInfos =
          threadBean.getThreadInfo(
              threadBean.getAllThreadIds(), threadBean.isObjectMonitorUsageSupported(), false);
      long currentThreadId = Thread.currentThread().getId();
      for (ThreadInfo threadInfo : threadInfos) {
        if (threadInfo.getThreadId() != currentThreadId) {
          write(threadInfo);
        }
      }
      out.flush();
    }

    private void write(ThreadInfo threadInfo) {
      out.println(threadInfo.getThreadName() + " #" + threadInfo.getThreadId());
      out.println("   java.lang.Thread.State: " + threadInfo.getThreadState());
      for (StackTraceElement ste : threadInfo.getStackTrace()) {
        out.println("        " + ste);
      }
      out.println();
    }
  }
}
