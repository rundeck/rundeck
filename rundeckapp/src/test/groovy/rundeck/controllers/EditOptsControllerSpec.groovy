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

package rundeck.controllers

import grails.testing.gorm.DataTest
import grails.artefact.Artefact
import grails.test.hibernate.HibernateSpec
import grails.testing.web.controllers.ControllerUnitTest
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.data.providers.GormUserDataProvider
import org.rundeck.core.auth.AuthConstants
import rundeck.*
import rundeck.codecs.URIComponentCodec
import rundeck.services.ConfigurationService
import rundeck.services.FileUploadService
import rundeck.services.FrameworkService
import rundeck.services.optionvalues.OptionValuesService
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 2/11/16.
 */
class EditOptsControllerSpec extends Specification implements ControllerUnitTest<EditOptsController>, DataTest {
    GormUserDataProvider provider = new GormUserDataProvider()

    def setupSpec() { mockDomains Option, ScheduledExecution, CommandExec, Workflow }

    def setup() {
        mockCodec(URIComponentCodec)
//        mockCodec(URLCodec)

        grailsApplication.config.clear()
        grailsApplication.config.rundeck.security.useHMacRequestTokens = 'false'

        defineBeans {
            configurationService(ConfigurationService) {
                grailsApplication = grailsApplication
            }
        }
    }

    @Unroll
    def "action #action requires form token"() {
        when:
            def utilTagLib = mockTagLib(UtilityTagLib)
            request.method = 'POST'
            controller."$action"()
        then:
            response.status == 400
            request.error == 'request.error.invalidtoken.message'
        where:
            action    | _
            'save'    | _
            'remove'  | _
            'reorder' | _
            'undo'    | _
            'redo'    | _
            'revert'  | _
            'duplicate'  | _
    }

    @Unroll
    def "action #action requires name param"() {
        when:
            controller."$action"()
        then:
            flash.error == 'name parameter required'
        where:
            action << ['edit', 'renderOpt']
    }

    @Unroll
    def "actino #action name param must be available"() {
        when:
            params.name = 'dne'
            controller."$action"()
        then:
            flash.error == 'no option with name dne found'
        where:
            action << ['edit', 'renderOpt']
    }

    protected setupFormTokens(session) {
        def token = SynchronizerTokensHolder.store(session)
        params[SynchronizerTokensHolder.TOKEN_KEY] = token.generateToken('/test')
        params[SynchronizerTokensHolder.TOKEN_URI] = '/test'
    }

    @Unroll
    def "action #action requires name or newoption param"() {
        when:
            request.method = 'POST'
            setupFormTokens(session)
            def assetTaglib = mockTagLib(FakeTagLib)
            controller."$action"()
        then:
            flash.error == 'name parameter is required'
        where:
            action << ['save', 'remove', 'reorder']
    }

    @Unroll
    def "action reorder requires certain opts"() {
        when:
            request.method = 'POST'
            params.name = 'test'
            setupFormTokens(session)
            controller.reorder()
        then:
            flash.error == 'relativePosition, last, or before parameter is required'
    }

    protected ScheduledExecution createJob(Map overrides = [:]) {
        def map = [
                      jobName       : 'blue',
                      project       : 'AProject',
                      groupPath     : 'some/where',
                      description   : 'a job',
                      argString     : '-a b -c d',
                      workflow      : new Workflow(
                          keepgoing: true,
                          commands: [new CommandExec([adhocRemoteString: 'test buddy'])]
                      ),
                      serverNodeUUID: null,
                      scheduled     : true
                  ] + overrides
        new ScheduledExecution(map).save(flush: true)
    }

