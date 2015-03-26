package com.dtolabs.rundeck.core.execution.utils
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import spock.lang.Specification
/**
 * Created by greg on 3/20/15.
 */
class ResolverUtilSpec extends Specification {

    def setup(){

    }
    def teardown(){
        def framework = AbstractBaseTest.createTestFramework()
        framework.getFrameworkProjectMgr().removeFrameworkProject('ResolverUtilSpec')
    }

    private static void mergeProps(File path, Properties props) {
        def oldprops = new Properties()
        path.withInputStream { oldprops.load(it) }
        oldprops.putAll(props)
        path.withOutputStream { oldprops.store(it, "") }
    }

    private static void removeProps(File path, Collection<String> props) {
        def oldprops = new Properties()
        path.withInputStream { oldprops.load(it) }
        props.each { oldprops.remove(it) }
        path.withOutputStream { oldprops.store(it, "") }
    }

    private INodeEntry setupConfigProperties(
            String propname,
            String attrval,
            String projectVal,
            String projectPropName,
            String fwkVal,
            String frameworkPropName
    )
    {
        INodeEntry node = new NodeEntryImpl("test1");
        node.getAttributes().put(propname, attrval)

        def removePrefixes = [] as Set
        def projprops = new Properties()


        if (null != projectVal) {
            projprops.put(projectPropName, projectVal)
        } else {
            removePrefixes << projectPropName
        }
        def framework = AbstractBaseTest.createTestFramework()
        framework.getFrameworkProjectMgr().removeFrameworkProject('ResolverUtilSpec')
        framework.getFrameworkProjectMgr().createFrameworkProject('ResolverUtilSpec')
        def testProject = framework.getFrameworkProjectMgr().getFrameworkProject('ResolverUtilSpec')
        testProject.mergeProjectProperties(projprops, removePrefixes)
        testProject.hasProperty(projectPropName)//trigger refresh


        if (null != fwkVal) {
            def fwkProps = new Properties()
            fwkProps.setProperty(frameworkPropName, fwkVal)
            mergeProps(new File(framework.getBaseDir(), "etc/framework.properties"), fwkProps)
        } else {
            removeProps(new File(framework.getBaseDir(), "etc/framework.properties"), [frameworkPropName])
        }

        node
    }

    def "resolve property"(
            String propname,
            String attrval,
            String projectVal,
            String fwkVal,
            String expectedVal,
            String defaultVal
    )
    {
        setup:
        def projectPropName = "project.${propname}".toString()
        def frameworkPropName = "framework.${propname}".toString()
        def node = setupConfigProperties(
                propname,
                attrval,
                projectVal,
                projectPropName,
                fwkVal,
                frameworkPropName
        )

        def framework = AbstractBaseTest.createTestFramework()
        def testProject = framework.getFrameworkProjectMgr().getFrameworkProject('ResolverUtilSpec')

        expect:
        testProject.hasProperty(projectPropName) == (null != projectVal)
        testProject.getProperties().get(projectPropName) == projectVal
        framework.hasProperty(frameworkPropName) == (null != fwkVal)
        if (null != fwkVal) {
            framework.getPropertyLookup().getProperty(frameworkPropName) == fwkVal
        }
        ResolverUtil.resolveProperty(propname, defaultVal, node, testProject, framework) == expectedVal

        where:
        propname        | attrval            | projectVal         | fwkVal             | expectedVal        | defaultVal
        'some-property' | null               | null               | null               | null               | null
        'some-property' | null               | null               | null               | 'abc'              | 'abc'
        'some-property' | 'option.xpassword' | null               | null               | 'option.xpassword' | null
        'some-property' | 'option.xpassword' | 'option.ypassword' | null               | 'option.xpassword' | null
        'some-property' | 'option.xpassword' | 'option.ypassword' | 'option.zpassword' | 'option.xpassword' | null
        'some-property' | null               | 'option.ypassword' | null               | 'option.ypassword' | null
        'some-property' | null               | 'option.ypassword' | 'option.zpassword' | 'option.ypassword' | null
        'some-property' | null               | null               | 'option.zpassword' | 'option.zpassword' | null
    }
}
