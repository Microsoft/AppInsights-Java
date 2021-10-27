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

package com.microsoft.applicationinsights.agent.internal.localstorage;

import com.microsoft.applicationinsights.agent.internal.common.OperationLogger;
import com.microsoft.applicationinsights.agent.internal.statsbeat.NonessentialStatsbeat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

/** This class manages writing a list of {@link ByteBuffer} to the file system. */
public final class LocalFileWriter {

  // 50MB per folder for all apps.
  private static final long MAX_FILE_SIZE_IN_BYTES = 52428800; // 50MB
  private static final String PERMANENT_FILE_EXTENSION = ".trn";

  private final LocalFileCache localFileCache;
  private final File telemetryFolder;
  // this is null for Statsbeat telemetry
  @Nullable private final NonessentialStatsbeat nonessentialStatsbeat;

  private static final OperationLogger operationLogger =
      new OperationLogger(
          LocalFileWriter.class, "Writing telemetry to disk (telemetry is discarded on failure)");

  public LocalFileWriter(
      LocalFileCache localFileCache,
      File telemetryFolder,
      @Nullable NonessentialStatsbeat nonessentialStatsbeat) {
    this.telemetryFolder = telemetryFolder;
    this.localFileCache = localFileCache;
    this.nonessentialStatsbeat = nonessentialStatsbeat;
  }

  public void writeToDisk(List<ByteBuffer> buffers) {
    long size = getTotalSizeOfPersistedFiles(telemetryFolder);
    if (size >= MAX_FILE_SIZE_IN_BYTES) {
      operationLogger.recordFailure(
          "Local persistent storage capacity has been reached. It's currently at ("
              + (size / 1024)
              + "KB). Telemetry will be lost");
      incrementWriteFailureCount();
      return;
    }

    File tempFile;
    try {
      tempFile = createTempFile(telemetryFolder);
    } catch (IOException e) {
      operationLogger.recordFailure("unable to create temporary file: " + e, e);
      incrementWriteFailureCount();
      return;
    }

    try {
      write(tempFile, buffers);
    } catch (IOException e) {
      operationLogger.recordFailure(String.format("unable to write to file: %s", e), e);
      incrementWriteFailureCount();
      return;
    }

    File permanentFile;
    try {
      String filename = tempFile.getName();
      File sourceFile = new File(telemetryFolder, filename);
      permanentFile =
          new File(telemetryFolder, FilenameUtils.getBaseName(filename) + PERMANENT_FILE_EXTENSION);
      FileUtils.moveFile(sourceFile, permanentFile);
    } catch (IOException e) {
      operationLogger.recordFailure(
          "Fail to change "
              + tempFile.getName()
              + " to have "
              + PERMANENT_FILE_EXTENSION
              + " extension: ",
          e);
      incrementWriteFailureCount();
      return;
    }

    localFileCache.addPersistedFilenameToMap(permanentFile.getName());

    operationLogger.recordSuccess();
  }

  private void incrementWriteFailureCount() {
    if (nonessentialStatsbeat != null) {
      nonessentialStatsbeat.incrementWriteFailureCount();
    }
  }

  private static void write(File file, List<ByteBuffer> buffers) throws IOException {
    try (FileChannel channel = new FileOutputStream(file).getChannel()) {
      for (ByteBuffer byteBuffer : buffers) {
        channel.write(byteBuffer);
      }
    }
  }

  private static File createTempFile(File telemetryFolder) throws IOException {
    String prefix = System.currentTimeMillis() + "-";
    return File.createTempFile(prefix, null, telemetryFolder);
  }

  private static long getTotalSizeOfPersistedFiles(File telemetryFolder) {
    if (!telemetryFolder.exists()) {
      return 0;
    }

    long sum = 0;
    Collection<File> files = FileUtils.listFiles(telemetryFolder, new String[] {"trn"}, false);
    for (File file : files) {
      sum += file.length();
    }

    return sum;
  }
}
