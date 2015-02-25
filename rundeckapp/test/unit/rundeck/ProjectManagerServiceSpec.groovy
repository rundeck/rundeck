package rundeck

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(ProjectManagerService)
class ProjectManagerServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "merge properties no conflict"() {
        given:
        Properties oldprops=new Properties()
        oldprops.setProperty("abc","123")
        oldprops.setProperty("def","456")
        Properties newprops=new Properties()
        newprops.setProperty("ghi","789")
        Set<String> removePrefixes=[]

        when:
        Properties result=ProjectManagerService.mergeProperties(removePrefixes,oldprops,newprops)

        then:
        3==result.size()
        "123"==result.getProperty("abc")
        "456"==result.getProperty("def")
        "789"==result.getProperty("ghi")

    }

    void "merge properties override"() {
        given:
        Properties oldprops=new Properties()
        oldprops.setProperty("abc","123")
        oldprops.setProperty("def","456")
        Properties newprops=new Properties()
        newprops.setProperty("abc","789")
        Set<String> removePrefixes=[]

        when:
        Properties result=ProjectManagerService.mergeProperties(removePrefixes,oldprops,newprops)

        then:
        2==result.size()
        "789"==result.getProperty("abc")
        "456"==result.getProperty("def")

    }

    void "merge properties remove prefix"() {
        given:
        Properties oldprops=new Properties()
        oldprops.setProperty("abc","123")
        oldprops.setProperty("def","456")
        Properties newprops=new Properties()
        newprops.setProperty("ghi","789")
        Set<String> removePrefixes=['de']

        when:
        Properties result=ProjectManagerService.mergeProperties(removePrefixes,oldprops,newprops)

        then:
        2==result.size()
        "123"==result.getProperty("abc")
        null==result.getProperty("def")
        "789"==result.getProperty("ghi")

    }

    void "merge properties remove prefix multiple hits"() {
        given:
        Properties oldprops=new Properties()
        oldprops.setProperty("abc","123")
        oldprops.setProperty("def","456")
        oldprops.setProperty("defleopard","money")
        Properties newprops=new Properties()
        newprops.setProperty("ghi","789")
        Set<String> removePrefixes=['de']

        when:
        Properties result=ProjectManagerService.mergeProperties(removePrefixes,oldprops,newprops)

        then:
        2==result.size()
        "123"==result.getProperty("abc")
        null==result.getProperty("def")
        null==result.getProperty("defleopard")
        "789"==result.getProperty("ghi")
    }

    void "merge properties remove multiple prefixes multiple hits"() {
        given:
        Properties oldprops=new Properties()
        oldprops.setProperty("abc","123")
        oldprops.setProperty("abcdef","488")
        oldprops.setProperty("def","456")
        oldprops.setProperty("defleopard","money")
        Properties newprops=new Properties()
        newprops.setProperty("ghi","789")
        Set<String> removePrefixes=['de','ab']

        when:
        Properties result=ProjectManagerService.mergeProperties(removePrefixes,oldprops,newprops)

        then:
        1==result.size()
        null==result.getProperty("abc")
        null==result.getProperty("abcdef")
        null==result.getProperty("def")
        null==result.getProperty("defleopard")
        "789"==result.getProperty("ghi")
    }
}
