package com.dtolabs.rundeck.core.dispatcher;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * $INTERFACE is ... User: greg Date: 7/1/13 Time: 3:56 PM
 */
@RunWith(JUnit4.class)
public class ReplaceTokenReaderTest {

    @Test
    public void tokenExpand() throws IOException {
        test("a value", "@value1@", defaultTokens(), true, '@', '@');
    }
    @Test
    public void multitokenExpand() throws IOException {
        test("a value, b value, c value", "@value1@, @value2@, @value3@", defaultTokens(), true, '@', '@');
    }
    @Test
    public void replaceWithBlank() throws IOException {
        test("a valuec value", "@value1@@valueDNE@@value3@", defaultTokens(), true, '@', '@');
    }
    @Test
    public void dontReplaceWithBlank() throws IOException {
        test("a value@valueDNE@c value", "@value1@@valueDNE@@value3@", defaultTokens(), false, '@', '@');
    }

    @Test
    public void differentTokens() throws IOException {
        test("a value, b value, c value", "@value1$, @value2$, @value3$", defaultTokens(), false, '@', '$');
    }
    @Test
    public void noEndToken() throws IOException {
        test("@value1monkeypotato", "@value1monkeypotato", defaultTokens(), false, '@', '@');
    }
    @Test
    public void noFirstStartToken() throws IOException {
        test("prefixed text value, c value", "prefixed text value, @value3@", defaultTokens(), true, '@', '@');
    }
    @Test
    public void noFinalEndToken() throws IOException {
        test("prefixed text value, c value, final tet", "prefixed text value, @value3@, final tet", defaultTokens(), true, '@', '@');
    }
    @Test
    public void script() throws IOException {
        test("test script some data this is a test\n" +
                "test line 2 some data @test.data2@\n",
                "test script some data @test.data@\n" +
                "test line 2 some data @test.data2@\n", scriptTokens(), false, '@', '@');
    }
    @Test
    public void requireTokenChars() throws IOException {
        test("test script some data @test.data\n" +
                "test line 2 some data \n",
                "test script some data @test.data\n" +
                "test line 2 some data @test.data2@\n", scriptTokens(), true, '@', '@');
    }
    @Test
    public void resumeTokenChars() throws IOException {
        test("test script some data \n" +
                "test line 2 some data this is a test\n",
                "test script some data @test.data:@\n" +
                "test line 2 some data @test.data@\n", scriptTokens(), true, '@', '@');
    }
    @Test
    public void allowedTokenChars() throws IOException {
        Map<String, String> tokens = scriptTokens();
        tokens.put("test.some:data", "a value");
        test("test script some data a value\n" +
                "test line 2 some data \n",
                "test script some data @test.some:data@\n" +
                        "test line 2 some data @test.other:data@\n", tokens, true, '@', '@');
    }
    @Test
    public void readLineDontReplaceWithBlank() throws IOException {

        ReplaceTokenReader replaceTokenReader = new ReplaceTokenReader(new StringReader("test script some data @test.data@\n" +
                "test line 2 some data @test.data2@\n"), scriptTokens(),
                false, '@', '@');
        BufferedReader bufferedReader = new BufferedReader(replaceTokenReader);
        Assert.assertEquals("test script some data this is a test", bufferedReader.readLine());
        Assert.assertEquals("test line 2 some data @test.data2@", bufferedReader.readLine());
        Assert.assertNull(bufferedReader.readLine());
    }
    @Test
    public void readLineReplaceWithBlank() throws IOException {

        ReplaceTokenReader replaceTokenReader = new ReplaceTokenReader(new StringReader("test script some data @test.data@\n" +
                "test line 2 some data @test.data2@\n"), scriptTokens(),
                true, '@', '@');
        BufferedReader bufferedReader = new BufferedReader(replaceTokenReader);
        Assert.assertEquals("test script some data this is a test", bufferedReader.readLine());
        Assert.assertEquals("test line 2 some data ", bufferedReader.readLine());
        Assert.assertNull(bufferedReader.readLine());
    }

    private Map<String, String> defaultTokens() {
        HashMap<String, String> stringStringHashMap = new HashMap<String, String>();
        stringStringHashMap.put("value1", "a value");
        stringStringHashMap.put("value2", "b value");
        stringStringHashMap.put("value3", "c value");
        return stringStringHashMap;
    }
    private Map<String, String> scriptTokens() {
        HashMap<String, String> stringStringHashMap = new HashMap<String, String>();
        stringStringHashMap.put("test.data", "this is a test");
        return stringStringHashMap;
    }

    public void test(String expected, String input, Map<String, String> tokens, boolean blankIfMissing, char tokenstart, char tokenend) throws IOException {

        ReplaceTokenReader replaceTokenReader = new ReplaceTokenReader(new StringReader(input), tokens,
                blankIfMissing, tokenstart, tokenend);
        StringWriter writer = new StringWriter();
        writeReader(replaceTokenReader, writer);
        Assert.assertEquals(expected, writer.toString());
    }

    static void writeReader(final Reader reader, final Writer writer) throws IOException {
        int inData;
        while ((inData = reader.read()) != -1) {
            writer.write(inData);
        }
        reader.close();
    }
}
