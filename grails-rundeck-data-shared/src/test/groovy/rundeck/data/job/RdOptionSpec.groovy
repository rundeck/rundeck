package rundeck.data.job

import spock.lang.Specification

class RdOptionSpec extends Specification {
    def "Option values should be parsed from valuesList"() {
        given:
            def option = new RdOption()
            option.valuesList = optionValuesList
            option.valuesListDelimiter = optionValuesDelimiter
        when:
            def values = option.getOptionValues()
        then:
            values == ["a", "b", "c"]
        where:
            optionValuesDelimiter | optionValuesList
            null                  | "a,b,c"
            ","                   | "a,b,c"
            ";"                   | "a;b;c"
            " "                   | "a b c"
    }

    def "All numeric Option values should be sorted numerically"() {
        given:
            def option = new RdOption()
            option.valuesList = valuesList
            option.sortValues = true
        when:
            def values = option.getOptionValues()
        then:
            values == expected
        where:
            valuesList         | expected
            "1,2,3,10,20,30"   | ["1", "2", "3", "10", "20", "30"]
            "30,20,10,3,2,1"   | ["1", "2", "3", "10", "20", "30"]
            "3,2,1,30,20,10"   | ["1", "2", "3", "10", "20", "30"]
            "10,20,30,1,2,3"   | ["1", "2", "3", "10", "20", "30"]
            "10,20.5,30,1,2,3" | ["1", "2", "3", "10", "20.5", "30"]

    }

    def "non numeric values should be sorted alphabetically"() {
        given:
            def option = new RdOption()
            option.valuesList = valuesList
            option.sortValues = true
        when:
            def values = option.getOptionValues()
        then:
            values == ["a", "b", "c", "x", "y", "z"]
        where:
            valuesList << ["a,b,c,x,y,z", "z,y,x,c,b,a"]
    }

    def "mixed values should be sorted"() {
        given:
            def option = new RdOption()
            option.valuesList = valuesList
            option.sortValues = true
        when:
            def values = option.getOptionValues()
        then:
            values == ["1", "2", "3", "a", "b", "c"]
        where:
            valuesList << ["1,2,3,a,b,c", "c,b,a,3,2,1"]
    }
}
