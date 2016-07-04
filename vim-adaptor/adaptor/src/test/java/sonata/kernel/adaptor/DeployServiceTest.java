
package sonata.kernel.adaptor;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.json.JSONObject;
import org.json.JSONTokener;

import sonata.kernel.adaptor.commons.DeployServiceData;
import sonata.kernel.adaptor.commons.DeployServiceResponse;
import sonata.kernel.adaptor.commons.ResourceAvailabilityData;
import sonata.kernel.adaptor.commons.Status;
import sonata.kernel.adaptor.commons.VnfRecord;
import sonata.kernel.adaptor.commons.nsd.ServiceDescriptor;
import sonata.kernel.adaptor.commons.vnfd.Unit;
import sonata.kernel.adaptor.commons.vnfd.Unit.MemoryUnit;
import sonata.kernel.adaptor.commons.vnfd.UnitDeserializer;
import sonata.kernel.adaptor.commons.vnfd.VnfDescriptor;
import sonata.kernel.adaptor.messaging.ServicePlatformMessage;
import sonata.kernel.adaptor.messaging.TestConsumer;
import sonata.kernel.adaptor.messaging.TestProducer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class DeployServiceTest extends TestCase implements MessageReceiver {
  private String output = null;
  private Object mon = new Object();
  private TestConsumer consumer;
  private String lastHeartbeat;
  private DeployServiceData data;
  private ObjectMapper mapper;

  /**
   * Create the test case
   *
   * @param testName name of the test case
   */
  public DeployServiceTest(String testName) throws Exception {
    super(testName);

    ServiceDescriptor sd;
    StringBuilder bodyBuilder = new StringBuilder();
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(new File("./YAML/sonata-demo.yml")), Charset.forName("UTF-8")));
    String line;
    while ((line = in.readLine()) != null)
      bodyBuilder.append(line + "\n\r");
    this.mapper = new ObjectMapper(new YAMLFactory());
    SimpleModule module = new SimpleModule();
    module.addDeserializer(Unit.class, new UnitDeserializer());
    mapper.registerModule(module);
    mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    mapper.disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
    mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
    mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
    mapper.setSerializationInclusion(Include.NON_NULL);

    sd = mapper.readValue(bodyBuilder.toString(), ServiceDescriptor.class);

    VnfDescriptor vnfd1;
    bodyBuilder = new StringBuilder();
    in = new BufferedReader(new InputStreamReader(
        new FileInputStream(new File("./YAML/iperf-vnfd.yml")), Charset.forName("UTF-8")));
    line = null;
    while ((line = in.readLine()) != null)
      bodyBuilder.append(line + "\n\r");
    vnfd1 = mapper.readValue(bodyBuilder.toString(), VnfDescriptor.class);

    VnfDescriptor vnfd2;
    bodyBuilder = new StringBuilder();
    in = new BufferedReader(new InputStreamReader(
        new FileInputStream(new File("./YAML/firewall-vnfd.yml")), Charset.forName("UTF-8")));
    line = null;
    while ((line = in.readLine()) != null)
      bodyBuilder.append(line + "\n\r");
    vnfd2 = mapper.readValue(bodyBuilder.toString(), VnfDescriptor.class);


    VnfDescriptor vnfd3;
    bodyBuilder = new StringBuilder();
    in = new BufferedReader(new InputStreamReader(
        new FileInputStream(new File("./YAML/tcpdump-vnfd.yml")), Charset.forName("UTF-8")));
    line = null;
    while ((line = in.readLine()) != null)
      bodyBuilder.append(line + "\n\r");
    vnfd3 = mapper.readValue(bodyBuilder.toString(), VnfDescriptor.class);

    this.data = new DeployServiceData();

    data.setServiceDescriptor(sd);
    data.addVnfDescriptor(vnfd1);
    data.addVnfDescriptor(vnfd2);
    data.addVnfDescriptor(vnfd3);

  }

  /**
   * @return the suite of tests being tested
   */
  public static Test suite() {
    return new TestSuite(DeployServiceTest.class);
  }

  public void testCheckResources() throws IOException, InterruptedException {

    BlockingQueue<ServicePlatformMessage> muxQueue =
        new LinkedBlockingQueue<ServicePlatformMessage>();
    BlockingQueue<ServicePlatformMessage> dispatcherQueue =
        new LinkedBlockingQueue<ServicePlatformMessage>();

    TestProducer producer = new TestProducer(muxQueue, this);
    consumer = new TestConsumer(dispatcherQueue);
    AdaptorCore core = new AdaptorCore(muxQueue, dispatcherQueue, consumer, producer, 0.1);

    core.start();
    int counter = 0;

    try {
      while (counter < 2) {
        synchronized (mon) {
          mon.wait();
          if (lastHeartbeat.contains("RUNNING")) counter++;
        }
      }
    } catch (Exception e) {
      assertTrue(false);
    }

    String message =
        "{\"wr_type\":\"compute\",\"tenant_ext_net\":\"ext-subnet\",\"tenant_ext_router\":\"ext-router\",\"vim_type\":\"Mock\",\"vim_address\":\"http://localhost:9999\",\"username\":\"Eve\",\"pass\":\"Operator\",\"tenant\":\"operator\"}";
    String topic = "infrastructure.management.compute.add";
    ServicePlatformMessage addVimMessage = new ServicePlatformMessage(message, "application/json",
        topic, UUID.randomUUID().toString(), topic);
    consumer.injectMessage(addVimMessage);
    Thread.sleep(2000);
    while (output == null)
      synchronized (mon) {
        mon.wait(1000);
      }

    JSONTokener tokener = new JSONTokener(output);
    JSONObject jsonObject = (JSONObject) tokener.nextValue();
    String status = jsonObject.getString("status");
    String wrUuid = jsonObject.getString("uuid");
    assertTrue(status.equals("COMPLETED"));
    System.out.println("Mock Wrapper added, with uuid: " + wrUuid);

    ResourceAvailabilityData data = new ResourceAvailabilityData();

    data.setCpu(4);
    data.setMemory(10);
    data.setMemoryUnit(MemoryUnit.GB);
    data.setStorage(50);
    data.setStorageUnit(MemoryUnit.GB);
    topic = "infrastructure.management.compute.resourceAvailability";


    message = mapper.writeValueAsString(data);

    ServicePlatformMessage checkResourcesMessage = new ServicePlatformMessage(message,
        "application/x-yaml", topic, UUID.randomUUID().toString(), topic);

    output = null;
    consumer.injectMessage(checkResourcesMessage);
    Thread.sleep(2000);
    while (output == null) {
      synchronized (mon) {
        mon.wait(1000);
      }
    }
    assertTrue(output.contains("OK"));
    message = "{\"wr_type\":\"compute\",\"uuid\":\"" + wrUuid + "\"}";
    topic = "infrastructure.management.compute.remove";
    ServicePlatformMessage removeVimMessage = new ServicePlatformMessage(message,
        "application/json", topic, UUID.randomUUID().toString(), topic);
    consumer.injectMessage(removeVimMessage);
    output = null;
    while (output == null) {
      synchronized (mon) {
        mon.wait(1000);
      }
    }


    tokener = new JSONTokener(output);
    jsonObject = (JSONObject) tokener.nextValue();
    status = jsonObject.getString("status");
    assertTrue(status.equals("COMPLETED"));
    core.stop();

  }

  public void testDeployServiceMock() throws IOException, InterruptedException {


    BlockingQueue<ServicePlatformMessage> muxQueue =
        new LinkedBlockingQueue<ServicePlatformMessage>();
    BlockingQueue<ServicePlatformMessage> dispatcherQueue =
        new LinkedBlockingQueue<ServicePlatformMessage>();

    TestProducer producer = new TestProducer(muxQueue, this);
    consumer = new TestConsumer(dispatcherQueue);
    AdaptorCore core = new AdaptorCore(muxQueue, dispatcherQueue, consumer, producer, 0.1);

    core.start();
    int counter = 0;

    try {
      while (counter < 2) {
        synchronized (mon) {
          mon.wait();
          if (lastHeartbeat.contains("RUNNING")) counter++;
        }
      }
    } catch (Exception e) {
      assertTrue(false);
    }


    String message =
        "{\"wr_type\":\"compute\",\"tenant_ext_net\":\"ext-subnet\",\"tenant_ext_router\":\"ext-router\",\"vim_type\":\"Mock\",\"vim_address\":\"http://localhost:9999\",\"username\":\"Eve\",\"pass\":\"Operator\",\"tenant\":\"op_sonata\"}";
    String topic = "infrastructure.management.compute.add";
    ServicePlatformMessage addVimMessage = new ServicePlatformMessage(message, "application/json",
        topic, UUID.randomUUID().toString(), topic);
    consumer.injectMessage(addVimMessage);
    Thread.sleep(2000);
    while (output == null)
      synchronized (mon) {
        mon.wait(1000);
      }

    JSONTokener tokener = new JSONTokener(output);
    JSONObject jsonObject = (JSONObject) tokener.nextValue();
    String status = jsonObject.getString("status");
    String wrUuid = jsonObject.getString("uuid");
    assertTrue(status.equals("COMPLETED"));
    System.out.println("Mock Wrapper added, with uuid: " + wrUuid);

    output = null;
    data.setVimUuid(wrUuid);

    String body = mapper.writeValueAsString(data);

    topic = "infrastructure.service.deploy";
    ServicePlatformMessage deployServiceMessage = new ServicePlatformMessage(body,
        "application/x-yaml", topic, UUID.randomUUID().toString(), topic);

    consumer.injectMessage(deployServiceMessage);

    Thread.sleep(2000);
    while (output == null)
      synchronized (mon) {
        mon.wait(1000);
      }
    assertNotNull(output);
    int retry = 0;
    int maxRetry = 60;
    while (output.contains("heartbeat") || output.contains("Vim Added") && retry < maxRetry)
      synchronized (mon) {
        mon.wait(1000);
        retry++;
      }

    assertTrue("No Deploy service response received", retry < maxRetry);

    DeployServiceResponse response = mapper.readValue(output, DeployServiceResponse.class);
    assertTrue(response.getRequestStatus().equals("DEPLOYED"));
    assertTrue(response.getNsr().getStatus() == Status.normal_operation);

    for (VnfRecord vnfr : response.getVnfrs())
      assertTrue(vnfr.getStatus() == Status.normal_operation);
    output = null;
    message = "{\"wr_type\":\"compute\",\"uuid\":\"" + wrUuid + "\"}";
    topic = "infrastructure.management.compute.remove";
    ServicePlatformMessage removeVimMessage = new ServicePlatformMessage(message,
        "application/json", topic, UUID.randomUUID().toString(), topic);
    consumer.injectMessage(removeVimMessage);

    while (output == null) {
      synchronized (mon) {
        mon.wait(1000);
      }
    }

    tokener = new JSONTokener(output);
    jsonObject = (JSONObject) tokener.nextValue();
    status = jsonObject.getString("status");
    assertTrue(status.equals("COMPLETED"));
    core.stop();

  }

  public void testDeployServiceOpenStack() throws IOException, InterruptedException {


    BlockingQueue<ServicePlatformMessage> muxQueue =
        new LinkedBlockingQueue<ServicePlatformMessage>();
    BlockingQueue<ServicePlatformMessage> dispatcherQueue =
        new LinkedBlockingQueue<ServicePlatformMessage>();

    TestProducer producer = new TestProducer(muxQueue, this);
    consumer = new TestConsumer(dispatcherQueue);
    AdaptorCore core = new AdaptorCore(muxQueue, dispatcherQueue, consumer, producer, 0.1);

    core.start();
    int counter = 0;

    try {
      while (counter < 2) {
        synchronized (mon) {
          mon.wait();
          if (lastHeartbeat.contains("RUNNING")) counter++;
        }
      }
    } catch (Exception e) {
      assertTrue(false);
    }


    String message =
        "{\"wr_type\":\"compute\",\"vim_type\":\"Heat\", \"tenant_ext_router\":\"20790da5-2dc1-4c7e-b9c3-a8d590517563\", \"tenant_ext_net\":\"decd89e2-1681-427e-ac24-6e9f1abb1715\",\"vim_address\":\"openstack.sonata-nfv.eu\",\"username\":\"op_sonata\",\"pass\":\"op_s0n@t@\",\"tenant\":\"op_sonata\"}";
    String topic = "infrastructure.management.compute.add";
    ServicePlatformMessage addVimMessage = new ServicePlatformMessage(message, "application/json",
        topic, UUID.randomUUID().toString(), topic);
    consumer.injectMessage(addVimMessage);
    Thread.sleep(2000);
    while (output == null)
      synchronized (mon) {
        mon.wait(1000);
      }

    JSONTokener tokener = new JSONTokener(output);
    JSONObject jsonObject = (JSONObject) tokener.nextValue();
    String status = jsonObject.getString("status");
    String wrUuid = jsonObject.getString("uuid");
    assertTrue(status.equals("COMPLETED"));
    System.out.println("OenStack Wrapper added, with uuid: " + wrUuid);

    output = null;
    String baseInstanceUuid = data.getNsd().getInstanceUuid();
    data.setVimUuid(wrUuid);
    data.getNsd().setInstanceUuid(baseInstanceUuid + "-01");

    String body = mapper.writeValueAsString(data);

    topic = "infrastructure.service.deploy";
    ServicePlatformMessage deployServiceMessage = new ServicePlatformMessage(body,
        "application/x-yaml", topic, UUID.randomUUID().toString(), topic);

    consumer.injectMessage(deployServiceMessage);

    Thread.sleep(2000);
    while (output == null)
      synchronized (mon) {
        mon.wait(1000);
      }
    assertNotNull(output);
    int retry = 0;
    int maxRetry = 60;
    while (output.contains("heartbeat") || output.contains("Vim Added") && retry < maxRetry)
      synchronized (mon) {
        mon.wait(1000);
        retry++;
      }

    System.out.println("DeployServiceResponse: ");
    System.out.println(output);
    assertTrue("No Deploy service response received", retry < maxRetry);
    DeployServiceResponse response = mapper.readValue(output, DeployServiceResponse.class);
    assertTrue(response.getRequestStatus().equals("DEPLOYED"));
    assertTrue(response.getNsr().getStatus() == Status.offline);

    for (VnfRecord vnfr : response.getVnfrs())
      assertTrue(vnfr.getStatus() == Status.offline);


    // Deploy a second instance of the same service

    data.getNsd().setInstanceUuid(baseInstanceUuid + "-02");
    output = null;

    body = mapper.writeValueAsString(data);

    topic = "infrastructure.service.deploy";
    deployServiceMessage = new ServicePlatformMessage(body, "application/x-yaml", topic,
        UUID.randomUUID().toString(), topic);

    consumer.injectMessage(deployServiceMessage);

    Thread.sleep(2000);
    while (output == null)
      synchronized (mon) {
        mon.wait(1000);
      }
    assertNotNull(output);
    retry = 0;
    while (output.contains("heartbeat") || output.contains("Vim Added") && retry < maxRetry)
      synchronized (mon) {
        mon.wait(1000);
        retry++;
      }

    System.out.println("DeployServiceResponse: ");
    System.out.println(output);
    assertTrue("No Deploy service response received", retry < maxRetry);
    response = mapper.readValue(output, DeployServiceResponse.class);
    assertTrue(response.getRequestStatus().equals("DEPLOYED"));
    assertTrue(response.getNsr().getStatus() == Status.offline);
    for (VnfRecord vnfr : response.getVnfrs())
      assertTrue(vnfr.getStatus() == Status.offline);


    // // Clean the OpenStack tenant from the stack
    // OpenStackHeatClient client =
    // new OpenStackHeatClient("143.233.127.3", "op_sonata", "op_s0n@t@", "op_sonata");
    // String stackName = response.getInstanceName();
    //
    // String deleteStatus = client.deleteStack(stackName, response.getInstanceVimUuid());
    // assertNotNull("Failed to delete stack", deleteStatus);
    //
    // if (deleteStatus != null) {
    // System.out.println("status of deleted stack " + stackName + " is " + deleteStatus);
    // assertEquals("DELETED", deleteStatus);
    // }


    // Service removal
    output = null;
    String instanceUuid = baseInstanceUuid + "-01";
    message = "{\"instance_uuid\":\"" + instanceUuid + "\",\"vim_uuid\":\"" + wrUuid + "\"}";
    topic = "infrastructure.service.remove";
    ServicePlatformMessage removeInstanceMessage = new ServicePlatformMessage(message,
        "application/json", topic, UUID.randomUUID().toString(), topic);
    consumer.injectMessage(removeInstanceMessage);

    while (output == null) {
      synchronized (mon) {
        mon.wait(2000);
        System.out.println(output);
      }
    }
    System.out.println(output);
    tokener = new JSONTokener(output);
    jsonObject = (JSONObject) tokener.nextValue();
    status = jsonObject.getString("request_status");
    assertTrue("Adapter returned an unexpected status: " + status, status.equals("SUCCESS"));

    output = null;
    instanceUuid = baseInstanceUuid + "-02";
    message = "{\"instance_uuid\":\"" + instanceUuid + "\",\"vim_uuid\":\"" + wrUuid + "\"}";
    topic = "infrastructure.service.remove";
    removeInstanceMessage = new ServicePlatformMessage(message, "application/json", topic,
        UUID.randomUUID().toString(), topic);
    consumer.injectMessage(removeInstanceMessage);

    while (output == null) {
      synchronized (mon) {
        mon.wait(2000);
        System.out.println(output);
      }
    }
    System.out.println(output);
    tokener = new JSONTokener(output);
    jsonObject = (JSONObject) tokener.nextValue();
    status = jsonObject.getString("request_status");
    assertTrue("Adapter returned an unexpected status: " + status, status.equals("SUCCESS"));



    output = null;
    message = "{\"wr_type\":\"compute\",\"uuid\":\"" + wrUuid + "\"}";
    topic = "infrastructure.management.compute.remove";
    ServicePlatformMessage removeVimMessage = new ServicePlatformMessage(message,
        "application/json", topic, UUID.randomUUID().toString(), topic);
    consumer.injectMessage(removeVimMessage);

    while (output == null) {
      synchronized (mon) {
        mon.wait(1000);
      }
    }
    System.out.println(output);
    tokener = new JSONTokener(output);
    jsonObject = (JSONObject) tokener.nextValue();
    status = jsonObject.getString("status");
    assertTrue(status.equals("COMPLETED"));
    core.stop();

  }



  public void receiveHeartbeat(ServicePlatformMessage message) {
    synchronized (mon) {
      this.lastHeartbeat = message.getBody();
      mon.notifyAll();
    }
  }

  public void receive(ServicePlatformMessage message) {
    synchronized (mon) {
      this.output = message.getBody();
      mon.notifyAll();
    }
  }

  public void forwardToConsumer(ServicePlatformMessage message) {
    consumer.injectMessage(message);
  }
}
