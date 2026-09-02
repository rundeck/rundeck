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
 * Regression coverage for https://github.com/rundeck/rundeck/issues/10493:
 * the {@code rduser} table's {@code id} column is created via a plain
 * Liquibase {@code autoIncrement: "true"} column definition, the same
 * pattern used for {@code scheduled_execution.id} and other core tables.
 * Long-lived (repeatedly upgraded) Postgres installations may not have a
 * real DB-level IDENTITY/SERIAL sequence attached to that column, so
 * mapping {@code User}'s id with an explicit {@code generator: 'identity'}
 * strategy -- which requires the database itself to generate the value --
 * caused saving a brand new user to fail with a not-null constraint
 * violation on {@code id}. These tests exercise real GORM persistence
 * (not mocked via DataTest) so they actually go through Hibernate's id
 * generator resolution, unlike the existing unit specs for User.
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
