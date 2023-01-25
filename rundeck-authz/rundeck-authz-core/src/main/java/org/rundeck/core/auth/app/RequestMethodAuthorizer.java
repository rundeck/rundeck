package org.rundeck.core.auth.app;

import java.lang.reflect.Method;
import java.util.List;

public interface RequestMethodAuthorizer {
    List<TypedNamedAuthRequest> requestsFromAnnotations(Method method);
}
