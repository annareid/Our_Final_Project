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

    //======================SYNC===============================
    void sync( ) { }
    //======================SYNC===============================

    //======================FORMAT=============================
    boolean format( int files ) {

        int inodeBlocksRequired = files / SuperBlock.inodesPerBlock;
        if (files % SuperBlock.inodesPerBlock != 0) {
            inodeBlocksRequired += 1;
        }

        superblock.format(inodeBlocksRequired);
        return true;
    }

    //======================FORMAT=============================

    //======================OPEN===============================
    FileTableEntry open( String filename, String mode ) {
        if (mode == "r" && this.directory.namei(filename) < 0) {
            return null;
        }

        return this.filetable.falloc(filename, mode);
    } //Anna
    //======================OPEN===============================


    //======================CLOSE=============================
    //Close the file corresponding to fd, commits all file transactions on this
    //file.
   public synchronized boolean close( FileTableEntry fte ) {
       Inode iNode;

       if (fte == null)
           return false;

       synchronized (fte) {
           if ((iNode = fte.inode) == null)
               return false;
           if (iNode.flag == Inode.DELETE && fte.count == 0) {
               // deallocate file table entry if no more threads are using the
               // file and it has been marked as deleted.
               if (!directory.ifree(fte.iNumber))
                   return false;
           }
           if (!filetable.ffree(fte))
               return false;
       }
       return true;
    }
    //======================CLOSE=============================


    //======================WRITE=============================
    int write( FileTableEntry ftEnt, byte[] buffer ) {
        if (buffer.length == 0) {                                                   //Return 0 if the buffer is empty
            return 0;
        }

        int offsetInBuffer = 0;
        while (offsetInBuffer != buffer.length) {                                   //Loop until get to the end of the buffer
            short blockNumber = ftEnt.inode.getDataBlock(ftEnt.seekPtr);
            if (blockNumber < 0) {
                blockNumber = allocateNewDataBlock(ftEnt);
                if (blockNumber < 0) {
                    return offsetInBuffer;
                }
            }

            byte[] blockBytes = new byte[Disk.blockSize];
            SysLib.rawread(blockNumber, blockBytes);

            int offsetInBlock = ftEnt.seekPtr % Disk.blockSize;
            int bytesToWriteInBlock = Math.min(buffer.length - offsetInBuffer, Disk.blockSize - offsetInBlock);
            for (int i = 0; i < bytesToWriteInBlock; ++i) {
                blockBytes[i + offsetInBlock] = buffer[offsetInBuffer++];
            }
            SysLib.rawwrite(blockNumber, blockBytes);                               //Write the block on the disk

            ftEnt.seekPtr += bytesToWriteInBlock;
            if (ftEnt.seekPtr > ftEnt.inode.length) {
                ftEnt.inode.length = ftEnt.seekPtr;
            }
        }

        return offsetInBuffer;                                                      //Return the number of bytes written
    } //Anna
    //======================WRITE=============================

    //======================SEEK=============================
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
    //======================SEEK=============================

    //======================allocateNewDataBlock==============
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

        final int sizeOfPointer = 2;
        byte[] indirectPointerBlockBytes = new byte[Disk.blockSize];
        short[] indirectPointers = new short[Disk.blockSize / sizeOfPointer];
        if (fileTableEntry.inode.indirect < 0) {
            // Indirect block of pointers doesn't exist, create it.
            short indirectPointerBlock = this.superblock.getFreeBlock();
            if (indirectPointerBlock < 0) {
                return -2;
            }

            // Set indirect block.
            fileTableEntry.inode.indirect = indirectPointerBlock;

            // Initialize indirect block.
            for (int i = 0; i < indirectPointers.length; ++i) {
                indirectPointers[i] = -1;
            }

            // Write initialized indirect block back to disk.
            for (int j = 0; j < indirectPointers.length; ++j) {
                SysLib.short2bytes(indirectPointers[j], indirectPointerBlockBytes, j * sizeOfPointer);
            }
            SysLib.rawwrite(fileTableEntry.inode.indirect, indirectPointerBlockBytes);
            fileTableEntry.inode.toDisk(fileTableEntry.iNumber);
        }
        else {
            // Indirect block of pointers exists, read it.
            SysLib.rawread(fileTableEntry.inode.indirect, indirectPointerBlockBytes);
            for (int i = 0; i < indirectPointers.length; ++i) {
                indirectPointers[i] = SysLib.bytes2short(indirectPointerBlockBytes, i * sizeOfPointer);
            }
        }

        // Create new data block referenced by indirect pointer
        for (int i = 0; i < indirectPointers.length; ++i) {
            if (indirectPointers[i] < 0) {
                short block = this.superblock.getFreeBlock();
                if (block < 0) {
                    return -3;
                }

                // Set data block.
                indirectPointers[i] = block;

                // Write updated indirect block back to disk.
                for (int j = 0; j < indirectPointers.length; ++j) {
                    SysLib.short2bytes(indirectPointers[j], indirectPointerBlockBytes, j * sizeOfPointer);
                }
                SysLib.rawwrite(fileTableEntry.inode.indirect, indirectPointerBlockBytes);

                return block;
            }
        }

        return -4;
    }
    //======================allocateNewDataBlock==============

    //======================writeEmptyBlock===================
    private void writeEmptyBlock(short blockNumber) {
        byte[] blockBytes = new byte[Disk.blockSize];
        SysLib.rawwrite(blockNumber, blockBytes);
    }

    //======================FSIZE=============================
    public int fsize( FileTableEntry ftEnt )
    {
        return ftEnt.inode.length;
    }
    //======================FSIZE=============================

    //======================DELETE=============================
    public boolean delete( String filename ) {
        int iNumber;
        if (filename == "")                                // if blank file name, return false
            return false;

        if ((iNumber = directory.namei(filename)) == -1)  // get the iNumber for this filename
            return false;                                 // if it does not exist, return false

        return directory.ifree((short)iNumber);           // deallocate file, return success or failure
    }
    //======================DELETE=============================

    //======================READ=============================
    int read( FileTableEntry ftEnt, byte[] buffer ) {
        synchronized(ftEnt.inode) {									//Synchronize the read block
            int bufptr = 0;
            // while buffer isn't full or seek pointer hasn't reached end of file
            while (bufptr != buffer.length && ftEnt.seekPtr != ftEnt.inode.length) {    //Continue as long as bufptr doesn't reach buffer's length OR the pointer doesn't reach the inode's length
                byte[] inBlk = new byte[512];					                        //Create an array for the inBlocks
                int offset = ftEnt.seekPtr % Disk.blockSize;		                    //mod the pointer to the block
                int readLength = Math.min(Disk.blockSize-offset, buffer.length - bufptr);//get the minimum of the blocksize - offset/buffer length - offset.
                int curBlk = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);                 //Whichever min hits first is the condition to compare to
                SysLib.rawread(curBlk, inBlk);					                        //Find target block and then read it from the disk
                System.arraycopy(inBlk, offset, buffer, bufptr, readLength);            //Copy the part read into the buffer
                bufptr += readLength;							                        //Keep incrementing the readlength accordingly
                ftEnt.seekPtr+= readLength;						                      	//Adjust the pointer accordingly
            }
            return bufptr;										                         //Return a pointer to the number read
        }
    } //paria
    //======================READ=============================
}
