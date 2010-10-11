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
 * ColonyHttpClient.java
 * 
 * User: greg
 * Created: Jan 14, 2005 4:44:40 PM
 * $Id: CommanderHttpClient.java 5690 2006-01-14 20:22:27Z connary_scott $
 */
package com.dtolabs.client.utils;


/**
 * WebserviceHttpClient is the interface for making Colony requests to the Webservice application. Once an instance has
 * been obtained via a {@link WebserviceHttpClientFactory} instance, then the {@link #makeRequest()} method can be
 * called, and the response evaluated.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision: 5690 $
 */
public interface WebserviceHttpClient extends BaseHttpClient, WebserviceResponse {


}