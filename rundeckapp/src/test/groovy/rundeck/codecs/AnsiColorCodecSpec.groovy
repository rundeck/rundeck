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
            39   | 'fg-default'

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
            49   | 'bg-default'
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
            AnsiColorCodec.decode('\u001B[' + mode + 'masdf') == '<span style="' + text + '">asdf</span>'
        where:
            mode           | text
            '38;2;0;0;0'   | 'color: rgb(0,0,0);'
            '48;2;0;0;0'   | 'background-color: rgb(0,0,0);'
            '38;2;255;0;0' | 'color: rgb(255,0,0);'
            '48;2;255;0;0' | 'background-color: rgb(255,0,0);'
            '38;2;0;255;0' | 'color: rgb(0,255,0);'
            '38;2;0;0;255' | 'color: rgb(0,0,255);'
    }

    @Unroll
    def "multiple #mode"() {
        expect:
            AnsiColorCodec.decode('\u001B[' + mode + 'masdf') == '<span ' + text + '>asdf</span>'
        where:
            mode                  | text
            '1;38;5;216'          | 'class="ansi-mode-bold ansi-fg-rgb-5-3-2"'
            '1;38;5;216;48;5;102' | 'class="ansi-mode-bold ansi-fg-rgb-5-3-2 ansi-bg-rgb-2-2-2"'
            '1;38;2;255;128;128'  | 'class="ansi-mode-bold" style="color: rgb(255,128,128);"'

    }
}
