package org.rundeck.app.components

import groovy.transform.CompileStatic
import org.rundeck.app.components.jobs.JobDefinitionException
import org.rundeck.app.components.jobs.JobFormat
import spock.lang.Specification
import spock.lang.Unroll

class JobJSONFormatSpec extends Specification {

    @Unroll
    def "decode expects list of maps"() {
        given:
            def sut = new JobJSONFormat()
        when:
            def result = sut.decode(new StringReader(input))
        then:
            JobDefinitionException e = thrown()
            e.message.contains(expected)

        where:
            input              | expected
            '{"a":"b"}'        | 'Expected list data'
            '"asdf"'             | 'Expected list data'
            '1123'             | 'Expected list data'
            'true'             | 'Expected list data'
            '[1]'              | 'Expected list of Maps'
            '["asdf"]'           | 'Expected list of Maps'
            '[[1]]'            | 'Expected list of Maps'
            '[true]'           | 'Expected list of Maps'
            '[{"a":"b"},1]'    | 'Expected list of Maps'
            '[{"a":"b"},"asdf"]' | 'Expected list of Maps'
            '[{"a":"b"},true]' | 'Expected list of Maps'
            '[{"a":"b"},[1]]'  | 'Expected list of Maps'
    }


    @Unroll
    def "encode"() {
        given:
            def sut = new JobJSONFormat()
            def writer = new StringWriter()
            def data = input
            def options = JobFormat.options(true, null, (String) null)
        when:
            sut.encode(data, options, writer)
        then:
            writer.toString() == expected
        where:
            input | expected
            [[:]] | '[ { } ]'
    }

    @Unroll
    def "encode option preserveId"() {
        given:
            def sut = new JobJSONFormat()
            def writer = new StringWriter()
            def data = input
            def options = JobFormat.options(preserve, null, (String) null)
        when:
            sut.encode(data, options, writer)
        then:
            writer.toString() == expected
        where:
            input                          | preserve | expected
            [[id: 'test1', uuid: 'test2']] | true     | '[ {\n  "id" : "test1",\n  "uuid" : "test2"\n} ]'
            [[id: 'test1', uuid: 'test2']] | false    | '[ { } ]'
    }

    @Unroll
    def "encode option replaceIds"() {
        given:
            def sut = new JobJSONFormat()
            def writer = new StringWriter()
            def data = input
            def options = JobFormat.options(false, replacements, (String) null)
        when:
            sut.encode(data, options, writer)
        then:
            writer.toString() == expected
        where:
            input                          | replacements | expected
            [[id: 'test1', uuid: 'test1']] | [:]          | '[ { } ]'
            [[id: 'test1', uuid: 'test1']] | [test1: 'x'] | '[ {\n  "id" : "x",\n  "uuid" : "x"\n} ]'
            [[id: 'test1', uuid: 'test1']] | [test2: 'x'] | '[ { } ]'
    }

    @Unroll
    def "encoding is canonical"() {
        given:
            def sut = new JobJSONFormat()
            def writer = new StringWriter()
            def data = input
            def options = JobFormat.options(false, null, (String) null)
        when:
            sut.encode(data, options, writer)
        then:
            writer.toString() == expected
        where:
            input                               | expected
            [[z: 'xyz', a: 'pqr']]              | '[ {\n  "a" : "pqr",\n  "z" : "xyz"\n} ]'
            [[a: 'pqr', z: 'xyz']]              | '[ {\n  "a" : "pqr",\n  "z" : "xyz"\n} ]'
            [[z: [Z: 'z', A: 'A'], a: 'pqr']]   | '[ {\n  "a" : "pqr",\n  "z" : {\n    "A" : "A",\n    "Z" : "z"\n  }\n} ]'
            [[z: [[Z: 'z', A: 'A']], a: 'pqr']] | '[ {\n  "a" : "pqr",\n  "z" : [ {\n    "A" : "A",\n    "Z" : "z"\n  } ]\n} ]'
    }


