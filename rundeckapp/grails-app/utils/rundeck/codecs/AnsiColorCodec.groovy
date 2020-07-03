/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck.codecs

import java.util.regex.Pattern

/**
 * AnsiColorCodec replaces ansi escapes with html spans
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
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
    static ansicolorsfg = [
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
    ]
    static ansicolorsbg = [
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
    static ansicolors = ansicolorsfg + ansicolorsbg
    static def decode = { string ->
        def ctx = [:]
        def cdepth=0
        def sb = new StringBuilder()
        def vals = string.split('\033[\\[%\\(]')
        boolean coded = false
        vals.each { str ->
            if (str == '') {
                coded = true
                return
            }
            if (!coded) {
                sb << str.encodeAsHTML()
                coded = true
                return
            }
            boolean reset=false

            def matcher = Pattern.compile('(?s)^(((\\d{1,2})?(;\\d{1,3})*)%?([mKGHfABCDRsuhl])).*$').matcher(str)
            if (matcher.matches() && matcher.groupCount() > 1 && matcher.group(5) == 'K') {
                //clear screen escape code
                //ignore this for now, and output the remaining text
                sb << str.substring(matcher.group(1).length()).encodeAsHTML()
                return
            }
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
                        reset=true
                    } else if(matcher.group(5) == 'm') {
                        def strs = matcher.group(2).split("\\s*;\\s*")

                        try {
                            strs.findAll { it != null }.each {
                                if (it != null) {
                                    cols << Integer.parseInt(it.trim())
                                }
                            }
                        } catch (NumberFormatException e) {
                            sb << matcher.group(1).encodeAsHTML()
                        }
                    }
                }

                    def nctx=new HashMap()
                    while(cols.size()>0) {
                        if(cols.size()>=5 && (cols[0]==38 || cols[0] == 48 ) && cols[1]==2){
                            //24 bit color
                            // https://en.wikipedia.org/wiki/ANSI_escape_code#Colors
                            def fg= cols[0]== 38 ? 'fg' : 'bg'
                            def rgb=[cols[2],cols[3],cols[4]]
                            if (rgb.any{it <0 || it>255}) {
                                cols = cols.subList(5, cols.size())
                                break
                            }
                            nctx[fg]=rgb
                            cols = cols.subList(5, cols.size())
                        }else if (cols.size() >= 3 && (cols[0] == 38 || cols[0] == 48) && cols[1] == 5) {
                            //256 col ansi
                            // https://en.wikipedia.org/wiki/ANSI_escape_code#Colors
                            def isfg = cols[0] == 38
                            def fg = isfg ? 'fg' : 'bg'
                            if (cols[2] < 8) {
                                nctx[fg]=ansicolors[cols[2] + (isfg ? 30 : 40)]//0x00-0x07 equiv 30-37
                            } else if (cols[2] < 15 && isfg) {
                                nctx['fg']=ansicolorsfg[cols[2] - 8 + 90]//0x08-0x0F equiv 90-97
                                //bg equiv?
                            } else if (cols[2] >= 0x10 && cols[2] <= 0xe7) {
                                //0x10-0xe7:  6*6*6=216 colors: 16 + 36*r + 6*g + b (0≤r,g,b≤5)
                                // or in english, r,g,b can be 0-5. encoded value is = 36*r + 6*g + b + 16
                                def val = (int) cols[2] - 0x10
                                def b = val % 6
                                val = (int) ((val - b) / 6)
                                def g = val % 6
                                val = (int) ((val - b) / 6)
                                def r = val % 6
                                nctx[fg]= "${fg}-rgb-${r}-${g}-${b}"
                            } else if (cols[2] >= 0xe8 && cols[2] <= 0xff) {
                                nctx[fg]= "${fg}-gray-${(cols[2] - 0xe8)}"
                            }
                            cols = cols.subList(3, cols.size())
                        }else if(ansicolors[cols[0]]){
                            if(ansicolorsfg[cols[0]]){
                                nctx.fg=ansicolorsfg[cols[0]]
                            }else if(ansicolorsbg[cols[0]]){
                                nctx.bg=ansicolorsbg[cols[0]]
                            }
                            cols = cols.subList(1,cols.size())
                        }else if(ansimode[cols[0]]){
                            if(cols[0]==0){
                                reset=true
                                nctx.clear()
                            }else{
                                if(!nctx.mode){
                                    nctx.mode=new HashSet<String>()
                                }
                                nctx.mode<<ansimode[cols[0]]
                            }
                            cols = cols.subList(1,cols.size())
                        }else{
                            break;
                        }
                    }
                    if(nctx.fg &&ctx.fg && (ctx.fg != nctx.fg) || nctx.bg && ctx.bg && (ctx.bg != nctx.bg)){
                        reset=true
                    }
                    if(reset) {
                        sb << '</span>'*cdepth
                        ctx=[:]
                        cdepth=0
                        reset=false
                    }
                    if(nctx.fg=='fg-default'){
                        nctx.remove('fg')
                    }
                    if(nctx.bg=='bg-default'){
                        nctx.remove('bg')
                    }
                    def apply=new HashMap()

                    if(nctx.fg){
                        apply.fg=nctx.fg
                    }
                    if(nctx.bg){
                        apply.bg=nctx.bg
                    }
                    if(nctx.mode){
                        apply.mode= new HashSet(nctx.mode)
                        apply.mode.removeAll(ctx.mode?:[])
                    }

                    if (apply) {
                        cdepth++
                        sb << '<span'
                        def css=[]
                        def style=[:]
                        if(apply.mode){
                            css.addAll(apply.mode)
                        }
                        if(apply.fg instanceof Collection){
                            style.fg=apply.fg
                        }else if(apply.fg){
                            css.add apply.fg
                        }
                        if (apply.bg instanceof Collection) {
                            style.bg = apply.bg
                        } else if (apply.bg) {
                            css.add apply.bg
                        }
                        if (css) {
                            sb << ' class="' + css.toSorted().collect { "ansi-${it}" }.join(' ') + '"'
                        }
                        if (style) {
                            sb << ' style="'
                            if (style.fg) {
                                sb << "color: rgb(${style.fg[0]},${style.fg[1]},${style.fg[2]});"
                            }
                            if (style.bg) {
                                sb << "background-color: rgb(${style.bg[0]},${style.bg[1]},${style.bg[2]});"
                            }
                            sb << '"'
                        }
                        sb << '>'
                        if(nctx.fg){
                            ctx.fg=nctx.fg
                        }else{
                            ctx.remove('fg')
                        }
                        if(nctx.bg){
                            ctx.bg=nctx.bg
                        }else{
                            ctx.remove('bg')
                        }
                        if(nctx.mode){
                            if(ctx.mode){
                                ctx.mode.addAll(nctx.mode)
                            }
                        }
                    }
                str = str.substring(len).encodeAsHTML()
            }
            sb << str.encodeAsHTML()
        }
        sb << '</span>'*cdepth
        sb.toString()
    }
}
