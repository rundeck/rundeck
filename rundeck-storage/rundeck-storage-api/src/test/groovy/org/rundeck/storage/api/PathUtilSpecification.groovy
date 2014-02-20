package org.rundeck.storage.api

import spock.lang.Specification

import static PathUtil.cleanPath
import static PathUtil.parentPathString
import static PathUtil.pathName

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 2/4/14
 * Time: 4:19 PM
 */
class PathUtilSpecification extends Specification {

    def "cleanPath removes trailing slashes"(){
        expect:
        cleanPath("abc")=="abc"
        cleanPath("abc/")=="abc"
        cleanPath("abc//")=="abc"
    }

    def "cleanPath removes leading slashes"(){
        expect:
        cleanPath("/abc")=="abc"
        cleanPath("//abc")=="abc"
    }
    def "cleanPath collapses multiple slashes"(){
        expect:
        cleanPath("abc//xyz")=="abc/xyz"
        cleanPath("abc//xyz////zyd")=="abc/xyz/zyd"
    }
    def "parentPath without parent returns null"(){
        expect:
        parentPathString("abc//")==null
        parentPathString("abc")==null
    }
    def "parentPath returns parent path"(){
        expect:
        parentPathString("abc//xyz")=="abc"
        parentPathString("abc//xyz/utiv")=="abc/xyz"
    }

    def "pathName returns last path component"(){
        expect:
        pathName("abc//xyz/utiv") == "utiv"
        pathName("abc//xyz/") == "xyz"
        pathName("abc///") == "abc"
        pathName("abc") == "abc"
    }

    def "hasRoot"() {
        expect:
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
}
