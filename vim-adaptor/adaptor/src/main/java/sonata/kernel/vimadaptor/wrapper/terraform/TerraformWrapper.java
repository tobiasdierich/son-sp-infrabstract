package sonata.kernel.vimadaptor.wrapper.terraform;

import com.mitchellbosecke.pebble.error.PebbleException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TerraformWrapper {

    private String baseDir;

    private String serviceId;

    public TerraformWrapper(String baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Write the given terraform template to disk.
     *
     * @param template TerraformTemplate
     * @return this
     */
    public TerraformWrapper writeTemplate(TerraformTemplate template) throws IOException, PebbleException {
        this.createFoldersIfNotExist();
        BufferedWriter writer = new BufferedWriter(new FileWriter(this.getTerraformConfigurationPath()));
        writer.write(template.getContent());

        return this;
    }

    /**
     * Set the service id.
     *
     * @param serviceId String
     *
     * @return TerraformWrapper
     */
    public TerraformWrapper forService(String serviceId) {
        this.serviceId = serviceId;

        return this;
    }

    /**
     * Create any directories that do not exist.
     */
    private void createFoldersIfNotExist() {
        if (!new File(getServicePath()).isDirectory()) {
            new File(this.getServicePath()).mkdirs();
        }
    }

    /**
     * Get the service path.
     *
     * @return String
     */
    private String getServicePath() {
        return this.baseDir + this.serviceId + File.separator;
    }

    /**
     * Get the full path to the terraform configuration file.
     *
     * @return String
     */
    private String getTerraformConfigurationPath() {
        return this.getServicePath() + "service.tf";
    }
}
