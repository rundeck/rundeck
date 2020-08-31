package com.dtolabs.rundeck.core.utils

import spock.lang.Specification

class PropertyLookupSpec extends Specification {

    private Properties properties1
    private File propertyFile

    def setup() {
        propertyFile = File.createTempFile("prop1", "properties")
        propertyFile.deleteOnExit()
        properties1 = new Properties()
        properties1.put("foo", "shizzle")
        properties1.put("bar", "madizzle")
        properties1.put("baz", "luzizle")
        properties1.store(new FileOutputStream(propertyFile), "test properties")
    }


    def testConstruction() {
        when:
            final PropertyLookup lookup = PropertyLookup.create(propertyFile)
        then:
            lookup != null
    }

    def testLookup() {
        when:
            final PropertyLookup lookup = PropertyLookup.create(propertyFile)
        then:
            lookup.hasProperty("foo")
            lookup.getProperty("foo") == "shizzle"
            lookup.getProperty("bar") == "madizzle"
            lookup.getProperty("baz") == "luzizle"
    }

    def testFetchProperties() throws IOException {
        when:
            final Properties props = PropertyLookup.fetchProperties(propertyFile)
        then:
            props.size() == 3
    }

    def testExpand() throws IOException {
        given:
            final File pFile = File.createTempFile("myprops", "properties")
            pFile.deleteOnExit()
            final Properties p = new Properties()
            p.put("foo", "shizzler")
            p.put("bar", '${foo}-madizzler')
            p.store(new FileOutputStream(pFile), "test properties")
        when:
            final PropertyLookup lookup = PropertyLookup.create(pFile)
        then:
            lookup.getProperty("foo") == "shizzler"
            PropertyLookup lookup2 = lookup.expand()
            lookup == lookup2
            "shizzler-madizzler" == lookup.getProperty("bar")
    }

    def testConstructionWithDefaults() throws IOException {
        given:
            final File pFile = File.createTempFile("myprops", "properties")
            pFile.deleteOnExit()
            final Properties p = new Properties()
            p.put("foo", "shizzler")
            p.put("bar", "bizzle")
            p.store(new FileOutputStream(pFile), "test properties")

        when:
            final PropertyLookup defaults = PropertyLookup.create(propertyFile)
            final PropertyLookup authoratitive = PropertyLookup.create(pFile, defaults)

        then:
            authoratitive.countProperties() == defaults.getPropertiesMap().size()

            "shizzler" == authoratitive.getProperty("foo")

    }

    def testDifference() {
        given:
            final PropertyLookup lookup = PropertyLookup.create(propertyFile)
            final Properties others = new Properties()
            others.put("blah", "bablazo")
        when:
            final Properties diff = lookup.difference(others)
        then:
            1 == diff.size()
            diff.containsKey("blah")
    }

    def "missing property should not throw exception"() {
        given:
            final PropertyLookup lookup = PropertyLookup.create(propertyFile)
        when:
            def result = lookup.getProperty(propname)
        then:
            result == expect

        where:
            propname | expect
            'foo'    | 'shizzle'
            'xfoo'   | null


    }
}
