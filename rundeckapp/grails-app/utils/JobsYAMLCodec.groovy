import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.DumperOptions

class JobsYAMLCodec {

    static encode = {list ->
        def writer = new StringWriter()
        final DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(dumperOptions)

        yaml.dump(list.collect{it.toMap()},writer)

        return writer.toString()
    }
}