    @Unroll
    def "encoding multiline line endings are unix"() {
        given:
            def sut = new JobJSONFormat(trimSpacesFromLines: trimSpaces)
            def writer = new StringWriter()
            def data = [[a: text]]
            def options = JobFormat.options(false, null, (String) null)
        when:
            sut.encode(data, options, writer)
        then:
            writer.toString() == expected
        where:
            text            | trimSpaces    | expected
            'ab\nc'         | false         | '[ {\n  "a" : "ab\\nc"\n} ]'
            'ab\r\nc'       | false         | '[ {\n  "a" : "ab\\nc"\n} ]'
            'ab\rc'         | false         | '[ {\n  "a" : "ab\\nc"\n} ]'
            'ab \rc'        | false         | '[ {\n  "a" : "ab \\nc"\n} ]'
            'ab \rc'        | true          | '[ {\n  "a" : "ab\\nc"\n} ]'
            'ab\n \nc'      | false         | '[ {\n  "a" : "ab\\n \\nc"\n} ]'
            'ab\n \nc'      | true          | '[ {\n  "a" : "ab\\n\\nc"\n} ]'
            'ab\n \nc \n '  | true          | '[ {\n  "a" : "ab\\n\\nc\\n"\n} ]'
            'ab\n \n c \n ' | true          | '[ {\n  "a" : "ab\\n\\n c\\n"\n} ]'
    }
    @Unroll
    def "encoding comma strings are quoted"() {
        given:
            def sut = new JobJSONFormat()
            def writer = new StringWriter()
            def data = [[a: text]]
            def options = JobFormat.options(false, null, (String) null)
        when:
            sut.encode(data, options, writer)
        then:
            writer.toString() == expected
        where:
            text   | expected
            'abc'  | '[ {\n  "a" : "abc"\n} ]'
            '123'  | '[ {\n  "a" : "123"\n} ]'
            '1,23' | '[ {\n  "a" : "1,23"\n} ]'
            'a,bc' | '[ {\n  "a" : "a,bc"\n} ]'
    }


    @Unroll
    def "should return a notification list of map with email and webhook notifs"() {
        given:
            def input = """[
  {
    "defaultTab": "nodes",
    "description": "",
    "executionEnabled": true,
    "loglevel": "INFO",
    "name": "a",
    "nodeFilterEditable": false,
    "notification": {
      "onsuccess": [
        {
          "email": {
            "attachLog": true,
            "attachLogInFile": true,
            "recipients": "leojesus.juarez@gmail.com",
            "subject": "RD-SUCCESS"
          }
        },
        {
          "format": "xml",
          "httpMethod": "get",
          "urls": "http://localhost:4440/project"
        }
      ]
    },
    "notifyAvgDurationThreshold": null,
    "plugins": {
      "ExecutionLifecycle": null
    },
    "scheduleEnabled": true,
    "schedules": [],
    "sequence": {
      "commands": [
        {
          "exec": "asd"
        }
      ],
      "keepgoing": false,
      "strategy": "node-first"
    }
  }
]
"""
            def sut = new JobJSONFormat()
        when:
            def result = sut.decode(new StringReader(input))
        then:
            result[0].notification['onsuccess'].size() == 2
            result[0].notification['onsuccess'].findAll{ it['email'] != null }.size() == 1
            result[0].notification['onsuccess'].findAll{ it['urls'] != null }.size() == 1
    }

    @Unroll
    def "should return a notification list with one webhook notification"() {
        given:
            def input = """[
  {
    "defaultTab": "nodes",
    "description": "",
    "executionEnabled": true,
    "loglevel": "INFO",
    "name": "a",
    "nodeFilterEditable": false,
    "notification": {
      "onsuccess": {
        "format": "json",
        "httpMethod": "post",
        "urls": "http://localhost:4440/project"
      }
    },
    "notifyAvgDurationThreshold": null,
    "plugins": {
      "ExecutionLifecycle": null
    },
    "scheduleEnabled": true,
    "schedules": [],
    "sequence": {
      "commands": [
        {
          "exec": "asd"
        }
      ],
      "keepgoing": false,
      "strategy": "node-first"
    }
  }
]
"""
            def sut = new JobJSONFormat()
        when:
            def result = sut.decode(new StringReader(input))
        then:
            result[0].notification['onsuccess'].size() == 1
            result[0].notification['onsuccess'].findAll{ it['urls'] != null }.size() == 1
    }

    @Unroll
    def "should return list of notifications for each trigger when some triggers have notifs in yaml list and some not in a list"() {
        given:
            def input = """[
  {
    "defaultTab": "nodes",
    "description": "",
    "executionEnabled": true,
    "loglevel": "INFO",
    "name": "a",
    "nodeFilterEditable": false,
    "notification": {
      "onsuccess": [
        {
          "email": {
            "attachLog": true,
            "attachLogInFile": true,
            "recipients": "leojesus.juarez@gmail.com",
            "subject": "RD-SUCCESS"
          }
        },
        {
          "email": {
            "attachLog": true,
            "attachLogInline": true,
            "recipients": "2@gmail.com",
            "subject": "RD-SUCCESS"
          }
        },
        {
          "format": "xml",
          "httpMethod": "get",
          "urls": "http://localhost:4440/project"
        },
        {
          "format": "json",
          "httpMethod": "post",
          "urls": "http://localhost:4440/project"
        }
      ],
      "onfailure": {
        "format": "json",
        "httpMethod": "post",
        "urls": "http://localhost:4440/project"
      }
    },
    "notifyAvgDurationThreshold": null,
    "plugins": {
      "ExecutionLifecycle": null
    },
    "scheduleEnabled": true,
    "schedules": [],
    "sequence": {
      "commands": [
        {
          "exec": "asd"
        }
      ],
      "keepgoing": false,
      "strategy": "node-first"
    }
  }
]
"""
            def sut = new JobJSONFormat()
        when:
            def result = sut.decode(new StringReader(input))
        then:
            result[0].notification['onsuccess'].size() == 4
            result[0].notification['onsuccess'].findAll{ it['email'] != null }.size() == 2
            result[0].notification['onsuccess'].findAll{ it['urls'] != null }.size() == 2

            result[0].notification['onfailure'].size() == 1
            result[0].notification['onfailure'].findAll{ it['urls'] != null }.size() == 1
    }

