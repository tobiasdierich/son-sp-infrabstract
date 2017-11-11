/*
 * Copyright (c) 2015 SONATA-NFV, UCL, NOKIA, NCSR Demokritos ALL RIGHTS RESERVED.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 * Neither the name of the SONATA-NFV, UCL, NOKIA, NCSR Demokritos nor the names of its contributors
 * may be used to endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * This work has been performed in the framework of the SONATA project, funded by the European
 * Commission under Grant number 671517 through the Horizon 2020 and 5G-PPP programmes. The authors
 * would like to acknowledge the contributions of their colleagues of the SONATA partner consortium
 * (www.sonata-nfv.eu).
 *
 * @author Dario Valocchi (Ph.D.), UCL
 *
 */

package sonata.kernel.vimadaptor.wrapper.kubernetes;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.LoggerFactory;

import sonata.kernel.vimadaptor.commons.*;
import sonata.kernel.vimadaptor.wrapper.ComputeWrapper;
import sonata.kernel.vimadaptor.wrapper.ResourceUtilisation;
import sonata.kernel.vimadaptor.wrapper.WrapperBay;
import sonata.kernel.vimadaptor.wrapper.WrapperConfiguration;
import sonata.kernel.vimadaptor.wrapper.WrapperStatusUpdate;

import io.fabric8.kubernetes.api.model.NodeList;
import sonata.kernel.vimadaptor.wrapper.terraform.TerraformException;
import sonata.kernel.vimadaptor.wrapper.terraform.TerraformTemplate;
import sonata.kernel.vimadaptor.wrapper.terraform.TerraformWrapper;

import java.io.IOException;
import java.util.Random;


public class KubernetesWrapper extends ComputeWrapper {

    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(KubernetesWrapper.class);

    private Random r;

    private String sid;

    private TerraformWrapper terraform;

    private ResponseHelper response;

    /**
     * Kubernetes related vars
     */
    private KubernetesClient client;
    private NodeList nodes;

    public KubernetesWrapper(WrapperConfiguration config) {
        super(config);
        this.r = new Random(System.currentTimeMillis());
        this.client = new KubernetesClient(config);
        this.terraform = new TerraformWrapper("/root/terraform_data/");
        this.response = new ResponseHelper(config);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * sonata.kernel.vimadaptor.wrapper.ComputeWrapper#deployFunction(sonata.kernel.vimadaptor.commons
     * .FunctionDeployPayload, java.lang.String)
     */
    @Override
    public void deployFunction(FunctionDeployPayload data, String sid) {
        Logger.error("[KubernetesWrapper] Received deploy function call. Ignoring.");
    }

    @Override
    public void deployCloudService(CloudServiceDeployPayload deployPayload, String sid) {
        Logger.info("[KubernetesWrapper] Received deploy cloud service call.");

        TerraformTemplate template = null;
        Logger.info("[KubernetesWrapper] Building Kubernetes template.");
        try {
            template = new KubernetesTerraformTemplate()
                    .forService(sid)
                    .withCsd(deployPayload.getCsd())
                    .withWrapperConfiguration(this.getConfig())
                    .build();
        } catch (Exception e) {
            Logger.error("[KubernetesWrapper] Failed to build Kubernetes template: " + e.getMessage());
            this.notifyDeploymentFailed(sid, "Failed to build Kubernetes template");

            return;
        }

        Logger.info("[KubernetesWrapper] Building Kubernetes template successful.");
        Logger.info("[KubernetesWrapper] Triggering terraform deployment.");

        try {
            this.terraform.forService(sid)
                    .writeTemplate(template)
                    .init()
                    .apply();
        } catch (TerraformException e) {
            Logger.error(e.getMessage());
            this.notifyDeploymentFailed(sid, "Failed to deploy service using terraform.");

            return;
        } catch (Exception e) {
            Logger.error("[KubernetesWrapper] Failed to run terraform command: " +  e.getMessage());
            this.notifyDeploymentFailed(sid, "Failed to deploy service using terraform.");

            return;
        }

        Logger.info("[KubernetesWrapper] Successfully deployed cloud service.");

        this.notifyDeploymentSuccessful(sid, deployPayload);
    }

    /**
     * Notify observers that the deployment was successful.
     */
    private void notifyDeploymentSuccessful(String sid, CloudServiceDeployPayload deployPayload) {
        CloudServiceDeployResponse response = this.response.buildDeployResponse(sid, deployPayload);

        try {
            String yaml = this.response.transformToYAML(response);
            WrapperStatusUpdate update = new WrapperStatusUpdate(sid, "SUCCESS", yaml);

            this.notifyUpdate(update);
        } catch (JsonProcessingException e) {
            this.notifyDeploymentFailed(sid, "Exception while sending deployment successful message.");
        }
    }

