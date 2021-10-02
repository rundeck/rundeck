package org.rundeck.core.auth.access;

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor;

import javax.security.auth.Subject;
import java.util.Map;

/**
 * Base implementation for a singleton application resources
 */
public abstract class BaseSingletonAuthorizingResource
        extends BaseAuthorizingResource<Singleton>
{
    public BaseSingletonAuthorizingResource(
            final AuthContextProcessor rundeckAuthContextProcessor,
            final Subject subject
    )
    {
        super(rundeckAuthContextProcessor, subject);
    }

    /**
     * @return authorization resource map for the singleton
     */
    protected abstract Map<String, String> getAuthresMapForSingleton();

    @Override
    protected Map<String, String> authresMapForResource(final Singleton resource) {
        return getAuthresMapForSingleton();
    }

    @Override
    protected Singleton retrieve() {
        return Singleton.ONLY;
    }

    @Override
    protected boolean exists() {
        return true;
    }
}
