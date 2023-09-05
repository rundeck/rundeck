package com.dtolabs.rundeck.core.execution.utils;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.service.FileCopier;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import com.dtolabs.rundeck.core.plugins.Plugin;

import java.io.File;
import java.io.InputStream;

@Plugin(name = DummyFileCopier.SERVICE_PROVIDER_TYPE, service = "FileCopier")
public class DummyFileCopier implements FileCopier {
    public static final String SERVICE_PROVIDER_TYPE = "dummy-fileCopier";
    @Override
    public String copyFileStream(ExecutionContext context, InputStream input, INodeEntry node, String destination) throws FileCopierException {
        return null;
    }

    @Override
    public String copyFile(ExecutionContext context, File file, INodeEntry node, String destination) throws FileCopierException {
        return null;
    }

    @Override
    public String copyScriptContent(ExecutionContext context, String script, INodeEntry node, String destination) throws FileCopierException {
        return null;
    }
}