    @Unroll
    def "error response for invalid #action"() {
        given:
            ScheduledExecution job = createJob()
            session[sessionDataVar] = [
                (job.id.toString()): [
                    [action: 'remove', name: 'test']
                ]
            ]
            params.scheduledExecutionId = job.id.toString()
            setupFormTokens(session)
            request.method = 'POST'
            controller.frameworkService = Mock(FrameworkService)


            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
        when:
            controller."$action"()
        then:
            flash.error == 'No option named test exists'
            response.status == 400
            1 * controller.rundeckAuthContextProcessor.authorizeProjectJobAll(_, _, [AuthConstants.ACTION_UPDATE], job.project) >> true

        where:
            action | sessionDataVar
            'redo' | 'redoOPTS'
            'undo' | 'undoOPTS'

    }
    def "validate opt required scheduled job with default storage path"() {
        given:
        Option opt = new Option(required: true, defaultValue: defval, defaultStoragePath: defstorageval, enforced: false)

        when:
        EditOptsController._validateOption(opt, provider, params, true)
        then:
        iserr == opt.errors.hasFieldErrors('defaultValue')

        where:
        iserr | defval | defstorageval
        true  | null   | null
        false | 'abc'  | null
        false | null   | 'abc'

    }

    def "validate file opt required with scheduled job"() {
        given:
        Option opt = new Option(required: true, optionType: 'file', name: 'abc',enforced: false)

        when:
        EditOptsController._validateOption(opt, provider, params, isched)
        then:
        iserr == opt.errors.hasFieldErrors('required')

        where:
        iserr | isched
        true  | true
        false | false
    }

    def "reorder option relative position"() {
        given:
        Option opt1 = new Option(name: 'abc')
        Option opt2 = new Option(name: 'def')
        Option opt3 = new Option(name: 'ghi')
        def opts = [opt1, opt2, opt3]
        def editopts = opts.collectEntries { [it.name, it] }

        when:
        def output = controller._applyOptionAction(
                editopts,
                [action: 'reorder', name: optA, params: [relativePosition: rel]]
        )

        then:
        editopts == [abc: opt1, def: opt2, ghi: opt3]
        output == [undo: [action: 'reorder', name: optA, params: [relativePosition: rel * -1]]]
        result == new TreeSet(opts)*.name

        where:
        optA | rel | result
        'abc' | 1   | ['def', 'abc', 'ghi']
        'abc' | 2   | ['def', 'ghi', 'abc']
        'def' | -1  | ['def', 'abc', 'ghi']
        'ghi' | -2  | ['ghi', 'abc', 'def']
    }

    def "reorder option before"() {
        given:
        Option opt1 = new Option(name: 'abc')
        Option opt2 = new Option(name: 'def')
        Option opt3 = new Option(name: 'ghi')
        def opts = [opt1, opt2, opt3]
        def editopts = opts.collectEntries { [it.name, it] }

        when:
        def output = controller._applyOptionAction(
                editopts,
                [action: 'reorder', name: optB, params: [before: otherOpt]]
        )

        then:
        editopts == [abc: opt1, def: opt2, ghi: opt3]
        output == [undo: [action: 'reorder', name: optB, params: [relativePosition: undoPos]]]
        result == new TreeSet(opts)*.name

        where:
        optB | otherOpt | undoPos | result
        'abc' | 'ghi'    | -1      | ['def', 'abc', 'ghi']
        'abc' | 'def'    | 0       | ['abc', 'def', 'ghi']
        'ghi' | 'abc'    | 2       | ['ghi', 'abc', 'def']
        'ghi' | 'def'    | 1       | ['abc', 'ghi', 'def']
        'def' | 'abc'    | 1       | ['def', 'abc', 'ghi']
    }

