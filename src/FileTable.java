/**
 * Created by Anna on 7/21/2016.
 */
import java.util.*;
public class FileTable {

    private Vector table;         // the actual entity of this file table
    private Directory dir;        // the root directory

    public FileTable( Directory directory ) { // constructor
        table = new Vector( );     // instantiate a file (structure) table
        dir = directory;           // receive a reference to the Director
    }                             // from the file system


    // major public methods
     public synchronized FileTableEntry falloc( String filename, String mode ) {
        // allocate a new file (structure) table entry for this file name
        // allocate/retrieve and register the corresponding inode using dir
        // increment this inode's count
        // immediately write back this inode to the disk
        // return a reference to this file (structure) table entry

    return null;

    }

    public synchronized boolean ffree( FileTableEntry e ) {
        // receive a file table entry reference
        // save the corresponding inode to the disk
        // free this file table entry.
        // return true if this file table entry found in my table

        if(table.removeElement(e))
        {
            e.inode.flag = 0;                //set flag to indicate removed entry
            if(e.inode.count != 0)
                e.inode.count--;             //Decrement count
            e.inode.toDisk(e.iNumber);       // save the corresponding inode to the disk
            e = null;

            notify();                       //wakes up the threads waiting on this slot
            return true;
        }

        return false;
    }


    public synchronized boolean fempty( ) {
        return table.isEmpty( );            // return if table is empty
    }                                       // should be called before starting a format
}