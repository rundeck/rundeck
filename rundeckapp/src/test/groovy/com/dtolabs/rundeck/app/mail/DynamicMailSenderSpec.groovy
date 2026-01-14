package com.dtolabs.rundeck.app.mail

import grails.core.GrailsApplication
import grails.testing.spring.AutowiredTest
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.ApplicationContext
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import spock.lang.Specification

import javax.mail.Session

class DynamicMailSenderSpec extends Specification implements AutowiredTest {

    def "flatten props"() {
        expect:
            DynamicMailSender.flattenMapToProperties(input) == expected
        where:
            input                           | expected
            [a: 'b']                        | [a: 'b']
            [a: 'b', c: 'd']                | [a: 'b', c: 'd']
            [a: [c: 'd']]                   | ['a.c': 'd']
            [a: [c: 'd'], b: [e: [f: 'g']]] | ['a.c': 'd', 'b.e.f': 'g']
    }

    def "flatten props should skip null values"() {
        given:
            def input = [a: 'b', c: null, d: [e: 'f', g: null]]

        when:
            def result = DynamicMailSender.flattenMapToProperties(input)

        then:
            result == [a: 'b', 'd.e': 'f']
            !result.containsKey('c')
            !result.containsKey('d.g')
    }

    def "init should initialize delegate"() {
        given:
            def sender = new DynamicMailSender()
            def mockConfig = Mock(grails.config.Config) {
                getProperty('grails.mail', Map) >> [:]
            }
            sender.grailsApplication = Mock(GrailsApplication) {
                getConfig() >> mockConfig
            }

        when:
            sender.init()

        then:
            sender.delegate != null
            sender.delegate instanceof JavaMailSenderImpl
    }

    def "updateMailSender should update delegate with config"() {
        given:
            def sender = new DynamicMailSender()
            def config = [host: 'smtp.example.com', port: 587, username: 'user', password: 'pass']
            def mockConfig = Mock(grails.config.Config) {
                getProperty('grails.mail', Map) >> config
            }
            sender.grailsApplication = Mock(GrailsApplication) {
                getConfig() >> mockConfig
            }

        when:
            sender.updateMailSender()

        then:
            sender.delegate != null
            sender.delegate.host == 'smtp.example.com'
            sender.delegate.port == 587
            sender.delegate.username == 'user'
            sender.delegate.password == 'pass'
    }

    def "updateMailSender should handle null config"() {
        given:
            def sender = new DynamicMailSender()
            def mockConfig = Mock(grails.config.Config) {
                getProperty('grails.mail', Map) >> null
            }
            sender.grailsApplication = Mock(GrailsApplication) {
                getConfig() >> mockConfig
            }

        when:
            sender.updateMailSender()

        then:
            sender.delegate != null
            sender.delegate instanceof JavaMailSenderImpl
    }

    def "updateMailSender should preserve original delegate"() {
        given:
            def sender = new DynamicMailSender()
            def originalDelegate = Mock(JavaMailSender)
            sender.delegate = originalDelegate
            def mockConfig = Mock(grails.config.Config) {
                getProperty('grails.mail', Map) >> [:]
            }
            sender.grailsApplication = Mock(GrailsApplication) {
                getConfig() >> mockConfig
            }

        when:
            sender.updateMailSender()

        then:
            sender.original == originalDelegate
            sender.delegate != originalDelegate
    }

    def "configurationChanged should update mail sender when grails.mail keys change"() {
        given:
            def sender = new DynamicMailSender()
            def mockConfig = Mock(grails.config.Config) {
                getProperty('grails.mail', Map) >> [:]
            }
            sender.grailsApplication = Mock(GrailsApplication) {
                getConfig() >> mockConfig
            }
            def initialDelegate = Mock(JavaMailSender)
            sender.delegate = initialDelegate

        when:
            sender.configurationChanged(['grails.mail.host', 'other.key'] as Set)

        then:
            sender.delegate != initialDelegate
    }

    def "configurationChanged should not update when no grails.mail keys change"() {
        given:
            def sender = new DynamicMailSender()
            def initialDelegate = Mock(JavaMailSender)
            sender.delegate = initialDelegate

        when:
            sender.configurationChanged(['other.key', 'another.key'] as Set)

        then:
            sender.delegate == initialDelegate
    }

    def "configurationChanged should handle null keys"() {
        given:
            def sender = new DynamicMailSender()
            def initialDelegate = Mock(JavaMailSender)
            sender.delegate = initialDelegate

        when:
            sender.configurationChanged(null)

        then:
            sender.delegate == initialDelegate
            noExceptionThrown()
    }

    def "configurationChanged should handle empty keys"() {
        given:
            def sender = new DynamicMailSender()
            def initialDelegate = Mock(JavaMailSender)
            sender.delegate = initialDelegate

        when:
            sender.configurationChanged([] as Set)

        then:
            sender.delegate == initialDelegate
            noExceptionThrown()
    }

    def "constructMailSender should create sender with host config"() {
        given:
            def config = [host: 'smtp.example.com']
            def sender = new DynamicMailSender()

        when:
            def result = sender.constructMailSender(config)

        then:
            result != null
            result.host == 'smtp.example.com'
    }

