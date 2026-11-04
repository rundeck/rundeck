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

import com.dtolabs.rundeck.app.support.DomainIndexHelper
import org.hibernate.LockMode
import org.rundeck.app.data.model.v1.project.RdProject

/**
 * Created by greg on 2/19/15.
 */
class Project implements RdProject {
    private static final int LOCK_WAIT_TIMEOUT_SECONDS = 5

    String name
    String description
    Date dateCreated
    Date lastUpdated
    State state

    static constraints={
        name(matches: '^[a-zA-Z0-9\\.,@\\(\\)_\\\\/-]+$',unique: true)
        description(nullable:true, maxSize: 255)
        state(nullable:true)
    }

    static mapping = {
        cache: true
        DomainIndexHelper.generate(delegate) {
            index 'PROJECT_IDX_NAME', ['name']
        }
    }

    /**
     * Acquire a pessimistic write lock with a database-appropriate bounded wait.
     */
    static Project lockWithTimeout(Serializable id) {
        withSession { session ->
            String databaseProductName
            session.doWork { connection ->
                databaseProductName = connection.metaData.databaseProductName
            }
            if (databaseProductName == 'Microsoft SQL Server') {
                return lockSqlServer(session, id)
            }
            if (databaseProductName == 'Oracle') {
                return lockOracle(session, id)
            }
            session.createQuery('from Project project where project.id = :id', Project)
                .setParameter('id', id)
                .setLockMode('project', LockMode.PESSIMISTIC_WRITE)
                .setTimeout(LOCK_WAIT_TIMEOUT_SECONDS)
                .uniqueResult()
        }
    }

    static Project lockSqlServer(def session, Serializable id) {
        session.createNativeQuery(
            'SELECT * FROM project WITH (UPDLOCK, ROWLOCK, NOWAIT) WHERE id = :id',
            Project
        )
            .setParameter('id', id)
            .uniqueResult()
    }

    static Project lockOracle(def session, Serializable id) {
        session.createNativeQuery(
            "SELECT * FROM project WHERE id = :id FOR UPDATE WAIT ${LOCK_WAIT_TIMEOUT_SECONDS}",
            Project
        )
            .setParameter('id', id)
            .uniqueResult()
    }
}
