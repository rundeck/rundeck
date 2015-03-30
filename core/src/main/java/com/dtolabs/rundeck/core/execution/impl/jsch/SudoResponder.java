package com.dtolabs.rundeck.core.execution.impl.jsch;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.IRundeckProject;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.utils.PasswordSource;
import com.dtolabs.rundeck.core.execution.utils.ResolverUtil;
import com.dtolabs.rundeck.core.execution.utils.Responder;

import java.util.regex.Pattern;

/**
 * Sudo responder that determines response patterns from node attributes and project properties. Also handles
 * responder thread result by closing the SSH connection on failure. The mechanism for sudo response: <ol> <li>look
 * for "[sudo] password for &lt;username&gt;: " (inputSuccessPattern)</li> <li>if not seen in 12 lines, then assume
 * password is not needed (isSuccessOnInputThreshold=true)</li> <li>if fewer than 12 lines, and no new input in 5000
 * milliseconds, then fail (isFailOnInputTimeoutThreshold=true)</li> <li>if seen, output password and carriage
 * return (inputString)</li> <li>look for "Sorry, try again" (responseFailurePattern)</li> <li>if not seen in 3
 * lines, or 5000 milliseconds, then assume success (isFailOnResponseThreshold)</li> <li>if seen, then fail</li>
 * </ol>
 */
class SudoResponder implements Responder {
    public static final String DEFAULT_DESCRIPTION = "Sudo execution password response";
    private String sudoCommandPattern;
    private String inputSuccessPattern;
    private String inputFailurePattern;
    private String responseSuccessPattern;
    private String responseFailurePattern;
    private int inputMaxLines = -1;
    private long inputMaxTimeout = -1;
    private boolean failOnInputLinesThreshold = false;
    private boolean failOnInputTimeoutThreshold = true;
    private boolean successOnInputThreshold = true;
    private int responseMaxLines = -1;
    private long responseMaxTimeout = -1;
    private boolean failOnResponseThreshold = false;
    private byte[] inputBytes;
    private String configPrefix;
    private String description;
    private PasswordSource passwordSource;
    private String defaultSudoCommandPattern;


    private SudoResponder() {
        description = DEFAULT_DESCRIPTION;
        defaultSudoCommandPattern = JschNodeExecutor.DEFAULT_SUDO_COMMAND_PATTERN;
        configPrefix = JschNodeExecutor.SUDO_OPT_PREFIX;
    }

    private SudoResponder(
            final String configPrefix,
            final PasswordSource passwordSource,
            final String defaultSudoCommandPattern
    ) {
        this();
        if (null != configPrefix) {
            this.configPrefix = configPrefix;
        }
        if (null != passwordSource) {
            this.passwordSource = passwordSource;
        }
        if (null != defaultSudoCommandPattern) {
            this.defaultSudoCommandPattern = defaultSudoCommandPattern;
        }
    }


    static SudoResponder create(final INodeEntry node,
                                final Framework framework,
                                final ExecutionContext context,
                                final String configPrefix,
                                final PasswordSource passwordSource, final String defaultSudoCommandPattern) {
        final SudoResponder sudoResponder = new SudoResponder(configPrefix, passwordSource,
                                                              defaultSudoCommandPattern);
        sudoResponder.init(
                node,
                framework.getFrameworkProjectMgr().getFrameworkProject(context.getFrameworkProject()),
                framework
        );
        return sudoResponder;
    }


    public boolean matchesCommandPattern(final String command) {
        final String sudoCommandPattern1 = getSudoCommandPattern();
        if (null != sudoCommandPattern1) {
            return Pattern.compile(sudoCommandPattern1).matcher(command).matches();
        } else {
            return false;
        }
    }

