package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.logging.OffsetIterator

/*
 * Copyright 2013 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/*
 * FSFileLineIterator.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 1/23/13 7:48 PM
 * 
 */
/**
 * Iterate the lines in a file, provide offset location of the current read position
 */
class FSFileLineIterator implements OffsetIterator<String>{
    private FileInputStream raf
    private InputStreamReader read
    private long offset
    private Queue<String> buffer = new ArrayDeque<String>()
    private String encoding
    private static final String lineSep=System.getProperty("line.separator")
    boolean closed=false
    public FSFileLineIterator(FileInputStream raf,String encoding){
        this.encoding=encoding
        this.raf=raf
        offset=raf.channel.position()
        if (encoding){
            read = new InputStreamReader(raf, encoding)
        }else{
            read = new InputStreamReader(raf)
        }
        readNext()
    }
    @Override
    boolean hasNext() {
        buffer.size()>0
    }

    @Override
    String next() {
        String s = buffer.remove()
        offset += (s.getBytes(encoding).length) + 1
        readNext()
        return s
    }
    private void readNext(){
        if(closed){
            throw new IllegalStateException("Stream is closed")
        }
        String v = read.readLine()
        if (null!=v){
            buffer<<v
        }
    }

    @Override
    long getOffset() {
        offset
    }

    @Override
    void remove() {
        throw new UnsupportedOperationException()
    }

    @Override
    void close() {
        read.close()
        closed=true
    }
}
