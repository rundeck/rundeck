package org.rundeck.app.providers

import grails.testing.gorm.DataTest
import org.rundeck.app.data.model.v1.AuthTokenMode
import org.rundeck.app.data.model.v1.AuthenticationToken
import org.rundeck.app.data.model.v1.SimpleTokenBuilder
import org.rundeck.app.data.model.v1.project.SimpleProjectBuilder
import org.rundeck.app.data.providers.GormProjectDataProvider
import org.rundeck.app.data.providers.GormTokenDataProvider
import org.rundeck.spi.data.DataAccessException
import org.springframework.context.MessageSource
import rundeck.AuthToken
import rundeck.Project
import rundeck.User
import rundeck.services.UserService
import rundeck.services.data.AuthTokenDataService
import rundeck.services.data.ProjectDataService
import spock.lang.Specification
import spock.lang.Unroll

import static org.rundeck.app.data.model.v1.AuthenticationToken.AuthTokenType

class GormProjectDataProviderSpec extends Specification implements DataTest{
    GormProjectDataProvider provider = new GormProjectDataProvider()

    void setup() {
        mockDomains(Project)
        mockDataService(ProjectDataService)
        provider.projectDataService = applicationContext.getBean(ProjectDataService)

    }

    @Unroll
    def "Create and Retrieve"() {

        when:
        SimpleProjectBuilder data =  new SimpleProjectBuilder()
                .setName(name)
                .setDescription(description)
        Long projectId = provider.create(data)
        def createProj = provider.getData(projectId)

        then:
        projectId == createProj.getId()
        createProj.getName() == name
        createProj.getDescription() == description


        where:
        name          | description
        "Project1"    | "project number 1"
        "Project2"    | "project number 2"
        "Project3"    | "project number 3"
    }

    @Unroll
    def "should throw an error when create fails"() {

        when:
        Project p = new Project(name: "Project1", description: "Project 1").save(flush:true)
        SimpleProjectBuilder data =  new SimpleProjectBuilder()
                .setName(name)
                .setDescription(description)

         provider.create(data)

        then:
        DataAccessException e = thrown()

        where:
        name        | description
        "Project1"  | "duplicate"
        null        | "no name"
        "!Project3" | "invalid char"

    }

    @Unroll
    def "Update and Retrieve"() {
        when:
        Project p = new Project(name: "Project1", description: "Project 1").save(flush:true)
        SimpleProjectBuilder data =  new SimpleProjectBuilder()
                .setName("Project1")
                .setDescription(description)

        provider.update(p.getId(), data)
        def updatedProject = provider.getData(p.getId())

        then:
        updatedProject.description == result

        where:
        description | result
        null        |  null
        "abc"       |  "abc"
        "def"       |  "def"
    }


    def "should throw and error when updating fails"() {
        when:
        SimpleProjectBuilder data =  new SimpleProjectBuilder()
                .setName("Project1")
                .setDescription("P1")

        provider.update("FAKE", data)


        then:
        DataAccessException e = thrown()

    }

    def "Delete"() {
        given:
        Project p = new Project(name: "Project1", description: "Project 1").save(flush:true)

        when:
        def count = provider.countFrameworkProjects()
        then:
        count == 1
        when:
        provider.delete("Project1")
        then:
        provider.countFrameworkProjects() == 0
    }

    def "should throw an exception when deleting an invalid project"() {
        given:
        Project p = new Project(name: "Project1", description: "Project 1").save(flush:true)

        when:
        provider.delete("FAKE")
        then:
        DataAccessException e = thrown()
    }

    @Unroll
    def "Should Find Project by name"() {

        when:
        SimpleProjectBuilder data =  new SimpleProjectBuilder()
                .setName(name)
                .setDescription(description)
        Long projectId = provider.create(data)
        def createProj = provider.findByName(name)

        then:
        projectId == createProj.getId()
        createProj.getName() == name
        createProj.getDescription() == description


        where:
        name          | description
        "Project1"    | "project number 1"
        "Project2"    | "project number 2"
        "Project3"    | "project number 3"
    }
}
