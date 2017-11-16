package sonata.kernel.vimadaptor.wrapper.terraform;

import com.mitchellbosecke.pebble.error.PebbleException;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class TerraformClient {

    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(TerraformClient.class);

    private static final String TERRAFORM_LOCATION = "/root/terraform";

    private String baseDir;

    private String serviceId;

    public TerraformClient(String baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Run the given terraform command.
     *
     * @param command String
     * @return String
     */
    public String runCmd(String... command) throws IOException, InterruptedException, TerraformException {
        StringBuilder output = new StringBuilder();

        ProcessBuilder builder;

        ArrayList<String> params = new ArrayList<>(Arrays.asList(command));
        params.add(0, TERRAFORM_LOCATION);
        builder = new ProcessBuilder(params);

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
            String err = "[TerraformClient] Error while running terraform " + command[0] + ": " + output;

            throw new TerraformException(err);
        }

        return output.toString();
    }

    /**
     * Run "terraform init".
     *
     * @return this
     */
    public TerraformClient init() throws IOException, TerraformException, InterruptedException {
        Logger.info("[TerraformClient] Running terraform init for " + serviceId + "...");

        this.runCmd("init");

        Logger.info("[TerraformClient] terraform init completed for " + serviceId + ".");

        return this;
    }

    /**
     * Run "terraform apply".
     *
     * @return this
     */
    public TerraformClient apply() throws IOException, TerraformException, InterruptedException {
        Logger.info("[TerraformClient] Running terraform apply for " + serviceId + "...");

        this.runCmd("apply", "-auto-approve");

        Logger.info("[TerraformClient] terraform apply completed for " + serviceId + ".");

        return this;
    }

    /**
     * Run "terraform destroy"
     *
     * @return this
     */
    public TerraformClient destroy() throws IOException, TerraformException, InterruptedException {
        Logger.info("[TerraformClient] Running terraform destroy for " + serviceId + ".");

        this.runCmd("destroy", "-force");

        Logger.info("[TerraformClient] Removing service data for " + serviceId + "...");

        File serviceFolder = new File(this.getServicePath());
        FileUtils.deleteDirectory(serviceFolder);

        Logger.info("[TerraformClient] terraform destroy completed for " + serviceId + ".");

        return this;
    }

    /**
     * Write the given terraform template to disk.
     *
     * @param template TerraformTemplate
     * @param instanceId String
     * @return this
     */
    public TerraformClient writeTemplate(TerraformTemplate template, String instanceId) throws IOException, PebbleException {
        this.initialiseService(template);

        Logger.info("[TerraformClient] Writing terraform service config to " + this.getTerraformServiceConfigurationPath(instanceId));

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
     * @return TerraformClient
     */
    public TerraformClient forService(String serviceId) {
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
        Logger.info("[TerraformClient] Writing terraform main config to " + this.getTerraformMainConfigurationPath());

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
