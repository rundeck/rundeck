package org.rundeck.plugin.scm.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffAlgorithm
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.EditList
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.TreeWalk
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
        getCommit(repo,Constants.HEAD)
    }
    /**
     * get RevCommit for HEAD rev of the path
     * @return RevCommit or null if HEAD not found (empty git)
     */
    static RevCommit getCommit(Repository repo, String commitId) {
        final RevWalk walk = new RevWalk(repo);
        walk.setRetainBody(true);

        def resolve = repo.resolve(commitId)
        if (!resolve) {
            return null
        }
        final RevCommit revCommit = walk.parseCommit(resolve);
        walk.release()
        revCommit
    }

    static ObjectId lookupId(Repository repo, RevCommit commit, String path) {
        if (!commit) {
            return null
        }
        final TreeWalk walk2 = TreeWalk.forPath(repo, path, commit.getTree());

        if (walk2 == null) {
            return null
        };
        if ((walk2.getRawMode(0) & FileMode.TYPE_MASK) != FileMode.TYPE_FILE) {
            return null
        };

        def id = walk2.getObjectId(0)
        walk2.release()
        return id;
    }

    static byte[] getBytes(Repository repo, ObjectId id) {
        repo.open(id, Constants.OBJ_BLOB).getCachedBytes(Integer.MAX_VALUE)
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
    )
    {
        RawText rt1 = new RawText(leftSide);
        RawText rt2 = new RawText(rightSide);
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
    )
    {
        RawText rt1 = new RawText(leftSide);
        RawText rt2 = new RawText(rightSide);
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
    )
    {
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
    )
    {
        EditList diffList = new EditList();
        DiffAlgorithm differ = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM)

        diffList.addAll(differ.diff(COMP, leftSide, rightSide));
        if (diffList.size() > 0 && out != null) {
            new DiffFormatter(out).format(diffList, leftSide, rightSide);
        }
        diffList.size()
    }

    static RevCommit lastCommit(Repository repo, Git git) {
        lastCommitForPath(repo, git, null)
    }

    static RevCommit lastCommitForPath(Repository repo, Git git, String path) {
        def head = getHead(repo)
        if (!head) {
            return null
        }
        def logb = git.log()
        if (path) {
            logb.addPath(path)
        }
        def log = logb.call()
        def iter = log.iterator()
        if (iter.hasNext()) {
            def commit = iter.next()
            if (commit) {
                return commit
            }
        }
        null
    }
    static List<DiffEntry> listChanges(Git git,String oldRef, String newRef){
        ObjectReader reader = git.getRepository().newObjectReader();

        CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
        ObjectId oldTree = git.getRepository().resolve( oldRef );
        oldTreeIter.reset( reader, oldTree );

        CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
        ObjectId newTree = git.getRepository().resolve( newRef );
        newTreeIter.reset( reader, newTree );

        DiffFormatter diffFormatter = new DiffFormatter( DisabledOutputStream.INSTANCE );
        diffFormatter.setRepository( git.getRepository() );
        diffFormatter.scan( oldTreeIter, newTreeIter );
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
}
