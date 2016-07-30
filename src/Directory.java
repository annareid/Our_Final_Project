/**
 * Created by Michael on 7/23/2015.
 */
public class Directory {
    private static int maxChars = 30; // max characters of each file name

    // Directory entries
    private int fsizes[];        // each element stores a different file size.
    private char fnames[][];    // each element stores a different file name.

    public Directory( int maxInumber ) { // directory constructor
        fsizes = new int[maxInumber];     // maxInumber = max files
        for ( int i = 0; i < maxInumber; i++ )
            fsizes[i] = 0;                 // all file size initialized to 0
        fnames = new char[maxInumber][maxChars];
        String root = "/";                // entry(inode) 0 is "/"
        fsizes[0] = root.length( );        // fsize[0] is the size of "/".
        root.getChars( 0, fsizes[0], fnames[0], 0 ); // fnames[0] includes "/"
    }

    public void bytes2directory( byte data[] ) {
        // assumes data[] received directory information from disk
        // initializes the Directory instance with this data[]


        // Assumption:  data[] starts with file sizes. File names are stored after the file sizes.
        int byteOffset = 0;
        for (int i = 0; i < fsizes.length; i++, byteOffset +=4){
            fsizes[i] = SysLib.bytes2int(data, byteOffset);
        }
        for (int i = 0; i < fnames.length; i++, byteOffset = maxChars *2){  // each char is 2 bytes
            String fname = new String(data, byteOffset, maxChars *2);
            fname.getChars(0, fsizes[i], fnames[i], 0);                     // getChars(int srcBegin, int srcEnd, char[] dst,  int dstBegin)
        }
    }

    public byte[] directory2bytes( ) {
        // converts and return Directory information into a plain byte array
        // this byte array will be written back to disk
        // note: only meaningfull directory information should be converted
        // into bytes.

        // Assumption the meaningful information is file size & file name.

        byte[] dirBytes = new byte[(fsizes.length * 4) + (fnames.length *maxChars *2)];
        int byteOffset = 0;
        for(int i = 0; i < fsizes.length; i++, byteOffset += 4)     //stores file sizes into array dirBytes
        {
            SysLib.int2bytes(fsizes[i], dirBytes, byteOffset);
        }
        for(int i = 0; i <fsizes.length; i++)       //stores file names into array dirBytes
        {
            for(int j = 0; j < maxChars * 2; j++ )
            {
                dirBytes[byteOffset] = (byte) fnames[i][j];
                byteOffset++;
            }
        }
        return dirBytes;
    }

    public short ialloc( String filename ) {
        // filename is the one of a file to be created.
        // allocates a new inode number for this filename

        return 0;
    }

    public boolean ifree( short iNumber ) {
        // deallocates this inumber (inode number)
        // the corresponding file will be deleted.

        return true;
    }

    public short namei( String filename ) {
        // returns the inumber corresponding to this filename

        return 0;
    }
}