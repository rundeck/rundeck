package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * AuthorizationUtil provides utility methods for constructing authorization resource maps.
 *
 * @author greg
 * @since 2014-03-24
 */
public class AuthorizationUtil {

    public static final String TYPE_FIELD = "type";
    public static final String TYPE_KIND_FIELD = "kind";
    public static final String GENERIC_RESOURCE_TYPE_NAME = "resource";

    /**
     * Return a resource map for a resource of a certain type.
     *
     * @param type the type name
     *
     * @return the resource map
     */
    public static Map<String, String> resource(String type) {
        return resource(type, null);
    }

    /**
     * Return a resource map for a resource of a certain type, with attributes
     *
     * @param type the type name
     * @param meta the attributes about the resource
     *
     * @return the resource map
     */
    public static Map<String, String> resource(String type, Map<String, String> meta) {
        HashMap<String, String> authResource = new HashMap<String, String>();
        if (null != meta) {
            authResource.putAll(meta);
        }
        authResource.put(TYPE_FIELD, type);
        return authResource;
    }
    /**
     * Return a resource map for a resource of a certain type, with attributes
     *
     * @param type the type name
     * @param meta the attributes about the resource
     *
     * @return the resource map
     */
    public static Map<String, Object> resourceRule(String type, Map<String, Object> meta) {
        HashMap<String, Object> authResource = new HashMap<String, Object>();
        if (null != meta) {
            authResource.putAll(meta);
        }
        authResource.put(TYPE_FIELD, type);
        return authResource;
    }

    /**
     * Return a resource map for a generic resource type
     *
     * @param kind the resource type name
     *
     * @return the resource map describing a resource type
     */
    public static Map<String, String> resourceType(String kind) {
        return resourceType(kind, null);
    }

    /**
     * Return a resource map for a generic resource type
     *
     * @param kind the resource type name
     *
     * @return the resource map describing a resource type
     */
    public static Map<String, Object> resourceTypeRule(String kind) {
        return resourceTypeRule(kind, null);
    }

    /**
     * Return a resource map for a generic resource type
     *
     * @param kind the resource type name
     * @param meta the attributes about the resource type
     *
     * @return the resource map describing a resource type
     */
    public static Map<String, String> resourceType(String kind, Map<String, String> meta) {
        HashMap<String, String> authResource = new HashMap<String, String>();
        if (null != meta) {
            authResource.putAll(meta);
        }
        authResource.put(TYPE_FIELD, GENERIC_RESOURCE_TYPE_NAME);
        authResource.put(TYPE_KIND_FIELD, kind);
        return authResource;
    }
    /**
     * Return a resource map for a generic resource type
     *
     * @param kind the resource type name
     * @param meta the attributes about the resource type
     *
     * @return the resource map describing a resource type
     */
    public static Map<String, Object> resourceTypeRule(String kind, Map<String, Object> meta) {
        HashMap<String, Object> authResource = new HashMap<String, Object>();
        if (null != meta) {
            authResource.putAll(meta);
        }
        authResource.put(TYPE_FIELD, GENERIC_RESOURCE_TYPE_NAME);
        authResource.put(TYPE_KIND_FIELD, kind);
        return authResource;
    }

    /**
     * Create a project context singleton attribute set
     * @param projectName project name
     * @return attribute set
     */
    public static Set<Attribute> projectContext(String projectName) {
        return context("project", projectName);
    }

    /**
     * Create a singleton context attribute set
     * @param key context key
     * @param value context value
     * @return attribute set
     */
    public static Set<Attribute> context(String key, String value){
        if(null==key) {
            throw new IllegalArgumentException("key cannot be null");
        }
        if(null==value){
            throw new IllegalArgumentException("value cannot be null");
        }
        return Collections.singleton(
                new Attribute(
                        URI.create(EnvironmentalContext.URI_BASE + key),
                        value
                )
        );
    }

    /**
     * Generate a string representation of the context attribute set
     * @param context context set
     * @return string
     */
    public static String contextAsString(final Set<Attribute> context) {
        StringBuilder sb = new StringBuilder();
        for (Attribute attribute : context) {
            if(sb.length()<1) {
                sb.append("{");
            }else {
                sb.append(", ");
            }
            sb.append(Attribute.propertyKeyForURIBase(attribute, EnvironmentalContext.URI_BASE))
              .append("=")
              .append(attribute.getValue())
              ;
        }
        sb.append("}");
        return sb.toString();
    }
}
