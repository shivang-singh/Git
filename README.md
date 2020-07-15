# Git
Recreated Git command-line tool from scratch.

The main functionality that Gitlet supports is:

  1) Saving the contents of entire directories of files. In Gitlet, this is called committing, and the saved contents themselves are called commits.
  2) Restoring a version of one or more files or entire commits. In Gitlet, this is called checking out those files or that commit.
  3) Viewing the history of your backups. In Gitlet, you view this history in something called the log.
  4) Maintaining related sequences of commits, called branches.
  5) Merging changes made in one branch into another.
  
 
 
Commands:
  - init: Creates new version control system in directory
  - add: stages files for commit
  - rm: removes files from staging
  - commit: saves snapshot of current setup
  - log: displays info about each commit in current branch
  - global-log: displays info about each commit ever made
  - find: finds commit based on message
  - status: displays which branches currently exist, which files have been staged, untracked files, and modifications not staged
  - checkout: retrieve either a branch, a file from current commit, or file from specified commit
  - branch: creates new branch
  - rm-branch: deletes branch with given name
  - reset: checksout all files tracked by commit
  - merge: merges files from a given branch into current
 
