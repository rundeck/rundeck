package org.rundeck.storage.conf;

import org.rundeck.storage.api.Path;

/**
 * SubPath is ...
 *
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-03-27
 */
public interface SubPath {
    /**
     * Return the subpath
     * @return the subpath
     */
    Path getSubPath();

}
