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
 * JasyptEncryptionConverterPlugin is ...
 *
 * @author Greg Schueler &lt;greg@simplifyops.com&gt;
 * @since 2014-03-26
 */
@Plugin(name = JasyptEncryptionConverterPlugin.PROVIDER_NAME, service = ServiceNameConstants.StorageConverter)
@PluginDescription(title = "Jasypt Encryption", description = "Encrypts data in the Rundeck Storage layer\n\n" +
                                                              "This plugin uses Jasypt to perform encryption. The " +
                                                              "built in java JCE is used unless another provider is " +
                                                              "specified, Bouncycastle can be used by specifying the " +
                                                              "'BC' provider name.\n\n" +
                                                              "Password, algorithm, provider, etc can be specified " +
                                                              "directly, or via environment variables (the `*EnvVarName` " +
                                                              "properties), " +
                                                              "or Java System properties (the `*SysPropName` properties)." )


public class JasyptEncryptionConverterPlugin implements StorageConverterPlugin {
    public static final String PROVIDER_NAME = "jasypt-encryption";
    public static final Logger logger = Logger.getLogger(JasyptEncryptionConverterPlugin.class);

    @PluginProperty(title = "Encryptor Type",
                    description =
                            "Jasypt Encryptor to use.\n\n" +
                            "Either 'basic', 'strong', or 'custom'. \n\n" +
                            "* 'basic' uses algorithm PBEWithMD5AndDES\n" +
                            "* 'strong' requires use of the JCE Unlimited Strength policy files. (Algorithm: " +
                            "PBEWithMD5AndTripleDES)\n" +
                            "* 'custom' is required to specify algorithm, provider, etc.\n" +
                            "\n" +
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

    @PluginProperty(title = "Algorithm", description = "(optional)" )
    String algorithm;
    @PluginProperty(title = "Algorithm Environment Variable", description = "(optional)" )
    String algorithmEnvVarName;
    @PluginProperty(title = "Algorithm System Property", description = "(optional)" )
    String algorithmSysPropName;

    @PluginProperty(title = "Provider Name",
                    description = "Example: 'BC' (specifies bouncycastle)"
    )
    String provider;
    @PluginProperty(title = "Provider Name Environment Variable",
                    description = "(optional)" )
    String providerEnvVarName;
    @PluginProperty(title = "Provider Name System Property",
                    description = "(optional)" )
    String providerSysPropName;

    @PluginProperty(title = "Provider Class Name",
                    description = "Overrides " +
                                  "Provider Name." )
    String providerClassName;

    @PluginProperty(title = "Provider Class Name Environment Variable",
                    description = "Overrides " +
                                  "Provider Name." )
    String providerClassNameEnvVarName;

    @PluginProperty(title = "Provider Class Name System Property",
                    description = "Overrides " +
                                  "Provider Name." )
    String providerClassNameSysPropName;

    @PluginProperty(title = "Key Obtention Iterations",
                    description = "(optional) Number of hash operations on password when generating key, default: " +
                                  "1000." )
    String keyObtentionIterations;
    @PluginProperty(title = "Key Obtention Iterations Environment Variable",
                    description = "(optional)" )
    String keyObtentionIterationsEnvVarName;
    @PluginProperty(title = "Key Obtention Iterations System Property",
                    description = "(optional)" )
    String keyObtentionIterationsSysPropName;

    private volatile StandardPBEByteEncryptor standardPBEByteEncryptor = null;

    private StandardPBEByteEncryptor getEncryptor() {
        if (null == standardPBEByteEncryptor) {
            synchronized (this) {
                if (null == standardPBEByteEncryptor) {
                    logger.debug("JasyptEncryptionConverterPlugin begin setup...");
                    EnvironmentPBEConfig config = new EnvironmentPBEConfig();

                    addPasswordValue(config, password, passwordEnvVarName, passwordSysPropName, true, "password");

                    password = null;
                    passwordEnvVarName = null;
                    passwordSysPropName = null;

                    StandardPBEByteEncryptor encryptor = new StandardPBEByteEncryptor();
                    if ("strong".equals(encryptorType)) {
                        logger.debug("JasyptEncryptionConverterPlugin use STRONG type");
                        config.setAlgorithm("PBEWithMD5AndTripleDES");
                    } else if ("basic".equals(encryptorType)) {
                        logger.debug("JasyptEncryptionConverterPlugin use BASIC type");
                        config.setAlgorithm("PBEWithMD5AndDES");
                    } else if ("custom".equals(encryptorType)) {
                        logger.debug("JasyptEncryptionConverterPlugin use CUSTOM type");

                        addAlgorithmValue(
                                config,
                                algorithm,
                                algorithmEnvVarName,
                                algorithmSysPropName,
                                false,
                                "algorithm"
                        );
                    } else {

                        throw new IllegalStateException(
                                "encryptorType is required"
                        );
                    }

                    if (!addProviderClassNameValue(
                            config,
                            providerClassName,
                            providerClassNameEnvVarName,
                            providerClassNameSysPropName,
                            false,
                            "providerClassName"
                    )) {

                        addProviderNameValue(
                                config,
                                provider,
                                providerEnvVarName,
                                providerSysPropName,
                                false,
                                "provider"
                        );
                    }


                    addKeyObtentionIterationsValue(
                            config,
                            keyObtentionIterations,
                            keyObtentionIterationsEnvVarName,
                            keyObtentionIterationsSysPropName,
                            false,
                            "keyObtentionIterations"
                    );
                    encryptor.setConfig(config);
                    logger.debug("JasyptEncryptionConverterPlugin configured");

                    standardPBEByteEncryptor = encryptor;
                }
            }
        }
        return standardPBEByteEncryptor;
    }

    private boolean addPasswordValue(
            final EnvironmentPBEConfig config,
            final String directValue,
            final String envVarValue,
            final String sysPropValue,
            final boolean required,
            final String description
    )
    {
        if (notBlank(directValue)) {
            logger.debug("JasyptEncryptionConverterPlugin use value for " + description);
            config.setPassword(directValue);
        } else if (notBlank(envVarValue)) {
            logger.debug("JasyptEncryptionConverterPlugin use env var for " + description);
            config.setPasswordEnvName(envVarValue);
        } else if (notBlank(sysPropValue)) {
            config.setPasswordSysPropertyName(sysPropValue);
            logger.debug("JasyptEncryptionConverterPlugin use sys prop for " + description);
            System.clearProperty(sysPropValue);
        } else if (required) {
            throw new IllegalStateException(
                    description + ", " + description + "EnvVarName, or " + description + "SysPropName is required"
            );
        } else {
            return false;
        }
        return true;
    }

    private boolean addAlgorithmValue(
            final EnvironmentPBEConfig config,
            final String directValue,
            final String envVarValue,
            final String sysPropValue,
            final boolean required,
            final String description
    )
    {
        if (notBlank(directValue)) {
            logger.debug("JasyptEncryptionConverterPlugin use value for " + description);
            config.setAlgorithm(directValue);
        } else if (notBlank(envVarValue)) {
            logger.debug("JasyptEncryptionConverterPlugin use env var for " + description);
            config.setAlgorithmEnvName(envVarValue);
        } else if (notBlank(sysPropValue)) {
            config.setAlgorithmSysPropertyName(sysPropValue);
            logger.debug("JasyptEncryptionConverterPlugin use sys prop for " + description);
            System.clearProperty(sysPropValue);
        } else if (required) {
            throw new IllegalStateException(
                    description + ", " + description + "EnvVarName, or " + description + "SysPropName is required"
            );
        } else {
            return false;
        }
        return true;
    }

    private boolean addProviderNameValue(
            final EnvironmentPBEConfig config,
            final String directValue,
            final String envVarValue,
            final String sysPropValue,
            final boolean required,
            final String description
    )
    {
        if (notBlank(directValue)) {
            logger.debug("JasyptEncryptionConverterPlugin use value for " + description);
            config.setProviderName(directValue);
        } else if (notBlank(envVarValue)) {
            logger.debug("JasyptEncryptionConverterPlugin use env var for " + description);
            config.setProviderNameEnvName(envVarValue);
        } else if (notBlank(sysPropValue)) {
            config.setProviderNameSysPropertyName(sysPropValue);
            logger.debug("JasyptEncryptionConverterPlugin use sys prop for " + description);
            System.clearProperty(sysPropValue);
        } else if (required) {
            throw new IllegalStateException(
                    description + ", " + description + "EnvVarName, or " + description + "SysPropName is required"
            );
        } else {
            return false;
        }
        return true;
    }

    private boolean addProviderClassNameValue(
            final EnvironmentPBEConfig config,
            final String directValue,
            final String envVarValue,
            final String sysPropValue,
            final boolean required,
            final String description
    )
    {
        if (notBlank(directValue)) {
            logger.debug("JasyptEncryptionConverterPlugin use value for " + description);
            config.setProviderClassName(directValue);
        } else if (notBlank(envVarValue)) {
            logger.debug("JasyptEncryptionConverterPlugin use env var for " + description);
            config.setProviderClassNameEnvName(envVarValue);
        } else if (notBlank(sysPropValue)) {
            config.setProviderClassNameSysPropertyName(sysPropValue);
            logger.debug("JasyptEncryptionConverterPlugin use sys prop for " + description);
            System.clearProperty(sysPropValue);
        } else if (required) {
            throw new IllegalStateException(
                    description + ", " + description + "EnvVarName, or " + description + "SysPropName is required"
            );
        } else {
            return false;
        }
        return true;
    }

    private boolean addKeyObtentionIterationsValue(
            final EnvironmentPBEConfig config,
            final String directValue,
            final String envVarValue,
            final String sysPropValue,
            final boolean required,
            final String description
    )
    {
        if (notBlank(directValue)) {
            logger.debug("JasyptEncryptionConverterPlugin use value for " + description);
            config.setKeyObtentionIterations(directValue);
        } else if (notBlank(envVarValue)) {
            logger.debug("JasyptEncryptionConverterPlugin use env var for " + description);
            config.setKeyObtentionIterationsEnvName(envVarValue);
        } else if (notBlank(sysPropValue)) {
            config.setKeyObtentionIterationsSysPropertyName(sysPropValue);
            logger.debug("JasyptEncryptionConverterPlugin use sys prop for " + description);
            System.clearProperty(sysPropValue);
        } else if (required) {
            throw new IllegalStateException(
                    description + ", " + description + "EnvVarName, or " + description + "SysPropName is required"
            );
        } else {
            return false;
        }
        return true;
    }

    private boolean notBlank(final String value) {
        return null != value && !"".equals(value);
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
