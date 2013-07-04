package rundeck.controllers

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.CommandExec
import rundeck.Execution
import rundeck.Option
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.WorkflowStep

@TestFor(EditOptsController)
@Mock([ScheduledExecution, Option, Workflow, WorkflowStep, CommandExec, Execution])
class EditOptsControllerTests  {

    void test_setOptionFromParamstestEmpty(){
        EditOptsController ctrl = new EditOptsController()
            Option inputOption = new Option()
            final Map testmap = [:]
            Option option = ctrl._setOptionFromParams(inputOption,testmap)

            assertNotNull(option)
            //test properties
            assertNull option.name
            assertNull option.description
            assertFalse option.required
            assertFalse option.enforced
            assertNull option.values
            assertNull option.valuesList
            assertNull option.realValuesUrl
            assertNull option.regex
            assertNull option.defaultValue
        }

        void test_setOptionFromParamstestValuesTypeList() {
            EditOptsController ctrl = new EditOptsController()
            //use valuesType='list', enforcedType='none'
            Option inputOption = new Option()
            final Map testmap = [name:'testopt',description:'a description',valuesType:'list',enforcedType:'none',valuesList:'a,b,c',valuesUrl:'http://test.com',regex:'testregex']
            Option option = ctrl._setOptionFromParams(inputOption,testmap)

            assertNotNull(option)
            assertFalse(option.errors.hasErrors())
            //test properties
            assertEquals 'testopt', option.name
            assertEquals 'a description', option.description
            assertFalse option.required
            assertFalse option.enforced
            assertNotNull option.values
            assertEquals 3,option.values.size()
            assertTrue option.values.contains('a')
            assertTrue option.values.contains('b')
            assertTrue option.values.contains('c')
            assertNull option.valuesList
            assertNull option.realValuesUrl
            assertNull option.regex
            assertNull option.defaultValue
        }

    void test_setOptionFromParams_testValuesTypeUrl() {
        EditOptsController ctrl = new EditOptsController()
            //use valuesType='url', enforcedType='none'
            Option inputOption = new Option()
            final Map testmap = [name:'testopt',description:'a description',valuesType:'url',enforcedType:'none',valuesList:'a,b,c',valuesUrl:'http://test.com',regex:'testregex']
            Option option = ctrl._setOptionFromParams(inputOption,testmap)

            assertNotNull(option)
            //test properties
            assertEquals 'testopt', option.name
            assertEquals 'a description', option.description
            assertFalse option.required
            assertFalse option.enforced
            assertNull option.values
            assertNull option.valuesList
            assertNotNull option.realValuesUrl
            assertEquals 'http://test.com', option.realValuesUrl.toExternalForm()
            assertNull option.regex
            assertNull option.defaultValue
        }

    void test_setOptionFromParams_testEnforcedType() {
        EditOptsController ctrl = new EditOptsController()
            //use valuesType='url', enforcedType='enforced'
            Option inputOption = new Option()
            final Map testmap = [name:'testopt',description:'a description',valuesType:'url',enforcedType:'enforced',valuesList:'a,b,c',valuesUrl:'http://test.com',regex:'testregex']
            Option option = ctrl._setOptionFromParams(inputOption,testmap)

            assertNotNull(option)
            //test properties
            assertEquals 'testopt', option.name
            assertEquals 'a description', option.description
            assertFalse option.required
            assertTrue option.enforced
            assertNull option.values
            assertNull option.valuesList
            assertNotNull option.realValuesUrl
            assertEquals 'http://test.com', option.realValuesUrl.toExternalForm()
            assertNull option.regex
            assertNull option.defaultValue
        }

    void test_setOptionFromParams_testEnforcedType2() {
        EditOptsController ctrl = new EditOptsController()
            //use valuesType='url', enforcedType='regex'
            Option inputOption = new Option()
            final Map testmap = [name:'testopt',description:'a description',valuesType:'url',enforcedType:'regex',valuesList:'a,b,c',valuesUrl:'http://test.com',regex:'testregex']
            Option option = ctrl._setOptionFromParams(inputOption,testmap)

            assertNotNull(option)
            //test properties
            assertEquals 'testopt', option.name
            assertEquals 'a description', option.description
            assertFalse option.required
            assertFalse option.enforced
            assertNull option.values
            assertNull option.valuesList
            assertNotNull option.realValuesUrl
            assertEquals 'http://test.com', option.realValuesUrl.toExternalForm()
            assertNotNull option.regex
            assertEquals 'testregex',option.regex
            assertNull option.defaultValue
        }

