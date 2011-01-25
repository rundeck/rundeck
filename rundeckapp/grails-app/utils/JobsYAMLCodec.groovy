import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.constructor.SafeConstructor

class JobsYAMLCodec {

    static encode = {list ->
        def writer = new StringWriter()
        final DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(dumperOptions)

        yaml.dump(list.collect {it.toMap()}, writer)

        return writer.toString()
    }

    static decodeFromStream = {InputStream stream ->

        Yaml yaml = new Yaml(new SafeConstructor())

        def data = yaml.load(stream)
        return createJobs(data);
    }

    static decode = {input ->
        Yaml yaml = new Yaml(new SafeConstructor())
        def data
        if(input instanceof File){
            input = input.getInputStream()
        }
        data= yaml.load(input)
        return createJobs(data);
    }

    static createJobs (data){
        ArrayList list = new ArrayList()
        if (data instanceof Collection) {
            //iterate through list of jobs
            data.each {jobobj ->
                if (jobobj instanceof Map) {
                    list << ScheduledExecution.fromMap(jobobj)
                } else {
                    throw new JobXMLException("Unexpected data type: " + jobobj.class.name)
                }
            }
        } else {
            throw new JobXMLException("Unexpected data type: " + data.class.name)
        }
        return list
    }
}