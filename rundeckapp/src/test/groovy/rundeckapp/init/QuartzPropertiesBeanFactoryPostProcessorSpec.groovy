package rundeckapp.init

import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.support.GenericBeanDefinition
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.scheduling.quartz.SchedulerFactoryBean
import spock.lang.Specification

/**
 * quartz.* settings arrive in rundeck-config.properties, which is not merged into the config the
 * quartz plugin reads in doWithSpring, so the scheduler came up on Quartz's defaults -- a pool of
 * 10 no matter what threadCount was set to, with nothing logged.
 */
class QuartzPropertiesBeanFactoryPostProcessorSpec extends Specification {

    private DefaultListableBeanFactory beanFactoryWith(Properties existing = null) {
        def factory = new DefaultListableBeanFactory()
        def definition = new GenericBeanDefinition(beanClass: SchedulerFactoryBean)
        if (existing != null) {
            definition.propertyValues.add('quartzProperties', existing)
        }
        factory.registerBeanDefinition('quartzScheduler', definition)
        factory
    }

    private QuartzPropertiesBeanFactoryPostProcessor processorFor(Map<String, Object> properties) {
        def environment = new StandardEnvironment()
        environment.propertySources.addFirst(new MapPropertySource('rundeck-config.properties', properties))
        new QuartzPropertiesBeanFactoryPostProcessor(environment: environment)
    }

    private static Properties appliedTo(DefaultListableBeanFactory factory) {
        factory.getBeanDefinition('quartzScheduler').propertyValues.getPropertyValue('quartzProperties')?.value as Properties
    }

    def "thread count from configuration reaches the scheduler"() {
        given:
        def factory = beanFactoryWith()

        when:
        processorFor(['quartz.threadPool.threadCount': '60']).postProcessBeanFactory(factory)

        then:
        appliedTo(factory).getProperty('org.quartz.threadPool.threadCount') == '60'
    }

    def "configured values win over what the plugin set, and the rest is kept"() {
        given:
        def factory = beanFactoryWith(new Properties([
            'org.quartz.threadPool.threadCount': '10',
            'org.quartz.scheduler.instanceName': 'quartzScheduler',
        ]))

        when:
        processorFor(['quartz.threadPool.threadCount': '60']).postProcessBeanFactory(factory)

        then:
        def applied = appliedTo(factory)
        applied.getProperty('org.quartz.threadPool.threadCount') == '60'
        applied.getProperty('org.quartz.scheduler.instanceName') == 'quartzScheduler'
    }

    def "the plugin's own switches are not forwarded as Quartz properties"() {
        given:
        def factory = beanFactoryWith()

        when:
        processorFor([
            'quartz.threadPool.threadCount': '60',
            'quartz.autoStartup'           : 'true',
            'quartz.jdbcStore'             : 'false',
        ]).postProcessBeanFactory(factory)

        then:
        def applied = appliedTo(factory)
        applied.getProperty('org.quartz.threadPool.threadCount') == '60'
        !applied.containsKey('org.quartz.autoStartup')
        !applied.containsKey('org.quartz.jdbcStore')
    }

    def "with nothing configured the definition is left untouched"() {
        given:
        def factory = beanFactoryWith(new Properties(['org.quartz.threadPool.threadCount': '10']))

        when:
        processorFor(['rundeck.something.else': 'x']).postProcessBeanFactory(factory)

        then:
        appliedTo(factory).getProperty('org.quartz.threadPool.threadCount') == '10'
    }

    def "no scheduler bean is not an error"() {
        given:
        def factory = new DefaultListableBeanFactory()

        when:
        processorFor(['quartz.threadPool.threadCount': '60']).postProcessBeanFactory(factory)

        then:
        noExceptionThrown()
    }
}
