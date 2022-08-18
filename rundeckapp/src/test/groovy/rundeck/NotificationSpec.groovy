package rundeck

import spock.lang.Specification
import spock.lang.Unroll

class NotificationSpec extends Specification {


    @Unroll
    def "fromNormalizeMap should set the attachLog values as true"() {

        given:"the data collected from a job"
        def type = 'email'
        def data = [type : type , config : config]

        when:"creating or updating a notification from a normalized map "
        def result = Notification.fromNormalizedMap(data)

        then:"attachLog should have a true value"
        result.type == type
        result.configuration == expect

        where:"the attachLog is set as 'true'"
        config = [recipients: 'recip1',subject: 'subj1',attachLog:"true",attachType:'inline']
        expect = [recipients: 'recip1',subject: 'subj1',attachLog:true,attachLogInline: true]
    }

    @Unroll
    def "toNormalizedMap email"() {
        given:
            Notification n = new Notification(data)
        when:
            def result = n.toNormalizedMap()
        then:
            result == expected
        where:
            data | expected
            [eventTrigger: 'onsuccess', type: 'email', configuration: [
                recipients     : 'asdf',
                subject        : 'subj1',
                attachLog      : true,
                attachLogInline: true
            ]]   | [type: 'email', trigger: 'onsuccess', config: [recipients: 'asdf', subject: 'subj1', attachLog:
                true, attachType                                            : 'inline']]
            [eventTrigger: 'onfailure', type: 'email', configuration: [
                recipients     : 'asdf',
                subject        : 'subj1',
                attachLog      : true,
                attachLogInFile: true
            ]]   | [type: 'email', trigger: 'onfailure', config: [recipients: 'asdf', subject: 'subj1', attachLog:
                true, attachType                                            : 'file']]
            [eventTrigger: 'onretryablefailure', type: 'email', configuration: [
                recipients     : 'asdf',
                subject        : 'subj1',
                attachLog      : false,
                attachLogInFile: true
            ]]   | [type: 'email', trigger: 'onretryablefailure', config: [recipients: 'asdf', subject: 'subj1',
                                                                           attachLog : false]]
    }

    @Unroll
    def "fromNormalizedMap email"() {
        given:
            def type = 'email'
            def data = [
                type   : type,
                trigger: trigger,
                config : config
            ]
        when:
            def result = Notification.fromNormalizedMap(data)
        then:
            result.eventTrigger == trigger
            result.type == type
            result.configuration == expect
        where:
            trigger     | config | expect
            'onsuccess' | [recipients: 'recip1',subject: 'subj1',attachLog:true,attachType:'inline'] | [recipients: 'recip1',subject: 'subj1',attachLog: true,attachLogInline: true]
            'onsuccess' | [recipients: 'recip1',subject: 'subj1',attachLog:true,attachType:'file'] | [recipients: 'recip1',subject: 'subj1',attachLog: true,attachLogInFile: true]
            'onsuccess' | [recipients: 'recip1',subject: 'subj1',attachLog:false] | [recipients: 'recip1',subject: 'subj1',attachLog: false]
    }

    @Unroll
    def "toNormalizedMap webhook"() {
        given:
            Notification n = new Notification(data)
        when:
            def result = n.toNormalizedMap()
        then:
            result == expected
        where:
            data                                                                      | expected
            [eventTrigger: 'onsuccess', type: 'url', content: 'asdf', format: 'xml']  | [type:'url', trigger:'onsuccess', config:[urls:'asdf', format:'xml', httpMethod:null]]
            [eventTrigger: 'onsuccess', type: 'url', content: 'asdf', format: 'json'] | [type:'url', trigger:'onsuccess', config:[urls:'asdf', format:'json', httpMethod:null]]
    }

    @Unroll
    def "urlConfiguration webhook"() {
        given:
            Notification n = new Notification(data)
        when:
            def result = n.urlConfiguration()
        then:
            result == expected
        where:
        data                                                                                                      | expected
        [content: 'asdf', format: 'xml']                                  | [urls: 'asdf']
        [content: '{"urls": "asdf", "httpMethod":"GET"}'] | [urls: 'asdf', httpMethod: "GET"]
    }

    @Unroll
    def "fromNormalizedMap webhook"() {
        given:
            def type = 'url'
            def data = [
                type   : type,
                trigger: trigger,
                config : config
            ]
        when:
            def result = Notification.fromNormalizedMap(data)
        then:
            result.eventTrigger == trigger
            result.type == type
            result.urlConfiguration().urls == expect
            result.urlConfiguration().httpMethod == expectMethod
            result.format == expectformat
        where:
            trigger     | config                                             | expect | expectformat | expectMethod
            'onsuccess' | [urls: 'blah', format: 'xml', httpMethod: 'post']  | 'blah' | 'xml'        | 'post'
            'onsuccess' | [urls: 'blah', format: 'json', httpMethod: 'get']  | 'blah' | 'json'       | 'get'

    }
    @Unroll
    def "toNormalizedMap plugin"() {
        given:
            Notification n = new Notification(data)
        when:
            def result = n.toNormalizedMap()
        then:
            result == expected
        where:
            data                                                                              | expected
            [eventTrigger: 'onsuccess', type: 'aplugin', configuration: [something: 'vague']] |
            [type: 'aplugin', trigger: 'onsuccess', config: [something: 'vague']]
    }
    @Unroll
    def "fromNormalizedMap plugin"() {
        given:
            def type = 'aplugin'
            def data = [
                type   : type,
                trigger: trigger,
                config : config
            ]
        when:
            def result = Notification.fromNormalizedMap(data)
        then:
            result.eventTrigger == trigger
            result.type == type
            result.configuration == expect
        where:
            trigger     | config       | expect
            'onsuccess' | [abc: 'xyz'] | [abc: 'xyz']
            'onfailure' | [zyx: 123]   | [zyx: 123]

    }
}
