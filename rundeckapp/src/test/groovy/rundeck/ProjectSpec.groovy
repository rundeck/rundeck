package rundeck

import org.hibernate.Session
import org.hibernate.query.NativeQuery
import spock.lang.Specification

class ProjectSpec extends Specification {

    def "SQL Server project lock uses NOWAIT"() {
        given:
        def session = Mock(Session)
        def query = Mock(NativeQuery)
        def locked = new Project(id: 42L)

        when:
        def result = Project.lockSqlServer(session, 42L)

        then:
        1 * session.createNativeQuery(
            'SELECT * FROM project WITH (UPDLOCK, ROWLOCK, NOWAIT) WHERE id = :id',
            Project
        ) >> query
        1 * query.setParameter('id', 42L) >> query
        1 * query.uniqueResult() >> locked
        result.is(locked)
    }

    def "Oracle project lock uses bounded WAIT"() {
        given:
        def session = Mock(Session)
        def query = Mock(NativeQuery)
        def locked = new Project(id: 42L)

        when:
        def result = Project.lockOracle(session, 42L)

        then:
        1 * session.createNativeQuery(
            'SELECT * FROM project WHERE id = :id FOR UPDATE WAIT 5',
            Project
        ) >> query
        1 * query.setParameter('id', 42L) >> query
        1 * query.uniqueResult() >> locked
        result.is(locked)
    }
}
