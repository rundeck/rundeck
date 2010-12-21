class ExecutionControllerTests extends GroovyTestCase {

    /**
     * Test parsing the output files.
     */
    void testParseOutput1() {
        def ec = new ExecutionController()
        assert ec != null
        def File tf1 = File.createTempFile("test.", "txt")
        tf1.deleteOnExit()
        def fos = new FileOutputStream(tf1)
        fos << """^^^11:17:34|SEVERE|greg|||localhost|Test.|error updating resources.properties: java.net.ConnectException: Connection refused^^^
^^^11:17:34|WARNING|greg|ATest|longTest|localhost|Test.ATest.something|Node localhost is sleeping for 15 seconds...^^^
^^^11:17:36|INFO|log4j:ERROR Could not connect to remote log4j server at [localhost]. We will try again later.^^^
^^^11:17:36|INFO|java.net.ConnectException: Connection refused^^^
^^^11:17:36|INFO|   at java.net.PlainSocketImpl.socketConnect(Native Method)^^^
^^^11:17:37|WARNING|greg||longTest|localhost|Test.ATest|Node localhost is sleeping for 15 seconds...^^^
^^^11:17:49|WARNING|greg|ATest|longTest|localhost|Test.ATest|Waking up localhost^^^
^^^11:17:52|WARNING|greg||longTest|localhost|Test.ATest|Waking up localhost^^^
^^^11:18:48|SEVERE|Command failed.
com.jcraft.jsch.JSchException: java.net.ConnectException: Operation timed out
^^^
^^^END^^^"""
        fos.close()
        def items = []
        def result = ec.parseOutput(tf1, 0, 0,null) {
            assert null != it
            items << it
            true
        }
        assertEquals "wrong items.size: "+items.size(), 9,items.size()
        def fsize = tf1.length()
        assertTrue "parsing should be completed", result.completed
        assertEquals "parsing should be completed", fsize, result.storeoffset

        //test partial metadata
        //11:17:34|SEVERE|greg|||localhost|Test.|error updating resources.properties: java.net.ConnectException: Connection refused
        assertEquals "incorrect time", "11:17:34", items[0].time
        assertEquals "incorrect level", "SEVERE", items[0].level
        assertEquals "incorrect user", "greg", items[0].user
        assertEquals "incorrect node", "localhost", items[0].node
        assertEquals "incorrect context", "Test.", items[0].context
        assertEquals "incorrect module", "", items[0].module
        assertEquals "incorrect command", "", items[0].command
        assertEquals "incorrect mesg:'${items[0].mesg}'", "error updating resources.properties: java.net.ConnectException: Connection refused\n", items[0].mesg

        //Test full metadata
        //11:17:34|WARNING|greg|ATest|longTest|localhost|Test.ATest|Node localhost is sleeping for 15 seconds...
        assertEquals "incorrect time", "11:17:34", items[1].time
        assertEquals "incorrect level", "WARNING", items[1].level
        assertEquals "incorrect user", "greg", items[1].user
        assertEquals "incorrect node", "localhost", items[1].node
        assertEquals "incorrect context", "Test.ATest.something", items[1].context
        assertEquals "incorrect module", "ATest", items[1].module
        assertEquals "incorrect command", "longTest", items[1].command
        assertEquals "incorrect mesg:'${items[1].mesg}'", "Node localhost is sleeping for 15 seconds...\n", items[1].mesg


        //test line without metadata
        //11:18:48|SEVERE|Command failed.\ncom.jcraft.jsch.JSchException: java.net.ConnectException: Operation timed out\n
        assertEquals "incorrect time", "11:18:48", items[8].time
        assertEquals "incorrect level", "SEVERE", items[8].level
        assertNull "user should be null", items[8].user
        assertNull "node should be null", items[8].node
        assertNull "context should be null", items[8].context
        assertNull "module should be null", items[8].module
        assertNull "command should be null", items[8].command
        assertEquals "incorrect mesg:'${items[8].mesg}'", "Command failed.\ncom.jcraft.jsch.JSchException: java.net.ConnectException: Operation timed out\n", items[8].mesg

    }
    void testParseOutput2() {
        def ec = new ExecutionController()
        assert ec != null
        def File tf1 = File.createTempFile("test.", "txt")
        tf1.deleteOnExit()
        def fos = new FileOutputStream(tf1)
        //no ^^^END^^^ at file end, and no ^^^ and end of final line.
        fos << """^^^11:17:49|WARNING|greg|ATest|longTest|localhost|Test.ATest|One Line
Another line
^^^
^^^11:17:49|SEVERE|bob|BTest|shortTest|somenode|Test.BTest|Waking up localhost
"""
        fos.close()
        def items = []
        def result = ec.parseOutput(tf1, 0, 0,null) {
            assert null != it
            items << it
            true
        }
        assertEquals "item size is not 1: " + items.size(), 1, items.size()
        assertFalse "parsing should not be completed", result.completed
        assertEquals "parsing should finish at start of second line", 87, result.storeoffset
        assertEquals "incorrect time", "11:17:49", items[0].time
        assertEquals "incorrect level", "WARNING", items[0].level
        assertEquals "incorrect user", "greg", items[0].user
        assertEquals "incorrect node", "localhost", items[0].node
        assertEquals "incorrect context", "Test.ATest", items[0].context
        assertEquals "incorrect module", "ATest", items[0].module
        assertEquals "incorrect command", "longTest", items[0].command
        assertEquals "incorrect mesg:'${items[0].mesg}'", "One Line\nAnother line\n", items[0].mesg



        fos = new FileOutputStream(tf1)
        //no ^^^END^^^ at file end
        fos << """^^^11:17:49|WARNING|greg|ATest|longTest|localhost|Test.ATest|One Line
Another line
^^^
^^^11:17:49|SEVERE|bob|BTest|shortTest|somenode|Test.BTest|Waking up localhost^^^
"""
        fos.close()
        items = []
        result = ec.parseOutput(tf1, 0, 0,null) {
            assert null != it
            items << it
            true
        }
        assertFalse "parsing should not be completed", result.completed
        assertEquals "parsing should have read entire file", 169, result.storeoffset
        assert 2 == items.size()
        assertEquals "incorrect time", "11:17:49", items[0].time
        assertEquals "incorrect level", "WARNING", items[0].level
        assertEquals "incorrect user", "greg", items[0].user
        assertEquals "incorrect node", "localhost", items[0].node
        assertEquals "incorrect context", "Test.ATest", items[0].context
        assertEquals "incorrect module", "ATest", items[0].module
        assertEquals "incorrect command", "longTest", items[0].command
        assertEquals "incorrect mesg:'${items[0].mesg}'", "One Line\nAnother line\n", items[0].mesg



        items = []
        //parse starting at offset point of 1 byte, which should cause jump to next valid line
        result = ec.parseOutput(tf1, 1, 0,null) {
            assert null != it
            items << it
            true
        }

        assertFalse "parsing should not be completed", result.completed
        assertEquals "parsing should have read entire file", 169, result.storeoffset
        assert 1 == items.size()
        assertEquals "incorrect time", "11:17:49", items[0].time
        assertEquals "incorrect level", "SEVERE", items[0].level
        assertEquals "incorrect user", "bob", items[0].user
        assertEquals "incorrect node", "somenode", items[0].node
        assertEquals "incorrect context", "Test.BTest", items[0].context
        assertEquals "incorrect module", "BTest", items[0].module
        assertEquals "incorrect command", "shortTest", items[0].command
        assertEquals "incorrect mesg:'${items[0].mesg}'", "Waking up localhost\n", items[0].mesg


        items = []
        //parse starting at offset point of 87 bytes, at start of second message
        result = ec.parseOutput(tf1, 87, 0,null) {
            assert null != it
            items << it
            true
        }

        assert 1 == items.size()
        assertFalse "parsing should not be completed", result.completed
        assertEquals "parsing should have read entire file", 169, result.storeoffset
        assertEquals "incorrect time", "11:17:49", items[0].time
        assertEquals "incorrect level", "SEVERE", items[0].level
        assertEquals "incorrect user", "bob", items[0].user
        assertEquals "incorrect node", "somenode", items[0].node
        assertEquals "incorrect context", "Test.BTest", items[0].context
        assertEquals "incorrect module", "BTest", items[0].module
        assertEquals "incorrect command", "shortTest", items[0].command
        assertEquals "incorrect mesg:'${items[0].mesg}'", "Waking up localhost\n", items[0].mesg


        items = []
        //parse starting at offset point of 88 bytes, which should cause no items to be parsed
        result = ec.parseOutput(tf1, 88, 0,null) {
            assert null != it
            items << it
            true
        }

        assertFalse "parsing should not be completed", result.completed
        assertEquals "parsing should have stayed at starting ofset", 88, result.storeoffset
        assert 0 == items.size()

        items = []
        //parse with only 86 bytes buffer, which should cause the second line to not be parsed
        result = ec.parseOutput(tf1, 0, 86,null) {
            assert null != it
            items << it
            true
        }
        assertFalse "parsing should not be completed", result.completed
        assertEquals "parsing should have stopped at beginning of second line", 87, result.storeoffset
        assert 1 == items.size()
        assertEquals "incorrect time", "11:17:49", items[0].time
        assertEquals "incorrect level", "WARNING", items[0].level
        assertEquals "incorrect user", "greg", items[0].user
        assertEquals "incorrect node", "localhost", items[0].node
        assertEquals "incorrect context", "Test.ATest", items[0].context
        assertEquals "incorrect module", "ATest", items[0].module
        assertEquals "incorrect command", "longTest", items[0].command
        assertEquals "incorrect mesg:'${items[0].mesg}'", "One Line\nAnother line\n", items[0].mesg
    }
     void testParseOutput_extended() {
        def ec = new ExecutionController()
        assert ec != null
        def File tf1 = File.createTempFile("test.", "txt")
        tf1.deleteOnExit()
        def fos = new FileOutputStream(tf1)
        //add extra | character in output message.
        fos << """^^^11:17:49|WARNING|greg|ATest|longTest|localhost|Test.ATest|One Line|extra^^^"""
        fos.close()
        def items = []
        def result = ec.parseOutput(tf1, 0, 0,null) {
            assert null != it
            items << it
            true
        }
        assert 1 == items.size()
        assertEquals "incorrect time", "11:17:49", items[0].time
        assertEquals "incorrect level", "WARNING", items[0].level
        assertEquals "incorrect user", "greg", items[0].user
        assertEquals "incorrect node", "localhost", items[0].node
        assertEquals "incorrect context", "Test.ATest", items[0].context
        assertEquals "incorrect module", "ATest", items[0].module
        assertEquals "incorrect command", "longTest", items[0].command
        assertEquals "incorrect mesg:'${items[0].mesg}'", "One Line|extra\n", items[0].mesg

         fos = new FileOutputStream(tf1)
        //add extra | character in output message, without metadata
        fos << """^^^11:17:49|WARNING|One Line|extra^^^"""
        fos.close()
        items = []
        result = ec.parseOutput(tf1, 0, 0,null) {
            assert null != it
            items << it
            true
        }
        assert 1 == items.size()
        assertEquals "incorrect time", "11:17:49", items[0].time
        assertEquals "incorrect level", "WARNING", items[0].level
        assertNull "incorrect user", items[0].user
        assertNull "incorrect node", items[0].node
        assertNull "incorrect context", items[0].context
        assertNull "incorrect module", items[0].module
        assertNull "incorrect command", items[0].command
        assertEquals "incorrect mesg:'${items[0].mesg}'", "One Line|extra\n", items[0].mesg
     }
     /**
      * Test handling whitespace inside context parts of the file
      */
     void testParseOutput_whitespace() {
        def ec = new ExecutionController()
        assert ec != null
        def File tf1 = File.createTempFile("test.", "txt")
        tf1.deleteOnExit()
        def fos = new FileOutputStream(tf1)
        //add extra whitespace to context items
        fos << """^^^11:17:49|WARNING|greg|ATest|longTest|localhost  |Test.ATest|Stuff^^^"""
        fos.close()
        def items = []
        def result = ec.parseOutput(tf1, 0, 0,null) {
            assert null != it
            items << it
            true
        }
        assert 1 == items.size()
        assertEquals "incorrect time", "11:17:49", items[0].time
        assertEquals "incorrect level", "WARNING", items[0].level
        assertEquals "incorrect user", "greg", items[0].user
        assertEquals "incorrect node", "localhost", items[0].node
        assertEquals "incorrect context", "Test.ATest", items[0].context
        assertEquals "incorrect module", "ATest", items[0].module
        assertEquals "incorrect command", "longTest", items[0].command
        assertEquals "incorrect mesg:'${items[0].mesg}'", "Stuff\n", items[0].mesg

        fos = new FileOutputStream(tf1)
        //add extra | character in output message, without metadata
        fos << """^^^11:17:49|WARNING|greg|ATest|longTest|  localhost|Test.ATest|Stuff^^^"""
        fos.close()
        items = []
        result = ec.parseOutput(tf1, 0, 0,null) {
            assert null != it
            items << it
            true
        }
        assert 1 == items.size()
        assertEquals "incorrect time", "11:17:49", items[0].time
        assertEquals "incorrect level", "WARNING", items[0].level
        assertEquals "incorrect user", "greg", items[0].user
        assertEquals "incorrect node", "localhost", items[0].node
        assertEquals "incorrect context", "Test.ATest", items[0].context
        assertEquals "incorrect module", "ATest", items[0].module
        assertEquals "incorrect command", "longTest", items[0].command
        assertEquals "incorrect mesg:'${items[0].mesg}'", "Stuff\n", items[0].mesg

        fos = new FileOutputStream(tf1)
        //add extra | character in output message, without metadata
        fos << """^^^11:17:49|WARNING|greg|ATest|  longTest  |localhost|Test.ATest|Stuff^^^"""
        fos.close()
        items = []
        result = ec.parseOutput(tf1, 0, 0,null) {
            assert null != it
            items << it
            true
        }
        assert 1 == items.size()
        assertEquals "incorrect time", "11:17:49", items[0].time
        assertEquals "incorrect level", "WARNING", items[0].level
        assertEquals "incorrect user", "greg", items[0].user
        assertEquals "incorrect node", "localhost", items[0].node
        assertEquals "incorrect context", "Test.ATest", items[0].context
        assertEquals "incorrect module", "ATest", items[0].module
        assertEquals "incorrect command", "longTest", items[0].command
        assertEquals "incorrect mesg:'${items[0].mesg}'", "Stuff\n", items[0].mesg

        fos = new FileOutputStream(tf1)
        //add extra | character in output message, without metadata
        fos << """^^^  11:17:49  |  WARNING | greg | ATest |  longTest  |   localhost | Test.ATest  |Stuff^^^"""
        fos.close()
        items = []
        result = ec.parseOutput(tf1, 0, 0,null) {
            assert null != it
            items << it
            true
        }
        assert 1 == items.size()
        assertEquals "incorrect time", "11:17:49", items[0].time
        assertEquals "incorrect level", "WARNING", items[0].level
        assertEquals "incorrect user", "greg", items[0].user
        assertEquals "incorrect node", "localhost", items[0].node
        assertEquals "incorrect context", "Test.ATest", items[0].context
        assertEquals "incorrect module", "ATest", items[0].module
        assertEquals "incorrect command", "longTest", items[0].command
        assertEquals "incorrect mesg:'${items[0].mesg}'", "Stuff\n", items[0].mesg
     }

    void testParseOutputUTF8() {
        def ec = new ExecutionController()
        assert ec != null
        def File tf1 = File.createTempFile("test.", "txt")
        tf1.deleteOnExit()
        def fos = new OutputStreamWriter(new FileOutputStream(tf1),"UTF-8")
        //File content contains UTF8, assert that the read result is correct
        fos << """^^^03:21:50|INFO|admin|||centos5||/bin/sh: httpd: ??????? ?? ???????^^^
^^^03:21:51|SEVERE|Execution failed on the following 1 nodes: [centos5]^^^
^^^END^^^"""
        fos.close()
        def items = []
        def result = ec.parseOutput(tf1, 0, 0,"UTF-8") {
            assert null != it
            items << it
            true
        }
        assertEquals "item size is not 1: " + items.size(), 2, items.size()
        assertTrue "parsing should be completed", result.completed
        assertEquals "parsing should finish with correct offset: "+result.storeoffset, 156, result.storeoffset
        assertEquals "incorrect mesg:'${items[0].mesg}'", "/bin/sh: httpd: ??????? ?? ???????\n", items[0].mesg


    }
    void testParseOutputUTF16() {
        def ec = new ExecutionController()
        assert ec != null
        def File tf1 = File.createTempFile("test.", "txt")
        tf1.deleteOnExit()
        def fos = new OutputStreamWriter(new FileOutputStream(tf1),"UTF-16")
        //File content contains UTF8, assert that the read result is correct
        fos << """^^^03:21:50|INFO|admin|||centos5||/bin/sh: httpd: ??????? ?? ???????^^^
^^^03:21:51|SEVERE|Execution failed on the following 1 nodes: [centos5]^^^
^^^END^^^"""
        fos.close()
        def items = []
        def result = ec.parseOutput(tf1, 0, 0,"UTF-16") {
            assert null != it
            items << it
            true
        }
        assertEquals "item size is not 1: " + items.size(), 2, items.size()
        assertTrue "parsing should be completed", result.completed
        assertEquals "parsing should finish with correct offset: "+result.storeoffset, 314, result.storeoffset
        assertEquals "incorrect mesg:'${items[0].mesg}'", "/bin/sh: httpd: ??????? ?? ???????\n", items[0].mesg


    }

}
