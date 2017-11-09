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
