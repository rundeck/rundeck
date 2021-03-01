package org.rundeck.app.acl;


import com.dtolabs.rundeck.core.authorization.providers.BaseValidator;
import com.dtolabs.rundeck.core.authorization.providers.YamlProvider;
import com.dtolabs.utils.Streams;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Provides ACLFileManager backed by files in a directory, does NOT implement Listener semantics
 */
@Builder
@RequiredArgsConstructor
public class ACLFSFileManager
        implements ACLFileManager
{
    final File directory;
    @Getter final BaseValidator validator;

    @Override
    public long storePolicyFile(final String fileName, final InputStream input) throws IOException {
        File file = fileForName(fileName);
        try (FileOutputStream out = new FileOutputStream(file)) {
            return Streams.copyStream(input, out);
        }
    }

    private File fileForName(final String fileName) {
        if (fileName.contains("/")) {
            throw new IllegalArgumentException("invalid aclpolicy filename: " + fileName);
        }
        if (!fileName.endsWith(".aclpolicy")) {
            throw new IllegalArgumentException("invalid aclpolicy filename: " + fileName);
        }
        if (fileName.charAt(0) == '.') {
            throw new IllegalArgumentException("invalid aclpolicy filename: " + fileName);
        }

        File file = new File(directory, fileName);
        if (!file.toPath().normalize().startsWith(directory.toPath().normalize())) {
            throw new IllegalArgumentException(String.format("Path is outside of destination directory: %s", file));
        }
        requireDirectory();
        return file;
    }

    @Override
    public boolean deletePolicyFile(final String fileName) throws IOException {
        return Files.deleteIfExists(fileForName(fileName).toPath());
    }

    @Override
    public long storePolicyFileContents(final String fileName, final String fileText) throws IOException {
        File file = fileForName(fileName);
        try (FileOutputStream out = new FileOutputStream(file)) {
            byte[] bytes = fileText.getBytes(StandardCharsets.UTF_8);
            out.write(bytes);
            return bytes.length;
        }
    }

    @Override
    public AclPolicyFile getAclPolicy(final String fileName) {
        return new AclPolicyImpl(fileForName(fileName));
    }

    @RequiredArgsConstructor
    static class AclPolicyImpl
            implements AclPolicyFile
    {
        private final File file;

        @Override
        public InputStream getInputStream() {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                return null;
            }
        }

        public Date getModified() {
            return new Date(file.lastModified());
        }

        public Date getCreated() {
            return new Date(file.lastModified());
        }

        public String getName() {
            return file.getName();
        }
    }

    @Override
    public String getPolicyFileContents(final String fileName) throws IOException {
        File file = fileForName(fileName);
        try (InputStreamReader is = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return readString(is);
        }
    }

    private String readString(final Reader is) throws IOException {
        StringWriter sw = new StringWriter();
        Streams.copyWriterCount(is, sw);
        return sw.toString();
    }

    @Override
    public long loadPolicyFileContents(final String fileName, final OutputStream outputStream) throws IOException {
        File file = fileForName(fileName);
        try (FileInputStream fis = new FileInputStream(file)) {
            return Streams.copyStream(fis, outputStream);
        }
    }

    @Override
    public boolean existsPolicyFile(final String file) {
        return fileForName(file).exists();
    }

    @Override
    public List<String> listStoredPolicyFiles() {
        requireDirectory();
        return Arrays.asList(Objects.requireNonNull(directory.list(YamlProvider.filenameFilter)));
    }

    private void requireDirectory() {
        if (!directory.mkdirs() && !directory.isDirectory()) {
            throw new IllegalStateException("Unable to create necessary directory: " + directory.getAbsolutePath());
        }
    }
}
