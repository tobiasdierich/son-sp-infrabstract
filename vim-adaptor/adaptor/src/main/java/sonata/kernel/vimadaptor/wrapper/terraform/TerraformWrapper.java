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
    public String runCmd(String command, boolean autoApprove) throws IOException, InterruptedException, TerraformException {
        StringBuilder output = new StringBuilder();

        ProcessBuilder builder;

        if (autoApprove) {
            builder = new ProcessBuilder(TERRAFORM_LOCATION, command, "-auto-approve=true");
        } else {
            builder = new ProcessBuilder(TERRAFORM_LOCATION, command);
        }

        builder = builder.directory(new File(this.getServicePath()))
                .redirectErrorStream(true);

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

        int exitCode = process.waitFor();

        if (exitCode > 0) {
            String err = "[TerraformWrapper] Error while running terraform " + command + ": " + output;

            throw new TerraformException(err);
        }

        return output.toString();
    }

    /**
     * Run "terraform init".
     *
     * @return this
     */
    public TerraformWrapper init() throws IOException, TerraformException, InterruptedException {
        Logger.info("[TerraformWrapper] Running terraform init...");

        this.runCmd("init", false);

        Logger.info("[TerraformWrapper] terraform init completed.");

        return this;
    }

    /**
     * Run "terraform apply".
     *
     * @return this
     */
    public TerraformWrapper apply() throws IOException, TerraformException, InterruptedException {
        Logger.info("[TerraformWrapper] Running terraform apply...");

        this.runCmd("apply", true);

        Logger.info("[TerraformWrapper] terraform apply completed.");

        return this;
    }

    /**
     * Write the given terraform template to disk.
     *
     * @param template TerraformTemplate
     * @param instanceId String
     * @return this
     */
    public TerraformWrapper writeTemplate(TerraformTemplate template, String instanceId) throws IOException, PebbleException {
        this.initialiseService(template);

        Logger.info("[TerraformWrapper] Writing terraform config to " + this.getTerraformServiceConfigurationPath(instanceId));

        BufferedWriter writer = new BufferedWriter(new FileWriter(this.getTerraformServiceConfigurationPath(instanceId)));
        writer.write(template.getServiceContent());
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
     * Initialise the terraform project if it has not been initialised yet.
     */
    private void initialiseService(TerraformTemplate template) throws IOException, PebbleException {
        this.createFoldersIfNotExist();

        File mainConfig = new File(this.getTerraformMainConfigurationPath());
        if (!mainConfig.exists() && template.getMainContent() != null) {
            this.writeMainTemplate(template);
        }
    }

    /**
     * Write the content of the main terraform template.
     *
     * @param template TerraformTemplate
     */
    private void writeMainTemplate(TerraformTemplate template) throws IOException, PebbleException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(this.getTerraformMainConfigurationPath()));
        writer.write(template.getMainContent());
        writer.close();
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
     * Get the path to the main configuration file.
     *
     * @return String
     */
    private String getTerraformMainConfigurationPath() {
        return this.getServicePath() + "main.tf";
    }

    /**
     * Get the full path to the terraform service configuration file.
     *
     * @param instanceId String
     *
     * @return String
     */
    private String getTerraformServiceConfigurationPath(String instanceId) {
        return this.getServicePath() + instanceId + ".tf";
    }
}
