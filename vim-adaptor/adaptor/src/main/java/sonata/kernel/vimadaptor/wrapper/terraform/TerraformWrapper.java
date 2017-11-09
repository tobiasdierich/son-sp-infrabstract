package sonata.kernel.vimadaptor.wrapper.terraform;

import com.mitchellbosecke.pebble.error.PebbleException;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TerraformWrapper {

    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(TerraformWrapper.class);

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

        Logger.info("[TerraformWrapper] Writing terraform config to " + this.getTerraformConfigurationPath());

        BufferedWriter writer = new BufferedWriter(new FileWriter(this.getTerraformConfigurationPath()));
        writer.write(template.getContent());
        writer.close();

        Logger.info("[TerraformWrapper] Wrote terraform config. Content:");
        Logger.info(template.getContent());

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
        File servicePath = new File(this.getServicePath());

        if (!servicePath.isDirectory()) {
            servicePath.mkdirs();
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
