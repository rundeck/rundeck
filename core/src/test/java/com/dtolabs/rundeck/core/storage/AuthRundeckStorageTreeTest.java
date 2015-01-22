package com.dtolabs.rundeck.core.storage;

import com.dtolabs.rundeck.core.authorization.Attribute;
import com.dtolabs.rundeck.core.authorization.AuthContext;
import com.dtolabs.rundeck.core.authorization.Decision;
import com.dtolabs.rundeck.core.authorization.Explanation;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rundeck.storage.api.PathUtil;
import org.rundeck.storage.api.Resource;
import org.rundeck.storage.api.StorageException;
import org.rundeck.storage.data.MemoryTree;

import javax.security.auth.Subject;
import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * AuthRundeckStorageTreeTest is ...
 *
 * @author greg
 * @since 2014-03-20
 */
@RunWith(JUnit4.class)
public class AuthRundeckStorageTreeTest {


    class testAuth implements AuthContext {
        Iterator<Decision> evaluateSingle;
        Set<Decision> evaluateMulti;
        Map<String, String> resource;
        String action;
        Set<Attribute> environment;

        Set<Map<String, String>> resources;
        Set<String> actions;
        Set<Attribute> environments;

        @Override
        public Decision evaluate(Map<String, String> resource, String action, Set<Attribute> environment) {
            this.resource = resource;
            this.action = action;
            this.environment = environment;
            return evaluateSingle.next();
        }

        @Override
        public Set<Decision> evaluate(Set<Map<String, String>> resources, Set<String> actions, Set<Attribute>
                environment) {
            this.resources = resources;
            this.actions = actions;
            this.environments = environment;
            return evaluateMulti;
        }
    }

    class mydecision implements Decision {
        boolean authorized;
        Map<String, String> resource;
        String action;
        Set<Attribute> environment;
        Subject subject;
        Explanation explanation;

        mydecision(boolean authorized) {
            this.authorized = authorized;
        }

        @Override
        public boolean isAuthorized() {
            return authorized;
        }

        @Override
        public Explanation explain() {
            return explanation;
        }

        @Override
        public long evaluationDuration() {
            return 0;
        }

        @Override
        public Map<String, String> getResource() {
            return resource;
        }

        @Override
        public String getAction() {
            return action;
        }

        @Override
        public Set<Attribute> getEnvironment() {
            return environment;
        }

        @Override
        public Subject getSubject() {
            return subject;
        }
    }


    @Test
    public void testEnvironmentForPath_applevel() {
        StorageTree testTree = StorageUtil.asStorageTree(new MemoryTree<ResourceMeta>());
        AuthRundeckStorageTree authRundeckStorageTree = new AuthRundeckStorageTree(testTree);
        Assert.assertEquals(Framework.RUNDECK_APP_ENV,
                authRundeckStorageTree.environmentForPath(PathUtil.asPath("test1")));
        Assert.assertEquals(Framework.RUNDECK_APP_ENV,
                authRundeckStorageTree.environmentForPath(PathUtil.asPath("project")));
        Assert.assertEquals(Framework.RUNDECK_APP_ENV,
                authRundeckStorageTree.environmentForPath(PathUtil.asPath("project/milkdud")));
    }

