package sonata.kernel.vimadaptor.wrapper.kubernetes;

import sonata.kernel.vimadaptor.commons.csd.CsDescriptor;
import sonata.kernel.vimadaptor.wrapper.WrapperConfiguration;
import sonata.kernel.vimadaptor.wrapper.terraform.TerraformTemplate;

import java.util.HashMap;
import java.util.Map;

public class KubernetesTerraformTemplate extends TerraformTemplate {

    private CsDescriptor csd;

    private WrapperConfiguration wrapper;

    @Override
    public String getBaseTemplate() {
        return "templates/kubernetes.tf";
    }

    @Override
    public Map<String, Object> getContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("csd", this.csd);
        context.put("endpoint", String.format("https://%s", wrapper.getVimEndpoint()));

        return context;
    }

    public KubernetesTerraformTemplate withCsd(CsDescriptor csd) {
        this.csd = csd;

        return this;
    }

    public KubernetesTerraformTemplate withWrapperConfiguration(WrapperConfiguration wrapper) {
        this.wrapper = wrapper;

        return this;
    }
}
