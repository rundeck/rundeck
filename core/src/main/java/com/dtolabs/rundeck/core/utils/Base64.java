/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 * Base64.java
 *
 * Created on November 13, 2003, 3:56 PM
 * $Id: Base64.java 1079 2008-02-05 04:53:32Z ahonor $
 */

package com.dtolabs.rundeck.core.utils;

/**
 * MIME Base64 encoder.
 *
 * @author greg
 */
public final class Base64 {
    private static final boolean debug = false;
    static final byte[] X = new byte[]{
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/', '='
    };

    private Base64() {
    }

    /**
     * @return true if the string is base64 encoded data.  return false if it
     * is not.
     * @param t string
     */
    public static final boolean isBase64(String t) {
        return isBase64(t.getBytes());
    }

    /**
     * @return  true if the string is base64 encoded data.  return false if it
     * is not.
     * @param b bytes
     */
    public static final boolean isBase64(byte[] b) {
        if (b.length < 4)
            return false;
        else if (b.length == 0)
            return true;
        String t = new String(b);
        int end = 0;
        if (t.endsWith("=="))
            end = 2;
        else if (t.endsWith("="))
            end = 1;
        int vc = 0;
        for (int i = 0; i < b.length - end; i++) {
            if (b[i] == (byte) '\r' || b[i] == (byte) '\n' || b[i] == (byte) ' ')
                continue;
            boolean ok = false;
            for (int j = 0; j < X.length - 1; j++)
                if (X[j] == b[i]) {
                    ok = true;
                    break;
                }
            if (!ok)
                return false;
            vc++;
        }

        if ((vc + end) % 4 != 0)
            return false;
        return true;
    }
    
//    private static final void debug(String txt){
//        if(debug)
//            System.err.println(txt);
//    }
    
    /**
     * @return  encoded string
     * @param in string
     */
    public static final String encode(String in) {
        return new String(encode(in.getBytes()));
    }

    private static final String hex(byte in) {
        return Long.toHexString(in & 0x00FFL);
    }

    /**
     * @return  encoded byte[]
     * @param in bytes
     */
    public static final byte[] encode(byte[] in) {
        int pad = 0 == in.length % 3 ? 0 : (3 - (in.length % 3));
        int padded = in.length + pad;
        byte[] sa = new byte[padded];
        java.util.Arrays.fill(sa, (byte) 0);
        System.arraycopy(in, 0, sa, 0, in.length);

        byte[] ou = new byte[(padded / 3) * 4];

        int dest = 0;
        int mx = 0;
        for (int i = 0; i < sa.length; i += 3, mx = 0) {
//            debug("bytes: "+hex(sa[i])+" "+hex(sa[i+1])+" "+hex(sa[i+2])  );
        
            ou[dest++] = X[
                    ((sa[i] & 0xFC) >> 2)
                    ];
//            debug((i+0)+": val: "+ ( ( sa[i] & 0xFC ) >> 2 ) + ", char: "+(char)ou[dest-1]);
            ou[dest++] = X[
                    ((sa[i] & 0x03) << 4) | ((sa[i + 1] & 0x00F0) >> 4)

                    ];
//            debug((i+0)+","+(i+1)+": val: "+
//                ( ( ( sa[i] & 0x03 ) << 4 ) | ( ( sa[i+1] & 0x00F0 ) >> 4 ) )
//                + ", char: "+(char)ou[dest-1]);
            
            if (2 == pad && i + 3 >= sa.length) {
//                debug("end: 2 pad");
                ou[dest++] = X[64];
                ou[dest++] = X[64];
                break;
            }


            ou[dest++] = X[
                    ((sa[i + 1] & 0x0F) << 2) | ((sa[i + 2] & 0x00C0) >> 6)
                    ];
            
//            debug((i+1)+","+(i+2)+": val: "+
//                ( ( ( sa[i+1] & 0x0F ) >> 2 ) | ( ( sa[i+2] & 0x00C0 ) >> 6 ) )
//                + ", char: "+(char)ou[dest-1]);

            if (1 == pad && i + 3 >= sa.length) {
//                debug("end: 1 pad");
                ou[dest++] = X[64];
                break;
            }

            ou[dest++] = X[
                    ((sa[i + 2] & 0x3F))
                    ];
            
            
//            debug((i+2)+": val: "+
//                ( ( sa[i+2] & 0x3F ) )
//                + ", char: "+(char)ou[dest-1]);

        }
        return ou;

    }

    /**
     * @return  decoded data
     * @param in string
     */
    public static final byte[] decode(String in) {
        return decode(in.getBytes());
    }

    /**
     * @return decoded byte[] data.
     * @param in bytes
     */
    public static final byte[] decode(byte[] in) {
        if (in.length == 0)
            return new byte[]{};
        else if (!isBase64(in))
            return null;
        int olen = (in.length / 4) * 3;
        int pad = 0;
        if (in[in.length - 1] == X[64]) {
            pad++;
            if (in[in.length - 2] == X[64])
                pad++;
        }
        olen -= pad;
        byte[] ou = new byte[olen];
        int dest = 0;
        int mx = 0;
        byte[] ck = ndx(in);
        for (int i = 0; i < ck.length; i += 4, mx = 0) {

            ou[dest++] = (byte) ((
                    ((ck[i] & 0x3F) << 2) | ((ck[i + 1] & 0x30) >> 4)
                    ) & 0xFF);

            if (pad == 2 && i + 4 >= ck.length)
                break;

            ou[dest++] = (byte) ((
                    ((ck[i + 1] & 0x0F) << 4) | ((ck[i + 2] & 0x3C) >> 2)
                    ) & 0xFF);

            if (pad == 1 && i + 4 >= ck.length)
                break;

            ou[dest++] = (byte) ((
                    ((ck[i + 2] & 0x03) << 6) | ((ck[i + 3] & 0x3F))
                    ) & 0xFF);
        }

        return ou;
    }

    private static final byte ndx(byte w) {
        for (int i = 0; i < X.length - 1; i++)
            if (X[i] == w)
                return (byte) i;
        return (byte) -1;
    }

    private static final byte[] ndx(byte[] w) {
        byte[] o = new byte[w.length];
        for (int i = 0; i < w.length; i++)
            o[i] = ndx(w[i]);
        return o;
    }

}
