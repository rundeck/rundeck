package com.dtolabs.rundeck.util

import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import org.codehaus.groovy.grails.web.converters.Converter

/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
 * ZipBuilder.groovy
 *
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 10/10/12 12:47 PM
 *
 */
/**
 * Simple builder mechanism for writing Zip contents, create with a ZipOutputStream or using static {@link ZipBuilder#toFile} or {@link ZipBuilder#toOutputStream}:
 * <pre>
 *     ZipBuilder.toFile("file.zip"){
 *         //use dir method to explicitly create a dir
 *         dir("next"){
 *             //or use dynamic method with a '/' at the end to create a dir
 *             'bottom/' {
 *                  //file method can take a string, File or closure
 *                 file("some.txt","string contents")
 *                 file("file.data",new File("file.data")) //write file contents
 *                 file("writer.txt"){ writer-> // closure is passed a writer
 *                     writer.write "text\n"
 *                     //writer also is the delegate
 *                     write "more text"
 *                 }
 *
 *                 //or use a file name directly to create a file
 *                 'my.txt'{
 *                     write("blah")
 *                 }
 *
 *                 // fileStream closure is passed an outputstream
 *                 fileStream("writer.txt"){ stream->
 *                     stream.write(byteArray)
 *                 }
 *             }
 *         }
 *         'sibling/'{
 *             'another.txt' "text content"
 *             'simple.txt' new File("localfile.txt")
 *             'again.txt'{out->
 *                 out<<"contents"
 *             }
 *         }
 *     }
 * </pre>
 */
class ZipBuilder {
    private ZipOutputStream output
    private paths=[]
    def debug=false
    public ZipBuilder(ZipOutputStream stream){
        this.output=stream
    }

    public static void copyStream(final InputStream inputStream, final OutputStream outputStream) throws IOException {
        final byte[] buffer = new byte[1024 * 4];
        int c;
        while (-1 != (c = inputStream.read(buffer))) {
            outputStream.write(buffer, 0, c);
        }
    }
    static toFile(String filename, Closure build){
        return toFile(new File(filename),build)
    }
    static toFile(File output, Closure build){
        output.withOutputStream {os->
            def zos=new ZipOutputStream(os)
            def zip = new ZipBuilder(zos)
            build.delegate=zip
            build.call()
            zos.close()
        }
    }
    static toOutputStream(OutputStream os, Closure build){
        def zos= new ZipOutputStream(os)
        def zip = new ZipBuilder(zos)
        build.delegate=zip
        build.call()
        zos.close()
    }
    static String toDirName(String name){
        return name.replaceAll('(.+?)/*$','$1/')
    }

    def ZipBuilder dir(String name,Closure call){
        def dirname=toDirName(name)
        def filename = filename(dirname)
        if(debug){
            System.err.println("dir: ${filename}")
        }
        ZipEntry dirEntry = new ZipEntry(filename)
        output.putNextEntry(dirEntry)
        paths.push(dirname)
        call.delegate = this
        call.call()
        paths.pop()
        output.closeEntry()
        this
    }
    //generate current level relative filename
    def String filename(String name){
        if(name.startsWith('/')){
            return name
        }
        return (paths + name).join('')
    }

    private privateFile(String name, Closure withOutput){
        def filename = filename(name)
        ZipEntry fileEntry = new ZipEntry(filename)
        if (debug) {
            System.err.println("file: ${filename}")
        }
        output.putNextEntry(fileEntry)
        //write output
        withOutput.delegate=this
        withOutput.call()
        output.closeEntry()
        return this
    }
    /**
     * Write string contents as a file to the output
     * @param name
     * @param source
     * @return
     */
    def ZipBuilder file(String name, String source) {
        file(name){ write source }
    }
    def ZipBuilder file(String name, File source){
        source.withInputStream {InputStream is->
            this.file name, is
        }
        this
    }
    def ZipBuilder file(String name, Converter source){
        privateFile(name){
            //write file to output
            def baos = new ByteArrayOutputStream(8192)
            source.render(new OutputStreamWriter(baos,"UTF-8"))
            output.write baos.toByteArray()
        }
    }
    def ZipBuilder file(String name, InputStream source){
        privateFile(name) {
            //write file to output
            copyStream source, output
        }
    }
    def ZipBuilder fileStream(String name, Closure withStream){
        privateFile(name){
            withStream.delegate=output
            if (withStream.maximumNumberOfParameters >= 1) {
                withStream.call output
            } else if (withStream.maximumNumberOfParameters == 0) {
                withStream.call()
            }
        }
    }
    def ZipBuilder file(String name, Closure withStream){
        privateFile(name){
            def writer= new OutputStreamWriter(output,"UTF-8")
            withStream.delegate=writer
            if(withStream.maximumNumberOfParameters>=1){
                withStream.call(writer)
            }else if(withStream.maximumNumberOfParameters==0){
                withStream.call()
            }
            writer.flush()
        }
    }
    def methodMissing(String name, args) {
        if(name.endsWith('/') && args.length==1 && args[0] instanceof Closure){
            return this.dir(name,args[0])
        } else if(name.contains('.') && args.length == 1 && (args[0] instanceof Closure || args[0] instanceof String || args[0] instanceof File || args[0] instanceof InputStream)){
            return this.file(name, args[0])
        } else {
            throw new MissingMethodException(name, this.class, args)
        }
    }
}
