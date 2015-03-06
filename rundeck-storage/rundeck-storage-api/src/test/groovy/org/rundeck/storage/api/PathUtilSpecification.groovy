package org.rundeck.storage.api

import spock.lang.Specification

import static PathUtil.cleanPath
import static PathUtil.parentPathString
import static PathUtil.pathName
import static org.rundeck.storage.api.PathUtil.pathFromComponents
import static org.rundeck.storage.api.PathUtil.pathStringFromComponents

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 2/4/14
 * Time: 4:19 PM
 */
class PathUtilSpecification extends Specification {

    def "cleanPath removes trailing slashes"() {
        expect:
        cleanPath("abc") == "abc"
        cleanPath("abc/") == "abc"
        cleanPath("abc//") == "abc"
    }

    def "cleanPath removes leading slashes"() {
        expect:
        cleanPath("/abc") == "abc"
        cleanPath("//abc") == "abc"
    }

    def "cleanPath collapses multiple slashes"() {
        expect:
        cleanPath("abc//xyz") == "abc/xyz"
        cleanPath("abc//xyz////zyd") == "abc/xyz/zyd"
    }

    def "parentPath without parent returns blank"() {
        expect:
        parentPathString("abc//") == ""
        parentPathString("abc") == ""
    }

    def "parentPath returns parent path"() {
        expect:
        parentPathString("abc//xyz") == "abc"
        parentPathString("abc//xyz/utiv") == "abc/xyz"
        parentPathString("abc//") == ""
    }

    def "pathName returns last path component"() {
        expect:
        pathName("abc//xyz/utiv") == "utiv"
        pathName("abc//xyz/") == "xyz"
        pathName("abc///") == "abc"
        pathName("abc") == "abc"
    }
    def "pathFromComponents joins components"() {
        expect:
        pathFromComponents(['a','b','c'] as String[]).path == "a/b/c"
    }

    def "pathStringFromComponents joins components"() {
        expect:
        pathStringFromComponents(['a','b','c'] as String[]) == "a/b/c"
    }

    def "hasRoot"() {
        expect:
        PathUtil.hasRoot("a/b", "")
        PathUtil.hasRoot("a/b", "/")
        PathUtil.hasRoot("a/b", "a")
        !PathUtil.hasRoot("a/b", "b")

        PathUtil.hasRoot("a/b/c", "a")
        !PathUtil.hasRoot("a/b/c", "arg")
        !PathUtil.hasRoot("arg/b/c", "a")
        !PathUtil.hasRoot("arg/b/c", "ar")
        PathUtil.hasRoot("arg/b/c", "arg")
        PathUtil.hasRoot("a/b/c", "a/")
        PathUtil.hasRoot("a/b/c", "a/b")
        PathUtil.hasRoot("a/b/c", "a/b/")
        PathUtil.hasRoot("a/b/c", "a/b/c")
        PathUtil.hasRoot("a/b/c", "a/b/c/")
        PathUtil.hasRoot("a/b/c/", "a/b/c/")

        !PathUtil.hasRoot("a/b/c/", "a/b/c/d")
        PathUtil.hasRoot("a/b/c/d", "a/b/c/d")
        PathUtil.hasRoot("a/b/c/d", "a/b/c/")
    }

    def "removePrefix removes a root prefix"() {
        expect:
        PathUtil.removePrefix("a", "a/b") == "b"
        PathUtil.removePrefix("a/b/", "a/b") == ""
        PathUtil.removePrefix("balogna/fooey/potato/partridge",
                "balogna/fooey/potato/partridge/lemonade/canister/buffalo") == "lemonade/canister/buffalo"
    }

    def "removePrefix doesn't change unprefixed path"() {
        expect:
        PathUtil.removePrefix("c", "a/b") == "a/b"
        PathUtil.removePrefix("parsnip/pencil", "a/b") == "a/b"
        PathUtil.removePrefix("balogna/fooey/potato/partridge",
                "lemonade/canister/buffalo/balogna/fooey/potato/partridge") == "lemonade/canister/buffalo/balogna" +
                "/fooey/potato/partridge"
    }

    static class CMTest implements ContentMeta {
        Map<String, String> meta
        InputStream content

        @Override
        InputStream getInputStream() throws IOException {
            return content
        }