    def "reorder option last"() {
        given:
        Option opt1 = new Option(name: 'abc')
        Option opt2 = new Option(name: 'def')
        Option opt3 = new Option(name: 'ghi')
        def opts = [opt1, opt2, opt3]
        def editopts = opts.collectEntries { [it.name, it] }

        when:
        def output = controller._applyOptionAction(
                editopts,
                [action: 'reorder', name: optC, params: [last: true]]
        )

        then:
        editopts == [abc: opt1, def: opt2, ghi: opt3]
        output == [undo: [action: 'reorder', name: optC, params: [relativePosition: undoPosition]]]
        result == new TreeSet(opts)*.name

        where:
        optC | undoPosition | result
        'abc' | -2           | ['def', 'ghi', 'abc']
        'def' | -1           | ['abc', 'ghi', 'def']
        'ghi' | 0            | ['abc', 'def', 'ghi']
    }

    def "apply option action insert"() {
        given:
        //test insert, should have remove undo action
        def optsmap = [:]
        controller.fileUploadService = Mock(FileUploadService)
        when:
        def result = controller._applyOptionAction(
                optsmap,
                [action: 'insert', name: 'optname', params: [name: 'optname']]
        )
        then:
        result.error == null
        optsmap.size() == 1
        optsmap['optname'] != null
        final Object item = optsmap['optname']
        item instanceof Option

        //test undo
        result.undo != null
        result.undo.action == 'remove'
        result.undo.name == 'optname'
    }

    def "apply option action insert option value plugin type"() {
        given:
        //test insert, should have remove undo action
        def optsmap = [:]
        controller.fileUploadService = Mock(FileUploadService)
        when:
        def result = controller._applyOptionAction(
                optsmap,
                [action: 'insert', name: 'optname', params: [name: 'optname',valuesType:'optValPlugin']]
        )
        then:
        result.error == null
        optsmap.size() == 1
        optsmap['optname'] != null
        final Object item = optsmap['optname']
        item instanceof Option
        item.optionValuesPluginType == 'optValPlugin'
    }

    def "apply option action remove"() {
        given:
        //test remove, should have insert undo action
        Option test1 = new Option(name: 'optname')
        def optsmap = [optname: test1]

        controller.fileUploadService = Mock(FileUploadService)
        when:
        def result = controller._applyOptionAction(
                optsmap,
                [action: 'remove', name: 'optname', params: [name: 'optname']]
        )
        then:
        result.error == null
        optsmap.size() == 0
        optsmap['optname'] == null

        //test undo
        result.undo != null
        result.undo.action == 'insert'
        result.undo.name == 'optname'
        result.undo.params != null
    }

    def "apply option action modify"() {
        given:
        //test modify, should have original params in undo action
        Option test1 = new Option(name: 'optname', description: 'original description')
        def optsmap = [optname: test1]

        controller.fileUploadService = Mock(FileUploadService)
        when:
        def result = controller._applyOptionAction(
                optsmap,
                [action: 'modify', name: 'optname', params: [name: 'optname', description: 'a new description']]
        )
        then:
        result.error == null
        optsmap.size() == 1
        optsmap['optname'] != null
        Option test2 = optsmap['optname']
        test2.name == 'optname'
        test2.description == 'a new description'

        //test undo
        result.undo != null
        result.undo.action == 'modify'
        result.undo.name == 'optname'
        result.undo.params != null
        result.undo.params.description == 'original description'
    }

    def "apply option action modify rename"() {
        given:
        //test modify, renaming to new  name
        Option test1 = new Option(name: 'optname', description: 'original description')
        def optsmap = [optname: test1]

        controller.fileUploadService = Mock(FileUploadService)
        when:
        def result = controller._applyOptionAction(
                optsmap,
                [action: 'modify', name: 'optname', params: [name: 'newoptname', description: 'a new description']]
        )
        then:
        result.error == null
        optsmap.size() == 1
        optsmap['optname'] == null
        optsmap['newoptname'] != null
        Option test2 = optsmap['newoptname']
        test2.name == 'newoptname'
        test2.description == 'a new description'

        //test undo
        result.undo != null
        result.undo.action == 'modify'
        result.undo.name == 'newoptname'
        result.undo.params != null
        result.undo.params.name == 'optname'
        result.undo.params.description == 'original description'
    }

