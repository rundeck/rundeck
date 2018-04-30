/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.common.NodeSetImpl;
import com.dtolabs.rundeck.core.resources.format.*;
import com.dtolabs.utils.Streams;

import java.io.*;
import java.nio.file.Files;

/**
 * <p> A base class for a Writeable ResourceModelSource that supports the Rundeck parser formats.
 * This class maintains cached Nodes data, and a "last modified" timestamp can be used to determine
 * if cached or fresh data should be returned.  Subclasses can define the specific
 * ResourceFormat to use, either by returning a provider name from {@link #getResourceFormat()}, a
 * file extension from {@link #getDocumentFileExtension()} or overridding {@link #getResourceFormatParser()}
 * directly.
 * </p>
 *
 * <p>
 * Methods implementing {@link WriteableModelSource} are provided, but can be overridden.
 * </p>
 *
 * <p>
 * This class is meant to serve as a base class for a ResourceModelSource similar to the {@link FileResourceModelSource}
 * where the backend is similar to a file system.
 * </p>
 *
 * <p>Abstract methods which should be implemented:</p>
 *
 * <ul>
 * <li>{@link #getResourceFormat()}</li>
 * <li>{@link #getDocumentFileExtension()}</li>
 * <li>{@link #writeFileData(InputStream)}</li>
 * <li>{@link #openFileDataInputStream()}</li>
 * </ul>
 *
 * <p>Methods which can be overridden:</p>
 * <ul>
 * <li>{@link #isDataWritable()}</li>
 * <li>{@link #shouldGenerateServerNode()}</li>
 * <li>{@link #isSupportsLastModified()}</li>
 * <li>{@link #getLastModified()}</li>
 * <li>{@link #getResourceFormatParser()}</li>
 * </ul>
 *
 * @author greg
 * @since 9/11/17
 */
public abstract class BaseFileResourceModelSource implements ResourceModelSource, WriteableModelSource {
    protected Framework framework;
    protected NodeSetImpl nodeSet;
    long lastModTime = 0;

    public BaseFileResourceModelSource(
            final Framework framework
    )
    {
        nodeSet = new NodeSetImpl();
        this.framework = framework;
    }


