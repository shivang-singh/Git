package gitlet;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.ArrayList;
import java.util.LinkedList;


/** This class represents a local repository.
 * @author shivang singh
 * */

public class Repository {
    /** Class constructor loads data to fields if
     *  we have initialized gitlet.
     */
    @SuppressWarnings({"unchecked", "deprecated"})
    Repository() {
        if (HEAD.exists()) {
            curBranch = Utils.readContentsAsString(HEAD);
            branchHeads = Utils.readObject(BRANCHES, HashMap.class);
            addMap = Utils.readObject(ADD, HashMap.class);
            toRemove = Utils.readObject(REMOVE, HashSet.class);
        }
    }


    /** Initializes our version control system.
     * Also sets up persistence.
     */
    @SuppressWarnings({"unchecked", "deprecation"})
    void initialize() throws IOException {
        File initialized = new File(".gitlet");
        if (initialized.exists()) {
            System.out.print("A Gitlet version-control system "
                    + "already exists in the current directory.");
            throw new GitletException();
        } else {
            GITLET.mkdir();
            STAGE.mkdir();
            ADD.createNewFile();
            REMOVE.createNewFile();
            COMMITS.mkdir();
            BLOBS.mkdir();
            BRANCHES.createNewFile();
            HEAD.createNewFile();
            Date d = new Date(0);
            d.setDate(0);
            SimpleDateFormat f = new SimpleDateFormat("EEE "
                    + "MMM d HH:mm:ss yyyy Z");
            Commit initialCommit = new Commit("initial commit",
                                f.format(d), null, null);
            String curID = initialCommit.getUID();
            curBranch = "master";
            branchHeads.put(curBranch, curID);
            File toCommits = new File(".gitlet//commits//" + curID);
            toCommits.createNewFile();
            Utils.writeObject(toCommits, initialCommit);
            Utils.writeObject(BRANCHES, branchHeads);
            Utils.writeContents(HEAD, curBranch);
            Utils.writeObject(ADD, addMap);
            Utils.writeObject(REMOVE, toRemove);
        }
    }

    /** This method will stage a file in add.txt.
     * @param fileName Name of file
     * @throws IOException
     * */
    void add(String fileName) throws IOException {
        File tmpFile = new File(fileName);
        if (!tmpFile.exists()) {
            System.out.println("File does not exist.");
            throw new GitletException("File does not exist.");
        }
        Blob b = new Blob(tmpFile);
        Commit temp = getCommit();
        if (temp.getMap().containsKey(fileName)) {
            if (temp.getMap().get(fileName).equals(b.getUID())) {
                addMap.remove(fileName);
            } else {
                addMap.put(fileName, b.getUID());
            }
        } else {
            addMap.put(fileName, b.getUID());
        }
        if (toRemove.contains(fileName)) {
            toRemove.remove(fileName);
        }
        File toAdd = new File(".gitlet//blobs//" + b.getUID());
        toAdd.createNewFile();
        Utils.writeObject(toAdd, b);
        Utils.writeObject(ADD, addMap);
        Utils.writeObject(REMOVE, toRemove);
    }

    /** Commits with given message and time.
     * @param msg message
     * @param time time stamp
     */
    void commit(String msg, String time) {
        if (addMap.size() == 0 && toRemove.size() == 0) {
            System.out.println("No changes added to the commit.");
            throw new GitletException("No changes added to the commit.");
        }
        if (msg == null || msg.equals("")) {
            System.out.println("Please enter a commit message.");
            throw new GitletException("Please enter a commit message.");
        }
        Commit par = getCommit();
        Commit current = new Commit(par, msg, time);
        for (String key: addMap.keySet()) {
            current.getMap().put(key, addMap.get(key));
        }
        for (String key: toRemove) {
            current.getMap().remove(key);
        }
        File newCommit = new File(".gitlet//commits//" + current.getUID());
        Utils.writeObject(newCommit, current);
        branchHeads.put(Utils.readContentsAsString(HEAD),
                            current.getUID());
        addMap.clear();
        toRemove.clear();
        Utils.writeObject(BRANCHES, branchHeads);
        Utils.writeObject(ADD, addMap);
        Utils.writeObject(REMOVE, toRemove);
    }

