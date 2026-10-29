/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
