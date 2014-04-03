package com.dtolabs.rundeck.core.storage;

import com.dtolabs.rundeck.core.authorization.AuthContext;
import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.Resource;

import java.util.Set;

/**
 * AuthTree authenticated facade to {@link org.rundeck.storage.api.Tree}
 *
 * @author greg
 * @since 2014-03-20
 */
public interface AuthTree<T extends ContentMeta> extends ExtTree<AuthContext, T> {

}
