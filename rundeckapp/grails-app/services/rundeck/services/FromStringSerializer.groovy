/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package rundeck.services

import com.dtolabs.rundeck.plugins.scm.JobSerializer

/**
 * Created by greg on 4/28/15.
 */
class FromStringSerializer implements JobSerializer {
    Map<String,String> data

    FromStringSerializer(final Map<String, String> data) {
        this.data = data
    }

    @Override
    void serialize(final String format, final OutputStream outputStream) {
        def s = data[format]
        if(s==null){
            throw new IllegalArgumentException("Format not supported: " + format)
        }
        outputStream.write(s.getBytes("UTF-8"))
    }
}
