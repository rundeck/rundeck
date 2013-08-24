package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;

import java.io.File;
import java.io.InputStream;

/**
 * $INTERFACE is ... User: greg Date: 8/23/13 Time: 6:07 PM
 */
public interface DestinationFileCopier extends FileCopier {

    /**
     * Copy the contents of an input stream to the node
     *
     * @param context context
     * @param input   the input stream
     * @param node
     * @param destination the full path destination for the file
     *
     * @return File path of the file after copying to the node
     *
     * @throws FileCopierException if an error occurs
     */
    public String copyFileStream(final ExecutionContext context, InputStream input, INodeEntry node, String destination) throws
            FileCopierException;

    /**
     * Copy the contents of an input stream to the node
     *
     * @param context context
     * @param file    local file tocopy
     * @param node
     * @param destination the full path destination for the file
     *
     * @return File path of the file after copying to the node
     *
     * @throws FileCopierException if an error occurs
     */
    public String copyFile(final ExecutionContext context, File file, INodeEntry node, String destination) throws FileCopierException;

    /**
     * Copy the contents of an input stream to the node
     *
     * @param context context
     * @param script  file content string
     * @param node
     * @param destination the full path destination for the file
     *
     * @return File path of the file after copying to the node
     *
     * @throws FileCopierException if an error occurs
     */
    public String copyScriptContent(final ExecutionContext context, String script, INodeEntry node, String destination) throws
            FileCopierException;
}