    /** Removes a file.
     * @param fileName Name of file
     */
    @SuppressWarnings({"unchecked", "deprecated"})
    void remove(String fileName) {
        Commit curCommit = getCommit();
        addMap = (HashMap) Utils.readObject(ADD, HashMap.class);
        if (!addMap.containsKey(fileName)
                    && !curCommit.getMap().containsKey(fileName)) {
            System.out.println("No reason to remove the file.");
            throw new GitletException();
        }
        if (addMap.containsKey(fileName)) {
            addMap.remove(fileName);
        }
        if (curCommit.getMap().containsKey(fileName)) {
            toRemove.add(fileName);
            Utils.restrictedDelete(fileName);
        }
        Utils.writeObject(ADD, addMap);
        Utils.writeObject(REMOVE, toRemove);
    }

    /**
     * Returns a log for user.
     */
    void log() {
        for (Commit c = getCommit(); c != null; c = loadCommit(c.getP1())) {
            c.log();
        }

    }

    /**
     * Performs gitlet globalLog operation.
     */
    void globalLog() {
        File[] listC = COMMITS.listFiles();
        for (File file: listC) {
            Commit c = loadCommit(file.getName());
            c.log();
        }
    }

    /** Performs status uperation.
     */
    void status() {
        HashSet<String> seen;
        PriorityQueue<String> multiUse = new PriorityQueue<>();
        System.out.println("=== Branches ===");
        for (String k: branchHeads.keySet()) {
            multiUse.offer(k);
        }
        while (!multiUse.isEmpty()) {
            String o = multiUse.poll();
            if (o.equals(curBranch)) {
                System.out.println("*" + curBranch);
            } else {
                System.out.println(o);
            }
        }
        System.out.println("\n=== Staged Files ===");
        for (String k: addMap.keySet()) {
            multiUse.offer(k);
        }
        while (!multiUse.isEmpty()) {
            System.out.println(multiUse.poll());
        }
        System.out.println("\n=== Removed Files ===");
        removedFiles(multiUse);
        while (!multiUse.isEmpty()) {
            System.out.println(multiUse.poll());
        }
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        for (String k: getCommit().getMap().keySet()) {
            File inCWD = new File(k);
            if (!addMap.containsKey(k) && !toRemove.contains(k)) {
                if (inCWD.exists() && !addMap.containsKey(k)) {
                    Blob b = new Blob(inCWD);
                    if (!b.getUID().equals(getCommit().getMap().get(k))
                            && !toRemove.contains(k)) {
                        multiUse.offer(k + " (modified)");
                    }
                } else {
                    multiUse.offer(k + " (deleted)");
                }
            }
        }
        while (!multiUse.isEmpty()) {
            System.out.println(multiUse.poll());
        }
        System.out.println("\n=== Untracked Files ===");
        untracked(multiUse);
        while (!multiUse.isEmpty()) {
            System.out.println(multiUse.poll());
        }
        System.out.println();
    }

    /** Returns removed files for status.
     * @param multiUse pq
     */
    private void removedFiles(PriorityQueue<String> multiUse) {
        for (String s : toRemove) {
            multiUse.offer(s);
        }
    }

    /** Untracked for status.
     * @param multiUse the pq
     */
    private void untracked(PriorityQueue<String> multiUse) {
        String curDir = System.getProperty("user.dir");
        File curD = new File(curDir);
        for (File x: curD.listFiles()) {
            String s = x.getName();
            if (!s.equals(GITLET.getName())
                    && !getCommit().getMap().containsKey(s)
                    && !addMap.containsKey(s) && !toRemove.contains(s)) {
                multiUse.offer(s);
            }
        }
    }


