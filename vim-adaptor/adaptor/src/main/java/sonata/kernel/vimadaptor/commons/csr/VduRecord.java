package sonata.kernel.vimadaptor.commons.csr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import sonata.kernel.vimadaptor.commons.VnfcInstance;
import sonata.kernel.vimadaptor.commons.csd.ResourceRequirements;

import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VduRecord {
    private String id;

    @JsonProperty("number_of_instances")
    private int numberOfInstances;

    @JsonProperty("resource_requirements")
    private ResourceRequirements resourceRequirements;

    @JsonProperty("vdu_reference")
    private String vduReference;

    @JsonProperty("vnfc_instance")
    private ArrayList<VnfcInstance> vnfcInstance;

    public VduRecord() {
        vnfcInstance = new ArrayList<VnfcInstance>();
    }

    public void addVnfcInstance(VnfcInstance instance) {
        vnfcInstance.add(instance);
    }

    public String getId() {
        return id;
    }


    public int getNumberOfInstances() {
        return numberOfInstances;
    }

    public ResourceRequirements getResourceRequirements() {
        return resourceRequirements;
    }

    public String getVduReference() {
        return vduReference;
    }

    public ArrayList<VnfcInstance> getVnfcInstance() {
        return vnfcInstance;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setNumberOfInstances(int numberOfInstances) {
        this.numberOfInstances = numberOfInstances;
    }

    public void setResourceRequirements(ResourceRequirements resourceRequirements) {
        this.resourceRequirements = resourceRequirements;
    }

    public void setVduReference(String vduReference) {
        this.vduReference = vduReference;
    }

    public void setVnfcInstance(ArrayList<VnfcInstance> vnfcInstance) {
        this.vnfcInstance = vnfcInstance;
    }
}
