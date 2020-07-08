package rundeck.codecs

import grails.testing.web.GrailsWebUnitTest
import org.grails.plugins.codecs.HTMLCodec
import spock.lang.Specification
import spock.lang.Unroll

class AnsiColorCodecSpec extends Specification implements GrailsWebUnitTest {

    def setup() {
        mockCodec(HTMLCodec)
    }

    @Unroll
    def "basic"() {
        expect:
            AnsiColorCodec.decode(input) == output
        where:
            input                    | output
            'asdf'                   | 'asdf'
            '\u001B[masdf'           | 'asdf'
            '\u001B[0masdf'          | 'asdf'
            '\u001B[1masdf'          | '<span class="ansi-mode-bold">asdf</span>'
            '\u001B[1mas\u001B[0mdf' | '<span class="ansi-mode-bold">as</span>df'
    }

    @Unroll
    def "modes"() {
        expect:
            AnsiColorCodec.decode('\u001B[' + mode + 'masdf') == '<span class="ansi-mode-' + text + '">asdf</span>'
        where:
            mode | text
            1    | 'bold'
            4    | 'underline'
            5    | 'blink'
            7    | 'reverse'
            8    | 'nondisplayed'
    }

    @Unroll
    def "colors"() {
        expect:
            AnsiColorCodec.decode('\u001B[' + mode + 'masdf') == '<span class="ansi-' + text + '">asdf</span>'
        where:
            mode | text
            30   | 'fg-black'
            31   | 'fg-red'
            32   | 'fg-green'
            33   | 'fg-yellow'
            34   | 'fg-blue'
            35   | 'fg-magenta'
            36   | 'fg-cyan'
            37   | 'fg-white'
//            39   | 'fg-default'

            90   | 'fg-light-black'
            91   | 'fg-light-red'
            92   | 'fg-light-green'
            93   | 'fg-light-yellow'
            94   | 'fg-light-blue'
            95   | 'fg-light-magenta'
            96   | 'fg-light-cyan'
            97   | 'fg-light-white'
            //background colors
            40   | 'bg-black'
            41   | 'bg-red'
            42   | 'bg-green'
            43   | 'bg-yellow'
            44   | 'bg-blue'
            45   | 'bg-magenta'
            46   | 'bg-cyan'
            47   | 'bg-white'
//            49   | 'bg-default'
    }
    @Unroll
    def "colors default"() {
        expect:
            AnsiColorCodec.decode('\u001B[' + mode + 'masdf') == text
        where:
            mode | text
            39   | 'asdf'

            //background colors
            49   | 'asdf'
    }

    @Unroll
    def "8 bit color #text"() {
        expect:
            AnsiColorCodec.decode('\u001B[' + mode + 'masdf') == '<span class="ansi-' + text + '">asdf</span>'
        where:
            mode       | text
            '38;5;16'  | 'fg-rgb-0-0-0'
            '48;5;16'  | 'bg-rgb-0-0-0'
            '38;5;17'  | 'fg-rgb-0-0-1'
            '38;5;21'  | 'fg-rgb-0-0-5'
            '38;5;22'  | 'fg-rgb-0-1-0'
            '38;5;46'  | 'fg-rgb-0-5-0'
            '38;5;52'  | 'fg-rgb-1-0-0'
            '38;5;196' | 'fg-rgb-5-0-0'
            '38;5;231' | 'fg-rgb-5-5-5'
            '48;5;231' | 'bg-rgb-5-5-5'
            '38;5;59'  | 'fg-rgb-1-1-1'
            '48;5;59'  | 'bg-rgb-1-1-1'
    }

