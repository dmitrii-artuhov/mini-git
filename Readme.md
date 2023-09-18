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


## Implementation insights

The mini-git repository is stored as a tree data structure. There is a couple of file types that I used (git also uses them) that are related to this tree abstraction and to the mini-git implementation in general:
- `BlobFile`: these are actual files that are added to the repository.
- `TreeFile`: in order to reuse some files from previous commits we add edges to the our tree abstraction. The edges are represented by this file type.
- `CommitFile`: this is the commit file, it stores the hash of the root `TreeFile`. By traversing the tree starting at this root node we are able to extract all files that are related to the particular commit.
- There are some other files like `IndexFile`, `HeadFile`, and `BranchFile`: the last two store the current commit hash and current branch, respectively. Index file allows to stage new and updated files and compare them to those that are already commited.

You can get more insights from these articles:
- https://habr.com/ru/articles/313890/ (this one in russian, but you can translate the webpage)
- https://git-scm.com/book/en/v2 (chapter 10, "Git Internals")