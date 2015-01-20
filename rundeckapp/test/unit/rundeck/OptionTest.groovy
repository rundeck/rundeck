

package rundeck;
import org.codehaus.groovy.grails.plugins.databinding.DataBindingGrailsPlugin;

import grails.test.mixin.support.GrailsUnitTestMixin;
import grails.test.mixin.web.ControllerUnitTestMixin;


/**
 */
@TestFor(Option)
@TestMixin(ControllerUnitTestMixin)
public class OptionTest {
    
    @Before
    public void setup(){
        // hack for 2.3.9:  https://jira.grails.org/browse/GRAILS-11136
        defineBeans(new DataBindingGrailsPlugin().doWithSpring)
    }
    void testSortIndexShouldDetermineOrder() {
        
        def options = [
                new Option(name: 'abc-4', defaultValue: '12', sortIndex: 4),
                new Option(name: 'bcd-2', defaultValue: '12', sortIndex: 2),
                new Option(name: 'cde-3', defaultValue: '12', sortIndex: 3),
                new Option(name: 'def-1', defaultValue: '12', sortIndex: 1),
        ] as TreeSet
        assertEquals(['def-1', 'bcd-2', 'cde-3', 'abc-4'], options*.name)
    }

    void testNullSortIndexShouldUseNameToDetermineOrder() {
        
        def options = [
                new Option(name: 'def-4', defaultValue: '12',),
                new Option(name: 'abc-1', defaultValue: '12', ),
                new Option(name: 'cde-3', defaultValue: '12', ),
                new Option(name: 'bcd-2', defaultValue: '12', ),
        ] as TreeSet
        assertEquals(['abc-1','bcd-2','cde-3','def-4'], options*.name)
    }

    void testMixedSortShouldUseSortIndexFirstThenName() {
        
        def options = [
                new Option(name: 'def-4', defaultValue: '12',),
                new Option(name: 'abc-2', defaultValue: '12', sortIndex: 1),
                new Option(name: 'cde-3', defaultValue: '12',),
                new Option(name: 'bcd-1', defaultValue: '12',sortIndex: 0),
        ] as TreeSet
        assertEquals(['bcd-1', 'abc-2', 'cde-3', 'def-4'], options*.name)
    }

    void testToMapPreservesSortIndex(){
        assertEquals 0,new Option(name: 'bcd-1', sortIndex: 0).toMap().sortIndex
        assertEquals 1,new Option(name: 'bcd-1', sortIndex: 1).toMap().sortIndex
        assertEquals null,new Option(name: 'bcd-1',).toMap().sortIndex
    }
    void testfromMapPreservesSortIndex(){
        assertEquals 0,Option.fromMap('test',[sortIndex: 0]).sortIndex
        assertEquals 1,Option.fromMap('test',[sortIndex: 1]).sortIndex
        assertEquals null, Option.fromMap('test', [sortIndex: null]).sortIndex
    }
    void testConstraints() {
        
        def option = new Option(name: 'ABCdef-4._12390', defaultValue: '12',enforced: true)
        def validate = option.validate()
        if(!validate){
            option.errors.allErrors.each {println it}
        }
        assertEquals(true, validate)
        assertEquals(false, option.errors.hasErrors())
        assertEquals(false, option.errors.hasFieldErrors('name'))
    }
    void testInvalidName() {
        
        assertInvalidName(new Option(name: 'abc def', defaultValue: '12',enforced: true))
        assertInvalidName(new Option(name: 'abc+def', defaultValue: '12',enforced: true))
        assertInvalidName(new Option(name: 'abc/def', defaultValue: '12',enforced: true))
        assertInvalidName(new Option(name: 'abc!@#$%^&*()def', defaultValue: '12',enforced: true))
    }
    void testDelimiter() {
        def opt1=new Option(name:'abc',multivalued:true,delimiter:',')
        assertEquals(',',opt1.delimiter)
        def opt2=new Option(name:'abc',multivalued:true,delimiter:" ")
        assertEquals(' ',opt2.delimiter)
    }

    private void assertInvalidName(Option option) {
        assertEquals(false, option.validate())
        assertEquals(true, option.errors.hasErrors())
        assertEquals(true, option.errors.hasFieldErrors('name'))
    }
}
