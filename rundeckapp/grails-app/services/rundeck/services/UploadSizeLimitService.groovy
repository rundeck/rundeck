/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
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
package rundeck.services

import grails.compiler.GrailsCompileStatic
import org.rundeck.app.config.SysConfigProp
import org.rundeck.app.config.SystemConfig
import org.rundeck.app.config.SystemConfigurable
import org.rundeck.util.Sizes
import org.rundeck.util.ThresholdInputStream

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.nio.charset.StandardCharsets

/**
 * Configurable upload size limits for Key Storage and Access Control policy files.
 *
 * Both limits default to {@code 2MB} and stay in effect when unset or invalid.
 * Customers change them in {@code rundeck-config.properties} or System Configuration:
 * <ul>
 *   <li>{@code rundeck.security.upload.keystorage.maxsize}</li>
 *   <li>{@code rundeck.security.upload.acl.maxsize}</li>
 * </ul>
 * Values use the file-size form {@code ###[tgkm][b]} (for example {@code 2MB}, {@code 512KB}, or {@code 2097152}).
 */
@GrailsCompileStatic
class UploadSizeLimitService implements SystemConfigurable {
    static transactional = false

    static final long DEFAULT_MAX_BYTES = 2L * 1024L * 1024L
    static final String DEFAULT_MAX_SIZE = '2MB'
    static final String KEY_STORAGE_PROPERTY = 'security.upload.keystorage.maxsize'
    static final String ACL_PROPERTY = 'security.upload.acl.maxsize'
    static final String KEY_STORAGE_CONFIG_KEY = 'rundeck.' + KEY_STORAGE_PROPERTY
    static final String ACL_CONFIG_KEY = 'rundeck.' + ACL_PROPERTY
    static final String ERROR_CODE = 'api.error.upload.filesize'

    static final SysConfigProp KEY_STORAGE_MAX_SIZE_PROP = sizeProp(
            KEY_STORAGE_CONFIG_KEY,
            'Key Storage upload max size',
            'Maximum size of one file uploaded to Key Storage (system or project keys). ' +
                    'File size such as 2MB or 512KB. Invalid or non-positive values use the 2MB default.'
    )
    static final SysConfigProp ACL_MAX_SIZE_PROP = sizeProp(
            ACL_CONFIG_KEY,
            'Access Control upload max size',
            'Maximum size of one Access Control policy file upload (system or project). ' +
                    'File size such as 2MB or 512KB. Invalid or non-positive values use the 2MB default.'
    )

    ConfigurationService configurationService

    /**
     * @return configured Key Storage maximum in bytes, or {@link #DEFAULT_MAX_BYTES}
     */
    long keyStorageMaxBytes() {
        return positiveFileSize(KEY_STORAGE_PROPERTY)
    }

    /**
     * @return configured Access Control maximum in bytes, or {@link #DEFAULT_MAX_BYTES}
     */
    long aclMaxBytes() {
        return positiveFileSize(ACL_PROPERTY)
    }

    /**
     * @return display label for the Key Storage limit
     */
    String keyStorageMaxSizeLabel() {
        return sizeLabel(KEY_STORAGE_PROPERTY)
    }

    /**
     * @return display label for the Access Control limit
     */
    String aclMaxSizeLabel() {
        return sizeLabel(ACL_PROPERTY)
    }

    @Override
    List<SysConfigProp> getSystemConfigProps() {
        return Collections.unmodifiableList([KEY_STORAGE_MAX_SIZE_PROP, ACL_MAX_SIZE_PROP])
    }

    /**
     * @param service wired service, or null in unit tests that have not injected it
     * @return Key Storage maximum in bytes
     */
    static long resolveKeyStorageMaxBytes(UploadSizeLimitService service) {
        return service != null ? service.keyStorageMaxBytes() : DEFAULT_MAX_BYTES
    }

    /**
     * @param service wired service, or null in unit tests that have not injected it
     * @return Access Control maximum in bytes
     */
    static long resolveAclMaxBytes(UploadSizeLimitService service) {
        return service != null ? service.aclMaxBytes() : DEFAULT_MAX_BYTES
    }

