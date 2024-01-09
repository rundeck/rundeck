package com.dtolabs.rundeck.core.authorization.providers.yaml.model


import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.error.YAMLException
import spock.lang.Specification

class YamlPolicyDocConstructorSpec extends Specification {
    def "parse valid document"() {
        given:
            def yaml = new Yaml(new YamlPolicyDocConstructor())
        when:
            def all = yaml.loadAll('''
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
''')
        def obj = all.iterator().next()
        then:
            obj != null
            obj instanceof ACLPolicyDoc
            obj.getContext().getProject()=='test'
            obj.getBy().getUsername()=='elf'
            obj.getBy().getGroup()=='jank'
            obj.getFor().size()==1
            obj.getFor()['type'].size()==1
            obj.getFor()['type'][0] instanceof ACLPolicyDoc.TypeRule
            obj.getFor()['type'][0].allow=='*'
            obj.getDescription()=='blah'
            obj.getId()=='any string'
    }
    def "parse  empty document"() {
        given:
            def yaml = new Yaml(new YamlPolicyDocConstructor())
        when:
            def all = yaml.loadAll('''
---
''')

        then:
            all.iterator().next()==null
            !all.iterator().hasNext()
    }

    def "parse invalid document"() {
        given:
            def yaml = new Yaml(new YamlPolicyDocConstructor())
        when:
            def all = yaml.loadAll('''
context:
    project: test
by:
    username: elf
    group: jank
for: {}
description: blah
id: any string
''')
        def obj = all.iterator().next()
        then:
            YAMLException e = thrown()
            e.message.contains 'Section \'for:\' cannot be empty'
    }
    def "parse doc invalid tag"() {
        given:
            final Yaml yaml = new Yaml(new YamlPolicyDocConstructor());
        when:
            def result = yaml.loadAll(yamlString)
            def first = result.iterator().next()
        then:
            YAMLException e = thrown()
            e.message.contains 'Global tag is not allowed: tag:yaml.org,2002:java.lang.Object'

        where:
            yamlString<<[
                TEST_YAML1,
                TEST_YAML2,
                TEST_YAML3,
            ]
    }
    public static final String TEST_YAML1='''context:
    project: test
by:
    username: elf
    group: jank
for:
    type:
        - allow: !!java.lang.Object
description: asdf
id: any string'''
    public static final String TEST_YAML2='''context:
    project: test
by:
    username: elf
    group: jank
for:
    type:
        - allow: asdf
description: !!java.lang.Object
id: any string'''
    public static final String TEST_YAML3='''context:
    project: test
by:
    username: elf
    group:
      - !!java.lang.Object
for:
    type:
        - allow: asdf
description: asdf
id: any string'''
}
