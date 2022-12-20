package org.rundeck.core.auth.app;

import org.rundeck.core.auth.web.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Defines base authorization annotations for controller actions
 */
public class BaseAppRequestMethodAuthorizer
        implements RequestMethodAuthorizer
{
    public List<TypedNamedAuthRequest> requestsFromAnnotations(Method method) {
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