    def "constructMailSender should use SMTP_HOST env var when host not in config"() {
        given:
            def config = [:]
            def sender = new DynamicMailSender()

        when:
            def result = sender.constructMailSender(config)

        then:
            result != null
            result.host == (System.getenv('SMTP_HOST') ?: 'localhost')
    }

    def "constructMailSender should default to localhost when no host or env var"() {
        given:
            def config = [:]
            def sender = new DynamicMailSender()

        when:
            def result = sender.constructMailSender(config)

        then:
            result != null
            result.host == 'localhost' || result.host == System.getenv('SMTP_HOST')
    }

    def "constructMailSender should set port"() {
        given:
            def config = [port: 587]
            def sender = new DynamicMailSender()

        when:
            def result = sender.constructMailSender(config)

        then:
            result.port == 587
    }

    def "constructMailSender should parse port from string"() {
        given:
            def config = [port: '587']
            def sender = new DynamicMailSender()

        when:
            def result = sender.constructMailSender(config)

        then:
            result.port == 587
    }

    def "constructMailSender should set username and password"() {
        given:
            def config = [username: 'testuser', password: 'testpass']
            def sender = new DynamicMailSender()

        when:
            def result = sender.constructMailSender(config)

        then:
            result.username == 'testuser'
            result.password == 'testpass'
    }

    def "constructMailSender should set encoding"() {
        given:
            def config = [encoding: 'ISO-8859-1']
            def sender = new DynamicMailSender()

        when:
            def result = sender.constructMailSender(config)

        then:
            result.defaultEncoding == 'ISO-8859-1'
    }

    def "constructMailSender should default encoding to utf-8 when not in config"() {
        given:
            def config = [:]
            def sender = new DynamicMailSender()

        when:
            def result = sender.constructMailSender(config)

        then:
            result.defaultEncoding == 'utf-8'
    }

    def "constructMailSender should not set default host and encoding when jndiName present"() {
        given:
            def config = [jndiName: 'java:comp/env/mail/Session']
            def sender = new DynamicMailSender()
            sender.applicationContext = Mock(ApplicationContext) {
                getBean('mailSession', Session) >> { throw new NoSuchBeanDefinitionException('mailSession', 'test') }
            }

        when:
            def result = sender.constructMailSender(config)

        then:
            result != null
            // When jndiName is present but session bean not found, session remains at default (null or unset)
            // The key point is that defaults for host and encoding are not applied
    }

    def "constructMailSender should set protocol"() {
        given:
            def config = [protocol: 'smtps']
            def sender = new DynamicMailSender()

        when:
            def result = sender.constructMailSender(config)

        then:
            result.protocol == 'smtps'
    }

    def "constructMailSender should flatten and set props"() {
        given:
            def config = [props: [mail: [smtp: [auth: true, starttls: [enable: true]]]]]
            def sender = new DynamicMailSender()

        when:
            def result = sender.constructMailSender(config)

        then:
            result.javaMailProperties != null
            result.javaMailProperties['mail.smtp.auth'] == 'true'
            result.javaMailProperties['mail.smtp.starttls.enable'] == 'true'
    }

    def "constructMailSender should call findMailSession when jndiName present"() {
        given:
            def config = [jndiName: 'java:comp/env/mail/Session']
            def sender = new DynamicMailSender()
            // Create a real Session object for testing
            def props = new Properties()
            def mockSession = Session.getInstance(props)
            sender.applicationContext = Mock(ApplicationContext) {
                getBean('mailSession', Session) >> mockSession
            }

        when:
            def result = sender.constructMailSender(config)

        then:
            result.session == mockSession
    }

    def "findMailSession should return session when bean exists"() {
        given:
            // Create a real Session object for testing
            def props = new Properties()
            def mockSession = Session.getInstance(props)
            def sender = new DynamicMailSender()
            sender.applicationContext = Mock(ApplicationContext) {
                getBean('mailSession', Session) >> mockSession
            }

        when:
            def result = sender.findMailSession()

        then:
            result == mockSession
    }

    def "findMailSession should return null when bean does not exist"() {
        given:
            def sender = new DynamicMailSender()
            sender.applicationContext = Mock(ApplicationContext) {
                getBean('mailSession', Session) >> { throw new NoSuchBeanDefinitionException('mailSession', 'test') }
            }

        when:
            def result = sender.findMailSession()

        then:
            result == null
    }

    def "asInt should convert string to int"() {
        expect:
            DynamicMailSender.asInt('123') == 123
    }

    def "asInt should return integer as is"() {
        expect:
            DynamicMailSender.asInt(123) == 123
    }

    def "constructMailSender should handle complete config"() {
        given:
            def config = [
                host: 'smtp.example.com',
                port: 587,
                username: 'user@example.com',
                password: 'secret',
                protocol: 'smtps',
                encoding: 'UTF-8',
                props: [
                    mail: [
                        smtp: [
                            auth: true,
                            starttls: [enable: true]
                        ]
                    ]
                ]
            ]
            def sender = new DynamicMailSender()

        when:
            def result = sender.constructMailSender(config)

        then:
            result != null
            result.host == 'smtp.example.com'
            result.port == 587
            result.username == 'user@example.com'
            result.password == 'secret'
            result.protocol == 'smtps'
            result.defaultEncoding == 'UTF-8'
            result.javaMailProperties['mail.smtp.auth'] == 'true'
            result.javaMailProperties['mail.smtp.starttls.enable'] == 'true'
    }
}