    ///**** test failures *******////

    def "apply option action insert duplicate"() {
        given:
        //test insert with name of existing option
        Option test1 = new Option(name: 'optname', description: 'original description')
        def optsmap = [optname: test1]

        controller.fileUploadService = Mock(FileUploadService)
        when:
        def result = controller._applyOptionAction(
                optsmap,
                [action: 'insert', name: 'optname', params: [name: 'optname', description: 'a new description']]
        )
        then:
        result.error == 'Invalid'
        result.option != null
    }

    def "invalid save returns fields in map"() {
        given:
        request.method = 'POST'
        controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
        def assetTaglib = mockTagLib(FakeTagLib)
        setupFormTokens(session)

        params.scheduledExecutionId = null
        params.name='test1'
        params.newoption = 'insert'
        params.num = 'testNum'
        controller.fileUploadService = Mock(FileUploadService)
        controller.optionValuesService = Mock(OptionValuesService)

        when:
        views['/scheduledExecution/_optEdit.gsp'] = 'mock template contents'
        controller.save()

        then:
        model.edit == true
        model.name == params.num
        model.newoption == params.newoption
        model.scheduledExecutionId == params.scheduledExecutionId
        model.origName == null
        1* controller.fileUploadService.getPluginDescription()
        1* controller.optionValuesService.listOptionValuesPlugins()
    }


    def "apply option action remove not found"() {
        given:
        //test remove, name does not exist
        Option test1 = new Option(name: 'optname', description: 'original description')
        def optsmap = [optname: test1]

        controller.fileUploadService = Mock(FileUploadService)
        when:
        def result = controller._applyOptionAction(optsmap, [action: 'remove', name: 'test2', params: [:]])
        then:
        result.error == "No option named test2 exists"
    }

    def "apply option action modify not found"() {
        given:
        //test modify, name does not exist
        Option test1 = new Option(name: 'optname', description: 'original description')
        def optsmap = [optname: test1]

        controller.fileUploadService = Mock(FileUploadService)
        when:
        def result = controller._applyOptionAction(optsmap, [action: 'modify', name: 'test2', params: [:]])
        then:
        result.error == 'No option named test2 exists'
    }

    def "apply option action rename duplicate"() {
        given:
        //test modify, renaming to already existing option name
        Option test1 = new Option(name: 'optname', description: 'original description')
        Option test2 = new Option(name: 'optname2', description: 'original description')
        def optsmap = [optname: test1, optname2: test2]

        controller.fileUploadService = Mock(FileUploadService)
        when:
        def result = controller._applyOptionAction(
                optsmap,
                [action: 'modify', name: 'optname', params: [name: 'optname2']]
        )
        then:
        result.error == 'Invalid'
        result.option != null
        result.option.errors.hasErrors()
    }

    def "apply action undo insert"() {
        given:
        //apply insert, apply undo (remove)
        def optsmap = [:]

        controller.fileUploadService = Mock(FileUploadService)
        def result = controller._applyOptionAction(
                optsmap,
                [action: 'insert', name: 'optname', params: [name                            : 'optname',
                                                             description                     : 'a description',
                                                             valuesType                      : 'url', enforcedType:
                                                                     'regex', valuesUrl      :
                                                                     'http://test.com', regex: 'testregex']]
        )
        when:
        def result2 = controller._applyOptionAction(optsmap, result.undo)
        then:

        //test undo
        result.undo != null
        result.undo.action == 'remove'
        result.undo.name == 'optname'

        //apply undo
        result2.error == null
        optsmap.size() == 0
    }

