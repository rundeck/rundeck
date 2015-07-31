package com.dtolabs.rundeck.core.authorization.providers

import spock.lang.Specification
import spock.lang.Unroll

import static com.dtolabs.rundeck.core.authorization.providers.YamlProvider.sourceFromString
import static com.dtolabs.rundeck.core.authorization.providers.YamlProvider.validate

/**
 * Created by greg on 7/31/15.
 */
class YamlProviderSpec extends Specification {

    def "validate empty list"(){
        when:
        def validation = validate(Arrays.asList())

        then:
        validation.valid
    }
    def "validate basic ok"(){
        when:
        def validation = validate(sourceFromString("test1",'''
context:
    project: test
by:
    username: elf
    group: jank
for:
    type:
        - allow: '*'
description: blah
id: any string
''',new Date()))

        then:
        validation.valid
    }
    def "validate no context"(){
        when:
        def validation = validate(sourceFromString("test1",'''
by:
    username: elf
for:
    type:
        - allow: '*'
description: blah
''',new Date()))

        then:
        !validation.valid
        validation.errors.size()==1
        validation.errors['test1[1]']==['Required \'context:\' section was not present.']
    }
    def "validate no description"(){
        when:
        def validation = validate(sourceFromString("test1",'''
context:
    project: test
by:
    username: elf
for:
    type:
        - allow: '*'
''',new Date()))

        then:
        !validation.valid
        validation.errors.size()==1
        validation.errors['test1[1]']==['Policy is missing a description.']
    }
    def "validate no by"(){
        when:
        def validation = validate(sourceFromString("test1",'''
context:
    project: test
description: wat
for:
    type:
        - allow: '*'
''',new Date()))

        then:
        !validation.valid
        validation.errors.size()==1
        validation.errors['test1[1]']==['Required \'by:\' section was not present.']
    }
    def "validate no for"(){
        when:
        def validation = validate(sourceFromString("test1",'''
context:
    project: test
description: wat
by:
    username: elf

''',new Date()))

        then:
        !validation.valid
        validation.errors.size()==1
        validation.errors['test1[1]']==['Required \'for:\' section was not present.']
    }
    def "validate extraneous section"(){
        when:
        def validation = validate(sourceFromString("test1",'''
context:
    project: test
description: wat
by:
    username: elf
for:
    type:
        - allow: '*'
invalid: blah
''',new Date()))

        then:
        !validation.valid
        validation.errors.size()==1
        validation.errors['test1[1]']==['Policy contains invalid keys: [invalid], allowed keys: [by, id, for, context, description]']
    }
    def "validate context invalid entry"(){
        when:
        def validation = validate(sourceFromString("test1",'''
context:
    blab: test
description: wat
by:
    username: elf
for:
    type:
        - allow: '*'
''',new Date()))

        then:
        !validation.valid
        validation.errors.size()==1
        validation.errors['test1[1]']==['Context section is not valid: {blab=test}, it should contain only \'application:\' or \'project:\'']
    }
    def "validate by invalid entry"(){
        when:
        def validation = validate(sourceFromString("test1",'''
context:
    project: test
description: wat
by:
    blonk: elf
for:
    type:
        - allow: '*'
''',new Date()))

        then:
        !validation.valid
        validation.errors.size()==1
        validation.errors['test1[1]']==['Section \'by:\' is not valid: {blonk=elf} it must contain \'group:\' and/or \'username:\'']
    }
    def "validate for empty"(){
        when:
        def validation = validate(sourceFromString("test1",'''
context:
    project: test
description: wat
by:
    username: elf
for: { }
''',new Date()))

        then:
        !validation.valid
        validation.errors.size()==1
        validation.errors['test1[1]']==['Section \'for:\' should not be empty.']
    }
    def "validate for expect map"(){
        when:
        def validation = validate(sourceFromString("test1",'''
context:
    project: test
description: wat
by:
    username: elf
for: [ ]
''',new Date()))

        then:
        !validation.valid
        validation.errors.size()==1
        validation.errors['test1[1]']==['Expected \'for:\' section to contain a map, but was [java.util.ArrayList].']
    }
    def "validate for expect map entry is list"(){
        when:
        def validation = validate(sourceFromString("test1",'''
context:
    project: test
description: wat
by:
    username: elf
for:
    blah: { }
''',new Date()))

        then:
        !validation.valid
        validation.errors.size()==1
        validation.errors['test1[1]']==['Expected \'for: { blah: <...> }\' section to contain a List, but was [java.util.LinkedHashMap].']
    }
    def "validate type entry requires non-empty"(){
        when:
        def validation = validate(sourceFromString("test1",'''
context:
    project: test
description: wat
by:
    username: elf
for:
    blah: [ ]
''',new Date()))

        then:
        !validation.valid
        validation.errors.size()==1
        validation.errors['test1[1]']==['Section \'for: { blah: [...] }\' list should not be empty.']
    }
    def "validate type entry requires map"(){
        when:
        def validation = validate(sourceFromString("test1",'''
context:
    project: test
description: wat
by:
    username: elf
for:
    blah:
        - [ ]
''',new Date()))

        then:
        !validation.valid
        validation.errors.size()==1
        validation.errors['test1[1]']==['Type rule \'for: { blah: [...] }\'\' entry at index [1] expected a Map but saw: java.util.ArrayList']
    }
    def "validate type entry requires non-empty map"(){
        when:
        def validation = validate(sourceFromString("test1",'''
context:
    project: test
description: wat
by:
    username: elf
for:
    blah:
        - { }
''',new Date()))

        then:
        !validation.valid
        validation.errors.size()==1
        validation.errors['test1[1]']==['Type rule \'for: { blah: [...] }\' entry at index [1] One of \'allow:\' or \'deny:\' must be present.']
    }
    @Unroll
    def "validate type entry actions expects string or list"(String actionsname,_){
        when:
        def validation = validate(sourceFromString("test1","""
context:
    project: test
description: wat
by:
    username: elf
for:
    blah:
        - ${actionsname}: {}
""",new Date()))

        then:
        !validation.valid
        validation.errors.size()==1
        validation.errors['test1[1]']== [('Type rule \'for: { blah: [...] }\' entry at index [1] Section \'' +
                                                  actionsname +
                                                  ':\' expected a String or a sequence of Strings, but was a java.util.LinkedHashMap')]

        where:
        actionsname | _
        "allow"     | _
        "deny"      | _
    }
    @Unroll
    def "validate type entry actions expects non-empty list"(String actionsname,_){
        when:
        def validation = validate(sourceFromString("test1","""
context:
    project: test
description: wat
by:
    username: elf
for:
    blah:
        - ${actionsname}: []
""",new Date()))

        then:
        !validation.valid
        validation.errors.size()==1
        validation.errors['test1[1]']== [('Type rule \'for: { blah: [...] }\' entry at index [1] Section \'' +
                                                  actionsname +
                                                  ':\' should not be empty.')]

        where:
        actionsname | _
        "allow"     | _
        "deny"      | _
    }
    def "validate type entry match/equals/contains not empty"(String matchname,_){
        when:
        def validation = validate(sourceFromString("test1","""
context:
    project: test
description: wat
by:
    username: elf
for:
    blah:
        - ${matchname}: {}
          allow: asdf
""",new Date()))

        then:
        !validation.valid
        validation.errors.size()==1
        validation.errors['test1[1]']== [('Type rule \'for: { blah: [...] }\' entry at index [1] Section \'' +
                                                  matchname +
                                                  ':\' should not be empty.')]

        where:
        matchname | _
        "match"   | _
        "equals"  | _
        "contains"  | _
    }
    @Unroll
    def "validate type entry match/equals/contains does not contain allow/deny"(String matchname,String actionsname,_){
        when:
        def validation = validate(sourceFromString("test1","""
context:
    project: test
description: wat
by:
    username: elf
for:
    blah:
        - ${matchname}: { ${actionsname}: asdf }
          allow: asdf
""",new Date()))

        then:
        !validation.valid
        validation.errors.size()==1
        validation.errors['test1[1]'].size()==(matchname=='contains'?2:1)
        if(matchname=='contains'){
            validation.errors['test1[1]']== [
                    ('Type rule \'for: { blah: [...] }\' entry at index [1] Section \'' +
                            matchname +
                            ':\' can only be applied to \'tags\'.')
                    ,
                    ('Type rule \'for: { blah: [...] }\' entry at index [1] Section \'' +
                    matchname +
                    ':\' should not contain \'allow:\' or \'deny:\'.')]
        }else{
            validation.errors['test1[1]']== [('Type rule \'for: { blah: [...] }\' entry at index [1] Section \'' +
                    matchname +
                    ':\' should not contain \'allow:\' or \'deny:\'.')]
        }


        where:
        matchname  | actionsname | _
        "match"    | "allow"     | _
        "match"    | "deny"      | _
        "equals"   | "allow"     | _
        "equals"   | "deny"      | _
        "contains" | "allow"     | _
        "contains" | "deny"      | _
    }


}
