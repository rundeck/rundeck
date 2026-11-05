package rundeck.services

import spock.lang.Specification

class LocalProjectDeleteCoordinatorSpec extends Specification {

    def "only one local cleanup can own a project row"() {
        given:
        def coordinator = new LocalProjectDeleteCoordinator()

        when:
        String claim = coordinator.tryAcquire('project', 42L)

        then:
        claim
        coordinator.isOwner('project', 42L, claim)
        coordinator.tryAcquire('project', 42L) == null
    }

    def "release is fenced by the claim token"() {
        given:
        def coordinator = new LocalProjectDeleteCoordinator()
        String claim = coordinator.tryAcquire('project', 42L)

        when:
        coordinator.release('project', 42L, 'stale-token')

        then:
        coordinator.isOwner('project', 42L, claim)

        when:
        coordinator.release('project', 42L, claim)

        then:
        !coordinator.isOwner('project', 42L, claim)
        coordinator.tryAcquire('project', 42L)
    }
}
