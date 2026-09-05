/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

/**
 * Integration tests for {@link User} persistence against a real database.
 *
 * Background for https://github.com/rundeck/rundeck/issues/10493: the
 * {@code rduser} table's {@code id} column is created via a plain Liquibase
 * {@code autoIncrement: "true"} column definition, the same pattern used for
 * {@code scheduled_execution.id} and other core tables. Long-lived
 * (repeatedly upgraded) Postgres installations may not have a real DB-level
 * IDENTITY/SERIAL sequence attached to that column, so mapping {@code User}'s
 * id with an explicit {@code generator: 'identity'} strategy -- which
 * requires the database itself to generate the value -- caused saving a
 * brand new user to fail with a not-null constraint violation on
 * {@code id}.
 *
 * Note on coverage: the test datasource here uses Hibernate
 * {@code ddl-auto: create-drop} (see {@code application-test.yml}), which
 * generates the schema fresh from the current mapping rather than from
 * Liquibase -- so it does not reproduce the specific legacy-schema mismatch
 * from #10493 (a schema that predates/lacks a real identity column). What
 * these tests verify is that real (non-DataTest-mocked) GORM persistence of
 * a new User works end-to-end and assigns distinct ids, exercising
 * Hibernate's actual id generator resolution instead of DataTest's mocked
 * persistence, which the existing unit specs for User use and which would
 * not have caught this class of bug either way. The specific regression --
 * reintroducing an explicit {@code identity} generator on User -- is guarded
 * against directly by {@code UserTests#"does not map id with an explicit
 * generator"}, which asserts on the mapping itself.
 */
@Integration
@Rollback
class UserIntegrationSpec extends Specification {

    def "saving a new User assigns an id without a database round-trip failure"() {
        given:
        def user = new User(login: "test-user-${UUID.randomUUID()}")

        when:
        def saved = user.save(flush: true, failOnError: true)

        then:
        saved != null
        saved.id != null
    }

    def "saving multiple new Users assigns distinct ids"() {
        given:
        def user1 = new User(login: "test-user-1-${UUID.randomUUID()}")
        def user2 = new User(login: "test-user-2-${UUID.randomUUID()}")

        when:
        user1.save(flush: true, failOnError: true)
        user2.save(flush: true, failOnError: true)

        then:
        user1.id != null
        user2.id != null
        user1.id != user2.id
    }
}