    @Override
    public String getSyntaxMimeType() {
        try {
            return getResourceFormatParser().getPreferredMimeType();
        } catch (ResourceModelSourceException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Uses the result of {@link #getResourceFormat()} or {@link #getDocumentFileExtension()} to
     * retrieve a ResourceFormatParser.
     *
     * @return implementation of ResourceFormatParser
     *
     * @throws ResourceModelSourceException
     */
    protected ResourceFormatParser getResourceFormatParser() throws ResourceModelSourceException {
        try {
            String format = getResourceFormat();
            if (null != format) {
                return framework.getResourceFormatParserService()
                                .getParserForFormat(format);
            } else {
                return framework.getResourceFormatParserService()
                                .getParserForFileExtension(getDocumentFileExtension());
            }
        } catch (UnsupportedFormatException e) {
            throw new ResourceModelSourceException(e);
        }
    }

    /**
     * @return the name of the supported ResourceFormatParser provider, or null if the file extension is used
     */
    protected abstract String getResourceFormat();

    /**
     * @return the file extension of the document, or the {@link #getResourceFormat()} must be used.
     */
    protected abstract String getDocumentFileExtension();

    /**
     * Writes the data to a temp file, and attempts to parser it, then if successful it will
     * call {@link #writeFileData(InputStream)} to invoke the sub class
     *
     * @param data data
     *
     * @return number of bytes written
     *
     * @throws IOException                  if an IO error occurs
     * @throws ResourceModelSourceException if an error occurs parsing the data.
     */
    @Override
    public long writeData(final InputStream data) throws IOException, ResourceModelSourceException {
        if (!isDataWritable()) {
            throw new IllegalArgumentException("Cannot write to file, it is not configured to be writeable");
        }
        ResourceFormatParser resourceFormat = getResourceFormatParser();
        //validate data
        File temp = Files.createTempFile("temp", "." + resourceFormat.getPreferredFileExtension()).toFile();
        temp.deleteOnExit();


        try {

            try (FileOutputStream fos = new FileOutputStream(temp)) {
                Streams.copyStream(data, fos);
            }
            final ResourceFormatParser parser = getResourceFormatParser();
            try {
                final INodeSet set = parser.parseDocument(temp);
            } catch (ResourceFormatParserException e) {
                throw new ResourceModelSourceException(e);
            }
            try (FileInputStream tempStream = new FileInputStream(temp)) {
                return writeFileData(tempStream);
            }
        } finally {
            temp.delete();
        }
    }

    /**
     * Write the file data from the inputstream to the backing store
     *
     * @param tempStream input stream
     *
     * @return bytes writen
     *
     * @throws IOException
     */
    public abstract long writeFileData(final InputStream tempStream) throws IOException;

    /**
     * @return an input stream that reads the data from the backing store
     *
     * @throws IOException
     * @throws ResourceModelSourceException
     */
    public abstract InputStream openFileDataInputStream() throws IOException, ResourceModelSourceException;

    @Override
    public long readData(final OutputStream sink) throws IOException, ResourceModelSourceException {
        if (!hasData()) {
            return 0;
        }
        try (InputStream inputStream = openFileDataInputStream()) {
            return Streams.copyStream(inputStream, sink);
        }
    }

    @Override
    public SourceType getSourceType() {
        return isDataWritable() ? SourceType.READ_WRITE : SourceType.READ_ONLY;
    }

    /**
     * @return true if the model source is writable, false if the data should not be modified with {@link
     * #writeData(InputStream)}
     */
    public boolean isDataWritable() {
        return false;
    }

    /**
     * @return true if the node set should always include the local server node data
     */
    protected boolean shouldGenerateServerNode() {
        return false;
    }

    /**
     * @return true if the underlying data set supports a "last modified" timestamp which should be used to determine
     * whether the nodes data should be reloaded or cached data can be used
     */
    protected boolean isSupportsLastModified() {
        return false;
    }

    /**
     * @return last modified unix timestamp, or -1 if not available
     */
    protected long getLastModified() {
        return -1;
    }

    @Override
    public WriteableModelSource getWriteable() {
        return isDataWritable() ? this : null;
    }

    public INodeSet getNodes() throws ResourceModelSourceException {
        return getNodes(getResourceFormatParser());
    }

    /**
     * Returns a {@link INodeSet} object conatining the nodes config data.
     *
     * @param parser model parser
     *
     * @return an instance of {@link INodeSet}
     *
     * @throws ResourceModelSourceException on error
     */
    public synchronized INodeSet getNodes(ResourceFormatParser parser) throws
            ResourceModelSourceException
    {
        final Long modtime = isSupportsLastModified() ? getLastModified() : -1;
        if (0 == nodeSet.getNodes().size() || modtime < 0 || (modtime > lastModTime)) {
            nodeSet = loadNodes(parser, shouldGenerateServerNode());
            lastModTime = modtime;
        }
        return nodeSet;
    }

    private NodeSetImpl loadNodes(ResourceFormatParser parser, final boolean generateServerNode)
            throws ResourceModelSourceException
    {
        NodeSetImpl newNodes = new NodeSetImpl();

        if (generateServerNode) {
            final NodeEntryImpl node = framework.createFrameworkNode();
            newNodes.putNode(node);
        }
        if (hasData()) {
            try {
                try (InputStream input = openFileDataInputStream()) {
                    final INodeSet set = parser.parseDocument(input);
                    if (null != set) {
                        newNodes.putNodes(set);
                    }
                }
            } catch (ResourceFormatParserException | IOException e) {
                throw new ResourceModelSourceException(e);
            }
        }
        return newNodes;
    }


}