    /** Checks out a specific file.
     * @param fileName Name of file
     */
    void checkout(String fileName) {
        Commit c = getCommit();
        if (!c.getMap().containsKey(fileName)) {
            System.out.println("File does not exist in the commit.");
            throw new GitletException("File does not exist in the commit.");
        }
        Blob b = loadBlob(c.getMap().get(fileName));
        byte[] content = b.getContents();
        File toChange = new File(fileName);
        Utils.writeContents(toChange, content);
    }

    /** Checks out a file from specified commit.
     * @param cID Commit id
     * @param fileName Name of file
     */
    void checkout(String cID, String fileName) {
        Commit toUse = loadCommit(cID);
        if (!toUse.getMap().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            throw new GitletException("File does not exist in that commit.");
        }
        Blob b = loadBlob(toUse.getMap().get(fileName));
        byte[] content = b.getContents();
        File toChange = new File(fileName);
        Utils.writeContents(toChange, content);
    }

    /** Checks out a specific branch.
     * @param branchName Name of branch.
     * @param l Indicator.
     * @throws IOException
     */
    void checkout(String branchName, int l) throws IOException {
        if (!branchHeads.containsKey(branchName)) {
            System.out.println("No such branch exists.");
            throw new GitletException("No such branch exists");
        } else if (curBranch.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            throw new GitletException("No need to checkout the current branch");
        }
        Commit comToChange = getCommit();
        Commit newCom = loadCommit(branchHeads.get(branchName));
        String curDir = System.getProperty("user.dir");
        File curD = new File(curDir);
        for (File x: curD.listFiles()) {
            String s = x.getName();
            if (!comToChange.getMap().containsKey(s)
                    && newCom.getMap().containsKey(s)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                throw new GitletException();
            }
        }
        for (String s: comToChange.getMap().keySet()) {
            Utils.restrictedDelete(s);
        }
        for (String s: newCom.getMap().keySet()) {
            File oldBlob = new File(".gitlet//blobs//"
                    + newCom.getMap().get(s));
            File x = new File(s);
            x.createNewFile();
            Blob b = loadBlob(newCom.getMap().get(s));
            Utils.writeContents(x, b.getContents());
        }
        addMap.clear();
        toRemove.clear();
        Utils.writeObject(ADD, addMap);
        Utils.writeObject(REMOVE, toRemove);
        Utils.writeContents(HEAD, branchName);
    }

    /** Performs gitlet find operation.
     * @param msg Commits msg to find.
     */
    void findCWMsg(String msg) {
        File[] listC = COMMITS.listFiles();
        ArrayList<String> x = new ArrayList<>();
        for (File file: listC) {
            Commit c = loadCommit(file.getName());
            if (c.getLogMsg().equals(msg)) {
                x.add(c.getUID());
            }
        }
        if (x.size() == 0) {
            System.out.println("Found no commit with that message");
            throw new GitletException();
        }
        for (String s: x) {
            System.out.println(s);
        }
    }

    /** Creates new branch.
     * @param bName Name of branch
     */
    void branch(String bName) {
        if (branchHeads.containsKey(bName)) {
            System.out.println("A branch with that name already exists.");
            throw new GitletException("A branch with that "
                    + "name already exists.");
        }
        Commit curHead = getCommit();
        branchHeads.put(bName, curHead.getUID());
        Utils.writeObject(BRANCHES, branchHeads);
    }

    /** Removes reference to specific branch.
     * @param bName Name of branch to remove.
     */
    void rmBranch(String bName) {
        if (!branchHeads.containsKey(bName)) {
            System.out.println("A branch with that name does not exist.");
            throw new GitletException("A branch with that "
                    + "name already exists.");
        }
        if (bName.equals(curBranch)) {
            System.out.println("Cannot remove the current branch.");
            throw new GitletException("Cannot remove the current branch.");
        }
        branchHeads.remove(bName);
        Utils.writeObject(BRANCHES, branchHeads);
    }

