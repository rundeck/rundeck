package org.rundeck.storage.conf;

import org.rundeck.storage.api.Path;

/**
 * SubPath is ...
 *
 * @author Greg Schueler <greg@simplifyops.com>
 * @since 2014-03-27
 */
public interface SubPath {
    /**
     * Return the subpath
     */
    Path getSubPath();

}