    /**
     * Notify observers that the deployment failed.
     *
     * @param error String
     */
    private void notifyDeploymentFailed(String sid, String error) {
        WrapperStatusUpdate update = new WrapperStatusUpdate(sid, "ERROR", error);
        this.notifyUpdate(update);
    }

    /**
     * Propagate status update to observers.
     *
     * @param update WrapperStatusUpdate
     */
    private void notifyUpdate(WrapperStatusUpdate update) {
        this.markAsChanged();
        this.notifyObservers(update);
    }

    @Deprecated
    @Override
    public boolean deployService(ServiceDeployPayload data, String callSid) {
        return false;
    }

    @Override
    public ResourceUtilisation getResourceUtilisation() {
        long start = System.currentTimeMillis();
        Logger.info("[KubernetesWrapper] Getting resource utilisation...");

        ResourceUtilisation resourceUtilisation = new ResourceUtilisation();

        try {
            resourceUtilisation = this.client.getClusterResourceUtilisation(this.getNodes());

            Logger.info("[KubernetesWrapper] Resource utilisation retrieved in " + (System.currentTimeMillis() - start) + "ms.");
        } catch (IOException e) {
            Logger.error("[KubernetesWrapper] Failed to retrieve resource utilisation. Error message: " + e.getMessage());
        }

        return resourceUtilisation;
    }

    /*
     * (non-Javadoc)
     *
     * @see sonata.kernel.vimadaptor.wrapper.ComputeWrapper#isImageStored(java.lang.String)
     */
    @Override
    public boolean isImageStored(VnfImage image, String callSid) {
        double avgTime = 1357.34;
        double stdTime = 683.96;
        waitGaussianTime(avgTime, stdTime);
        return r.nextBoolean();
    }

    /*
     * (non-Javadoc)
     *
     * @see sonata.kernel.vimadaptor.wrapper.ComputeWrapper#prepareService(java.lang.String)
     */
    @Override
    public boolean prepareService(String instanceId) {
        double avgTime = 10576.52;
        double stdTime = 1683.12;
        waitGaussianTime(avgTime, stdTime);
        WrapperBay.getInstance().getVimRepo().writeServiceInstanceEntry(instanceId, instanceId,
                instanceId, this.getConfig().getUuid());
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see sonata.kernel.vimadaptor.wrapper.ComputeWrapper#removeImage(java.lang.String)
     */
    @Override
    public void removeImage(VnfImage image) {
        this.setChanged();
        String body = "{\"status\":\"SUCCESS\"}";
        WrapperStatusUpdate update = new WrapperStatusUpdate(this.sid, "SUCCESS", body);
        this.notifyObservers(update);
    }

    @Override
    public boolean removeService(String instanceUuid, String callSid) {
        boolean out = true;

        double avgTime = 1309;
        double stdTime = 343;
        waitGaussianTime(avgTime, stdTime);

        this.setChanged();
        String body = "{\"status\":\"SUCCESS\"}";
        WrapperStatusUpdate update = new WrapperStatusUpdate(this.sid, "SUCCESS", body);
        this.notifyObservers(update);

        return out;
    }

    @Override
    public void scaleFunction(FunctionScalePayload data, String sid) {
        // TODO - smendel - add implementation and comments on function
    }

    @Override
    public String toString() {
        return "KubernetesWrapper-"+this.getConfig().getUuid();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * sonata.kernel.vimadaptor.wrapper.ComputeWrapper#uploadImage(sonata.kernel.vimadaptor.commons.
     * VnfImage)
     */
    @Override
    public void uploadImage(VnfImage image) throws IOException {

        double avgTime = 7538.75;
        double stdTime = 1342.06;
        waitGaussianTime(avgTime, stdTime);

        return;
    }

    private void waitGaussianTime(double avgTime, double stdTime) {
        double waitTime = Math.abs((r.nextGaussian() - 0.5) * stdTime + avgTime);
        //Logger.debug("Simulating processing delay.Waiting "+waitTime/1000.0+"s");
        try {
            Thread.sleep((long) Math.floor(waitTime));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get all nodes of the Kubernetes cluster.
     *
     * @return NodeList
     */
    private NodeList getNodes() {
        if (this.nodes == null) {
            return this.nodes = this.client.fetchNodes();
        }

        return this.nodes;
    }
}
