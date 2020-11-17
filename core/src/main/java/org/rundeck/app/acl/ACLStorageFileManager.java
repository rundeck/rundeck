package org.rundeck.app.acl;

import com.dtolabs.rundeck.core.authorization.RuleSetValidation;
import com.dtolabs.rundeck.core.authorization.providers.PolicyCollection;
import com.dtolabs.rundeck.core.authorization.providers.Validator;
import com.dtolabs.rundeck.core.storage.ResourceMeta;
import com.dtolabs.rundeck.core.storage.StorageManager;
import com.dtolabs.utils.Streams;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.rundeck.storage.api.Resource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Builder
@RequiredArgsConstructor
public class ACLStorageFileManager
        implements ACLFileManager

{
    private final String prefix;
    private final StorageManager storage;
    @Getter private final Validator validator;
    private final String pattern = ".*\\.aclpolicy";

    /**
     * List the system aclpolicy file paths, including the base dir name of acls/
     */
    @Override
    public List<String> listStoredPolicyPaths() {
        return storage.listDirPaths(prefix, pattern);
    }

    /**
     * List the system aclpolicy file names, not including the dir path
     */
    public List<String> listStoredPolicyFiles() {
        return listStoredPolicyPaths().stream().map((it) -> it.substring(prefix.length())).collect(Collectors.toList());
    }

    /**
     * @param file name without path
     * @return true if the policy file with the given name exists
     */
    @Override
    public boolean existsPolicyFile(String file) {
        return storage.existsFileResource(prefix + file);
    }

    public RuleSetValidation<PolicyCollection> validatePolicyFile(String fname) throws IOException {
        boolean exists = existsPolicyFile(fname);
        if (!exists) {
            return null;
        }
        return validator.validateYamlPolicy(
                null,
                fname,
                getPolicyFileContents(fname)
        );
    }

    /**
     * @param fileName name of policy file, without path
     * @return text contents of the policy file
     */
    @Override
    public String getPolicyFileContents(String fileName) throws IOException {
        AclPolicyFile aclPolicy = getAclPolicy(fileName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Streams.copyStream(aclPolicy.getInputStream(), baos);
        return new String(baos.toByteArray(),StandardCharsets.UTF_8);
    }

    /**
     * Load content to output stream
     *
     * @param fileName name of policy file, without path
     * @return length of output
     */
    @Override
    public long loadPolicyFileContents(String fileName, OutputStream outputStream) throws IOException {
        return storage.loadFileResource(prefix + fileName, outputStream);
    }

    @Override
    public AclPolicyFile getAclPolicy(final String fileName) {
        Resource<ResourceMeta> resource = storage.getFileResource(prefix + fileName);
        ResourceMeta file = resource.getContents();

        return new AclPolicyImpl(
                ()-> {
                    try{
                        return file.getInputStream();
                    }catch (IOException e){
                        return null;
                    }
                },
                file.getModificationTime(),
                file.getCreationTime(),
                fileName
        );
    }

    @RequiredArgsConstructor
    static class AclPolicyImpl
            implements AclPolicyFile
    {
        private final Supplier<InputStream> inputStream;

        @Override
        public InputStream getInputStream() {
            return inputStream.get();
        }

        @Getter private final Date modified;
        @Getter private final Date created;
        @Getter private final String name;
    }

    /**
     * Store a system policy file
     *
     * @param fileName name without path
     * @param fileText contents
     * @return size of bytes stored
     */
    @Override
    public long storePolicyFileContents(String fileName, String fileText) {
        byte[] bytes = fileText.getBytes(StandardCharsets.UTF_8);
        Resource<ResourceMeta> result = storage.writeFileResource(
                prefix + fileName,
                new ByteArrayInputStream(bytes),
                new HashMap<>()
        );
        return bytes.length;
    }

    /**
     * Delete a policy file
     *
     * @return true if successful
     */
    @Override
    public boolean deletePolicyFile(String fileName) {
        return storage.deleteFileResource(prefix + fileName);
    }
}
