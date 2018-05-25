# How to Contribute to Rundeck

If you have a patch or pull request for us, thank you!  

The more of these guidelines you can follow, the easier (and faster) it is for us to evaluate your code and incorporate it into the development branch.

(Borrowing from [ThinkUp's Pull Request Checklist](https://github.com/ginatrapani/thinkup/wiki/Developer-Guide:-Pull-Request-Checklist)):

2. Make sure you can perform a full build and that the tests all pass. (See [Building and Testing])
3. Please add unit tests to demonstrate that your submission fixes an existing bug, or performs as intended.
1. Rebase your branch on the current state of the `master` branch. Use `git rebase master`.
   This means you might have to deal with conflicts.
1. Consolidate commits so that all related file changes are within a single commit.
   You can [squash commits](http://www.gitready.com/advanced/2009/02/10/squashing-commits-with-rebase.html)
   into a single commit by doing `git rebase -i`.  (This doesn't mean your entire PR has to be a single commit, just that
   any related changes are within a single commit.)
2. Be descriptive in your commit messages: explain the purpose of all changes.
   You can modify commit messages by doing `git commit --amend` (for the previous change),
   or `git rebase -i` for earlier changes.

Once your branch is cleaned up, please submit the Pull request against the `master` branch.

Thanks for your help!

A typical workflow:

~~~ {.sh}
# get up to date with origin/master and create a branch
git checkout master
git pull
git checkout -b newbranch
# now make your commits.  
$EDITOR somefile
git commit -m "my change"
# If you are now out of date with master, update your master:
git checkout master && git pull
# now go back to your branch and rebase on top of master
git checkout - # "-" will checkout previous branch
git rebase master
# resolve any conflicts, and possibly do `git rebase --continue` to finish rebasing
# now push your branch to your fork, and submit a pull request
git push myfork newbranch
~~~


[Building and Testing]: https://github.com/rundeck/rundeck/wiki/Building-and-Testing
