/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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
package com.rundeck.plugins.aws;


import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.S3Object;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.resources.ResourceModelSource;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException;
import com.dtolabs.rundeck.core.resources.SourceType;
import com.dtolabs.rundeck.core.resources.WriteableModelSource;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParser;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserException;
import com.dtolabs.rundeck.core.resources.format.UnsupportedFormatException;
import com.dtolabs.utils.Streams;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.file.Files;

public class S3Base implements AWSCredentials,ResourceModelSource, WriteableModelSource {

    private static final Logger logger = Logger.getLogger(S3Base.class);

    private String AWSAccessKeyId;
    private String AWSSecretKey;
    private boolean useKey = false;
    private PropertiesCredentials cred;
    private boolean useFile = false;

    private S3ClientOptions clientOptions;
    private boolean useClientOptions = false;

    private String regionName;
    private boolean useRegion = false;
    private String endpoint;
    private boolean useEndpoint = false;

    private String bucket;
    private String filePath;
    private String extension;


    private boolean writable=false;

    private Framework framework;
    private AmazonS3 amazonS3;


    public void setAmazonS3(AmazonS3 amazonS3){
        this.amazonS3 = amazonS3;
    }

    public S3Base(String bucket, String filePath,
           String extension, Framework framework){
        this.bucket=bucket;
        this.filePath=filePath;
        this.extension=extension;
        this.framework=framework;
    }

    public void setCredentials(String AWSAccessKeyId, String AWSSecretKey){
        this.AWSAccessKeyId = AWSAccessKeyId;
        this.AWSSecretKey = AWSSecretKey;
        this.useKey = true;
    }

    public void setCredentials(PropertiesCredentials cred){
        this.cred = cred;
        this.useFile = true;
    }

    public void setRegion(String region){
        this.regionName = region;
        this.useRegion = true;
    }

    public void setWritable(){
        this.writable=true;
    }

    public void setEndpoint(String endpoint){
        this.endpoint = endpoint;
        this.useEndpoint = true;
    }

    public void setS3ClientOptions(S3ClientOptions clientOptions){
        this.clientOptions = clientOptions;
        this.useClientOptions = true;
    }

    @Override
    public INodeSet getNodes() throws ResourceModelSourceException {
        InputStream remoteFile;
        try {
            remoteFile = getFile();
        }catch (AmazonClientException e){//file doesn't exist
            throw new ResourceModelSourceException(
                    "Error requesting Resource Model Source from S3",e);
        }
        final ResourceFormatParser parser;
        try {
            parser = getResourceFormatParser();
        } catch (UnsupportedFormatException e) {
            throw new ResourceModelSourceException(
                    "Response content type is not supported: " + extension, e);
        }
        try {
            return parser.parseDocument(remoteFile);
        } catch (ResourceFormatParserException e) {
            throw new ResourceModelSourceException(
                    "Error requesting Resource Model Source from S3, "
                            + "Content could not be parsed: "+e.getMessage(),e);
        }

    }

    @Override
    public SourceType getSourceType() {
        return writable ? SourceType.READ_WRITE : SourceType.READ_ONLY;
    }

    @Override
    public WriteableModelSource getWriteable() {
        return writable ? this : null;
    }

    @Override
    public String getAWSAccessKeyId() {
        return AWSAccessKeyId;
    }

    @Override
    public String getAWSSecretKey() {
        return AWSSecretKey;
    }


    @Override
    public String getSyntaxMimeType() {
        try {
            return getResourceFormatParser().getPreferredMimeType();
        } catch (UnsupportedFormatException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getSourceDescription() {
        return "S3 bucket: "+bucket+", file:"+filePath;
    }

    @Override
    public long readData(OutputStream sink) throws IOException, ResourceModelSourceException {
        if (!hasData()) {
            return 0;
        }
        try (InputStream inputStream = getFile()) {
            return Streams.copyStream(inputStream, sink);
        }
    }

    @Override
    public boolean hasData() {
        try{
            getFile();
        }catch (Exception e){
            return false;
        }
        return true;
    }

    @Override
    public long writeData(InputStream data) throws IOException, ResourceModelSourceException {
        if (!writable) {
            throw new IllegalArgumentException("Cannot write to file, it is not configured to be writeable");
        }
        File temp = isToFile(data);
        try {
            final INodeSet set = getResourceFormatParser().parseDocument(temp);
        } catch (ResourceFormatParserException e) {
            throw new ResourceModelSourceException(e);
        }
        putFile(temp);
        return temp.length();
    }

    private AmazonS3 getAmazonS3(){
        if(null == amazonS3){
            if(useKey) {
                amazonS3 = new AmazonS3Client(this);
            }else if(useFile){
                amazonS3 = new AmazonS3Client(cred);
            }else{
                amazonS3 = new AmazonS3Client();
            }
            if(useRegion) {
                Region region = RegionUtils.getRegion(regionName);
                amazonS3.setRegion(region);
            }else if(useEndpoint){
                amazonS3.setEndpoint(endpoint);
            }

            if(useClientOptions){
                amazonS3.setS3ClientOptions(clientOptions);
            }
        }
        return amazonS3;
    }

    private InputStream getFile() throws AmazonClientException {
        AmazonS3 amazonS3 = getAmazonS3();
        S3Object object;
        logger.info("Reading Resource from S3. "+bucket+" "+filePath);
        object = amazonS3.getObject(bucket,filePath);
        String amazonContentType = object.getObjectMetadata().getContentType();
        //we can check the xml and json content type from amazon object data
        if(extension.equalsIgnoreCase("xml") || extension.equalsIgnoreCase("json")){
            if(!amazonContentType.equalsIgnoreCase(getMimeType())){
                logger.warn("S3Object content type isn't equals to input content type.");
                logger.warn(
                        "S3 Object content type: "+amazonContentType + " expected content type:"+getMimeType());
            }
        }
        return object.getObjectContent();
    }

    private void putFile(File file){
        AmazonS3 amazonS3 = getAmazonS3();
        logger.info("Writing Resource to S3. "+bucket+" "+filePath);
        amazonS3.putObject(bucket,filePath,file);
    }


    private String getMimeType(){
        if(extension.equalsIgnoreCase("yaml")){
            return "text/yaml";
        }
        if(extension.equalsIgnoreCase("json")){
            return "application/json";
        }
        return "application/xml";
    }

    private ResourceFormatParser getResourceFormatParser() throws UnsupportedFormatException {
        return framework.getResourceFormatParserService().getParserForMIMEType(getMimeType());
    }

    private File isToFile(InputStream is) throws IOException, ResourceModelSourceException {
        try {
            final ResourceFormatParser parser = getResourceFormatParser();
            File temp = Files.createTempFile("temp", "." + parser.getPreferredFileExtension()).toFile();
            temp.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(temp)) {
                Streams.copyStream(is, fos);
            }
            return temp;
        } catch (UnsupportedFormatException e) {
            throw new ResourceModelSourceException(
                    "Response content type is not supported: " + extension, e);
        }
    }

}
