package rundeck.codecs

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.constructor.SafeConstructor
import com.dtolabs.rundeck.core.utils.snakeyaml.ForceMultilineLiteralOptions
import rundeck.ScheduledExecution
import rundeck.controllers.JobXMLException

class JobsYAMLCodec {

    static Map canonicalMap(Map input) {
        def result = [:]//linked hash map has ordered keys
        input.keySet().sort().each{
            def val = input[it]
            if(val instanceof Map){
                val = canonicalMap(val)
            }
            result[it]=val
        }
        result
    }
    static encode = {list ->
        def writer = new StringWriter()
        final DumperOptions dumperOptions = new ForceMultilineLiteralOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(dumperOptions)

        yaml.dump(list.collect {canonicalMap it.toMap()}, writer)

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

    public static createJobs (data){
        ArrayList list = new ArrayList()
        if (data instanceof Collection) {
            //iterate through list of jobs
            data.each{ jobobj ->
                if (jobobj instanceof Map) {
                    try {
                        list << ScheduledExecution.fromMap(jobobj)
                    } catch (Exception e) {
                        throw new JobXMLException("Unable to create Job: " + e.getMessage(),e)
                    }
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