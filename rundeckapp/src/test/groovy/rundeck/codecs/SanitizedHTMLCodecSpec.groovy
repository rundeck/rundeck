package rundeck.codecs

import spock.lang.Specification

/**
 * SanitizedHTMLCodecSpec covers the OWASP-based HTML allow-list used to
 * render plugin-declared HTML content safely.
 */
class SanitizedHTMLCodecSpec extends Specification {

    def "anchor href is preserved with rel=nofollow added"() {
        expect:
        SanitizedHTMLCodec.encode('<a href="http://test.com">a</a>') ==
            '<a href="http://test.com" rel="nofollow">a</a>'
    }

    def "javascript href is stripped"() {
        expect:
        SanitizedHTMLCodec.encode('<a href="javascript://alert(1)">a</a>') == 'a'
    }

    def "onclick attribute is stripped"() {
        expect:
        SanitizedHTMLCodec.encode('<a href="http://test.com" onclick="alert(1)">a</a>') ==
            '<a href="http://test.com" rel="nofollow">a</a>'
    }

    def "script tag is stripped"() {
        expect:
        SanitizedHTMLCodec.encode('<script>alert(1)</script>') == ''
    }

    def "style attribute must be CSS-guarded, not passed through verbatim"() {
        expect:
        SanitizedHTMLCodec.encode(
            '<td style="position:fixed;inset:0;width:100vw;height:100vh;background:url(\'//attacker/leak\')">x</td>'
        ) == '<table><tbody><tr><td style="width:100vw;height:100vh">x</td></tr></tbody></table>'
    }

    def "svg rect style with malicious CSS is stripped"() {
        expect:
        SanitizedHTMLCodec.encode(
            '<svg><rect style="position:fixed;inset:0;background:url(\'//attacker/leak\')" fill="red"/></svg>'
        ) == '<svg><rect fill="red"></rect></svg>'
    }

    def "normal CSS on td style still works"() {
        expect:
        SanitizedHTMLCodec.encode('<td style="color:red;font-weight:bold;text-align:center">ok</td>') ==
            '<table><tbody><tr><td style="color:red;font-weight:bold;text-align:center">ok</td></tr></tbody></table>'
    }

    def "a plugin-declared HTML table survives sanitization intact"() {
        given:
        String info = '<table>\n' +
                      '    <tr><td>1.</td><td class="text-info">NodeA</td> <td>step 1</td></tr>\n' +
                      '    <tr><td>2.</td><td class="text-info">"</td> <td>step 2</td></tr>\n' +
                      '    <tr><td>3.</td><td class="text-info">"</td> <td>step 3</td></tr>\n' +
                      '    <tr><td>4.</td><td class="text-muted">NodeB</td> <td>step 1</td></tr>\n' +
                      '    <tr><td>5.</td><td class="text-muted">"</td> <td>step 2</td></tr>\n' +
                      '    <tr><td>6.</td><td class="text-muted">"</td> <td>step 3</td></tr>\n' +
                      '</table>'

        when:
        String result = SanitizedHTMLCodec.encode(info)

        then:
        result.contains('<table>')
        !result.contains('&lt;table&gt;')
        (result =~ /<td[ >]/).count == 18
        result.contains('NodeA')
        result.contains('step 1')
        result.contains('class="text-info"')
    }

    def "an injected script tag is stripped even from otherwise-safe content"() {
        given:
        String tampered = '<table><tr><td>1.</td><td class="text-info">NodeA<script>alert(document.cookie)</script></td></tr></table>'

        when:
        String result = SanitizedHTMLCodec.encode(tampered)

        then:
        !result.toLowerCase().contains('<script')
        !result.toLowerCase().contains('alert(')
        result.contains('<table>')
        result.contains('NodeA')
    }
}
