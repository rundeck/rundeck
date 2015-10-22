package com.dtolabs.rundeck.app.support

import groovy.xml.MarkupBuilder
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 10/16/15.
 */
class BuilderUtilSpec extends Specification {

    @Unroll
    def "multiline strings output with original or forced line endings"() {
        given:
        final StringWriter writer = new StringWriter()
        def builder = new MarkupBuilder(new IndentPrinter(new PrintWriter(writer), "", false))
        def bu = new BuilderUtil()
        bu.forceLineEndings = force
        bu.lineEndingChars = chars

        def map = [(key): string]

        when:
        bu.objToDom('test', map, builder)
        final String result = writer.toString()

        then:
        result == expected

        where:
        force | chars  | key           | string              | expected
        true  | '\n'   | 'data<cdata>' | 'abc def ghi'       | '<test><data><![CDATA[abc def ghi]]></data></test>'
        true  | '\n'   | 'data<cdata>' | 'abc\rdef\rghi'     | '<test><data><![CDATA[abc\ndef\nghi]]></data></test>'
        true  | '\n'   | 'data<cdata>' | 'abc\ndef\nghi'     | '<test><data><![CDATA[abc\ndef\nghi]]></data></test>'
        true  | '\n'   | 'data<cdata>' | 'abc\r\ndef\r\nghi' | '<test><data><![CDATA[abc\ndef\nghi]]></data></test>'
        true  | '\n'   | 'data'        | 'abc def ghi'       | '<test><data>abc def ghi</data></test>'
        true  | '\n'   | 'data'        | 'abc\rdef\rghi'     | '<test><data>abc\ndef\nghi</data></test>'
        true  | '\n'   | 'data'        | 'abc\ndef\nghi'     | '<test><data>abc\ndef\nghi</data></test>'
        true  | '\n'   | 'data'        | 'abc\r\ndef\r\nghi' | '<test><data>abc\ndef\nghi</data></test>'

        true  | '\r'   | 'data<cdata>' | 'abc def ghi'       | '<test><data><![CDATA[abc def ghi]]></data></test>'
        true  | '\r'   | 'data<cdata>' | 'abc\rdef\rghi'     | '<test><data><![CDATA[abc\rdef\rghi]]></data></test>'
        true  | '\r'   | 'data<cdata>' | 'abc\ndef\nghi'     | '<test><data><![CDATA[abc\rdef\rghi]]></data></test>'
        true  | '\r'   | 'data<cdata>' | 'abc\r\ndef\r\nghi' | '<test><data><![CDATA[abc\rdef\rghi]]></data></test>'
        true  | '\r'   | 'data'        | 'abc def ghi'       | '<test><data>abc def ghi</data></test>'
        true  | '\r'   | 'data'        | 'abc\rdef\rghi'     | '<test><data>abc\rdef\rghi</data></test>'
        true  | '\r'   | 'data'        | 'abc\ndef\nghi'     | '<test><data>abc\rdef\rghi</data></test>'
        true  | '\r'   | 'data'        | 'abc\r\ndef\r\nghi' | '<test><data>abc\rdef\rghi</data></test>'

        true  | '\r\n' | 'data<cdata>' | 'abc def ghi'       | '<test><data><![CDATA[abc def ghi]]></data></test>'
        true  | '\r\n' | 'data<cdata>' | 'abc\rdef\rghi'     | '<test><data><![CDATA[abc\r\ndef\r\nghi]]></data></test>'
        true  | '\r\n' | 'data<cdata>' | 'abc\ndef\nghi'     | '<test><data><![CDATA[abc\r\ndef\r\nghi]]></data></test>'
        true  | '\r\n' | 'data<cdata>' | 'abc\r\ndef\r\nghi' | '<test><data><![CDATA[abc\r\ndef\r\nghi]]></data></test>'
        true  | '\r\n' | 'data'        | 'abc def ghi'       | '<test><data>abc def ghi</data></test>'
        true  | '\r\n' | 'data'        | 'abc\rdef\rghi'     | '<test><data>abc\r\ndef\r\nghi</data></test>'
        true  | '\r\n' | 'data'        | 'abc\ndef\nghi'     | '<test><data>abc\r\ndef\r\nghi</data></test>'
        true  | '\r\n' | 'data'        | 'abc\r\ndef\r\nghi' | '<test><data>abc\r\ndef\r\nghi</data></test>'

        false | '\n'   | 'data<cdata>' | 'abc def ghi'       | '<test><data><![CDATA[abc def ghi]]></data></test>'
        false | '\n'   | 'data<cdata>' | 'abc\rdef\rghi'     | '<test><data><![CDATA[abc\rdef\rghi]]></data></test>'
        false | '\n'   | 'data<cdata>' | 'abc\ndef\nghi'     | '<test><data><![CDATA[abc\ndef\nghi]]></data></test>'
        false | '\n'   | 'data<cdata>' | 'abc\r\ndef\r\nghi' | '<test><data><![CDATA[abc\r\ndef\r\nghi]]></data></test>'
        false | '\n'   | 'data'        | 'abc def ghi'       | '<test><data>abc def ghi</data></test>'
        false | '\n'   | 'data'        | 'abc\rdef\rghi'     | '<test><data>abc\rdef\rghi</data></test>'
        false | '\n'   | 'data'        | 'abc\ndef\nghi'     | '<test><data>abc\ndef\nghi</data></test>'
        false | '\n'   | 'data'        | 'abc\r\ndef\r\nghi' | '<test><data>abc\r\ndef\r\nghi</data></test>'
    }
    def "replace line endings"() {

        when:
        def result = BuilderUtil.replaceLineEndings(string,chars)

        then:
        result == expected

        where:
         chars  | string              | expected
         '\n'   | 'abc def ghi'       | 'abc def ghi'
         '\n'   | 'abc\rdef\rghi'     | 'abc\ndef\nghi'
         '\n'   | 'abc\ndef\nghi'     | 'abc\ndef\nghi'
         '\n'   | 'abc\r\ndef\r\nghi' | 'abc\ndef\nghi'

         '\r'   | 'abc def ghi'       | 'abc def ghi'
         '\r'   | 'abc\rdef\rghi'     | 'abc\rdef\rghi'
         '\r'   | 'abc\ndef\nghi'     | 'abc\rdef\rghi'
         '\r'   | 'abc\r\ndef\r\nghi' | 'abc\rdef\rghi'

         '\r\n' | 'abc def ghi'       | 'abc def ghi'
         '\r\n' | 'abc\rdef\rghi'     | 'abc\r\ndef\r\nghi'
         '\r\n' | 'abc\ndef\nghi'     | 'abc\r\ndef\r\nghi'
         '\r\n' | 'abc\r\ndef\r\nghi' | 'abc\r\ndef\r\nghi'

    }
}
