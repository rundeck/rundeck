package org.rundeck.app.authorization.domain

import groovy.transform.CompileStatic
import org.rundeck.app.authorization.domain.appType.AppResourceTypeAuthorizingProvider
import org.rundeck.app.authorization.domain.execution.AppExecutionResourceAuthorizingProvider
import org.rundeck.app.authorization.domain.execution.AuthorizingExecution
import org.rundeck.app.authorization.domain.job.AppJobResourceAuthorizingProvider
import org.rundeck.app.authorization.domain.job.AuthorizingJob
import org.rundeck.app.authorization.domain.project.AppProjectAdhocResourceAuthorizingProvider
import org.rundeck.app.authorization.domain.project.AppProjectResourceAuthorizingProvider

import org.rundeck.app.auth.types.AuthorizingProject
import org.rundeck.app.authorization.domain.projectAcl.AppProjectAclAuthorizingProvider
import org.rundeck.core.auth.app.type.AuthorizingProjectAcl
import org.rundeck.core.auth.app.type.AuthorizingProjectAdhoc
import org.rundeck.app.authorization.domain.projectType.AppProjectTypeAuthorizingProvider
import org.rundeck.app.authorization.domain.system.AppSystemAuthorizingProvider
import org.rundeck.core.auth.app.type.AuthorizingAppType
import org.rundeck.core.auth.app.type.AuthorizingProjectType
import org.rundeck.core.auth.app.type.AuthorizingSystem
import org.rundeck.core.auth.access.AuthorizingAccess
import org.rundeck.core.auth.access.MissingParameter
import org.rundeck.core.auth.access.NotFound
import org.rundeck.core.auth.access.UnauthorizedAccess
import org.rundeck.core.auth.app.RundeckAccess
import org.rundeck.core.auth.access.ResIdResolver
import org.rundeck.core.auth.app.TypedNamedAuthRequest
import org.springframework.beans.factory.annotation.Autowired

import javax.security.auth.Subject

/**
 * Consolidated access to authorized resources
 */
@CompileStatic
class RundeckAppAuthorizer implements AppAuthorizer {
    @Autowired
    AppJobResourceAuthorizingProvider rundeckJobAuthorizer
    @Autowired
    AppExecutionResourceAuthorizingProvider rundeckExecutionAuthorizer
    @Autowired
    AppProjectResourceAuthorizingProvider rundeckProjectAuthorizer
    @Autowired
    AppSystemAuthorizingProvider rundeckSystemAuthorizer
    @Autowired
    AppProjectAdhocResourceAuthorizingProvider rundeckProjectAdhocAuthorizer
    @Autowired
    AppResourceTypeAuthorizingProvider rundeckAppResourceTypeAuthorizer
    @Autowired
    AppProjectTypeAuthorizingProvider rundeckProjectTypeAuthorizer
    @Autowired
    AppProjectAclAuthorizingProvider rundeckProjectAclAuthorizer


    @Override
    boolean isAuthorized(final Subject subject, ResIdResolver resolver, final TypedNamedAuthRequest request)
        throws MissingParameter {
        def access = getAuthorizingAccess(subject, resolver, request.getType())
        return access.isAuthorized(request)
    }

    @Override
    void authorize(final Subject subject, ResIdResolver resolver, final TypedNamedAuthRequest request)
        throws UnauthorizedAccess, NotFound, MissingParameter {
        def access = getAuthorizingAccess(subject, resolver, request.getType())
        access.authorizeNamed(request)
    }

    /**
     * Get authorizing access to a resource type, for types defined in {@link RundeckAccess}
     * @param subject  subject
     * @param resolver id resolver
     * @param type     resource type
     * @return
     * @throws MissingParameter
     */
    AuthorizingAccess getAuthorizingAccess(Subject subject, ResIdResolver resolver, String type)
        throws MissingParameter {
        if (type == RundeckAccess.System.TYPE) {
            return system(subject)
        } else if (type == RundeckAccess.Project.TYPE) {
            return project(subject, resolver)
        } else if (type == RundeckAccess.Adhoc.TYPE) {
            return adhoc(subject, resolver)
        } else if (type == RundeckAccess.Execution.TYPE) {
            return execution(subject, resolver)
        } else if (type == RundeckAccess.Job.TYPE) {
            return job(subject, resolver)
        } else if (type == RundeckAccess.ProjectAcl.TYPE) {
            return projectAcl(subject, resolver)
        } else if (type.startsWith(RundeckAccess.ApplicationType.TYPE)) {
            return applicationType(
                subject,
                resourceTypeResolver(
                    RundeckAccess.ApplicationType.TYPE,
                    RundeckAccess.ApplicationType.kindForTypeId(type)
                )
            )
        } else if (type.startsWith(RundeckAccess.ProjectType.TYPE)) {
            return projectType(
                subject,
                resourceTypeResolver(
                    resolver,
                    RundeckAccess.ProjectType.TYPE,
                    RundeckAccess.ProjectType.kindForTypeId(type)
                )
            )
        }
        throw new IllegalArgumentException("unknown type for authorizing access: " + type)
    }

