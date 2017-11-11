package sonata.kernel.vimadaptor.wrapper.kubernetes;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import sonata.kernel.vimadaptor.commons.CloudServiceDeployPayload;
import sonata.kernel.vimadaptor.commons.CloudServiceDeployResponse;
import sonata.kernel.vimadaptor.commons.csr.CsRecord;
import sonata.kernel.vimadaptor.commons.Status;
import sonata.kernel.vimadaptor.commons.csd.CsDescriptor;
import sonata.kernel.vimadaptor.commons.csd.VirtualDeploymentUnit;
import sonata.kernel.vimadaptor.commons.csr.VduRecord;
import sonata.kernel.vimadaptor.wrapper.WrapperBay;
import sonata.kernel.vimadaptor.wrapper.WrapperConfiguration;

import java.util.Hashtable;

public class ResponseHelper {

    private WrapperConfiguration wrapper;

    public ResponseHelper(WrapperConfiguration wrapper) {
        this.wrapper = wrapper;
    }

    /**
     * Build the response from the given data.
     *
     * @return CloudServiceDeployResponse
     */
    public CloudServiceDeployResponse buildDeployResponse(String sid, CloudServiceDeployPayload deployPayload) {
        CloudServiceDeployResponse response = new CloudServiceDeployResponse();
        response.setRequestStatus("COMPLETED");
        response.setInstanceVimUuid(WrapperBay.getInstance().getVimRepo().getServiceInstanceVimUuid(deployPayload.getServiceInstanceId(), wrapper.getUuid()));
        response.setInstanceName(WrapperBay.getInstance().getVimRepo().getServiceInstanceVimName(deployPayload.getServiceInstanceId(), wrapper.getUuid()));
        response.setVimUuid(wrapper.getUuid());
        response.setMessage("");

        CsDescriptor csd = deployPayload.getCsd();

        CsRecord csr = new CsRecord();
        csr.setDescriptorVersion("csr-schema-01");
        csr.setDescriptorReference(csd.getUuid());
        csr.setId(csd.getInstanceUuid());
        csr.setStatus(Status.normal_operation);

        for (VirtualDeploymentUnit vdu : csd.getVirtualDeploymentUnits()) {
            VduRecord vdur = new VduRecord();
            vdur.setId(vdu.getId());
            vdur.setNumberOfInstances(1);
            vdur.setVduReference(csd.getName() + ":" + vdu.getId());
            csr.addVdu(vdur);
        }

        response.setCsr(csr);

        return response;
    }

    /**
     * Transformer an object to YAML.
     *
     * @param data Object
     *
     * @return String
     * @throws JsonProcessingException
     */
    public String transformToYAML(Object data) throws JsonProcessingException {
        return getObjectMapper().writeValueAsString(data);
    }

    /**
     * Get the object mapper which transforms object to YAML.
     *
     * @return ObjectMapper
     */
    private static ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        return mapper;
    }
}
