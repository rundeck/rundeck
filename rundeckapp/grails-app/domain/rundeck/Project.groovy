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

package rundeck

/**
 * Created by greg on 2/19/15.
 */
class Project {
    String name
    String description
    Date dateCreated
    Date lastUpdated

    static constraints={
        name(matches: '^[a-zA-Z0-9\\.,@\\(\\)_\\\\/-]+$',unique: true)
<<<<<<< HEAD
<<<<<<< HEAD
        description(nullable:true, matches: '^[a-zA-Z0-9\\s\\.,\\(\\)-]+$')
=======
        description(nullable:true, matches: '^[a-zA-Z0-9\\p{L}\\p{M}\\s\\.,\\(\\)-?_]+$')
=======
        description(nullable:true, matches: '^[a-zA-Z0-9\\p{L}\\p{M}\\s\\.,\\(\\)_-]+$')
>>>>>>> 66c172d226... correct underscore position in project description regex
    }

    static mapping = {
        cache: true
        DomainIndexHelper.generate(delegate) {
            index 'PROJECT_IDX_NAME', ['name']
        }
>>>>>>> 69438529c0... fix #5744 restore capability to incluide underscore character in project description
    }
}