    @Unroll
    def "should return list of notifications for each trigger when all triggers have notifs in yaml maps"() {
        given:
            def input = """[
  {
    "defaultTab": "nodes",
    "description": "",
    "executionEnabled": true,
    "loglevel": "INFO",
    "name": "a",
    "nodeFilterEditable": false,
    "notification": {
      "onstart": {
        "email": {
          "attachLog": "true",
          "attachLogInFile": true,
          "recipients": "tom@example.com",
          "subject": "JOB-STARTED"
        }
      },
      "onfailure": {
        "email": {
          "recipients": "tom@example.com,shirley@example.com"
        }
      },
      "onsuccess": {
        "format": "json",
        "httpMethod": "post",
        "urls": "http://localhost:4440/project",
        "plugin": {
          "type": "my-plugin",
          "configuration": {
            "somekey": "somevalue"
          }
        }
      },
      "onavgduration": {
        "email": {
          "recipients": "test@example.com",
          "subject": "Job Exceeded average duration"
        },
        "plugin": {
          "type": "my-plugin",
          "configuration": {
            "somekey": "somevalue"
          }
        }
      },
      "onretryablefailure": {
        "plugin": {
          "type": "my-plugin",
          "configuration": {
            "somekey": "somevalue"
          }
        }
      }
    },
    "notifyAvgDurationThreshold": "+30",
    "plugins": {
      "ExecutionLifecycle": null
    },
    "scheduleEnabled": true,
    "schedules": [],
    "sequence": {
      "commands": [
        {
          "exec": "asd"
        }
      ],
      "keepgoing": false,
      "strategy": "node-first"
    }
  }
]
"""
            def sut = new JobJSONFormat()
        when:
            def result = sut.decode(new StringReader(input))
        then:
            result[0].notification['onstart'].size() == 1
            result[0].notification['onstart'].findAll{ it['email'] != null }.size() == 1

            result[0].notification['onfailure'].size() == 1
            result[0].notification['onfailure'].findAll{ it['email'] != null }.size() == 1

            result[0].notification['onsuccess'].size() == 1
            result[0].notification['onsuccess'].findAll{ it['urls'] != null }.size() == 1
            result[0].notification['onsuccess'].findAll{ it['plugin'] != null }.size() == 1

            result[0].notification['onavgduration'].size() == 1
            result[0].notification['onavgduration'].findAll{ it['email'] != null }.size() == 1
            result[0].notification['onavgduration'].findAll{ it['plugin'] != null }.size() == 1

            result[0].notification['onretryablefailure'].size() == 1
            result[0].notification['onretryablefailure'].findAll{ it['plugin'] != null }.size() == 1
    }

    @Unroll
    def "should return an yaml str with list of objects in notification trigger"() {
        given:
            List<Map> input = [[
                                   id: 0,
                                   defaultTab: "nodes",
                                   description: "",
                                   loglevel: "INFO",
                                   name: "a",
                                   nodeFilterEditable: false,
                                   notification: [
                                       onsuccess: [
                                           [email: [recipients: "mail@example.com"]],
                                           [email: [recipients: "mail2@example.com"]],
                                           [
                                               format: "xml",
                                               httpMethod: "get",
                                               urls: "http://example1.com/1"
                                           ],
                                           [
                                               format: "json",
                                               httpMethod: "post",
                                               urls: "https://example2.com/2"
                                           ]
                                       ]
                                   ],
                                   notifyAvgDurationThreshold: "",
                                   plugins: "",
                                   scheduledEnabled: "true",
                                   schedules: "",
                                   sequence: [
                                       keepgoing: false,
                                       strategy: "node-first",
                                       commands: [[exec:"asd"]]
                                   ],
                                   executionEnabled: true,
                                   multipleExecutions: false
                               ]]
            def sut = new JobJSONFormat()
            def writer = new StringWriter()
            def options = JobFormat.options(true, [:], (String) null)

        when:
            sut.encode(input, options, writer)
            String notifStr = writer.toString()
            notifStr = notifStr.replaceAll("\\s","")

        then:
            notifStr.contains('{"email":{"recipients":"mail@example.com"}}')
            notifStr.contains('{"email":{"recipients":"mail2@example.com"}}')
            notifStr.contains('{"format":"xml","httpMethod":"get","urls":"http://example1.com/1"}')
            notifStr.contains('{"format":"json","httpMethod":"post","urls":"https://example2.com/2"}')
    }
}
