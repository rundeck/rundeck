package org.rundeck.plugin.encryption;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.storage.ResourceMetaBuilder;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.Password;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.descriptions.SelectValues;
import com.dtolabs.rundeck.plugins.storage.StorageConverterPlugin;
import com.dtolabs.utils.Streams;
import org.apache.log4j.Logger;
import org.jasypt.encryption.pbe.PBEByteEncryptor;
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor;
import org.jasypt.encryption.pbe.config.EnvironmentPBEConfig;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
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
    public static final Logger logger = Logger.getLogger(EncryptionConverterPlugin.class);

    @PluginProperty(title = "Encryptor Type",
                    description =
                            "Jasypt Encryptor to use.\n\nEither 'strong', 'basic', or 'custom'. \n" +
                            "'custom' is required to specify algorithm and provider, etc.\n" +
                            "'strong' requires use of the JCE Unlimited Strength policy files.\n" +
                            "Default: 'basic'.",
                    defaultValue = "basic",
                    required = true)
    @SelectValues(values = {"strong", "basic", "custom"})
    String encryptorType;

    @PluginProperty(title = "Password", description = "Encryption password", required = false)
    @Password
    String password;
    @PluginProperty(title = "Password Environment Variable",
                    description = "Name of Environment variable storing Encryption password",
                    required = false)
    String passwordEnvVarName;
    @PluginProperty(title = "Password System Property",
                    description = "Name of JVM System Property storing Encryption password",
                    required = false)
    String passwordSysPropName;

    @PluginProperty(title = "Algorithm", description = "(optional)")
    String algorithm;

    @PluginProperty(title = "Provider Name",
                    description = "Default: BC (bouncycastle)",
                    defaultValue = "BC")
    String provider;

    @PluginProperty(title = "Provider Class Name",
                    description = "Overrides " +
                                  "Provider Name.")
    String providerClassName;

    @PluginProperty(title = "Key Obtention Iterations",
                    description = "(optional)")
    String keyObtentionIterations;

    private volatile StandardPBEByteEncryptor standardPBEByteEncryptor = null;

    private StandardPBEByteEncryptor getEncryptor() {
        if (null == standardPBEByteEncryptor) {
            synchronized (this) {
                if (null == standardPBEByteEncryptor) {
                    logger.debug("PBEByteEncryptor begin setup...");
                    EnvironmentPBEConfig config = new EnvironmentPBEConfig();

                    if (null != password) {
                        logger.debug("PBEByteEncryptor use password");
                        config.setPassword(password);
                    } else if (null != passwordEnvVarName) {
                        logger.debug("PBEByteEncryptor use password env var");
                        config.setPasswordEnvName(passwordEnvVarName);
                    } else if (null != passwordSysPropName) {
                        config.setPasswordSysPropertyName(passwordSysPropName);
                        logger.debug("PBEByteEncryptor use password sys prop");
                        System.clearProperty(passwordSysPropName);
                    } else {
                        throw new IllegalStateException(
                                "password, passwordEnvVarName, or passwordSysPropName is required"
                        );
                    }

                    StandardPBEByteEncryptor encryptor = new StandardPBEByteEncryptor();
                    if ("strong".equals(encryptorType)) {
                        logger.debug("PBEByteEncryptor use STRONG type");
                        config.setAlgorithm("PBEWithMD5AndTripleDES");
                    } else if ("basic".equals(encryptorType)) {
                        logger.debug("PBEByteEncryptor use BASIC type");
                        config.setAlgorithm("PBEWithMD5AndDES");
                    } else if ("custom".equals(encryptorType)) {
                        logger.debug("PBEByteEncryptor use CUSTOM type");

                        if (null != algorithm && !"".equals(algorithm)) {
                            config.setAlgorithm(algorithm);
                        }
                        if (null != providerClassName && !"".equals(providerClassName)) {
                            config.setProviderClassName(providerClassName);
                        } else if (null != provider && !"".equals(provider)) {
                            config.setProviderName(provider);
                        }

                        if (null != keyObtentionIterations && !"".equals(keyObtentionIterations)) {
                            config.setKeyObtentionIterations(keyObtentionIterations);
                        }
                    } else {

                        throw new IllegalStateException(
                                "encryptorType is required"
                        );
                    }
                    encryptor.setConfig(config);
                    logger.debug("PBEByteEncryptor configured");

                    password = null;
                    standardPBEByteEncryptor = encryptor;
                }
            }
        }
        return standardPBEByteEncryptor;
    }

    @Override
    public HasInputStream readResource(
            Path path, ResourceMetaBuilder resourceMetaBuilder, HasInputStream
            hasInputStream
    )
    {
        if ("true".equals(resourceMetaBuilder.getResourceMeta().get(PROVIDER_NAME + ":encrypted"))) {
            logger.debug("readResource (encrypted) " + path);
            return decrypt(hasInputStream);
        }
        logger.debug("readResource (unencrypted) " + path);
        return null;
    }

    @Override
    public HasInputStream createResource(
            Path path, ResourceMetaBuilder resourceMetaBuilder,
            HasInputStream hasInputStream
    )
    {
        resourceMetaBuilder.getResourceMeta().put(PROVIDER_NAME + ":encrypted", "true");
        logger.debug("createResource " + path);
        return encrypt(hasInputStream);
    }

    @Override
    public HasInputStream updateResource(
            Path path, ResourceMetaBuilder resourceMetaBuilder,
            HasInputStream hasInputStream
    )
    {

        resourceMetaBuilder.getResourceMeta().put(PROVIDER_NAME + ":encrypted", "true");
        logger.debug("updateResource " + path);
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
            try {
                return new ByteArrayInputStream(encryptor.decrypt(getBytes(hasInputStream.getInputStream())));
            } catch (EncryptionOperationNotPossibleException e) {
                throw new IOException("Decryption failed.", e);
            }
        }

        @Override
        public long writeContent(OutputStream outputStream) throws IOException {
            return DataUtil.copyStream(getInputStream(), outputStream);
        }
    }
}
