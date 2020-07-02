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
    static def decode = { string ->
        def ctx = 0
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

                    def ncols=[]
                    def tcols=[:]
                    while(cols.size()>0) {
                        if(cols.size()>=5 && (cols[0]==38 || cols[0] == 48 ) && cols[1]==2){
                            //24 bit color
                            // https://en.wikipedia.org/wiki/ANSI_escape_code#Colors
                            def fg= cols[0]== 38 ? 'fg' : 'bg'
                            def rgb=[r:cols[2],g:cols[3],b:cols[4]]
                            if (rgb.values().any{it <0 || it>255}) {
                                break
                            }
                            tcols[fg]=rgb
                            cols = cols.subList(5, cols.size())
                        }else if (cols.size() >= 3 && (cols[0] == 38 || cols[0] == 48) && cols[1] == 5) {
                            //256 col ansi
                            // https://en.wikipedia.org/wiki/ANSI_escape_code#Colors
                            def isfg = cols[0] == 38
                            def fg = isfg ? 'fg' : 'bg'
                            if (cols[2] < 8) {
                                ncols << (cols[2] + (isfg ? 30 : 40))//0x00-0x07 equiv 30-37
                            } else if (cols[2] < 15 && isfg) {
                                ncols << (cols[2] - 8 + 90)//0x08-0x0F equiv 90-97
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
                                ncols << "${fg}-rgb-${r}-${g}-${b}"
                            } else if (cols[2] >= 0xe8 && cols[2] <= 0xff) {
                                ncols << "${fg}-gray-${(cols[2] - 0xe8)}"
                            }
                            cols = cols.subList(3, cols.size())
                        }else if(ansicolors[cols[0]]){
                            ncols<<ansicolors[cols[0]]
                            cols = cols.subList(1,cols.size())
                        }else if(ansimode[cols[0]]){
                            if(cols[0]==0){
                                reset=true
                            }else{
                                ncols<<ansimode[cols[0]]
                            }
                            cols = cols.subList(1,cols.size())
                        }else{
                            break;
                        }
                    }
                    //ctx
                    if (ncols || tcols) {
                        ctx++
                    }


                    if(reset) {
                        sb << ('' + (ctx ? ('</span>' * ctx) : ''))
                        ctx = 0
                        reset=false
                    }else {
                        sb << ((ncols || tcols) ? '<span' : '')
                        if (ncols) {
                            sb << ' class="' + ncols.collect { "ansi-${it}" }.join(' ') + '"'
                        }
                        if (tcols) {
                            sb << ' style="'
                            if (tcols.fg) {
                                sb << "color: rgb(${tcols.fg.r},${tcols.fg.g},${tcols.fg.b});"
                            }
                            if (tcols.bg) {
                                sb << "background-color: rgb(${tcols.bg.r},${tcols.bg.g},${tcols.bg.b});"
                            }
                            sb << '"'
                        }
                        sb << (ncols || tcols ? '>' : '')
                }
                str = str.substring(len).encodeAsHTML()
            }
            sb << str.encodeAsHTML()
        }
        sb << ('' + (ctx ? ('</span>'*ctx) : ''))
        sb.toString()
    }
}
