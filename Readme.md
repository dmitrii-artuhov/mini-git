# MiniGit

This is a toy git replica that is build according to the actual git specification.

## Functionality

* `init` - initializing the repository
* `add <files>` - adding a file
* `rm <files>` - the file is deleted from the repository, physically remains
* `status` - modified/deleted/not added files
* `commit <message>` with date and time
* `reset <to_revision>` - the behavior of `reset` is the same as `git reset --hard`
* `log [from_revision]`
* `checkout <revision>`
    * Possible values of `revision`:
        * `commit hash` - hash of the commit
        * `master` - return the branch to its original state
        * `HEAD~N`, where `N` is a non-negative integer. `HEAD~N` means the n-th commit before HEAD (`HEAD~0 == HEAD`)
* `checkout - <files>` - resets changes in files
* `branch-create <branch>` - create a branch named `<branch>`
* `branch-remove <branch>` - remove branch `<branch>`
* `show-branches` - show all available branches
