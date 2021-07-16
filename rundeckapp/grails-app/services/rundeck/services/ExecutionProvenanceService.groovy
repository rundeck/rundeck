package rundeck.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.NamedType
import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.NotTransactional
import grails.gorm.transactions.Transactional
import org.rundeck.core.executions.provenance.GenericProvenance
import org.rundeck.core.executions.provenance.Provenance
import org.rundeck.core.executions.provenance.ProvenanceComponent
import org.rundeck.core.executions.provenance.ProvenanceList
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import rundeck.Execution

@Transactional
@GrailsCompileStatic
class ExecutionProvenanceService implements ApplicationContextAware {
    ApplicationContext applicationContext

    @NotTransactional
    Map<String, ProvenanceComponent> getComponents() {
        applicationContext.getBeansOfType(ProvenanceComponent)
    }


    private ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper()
        //load  subtypes
        mapper.registerSubtypes(Provenance)
        getComponents().values().each {
            it.getProvenanceTypes().each {
                mapper.registerSubtypes(new NamedType(it.value, it.key));
            }
        }
        mapper
    }

    /**
     * Stores list of provenance data objects for the execution
     * @param e
     * @param provenance list of provenance data objects
     */
    void setProvenanceForExecution(Execution e, List<Provenance<?>> provenance) {
        ObjectMapper mapper = createMapper()
        String json = mapper.writeValueAsString(new ProvenanceList(provenance))
        setProvenanceJson(e, json)
    }


    List<Provenance<?>> getProvenanceForExecution(Execution e) {
        //find provenance component for executionType
        String jsonData = getProvenanceJson(e)
        ObjectMapper mapper = createMapper()
        ProvenanceList list = mapper.readValue(jsonData, ProvenanceList)
        list.getProvenances()
    }


    String getProvenanceJson(Execution execution) {
        execution.provenanceData// TODO use PluginMeta
    }

    void setProvenanceJson(Execution execution, String json) {
        execution.provenanceData = json// TODO use PluginMeta
    }
}
