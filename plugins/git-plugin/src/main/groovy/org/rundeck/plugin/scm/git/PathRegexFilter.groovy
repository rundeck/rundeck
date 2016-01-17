package org.rundeck.plugin.scm.git

import org.eclipse.jgit.errors.IncorrectObjectTypeException
import org.eclipse.jgit.errors.MissingObjectException
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.PathFilter
import org.eclipse.jgit.treewalk.filter.TreeFilter

import java.util.regex.Pattern

/**
 * Created by greg on 9/15/15.
 */
class PathRegexFilter extends TreeFilter {
    String regex
    Pattern pattern

    public static PathRegexFilter create(String regex) {
        return new PathRegexFilter(regex)
    }

    private PathRegexFilter(final String regex) {
        this.regex = regex
        this.pattern = Pattern.compile(regex)
    }

    @Override
    boolean include(final TreeWalk walker) throws MissingObjectException, IncorrectObjectTypeException, IOException {
        if (walker.isSubtree()) {
            return true
        } else {
            return walker.getPathString().matches(pattern)
        };
    }

    @Override
    boolean shouldBeRecursive() {
        return true
    }

    @Override
    TreeFilter clone() {
        return this
    }
}
