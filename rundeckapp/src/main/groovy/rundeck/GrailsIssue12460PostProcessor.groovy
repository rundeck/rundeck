package rundeck

import groovy.transform.CompileStatic
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor
import org.springframework.beans.factory.support.RootBeanDefinition

@CompileStatic
class GrailsIssue12460PostProcessor implements MergedBeanDefinitionPostProcessor {
    @Override
    void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        if (beanName == 'groovyPagesTemplateEngine') {

            // Grails issue : https://github.com/grails/grails-core/issues/12460'

            beanDefinition.getPropertyValues().removePropertyValue("classLoader")
        }
    }
}