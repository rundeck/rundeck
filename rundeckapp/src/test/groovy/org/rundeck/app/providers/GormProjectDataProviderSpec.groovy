package org.rundeck.app.providers

import grails.testing.gorm.DataTest
import org.rundeck.app.data.model.v1.AuthTokenMode
import org.rundeck.app.data.model.v1.AuthenticationToken
import org.rundeck.app.data.model.v1.SimpleTokenBuilder
import org.rundeck.app.data.model.v1.project.RdProject
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

    @Unroll
    def "Should Find Project by name and state"() {

        when:
        SimpleProjectBuilder data =  new SimpleProjectBuilder()
                .setName(name)
                .setDescription(description)
                .setState(state)
        Long projectId = provider.create(data)
        def createProj = provider.findByNameAndState(name, qstate)

        then:
        if(found) {
            assert createProj != null
            assert projectId == createProj.getId()
            assert createProj.getName() == name
            assert createProj.getDescription() == description
        }
        else {
            assert createProj == null
        }

        where:
        name       | state                    | qstate                   | description        | found
        "Project1" | null                     | null                     | "project number 1" | true
        "Project2" | null                     | RdProject.State.ENABLED  | "project number 2" | true
        "Project3" | null                     | RdProject.State.DISABLED | "project number 3" | false
        "Project1" | RdProject.State.ENABLED  | null                     | "project number 1" | true
        "Project2" | RdProject.State.ENABLED  | RdProject.State.ENABLED  | "project number 2" | true
        "Project3" | RdProject.State.ENABLED  | RdProject.State.DISABLED | "project number 3" | false
        "Project1" | RdProject.State.DISABLED | null                     | "project number 1" | false
        "Project2" | RdProject.State.DISABLED | RdProject.State.ENABLED  | "project number 2" | false
        "Project3" | RdProject.State.DISABLED | RdProject.State.DISABLED | "project number 3" | true
        
    }

    @Unroll
    def "Should Find Project names by state"() {

        given:
        buildProjectSet()

        when:
        Collection<String> result = provider.getFrameworkProjectNamesByState(qstate)

        then:
        result.size() == resultSize
        result.containsAll(names)

        where:
        qstate                   | resultSize | names
        RdProject.State.ENABLED  | 5          | ["Project1", "Project2", "ProjectE1", "ProjectE2", "ProjectE3"]
        RdProject.State.DISABLED | 4          | ["ProjectD1", "ProjectD2", "ProjectD3", "ProjectD4"]

    }

    @Unroll
    def "Should Count Projects by state"() {

        given:
        buildProjectSet()

        when:
        int result = provider.countFrameworkProjectsByState(qstate)

        then:
        result == resultSize

        where:
        qstate                   | resultSize
        RdProject.State.ENABLED  | 5
        RdProject.State.DISABLED | 4

    }

    @Unroll
    def "get project description"() {

        given:
        buildProjectSet()

        when:
        String result = provider.getProjectDescription(name)

        then:
        result == description

        where:
        name        | description
        "Project2"  | "Project 2"
        "ProjectE1" | "Project E1"
        "ProjectD2" | "Project D2"

    }

    private void buildProjectSet() {
        new Project(
                name: "Project1",
                description: "Project 1",
                state: null
        ).save(flush:true)
        new Project(
                name: "Project2",
                description: "Project 2",
                state: null
        ).save(flush:true)
        new Project(
                name: "ProjectE1",
                description: "Project E1",
                state: RdProject.State.ENABLED
        ).save(flush:true)
        new Project(
                name: "ProjectE2",
                description: "Project E2",
                state: RdProject.State.ENABLED
        ).save(flush:true)
        new Project(
                name: "ProjectE3",
                description: "Project E3",
                state: RdProject.State.ENABLED
        ).save(flush:true)
        new Project(
                name: "ProjectD1",
                description: "Project D1",
                state: RdProject.State.DISABLED
        ).save(flush:true)
        new Project(
                name: "ProjectD2",
                description: "Project D2",
                state: RdProject.State.DISABLED
        ).save(flush:true)
        new Project(
                name: "ProjectD3",
                description: "Project D3",
                state: RdProject.State.DISABLED
        ).save(flush:true)
        new Project(
                name: "ProjectD4",
                description: "Project D4",
                state: RdProject.State.DISABLED
        ).save(flush:true)
    }
}
