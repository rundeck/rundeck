package org.rundeck.app.providers

import grails.testing.gorm.DataTest
import org.rundeck.app.data.model.v1.AuthTokenMode
import org.rundeck.app.data.model.v1.AuthenticationToken
import org.rundeck.app.data.model.v1.SimpleTokenBuilder
import org.rundeck.app.data.providers.GormTokenDataProvider
import org.rundeck.spi.data.DataAccessException
import org.springframework.context.MessageSource
import rundeck.AuthToken
import rundeck.User
import rundeck.services.UserService
import rundeck.services.data.AuthTokenDataService
import spock.lang.Specification
import spock.lang.Unroll

import static org.rundeck.app.data.model.v1.AuthenticationToken.*

class GormTokenDataProviderSpec extends Specification implements DataTest{
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
            findOrCreateUser(_) >>  { new User(login: ownerName).save() }
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

    def "token lookup test"() {
        given:
        def user = new User(login: "admin")
        def tokenSec1   = "3y28pqenyp9p0834urn3p094urn30sdknf23onj8"
        def tokenSec2   = "a8oiy73xo7ybcf3l8cfnw3n58cyw4yn5cgf9w4y5"
        def tokenUnsec1 = "asdfdsfo87sdfn79ne734pw84ujrc4up9h3y93ho"
        def tokenUnsec2 = "d8up8y43p93ny2rp9dw8nyu3pt98mw4uyt89wn58"

        def newToken1 = new AuthToken(
                user: user,
                uuid: "sec1",
                authRoles: "admin",
                token: tokenSec1,
                tokenMode: AuthTokenMode.SECURED
        )
        def newToken2 = new AuthToken(
                user: user,
                uuid: "sec2",
                authRoles: "admin",
                token: tokenSec2,
                tokenMode: AuthTokenMode.SECURED
        )
        def newToken3 = new AuthToken(
                user: user,
                uuid: "unsec1",
                authRoles: "admin",
                token: tokenUnsec1,
                tokenMode: AuthTokenMode.LEGACY
        )
        def newToken4 = new AuthToken(
                user: user,
                uuid: "unsec2",
                authRoles: "admin",
                token: tokenUnsec2,
                tokenMode: AuthTokenMode.LEGACY
        )
        newToken1.save(flush: true, failOnError: true)
        newToken2.save(flush: true, failOnError: true)
        newToken3.save(flush: true, failOnError: true)
        newToken4.save(flush: true, failOnError: true)

        when:
        def st1 = provider.tokenLookup(tokenSec1);
        def st2 = provider.tokenLookup(tokenSec2);
        def ut1 = provider.tokenLookup(tokenUnsec1);
        def ut2 = provider.tokenLookup(tokenUnsec2);

        def hashLookup1 = provider.tokenLookup(tokenSec1.encodeAsSHA256())
        def hashLookup2 = provider.tokenLookup(tokenSec2.encodeAsSHA256())
        def clearLookup1 =AuthToken.findByToken(tokenSec1)
        def clearLookup2 =AuthToken.findByToken(tokenSec2)
        def clearLookup3 =AuthToken.findByToken(tokenUnsec1)
        def clearLookup4 =AuthToken.findByToken(tokenUnsec2)

        then:
        st1.uuid == "sec1"
        st2.uuid == "sec2"
        st1.token == tokenSec1.encodeAsSHA256()
        st2.token == tokenSec2.encodeAsSHA256()
        ut1.uuid == "unsec1"
        ut2.uuid == "unsec2"
        ut1.token == tokenUnsec1
        ut2.token == tokenUnsec2
        hashLookup1 == null
        hashLookup2 == null
        clearLookup1 == null
        clearLookup2 == null
        clearLookup3.uuid == "unsec1"
        clearLookup4.uuid == "unsec2"

    }

}
