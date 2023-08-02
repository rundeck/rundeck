package org.rundeck.tests.api.tests.execution

import org.rundeck.tests.api.util.Base

import java.util.function.Function


class ExecutionOutputSpec extends Base {
    def setup() {
        setupProject()
    }

    def "execution output using lastmod param"() {
        def params = "exec=echo+testing+execution+output+api1+line+1;sleep+2;echo+line+2;sleep+2;echo+line+3;sleep+2;" +
                     "echo+line+4+final"
        when:
            def adhoc = post("/project/${PROJECT_NAME}/run/command?${params}", Map)
        then:
            adhoc.execution != null
            adhoc.execution.id != null
        when:
            def execid = adhoc.execution.id
            def logs = getAllLogs(execid) {
                "offset=${it.offset}&lastmod=${it.lastmod}"
            }
        then:
            logs == [
                "testing execution output api1 line 1",
                "line 2",
                "line 3",
                "line 4 final"
            ]
    }

    def "execution output using maxlines param"() {
        def params = "exec=echo+testing+execution+output+api1+line+1;sleep+2;echo+line+2;sleep+2;echo+line+3;sleep+2;" +
                     "echo+line+4+final"
        when:
            def adhoc = post("/project/${PROJECT_NAME}/run/command?${params}", Map)
        then:
            adhoc.execution != null
            adhoc.execution.id != null
        when:
            def execid = adhoc.execution.id
            def logs = getAllLogs(execid) {
                "offset=${it.offset}&maxlines=1"
            }
        then:
            logs == [
                "testing execution output api1 line 1",
                "line 2",
                "line 3",
                "line 4 final"
            ]
    }

    def "execution output greedy"() {
        def params = "exec=echo+testing+execution+output+api1+line+1;sleep+2;echo+line+2;sleep+2;echo+line+3;sleep+2;" +
                     "echo+line+4+final"
        when:
            def adhoc = post("/project/${PROJECT_NAME}/run/command?${params}", Map)
        then:
            adhoc.execution != null
            adhoc.execution.id != null
        when:
            def execid = adhoc.execution.id
            def logs = getAllLogs(execid) {
                "offset=${it.offset}"
            }
        then:
            logs == [
                "testing execution output api1 line 1",
                "line 2",
                "line 3",
                "line 4 final"
            ]
    }

    def "execution output plain text"() {
        def params = "exec=echo+testing+execution+output+api1+line+1;sleep+2;echo+line+2;sleep+2;echo+line+3;sleep+2;" +
                     "echo+line+4+final"
        when:
            def adhoc = post("/project/${PROJECT_NAME}/run/command?${params}", Map)
        then:
            adhoc.execution != null
            adhoc.execution.id != null
        when:
            def execid = adhoc.execution.id
            def logs = getAllLogsPlain(execid) {
                "offset=${it.offset}&lastmod=${it.lastmod}"
            }
        then:
            logs == [
                "testing execution output api1 line 1",
                "line 2",
                "line 3",
                "line 4 final"
            ]
    }
    def "execution output plain text unicode"() {
        def params="exec=echo+%22%27testing+execution+%3Coutput%3E+api-unicode+line+1%27%22+;sleep+2;echo+line+%F0%9F%98%84;sleep+2;echo+%E4%BD%A0%E5%A5%BD;sleep+2;echo+line+4+final"
        when:
            def adhoc = post("/project/${PROJECT_NAME}/run/command?${params}", Map)
        then:
            adhoc.execution != null
            adhoc.execution.id != null
        when:
            def execid = adhoc.execution.id
            def logs = getAllLogsPlain(execid) {
                "offset=${it.offset}&lastmod=${it.lastmod}"
            }
        then:
            logs.join('\n') == '''testing execution <output> api-unicode line 1
line 😄
你好
line 4 final'''
    }