    @Unroll
    def "16 bit color #mode"() {
        expect:
            AnsiColorCodec.decode('\u001B[' + mode + 'masdf') == '<span class="'+css+'" style="' + style + '">asdf</span>'
        where:
            mode           |css|style
            '38;2;0;0;0'   |'ansi-fg'|'--fg-color: rgb(0,0,0);'
            '48;2;0;0;0'   |'ansi-bg'|'--bg-color: rgb(0,0,0);'
            '38;2;255;0;0' |'ansi-fg'|'--fg-color: rgb(255,0,0);'
            '48;2;255;0;0' |'ansi-bg'|'--bg-color: rgb(255,0,0);'
            '38;2;0;255;0' |'ansi-fg'|'--fg-color: rgb(0,255,0);'
            '38;2;0;0;255' |'ansi-fg'|'--fg-color: rgb(0,0,255);'
            '38;2;0;0;255;48;2;12;29;99' |'ansi-bg ansi-fg'|'--fg-color: rgb(0,0,255);--bg-color: rgb(12,29,99);'
    }

    @Unroll
    def "multiple #mode"() {
        expect:
            AnsiColorCodec.decode('\u001B[' + mode + 'masdf') == '<span ' + text + '>asdf</span>'
        where:
            mode                  | text
            '1;38;5;216'          | 'class="ansi-fg-rgb-5-3-2 ansi-mode-bold"'
            '1;38;5;216;48;5;102' | 'class="ansi-bg-rgb-2-2-2 ansi-fg-rgb-5-3-2 ansi-mode-bold"'
            '1;38;2;255;128;128'  | 'class="ansi-fg ansi-mode-bold" style="--fg-color: rgb(255,128,128);"'
    }

    @Unroll
    def "multiple clause"() {
        expect:
            AnsiColorCodec.decode('\u001B[' + mode1 + 'masdf' + '\u001B[' + mode2 + 'mxyz') == (
                '<span ' + text +
                '>asdf' +
                rest
            )
        where:
            mode1                  | mode2 | text                                       | rest
            '1;38;5;216'           | '0'   | 'class="ansi-fg-rgb-5-3-2 ansi-mode-bold"' | '</span>xyz'
            '1;38;5;216' |'4'| 'class="ansi-fg-rgb-5-3-2 ansi-mode-bold"'|'<span class="ansi-mode-underline">xyz</span></span>'
            '31' |'4'| 'class="ansi-fg-red"'|'<span class="ansi-mode-underline">xyz</span></span>'
            '31' |'32'| 'class="ansi-fg-red"'|'</span><span class="ansi-fg-green">xyz</span>'
            '31' |'0;4'| 'class="ansi-fg-red"'|'</span><span class="ansi-mode-underline">xyz</span>'
            '31' |'46'| 'class="ansi-fg-red"'|'<span class="ansi-bg-cyan">xyz</span></span>'
    }
    @Unroll
    def "general"() {
        expect:
            AnsiColorCodec.decode(input) == (text)
        where:
            input | text
            '\u001B[38;5;44m-\u001B[39m\u001B[38;5;44m \u001B[39m\u001B[38;5;43mR\u001B[39m\u001B[38;5;49mD\u001B[39m' |
            '<span class="ansi-fg-rgb-0-4-4">-</span><span class="ansi-fg-rgb-0-4-4"> </span><span class="ansi-fg-rgb-0-4-3">R</span><span class="ansi-fg-rgb-0-5-3">D</span>'
        '\u001B[38;2;255;2;255mThis \u001B[48;2;211;211;221mis a test' |
        '<span class="ansi-fg" style="--fg-color: rgb(255,2,255);">This <span class="ansi-bg" style="--bg-color: rgb(211,211,221);">is a test</span></span>'
        '\u001B[38;2;0;255;128;48;2;255;0;128mtest'|'<span class="ansi-bg ansi-fg" style="--fg-color: rgb(0,255,128);--bg-color: rgb(255,0,128);">test</span>'
    }
    @Unroll
    def "invalid 16 bit"() {
        expect:
            AnsiColorCodec.decode(input) == (text)
        where:
            input | text
            '\u001B[38;2;256;0;128mblah' | 'blah'

    }
}
