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
        /*
        FileTableEntry ft = open("/", "w");        // It opens the root directory
        byte[] buf = directory.directory2bytes();  // As processing, convert the directory to bytes
        write(ft, buf);                            // Write the data to the disk
        close(ft);                                 // Close root
        superblock.sync();                         // Call superBlock to continue the sync
        */
    }

    boolean format( int files ) {

        int inodeBlocksRequired = files / SuperBlock.inodesPerBlock;
        if (files % SuperBlock.inodesPerBlock != 0) {
            inodeBlocksRequired += 1;
        }

        superblock.format(inodeBlocksRequired);
        return true;
    }

    FileTableEntry open( String filename, String mode ) {
        if (mode == "r" && this.directory.namei(filename) < 0) {
            return null;
        }

        return this.filetable.falloc(filename, mode);
    } //Anna

    //Close the file corresponding to fd, commits all file transactions on this
    //file.
   public synchronized boolean close( FileTableEntry ftEnt ) {
       return this.filetable.ffree(ftEnt);
    }

    int fsize( FileTableEntry ftEnt ) {
        return 0;
    } //paria

    int read( FileTableEntry ftEnt, byte[] buffer ) {
        return 0;
    } //paria

    int write( FileTableEntry ftEnt, byte[] buffer ) {
        if (buffer.length == 0) {
            return 0;
        }

        int offsetInBuffer = 0;
        while (offsetInBuffer != buffer.length) {
            short blockNumber = ftEnt.inode.getDataBlock(ftEnt.seekPtr);
            if (blockNumber < 0) {
                blockNumber = allocateNewDataBlock(ftEnt);
                if (blockNumber < 0) {
                    SysLib.sync();
                    return offsetInBuffer;
                }
            }

            byte[] blockBytes = new byte[Disk.blockSize];
            SysLib.rawread(blockNumber, blockBytes);

            int offsetInBlock = ftEnt.seekPtr % Disk.blockSize;
            int bytesToWriteInBlock = Math.min(buffer.length, Disk.blockSize - offsetInBlock);
            for (int i = 0; i < bytesToWriteInBlock; ++i) {
                blockBytes[i + offsetInBlock] = buffer[offsetInBuffer++];
            }
            SysLib.rawwrite(blockNumber, blockBytes);
            ftEnt.seekPtr += bytesToWriteInBlock;
        }

        SysLib.sync();
        return offsetInBuffer;
    } //Anna

    private boolean deallocAllBlocks( FileTableEntry ftEnt ) {
        return true;
    } //Anna

    boolean delete( String filename ) {
        return true;
    } // Paria

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

    private short allocateNewDataBlock(FileTableEntry fileTableEntry) {
        // Check if new data block can be referenced by direct pointers.
        for (int i = 0; i < fileTableEntry.inode.direct.length; ++i) {
            if (fileTableEntry.inode.direct[i] < 0) {
                short block = this.superblock.getFreeBlock();
                if (block < 0) {
                    return -1;
                }

                writeEmptyBlock(block);

                fileTableEntry.inode.direct[i] = block;
                fileTableEntry.inode.toDisk(fileTableEntry.iNumber);
                return block;
            }
        }

        // Check if indirect block of pointers has been created.
        if (fileTableEntry.inode.indirect < 0) {
            short indirectPointerBlock = this.superblock.getFreeBlock();
            if (indirectPointerBlock < 0) {
                return -2;
            }

            writeEmptyBlock(indirectPointerBlock);

            fileTableEntry.inode.indirect = indirectPointerBlock;
            fileTableEntry.inode.toDisk(fileTableEntry.iNumber);
        }

        // Create new block in indirect block of pointers.
        byte[] indirectPointerBlockBytes = new byte[Disk.blockSize];
        SysLib.rawread(fileTableEntry.inode.indirect, indirectPointerBlockBytes);

        final int sizeOfPointer = 2;
        short[] indirectPointers = new short[Disk.blockSize / sizeOfPointer];
        for (int i = 0; i < indirectPointers.length; ++i) {
            indirectPointers[i] = SysLib.bytes2short(indirectPointerBlockBytes, i * sizeOfPointer);
        }

        for (int i = 0; i < indirectPointers.length; ++i) {
            if (indirectPointers[i] < 0) {
                short block = this.superblock.getFreeBlock();
                if (block < 0) {
                    return -3;
                }

                indirectPointers[i] = block;
                for (int j = 0; j < indirectPointers.length; ++j) {
                    SysLib.short2bytes(indirectPointers[j], indirectPointerBlockBytes, j * sizeOfPointer);
                }
                SysLib.rawwrite(fileTableEntry.inode.indirect, indirectPointerBlockBytes);

                return block;
            }
        }

        return -4;
    }

    private void writeEmptyBlock(short blockNumber) {
        byte[] blockBytes = new byte[Disk.blockSize];
        SysLib.rawwrite(blockNumber, blockBytes);
    }

    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;


}
