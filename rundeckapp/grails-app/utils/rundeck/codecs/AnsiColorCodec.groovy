package rundeck.codecs

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

            60: 'fg-light-black',
            61: 'fg-light-red',
            62: 'fg-light-green',
            63: 'fg-light-yellow',
            64: 'fg-light-blue',
            65: 'fg-light-magenta',
            66: 'fg-light-cyan',
            67: 'fg-light-white',
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
    def decode = { str ->
        def ctx = []
        str.replaceAll('\033\\[((\\d{1,2})?(;\\d{1,2})*)([mGHfABCDRsuhl])') { match ->
            def cols = []
            def invalid = false
            if (match.size() > 0) {
                if(match.size()>3 && match[4]!='m'){
                    return ''
                }
                if (!match[1]) {
                    cols << 0
                } else {
                    def strs = match[1].split(";")

                    try {
                        strs.findAll { it != null }.each {
                            if (it != null) {
                                cols << Integer.parseInt(it)
                            }
                        }
                    } catch (NumberFormatException e) {
                        return match[0]
                    }
                }
            }
            if (!cols) {
                return match[0]
            } else {
                //ctx
                def outstr=(''+(ctx? ctx.collect { '</span>' }.join(''):''))
                ctx=[]
                def vals = cols.collect {
                    if(it){
                        ctx << it
                        return (ansicolors[it] ?: ansimode[it] ?: '')
                    }else{
                        null
                    }
                }.findAll{it}.join(' ')

                return outstr+(vals? '<span class="ansicolor ' + vals + '">' :'')
            }
        } + (ctx.collect { '</span>' }.join(''))
    }
}
