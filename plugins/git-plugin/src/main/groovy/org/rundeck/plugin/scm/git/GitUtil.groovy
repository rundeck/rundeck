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

import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import com.dtolabs.rundeck.plugins.scm.ScmPluginInvalidInput
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffAlgorithm
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.EditList
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.errors.MissingObjectException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevSort
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.PathFilterGroup
import org.eclipse.jgit.util.io.DisabledOutputStream

/**
 * Created by greg on 9/10/15.
 */
class GitUtil {
    /**
     * get RevCommit for HEAD rev of the path
     * @return RevCommit or null if HEAD not found (empty git)
     */
    static RevCommit getHead(Repository repo) {
        getCommit(repo, Constants.HEAD)
    }
    /**
     * get RevCommit for HEAD rev of the path
     * @return RevCommit or null if HEAD not found (empty git)
     */
    static RevCommit getCommit(Repository repo, String commitId) {
        final RevWalk walk = new RevWalk(repo)
        walk.setRetainBody(true)

        try {
            def resolve = repo.resolve(commitId)
            if (!resolve) {
                return null
            }
            return walk.parseCommit(resolve)
        } catch (IOException e) {
            return null
        } finally {
            walk.close()
        }
    }

    /**
     * Blob id of a regular file at the given path in the commit tree.
     *
     * @return the object id, or null if the commit is null, the path is absent, or it is not a regular file
     */
    static ObjectId lookupId(Repository repo, RevCommit commit, String path) {
        if (!commit) {
            return null
        }
        final TreeWalk walk2 = TreeWalk.forPath(repo, path, commit.getTree())
        if (walk2 == null) {
            return null
        }
        try {
            if ((walk2.getRawMode(0) & FileMode.TYPE_MASK) != FileMode.TYPE_FILE) {
                return null
            }
            return walk2.getObjectId(0)
        } finally {
            walk2.close()
        }
    }

    /**
     * Guard against loading an oversized blob fully into memory: reject rather than risk an OOM.
     */
    static final long MAX_BLOB_SIZE = 100L * 1024 * 1024

    static byte[] getBytes(Repository repo, ObjectId id) {
        def loader = repo.open(id, Constants.OBJ_BLOB)
        if (loader.getSize() > MAX_BLOB_SIZE) {
            throw new ScmPluginException(
                    "Object ${id.name} is too large to load (${loader.getSize()} bytes, max ${MAX_BLOB_SIZE})"
            )
        }
        loader.getCachedBytes(MAX_BLOB_SIZE as int)
    }

    /**
     * print diff to output stream
     * @param out stream, or null to simply return count of differences
     * @param leftSide
     * @param rightSide
     * @param COMP
     * @return
     */
    static int diffContent(
            OutputStream out,
            byte[] leftSide,
            File rightSide,
            RawTextComparator COMP = RawTextComparator.DEFAULT
    ) {
        RawText rt1 = new RawText(leftSide)
        RawText rt2 = new RawText(rightSide)
        return diffContent(out, rt1, rt2, COMP)
    }

    /**
     * print diff to output stream
     * @param out stream, or null to simply return count of differences
     * @param leftSide
     * @param rightSide
     * @param COMP
     * @return
     */
    static int diffContent(
            OutputStream out,
            File leftSide,
            byte[] rightSide,
            RawTextComparator COMP = RawTextComparator.DEFAULT
    ) {
        RawText rt1 = new RawText(leftSide)
        RawText rt2 = new RawText(rightSide)
        return diffContent(out, rt1, rt2, COMP)
    }

    /**
     * print diff to output stream
     * @param out stream, or null to simply return count of differences
     * @param leftSide
     * @param rightSide
     * @param COMP
     * @return
     */
    static int diffContent(
            OutputStream out,
            byte[] leftSide,
            byte[] rightSide,
            RawTextComparator COMP = RawTextComparator.DEFAULT
    ) {
        RawText rt1 = new RawText(leftSide)
        RawText rt2 = new RawText(rightSide)
        return diffContent(out, rt1, rt2, COMP)
    }

    /**
     * print diff to output stream
     * @param out stream, or null to simply return count of differences
     * @param leftSide
     * @param rightSide
     * @param COMP
     * @return
     */
    static int diffContent(
            OutputStream out,
            RawText leftSide,
            RawText rightSide,
            RawTextComparator COMP = RawTextComparator.DEFAULT
    ) {
        EditList diffList = new EditList()
        DiffAlgorithm differ = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM)

