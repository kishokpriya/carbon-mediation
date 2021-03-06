/*
 *  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.inbound.endpoint.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.task.Task;
import org.wso2.carbon.mediation.clustering.ClusteringAgentUtil;
import org.wso2.carbon.utils.CarbonUtils;

/**
 * 
 * InboundRunner class is used to run the non coordinated processors in
 * background according to the scheduled interval
 * 
 */
public class InboundRunner implements Runnable {

    private Task task;
    private long interval;

    private volatile boolean execute = true;
    private volatile boolean init = false;

    private static final Log log = LogFactory.getLog(InboundRunner.class);

    public InboundRunner(Task task, long interval) {
        this.task = task;
        this.interval = interval;
    }

    /**
     * Exit the running while loop and terminate the thread
     */
    protected void terminate() {
        execute = false;
    }

    @Override
    public void run() {
        log.debug("Starting the Inbound Endpoint.");
        // Wait for the clustering configuration to be loaded.
        while (!init) {
            log.debug("Waiting for the configuration context to be loaded to run Inbound Endpoint.");
            Boolean isSinglNode = ClusteringAgentUtil.isSingleNode();
            if (isSinglNode != null) {
                if (!isSinglNode && !CarbonUtils.isWorkerNode()) {
                    // Given node is the manager in the cluster, and not
                    // required to run the service
                    execute = false;
                    log.info("Inbound EP will not run in manager node. Same will run on worker(s).");
                }
                init = true;
            }
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                log.warn(
                        "Unable to sleep the inbound thread for interval of : " + interval + "ms.",
                        e);
            }
        }
        
        
        log.debug("Configuration context loaded. Running the Inbound Endpoint.");
        // Run the poll cycles
        while (execute) {
            log.debug("Executing the Inbound Endpoint.");
            try {
                task.execute();
            } catch (Exception e) {
                log.error("Error executing the inbound endpoint polling cycle.", e);
            }
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                log.warn(
                        "Unable to sleep the inbound thread for interval of : " + interval + "ms.",
                        e);
            }
        }
        log.debug("Exit the Inbound Endpoint running loop.");
    }

}
