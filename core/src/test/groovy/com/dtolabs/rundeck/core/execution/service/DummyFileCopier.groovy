package com.dtolabs.rundeck.core.execution.service

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.execution.ExecutionContext

class DummyFileCopier implements FileCopier{
    @Override
    String copyFileStream(ExecutionContext context, InputStream input, INodeEntry node, String destination) throws FileCopierException {
        return null
    }

    @Override
    String copyFile(ExecutionContext context, File file, INodeEntry node, String destination) throws FileCopierException {
        return null
    }

    @Override
    String copyScriptContent(ExecutionContext context, String script, INodeEntry node, String destination) throws FileCopierException {
        return null
    }
}