        diffList.addAll(differ.diff(COMP, leftSide, rightSide))
        if (diffList.size() > 0 && out != null) {
            DiffFormatter formatter = new DiffFormatter(out)
            try {
                formatter.format(diffList, leftSide, rightSide)
            } finally {
                formatter.close()
            }
        }
        diffList.size()
    }

    static RevCommit lastCommit(Repository repo, Git git) {
        lastCommitForPath(repo, git, null)
    }

    static RevCommit lastCommitForPath(Repository repo, Git git, String path) {
        lastCommitForPath(repo, git, getHead(repo), path)
    }

    /**
     * Last commit touching the path, walking history from the given start commit instead of the current HEAD.
     *
     * @param start commit to start from; null yields null
     * @param path repository path, or null for the start commit's history
     * @return the most recent commit touching the path, or null if none
     */
    static RevCommit lastCommitForPath(Repository repo, Git git, RevCommit start, String path) {
        if (!start) {
            return null
        }
        def logb = git.log().add(start)
        if (path) {
            logb.addPath(path)
        }
        def log = logb.call()
        try {
            def iter = log.iterator()
            if (iter.hasNext()) {
                def commit = iter.next()
                if (commit) {
                    return commit
                }
            }
            null
        } finally {
            log.close()
        }
    }

    /**
     * Resolve the last commit touching each of the given paths with a single history walk from head, giving
     * exactly the commit that {@code git log -- path} reports for every path.
     *
     * <p>Mirrors JGit's path-filtered rev walk per path: a commit is attributed to a path when the path
     * differs from every parent (or exists in a root commit); when a merge leaves the path identical to some
     * parent, only the first such parent is followed for that path. Paths are carried down the commit graph
     * in topological order as pending sets, so the chains for all paths share one traversal. Paths never
     * found before the history is exhausted are absent from the result.
     *
     * @param repo repository
     * @param head commit to start walking from
     * @param paths paths to resolve
     * @return map of path to the last commit that touched it
     */
    static Map<String, RevCommit> lastCommitsForPaths(Repository repo, RevCommit head, Collection<String> paths) {
        Map<String, RevCommit> found = [:]
        if (!paths || !head) {
            return found
        }
        RevWalk walk = null
        TreeWalk tree = null
        try {
            walk = new RevWalk(repo)
            walk.retainBody = false
            walk.sort(RevSort.TOPO)
            RevCommit start = walk.parseCommit(head)
            walk.markStart(start)
            tree = new TreeWalk(repo)
            tree.recursive = true
            tree.filter = PathFilterGroup.createFromStrings(paths)
            Map<RevCommit, Set<String>> pending = [(start): new HashSet<String>(paths)]
            for (RevCommit commit = walk.next(); commit && pending; commit = walk.next()) {
                Set<String> want = pending.remove(commit)
                if (!want) {
                    continue
                }
                int parentCount = commit.parentCount
                commit.parents.each { walk.parseHeaders(it) }
                tree.reset((commit.parents*.tree + [commit.tree]) as ObjectId[])
                Set<String> seen = []
                while (tree.next()) {
                    String path = tree.pathString
                    if (!(path in want)) {
                        continue
                    }
                    seen << path
                    Integer sameParent = (0..<parentCount).find { int i ->
                        tree.getRawMode(i) == tree.getRawMode(parentCount) && tree.idEqual(i, parentCount)
                    }
                    if (sameParent == null) {
                        found[path] = commit
                    } else {
                        pending.computeIfAbsent(commit.parents[sameParent]) { new HashSet<String>() } << path
                    }
                }
                if (parentCount > 0 && seen.size() < want.size()) {
                    // paths absent from this commit and all its parents are unchanged here: follow the first parent
                    pending.computeIfAbsent(commit.parents[0]) { new HashSet<String>() }.addAll(want - seen)
                }
            }
            found.values().each { walk.parseBody(it) }
        } finally {
            tree?.close()
            walk?.close()
        }
        found
    }

    static List<DiffEntry> listChanges(Git git, String oldRef, String newRef) {
        ObjectReader reader = git.getRepository().newObjectReader()
        try {
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser()
            ObjectId oldTree = git.getRepository().resolve(oldRef)
            oldTreeIter.reset(reader, oldTree)

            CanonicalTreeParser newTreeIter = new CanonicalTreeParser()
            ObjectId newTree = git.getRepository().resolve(newRef)
            newTreeIter.reset(reader, newTree)

            DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)
            try {
                diffFormatter.setRepository(git.getRepository())
                diffFormatter.scan(oldTreeIter, newTreeIter)
            } finally {
                diffFormatter.close()
            }
        } finally {
            reader.close()
        }
    }

    static Map<String, Serializable> metaForCommit(RevCommit commit) {
        [
                commitId      : commit.name,
                commitId6     : commit.abbreviate(6).name(),
                date          : new Date(commit.commitTime * 1000L),
                authorName    : commit.authorIdent.name,
                authorEmail   : commit.authorIdent.emailAddress,
                authorTime    : commit.authorIdent.when,
                authorTimeZone: commit.authorIdent.timeZone.displayName,
                message       : commit.shortMessage
        ]
    }

    static Ref createTag(Git git, String tag, String message, RevCommit commit) {
        def tagb = git.tag().
                setAnnotated(true).
                setName(tag).
                setObjectId(commit).
                setMessage(message)

        return tagb.call()
    }

    /**
     * Find a tag ref by name
     * @param tag tag name
     * @param git git
     * @return tag ref or null
     */
    static Ref findTag(String tag, Git git) {
        def tagrefs = git.tagList().call()
        def found = tagrefs.find { Ref ref ->
            ref.name == Constants.R_TAGS + tag
        }
        return found
    }
    static List<String> listPaths(Git git, String ref, List<String> trackedItems = null, String trackingRegex = null) {
        ObjectId head = git.repository.resolve ref
        if (!head) {
            return null
        }
        def tree = new TreeWalk(git.repository)
        List<String> list = []
        try {
            tree.addTree(head)
            tree.setRecursive(true)
            if (trackedItems || trackingRegex) {
                if (trackingRegex) {
                    tree.setFilter(PathRegexFilter.create(trackingRegex))
                } else {
                    tree.setFilter(PathFilterGroup.createFromStrings(trackedItems))
                }
            }

            while (tree.next()) {
                list.add(tree.getPathString())
            }
        } finally {
            tree.close()
        }
        list
    }
}