    void test_setOptionFromParams_testInputTypePlain() {
        EditOptsController ctrl = new EditOptsController()
            //use inputType='plain'
            Option inputOption = new Option()
            final Map testmap = [name:'testopt',description:'a description',valuesList:'a,b,c',inputType:'plain']
            Option option = ctrl._setOptionFromParams(inputOption,testmap)

            assertNotNull(option)
            //test properties
            assertEquals 'testopt', option.name
            assertEquals 'a description', option.description
            assertFalse option.required
            assertFalse option.enforced
            assertNotNull option.values
            assertNull option.realValuesUrl
            assertNull option.regex
            assertNull option.defaultValue
            assertFalse option.secureInput
            assertFalse option.secureExposed
        }

    void test_setOptionFromParams_testInputTypeSecure() {
        EditOptsController ctrl = new EditOptsController()
            //use inputType='secure'
            Option inputOption = new Option()
            final Map testmap = [name:'testopt',description:'a description',valuesList:'a,b,c',inputType:'secure']
            Option option = ctrl._setOptionFromParams(inputOption,testmap)

            assertNotNull(option)
            //test properties
            assertEquals 'testopt', option.name
            assertEquals 'a description', option.description
            assertFalse option.required
            assertFalse option.enforced
            assertNotNull option.values
            assertNull option.realValuesUrl
            assertNull option.regex
            assertNull option.defaultValue
            assertTrue option.secureInput
            assertFalse option.secureExposed
        }

    void test_setOptionFromParams_testInputTypeSecureExposed() {
        EditOptsController ctrl = new EditOptsController()
            //use inputType='secureExposed'
            Option inputOption = new Option()
            final Map testmap = [name:'testopt',description:'a description',valuesList:'a,b,c',inputType:'secureExposed']
            Option option = ctrl._setOptionFromParams(inputOption,testmap)

            assertNotNull(option)
            //test properties
            assertEquals 'testopt', option.name
            assertEquals 'a description', option.description
            assertFalse option.required
            assertFalse option.enforced
            assertNotNull option.values
            assertNull option.realValuesUrl
            assertNull option.regex
            assertNull option.defaultValue
            assertTrue option.secureInput
            assertTrue option.secureExposed
        }

        //test on existing Option content

    void test_setOptionFromParams_test() {
        EditOptsController ctrl = new EditOptsController()
            Option test1 = new Option()
            ctrl._setOptionFromParams(test1,[name:'optname',description:'a description',valuesType:'url',enforcedType:'regex',valuesUrl:'http://test.com',regex:'testregex'])

            Option test2 = ctrl._setOptionFromParams(test1,[name:'optname',description:'a description2',defaultValue:'a',valuesType:'list',enforcedType:'enforced',valuesList:'a,b,c',regex:'testregex'])
            assertNotNull test2
            assertEquals 'optname',test2.name
            assertEquals 'a description2',test2.description
            assertEquals 'a',test2.defaultValue
            assertTrue test2.enforced
            assertFalse test2.required
            assertNull test2.realValuesUrl
            assertNull test2.regex
            assertNotNull test2.values
            assertEquals 3, test2.values.size()
            assertTrue test2.values.contains('a')
            assertTrue test2.values.contains('b')
            assertTrue test2.values.contains('c')
            assertNull test2.valuesList

        }