    /** Reset Essentially checks out a commit.
     * @param cName Name of commit
     * @throws IOException
     */
    void reset(String cName) throws IOException {
        Commit toUse = loadCommit(cName);
        Commit comToChange = getCommit();
        String curDir = System.getProperty("user.dir");
        File curD = new File(curDir);
        for (File x: curD.listFiles()) {
            String s = x.getName();
            if (!comToChange.getMap().containsKey(s)
                    && toUse.getMap().containsKey(s)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                throw new GitletException();
            }
        }
        for (String s: comToChange.getMap().keySet()) {
            Utils.restrictedDelete(s);
        }
        for (String s: addMap.keySet()) {
            Utils.restrictedDelete(s);
        }
        for (String s: toRemove) {
            Utils.restrictedDelete(s);
        }
        for (String s: toUse.getMap().keySet()) {
            File x = new File(s);
            x.createNewFile();
            Blob b = loadBlob(toUse.getMap().get(s));
            Utils.writeContents(x, b.getContents());
        }
        addMap.clear();
        toRemove.clear();
        branchHeads.replace(curBranch, toUse.getUID());
        Utils.writeObject(ADD, addMap);
        Utils.writeObject(REMOVE, toRemove);
        Utils.writeObject(BRANCHES, branchHeads);
    }


