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

import java.io.*;
import java.nio.file.Files;

public class S3Base implements AWSCredentials,ResourceModelSource, WriteableModelSource {
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


    private boolean writable=true;//TODO

    private Framework framework;
    private AmazonS3 amazonS3;

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

    public void setEndpoint(String endpoint){
        this.endpoint = endpoint;
        this.useEndpoint = true;
    }

    public void setS3ClientOptions(S3ClientOptions clientOptions){
        this.clientOptions = clientOptions;
        this.useClientOptions = true;
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

    public InputStream getFile() throws AmazonClientException {
        AmazonS3 amazonS3 = getAmazonS3();
        S3Object object;
        object = amazonS3.getObject(bucket,filePath);
        //remoteFile = object.getObjectContent();
        return object.getObjectContent();
    }

    public void putFile(File file){
        AmazonS3 amazonS3 = getAmazonS3();
        amazonS3.putObject(bucket,filePath,file);
    }


    public INodeSet getNodes() throws ResourceModelSourceException {
        InputStream remoteFile;
        try {
            remoteFile = getFile();
        }catch (AmazonClientException e){
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

    @Override
    public String getAWSAccessKeyId() {
        return AWSAccessKeyId;
    }

    @Override
    public String getAWSSecretKey() {
        return AWSSecretKey;
    }

    public void setAWSAccessKeyId(String keyId){
        AWSAccessKeyId = keyId;
    }

    public void setAWSSecretKey(String key){
        AWSSecretKey = key;
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
        return true;//TODO
    }

    @Override
    public long writeData(InputStream data) throws IOException, ResourceModelSourceException {
        if (!writable) {
            throw new IllegalArgumentException("Cannot write to file, it is not configured to be writeable");
        }
        try {
            final ResourceFormatParser parser = getResourceFormatParser();
            File temp = Files.createTempFile("temp", "." + parser.getPreferredFileExtension()).toFile();
            temp.deleteOnExit();

            try (FileOutputStream fos = new FileOutputStream(temp)) {
                Streams.copyStream(data, fos);
            }
            try {
                final INodeSet set = parser.parseDocument(temp);
            } catch (ResourceFormatParserException e) {
                throw new ResourceModelSourceException(e);
            }
            putFile(temp);
            return temp.length();
        }catch (UnsupportedFormatException e){
            throw new ResourceModelSourceException(
                    "Response content type is not supported: " + extension, e);
        }
    }

    private void displayTextInputStream(InputStream data)
            throws IOException {
        // Read one text line at a time and display.
        BufferedReader reader = new BufferedReader(new
                InputStreamReader(data));
        while (true) {
            String line = reader.readLine();
            if (line == null) break;

            System.out.println("    " + line);
        }
        System.out.println();
    }

}