    void test_applyOptionAction() {
        EditOptsController ctrl = new EditOptsController()
        test:{
            //test insert, should have remove undo action
            def optsmap = [:]

            def result = ctrl._applyOptionAction(optsmap,[action:'insert',name:'optname',params:[name:'optname']])
            assertNull result.error
            assertEquals 1,optsmap.size()
            assertNotNull optsmap['optname']
            final Object item = optsmap['optname']
            assertTrue item instanceof Option
            
            //test undo
            assertNotNull result.undo
            assertEquals 'remove',result.undo.action
            assertEquals 'optname',result.undo.name
        }
        test:{
            //test remove, should have insert undo action
            Option test1 = new Option(name:'optname')
            def optsmap = [optname:test1]

            def result = ctrl._applyOptionAction(optsmap,[action:'remove',name:'optname',params:[name:'optname']])
            assertNull result.error
            assertEquals 0,optsmap.size()
            assertNull optsmap['optname']

            //test undo
            assertNotNull result.undo
            assertEquals 'insert',result.undo.action
            assertEquals 'optname',result.undo.name
            assertNotNull result.undo.params
        }
        test:{
            //test modify, should have original params in undo action
            Option test1 = new Option(name:'optname',description:'original description')
            def optsmap = [optname:test1]

            def result = ctrl._applyOptionAction(optsmap,[action:'modify',name:'optname',params:[name:'optname',description:'a new description']])
            assertNull result.error
            assertEquals 1,optsmap.size()
            assertNotNull optsmap['optname']
            Option test2 = optsmap['optname']
            assertEquals 'optname',test2.name
            assertEquals 'a new description',test2.description

            //test undo
            assertNotNull result.undo
            assertEquals 'modify',result.undo.action
            assertEquals 'optname',result.undo.name
            assertNotNull result.undo.params
            assertEquals 'original description',result.undo.params.description
        }
        testRename:{
            //test modify, renaming to new  name
            Option test1 = new Option(name:'optname',description:'original description')
            def optsmap = [optname:test1]

            def result = ctrl._applyOptionAction(optsmap,[action:'modify',name:'optname',params:[name:'newoptname',description:'a new description']])
            assertNull result.error
            assertEquals 1,optsmap.size()
            assertNull optsmap['optname']
            assertNotNull optsmap['newoptname']
            Option test2 = optsmap['newoptname']
            assertEquals 'newoptname',test2.name
            assertEquals 'a new description',test2.description

            //test undo
            assertNotNull result.undo
            assertEquals 'modify',result.undo.action
            assertEquals 'newoptname',result.undo.name
            assertNotNull result.undo.params
            assertEquals 'optname',result.undo.params.name
            assertEquals 'original description',result.undo.params.description
        }

        ///**** test failures *******////

        testInsertDupe:{
            //test insert with name of existing option
            Option test1 = new Option(name:'optname',description:'original description')
            def optsmap = [optname:test1]

            def result = ctrl._applyOptionAction(optsmap,[action:'insert',name:'optname',params:[name:'optname',description:'a new description']])
            assertNotNull result.error
            assertNotNull result.option
        }
        testRemoveNoExist:{
            //test remove, name does not exist
            Option test1 = new Option(name:'optname',description:'original description')
            def optsmap = [optname:test1]

            def result = ctrl._applyOptionAction(optsmap,[action:'remove',name:'test2',params:[:]])
            assertNotNull result.error
            assertEquals "No option named test2 exists", result.error
        }
        testModifyNoExist:{
            //test modify, name does not exist
            Option test1 = new Option(name:'optname',description:'original description')
            def optsmap = [optname:test1]

            def result = ctrl._applyOptionAction(optsmap,[action:'modify',name:'test2',params:[:]])
            assertNotNull result.error
            assertEquals 'No option named test2 exists', result.error
        }
        testRenameToDupe:{
            //test modify, renaming to already existing option name
            Option test1 = new Option(name:'optname',description:'original description')
            Option test2 = new Option(name:'optname2',description:'original description')
            def optsmap = [optname:test1,optname2:test2]

            def result = ctrl._applyOptionAction(optsmap,[action:'modify',name:'optname',params:[name:'optname2']])
            assertNotNull result.error
            assertEquals 'Invalid', result.error
            assertNotNull result.option
            assertTrue result.option.errors.hasErrors()
        }

    }





