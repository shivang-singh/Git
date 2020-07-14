package gitlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class represents one commit
 * and all of its contents.
 * @author shivang singh
 */
public class Commit implements Serializable {
    /**Class constructor. Represents a commit, which holds
     * information about all the files the make up this
     * snapshot. Holds metadata, maps to files and names,
     * as well as references to parent.
     * @param msg message.
     * @param p1 p2
     * @param p2  p1
     * @param time  timesStamp
     */
    Commit(String msg, String time, String p1, String p2) {
        logMsg = msg;
        timeStamp = time;
        parent1 = p1;
        parent2 = p2;
        uid = Utils.sha1(Utils.serialize(this));
    }

    /** Normal constructor to create new commit.
     * @param parent Parent of commit
     * @param msg Message
     * @param time Timestamp
     */
    @SuppressWarnings({"unchecked", "deprecated"})
    Commit(Commit parent, String msg, String time) {
        logMsg = msg;
        timeStamp = time;
        parent1 = parent.getUID();
        blobMap = (HashMap<String, String>) parent.blobMap.clone();
        uid = Utils.sha1(Utils.serialize(this));
    }

    /** Sets uid.
     * @param x the uid to use
     */
    void setUID(String x) {
        uid = x;
    }

    /** Returns UID.
     * @return
     */
    String getUID() {
        return uid;
    }

    /** Returns p1.
     * @return
     */
    String getP1() {
        return parent1;
    }

    /** Returns both parents.
     * @return
     */
    ArrayList<String> getParents() {
        ArrayList<String> toRet = new ArrayList<>();
        if (parent1 != null) {
            toRet.add(parent1);
        }
        if (parent2 != null) {
            toRet.add(parent2);
        }
        return toRet;
    }

    /**Returns p2.
     * @return
     */
    String getP2() {
        return parent2;
    }

    /** Gets the message this commit was made with.
     * @return
     */
    String getLogMsg() {
        return logMsg;
    }

    /** Takes care of formatting for log.
     */
    void log() {
        System.out.println("===");
        System.out.println("commit " + getUID());
        if (getP2() != null) {
            System.out.println("Merge: " + getP1().substring(0, 7)
                    + " " + getP2().substring(0, 7));
        }
        System.out.println("Date: " + timeStamp);
        System.out.println(getLogMsg());
        System.out.println();
    }

    /** Returns blobMap.
     * @return
     */
    HashMap<String, String> getMap() {
        return blobMap;
    }

    /** Stores the message user inputted for commit. */
    private String logMsg;
    /** Holds the timestamp for the commit. */
    private String timeStamp;
    /** String universal Id for this commit. */
    private String uid;
    /** Contains a reference to the 1st parent of commit. */
    private String parent1;
    /** Contains a reference to the 2nd parent of commit if
     * it exists.
     */
    private String parent2;
    /** This Hashmap has key value pairs that consist of the file
     * name, and the corresponding UID for that blob.
     */
    private HashMap<String, String> blobMap = new HashMap<String, String>();
}
