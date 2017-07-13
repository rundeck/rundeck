dataSource {
    pooled = false; //it is recommended not to use connection pool unless file encryption is enabled
    driverClassName = "org.h2.Driver"
    username = "sa"
    password = ""
}
hibernate {
    cache.use_second_level_cache=true
    cache.use_query_cache=true
    cache.provider_class='net.sf.ehcache.hibernate.EhCacheProvider'
}
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

// environment specific settings
environments {
    development {
        dataSource {
            url = "jdbc:h2:./db/devDb"
            dbCreate = "none"
        }
        grails.plugin.databasemigration.updateOnStart = true
        grails.plugin.databasemigration.updateOnStartFileNames = ['changelog.groovy']
        // Required so that migration is run for grails integration tests.
        grails.plugin.databasemigration.forceAutoMigrate = true
    }
    test {
        dataSource {
            url = "jdbc:h2:./db/testDb"
            dbCreate = "none"
        }
        grails.plugin.databasemigration.updateOnStart = true
        grails.plugin.databasemigration.updateOnStartFileNames = ['changelog.groovy']
        // Required so that migration is run for grails integration tests.
        grails.plugin.databasemigration.forceAutoMigrate = true

    }
    production {
        dataSource {
            url = "jdbc:h2:./rundeck/grailsh2"
            dbCreate = "none"
        }
        grails.plugin.databasemigration.updateOnStart = true
        grails.plugin.databasemigration.updateOnStartFileNames = ['changelog.groovy']
    }
}