    /**
     * Apply actions then apply the undo action set, test result
     */
    void test_applyOptionActionUndo() {

        EditOptsController ctrl = new EditOptsController()
        test:{
            //apply insert, apply undo (remove)
            def optsmap = [:]

            def result = ctrl._applyOptionAction(optsmap,[action:'insert',name:'optname',params:[name:'optname',description:'a description',valuesType:'url',enforcedType:'regex',valuesUrl:'http://test.com',regex:'testregex']])
            assertNull result.error
            assertEquals 1,optsmap.size()
            assertNotNull optsmap['optname']
            final Object item = optsmap['optname']
            assertTrue item instanceof Option

            //test undo
            assertNotNull result.undo
            assertEquals 'remove',result.undo.action
            assertEquals 'optname',result.undo.name

            //apply undo
            def result2 = ctrl._applyOptionAction(optsmap,result.undo)
            assertNull result2.error
            assertEquals 0,optsmap.size()
        }
        test:{
            //apply remove, apply undo (insert)
            Option test1 = new Option()
            ctrl._setOptionFromParams(test1,[name:'optname',description:'a description',valuesType:'url',enforcedType:'regex',valuesUrl:'http://test.com',regex:'testregex'])
            def optsmap = [optname:test1]

            def result = ctrl._applyOptionAction(optsmap,[action:'remove',name:'optname',params:[name:'optname']])
            assertNull result.error
            assertEquals 0,optsmap.size()
            assertNull optsmap['optname']

            //test undo
            assertNotNull result.undo
            assertEquals 'insert',result.undo.action
            assertEquals 'optname',result.undo.name
            assertNotNull result.undo.params

            System.err.println("undo: ${result.undo}");
            //apply undo
            def result2 = ctrl._applyOptionAction(optsmap,result.undo)
            assertNull result2.error
            assertEquals 1,optsmap.size()
            assertNotNull optsmap['optname']
            final Object item = optsmap['optname']
            assertTrue item instanceof Option
            Option option =(Option) item
            assertEquals 'optname',option.name
            assertEquals 'a description',option.description
            assertNull option.defaultValue
            assertFalse option.enforced
            assertFalse option.required
            assertEquals 'http://test.com',option.realValuesUrl.toExternalForm()
            assertEquals 'testregex',option.regex
            assertTrue  null==option.values || 0==option.values.size()
            assertNull option.valuesList

        }
        test:{
            //apply modify, apply undo (modify)
            Option test1 = new Option()
            ctrl._setOptionFromParams(test1,[name:'optname',description:'a description',valuesType:'url',enforcedType:'regex',valuesUrl:'http://test.com',regex:'testregex'])
            def optsmap = [optname:test1]

            def result = ctrl._applyOptionAction(optsmap,[action:'modify',name:'optname',params:[name:'optname',description:'a description2',defaultValue:'a',valuesType:'list',enforcedType:'enforced',valuesList:'a,b,c',regex:'testregex']])
            assertNull result.error
            assertEquals 1,optsmap.size()
            assertNotNull optsmap['optname']
            Option test2 = optsmap['optname']
            assertEquals 'optname',test2.name
            assertEquals 'a description2',test2.description
            assertEquals 'a',test2.defaultValue
            assertTrue test2.enforced
            assertFalse test2.required
            assertNull test2.realValuesUrl
            assertNull test2.regex
            assertNotNull test2.values
            assertEquals 3, test2.values.size()
            assertTrue test2.values.contains('a')
            assertTrue test2.values.contains('b')
            assertTrue test2.values.contains('c')
            assertNull test2.valuesList

            //test undo
            def result2 = ctrl._applyOptionAction(optsmap,result.undo)
            assertNull result2.error
            assertEquals 1,optsmap.size()
            assertNotNull optsmap['optname']
            final Object item = optsmap['optname']
            assertTrue item instanceof Option
            Option option =(Option) item

            assertEquals 'optname',option.name
            assertEquals 'a description',option.description
            assertNull option.defaultValue
            assertFalse option.enforced
            assertFalse option.required
            assertEquals 'http://test.com',option.realValuesUrl.toExternalForm()
            assertEquals 'testregex',option.regex
            assertTrue  null==option.values || 0==option.values.size()
            assertNull option.valuesList
        }
        testRename:{
            //apply modify rename, apply undo (modify)
            Option test1 = new Option()
            ctrl._setOptionFromParams(test1,[name:'optname',description:'a description',valuesType:'url',enforcedType:'regex',valuesUrl:'http://test.com',regex:'testregex'])
            def optsmap = [optname:test1]

            def result = ctrl._applyOptionAction(optsmap,[action:'modify',name:'optname',params:[name:'optname2',description:'a description2',defaultValue:'a',valuesType:'list',enforcedType:'enforced',valuesList:'a,b,c',regex:'testregex']])
            assertNull result.error
            assertEquals 1,optsmap.size()
            assertNotNull optsmap['optname2']
            Option test2 = optsmap['optname2']
            assertEquals 'optname2',test2.name
            assertEquals 'a description2',test2.description
            assertEquals 'a',test2.defaultValue
            assertTrue test2.enforced
            assertFalse test2.required
            assertNull test2.realValuesUrl
            assertNull test2.regex
            assertNotNull test2.values
            assertEquals 3, test2.values.size()
            assertTrue test2.values.contains('a')
            assertTrue test2.values.contains('b')
            assertTrue test2.values.contains('c')
            assertNull test2.valuesList

            //test undo
            def result2 = ctrl._applyOptionAction(optsmap,result.undo)
            assertNull result2.error
            assertEquals 1,optsmap.size()
            assertNotNull optsmap['optname']
            final Object item = optsmap['optname']
            assertTrue item instanceof Option
            Option option =(Option) item

            assertEquals 'optname',option.name
            assertEquals 'a description',option.description
            assertNull option.defaultValue
            assertFalse option.enforced
            assertFalse option.required
            assertEquals 'http://test.com',option.realValuesUrl.toExternalForm()
            assertEquals 'testregex',option.regex
            assertTrue  null==option.values || 0==option.values.size()
            assertNull option.valuesList
        }
    }
}
