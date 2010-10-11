class FrameworkServiceTests extends GroovyTestCase {

    Properties props1
    void setUp(){
        props1=new Properties()
        props1.setProperty ("a.test","value for a")
        props1.setProperty ("b.test","value for b")
        props1.setProperty ("c.test",'embed a: ${a.test}')
        props1.setProperty ("d.test",'embed c: ${c.test}')
        props1.setProperty ("e.test",'embed 2 a: ${a.test}, ${a.test}')
        props1.setProperty ("f.test",'embed 1 a, 1 b, 1 c: ${a.test}, ${b.test}, ${c.test}')

    }
    void testPropertyUtil() {
        assert 6 == props1.size()
        assert "value for a"==props1.getProperty("a.test")
        assert 'embed a: ${a.test}' == props1.get("c.test")

        Properties props2 = com.dtolabs.rundeck.core.utils.PropertyUtil.expand(props1)

        //test that props without embedded ant refs are not modified
        String t1 = props2.getProperty('a.test')
        assertEquals("a.test result was not correct: ${t1}", 'value for a',t1)

        t1 = props2.getProperty('b.test')
        assertEquals("b.test result was not correct: ${t1}",'value for b',t1)

        //test non existent prop
        t1 = props2.getProperty('z.test')
        assertNull(t1)

        //test single embedded ant ref
        t1 = props2.getProperty('c.test')
        assertEquals("embeded a test is wrong: ${t1}",'embed a: value for a',t1)

        //test double embedded ant ref
        t1 = props2.getProperty('d.test')
        assertEquals("embeded c test is wrong: ${t1}",'embed c: embed a: value for a',t1)

        //test two embedded ant refs
        t1 = props2.getProperty('e.test')
        assertEquals("embeded 2 a test is wrong: ${t1}",'embed 2 a: value for a, value for a',t1)

        //test multiples
        t1 = props2.getProperty('f.test')
        assertEquals("embeded multiple test is wrong: ${t1}",'embed 1 a, 1 b, 1 c: value for a, value for b, embed a: value for a',t1)
    }
	void tearDown(){
        props1=null
    }

    void testParseOptsFromString(){
        def FrameworkService testService = new FrameworkService();

        test:{
            def m1 = testService.parseOptsFromString("-test 1")
            assertNotNull(m1)
            assertTrue(m1 instanceof Map<String, String>)
            assertLength(1, m1)
            assertNotNull(m1['test'])
            assertEquals("1", m1['test'])
        }
        test:{
            def m1 = testService.parseOptsFromString("-test 1 -test2 flamjamps")
            assertNotNull(m1)
            assertTrue(m1 instanceof Map<String, String>)
            assertEquals(2, m1.keySet().size())
            assertNotNull(m1['test'])
            assertEquals("1", m1['test'])
            assertNotNull(m1['test2'])
            assertEquals("flamjamps", m1['test2'])
        }
        test:{
            def m1 = testService.parseOptsFromString("-test 1 -test2 'flam jamps'")
            assertNotNull(m1)
            assertTrue(m1 instanceof Map<String, String>)
            assertEquals(2, m1.size())
            assertNotNull(m1['test'])
            assertEquals("1", m1['test'])
            assertNotNull(m1['test2'])
            assertEquals("flam jamps", m1['test2'])
        }
        test:{
            def m1 = testService.parseOptsFromString("-test 1 -test2 'flam jamps' notparsed")
            assertNotNull(m1)
            assertTrue(m1 instanceof Map<String, String>)
            assertEquals(2, m1.size())
            assertNotNull(m1['test'])
            assertEquals("1", m1['test'])
            assertNotNull(m1['test2'])
            assertEquals("flam jamps", m1['test2'])
        }
        test:{
            def m1 = testService.parseOptsFromString("-test 1 -test2 'flam jamps' notparsed -isboolean")
            assertNotNull(m1)
            assertTrue(m1 instanceof Map<String, String>)
            assertEquals("wrong size: "+m1,3, m1.size())
            assertNotNull(m1['test'])
            assertEquals("1", m1['test'])
            assertNotNull(m1['test2'])
            assertEquals("flam jamps", m1['test2'])
            assertNotNull(m1['isboolean'])
            assertEquals("true", m1['isboolean'])
        }
    }
}