    @Test
    public void testEnvironmentForPath_projectLevel() {
        StorageTree testTree = StorageUtil.asStorageTree(new MemoryTree<ResourceMeta>());
        AuthRundeckStorageTree authRundeckStorageTree = new AuthRundeckStorageTree(testTree);
        Assert.assertEquals(FrameworkProject.authorizationEnvironment("milkdud"),
                authRundeckStorageTree.environmentForPath(PathUtil.asPath("project/milkdud/tooth")));
        Assert.assertEquals(FrameworkProject.authorizationEnvironment("milkdud"),
                authRundeckStorageTree.environmentForPath(PathUtil.asPath("project/milkdud/tooth/something/blah")));
    }
    @Test
    public void testGetPathNoRead_app() {
        StorageTree testTree = StorageUtil.asStorageTree(new MemoryTree<ResourceMeta>());
        AuthRundeckStorageTree authRundeckStorageTree = new AuthRundeckStorageTree(testTree);
        testAuth testAuth = new AuthRundeckStorageTreeTest.testAuth();
        testAuth.evaluateSingle = decisions(false);
        try {
            Resource<ResourceMeta> test1 = authRundeckStorageTree.getPath(testAuth, PathUtil.asPath("test1"));
            Assert.fail("expected failure");
        } catch (StorageException e) {

        }
        Assert.assertEquals("read", testAuth.action);
        Assert.assertNotNull(testAuth.environment);
        Assert.assertEquals(1, testAuth.environment.size());
        Assert.assertTrue(testAuth.environment.contains(Framework.RUNDECK_APP_CONTEXT));
        Assert.assertNotNull(testAuth.resource);
        Assert.assertEquals(3, testAuth.resource.size());
        Assert.assertEquals(AuthRundeckStorageTree.STORAGE_PATH_AUTH_RES_TYPE, testAuth.resource.get("type"));
        Assert.assertEquals("test1", testAuth.resource.get("path"));
        Assert.assertEquals("test1", testAuth.resource.get("name"));
    }
    @Test
    public void testGetPathNoRead_project() {
        StorageTree testTree = StorageUtil.asStorageTree(new MemoryTree<ResourceMeta>());
        AuthRundeckStorageTree authRundeckStorageTree = new AuthRundeckStorageTree(testTree);
        testAuth testAuth = new AuthRundeckStorageTreeTest.testAuth();
        testAuth.evaluateSingle = decisions(false);
        try {
            Resource<ResourceMeta> test1 = authRundeckStorageTree.getPath(testAuth,
                    PathUtil.asPath("project/milkdud/test1"));
            Assert.fail("expected failure");
        } catch (StorageException e) {

        }
        Assert.assertEquals("read", testAuth.action);
        Assert.assertNotNull(testAuth.environment);
        Assert.assertEquals(1, testAuth.environment.size());
        Assert.assertEquals(FrameworkProject.authorizationEnvironment("milkdud"), testAuth.environment);
        Assert.assertNotNull(testAuth.resource);
        Assert.assertEquals(3, testAuth.resource.size());
        Assert.assertEquals(AuthRundeckStorageTree.STORAGE_PATH_AUTH_RES_TYPE, testAuth.resource.get("type"));
        Assert.assertEquals("project/milkdud/test1", testAuth.resource.get("path"));
        Assert.assertEquals("test1", testAuth.resource.get("name"));
    }
    @Test
    public void testGetPathReadDir() {
        StorageTree testTree = StorageUtil.asStorageTree(new MemoryTree<ResourceMeta>());
        Resource<ResourceMeta> resource = testTree.createResource(PathUtil.asPath("project/milkdud/test1/file1"),
                StorageUtil.withStream(
                        new ByteArrayInputStream("asdf".getBytes()),
                        new HashMap<String, String>()
                ));

        AuthRundeckStorageTree authRundeckStorageTree = new AuthRundeckStorageTree(testTree);
        testAuth testAuth = new AuthRundeckStorageTreeTest.testAuth();
        testAuth.evaluateSingle = decisions(true);

        Resource<ResourceMeta> test1 = authRundeckStorageTree.getPath(testAuth,
                PathUtil.asPath("project/milkdud/test1"));
        Assert.assertNotNull(test1);
        Assert.assertTrue(test1.isDirectory());

        Assert.assertEquals("read", testAuth.action);
        Assert.assertNotNull(testAuth.environment);
        Assert.assertEquals(1, testAuth.environment.size());
        Assert.assertEquals(FrameworkProject.authorizationEnvironment("milkdud"), testAuth.environment);
        Assert.assertNotNull(testAuth.resource);
        Assert.assertEquals(3, testAuth.resource.size());
        Assert.assertEquals(AuthRundeckStorageTree.STORAGE_PATH_AUTH_RES_TYPE, testAuth.resource.get("type"));
        Assert.assertEquals("project/milkdud/test1", testAuth.resource.get("path"));
        Assert.assertEquals("test1", testAuth.resource.get("name"));
    }
    @Test
    public void testHasPathReadDir() {
        StorageTree testTree = StorageUtil.asStorageTree(new MemoryTree<ResourceMeta>());
        Resource<ResourceMeta> resource = testTree.createResource(PathUtil.asPath("project/milkdud/test1/file1"),
                StorageUtil.withStream(
                        new ByteArrayInputStream("asdf".getBytes()),
                        new HashMap<String, String>()
                ));

        AuthRundeckStorageTree authRundeckStorageTree = new AuthRundeckStorageTree(testTree);
        testAuth testAuth = new AuthRundeckStorageTreeTest.testAuth();
        testAuth.evaluateSingle = decisions(true);

        boolean test1 = authRundeckStorageTree.hasPath(testAuth,
                PathUtil.asPath("project/milkdud/test1"));
        Assert.assertTrue(test1);

        Assert.assertEquals("read", testAuth.action);
        Assert.assertNotNull(testAuth.environment);
        Assert.assertEquals(1, testAuth.environment.size());
        Assert.assertEquals(FrameworkProject.authorizationEnvironment("milkdud"), testAuth.environment);
        Assert.assertNotNull(testAuth.resource);
        Assert.assertEquals(3, testAuth.resource.size());
        Assert.assertEquals(AuthRundeckStorageTree.STORAGE_PATH_AUTH_RES_TYPE, testAuth.resource.get("type"));
        Assert.assertEquals("project/milkdud/test1", testAuth.resource.get("path"));
        Assert.assertEquals("test1", testAuth.resource.get("name"));
    }
    @Test
    public void testHasResourceReadDir() {
        StorageTree testTree = StorageUtil.asStorageTree(new MemoryTree<ResourceMeta>());
        Resource<ResourceMeta> resource = testTree.createResource(PathUtil.asPath("project/milkdud/test1/file1"),
                StorageUtil.withStream(
                        new ByteArrayInputStream("asdf".getBytes()),
                        new HashMap<String, String>()
                ));

        AuthRundeckStorageTree authRundeckStorageTree = new AuthRundeckStorageTree(testTree);
        testAuth testAuth = new AuthRundeckStorageTreeTest.testAuth();
        testAuth.evaluateSingle = decisions(true);

        boolean test1 = authRundeckStorageTree.hasResource(testAuth,
                PathUtil.asPath("project/milkdud/test1"));
        Assert.assertFalse(test1);

        Assert.assertEquals("read", testAuth.action);
        Assert.assertNotNull(testAuth.environment);
        Assert.assertEquals(1, testAuth.environment.size());
        Assert.assertEquals(FrameworkProject.authorizationEnvironment("milkdud"), testAuth.environment);
        Assert.assertNotNull(testAuth.resource);
        Assert.assertEquals(3, testAuth.resource.size());
        Assert.assertEquals(AuthRundeckStorageTree.STORAGE_PATH_AUTH_RES_TYPE, testAuth.resource.get("type"));
        Assert.assertEquals("project/milkdud/test1", testAuth.resource.get("path"));
        Assert.assertEquals("test1", testAuth.resource.get("name"));
    }
    @Test
    public void testHasDirectoryReadDir() {
        StorageTree testTree = StorageUtil.asStorageTree(new MemoryTree<ResourceMeta>());
        Resource<ResourceMeta> resource = testTree.createResource(PathUtil.asPath("project/milkdud/test1/file1"),
                StorageUtil.withStream(
                        new ByteArrayInputStream("asdf".getBytes()),
                        new HashMap<String, String>()
                ));

        AuthRundeckStorageTree authRundeckStorageTree = new AuthRundeckStorageTree(testTree);
        testAuth testAuth = new AuthRundeckStorageTreeTest.testAuth();
        testAuth.evaluateSingle = decisions(true);

        boolean test1 = authRundeckStorageTree.hasDirectory(testAuth,
                PathUtil.asPath("project/milkdud/test1"));
        Assert.assertTrue(test1);

        Assert.assertEquals("read", testAuth.action);
        Assert.assertNotNull(testAuth.environment);
        Assert.assertEquals(1, testAuth.environment.size());
        Assert.assertEquals(FrameworkProject.authorizationEnvironment("milkdud"), testAuth.environment);
        Assert.assertNotNull(testAuth.resource);
        Assert.assertEquals(3, testAuth.resource.size());
        Assert.assertEquals(AuthRundeckStorageTree.STORAGE_PATH_AUTH_RES_TYPE, testAuth.resource.get("type"));
        Assert.assertEquals("project/milkdud/test1", testAuth.resource.get("path"));
        Assert.assertEquals("test1", testAuth.resource.get("name"));
    }
    @Test
    public void testGetPathReadContent_unauthorized() {
        StorageTree testTree = StorageUtil.asStorageTree(new MemoryTree<ResourceMeta>());
        Resource<ResourceMeta> resource = testTree.createResource(PathUtil.asPath("project/milkdud/test1/file1"),
                StorageUtil.withStream(
                        new ByteArrayInputStream("asdf".getBytes()),
                        new HashMap<String, String>() {{
                            put("test1", "value1");
                        }}
                ));

        AuthRundeckStorageTree authRundeckStorageTree = new AuthRundeckStorageTree(testTree);
        testAuth testAuth = new AuthRundeckStorageTreeTest.testAuth();
        testAuth.evaluateSingle = decisions(false);

        Resource<ResourceMeta> test1 = null;
        try {
            test1 = authRundeckStorageTree.getPath(testAuth, PathUtil.asPath("project/milkdud/test1/file1"));
            Assert.fail("expected failure");
        } catch (StorageException e) {

        }

        Assert.assertEquals("read", testAuth.action);
        Assert.assertNotNull(testAuth.environment);
        Assert.assertEquals(1, testAuth.environment.size());
        Assert.assertEquals(FrameworkProject.authorizationEnvironment("milkdud"), testAuth.environment);
        Assert.assertNotNull(testAuth.resource);
        Assert.assertEquals(3, testAuth.resource.size());
        Assert.assertEquals(AuthRundeckStorageTree.STORAGE_PATH_AUTH_RES_TYPE, testAuth.resource.get("type"));
        Assert.assertEquals("project/milkdud/test1/file1", testAuth.resource.get("path"));
        Assert.assertEquals("file1", testAuth.resource.get("name"));
    }
    @Test
    public void testGetPathReadContent_authorized() {
        StorageTree testTree = StorageUtil.asStorageTree(new MemoryTree<ResourceMeta>());
        Resource<ResourceMeta> resource = testTree.createResource(PathUtil.asPath("project/milkdud/test1/file1"),
                StorageUtil.withStream(
                        new ByteArrayInputStream("asdf".getBytes()),
                        new HashMap<String, String>() {{
                            put("test1", "value1");
                        }}
                ));

        AuthRundeckStorageTree authRundeckStorageTree = new AuthRundeckStorageTree(testTree);
        testAuth testAuth = new AuthRundeckStorageTreeTest.testAuth();
        testAuth.evaluateSingle = decisions(true, true);

        Resource<ResourceMeta> test1 = null;
        test1 = authRundeckStorageTree.getPath(testAuth, PathUtil.asPath("project/milkdud/test1/file1"));

        Assert.assertNotNull(test1);
        Assert.assertFalse(test1.isDirectory());

        Assert.assertEquals("read", testAuth.action);
        Assert.assertNotNull(testAuth.environment);
        Assert.assertEquals(1, testAuth.environment.size());
        Assert.assertEquals(FrameworkProject.authorizationEnvironment("milkdud"), testAuth.environment);
        Assert.assertNotNull(testAuth.resource);
        Assert.assertEquals(3, testAuth.resource.size());
        Assert.assertEquals(AuthRundeckStorageTree.STORAGE_PATH_AUTH_RES_TYPE, testAuth.resource.get("type"));
        Assert.assertEquals("project/milkdud/test1/file1", testAuth.resource.get("path"));
        Assert.assertEquals("file1", testAuth.resource.get("name"));
    }
    @Test
    public void testHasPathReadContent_authorized() {
        StorageTree testTree = StorageUtil.asStorageTree(new MemoryTree<ResourceMeta>());
        Resource<ResourceMeta> resource = testTree.createResource(PathUtil.asPath("project/milkdud/test1/file1"),
                StorageUtil.withStream(
                        new ByteArrayInputStream("asdf".getBytes()),
                        new HashMap<String, String>() {{
                            put("test1", "value1");
                        }}
                ));

        AuthRundeckStorageTree authRundeckStorageTree = new AuthRundeckStorageTree(testTree);
        testAuth testAuth = new AuthRundeckStorageTreeTest.testAuth();
        testAuth.evaluateSingle = decisions(true, true);

        boolean test1 = authRundeckStorageTree.hasPath(testAuth, PathUtil.asPath("project/milkdud/test1/file1"));

        Assert.assertTrue(test1);

        Assert.assertEquals("read", testAuth.action);
        Assert.assertNotNull(testAuth.environment);
        Assert.assertEquals(1, testAuth.environment.size());
        Assert.assertEquals(FrameworkProject.authorizationEnvironment("milkdud"), testAuth.environment);
        Assert.assertNotNull(testAuth.resource);
        Assert.assertEquals(3, testAuth.resource.size());
        Assert.assertEquals(AuthRundeckStorageTree.STORAGE_PATH_AUTH_RES_TYPE, testAuth.resource.get("type"));
        Assert.assertEquals("project/milkdud/test1/file1", testAuth.resource.get("path"));
        Assert.assertEquals("file1", testAuth.resource.get("name"));
    }
    @Test
    public void testHasResourceReadContent_authorized() {
        StorageTree testTree = StorageUtil.asStorageTree(new MemoryTree<ResourceMeta>());
        Resource<ResourceMeta> resource = testTree.createResource(PathUtil.asPath("project/milkdud/test1/file1"),
                StorageUtil.withStream(
                        new ByteArrayInputStream("asdf".getBytes()),
                        new HashMap<String, String>() {{
                            put("test1", "value1");
                        }}
                ));

        AuthRundeckStorageTree authRundeckStorageTree = new AuthRundeckStorageTree(testTree);
        testAuth testAuth = new AuthRundeckStorageTreeTest.testAuth();
        testAuth.evaluateSingle = decisions(true, true);

        boolean test1 = authRundeckStorageTree.hasResource(testAuth, PathUtil.asPath("project/milkdud/test1/file1"));

        Assert.assertTrue(test1);

        Assert.assertEquals("read", testAuth.action);
        Assert.assertNotNull(testAuth.environment);
        Assert.assertEquals(1, testAuth.environment.size());
        Assert.assertEquals(FrameworkProject.authorizationEnvironment("milkdud"), testAuth.environment);
        Assert.assertNotNull(testAuth.resource);
        Assert.assertEquals(3, testAuth.resource.size());
        Assert.assertEquals(AuthRundeckStorageTree.STORAGE_PATH_AUTH_RES_TYPE, testAuth.resource.get("type"));
        Assert.assertEquals("project/milkdud/test1/file1", testAuth.resource.get("path"));
        Assert.assertEquals("file1", testAuth.resource.get("name"));
    }
    @Test
    public void testHasDirectoryReadContent_authorized() {
        StorageTree testTree = StorageUtil.asStorageTree(new MemoryTree<ResourceMeta>());
        Resource<ResourceMeta> resource = testTree.createResource(PathUtil.asPath("project/milkdud/test1/file1"),
                StorageUtil.withStream(
                        new ByteArrayInputStream("asdf".getBytes()),
                        new HashMap<String, String>() {{
                            put("test1", "value1");
                        }}
                ));

        AuthRundeckStorageTree authRundeckStorageTree = new AuthRundeckStorageTree(testTree);
        testAuth testAuth = new AuthRundeckStorageTreeTest.testAuth();
        testAuth.evaluateSingle = decisions(true, true);

        boolean test1 = authRundeckStorageTree.hasDirectory(testAuth, PathUtil.asPath("project/milkdud/test1/file1"));

        Assert.assertFalse(test1);

        Assert.assertEquals("read", testAuth.action);
        Assert.assertNotNull(testAuth.environment);
        Assert.assertEquals(1, testAuth.environment.size());
        Assert.assertEquals(FrameworkProject.authorizationEnvironment("milkdud"), testAuth.environment);
        Assert.assertNotNull(testAuth.resource);
        Assert.assertEquals(3, testAuth.resource.size());
        Assert.assertEquals(AuthRundeckStorageTree.STORAGE_PATH_AUTH_RES_TYPE, testAuth.resource.get("type"));
        Assert.assertEquals("project/milkdud/test1/file1", testAuth.resource.get("path"));
        Assert.assertEquals("file1", testAuth.resource.get("name"));
    }

    private Iterator<Decision> decisions(boolean... decisions) {
        ArrayList<Decision> d1 = new ArrayList<Decision>();
        for (boolean decision : decisions) {
            d1.add(new mydecision(decision));
        }
        return d1.iterator();
    }
}
