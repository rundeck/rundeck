/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import grails.converters.JSON


/*
 * TestController.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: May 7, 2010 7:06:02 PM
 * $Id$
 */

public class TestController {
    def index = { redirect(controller:'menu',action:'list',params:params) }
    def testOptions = {
        if(params.timeout){
            Thread.currentThread().sleep(1000* params.long('timeout')) 
        }
        def data=["x value","y value","a value"]
        if(params.object){
            data=["x value for "+params.object,"y value for "+params.object]
        }
        if(params.extra){
            data<<params.extra
        }

        return render(data as JSON)

    }
    def testOptionsInvalid = {
        response.setHeader("Content-Type","application/json; charset=UTF-8")
        return render("['a','b','z]")

    }
    def testOptionsMapped = {
        final Map data = ["X Label": 'x value', "Y Label": 'y value', "A Label": 'a value']
        if(params.extra){
            data[params.extra]=params.extra
        }
        return render(data as JSON)

    }

}