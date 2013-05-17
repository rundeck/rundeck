import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants;
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException;

rundeckPlugin(NotificationPlugin){
    title="Script Test"
    description="A Test"
    configuration{
        test1 title:"Test1", description:"Simple string"
        testarea1 title:"Textarea", description:"Multi-line string", multiline:true
        testarea2 title:"Textarea2", description:"Multi-line string", renderingOptions:[displayType:StringRenderingConstants.DisplayType.MULTI_LINE]

        test2(title:'Test2',description:'Matches regex ^\\d+$'){
            if(!(it==~/^\d+$/)){
                throw new ValidationException('Must match ^\\d+$')
            }else{
                return true
            }
        }

        test3 values: ["a","b","c"], required:true
        test4 defaultValue: 3
        test5 values: ["a","b","c"]
        test6 defaultValue:true

        test7=123
        test8="abc"
        test9=["x","y","z"]
        test9 title:"My Select Field", description:"Should be select", defaultValue:"y", required:true
        test10=true
        test11=false
        test12='''A multiline string default value
will use multiline renderingOptions
'''
    }
    onstart { Map executionData,Map config ->
        System.err.println("script, success: data ${executionData}, config: ${config}")
        true
    }
    onfailure { Map executionData ->
        //config values are available as variables
        System.err.println("script, failure: data ${executionData}, test1: ${test1}, test2: ${test2} test3: ${test3}")
        true
    }
    onsuccess {
        //with no args, there is a "config" and an "execution" variable in the context
        System.err.println("script, start: data ${execution}, test1: ${config.test1}, test2: ${config.test2} test3: ${config.test3}")
        true
    }

}