    private ResIdResolver resourceTypeResolver(String typeId, String kind) {
        return resourceTypeResolver(null, typeId, kind)
    }

    /**
     * Parameter resolver with an override for a specific type
     * @param base fallback resovler, can be null
     * @param typeName resource type name
     * @param value value for the resource type
     * @return
     */
    private ResIdResolver resourceTypeResolver(ResIdResolver base, String typeName, String value) {
        new ResIdResolver() {
            @Override
            String idForType(final String type) throws MissingParameter {
                return idForTypeOptional(type)
                    .orElseThrow({ -> new MissingParameter("Internal:resource.type") });
            }

            @Override
            Optional<String> idForTypeOptional(final String type) {
                if (type == typeName) {
                    return Optional.of(value)
                } else if (base != null) {
                    return base.idForTypeOptional(type)
                } else {
                    return Optional.empty()
                }
            }
        }
    }

    @Override
    AuthorizingAppType applicationType(Subject subject, ResIdResolver resolver) {
        rundeckAppResourceTypeAuthorizer.getAuthorizingResource(subject, resolver)
    }

    @Override
    AuthorizingAppType applicationType(Subject subject, String type) {
        rundeckAppResourceTypeAuthorizer.getAuthorizingResource(subject, type)
    }

    @Override
    AuthorizingProjectType projectType(Subject subject, ResIdResolver resolver) {
        rundeckProjectTypeAuthorizer.getAuthorizingResource(subject, resolver)
    }

    @Override
    AuthorizingProjectType projectType(Subject subject, String project, String type) {
        rundeckProjectTypeAuthorizer.getAuthorizingResource(subject, project, type)
    }

    @Override
    AuthorizingExecution execution(Subject subject, ResIdResolver resolver) {
        rundeckExecutionAuthorizer.getAuthorizingResource(subject, resolver)
    }

    @Override
    AuthorizingExecution execution(final Subject subject, final String project, final String id) {
        rundeckExecutionAuthorizer.getAuthorizingResource(subject, project, id)
    }

    @Override
    AuthorizingJob job(Subject subject, ResIdResolver resolver) {
        rundeckJobAuthorizer.getAuthorizingResource(subject, resolver)
    }

    @Override
    AuthorizingJob job(final Subject subject, final String project, final String id) {
        rundeckJobAuthorizer.getAuthorizingResource(subject, project, id)
    }

    @Override
    AuthorizingProject project(Subject subject, ResIdResolver resolver) {
        rundeckProjectAuthorizer.getAuthorizingResource(subject, resolver)
    }

    @Override
    AuthorizingProject project(Subject subject, String project) {
        rundeckProjectAuthorizer.getAuthorizingResource(subject, project)
    }

    @Override
    AuthorizingProjectAdhoc adhoc(Subject subject, ResIdResolver resolver) {
        rundeckProjectAdhocAuthorizer.getAuthorizingResource(subject, resolver)
    }

    @Override
    AuthorizingProjectAdhoc adhoc(Subject subject, String project) {
        rundeckProjectAdhocAuthorizer.getAuthorizingResource(subject, project)
    }

    @Override
    AuthorizingProjectAcl projectAcl(Subject subject, ResIdResolver resolver) {
        rundeckProjectAclAuthorizer.getAuthorizingResource(subject, resolver)
    }

    @Override
    AuthorizingProjectAcl projectAcl(Subject subject, String project) {
        rundeckProjectAclAuthorizer.getAuthorizingResource(subject, project)
    }

    @Override
    AuthorizingSystem system(Subject subject) {
        rundeckSystemAuthorizer.getAuthorizingResource(subject, null)
    }
}
