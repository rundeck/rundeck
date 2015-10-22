package rundeck.services.logging

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Created by greg on 9/21/15.
 */
@RunWith(JUnit4)
class LargestNodeLinesCounterTest extends GroovyTestCase {
    @Test void testMax() {
        def test = new LargestNodeLinesCounter()
        Assert.assertEquals 0, test.value

        test.nodeLogged("a", 1)
        test.nodeLogged("a", 1)

        Assert.assertEquals 2, test.value

        test.nodeLogged("b", 1)
        test.nodeLogged("b", 1)

        Assert.assertEquals 2, test.value

        test.nodeLogged("a", 1)
        test.nodeLogged("a", 1)

        Assert.assertEquals 4, test.value

        test.nodeLogged("b", 1)
        test.nodeLogged("c", 1)

        Assert.assertEquals 4, test.value

        test.nodeLogged("b", 1)
        test.nodeLogged("b", 1)

        Assert.assertEquals 5, test.value

    }

    private Thread run(Closure clos) {
        def t = new Thread(clos)
        t.start()
        t
    }

    @Test void testMaxParallel() {
        def test = new LargestNodeLinesCounter()
        Assert.assertEquals 0, test.value

        [
                run {
                    test.nodeLogged("a", 1)
                    test.nodeLogged("a", 1)


                    test.nodeLogged("b", 1)
                    test.nodeLogged("b", 1)
                },

                run {
                    test.nodeLogged("a", 1)
                    test.nodeLogged("a", 1)


                    test.nodeLogged("b", 1)
                },
                run {
                    test.nodeLogged("c", 1)


                    test.nodeLogged("b", 1)
                    test.nodeLogged("b", 1)
                }
        ]*.join()

        Assert.assertEquals 5, test.value

    }
}