    def "apply action undo remove"() {
        given:
        //apply remove, apply undo (insert)
        Option test1 = new Option()
        controller._setOptionFromParams(
                test1,
                [name                            : 'optname', description: 'a description', valuesType: 'url',
                 enforcedType                    : 'regex', valuesUrl:
                         'http://test.com', regex: 'testregex']
        )
        def optsmap = [optname: test1]

        controller.fileUploadService = Mock(FileUploadService)
        def result = controller._applyOptionAction(
                optsmap,
                [action: 'remove', name: 'optname', params: [name: 'optname']]
        )
        when:
        //apply undo
        def result2 = controller._applyOptionAction(optsmap, result.undo)
        then:

        //test undo
        result.undo != null
        result.undo.action == 'insert'
        result.undo.name == 'optname'
        result.undo.params != null

        result2.error == null
        optsmap.size() == 1
        optsmap['optname'] != null
        final Object item = optsmap['optname']
        item instanceof Option
        Option option = (Option) item
        option.name == 'optname'
        option.description == 'a description'
        option.defaultValue == null
        ! option.enforced
        ! option.required
        option.realValuesUrl.toExternalForm() == 'http://test.com'
        option.regex == 'testregex'
        option.valuesList == null

    }

    def "apply action undo modify"() {
        given:
        //apply modify, apply undo (modify)
        Option test1 = new Option()
        controller._setOptionFromParams(
                test1,
                [name                            : 'optname', description: 'a description', valuesType: 'url',
                 enforcedType                    : 'regex', valuesUrl:
                         'http://test.com', regex: 'testregex']
        )
        def optsmap = [optname: test1]

        controller.fileUploadService = Mock(FileUploadService)
        def result = controller._applyOptionAction(
                optsmap,
                [action: 'modify', name: 'optname', params: [name                          : 'optname', description:
                        'a description2',
                                                             defaultValue                  : 'a', valuesType: 'list',
                                                             enforcedType                  :
                                                                     'enforced', valuesList: 'a,b,c', regex:
                                                                     'testregex']]
        )
        when:
        //test undo
        def result2 = controller._applyOptionAction(optsmap, result.undo)
        then:


        result2.error == null
        optsmap.size() == 1
        optsmap['optname'] != null
        final Object item = optsmap['optname']
        item instanceof Option
        Option option = (Option) item

        option.name == 'optname'
        option.description == 'a description'
        option.defaultValue == null
        ! option.enforced
        ! option.required
        option.realValuesUrl.toExternalForm() == 'http://test.com'
        option.regex == 'testregex'
        option.valuesList == null
    }

    def "apply action undo rename"() {
        given:
        //apply modify rename, apply undo (modify)
        Option test1 = new Option()
        controller._setOptionFromParams(
                test1,
                [name                            : 'optname', description: 'a description', valuesType: 'url',
                 enforcedType                    : 'regex', valuesUrl:
                         'http://test.com', regex: 'testregex']
        )
        def optsmap = [optname: test1]

        controller.fileUploadService = Mock(FileUploadService)
        def result = controller._applyOptionAction(
                optsmap,
                [action: 'modify', name: 'optname', params: [name                          : 'optname2', description:
                        'a description2',
                                                             defaultValue                  : 'a', valuesType: 'list',
                                                             enforcedType                  :
                                                                     'enforced', valuesList: 'a,b,c', regex:
                                                                     'testregex']]
        )
        when:
        //test undo
        def result2 = controller._applyOptionAction(optsmap, result.undo)
        then:


        result2.error == null
        optsmap.size() == 1
        optsmap['optname'] != null
        final Object item = optsmap['optname']
        item instanceof Option
        Option option = (Option) item

        option.name == 'optname'
        option.description == 'a description'
        option.defaultValue == null
        ! option.enforced
        ! option.required
        option.realValuesUrl.toExternalForm() == 'http://test.com'
        option.regex == 'testregex'
        option.valuesList == null
    }

    def "enforce allowed values with option plugin is valid"() {

        when:
        Option opt = new Option()
        opt.name = "opt1"
        opt.optionValuesPluginType = "optionValues"
        opt.enforced = true
        def result = controller._validateOption(opt, provider)

        then:
        result.isEmpty()
    }