    def "execution output plain text using lastlines"() {
        def params = "exec=echo+testing+execution+output+api1+line+1;sleep+2;echo+line+2;sleep+2;echo+line+3;sleep+2;" +
                     "echo+line+4+final"
        when:
            def adhoc = post("/project/${PROJECT_NAME}/run/command?${params}", Map)
        then:
            adhoc.execution != null
            adhoc.execution.id != null
        when:
            def execid = adhoc.execution.id
            def status = waitForExec(execid)
        then:
            status == 'succeeded'
        when:
            def resp = request("/execution/${execid}/output?lastlines=1") {
                it.header 'Accept', 'text/plain'
            }
        then:
            resp.body().string() == 'line 4 final\n'
        when:
            resp = request("/execution/${execid}/output?lastlines=2") {
                it.header 'Accept', 'text/plain'
            }
        then:
            resp.body().string() == "line 3\nline 4 final\n"
        when:
            resp = request("/execution/${execid}/output?lastlines=3") {
                it.header 'Accept', 'text/plain'
            }
        then:
            resp.body().string() == "line 2\nline 3\nline 4 final\n"
        when:
            resp = request("/execution/${execid}/output?lastlines=4") {
                it.header 'Accept', 'text/plain'
            }
        then:
            resp.body().string() == "testing execution output api1 line 1\nline 2\nline 3\nline 4 final\n"
    }

    private String waitForExec(def execid, long maxwait = 10000) {
        long now = System.currentTimeMillis()
        String status = 'running'
        while (status in ['running', 'scheduled'] && System.currentTimeMillis() - now < maxwait) {
            def exec = get("/execution/${execid}", Map)
            status = exec.status
            if (status == 'running') {
                sleep(1000)
            }
        }
        return status
    }

    private List<String> getAllLogsPlain(execid, Function<Map, String> paramGen) {

        def logs = []
        def logging = [
            lastmod: "0",
            offset : "0",
            done   : false,
        ]
        def count = 1
        def max = 20
        while (!logging.done && count < max) {
            def params = paramGen.apply(logging)
            def resp = request("/execution/${execid}/output?${params}") {
                it.header 'Accept', 'text/plain'
            }
            def text = resp.body().string()
//            System.err.println("output: ${text}")
            if (text) {
                logs.addAll(text.split("\n"))
            }
            if (resp.header('X-Rundeck-ExecOutput-Offset') != null) {
                logging.offset = resp.header('X-Rundeck-ExecOutput-Offset')
            }
            if (resp.header('X-Rundeck-ExecOutput-LastModifed')) {
                logging.lastmod = resp.header('X-Rundeck-ExecOutput-LastModifed')
            }
            if (resp.header('X-Rundeck-Exec-Completed') && resp.header('X-Rundeck-ExecOutput-Completed')) {
                logging.done = (resp.header('X-Rundeck-Exec-Completed') == 'true')
                    && (resp.header('X-Rundeck-ExecOutput-Completed') == 'true')
            }
            if (resp.header('X-Rundeck-ExecOutput-Unmodified') == 'true') {
                sleep(2000)
            } else if (!logging.done) {
                sleep(1000)
            }
            count++
        }
        return logs
    }


    private List<String> getAllLogs(execid, Function<Map, String> paramGen) {
        def logs = []
        def logging = [
            lastmod: "0",
            offset : "0",
            done   : false,
        ]
        def count = 1
        def max = 20
        while (!logging.done && count < max) {
            def params = paramGen.apply(logging)
            Map output = get("/execution/${execid}/output?${params}", Map)
            assert output != null
            assert output.id == execid.toString()
//                System.err.println("output: ${output}")
            if (output.entries && output.entries.size() > 0) {
                logs.addAll(output.entries*.log)
            }
            if (output.offset != null) {
                logging.offset = output.offset
            }
            if (output.lastModified) {
                logging.lastmod = output.lastModified
            }
            if (output.completed != null && output.execCompleted != null) {
                logging.done = output.execCompleted && output.completed
            }
            if (output.unmodified == true) {
                sleep(2000)
            } else if (!logging.done) {
                sleep(1000)
            }
            count++
        }
        return logs
    }
}
