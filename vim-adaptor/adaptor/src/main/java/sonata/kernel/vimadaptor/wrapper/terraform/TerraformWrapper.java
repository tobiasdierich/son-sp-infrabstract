package sonata.kernel.vimadaptor.wrapper.terraform;

import com.mitchellbosecke.pebble.error.PebbleException;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;

public class TerraformWrapper {

    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(TerraformWrapper.class);

    private static final String TERRAFORM_LOCATION = "/root/terraform";

    private String baseDir;

    private String serviceId;

    public TerraformWrapper(String baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Run the given terraform command.
     *
     * @param command String
     * @return String
     */
    public String runCmd(String command) throws IOException {
        StringBuilder output = new StringBuilder();

        ProcessBuilder builder = new ProcessBuilder(TERRAFORM_LOCATION, command)
                .directory(new File(this.getServicePath()));

        Map<String, String> env = builder.environment();
        env.put("HOME", "/root");

        Process process = builder.start();
        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;

        while ((line = br.readLine()) != null) {
            output.append(line).append('\n');
        }

        return output.toString();
    }

    /**
     * Run "terraform init".
     *
     * @return this
     */
    public TerraformWrapper init() {
        Logger.info("[TerraformWrapper] Running terraform init...");

        try {
            this.runCmd("init");

            Logger.info("[TerraformWrapper] terraform init completed.");
        } catch (IOException e) {
            Logger.error("[TerraformWrapper] Error while running terraform init: " + e.getMessage());
        }

        return this;
    }

    /**
     * Run "terraform apply".
     *
     * @return this
     */
    public TerraformWrapper apply() {
        Logger.info("[TerraformWrapper] Running terraform apply...");

        try {
            String output = this.runCmd("apply");

            Logger.info("[TerraformWrapper] terraform apply completed. Output: \n" + output);
        } catch (IOException e) {
            Logger.error("[TerraformWrapper] Error while running terraform apply: " + e.getMessage());
        }

        return this;
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
