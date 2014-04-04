package org.rundeck.plugin.encryption;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.storage.ResourceMetaBuilder;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.storage.StorageConverterPlugin;
import com.dtolabs.utils.Streams;
import org.jasypt.encryption.pbe.PBEByteEncryptor;
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor;
import org.rundeck.storage.api.HasInputStream;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.data.DataUtil;

import java.io.*;

/**
 * EncryptionConverterPlugin is ...
 *
 * @author Greg Schueler <greg@simplifyops.com>
 * @since 2014-03-26
 */
@Plugin(name = EncryptionConverterPlugin.PROVIDER_NAME, service = ServiceNameConstants.StorageConverter)
@PluginDescription(title = "Jasypt Encryption", description = "Encrypts data in the Rundeck Storage layer")
public class EncryptionConverterPlugin implements StorageConverterPlugin {
    public static final String PROVIDER_NAME = "jasypt-encryption";
    @PluginProperty(description = "Encryption password", required = true)
    String password;
    @PluginProperty(description = "Optionally specified algorithm for encryption")
    String algorithm;

    private volatile StandardPBEByteEncryptor standardPBEByteEncryptor = null;

    private PBEByteEncryptor getEncryptor() {
        if (null == standardPBEByteEncryptor) {
            synchronized (this) {
                if (null == standardPBEByteEncryptor) {
                    if(null== password || "".equals(password)) {
                        throw new IllegalStateException("password is required");
                    }
                    StandardPBEByteEncryptor encryptor = new StandardPBEByteEncryptor();
                    encryptor.setPassword(password);
                    if (null != algorithm && !"".equals(algorithm)) {
                        encryptor.setAlgorithm(algorithm);
                    }
                    password = null;
                    standardPBEByteEncryptor = encryptor;
                }
            }
        }
        return standardPBEByteEncryptor;
    }

    @Override
    public HasInputStream readResource(Path path, ResourceMetaBuilder resourceMetaBuilder, HasInputStream
            hasInputStream) {
        if ("true".equals(resourceMetaBuilder.getResourceMeta().get(PROVIDER_NAME + ":encrypted"))) {
            return decrypt(hasInputStream);
        }
        return null;
    }

    @Override
    public HasInputStream createResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
            HasInputStream hasInputStream) {
        resourceMetaBuilder.getResourceMeta().put(PROVIDER_NAME + ":encrypted", "true");
        return encrypt(hasInputStream);
    }

    @Override
    public HasInputStream updateResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
            HasInputStream hasInputStream) {

        resourceMetaBuilder.getResourceMeta().put(PROVIDER_NAME + ":encrypted", "true");
        return encrypt(hasInputStream);
    }

    private HasInputStream encrypt(final HasInputStream hasInputStream) {
        return new EncryptStream(hasInputStream, getEncryptor());
    }

    private HasInputStream decrypt(final HasInputStream hasInputStream) {
        return new DecryptStream(hasInputStream, getEncryptor());
    }


    private static byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Streams.copyStream(inputStream, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }


    private static class EncryptStream implements HasInputStream {
        private final HasInputStream hasInputStream;
        private PBEByteEncryptor encryptor;

        private EncryptStream(HasInputStream hasInputStream, PBEByteEncryptor encryptor) {
            this.hasInputStream = hasInputStream;
            this.encryptor = encryptor;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(encryptor.encrypt(getBytes(hasInputStream.getInputStream())));
        }

        @Override
        public long writeContent(OutputStream outputStream) throws IOException {
            return DataUtil.copyStream(getInputStream(), outputStream);
        }
    }

    private static class DecryptStream implements HasInputStream {
        private final HasInputStream hasInputStream;
        private PBEByteEncryptor encryptor;

        private DecryptStream(HasInputStream hasInputStream, PBEByteEncryptor encryptor) {
            this.hasInputStream = hasInputStream;
            this.encryptor = encryptor;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(encryptor.decrypt(getBytes(hasInputStream.getInputStream())));
        }

        @Override
        public long writeContent(OutputStream outputStream) throws IOException {
            return DataUtil.copyStream(getInputStream(), outputStream);
        }
    }
}
