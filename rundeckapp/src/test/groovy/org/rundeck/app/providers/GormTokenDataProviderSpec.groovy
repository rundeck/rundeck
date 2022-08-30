package org.rundeck.app.providers

import grails.testing.gorm.DataTest
import org.rundeck.app.data.model.v1.AuthenticationToken
import org.rundeck.app.data.model.v1.SimpleTokenBuilder
import org.rundeck.app.data.providers.GormTokenDataProvider
import org.rundeck.spi.data.DataAccessException
import org.springframework.context.MessageSource
import rundeck.AuthToken
import rundeck.User
import rundeck.services.UserService
import rundeck.services.data.AuthTokenDataService
import spock.lang.Unroll
import testhelper.RundeckHibernateSpec

import static org.rundeck.app.data.model.v1.AuthenticationToken.*

class GormTokenDataProviderSpec extends RundeckHibernateSpec implements DataTest{
    GormTokenDataProvider provider = new GormTokenDataProvider()

    void setup() {
        mockDomains(AuthToken, User)
        mockDataService(AuthTokenDataService)
        provider.authTokenDataService = applicationContext.getBean(AuthTokenDataService)

    }

    @Unroll
    def "Create and Retrieve"() {
        when:
        provider.userService = Mock(UserService){
            findOrCreateUser(_) >>  new User(login: ownerName)
        }
        SimpleTokenBuilder data =  new SimpleTokenBuilder()
                .setToken(token)
                .setOwnerName(ownerName)
                .setAuthRolesSet(AuthenticationToken.parseAuthRoles(roles))
                .setType(type)
                .setName(name)
                .setUuid(uuid)
        String createdUuid = provider.create(data)
        AuthenticationToken createdToken = provider.getData(uuid)

        then:
        createdUuid == uuid
        createdToken.getOwnerName() == ownerName
        createdToken.getAuthRolesSet() == AuthenticationToken.parseAuthRoles(roles)
        createdToken.type == type
        createdToken.name == name
        createdToken.uuid == createdUuid

        where:
        token       | ownerName  | roles     | type                   | name  | uuid
        "Token1"    | "user1"    | 'a,b'     | AuthTokenType.WEBHOOK  | null  | UUID.randomUUID().toString()
        "Token2"    | "user2"    | 'a,b'     | AuthTokenType.WEBHOOK  | 'abc' | UUID.randomUUID().toString()
        "Token3"    | "user3"    | 'd'       | AuthTokenType.WEBHOOK  | 'def' | UUID.randomUUID().toString()
    }

    def "should throw an error when create fails"() {
        when:
        provider.userService = Mock(UserService){
            findOrCreateUser(_) >>  new User(login: "auser")
        }
        provider.messageSource = Mock(MessageSource) {
            getMessage(_,_) >> null
        }
        SimpleTokenBuilder data =  new SimpleTokenBuilder()
                                       .setType(AuthTokenType.WEBHOOK)
                                       .setAuthRolesSet(AuthenticationToken.parseAuthRoles('a,b'))


        provider.create(data)

        then:
        DataAccessException e = thrown()

    }
    @Unroll
    def "Update and Retrieve"() {
        when:
        User bob = new User(login: 'bob')
        bob.save()
        provider.userService = Mock(UserService){
            findOrCreateUser(_) >>  bob
        }
        String uuid = '123uuid'
        AuthToken createdToken = new AuthToken(
                user: bob,
                type: AuthenticationToken.AuthTokenType.USER,
                token: 'abc',
                authRoles: 'g,f',
                uuid: uuid,
                creator: 'elf',
        )
        createdToken.save(flush: true);
        SimpleTokenBuilder data =  new SimpleTokenBuilder()
                .setToken(token)
                .setAuthRolesSet(AuthenticationToken.parseAuthRoles(roles))
                .setName(name)
        provider.update(uuid, data)
        AuthenticationToken updatedToken = provider.getData(uuid)

        then:
        updatedToken.getAuthRolesSet() == AuthenticationToken.parseAuthRoles(roles)
        updatedToken.name == name
        updatedToken.uuid == uuid

        where:
        token       | roles     | name
        "Token1"    | 'a,b'     | null
        "Token2"    | 'a,b'     | 'abc'
        "Token3"    | 'd'       | 'def'
    }

    def "should throw and error when updating fails"() {
        when:
        User bob = new User(login: 'bob')
        bob.save()
        provider.userService = Mock(UserService){
            findOrCreateUser(_) >>  bob
        }
        String uuid = '123uuid'
        AuthToken createdToken = new AuthToken(
                user: bob,
                type: AuthenticationToken.AuthTokenType.USER,
                token: 'abc',
                authRoles: 'g,f',
                uuid: uuid,
                creator: 'elf',
        )
        createdToken.save(flush: true);
        SimpleTokenBuilder data =  new SimpleTokenBuilder()
                                      .setName("name")
        provider.update("FAKE", data)

        then:
        DataAccessException e = thrown()

    }

    def "Delete"() {
        given:
        User bob = new User(login: 'bob')
        bob.save()

        String uuid = '123uuid'
        AuthToken createdToken = new AuthToken(
                user: bob,
                type: AuthenticationToken.AuthTokenType.USER,
                token: 'abc',
                authRoles: 'g,f',
                uuid: uuid,
                creator: 'elf',
        )
        createdToken.save(flush: true);
        when:
        def list = provider.list()
        then:
        list.size() == 1
        when:
        provider.delete(uuid)
        then:
        provider.list().size() == 0
    }

    def "should throw an exception when deleting an invalid token"() {
        given:
        User bob = new User(login: 'bob')
        bob.save()

        String uuid = '123uuid'
        AuthToken createdToken = new AuthToken(
                user: bob,
                type: AuthenticationToken.AuthTokenType.USER,
                token: 'abc',
                authRoles: 'g,f',
                uuid: uuid,
                creator: 'elf',
        )
        createdToken.save(flush: true);
        when:
        provider.delete("FAKE")
        then:
        DataAccessException e = thrown()
    }


}
