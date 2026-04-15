package rundeck.data.validation.shared

import spock.lang.Specification
import spock.lang.Unroll

class SharedJobConstraintsSpec extends Specification {

    @Unroll
    def "groupPath rejects path traversal: #invalidPath"() {
        given:
        def constraints = new SharedJobConstraints(groupPath: invalidPath)

        when:
        def valid = constraints.validate(['groupPath'])

        then:
        !valid
        constraints.errors.hasFieldErrors('groupPath')
        constraints.errors.getFieldError('groupPath').codes.any { it.contains('invalid.path.traversal') }

        where:
        invalidPath << [
            '../etc',
            '../../tmp',
            'valid/../invalid',
            '../../../../../tmp',
            '..\\windows',
            '..\\..\\system32',
            'group/./hidden',
            './relative'
        ]
    }

    @Unroll
    def "groupPath rejects absolute paths: #invalidPath"() {
        given:
        def constraints = new SharedJobConstraints(groupPath: invalidPath)

        when:
        def valid = constraints.validate(['groupPath'])

        then:
        !valid
        constraints.errors.hasFieldErrors('groupPath')
        constraints.errors.getFieldError('groupPath').codes.any { it.contains('invalid.absolute.path') }

        where:
        invalidPath << [
            '/absolute/path',
            '/etc/passwd',
            '\\windows\\path',
            'C:\\Windows\\System32',
            'C:/Program Files',
            'D:\\etc\\passwd',
            'Z:/secret'
        ]
    }

    @Unroll
    def "groupPath allows valid paths: #validPath"() {
        given:
        def constraints = new SharedJobConstraints(groupPath: validPath)

        when:
        def valid = constraints.validate(['groupPath'])

        then:
        valid
        !constraints.errors.hasFieldErrors('groupPath')

        where:
        validPath << [
            null,
            '',
            'mygroup',
            'group/subgroup',
            'team_ops',
            'prod-servers',
            'group/nested/deep',
            'team-1/subteam-2',
            'The Group',
            'my group',
            'v1.0/deploy',
            'group with spaces/sub group',
            'release.2026'
        ]
    }

    def "groupPath enforces maxSize constraint"() {
        given:
        def longPath = 'a' * 2049
        def constraints = new SharedJobConstraints(groupPath: longPath)

        when:
        def valid = constraints.validate(['groupPath'])

        then:
        !valid
        constraints.errors.hasFieldErrors('groupPath')
    }
}
