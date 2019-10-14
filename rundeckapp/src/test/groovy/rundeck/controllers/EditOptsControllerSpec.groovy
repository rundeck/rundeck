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

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.web.GroovyPageUnitTestMixin
import org.grails.plugins.codecs.URLCodec
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import rundeck.CommandExec
import rundeck.Option
import rundeck.ScheduledExecution
import rundeck.UtilityTagLib
import rundeck.Workflow
import rundeck.codecs.URIComponentCodec
import rundeck.services.FileUploadService
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 2/11/16.
 */
@TestFor(EditOptsController)
@Mock([Option, ScheduledExecution, Workflow])
@TestMixin(GroovyPageUnitTestMixin)
class EditOptsControllerSpec extends Specification {
    def setup() {
        mockCodec(URIComponentCodec)
//        mockCodec(URLCodec)
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
        when:
            ScheduledExecution job = createJob()
            session[sessionDataVar] = [
                (job.id.toString()): [
                    [action: 'remove', name: 'test']
                ]
            ]
            params.scheduledExecutionId = job.id.toString()
            setupFormTokens(session)
            request.method = 'POST'
            controller."$action"()
        then:
            flash.error == 'No option named test exists'
            response.status == 400
        where:
            action | sessionDataVar
            'redo' | 'redoOPTS'
            'undo' | 'undoOPTS'

    }
    def "validate opt required scheduled job with default storage path"() {
        given:
        Option opt = new Option(required: true, defaultValue: defval, defaultStoragePath: defstorageval, enforced: false)

        when:
        EditOptsController._validateOption(opt, params, true)
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
        EditOptsController._validateOption(opt, params, isched)
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
                [action: 'reorder', name: opt, params: [relativePosition: rel]]
        )

        then:
        editopts == [abc: opt1, def: opt2, ghi: opt3]
        output == [undo: [action: 'reorder', name: opt, params: [relativePosition: rel * -1]]]
        result == new TreeSet(opts)*.name

        where:
        opt   | rel | result
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
                [action: 'reorder', name: opt, params: [before: otherOpt]]
        )

        then:
        editopts == [abc: opt1, def: opt2, ghi: opt3]
        output == [undo: [action: 'reorder', name: opt, params: [relativePosition: undoPos]]]
        result == new TreeSet(opts)*.name

        where:
        opt   | otherOpt | undoPos | result
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
                [action: 'reorder', name: opt, params: [last: true]]
        )

        then:
        editopts == [abc: opt1, def: opt2, ghi: opt3]
        output == [undo: [action: 'reorder', name: opt, params: [relativePosition: undoPosition]]]
        result == new TreeSet(opts)*.name

        where:
        opt   | undoPosition | result
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
        null == option.values || 0 == option.values.size()
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
        null == option.values || 0 == option.values.size()
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
        null == option.values || 0 == option.values.size()
        option.valuesList == null
    }

    def "enforce allowed values with option plugin is valid"() {

        when:
        Option opt = new Option()
        opt.name = "opt1"
        opt.optionValuesPluginType = "optionValues"
        opt.enforced = true
        def result = controller._validateOption(opt)

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
}
