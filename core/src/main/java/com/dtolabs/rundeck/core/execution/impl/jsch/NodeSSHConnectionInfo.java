package com.dtolabs.rundeck.core.execution.impl.jsch;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.IRundeckProject;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.utils.ResolverUtil;
import com.dtolabs.rundeck.core.storage.ResourceMeta;
import com.dtolabs.rundeck.core.tasks.net.SSHTaskBuilder;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
* Created by greg on 3/19/15.
*/
final class NodeSSHConnectionInfo implements SSHTaskBuilder.SSHConnectionInfo {
    final INodeEntry node;
    final Framework framework;
    final ExecutionContext context;
    IRundeckProject frameworkProject;

    NodeSSHConnectionInfo(final INodeEntry node, final Framework framework, final ExecutionContext context) {

        this.node = node;
        this.framework = framework;
        this.context = context;
        this.frameworkProject = framework.getFrameworkProjectMgr().getFrameworkProject(
            context.getFrameworkProject());
    }

    public SSHTaskBuilder.AuthenticationType getAuthenticationType() {
        String authType=resolve(JschNodeExecutor.NODE_ATTR_SSH_AUTHENTICATION);
        if (null != authType) {
            return SSHTaskBuilder.AuthenticationType.valueOf(authType);
        }
        return SSHTaskBuilder.AuthenticationType.privateKey;
    }

    public String getPrivateKeyfilePath() {
        String path = resolve(JschNodeExecutor.NODE_ATTR_SSH_KEYPATH);
        if (path == null && framework.hasProperty(Constants.SSH_KEYPATH_PROP)) {
            //return default framework level
            path = framework.getProperty(Constants.SSH_KEYPATH_PROP);
        }
        //expand properties in path
        if (path != null && path.contains("${")) {
            path = DataContextUtils.replaceDataReferences(path, context.getDataContext());
        }
        return path;
    }

    public InputStream getPrivateKeyStorageData() throws IOException {
        String privateKeyResourcePath = getPrivateKeyStoragePath();
        if (null == privateKeyResourcePath) {
            return null;
        }
        return context
                .getStorageTree()
                .getResource(privateKeyResourcePath)
                .getContents()
                .getInputStream();
    }

    public byte[] getPasswordStorageData() throws IOException{
        return loadStoragePathData(getPasswordStoragePath());
    }