    /**
     * @param service wired service, or null when the default label should be used
     * @return Key Storage limit label
     */
    static String resolveKeyStorageMaxSizeLabel(UploadSizeLimitService service) {
        return service != null ? service.keyStorageMaxSizeLabel() : DEFAULT_MAX_SIZE
    }

    /**
     * @param service wired service, or null when the default label should be used
     * @return Access Control limit label
     */
    static String resolveAclMaxSizeLabel(UploadSizeLimitService service) {
        return service != null ? service.aclMaxSizeLabel() : DEFAULT_MAX_SIZE
    }

    /**
     * A negative length means the client did not declare a size and is not a violation.
     *
     * @param contentLength declared length, or negative when unknown
     * @param maxBytes allowed maximum
     * @return true when the declared length is over the maximum
     */
    static boolean declaredLengthExceeds(long contentLength, long maxBytes) {
        return contentLength >= 0L && contentLength > maxBytes
    }

    /**
     * @param text payload text
     * @param maxBytes allowed maximum
     * @return true when the UTF-8 encoding of {@code text} is over the maximum
     */
    static boolean textExceeds(String text, long maxBytes) {
        return text != null && text.getBytes(StandardCharsets.UTF_8).length > maxBytes
    }

    /**
     * Reads the request body, stopping once {@code maxBytes} is exceeded.
     *
     * @param request current request
     * @param maxBytes allowed maximum
     * @return body bytes
     * @throws UploadTooLargeException when the body is larger than {@code maxBytes}
     */
    static byte[] readBody(HttpServletRequest request, long maxBytes) {
        if (declaredLengthExceeds(request.getContentLengthLong(), maxBytes)) {
            throw new UploadTooLargeException(maxBytes)
        }
        return readLimited(request.getInputStream(), maxBytes)
    }

    /**
     * @param request current request
     * @param maxBytes allowed maximum
     * @return body decoded as UTF-8
     * @throws UploadTooLargeException when the body is larger than {@code maxBytes}
     */
    static String readText(HttpServletRequest request, long maxBytes) {
        return new String(readBody(request, maxBytes), StandardCharsets.UTF_8)
    }

    /**
     * @param input source stream
     * @param maxBytes allowed maximum
     * @return bytes read
     * @throws UploadTooLargeException when the stream is longer than {@code maxBytes}
     */
    static byte[] readLimited(InputStream input, long maxBytes) {
        ThresholdInputStream limited = new ThresholdInputStream(input, maxBytes)
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        byte[] buf = new byte[8192]
        try {
            int read = limited.read(buf)
            while (read >= 0) {
                out.write(buf, 0, read)
                read = limited.read(buf)
            }
        } catch (ThresholdInputStream.Threshold ignored) {
            throw new UploadTooLargeException(maxBytes)
        } finally {
            limited.close()
        }
        return out.toByteArray()
    }

    /**
     * @param maxSizeLabel configured size label shown to the caller
     * @return API error payload for a 413 response
     */
    static Map<String, Object> tooLargeError(String maxSizeLabel) {
        return [
                status: HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                code  : ERROR_CODE,
                args  : [maxSizeLabel]
        ]
    }

    private long positiveFileSize(String property) {
        long parsed = configurationService.getFileSize(property, DEFAULT_MAX_BYTES)
        return parsed > 0L ? parsed : DEFAULT_MAX_BYTES
    }

    private String sizeLabel(String property) {
        String configured = configurationService.getString(property, DEFAULT_MAX_SIZE)
        if (!configured) {
            return DEFAULT_MAX_SIZE
        }
        Long parsed = Sizes.parseFileSize(configured)
        if (parsed == null || parsed <= 0L) {
            return DEFAULT_MAX_SIZE
        }
        return configured
    }

    private static SysConfigProp sizeProp(String key, String label, String description) {
        return SystemConfig.builder()
                .key(key)
                .label(label)
                .description(description)
                .defaultValue(DEFAULT_MAX_SIZE)
                .datatype('String')
                .required(false)
                .encrypted(false)
                .restart(false)
                .visibility('Advanced')
                .category('Security')
                .authRequired('app_admin')
                .build()
    }
}
