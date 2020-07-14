package gitlet;

import java.io.File;
import java.io.Serializable;

/** This class represents a blob, or file.
 * @author shivang singh
 */
public class Blob implements Serializable {
    /** Class constructor. This will save the contents of a
     * file passed in to blob.
     * @param contents represents the contents of File.
     */
    Blob(File contents) {
        _name = contents.getName();
        _contents = Utils.readContents(contents);
        stringCont = Utils.readContentsAsString(contents);
        _UID = Utils.sha1(_contents);
    }

    /** This method allows other classes to acces UID.
     * @return
     * */
    String getUID() {
        return _UID;
    }

    /** Return contents of file as byte array.
     * @return
     */
    byte[] getContents() {
        return _contents;
    }

    /** Returns contents of file as string.
     * @return
     */
    String getStringCont() {
        return stringCont;
    }

    /** Represents this blobs Universal ID. */
    private String _UID;

    /** Holds the serialized contents of the blob. */
    private byte[] _contents;

    /** Holds blob name. */
    private String _name;

    /** Holds the String vers of contents. */
    private String stringCont;

}
