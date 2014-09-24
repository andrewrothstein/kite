/**
 * Copyright 2014 Cloudera Inc.
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
package org.kitesdk.minicluster.cli;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;

import org.apache.hadoop.conf.Configuration;
import org.kitesdk.minicluster.MiniCluster;
import org.kitesdk.minicluster.Service;
import org.slf4j.Logger;

@Parameters(commandDescription = "Run a minicluster")
public class RunCommand implements Command {

  private static final Map<String, String> simpleServiceNameMap = Maps
      .newHashMap();
  static {
    simpleServiceNameMap.put("hdfs", "org.kitesdk.minicluster.HdfsService");
    simpleServiceNameMap.put("zk", "org.kitesdk.minicluster.ZookeeperService");
    simpleServiceNameMap.put("hbase", "org.kitesdk.minicluster.HBaseService");
  }

  private final Logger console;
  private final Configuration conf;

  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "UWF_NULL_FIELD", justification = "Field set by JCommander")
  @Parameter(names = { "-d", "--work-dir" }, description = "The base directory to store mini cluster data in.")
  String workDir = "/tmp/kite-minicluster";

  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "UWF_NULL_FIELD", justification = "Field set by JCommander")
  @Parameter(names = { "-b", "--bind" }, description = "The IP address for all mini cluster services to bind to.")
  String bindIP = "127.0.0.1";

  @Parameter(names = { "-c", "--clean" }, description = "Clean the mini cluster data directory before starting.")
  boolean clean = false;

  @Parameter(names = { "-s", "--services" }, variableArity = true, description = "A space separated list of fully "
      + "qualified service classnames to run. The following short names exist: hdfs, zk, hbase ")
  public List<String> services = new ArrayList<String>();

  @DynamicParameter(names = "-D", description = "Service specific configs go here. These configs are passed through "
      + "to the ServiceConfig using the key/value specified here.")
  private Map<String, String> serviceParams = new HashMap<String, String>();

  public RunCommand(Logger console, Configuration conf) {
    this.console = console;
    this.conf = conf;
  }

  @SuppressWarnings("unchecked")
  @Override
  public int run() throws IOException {
    Preconditions.checkArgument(services.size() > 0,
        "At least one service must be specified.");
    Preconditions.checkArgument(workDir != null,
        "A valid work dir is required.");

    MiniCluster.Builder builder = new MiniCluster.Builder().workDir(workDir)
        .clean(clean).hadoopConf(conf).bindIP(bindIP);
    for (String serviceName : services) {
      if (simpleServiceNameMap.containsKey(serviceName)) {
        serviceName = simpleServiceNameMap.get(serviceName);
      }
      try {
        builder.addService((Class<? extends Service>) Class
            .forName(serviceName));
      } catch (ClassNotFoundException e) {
        console.error("Unknown service class specified: " + serviceName);
        throw new RuntimeException(e);
      }
    }
    for (Entry<String, String> serviceParam : serviceParams.entrySet()) {
      builder.setServiceConfig(serviceParam.getKey(), serviceParam.getValue());
    }
    final MiniCluster miniCluster = builder.build();

    // Create an exit thread that listens for a kill command, and notifies the
    // main thread to exit.
    final CountDownLatch doneSignal = new CountDownLatch(1);
    Thread exitThread = new Thread() {
      @Override
      public void run() {
        try {
          miniCluster.stop();
        } catch (Throwable e) {
          console.error("Error stopping mini cluster. Exiting anyways...", e);
        }
        doneSignal.countDown();
      }
    };
    Runtime.getRuntime().addShutdownHook(exitThread);

    // Start the mini cluster, and wait for the exit notification.
    try {
      miniCluster.start();
      doneSignal.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return 1;
    }
    return 0;
  }

  @Override
  public List<String> getExamples() {
    return Lists
        .newArrayList(
            "# Run a mini HDFS cluster:",
            "-s hdfs -d /tmp/kite-minicluster",
            "# Run an HBase cluster that forces everything to listen on IP 10.0.0.1:",
            "-s hdfs zk hbase -d /tmp/kite-minicluster -b 10.0.0.1",
            "# Run an HBase cluster, cleaning out any data from previous cluster runs:",
            "-s hdfs zk hbase -d /tmp/kite-minicluster -c",
            "# Run an HBase cluster with custom ports using the service configs:",
            "-s hdfs zk hbase -d /tmp/kite-minicluster -Dhbase-master-port=63000 -Dhbase-regionserver-port=63020");
  }

}
