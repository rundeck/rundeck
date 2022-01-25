package org.rundeck.core.auth.app;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.rundeck.core.auth.web.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility methods for AuthRequest
 */
public class NamedAuthRequestUtil {
    private NamedAuthRequestUtil() {
    }

    @Getter
    @RequiredArgsConstructor
    public static class TypedRequest
            implements TypedNamedAuthRequest
    {
        @NonNull
        private final String type;
        @NonNull
        private final String authGroup;
        @NonNull
        private final String namedAuth;
        private final String description;
    }

    public static TypedNamedAuthRequest authorizeRequest(String type, String group, String access, String description) {
        return new TypedRequest(type, group, access, description);
    }

    public static TypedNamedAuthRequest authorizeRequest(RdAuthorize authorize) {
        return authorizeRequest(authorize.type(), authorize.group(), authorize.access(), authorize.description());
    }

    /**
     * Create request for the annotation
     */
    public static TypedNamedAuthRequest authorizeRequest(RdAuthorizeApplicationType authorize) {
        return authorizeRequest(
                RundeckAccess.ApplicationType.typeForKind(authorize.type()),
                authorize.group(),
                authorize.access(),
                authorize.description()
        );
    }

    /**
     * Create request for the annotation
     */
    public static TypedNamedAuthRequest authorizeRequest(RdAuthorizeProjectType authorize) {
        return authorizeRequest(
                RundeckAccess.ProjectType.typeForKind(authorize.type()),
                authorize.group(),
                authorize.access(),
                authorize.description()
        );
    }

    public static TypedNamedAuthRequest authorizeRequest(RdAuthorizeSystem authorize) {
        return authorizeRequest(
                RundeckAccess.System.TYPE,
                authorize.group(),
                authorize.value(),
                authorize.description()
        );
    }

    public static TypedNamedAuthRequest authorizeRequest(RdAuthorizeProject authorize) {
        return authorizeRequest(
                RundeckAccess.Project.TYPE,
                authorize.group(),
                authorize.value(),
                authorize.description()
        );
    }

    public static TypedNamedAuthRequest authorizeRequest(RdAuthorizeProjectAcl authorize) {
        return authorizeRequest(
                RundeckAccess.ProjectAcl.TYPE,
                authorize.group(),
                authorize.value(),
                authorize.description()
        );
    }

    public static TypedNamedAuthRequest authorizeRequest(RdAuthorizeAdhoc authorize) {
        return authorizeRequest(
                RundeckAccess.Adhoc.TYPE,
                authorize.group(),
                authorize.value(),
                authorize.description()
        );
    }

    public static TypedNamedAuthRequest authorizeRequest(RdAuthorizeExecution authorize) {
        return authorizeRequest(
                RundeckAccess.Execution.TYPE,
                authorize.group(),
                authorize.value(),
                authorize.description()
        );
    }

    public static TypedNamedAuthRequest authorizeRequest(RdAuthorizeJob authorize) {
        return authorizeRequest(
                RundeckAccess.Job.TYPE,
                authorize.group(),
                authorize.value(),
                authorize.description()
        );
    }

    public static List<TypedNamedAuthRequest> requestsFromAnnotations(Method method) {
        List<TypedNamedAuthRequest> list = new ArrayList<>();
        list.addAll(Arrays
                            .stream(method.getAnnotationsByType(RdAuthorize.class))
                            .map(NamedAuthRequestUtil::authorizeRequest)
                            .collect(
                                    Collectors.toList()));
        list.addAll(Arrays
                            .stream(method.getAnnotationsByType(RdAuthorizeSystem.class))
                            .map(NamedAuthRequestUtil::authorizeRequest)
                            .collect(
                                    Collectors.toList()));
        list.addAll(Arrays
                            .stream(method.getAnnotationsByType(RdAuthorizeProject.class))
                            .map(NamedAuthRequestUtil::authorizeRequest)
                            .collect(
                                    Collectors.toList()));
        list.addAll(Arrays
                            .stream(method.getAnnotationsByType(RdAuthorizeAdhoc.class))
                            .map(NamedAuthRequestUtil::authorizeRequest)
                            .collect(
                                    Collectors.toList()));
        list.addAll(Arrays
                            .stream(method.getAnnotationsByType(RdAuthorizeExecution.class))
                            .map(NamedAuthRequestUtil::authorizeRequest)
                            .collect(
                                    Collectors.toList()));
        list.addAll(Arrays
                            .stream(method.getAnnotationsByType(RdAuthorizeJob.class))
                            .map(NamedAuthRequestUtil::authorizeRequest)
                            .collect(
                                    Collectors.toList()));
        list.addAll(Arrays
                            .stream(method.getAnnotationsByType(RdAuthorizeApplicationType.class))
                            .map(NamedAuthRequestUtil::authorizeRequest)
                            .collect(
                                    Collectors.toList()));
        list.addAll(Arrays
                            .stream(method.getAnnotationsByType(RdAuthorizeProjectAcl.class))
                            .map(NamedAuthRequestUtil::authorizeRequest)
                            .collect(
                                    Collectors.toList()));
        list.addAll(Arrays
                            .stream(method.getAnnotationsByType(RdAuthorizeProjectType.class))
                            .map(NamedAuthRequestUtil::authorizeRequest)
                            .collect(
                                    Collectors.toList()));
        return list;
    }
}
