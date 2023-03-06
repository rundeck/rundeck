package org.rundeck.app.components

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import groovy.transform.CompileStatic
import org.rundeck.app.components.jobs.JobDefinitionException
import org.rundeck.app.components.jobs.JobFormat

@CompileStatic
class JobJSONFormat implements JobFormat {
    final String format = 'json'

    Boolean trimSpacesFromLines = false

    @Override
    List<Map> decode(final Reader reader) throws JobDefinitionException {

        final ObjectMapper mapper = new ObjectMapper()
        try {
            def data = mapper.readValue(reader, List.class)
            if (!(data instanceof List)) {
                throw new JobDefinitionException("Job Json Format: Expected list data")
            }
            if (!data.every { it instanceof Map }) {
                throw new JobDefinitionException("Job Json Format: Expected list of Maps")
            }

            data.each (JobYAMLFormat.&convertJobNotifications)
            return data
        } catch (JsonMappingException exception) {
            if (exception instanceof MismatchedInputException) {
                if (exception.targetType == ArrayList) {
                    throw new JobDefinitionException("Job Json Format: Expected list data")
                }
            }
            throw new JobDefinitionException("Job Json Format: " + exception.message, exception)
        }
    }

    @Override
    void encode(final List<Map> list, final Options options, final Writer writer) {

        final ObjectMapper mapper = new ObjectMapper()
        mapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
        mapper.writeValue(writer, list.collect { JobYAMLFormat.canonicalMap(JobYAMLFormat.performMapping(options,it), trimSpacesFromLines) })
    }
}
