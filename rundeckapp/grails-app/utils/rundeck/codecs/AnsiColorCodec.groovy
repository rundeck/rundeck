package rundeck.codecs

import java.util.regex.Pattern

/**
 * AnsiColorCodec replaces ansi escapes with html spans
 * @author Greg Schueler <greg@simplifyops.com>
 * @since 2014-05-15
 */
class AnsiColorCodec {

    static ansimode = [
            0: 'mode-normal',
            1: 'mode-bold',
            4: 'mode-underline',
            5: 'mode-blink',
            7: 'mode-reverse',
            8: 'mode-nondisplayed',
    ]
    static ansicolors = [
            //foreground colors
            30: 'fg-black',
            31: 'fg-red',
            32: 'fg-green',
            33: 'fg-yellow',
            34: 'fg-blue',
            35: 'fg-magenta',
            36: 'fg-cyan',
            37: 'fg-white',
            39: 'fg-default',

            90: 'fg-light-black',
            91: 'fg-light-red',
            92: 'fg-light-green',
            93: 'fg-light-yellow',
            94: 'fg-light-blue',
            95: 'fg-light-magenta',
            96: 'fg-light-cyan',
            97: 'fg-light-white',
            //background colors
            40: 'bg-black',
            41: 'bg-red',
            42: 'bg-green',
            43: 'bg-yellow',
            44: 'bg-blue',
            45: 'bg-magenta',
            46: 'bg-cyan',
            47: 'bg-white',
            49: 'bg-default',
    ]
    def decode = { string ->
        def ctx = []
        def sb = new StringBuilder()
        def vals = string.split('\033[\\[%]')
        boolean coded = false
        vals.each { str ->
            if (str == '') {
                coded = true
                return
            }
            if (!coded) {
                sb << str.encodeAsHTMLElement()
                coded = true
                return
            }

            sb << ('' + (ctx ? ctx.collect { '</span>' }.join('') : ''))
            ctx = []

            def matcher = Pattern.compile('(?s)^(((\\d{1,2})?(;\\d{1,2})*)%?([mGHfABCDRsuhl])).*$').matcher(str)
            if (matcher.matches()) {
                def len = matcher.group(1).length()
                def cols = []
                def invalid = false
                if (matcher.groupCount() > 1) {
                    if (matcher.groupCount() > 4 && matcher.group(5) == 'G' && matcher.group(3)) {
                        //column shift position
                        def val=Integer.parseInt(matcher.group(3))
                        def shift = val-sb.length()
                        if (shift > 0) {
                            sb << ' ' * shift
                        }
                    }else if (matcher.groupCount() > 4 && matcher.group(5) != 'm') {
                        sb << ''
                    }
                    if (!matcher.group(2)) {
                        cols << 0
                    } else {
                        def strs = matcher.group(2).split(";")

                        try {
                            strs.findAll { it != null }.each {
                                if (it != null) {
                                    cols << Integer.parseInt(it)
                                }
                            }
                        } catch (NumberFormatException e) {
                            sb << matcher.group(1).encodeAsHTMLElement()
                        }
                    }
                }
                if (!cols) {
                    sb << matcher.group(1).encodeAsHTMLElement()
                } else {
                    //ctx
                    def rvals = cols.findAll { it && (ansicolors[it] || ansimode[it]) }.collect {
                        'ansi-' + (ansicolors[it] ?: ansimode[it])
                    }.join(' ')
                    if (rvals) {
                        ctx << rvals
                    }

                    sb << (rvals ? '<span class="' + rvals + '">' : '')
                }
                str = str.substring(len).encodeAsHTMLElement()
            }
            sb << str.encodeAsHTMLElement()
        }
        sb << ('' + (ctx ? ctx.collect { '</span>' }.join('') : ''))
        sb.toString()
    }
}
