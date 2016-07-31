public class FileSystem {
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;

    public FileSystem( int diskBlocks ) {
        // create superblock, and format disk with 64 inodes in default
        superblock = new SuperBlock(diskBlocks);

        // create directory, and register "/" in directory entry 0
        directory = new Directory( superblock.totalInodes);

        // file table is created, and store directory in the file table
        filetable = new FileTable( directory );

        // directory reconstruction
        FileTableEntry dirEnt = open( "/", "r" );
        int dirSize = fsize( dirEnt );
        if ( dirSize > 0 ) {
            byte[] dirData = new byte[dirSize];
            read( dirEnt, dirData );               //read from a file to dirData[]
            directory.bytes2directory( dirData );   //write dirData[] to the directory
        }
        close( dirEnt );
    }

    void sync( ) {
        FileTableEntry ft = open("/", "w");     //It opens the root directory
        byte[] buf = dir.directory2bytes();     //As processing, convert the directory to bytes
        write(ft, buf);                         //Write the data to the disk
        close(ft);                              //Close root
        superblock.sync();                      //Call superBlock to continue the sync
    }

    boolean format( int files ) {

        int inodeBlocksRequired = files / SuperBlock.inodesPerBlock;
        if (files % SuperBlock.inodesPerBlock != 0) {
            inodeBlocksRequired += 1;
        }

        superblock.format(inodeBlocksRequired);
        return true;
    }

    FileTableEntry open( String filename, String mode ) {return null;}

    //Close the file corresponding to fd, commits all file transactions on this
    //file.
    public synchronized int close( FileTableEntry ftEnt ) {

        fte.count--;
        if (fte.count == 0)
            filetable.ffree(fte);		//unregisters from fd table of the calling thread's TCB
        return 0;                       //Returns 0 in success
    }

    int fsize( FileTableEntry ftEnt ) {
        return 0;
    }
    int read( FileTableEntry ftEnt, byte[] buffer ) {
        return 0;
    }
    int write( FileTableEntry ftEnt, byte[] buffer ) {
        return 0;
    }
    private boolean deallocAllBlocks( FileTableEntry ftEnt ) {
        return true;
    }
    boolean delete( String filename ) {
        return true;
    }

    public synchronized int seek( FileTableEntry ftEnt, int offset, int whence ) {
        if(whence < 1){											//If whence is SEEK_SET (= 0), the file's seek pointer is set to offset bytes
            ftEnt.seekPtr = 0 + offset;                         // from the beginning of the file
        }else if(whence == 1){									//If whence is SEEK_CUR (= 1), the file's seek pointer is set to
            ftEnt.seekPtr += offset;                            //its current value plus the offset. The offset can be positive or negative.
        }else if(whence == 2){									//If whence is SEEK_END (= 2), the file's seek pointer is set to the size of the
            ftEnt.seekPtr = ftEnt.inode.length + offset;        //file plus the offset. The offset can be positive or negative.
        }else{													//Set the pointer to the end of the file if whence is greater than 2.
            ftEnt.seekPtr = ftEnt.inode.length;
        }

        if (ftEnt.seekPtr > ftEnt.inode.length) {                 //If the pointer is beyond the file size, set seek pointer to the end of the ifle
            ftEnt.seekPtr = ftEnt.inode.length;
        }

        if (ftEnt.seekPtr < 0) {                                 //If the seek pointer is set to a negative number, it must clamp it to 0.
            ftEnt.seekPtr = 0;
        }

        return ftEnt.seekPtr;
    }

    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;


}