    private void init(
            final INodeEntry node,
            final IRundeckProject frameworkProject,
            final Framework framework
    ) {
        sudoEnabled = ResolverUtil.resolveBooleanProperty(
                configPrefix + JschNodeExecutor.NODE_ATTR_SUDO_COMMAND_ENABLED,
                false,
                node,
                frameworkProject,
                framework
        );
        if (sudoEnabled) {

            final byte[] sudoPassword = passwordSource.getPassword();
            inputBytes = (null != sudoPassword ? appendBytes(sudoPassword, "\n".getBytes()) : new byte[0]);
            passwordSource.clear();

            sudoCommandPattern = ResolverUtil.resolveProperty(
                    configPrefix + JschNodeExecutor.NODE_ATTR_SUDO_COMMAND_PATTERN,
                    defaultSudoCommandPattern,
                    node,
                    frameworkProject,
                    framework
            );
            inputSuccessPattern = ResolverUtil.resolveProperty(
                    configPrefix + JschNodeExecutor.NODE_ATTR_SUDO_PROMPT_PATTERN,
                    JschNodeExecutor.DEFAULT_SUDO_PROMPT_PATTERN,
                    node,
                    frameworkProject,
                    framework
            );
            inputFailurePattern = null;
            responseFailurePattern = ResolverUtil.resolveProperty(
                    configPrefix + JschNodeExecutor.NODE_ATTR_SUDO_FAILURE_PATTERN,
                    JschNodeExecutor.DEFAULT_SUDO_FAILURE_PATTERN,
                    node,
                    frameworkProject,
                    framework
            );
            responseSuccessPattern = null;
            inputMaxLines = ResolverUtil.resolveIntProperty(
                    configPrefix + JschNodeExecutor.NODE_ATTR_SUDO_PROMPT_MAX_LINES,
                    JschNodeExecutor.DEFAULT_SUDO_PROMPT_MAX_LINES,
                    node,
                    frameworkProject,
                    framework
            );
            inputMaxTimeout = ResolverUtil.resolveLongProperty(
                    configPrefix + JschNodeExecutor.NODE_ATTR_SUDO_PROMPT_MAX_TIMEOUT,
                    JschNodeExecutor.DEFAULT_SUDO_PROMPT_MAX_TIMEOUT,
                    node, frameworkProject, framework
            );
            responseMaxLines = ResolverUtil.resolveIntProperty(
                    configPrefix + JschNodeExecutor.NODE_ATTR_SUDO_RESPONSE_MAX_LINES,
                    JschNodeExecutor.DEFAULT_SUDO_RESPONSE_MAX_LINES,
                    node, frameworkProject, framework
            );
            responseMaxTimeout = ResolverUtil.resolveLongProperty(
                    configPrefix + JschNodeExecutor.NODE_ATTR_SUDO_RESPONSE_MAX_TIMEOUT,
                    JschNodeExecutor.DEFAULT_SUDO_RESPONSE_MAX_TIMEOUT,
                    node,
                    frameworkProject,
                    framework
            );

            failOnInputLinesThreshold = ResolverUtil.resolveBooleanProperty(
                    configPrefix + JschNodeExecutor.NODE_ATTR_SUDO_FAIL_ON_PROMPT_MAX_LINES,
                    JschNodeExecutor.DEFAULT_SUDO_FAIL_ON_PROMPT_MAX_LINES, node, frameworkProject, framework
            );

            failOnInputTimeoutThreshold = ResolverUtil.resolveBooleanProperty(
                    configPrefix + JschNodeExecutor.NODE_ATTR_SUDO_FAIL_ON_PROMPT_TIMEOUT,
                    JschNodeExecutor.DEFAULT_SUDO_FAIL_ON_PROMPT_TIMEOUT, node, frameworkProject, framework
            );
            failOnResponseThreshold = ResolverUtil.resolveBooleanProperty(
                    configPrefix + JschNodeExecutor.NODE_ATTR_SUDO_FAIL_ON_RESPONSE_TIMEOUT,
                    JschNodeExecutor.DEFAULT_SUDO_FAIL_ON_RESPONSE_TIMEOUT,
                    node,
                    frameworkProject,
                    framework
            );
            successOnInputThreshold = ResolverUtil.resolveBooleanProperty(
                    configPrefix + JschNodeExecutor.NODE_ATTR_SUDO_SUCCESS_ON_PROMPT_THRESHOLD,
                    JschNodeExecutor.DEFAULT_SUDO_SUCCESS_ON_PROMPT_THRESHOLD, node, frameworkProject, framework
            );
        }
    }

    private byte[] appendBytes(final byte[] first, final byte[] second) {
        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private boolean sudoEnabled = false;

    /**
     * Return true if sudo should be used for the command execution on this node
     */
    public boolean isSudoEnabled() {
        return sudoEnabled;
    }

    public byte[] getInputBytes() {
        return inputBytes;
    }


    public boolean isFailOnInputLinesThreshold() {
        return failOnInputLinesThreshold;
    }

    public boolean isFailOnInputTimeoutThreshold() {
        return failOnInputTimeoutThreshold;
    }

    public boolean isFailOnResponseThreshold() {
        return failOnResponseThreshold;
    }

    public boolean isSuccessOnInputThreshold() {
        return successOnInputThreshold;
    }

    /**
     * Return the regular expression to use to match the command to determine if Sudo should be used.
     */
    public String getSudoCommandPattern() {
        return sudoCommandPattern;
    }

    public String getInputFailurePattern() {
        return inputFailurePattern;
    }

    public String getInputSuccessPattern() {

        return inputSuccessPattern;
    }

    public String getResponseSuccessPattern() {
        return responseSuccessPattern;
    }

    public String getResponseFailurePattern() {
        return responseFailurePattern;
    }

    public int getInputMaxLines() {
        return inputMaxLines;
    }

    public long getInputMaxTimeout() {
        return inputMaxTimeout;
    }


    public int getResponseMaxLines() {
        return responseMaxLines;
    }

    public long getResponseMaxTimeout() {
        return responseMaxTimeout;
    }

    @Override
    public String toString() {
        return description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
