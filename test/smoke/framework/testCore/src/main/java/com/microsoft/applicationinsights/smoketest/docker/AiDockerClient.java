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

package com.microsoft.applicationinsights.smoketest.docker;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import com.microsoft.applicationinsights.smoketest.exceptions.SmokeTestException;
import com.microsoft.applicationinsights.smoketest.exceptions.TimeoutException;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

public class AiDockerClient {

  public static final String DEFAULT_LINUX_USER = "root";
  public static final String DEFAULT_LINUX_SHELL = "bash";

  private final String shellExecutor;

  public AiDockerClient(String user, String shellExecutor) {
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(shellExecutor, "shellExecutor");

    this.shellExecutor = shellExecutor;
  }

  public String getShellExecutor() {
    return this.shellExecutor;
  }

  public static AiDockerClient createLinuxClient() {
    return new AiDockerClient(DEFAULT_LINUX_USER, DEFAULT_LINUX_SHELL);
  }

  private static ProcessBuilder buildProcess(String... cmdLine) {
    return new ProcessBuilder(cmdLine).redirectErrorStream(true);
  }

  private static ProcessBuilder buildProcess(List<String> cmdLine) {
    return new ProcessBuilder(cmdLine).redirectErrorStream(true);
  }

  public static String startDependencyContainer(
      String image, String[] envVars, String portMapping, String network, String containerName)
      throws IOException, InterruptedException {
    Map<String, String> envVarMap = new HashMap<>();
    for (String envVar : envVars) {
      int index = envVar.indexOf('=');
      envVarMap.put(envVar.substring(0, index), envVar.substring(index + 1));
    }
    return startContainer(image, portMapping, network, containerName, envVarMap, true);
  }

  public static String startContainer(
      String image,
      String portMapping,
      String network,
      String containerName,
      Map<String, String> envVars,
      boolean dependencyContainer)
      throws IOException, InterruptedException {
    Objects.requireNonNull(image, "image");
    Objects.requireNonNull(portMapping, "portMapping");

    buildProcess(Arrays.asList("docker", "pull", image)).start().waitFor(30, TimeUnit.SECONDS);

    List<String> cmd = new ArrayList<>(Arrays.asList("docker", "run", "-d", "-p", portMapping));
    if (SystemUtils.IS_OS_UNIX) {
      // see https://stackoverflow.com/a/24326540/295416
      cmd.addAll(Arrays.asList("--add-host", "host.docker.internal:host-gateway"));
    }
    if (!dependencyContainer) {
      // port 5005 is exposed for hooking up IDE debugger
      cmd.addAll(Arrays.asList("-p", "5005:5005"));
    }
    if (!Strings.isNullOrEmpty(network)) {
      cmd.add("--network");
      cmd.add(network);
    }
    if (!Strings.isNullOrEmpty(containerName)) {
      cmd.add("--name");
      cmd.add(containerName);
    }
    if (envVars != null && !envVars.isEmpty()) {
      for (Entry<String, String> entry : envVars.entrySet()) {
        if (entry.getKey() == null || entry.getValue() == null) {
          continue;
        }
        cmd.add("--env");
        cmd.add(String.format("%s=%s", entry.getKey(), entry.getValue()));
      }
    }
    cmd.add(image);
    Process p = buildProcess(cmd).start();
    final int timeout = 30;
    final TimeUnit unit = TimeUnit.SECONDS;
    waitAndCheckCodeForProcess(p, timeout, unit, "starting container " + image);

    return getFirstLineOfProcessOutput(p);
  }

  @SuppressWarnings("SystemOut")
  private static void flushStdout(Process p) {
    Objects.requireNonNull(p);

    try (Scanner r = new Scanner(p.getInputStream(), UTF_8.name())) {
      while (r.hasNext()) {
        System.out.println(r.nextLine());
      }
    }
  }

  public void copyAndDeployToContainer(String id, File appArchive)
      throws IOException, InterruptedException {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(appArchive, "appArchive");

    Process p =
        buildProcess(
                "docker",
                "cp",
                appArchive.getAbsolutePath(),
                String.format("%s:%s", id, "/root/docker-stage"))
            .start();
    waitAndCheckCodeForProcess(
        p,
        10,
        TimeUnit.SECONDS,
        String.format("copy %s to container %s", appArchive.getPath(), id));

    execOnContainer(id, getShellExecutor(), "./deploy.sh", appArchive.getName());
  }