    private byte[] loadStoragePathData(final String passwordStoragePath) throws IOException {
        if (null == passwordStoragePath) {
            return null;
        }
        ResourceMeta contents = context.getStorageTree().getResource(passwordStoragePath).getContents();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        contents.writeContent(byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    public String getPrivateKeyStoragePath() {
        String path = resolve(JschNodeExecutor.NODE_ATTR_SSH_KEY_RESOURCE);
        if (null == path && framework.hasProperty(Constants.SSH_KEYRESOURCE_PROP)) {
            //return default framework level
            path = framework.getProperty(Constants.SSH_KEYRESOURCE_PROP);
        }
        //expand properties in path
        if (path != null && path.contains("${")) {
            path = DataContextUtils.replaceDataReferences(path, context.getDataContext());
        }
        return path;
    }
    public String getPasswordStoragePath() {
        String path = resolve(JschNodeExecutor.NODE_ATTR_SSH_PASSWORD_STORAGE_PATH);

        //expand properties in path
        if (path != null && path.contains("${")) {
            path = DataContextUtils.replaceDataReferences(path, context.getDataContext());
        }
        return path;
    }
    @Override
    public String getSudoPasswordStoragePath(String prefix) {
        String path = resolve(prefix + JschNodeExecutor.NODE_ATTR_SUDO_PASSWORD_STORAGE_PATH);
        //expand properties in path
        if (path != null && path.contains("${")) {
            path = DataContextUtils.replaceDataReferences(path, context.getDataContext());
        }
        return path;
    }
    @Override
    public byte[] getSudoPasswordStorageData(String prefix) throws IOException{
        return loadStoragePathData(getSudoPasswordStoragePath(prefix));
    }

    public String getSudoPassword(String prefix) {
        String opt = resolve(prefix + JschNodeExecutor.NODE_ATTR_SUDO_PASSWORD_OPTION);
        if (null != opt) {
            return evaluateSecureOption(opt, context);
        } else {
            return evaluateSecureOption(
                    JschNodeExecutor.SUDO_OPT_PREFIX.equals(prefix)
                    ? JschNodeExecutor.DEFAULT_SUDO_PASSWORD_OPTION
                    : JschNodeExecutor.DEFAULT_SUDO2_PASSWORD_OPTION,
                    context
            );
        }

    }

    public String getPrivateKeyPassphrase() {
        String opt = resolve(JschNodeExecutor.NODE_ATTR_SSH_KEY_PASSPHRASE_OPTION);
        if (null != opt) {
            return evaluateSecureOption(opt, context);
        } else {
            return evaluateSecureOption(JschNodeExecutor.DEFAULT_SSH_KEY_PASSPHRASE_OPTION, context);
        }

    }
    @Override
    public String getPrivateKeyPassphraseStoragePath() {
        String path = resolve(JschNodeExecutor.NODE_ATTR_SSH_KEY_PASSPHRASE_STORAGE_PATH);
        //expand properties in path
        if (path != null && path.contains("${")) {
            path = DataContextUtils.replaceDataReferences(path, context.getDataContext());
        }
        return path;
    }

    private String resolve(final String propName) {
        return ResolverUtil.resolveProperty(propName, null, node, frameworkProject, framework);
    }

    @Override
    public byte[] getPrivateKeyPassphraseStorageData() throws IOException{
        return loadStoragePathData(getPrivateKeyPassphraseStoragePath());
    }

    static String evaluateSecureOption(final String optionName, final ExecutionContext context) {
        if (null == optionName) {
            JschNodeExecutor.logger.debug("option name was null");
            return null;
        }
        if (null == context.getPrivateDataContext()) {
            JschNodeExecutor.logger.debug("private context was null");
            return null;
        }
        final String[] opts = optionName.split("\\.", 2);
        if (null != opts && 2 == opts.length) {
            final Map<String, String> option = context.getPrivateDataContext().get(opts[0]);
            if (null != option) {
                final String value = option.get(opts[1]);
                if (null == value) {
                    JschNodeExecutor.logger.debug("private context '" + optionName + "' was null");
                }
                return value;
            } else {
                JschNodeExecutor.logger.debug("private context '" + opts[0] + "' was null");
            }
        }
        return null;
    }


    public String getPassword() {
        String opt = resolve(JschNodeExecutor.NODE_ATTR_SSH_PASSWORD_OPTION);
        if (null != opt) {
            return evaluateSecureOption(opt, context);
        } else {
            return evaluateSecureOption(JschNodeExecutor.DEFAULT_SSH_PASSWORD_OPTION, context);
        }
    }

    public int getSSHTimeout() {
        int timeout = 0;
        if (framework.getPropertyLookup().hasProperty(Constants.SSH_TIMEOUT_PROP)) {
            final String val = framework.getProperty(Constants.SSH_TIMEOUT_PROP);
            try {
                timeout = Integer.parseInt(val);
            } catch (NumberFormatException e) {
            }
        }
        return timeout;
    }

    /**
     * Return null if the input is null or empty or whitespace, otherwise return the input string trimmed.
     */
    public static String nonBlank(final String input) {
        if (null == input || "".equals(input.trim())) {
            return null;
        } else {
            return input.trim();
        }
    }

    public String getUsername() {
        String user;
        if (null != nonBlank(node.getUsername()) || node.containsUserName()) {
            user = nonBlank(node.extractUserName());
        } else if (frameworkProject.hasProperty(JschNodeExecutor.PROJECT_SSH_USER)
                   && null != nonBlank(frameworkProject.getProperty(JschNodeExecutor.PROJECT_SSH_USER))) {
            user = nonBlank(frameworkProject.getProperty(JschNodeExecutor.PROJECT_SSH_USER));
        } else {
            user = nonBlank(framework.getProperty(Constants.SSH_USER_PROP));
        }
        if (null != user && user.contains("${")) {
            return DataContextUtils.replaceDataReferences(user, context.getDataContext());
        }
        return user;
    }

    public Boolean getLocalSSHAgent() {
        return ResolverUtil.resolveBooleanProperty(
                JschNodeExecutor.NODE_ATTR_LOCAL_SSH_AGENT,
                false,
                node,
                frameworkProject,
                framework
        );
    }

    public Integer getTtlSSHAgent() {
        return ResolverUtil.resolveIntProperty(
                JschNodeExecutor.NODE_ATTR_LOCAL_TTL_SSH_AGENT,
                0,
                node,
                frameworkProject,
                framework
        );
    }

    public static Map<String, String> sshConfigFromFramework(Framework framework) {
        HashMap<String, String> config = new HashMap<String, String>();
        IPropertyLookup propertyLookup = framework.getPropertyLookup();
        for (Object o : propertyLookup.getPropertiesMap().keySet()) {
            String key = (String) o;

            if (key.startsWith(JschNodeExecutor.FWK_SSH_CONFIG_PREFIX)) {
                String name = key.substring(JschNodeExecutor.FWK_SSH_CONFIG_PREFIX.length());
                config.put(name, propertyLookup.getProperty(key));
            }
        }
        return config;
    }

    public static Map<String, String> sshConfigFromProject(IRundeckProject frameworkProject) {
        HashMap<String, String> config = new HashMap<String, String>();
        for (Object o : frameworkProject.getProperties().keySet()) {
            String key = (String) o;

            if (key.startsWith(JschNodeExecutor.PROJ_SSH_CONFIG_PREFIX)) {
                String name = key.substring(JschNodeExecutor.PROJ_SSH_CONFIG_PREFIX.length());
                config.put(name, frameworkProject.getProperty(key));
            }
        }
        return config;
    }

    public static Map<String, String> sshConfigFromNode(INodeEntry node) {
        HashMap<String, String> config = new HashMap<String, String>();
        for (String s : node.getAttributes().keySet()) {
            if (s.startsWith(JschNodeExecutor.SSH_CONFIG_PREFIX)) {
                String name = s.substring(JschNodeExecutor.SSH_CONFIG_PREFIX.length());
                config.put(name, node.getAttributes().get(s));
            }
        }
        return config;
    }

    @Override
    public Map<String, String> getSshConfig() {
        Map<String, String> config = new HashMap<String, String>();
        Map<String, String> fwkConfig = sshConfigFromFramework(framework);
        Map<String, String> projConfig = sshConfigFromProject(frameworkProject);
        Map<String, String> nodeConfig = sshConfigFromNode(node);
        if(null!=fwkConfig){
            config.putAll(fwkConfig);
        }
        if(null!=projConfig){
            config.putAll(projConfig);
        }
        if(null!=nodeConfig){
            config.putAll(nodeConfig);
        }
        return config;
    }
}