    /** Performs the merge operation.
     * @param bName Name of branch to merge into current
     * @throws IOException
     */
    void merge(String bName) throws IOException {
        checkForBadMerge(bName);
        Commit c1 = getCommit();
        Commit c2 = loadCommit(branchHeads.get(bName));
        Commit splitPoint = cAncestor(c1, c2);
        boolean conflict = false;
        HashMap<String, String> c1map = c1.getMap();
        HashMap<String, String> c2map = c2.getMap();
        HashMap<String, String> splitmap = splitPoint.getMap();
        String curDir = System.getProperty("user.dir");
        File curD = new File(curDir);
        untrackedMerge(curD, bName);
        if (splitPoint.getUID().equals(c1.getUID())) {
            checkout(bName, 0);
            System.out.println("Current branch fast-forwarded");
            throw new GitletException();
        } else if (splitPoint.getUID().equals(c2.getUID())) {
            System.out.println("Given branch is an ancestor "
                    + "of the current branch.");
            throw new GitletException();
        } else {
            conflict = mergeOp1(splitmap, c1map, c2map, c2);
            for (String s: c2map.keySet()) {
                if (!splitmap.containsKey(s) && !c1map.containsKey(s)) {
                    checkout(c2.getUID(), s);
                    addMap.put(s, c2map.get(s));
                } else if (!splitmap.containsKey(s) && c1map.containsKey(s)
                            && !c2map.get(s).equals(c1map.get(s)))    {
                    String toWrite = "<<<<<<< HEAD\n"
                            + loadBlob(c1.getMap().get(s)).getStringCont()
                            + "=======\n"
                            + loadBlob(c2.getMap().get(s)).getStringCont()
                            + ">>>>>>>\n";
                    File xFile = new File(s);
                    xFile.createNewFile();
                    Utils.writeContents(xFile, toWrite);
                    conflict = true;
                }
            }
            for (String s: c1map.keySet()) {
                if (splitmap.containsKey(s) && !c2map.containsKey(s)) {
                    if (!splitmap.get(s).equals(c1map.get(s))) {
                        conflict = true;
                        String toWrite = "<<<<<<< HEAD\n"
                                + loadBlob(c1map.get(s)).getStringCont()
                                + "=======\n"
                                + "" + ">>>>>>>\n";
                        File xFile = new File(s);
                        xFile.createNewFile();
                        Utils.writeContents(xFile, toWrite);
                    }
                }
            }
        }
        String msg = String.format("Merged %s into %s.", bName, curBranch);
        mergeCommit(msg, c1.getUID(), c2.getUID());
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** Helper takes care of first set of merge ops.
     * @param splitmap map of splitpoint
     * @param c1map map of c1
     * @param c2map map of c2
     * @param c2 commit 2
     * @return
     */
    private boolean mergeOp1(HashMap<String, String> splitmap,
                             HashMap<String, String> c1map,
                             HashMap<String, String> c2map,
                             Commit c2) throws IOException {
        boolean conflict = false;
        for (String s: splitmap.keySet()) {
            if (c1map.containsKey(s) && c2map.containsKey(s)) {
                if (c1map.get(s).equals(splitmap.get(s))
                        && !c2map.get(s).equals(c1map.get(s))) {
                    checkout(c2.getUID(), s);
                } else if (!c1map.get(s).equals(splitmap.get(s))
                        && !c1map.get(s).equals(c2map.get(s))
                        && !c2map.get(s).equals(splitmap.get(s))) {
                    String toWrite = "<<<<<<< HEAD\n"
                            + loadBlob(c1map.get(s)).
                            getStringCont()
                            + "=======\n"
                            + loadBlob(c2map.
                            get(s)).getStringCont()
                            + ">>>>>>>\n";
                    File nFile = new File(s);
                    nFile.createNewFile();
                    Utils.writeContents(nFile, toWrite);
                    conflict = true;
                }
            } else if (!c2map.containsKey(s) && c1map.containsKey(s)) {
                if (c1map.get(s).equals(splitmap.get(s))) {
                    remove(s);
                } else if (!c1map.get(s).equals(splitmap.get(s))) {
                    String str = "<<<<<<< HEAD\n"
                            + loadBlob(c1map.get(s)).
                            getStringCont()
                            + "=======\n" + ""
                            + ">>>>>>>\n";
                    File cFile = new File(s);
                    Utils.writeContents(cFile, str);
                    conflict = true;
                }

            } else if (!c1map.containsKey(s) && c2map.containsKey(s)) {
                if (!c2map.get(s).equals(splitmap.get(s))) {
                    String str = "<<<<<<< HEAD\n" + ""
                            + "=======\n"
                            + loadBlob(c2map.get(s)).
                            getStringCont()
                            + ">>>>>>>\n";
                    File cFile = new File(s);
                    Utils.writeContents(cFile, str);
                    conflict = true;
                }
            }
        }
        return conflict;
    }

    /** Throw exception if improper merge.
     * @param bName Branch to merge.
     */
    private void checkForBadMerge(String bName) {
        if (bName.equals(curBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            throw new GitletException();
        } else if (!branchHeads.containsKey(bName)) {
            System.out.println("A branch with that name does not exist.");
            throw new GitletException();
        }
        if (!addMap.isEmpty() || !toRemove.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            throw new GitletException();
        }
    }

    /** Check if files are untracked for merge.
     * @param curD CWD
     * @param bName branch merge.
     */
    private void untrackedMerge(File curD, String bName) {
        Commit c1 = getCommit();
        Commit c2 = loadCommit(branchHeads.get(bName));
        Commit splitPoint = cAncestor(c1, c2);
        HashMap<String, String> c1map = c1.getMap();
        HashMap<String, String> c2map = c2.getMap();
        HashMap<String, String> splitmap = splitPoint.getMap();
        for (File x: curD.listFiles()) {
            if (!c1map.containsKey(x.getName())) {
                if (c2map.containsKey(x.getName())) {
                    File fileAsCurIS = new File(x.getName());
                    String contInDie = Utils.readContentsAsString(fileAsCurIS);
                    if (!loadBlob(c2map.get(x.getName())).
                            getStringCont().equals(contInDie)) {
                        System.out.println("There is an untracked file "
                                + "in the way; "
                                + "delete it, or add and commit it first.");
                        throw new GitletException();
                    }
                }
            }
        }
    }


    /** Commits the result of a merge.
     * @param msg Msg associated with merge.
     * @param p1 Parent 1
     * @param p2 Parent 2
     */
    void mergeCommit(String msg, String p1, String p2) {
        SimpleDateFormat f = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        Date d = new Date();
        Commit current = new Commit(msg, f.format(d), p1, p2);
        for (String key: addMap.keySet()) {
            current.getMap().put(key, addMap.get(key));
        }
        for (String key: toRemove) {
            current.getMap().remove(key);
        }
        String curDir = System.getProperty("user.dir");
        File curD = new File(curDir);
        for (File x: curD.listFiles()) {
            if (!x.getName().equals(".gitlet")) {
                Blob b = new Blob(x);
                current.getMap().put(x.getName(), b.getUID());
            }
        }
        File newCommit = new File(".gitlet//commits//" + current.getUID());
        Utils.writeObject(newCommit, current);
        branchHeads.put(Utils.readContentsAsString(HEAD),
                current.getUID());
        addMap.clear();
        toRemove.clear();
        Utils.writeObject(BRANCHES, branchHeads);
        Utils.writeObject(ADD, addMap);
        Utils.writeObject(REMOVE, toRemove);
    }

    /** Finds split point for a merge operation.
     * @param cur Represents Head commit of cur branch.
     * @param other Represents Head of branch to merge into cur.
     * @return
     */
    private Commit cAncestor(Commit cur, Commit other) {
        if (cur == null || other == null) {
            return null;
        }
        if (cur == other) {
            return cur;
        }
        HashSet<String> collisions = new HashSet<>();
        LinkedList<Commit> q = new LinkedList<Commit>();
        q.add(other);
        while (!q.isEmpty()) {
            Commit top = q.poll();
            collisions.add(top.getUID());
            for (String s: top.getParents()) {
                Commit p = loadCommit(s);
                q.add(p);
            }
        }
        q.add(cur);
        while (!q.isEmpty()) {
            Commit top = q.poll();
            if (collisions.contains(top.getUID())) {
                return top;
            }
            if (top.getP1() != null) {
                Commit p = loadCommit(top.getP1());
                q.add(p);
            }
            if (top.getP2() != null) {
                Commit p = loadCommit(top.getP2());
                q.add(p);
            }
        }
        return null;
    }

    /** Loads current head.
     * @return
     */
    Commit getCommit() {
        File headCommit = new File(".gitlet//commits//"
                        + branchHeads.get(curBranch));
        Commit par = (Commit) Utils.readObject(headCommit, Commit.class);
        return par;
    }

    /** Loads a commit given UID.
     * @param uid SHA1 for commit.
     * @return
     */
    Commit loadCommit(String uid) {
        if (uid == null) {
            return null;
        }
        Commit c = null;
        int n = uid.length();
        File commit = new File(".gitlet//commits//" + uid);
        if (commit.exists()) {
            c = Utils.readObject(commit, Commit.class);
        } else {
            for (File x: COMMITS.listFiles()) {
                if (x.getName().substring(0, n).equals(uid)) {
                    c = Utils.readObject(x, Commit.class);
                }
            }

        }
        if (c == null) {
            System.out.println("No commit with that id exists.");
            throw new GitletException("No commit with that id exists.");
        }
        return c;
    }

    /** Can load a blob given UID.
     * @param uid SHA1 for blob.
     * @return
     */
    Blob loadBlob(String uid) {
        File blob = new File(".gitlet//blobs//" + uid);
        Blob b = Utils.readObject(blob, Blob.class);
        return b;
    }



    /** Name of current branch. */
    private String curBranch;
    /** Map of Branches to their heads. */
    private HashMap<String, String> branchHeads = new HashMap<>();
    /** Collection of Files to be staged for adding. */
    private HashMap<String, String> addMap = new HashMap<>();
    /** Set of files to be removed. */
    private HashSet<String> toRemove = new HashSet<>();
    /**



    /** Current Working Directory. */
    static final File CWD = new File(".");
    /** Gitlet folder. */
    static final File GITLET = new File(".gitlet");
    /** Hold staged files. */
    static final File STAGE = new File(".gitlet//stage");
    /** Files staged for addition. */
    static final File ADD = new File(".gitlet//stage//add.txt");
    /** Files staged for removal. */
    static final File REMOVE = new File(".gitlet//stage//remove.txt");
    /** Hold commits. */
    static final File COMMITS = new File(".gitlet//commits");
    /** Holds all blobs. */
    static final File BLOBS = new File(".gitlet//blobs");
    /** Holds the branches. */
    static final File BRANCHES = new File(".gitlet//branches.txt");
    /** Keeps track of current head. */
    static final File HEAD = new File(".gitlet//head.txt");
}