        @Override
        long writeContent(OutputStream outputStream) throws IOException {
            return outputStream.write(content.bytes)
        }
    }

    def "single component resource selector string"(String selector, boolean matches) {
        def meta1 = new CMTest(meta: [a: 'alpha', x: 'wxyz', 'content-type': 'monkey'])
        expect:
        PathUtil.resourceSelector(selector).matchesContent(meta1) == matches
        where:
        selector                            | matches
        "*"                                 | true
        "a = alpha"                         | true
        "content-type = monkey"             | true
        "content-type = potato"             | false
        'content-type =~ ^(monkey|potato)$' | true
        'x =~ ^wx.*$'                       | true
    }

    def "multi component resource selector string"(String selector, boolean matches) {
        def meta1 = new CMTest(meta: [a: 'alpha', x: 'wxyz', 'content-type': 'monkey'])
        expect:
        PathUtil.resourceSelector(selector).matchesContent(meta1) == matches
        where:
        selector                                          | matches
        "a = alpha ; x =wxyz"                             | true
        "a = alpha ; x =~ .xy. ; content-type =~ monk.*"  | true
        "a = alphx ; x =~ .xy. ; content-type =~ monk.*"  | false
        "a = alpha ; x =~ .xy.. ; content-type =~ monk.*" | false
        "a = alpha ; x =~ .xy. ; content-type =~ zonk.*"  | false
    }

    def "regex selector"(Map<String, String> regexes, boolean matches) {
        def meta1 = new CMTest(meta: [a: 'alpha', x: 'wxyz', 'content-type': 'monkey'])
        expect:
        matches == PathUtil.regexMetadataResourceSelector(regexes, true).matchesContent(meta1)
        where:
        regexes                           | matches
        [a: ".*"]                         | true
        [a: "alpha"]                      | true
        [b: ".*"]                         | false
        [b: "beta"]                       | false
        [x: '^wx.*$']                     | true
        [x: '.*xy.*']                     | true
        [x: '.*yz$']                      | true
        ['content-type': 'monkey|potato'] | true
    }

    def "multi regex selector"(Map<String, String> regexes, boolean matches) {
        def meta1 = new CMTest(meta: [a: 'alpha', x: 'wxyz', 'content-type': 'monkey'])
        expect:
        matches == PathUtil.regexMetadataResourceSelector(regexes, true).matchesContent(meta1)
        where:
        regexes                                  | matches
        [a: ".*", x: ".*", 'content-type': '.*'] | true
        [a: ".*", x: "wxyz"]                     | true
        [a: ".lph.", 'content-type': "mon.*"]    | true
        [a: ".lph.", 'content-type': "Mon.*"]    | false
        [a: "Alph.", 'content-type': "mon.*"]    | false
    }

    def "equality selector"(Map<String, String> equals, boolean matches) {
        def meta1 = new CMTest(meta: [a: 'alpha', x: 'wxyz', 'content-type': 'monkey'])
        expect:
        matches == PathUtil.exactMetadataResourceSelector(equals, true).matchesContent(meta1)
        where:
        equals                            | matches
        [a: ".*"]                         | false
        [a: "alpha"]                      | true
        [b: ".*"]                         | false
        [b: "beta"]                       | false
        [x: '^wx.*$']                     | false
        [x: '.*xy.*']                     | false
        [x: '.*yz$']                      | false
        [x: 'wxyz']                       | true
        ['content-type': 'monkey|potato'] | false
        ['content-type': 'potato']        | false
        ['content-type': 'monkey']        | true
    }

    def "multi equality selector"(Map<String, String> equals, boolean matches) {
        def meta1 = new CMTest(meta: [a: 'alpha', x: 'wxyz', 'content-type': 'monkey'])
        expect:
        matches == PathUtil.exactMetadataResourceSelector(equals, true).matchesContent(meta1)
        where:
        equals                                            | matches
        [a: "alpha", x: 'wxyz', 'content-type': 'monkey'] | true
        [a: "not", x: 'wxyz', 'content-type': 'monkey']   | false
        [a: "alpha", x: 'not', 'content-type': 'monkey']  | false
        [a: "alpha", x: 'wxyz', 'content-type': 'not']    | false
        [a: "alpha", x: 'wxyz']                           | true
        [a: "alpha", x: 'not']                            | false
    }
}
