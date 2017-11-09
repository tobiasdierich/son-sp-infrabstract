package sonata.kernel.vimadaptor.wrapper.terraform;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

abstract public class TerraformTemplate {

    private String content;

    public String getContent() throws IOException, PebbleException {
        if (this.content == null) {
            return this.content = this.buildContent();
        }

        return this.content;
    }

    abstract public String getBaseTemplate();

    abstract public Map<String, Object> getContext();

    /**
     * Build the template.
     */
    private String buildContent() throws IOException, PebbleException {
        PebbleEngine engine = new PebbleEngine.Builder().build();
        PebbleTemplate compiledTemplate = engine.getTemplate(this.getBaseTemplate());

        Writer writer = new StringWriter();
        Map<String, Object> context = this.getContext();

        compiledTemplate.evaluate(writer, context);

        return writer.toString();
    }
}
