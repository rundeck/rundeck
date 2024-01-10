package org.rundeck.plugins.jsch.util

import com.dtolabs.launcher.Setup
import com.dtolabs.rundeck.core.common.BaseFrameworkExecutionServices
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkFactory
import com.dtolabs.rundeck.core.common.IFrameworkServices
import com.dtolabs.rundeck.core.common.ServiceSupport
import com.dtolabs.rundeck.core.utils.FileUtils
import org.apache.tools.ant.BuildException

class JschTestUtil {
    public static String RDECK_BASE = System.getProperty("rdeck.base","build/rdeck_base");
    private static String PROJECTS_BASE = RDECK_BASE + "/" + "projects";
    public static final String localNodeHostname = "test1";

    private static String baseDir;
    public static String[] SETUP_ARGS = [
        "-n", localNodeHostname
    ]

    static Framework createTestFramework() {
        ServiceSupport serviceSupport = new ServiceSupport();
        BaseFrameworkExecutionServices services = new BaseFrameworkExecutionServices();
        serviceSupport.setExecutionServices(services);
        Framework framework = createTestFramework(serviceSupport);
        services.setFramework(framework);
        return framework;
    }

    static Framework createTestFramework(IFrameworkServices services) {
        if(!new File(RDECK_BASE).exists()) {
            configureFramework();
        }
        return FrameworkFactory.createForFilesystem(RDECK_BASE, services);
    }

    protected static void configureFramework()
            throws BuildException {

        baseDir = RDECK_BASE;
        if(new File(baseDir).exists()){
            FileUtils.deleteDir(new File(baseDir));
        }
        File projectsDir = new File(PROJECTS_BASE);
        FileUtils.deleteDir(projectsDir);
        projectsDir.mkdirs();
        new File(baseDir,"etc").mkdirs();
        File dummykey = new File(baseDir, "etc/dummy_ssh_key.pub");
        try {
            dummykey.createNewFile();
        } catch (IOException e) {
            throw new BuildException("failed to create dummy keyfile: " + e.getMessage(), e);
        }

        // check to see if Setup was run, if so, just return.
        if (new File(baseDir, "etc" + "/" + "framework.properties").exists()) {
            //System.out.println("Setup already run");
            return;
        }

        final ArrayList argsList = new ArrayList(Arrays.asList(SETUP_ARGS));
        argsList.add("--framework.ssh.keypath=" + dummykey.getAbsolutePath());
        argsList.add("-d");
        argsList.add(new File(baseDir).getAbsolutePath());


        try {
            Setup setup = new Setup();
            setup.execute((String[]) argsList.toArray(new String[argsList.size()]));
        } catch (Exception e) {
            throw new BuildException("Caught Setup exception: " + e.getMessage(), e);
        }

    }
}
