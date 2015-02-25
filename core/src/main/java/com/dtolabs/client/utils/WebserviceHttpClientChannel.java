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
 * ColonyHttpClientChannel.java
 * 
 * User: greg
 * Created: Jan 14, 2005 11:57:17 AM
 * $Id: CommanderHttpClientChannel.java 8443 2008-07-18 02:03:59Z gschueler $
 */
package com.dtolabs.client.utils;


import com.dtolabs.rundeck.core.CoreException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;


/**
 * WebserviceHttpClientChannel is a {@link WebserviceHttpClient} implementation.  Used to
 * make api requests to Webservice via HTTP. Instances should be obtained via {@link WebserviceHttpClientFactory}.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 8443 $
 */
class WebserviceHttpClientChannel extends BaseHttpClientChannel implements WebserviceHttpClient {

    public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WebserviceHttpClientChannel.class);

    public static final String XML_CONTENT_TYPE = "text/xml";
    public static final String APPLICATION_XML_CONTENT_TYPE = "application/xml";
    public static final String HTTP_SECURE_PROTOCOL = "https";

    boolean errorResponse = false;
    boolean validResponse = false;
    boolean hasDoc = false;
    String responseMessage = null;
    private Document resultDoc = null;
    private File uploadFile = null;
    private Map<String,? extends Object> formData=null;
    private String fileparam = null;

    /**
     * Creates a new instance of ColonyHttpChannel.
     *
     * @param urlSpec       The base URL for the request
     * @param authenticator a ColonyHttpAuthenticator instance
     * @param query         a Map of query parameters to be added to the URL
     *
     * @throws com.dtolabs.rundeck.core.CoreException
     *          if the Bean cannot be correctly marshalled via the {@link com.networkgps.itnav.colony.MarshallFacility}.
     */
    public WebserviceHttpClientChannel(final String urlSpec,
                                      final HttpAuthenticator authenticator,
                                      final Map query)
        throws CoreException {
        super(urlSpec, authenticator, query);

    }

    /**
     * Creates a new instance of ColonyHttpChannel.
     *
     * @param urlSpec       The base URL for the request
     * @param authenticator a ColonyHttpAuthenticator instance
     * @param query         a Map of query parameters to be added to the URL
     * @param uploadFile    a file to upload as part of a multipart request
     * @param fileparam     parameter name for the uploaded file in the multipart request
     *
     * @throws com.dtolabs.rundeck.core.CoreException
     *          if the Bean cannot be correctly marshalled via the {@link com.networkgps.itnav.colony.MarshallFacility}.
     */
    public WebserviceHttpClientChannel(final String urlSpec,
                                      final HttpAuthenticator authenticator,
                                      final Map query,
                                      final File uploadFile,
                                      final String fileparam)
        throws CoreException {
        this(urlSpec, authenticator, query);
        setUploadFile(uploadFile);
        setFileparam(fileparam);
    }
    /**
     * Creates a new instance of ColonyHttpChannel.
     *
     * @param urlSpec       The base URL for the request
     * @param authenticator a ColonyHttpAuthenticator instance
     * @param query         a Map of query parameters to be added to the URL
     * @param uploadFile    a file to upload as part of a multipart request
     * @param fileparam     parameter name for the uploaded file in the multipart request
     *
     * @throws com.dtolabs.rundeck.core.CoreException
     *          if the Bean cannot be correctly marshalled via the {@link com.networkgps.itnav.colony.MarshallFacility}.
     */
    public WebserviceHttpClientChannel(final String urlSpec,
                                      final HttpAuthenticator authenticator,
                                      final Map query,
                                      final Map<String,? extends Object> formData)
        throws CoreException {
        this(urlSpec, authenticator, query);
        setFormData(formData);
    }

    /**
     * Creates a new instance of ColonyHttpChannel.
     *
     * @param urlSpec             The base URL for the request
     * @param authenticator       a ColonyHttpAuthenticator instance
     * @param query               a Map of query parameters to be added to the URL
     * @param destination         outputstream to write the response to
     * @param expectedContentType an expected content type
     *
     * @throws com.dtolabs.rundeck.core.CoreException
     *          if the Bean cannot be correctly marshalled via the {@link com.networkgps.itnav.colony.MarshallFacility}.
     */
    public WebserviceHttpClientChannel(final String urlSpec,
                                      final HttpAuthenticator authenticator,
                                      final Map query,
                                      final OutputStream destination,
                                      final String expectedContentType) {
        super(urlSpec, authenticator, query, destination, expectedContentType);

    }

    /**
     * Creates a new instance of ColonyHttpChannel.
     *
     * @param urlSpec             The base URL for the request
     * @param authenticator       a ColonyHttpAuthenticator instance
     * @param query               a Map of query parameters to be added to the URL
     * @param uploadFile          file to upload in a multipart request
     * @param fileparam           parameter name for the uploaded file in the multipart request
     * @param destination         outputstream to write the response to
     * @param expectedContentType an expected content type
     *
     * @throws com.dtolabs.rundeck.core.CoreException
     *          if the Bean cannot be correctly marshalled via the {@link com.networkgps.itnav.colony.MarshallFacility}.
     */
    public WebserviceHttpClientChannel(final String urlSpec,
                                      final HttpAuthenticator authenticator,
                                      final Map query,
                                      final File uploadFile,
                                      final String fileparam,
                                      final OutputStream destination,
                                      final String expectedContentType) {
        this(urlSpec, authenticator, query, destination, expectedContentType);
        setUploadFile(uploadFile);
        setFileparam(fileparam);
    }

    /**
     * Returns the RequestEntity for the Post method request.  If a file upload is included, it will return a
     * MultipartRequestEntity with the file as one part.  otherwise, null is returned.
     */
    protected RequestEntity getRequestEntity(final PostMethod method) {
        if (uploadFile != null) {
            logger.debug("attempting to upload file with colony request");
            if(null!=getFileparam() && getFileparam().contains("/")) {
                //upload directly as specified mime type
                try {
                    return new InputStreamRequestEntity(new FileInputStream(uploadFile), getFileparam());
                } catch (FileNotFoundException e) {
                    throw new CoreException(
                            "Could not upload file in request to server: " + uploadFile.getAbsolutePath(), e
                    );
                }
            }else {
                try {
                    final Part[] parts = new Part[]{
                            new FilePart(
                                    null != getFileparam() ? getFileparam() : "uploadFile",
                                    uploadFile.getName(),
                                    uploadFile
                            )
                    };
                    return new MultipartRequestEntity(parts, method.getParams());
                } catch (FileNotFoundException e) {
                    throw new CoreException(
                            "Could not upload file in request to server: " + uploadFile.getAbsolutePath(), e
                    );
                }
            }
        } else {
            return null;
        }
    }

    @Override
    protected NameValuePair[] getRequestBody(final PostMethod method) {
        if(null!=formData && formData.size()>0) {
            final ArrayList<NameValuePair> list = new ArrayList<NameValuePair>();
            for (final Map.Entry<String, ? extends Object> stringStringEntry : formData.entrySet()) {
                final Object value = stringStringEntry.getValue();
                if(value instanceof String){
                    list.add(new NameValuePair(stringStringEntry.getKey(), (String) value));
                }else if(value instanceof Collection) {
                    Collection values = (Collection) value;
                    for (final Object o : values) {
                        list.add(new NameValuePair(stringStringEntry.getKey(), o.toString()));
                    }
                }
            }
            return list.toArray(new NameValuePair[formData.size()]);
        }else{
            return null;
        }
    }

    protected boolean isPostMethod() {
        return null != uploadFile || null!=formData && formData.size()>0;
    }

    /**
     * return a response message
     */
    public String getResponseMessage() {
        return responseMessage;
    }

    /**
     * true if the response was an error message
     */
    public boolean isErrorResponse() {
        return errorResponse;
    }

    /**
     * return true if the resultbean was present
     */
    public boolean hasResultDoc() {
        if (null == getResultDoc()) {
            hasDoc = false;
        }
        return hasDoc;
    }

    /**
     * return a result bean if it was unmarshalled successfully. return null otherwise or if none was received.
     */
    public Document getResultDoc() {
        return resultDoc;
    }

    /**
     * is the response valid
     */
    public boolean isValidResponse() {
        return validResponse;
    }


    /**
     * called by makeRequest.  Evaluates the response to determine the state of the response and deserialize the content
     * (bean).
     */
    protected void postMakeRequest() {

        responseMessage = getResponseHeader(Constants.X_RUNDECK_RESULT_HEADER) != null ?
                          getResponseHeader(Constants.X_RUNDECK_RESULT_HEADER).getValue() : getReasonCode();
        validResponse = getResultCode() >= 200 && getResultCode() < 300;
        errorResponse = !validResponse;
        String type = getResultContentType();
        if (type!=null && type.indexOf(";") > 0) {
            type = type.substring(0, type.indexOf(";")).trim();
        }
        if (XML_CONTENT_TYPE.equals(type) || APPLICATION_XML_CONTENT_TYPE.equals(type)) {
            final SAXReader reader = new SAXReader();
            final Document document;
            try {
                document = reader.read(getResultStream());
                setResultDoc(document);
            } catch (DocumentException e) {
                logger.error("Unable to parse result document: " + e.getMessage(), e);
            }
        }
    }

    private void setUploadFile(final File uploadFile) {
        this.uploadFile = uploadFile;
        if (null != uploadFile) {
            logger.debug("uploading file: " + uploadFile.getAbsolutePath());
        }
    }

    public String getFileparam() {
        return fileparam;
    }

    public void setFileparam(final String fileparam) {
        this.fileparam = fileparam;
    }

    private void setResultDoc(final Document resultDoc) {
        this.resultDoc = resultDoc;
    }

    public Map<String, ? extends Object> getFormData() {
        return formData;
    }

    public void setFormData(Map<String, ? extends Object> formData) {
        this.formData = formData;
    }
}