    def "get Session opts convert value list to pass full object to session"(){
        given:
        def optClone = Mock(Option) {

        }
        def opt = Mock(Option){
            getName() >> 'blah'
            createClone() >> optClone
        }
        SortedSet options = new TreeSet( [opt] )
        def sched = Mock(ScheduledExecution){
            getOptions() >> options
        }
        params.scheduledExecutionId = 1L
        params.sched = sched
        when:

        def result2 = controller.getSessionOptions(session,params)
        then:
        result2
        result2.blah
        1 * optClone.getOptionValues() >> ['a', 'b']
    }

    @Unroll
    def "endpoint #endpoint authz required"() {
        given:
            ScheduledExecution job = createJob()
            params.scheduledExecutionId = job.id.toString()
            controller.frameworkService = Mock(FrameworkService)


            controller.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
            def assetTaglib = mockTagLib(FakeTagLib)
            setupFormTokens(session)
            request.method = 'POST'
        when:
            controller."$endpoint"()
        then:
            response.status == 403
            1 * controller.rundeckAuthContextProcessor.authorizeProjectJobAll(_, !null, [action], job.project) >> false
        where:
            endpoint        | action
            'edit'          | AuthConstants.ACTION_UPDATE
            'renderOpt'     | AuthConstants.ACTION_UPDATE
            'renderAll'     | AuthConstants.ACTION_UPDATE
            'renderSummary' | AuthConstants.ACTION_UPDATE
            'save'          | AuthConstants.ACTION_UPDATE
            'remove'        | AuthConstants.ACTION_UPDATE
            'reorder'       | AuthConstants.ACTION_UPDATE
            'undo'          | AuthConstants.ACTION_UPDATE
            'redo'          | AuthConstants.ACTION_UPDATE
            'duplicate'     | AuthConstants.ACTION_UPDATE

    }

    def "duplicate options"(){
        given:
        Option opt2 = new Option(name: 'def', sortIndex: 1)
        Option opt1 = new Option(name: 'abc', sortIndex: 2)
        Option opt3 = new Option(name: 'ghi', sortIndex: 3)
        def opts = [opt1, opt2, opt3]
        def editopts = opts.collectEntries { [it.name, it] }
        controller.fileUploadService = Mock(FileUploadService)

        params.scheduledExecutionId = 1L
        params.name = "abc"
        when:

        def result = controller._duplicateOption(editopts)
        def result1 = controller._duplicateOption(editopts)

        then:
        result.name == "abc_1"
        result.actions.undo.action == "remove"
        result.actions.undo.name == "abc_1"

        result1.name == "abc_2"
        result1.actions.undo.action == "remove"
        result1.actions.undo.name == "abc_2"

        editopts[opt1.name].sortIndex == 2
        editopts[opt2.name].sortIndex == 1
        editopts[opt3.name].sortIndex == 5

        editopts.size() == 5
    }
    def "duplicate options secure"(){
        given:
        Option opt1 = new Option(name: 'abc', secureInput: true, secureExposed: true, defaultStoragePath: 'keys/asdf',required: true)
        def opts = [opt1]
        def editopts = opts.collectEntries { [it.name, it] }
        controller.fileUploadService = Mock(FileUploadService)

        params.scheduledExecutionId = 1L
        params.name = "abc"
        when:

        def result = controller._duplicateOption(editopts)

        then:
        result.name == "abc_1"
        result.actions.undo.action == "remove"
        result.actions.undo.name == "abc_1"
        def map1=opt1.toMap()
        def map2=editopts['abc_1'].toMap()
        map1.remove('name')
        map2.remove('name')
        map1==map2

    }
}
@Artefact("TagLib")
class FakeTagLib {
    def refreshFormTokensHeader = { attrs, body ->
        response.addHeader('x-test', 'refreshFormTokensHeader')
    }
}
