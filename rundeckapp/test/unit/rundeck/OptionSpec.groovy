package rundeck

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 11/4/15.
 */
class OptionSpec extends Specification {
    @Unroll("path #path valid #value")
    def "validate default storage path"() {
        given:
        def opt = new Option(name: 'bob', defaultStoragePath: path)
        expect:
        value == opt.validate(['defaultStoragePath'])

        where:
        path             | value
        'keys/blah'      | true
        'keys/blah/blah' | true
        'keys/'          | false
        'blah/toodles'   | false
    }

    def "from map default storage path"() {
        given:
        def opt = Option.fromMap('test', datamap)

        expect:
        value == opt.defaultStoragePath

        where:
        datamap                   | value
        [:]                       | null
        [storagePath: 'keys/abc'] | 'keys/abc'

    }

    def "to map default storage path"() {
        given:
        def opt = new Option(name: 'bob', defaultStoragePath: path)
        expect:
        path == opt.toMap().storagePath

        where:
        path       | _
        'keys/abc' | _
        null       | _
    }
}
