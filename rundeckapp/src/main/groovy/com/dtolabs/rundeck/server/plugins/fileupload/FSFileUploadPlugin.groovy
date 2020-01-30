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

package com.dtolabs.rundeck.server.plugins.fileupload

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.file.FileUploadPlugin
import groovy.transform.ToString
import org.apache.commons.fileupload.util.Streams

import java.nio.file.Files

/**
 * @author greg
 * @since 2/15/17
 */
@Plugin(name = FSFileUploadPlugin.PROVIDER_NAME, service = ServiceNameConstants.FileUpload)
@PluginDescription(title = 'Temporary File',
        description = 'Stores uploaded files temporarily on the file system for the duration of the execution.')
@ToString(includeNames = true)
class FSFileUploadPlugin implements FileUploadPlugin {
    static final String PROVIDER_NAME = 'filesystem-temp'

    @PluginProperty(
            title = 'Base Path',
            description = 'Root Filesystem path to store uploaded files (Default: $RDECK_BASE/var/uploads)',
            scope = PropertyScope.Framework
    )
    String basePath

    private File basedir
    private inited = false

    synchronized void initialize() {
        if (inited) {
            return
        }
        if (!basePath) {
            throw new IllegalStateException('basePath is required')
        }
        basedir = new File(basePath)
        if (!basedir.exists()) {
            basedir.mkdirs()
        } else if (!basedir.isDirectory()) {
            throw new IllegalStateException("basePath must point to a directory: $basedir")
        }
        inited = true
    }

    @Override
    String uploadFile(final InputStream content, final long length, final String refid, final Map ignored)
            throws IOException
    {
        File output = new File(basedir, refid)
        long copied = -1
        try {
            output.withOutputStream { out ->
                copied = Streams.copy(content, out, false)
            }
        } catch (IOException e) {
            if (output.exists()) {
                output.delete()
            }
            throw e
        }
        if (copied != length) {
            output.delete()
            throw new IOException("Failed to copy file content ($copied of $length copied)")
        }
        return refid
    }

    @Override
    boolean hasFile(final String ref) {
        new File(basedir, ref).exists()
    }

    @Override
    void retrieveFile(final String ref, final OutputStream out) throws IOException {
        File output = new File(basedir, ref)
        output.withInputStream { ins ->
            Streams.copy(ins, out, false)
        }
    }

    @Override
    File retrieveLocalFile(final String ref) throws IOException {
        def f = new File(basedir, ref)
        f.exists() ? f : null
    }

    @Override
    InputStream retrieveFile(final String ref) {
        File output = new File(basedir, ref)
        return Files.newInputStream(output.toPath())
    }

    @Override
    boolean removeFile(final String reference) {
        File output = new File(basedir, reference)
        return Files.deleteIfExists(output.toPath())
    }

    @Override
    FileUploadPlugin.InternalState transitionState(
            final String reference,
            final FileUploadPlugin.ExternalState state
    )
    {
        removeFile(reference)
        FileUploadPlugin.InternalState.Deleted
    }
}