  public void execOnContainer(String id, String cmd, String... args)
      throws IOException, InterruptedException {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(cmd, "cmd");

    List<String> cmdList = new ArrayList<>();
    cmdList.addAll(Arrays.asList("docker", "container", "exec", id, cmd));
    if (args.length > 0) {
      cmdList.addAll(Arrays.asList(args));
    }
    Process p = buildProcess(cmdList).start();
    waitAndCheckCodeForProcess(
        p,
        10,
        TimeUnit.SECONDS,
        String.format("executing command on container %s: '%s'", id, Joiner.on(' ').join(cmdList)),
        id);
    flushStdout(p);
  }

  private static void waitAndCheckCodeForProcess(
      Process p, long timeout, TimeUnit unit, String actionName)
      throws IOException, InterruptedException {
    waitAndCheckCodeForProcess(p, timeout, unit, actionName, null);
  }

  private static void waitAndCheckCodeForProcess(
      Process p, long timeout, TimeUnit unit, String actionName, String containerId)
      throws IOException, InterruptedException {
    waitForProcessToReturn(p, timeout, unit, actionName);
    if (p.exitValue() != 0) {
      if (containerId != null) {
        printContainerLogs(containerId);
      }
      flushStdout(p);
      throw new SmokeTestException(
          String.format(
              "Nonzero exit code (%d)%s",
              p.exitValue(), Strings.isNullOrEmpty(actionName) ? "" : " trying to " + actionName));
    }
  }

  private static void waitForProcessToReturn(
      Process p, long timeout, TimeUnit unit, String actionName)
      throws IOException, InterruptedException {
    if (!p.waitFor(timeout, unit)) {
      String containerId = getFirstLineOfProcessOutput(p);
      printContainerLogs(containerId);
      p.destroyForcibly();
      flushStdout(p);
      throw new TimeoutException(
          Strings.isNullOrEmpty(actionName) ? "process" : actionName, timeout, unit);
    }
  }

  public static void printContainerLogs(String containerId) throws IOException {
    Objects.requireNonNull(containerId, "containerId");

    Process p = buildProcess("docker", "container", "logs", containerId).start();
    flushStdout(p);
  }

  public void stopContainer(String id) throws IOException, InterruptedException {
    Process p = buildProcess("docker", "container", "stop", id).start();
    waitAndCheckCodeForProcess(p, 30, TimeUnit.SECONDS, String.format("stopping container %s", id));
  }

  @SuppressWarnings("SystemOut")
  public boolean isContainerRunning(String id) throws IOException, InterruptedException {
    Process p = new ProcessBuilder("docker", "inspect", "-f", "{{.State.Running}}", id).start();
    waitAndCheckCodeForProcess(
        p, 10, TimeUnit.SECONDS, String.format("checking if container is running: %s", id));

    try (StringWriter sw = new StringWriter()) {
      CharStreams.copy(new InputStreamReader(p.getInputStream(), UTF_8), sw);
      System.out.printf("Checking for running container %s: %s%n", id, sw);
      return Boolean.parseBoolean(sw.toString());
    }
  }

  public String createNetwork(String name) throws IOException, InterruptedException {
    Process p = buildProcess("docker", "network", "create", "--driver", "bridge", name).start();
    waitAndCheckCodeForProcess(p, 10, TimeUnit.SECONDS, "creating network");
    return getFirstLineOfProcessOutput(p);
  }

  private static String getFirstLineOfProcessOutput(Process p) throws IOException {
    List<String> lines = CharStreams.readLines(new InputStreamReader(p.getInputStream(), UTF_8));
    return StringUtils.strip(StringUtils.trim(lines.get(0)));
  }

  public String deleteNetwork(String nameOrId) throws IOException, InterruptedException {
    Process p = buildProcess("docker", "network", "rm", nameOrId).start();
    waitAndCheckCodeForProcess(p, 10, TimeUnit.SECONDS, "deleting network");
    return getFirstLineOfProcessOutput(p);
  }
}
