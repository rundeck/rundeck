package org.rundeck.plugin.scriptnodestep


import spock.lang.Specification

class BashShellUtilSpec extends Specification {
    static final String Q = "'"
    static final String QQ = '"'
    static final String BS = "\\"

    def "encode value"() {
        given:
            def bashEncoderUtil = new BashShellUtil()
        when:
            def result = bashEncoderUtil.quote(input)
        then:
            result == expect
        where:
            input           | expect
            'asdf'          | 'asdf'
            'asdf😀'        | 'asdf😀'
            "asdf${Q}d"     | "asdf${BS}${Q}d"
            "asdf${QQ}d"    | "asdf${BS}${QQ}d"
            'asdf;d'        | "asdf${BS};d"
            'asdf!d'        | "asdf${BS}!d"
            'asdf~d'        | "asdf${BS}~d"
            'asdf$d'        | "asdf${BS}\$d"
            'asdf<d'        | "asdf${BS}<d"
            'asdf>d'        | "asdf${BS}>d"
            "asdf${BS}d"    | "asdf${BS}${BS}d"
            'asdf d'        | "asdf${BS} d"
            'asdf  d'       | "asdf${BS} ${BS} d"
            'asdf \t d'     | "asdf${BS} \$${Q}\\t${Q}${BS} d"
            'asdf \n\r\b d' | "asdf${BS} \$${Q}\\n${Q}\$${Q}\\r${Q}\$${Q}\\b${Q}${BS} d"
            "asdf 'd"       | "asdf${BS} ${BS}${Q}d"
            "asdf 'd farms" | "asdf${BS} ${BS}${Q}d${BS} farms"
            "asdf1234!@#\$%^&*()_+-=[]{};':\"',.<>/?`~" | "asdf1234\\!@\\#\\\$%\\^\\&\\*\\(\\)_+-=\\[\\]\\{\\}\\;\\':\\\"\\',.\\<\\>/\\?\\`\\~"
            "! @ # \$ % ^ & * ( ) _ < > , . / ; ' : \" [ ] { } - = _ +"| "\\!\\ @\\ \\#\\ \\\$\\ %\\ \\^\\ \\&\\ \\*\\ \\(\\ \\)\\ _\\ \\<\\ \\>\\ ,\\ .\\ /\\ \\;\\ \\'\\ :\\ \\\"\\ \\[\\ \\]\\ \\{\\ \\}\\ -\\ =\\ _\\ +"
    }

    def "append env line"() {
        given:
            def bashEncoderUtil = new BashShellUtil()
            def sb = new StringBuilder()
        when:
            bashEncoderUtil.appendEnvLine(name, value, sb)
        then:
            sb.toString() == expect
        where:
            name   | value             | expect
            'asdf' | 'qwer'            | "asdf=qwer;"
            'asdf' | 'qwer ty'         | "asdf=qwer\\ ty;"
            'asdf' | 'qwer\nty'        | "asdf=qwer\$'\\n'ty;"
            'asdf' | 'qwer ty\'bin go' | "asdf=qwer\\ ty\\'bin\\ go;"
    }
}
