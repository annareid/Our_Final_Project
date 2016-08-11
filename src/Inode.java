
public class Inode {
    private final static int iNodeSize = 32;                    // fix to 32 bytes
    private final static int directSize = 11;                   // # direct pointers
    public final static short DELETE = 1;

    public int length;                                          // file size in bytes
    public short count;                                         // # file-table entries pointing to this
    public short flag;                                          // 0 = unused, 1 = used, ...
    public short direct[] = new short[directSize];              // direct pointers
    public short indirect;                                      // a indirect pointer

    public Inode( ) {                                           // a default constructor
        length = 0;
        count = 0;
        flag = 1;
        for ( int i = 0; i < directSize; i++ )
            direct[i] = -1;
        indirect = -1;
    }

    public Inode( short iNumber ) {                             // retrieving inode from disk
        byte[] inodeBytes = new byte[Disk.blockSize];           //buffer array
        SysLib.rawread(getBlock(iNumber), inodeBytes);          // read a block to the buffer array

        int byteOffSet = getByteOffsetInBlock(iNumber);         //offset in block for this inode

        // write length, count, flag, direct, indirect into the buffer array
        length = SysLib.bytes2int(inodeBytes, byteOffSet + 0);
        count = SysLib.bytes2short(inodeBytes, byteOffSet + 4);
        flag = SysLib.bytes2short(inodeBytes, byteOffSet + 6);
        for (int i = 0; i < direct.length; ++i)
        {
            direct[i] = SysLib.bytes2short(inodeBytes, byteOffSet + 8 + i * 2);
        }
        indirect = SysLib.bytes2short(inodeBytes, byteOffSet + 30);
    }

    public int toDisk( short iNumber ) {                        // save to disk as the i-th inode
        byte[] inodeBytes = new byte[iNodeSize];                //buffer array

        // write length, count, flag, direct, indirect into the buffer array
        SysLib.int2bytes(length, inodeBytes, 0);
        SysLib.short2bytes(count, inodeBytes, 4);
        SysLib.short2bytes(flag, inodeBytes, 6);
        for (int i = 0; i < direct.length; ++i)
        {
            SysLib.short2bytes(direct[i], inodeBytes, 8 + i * 2);
        }
        SysLib.short2bytes(indirect, inodeBytes, 30);

        byte[] blockBytes = new byte[Disk.blockSize];
        int block = getBlock(iNumber);
        SysLib.rawread(block, blockBytes);

        int byteOffset = getByteOffsetInBlock(iNumber);          //get the offset
        for (int i = 0; i < inodeBytes.length; ++i) {
            blockBytes[byteOffset + i] = inodeBytes[i];
        }

        //write the block back to the disk
        return SysLib.rawwrite(block, blockBytes);
    }

    // Based on the inode number passed to it, this method finds the right block for an inode to be stored.
    public int getBlock( short iNumber ) {
        return 1 + iNumber / SuperBlock.inodesPerBlock;
    }

    //Based on the inode number passed to it, this method finds the right byte offset for the inode.
    public int getByteOffsetInBlock( short iNumber ) {
        return iNumber % SuperBlock.inodesPerBlock * iNodeSize;
    }

    public short getDataBlock( int byteOffsetInFile ) {
        int blockOffset = byteOffsetInFile / Disk.blockSize;
        if (blockOffset < this.direct.length) {                         //
            return this.direct[blockOffset];
        }

        if (this.indirect < 0) {
            return this.indirect;
        }

        byte[] indirectPointersBlock = new byte[Disk.blockSize];
        SysLib.rawread(this.indirect, indirectPointersBlock);

        final int sizeOfPointer = 2;
        short[] indirectPointers = new short[Disk.blockSize / sizeOfPointer];
        for (int i = 0; i < indirectPointers.length; ++i) {
            indirectPointers[i] = SysLib.bytes2short(indirectPointersBlock, i * sizeOfPointer);
        }

        blockOffset = blockOffset - this.direct.length;
        return indirectPointers[blockOffset];
    }

    public int findTargetBlock(int seekptr){						//Used to find a target block
        if (seekptr > length)									//If the seeker is greater or equal to length, return false
            return -1;
        int ptr = seekptr/Disk.blockSize;
        if (ptr < 11)												//If the seek pointer is within the 11 direct pointers
            return direct[ptr];									//return back the direct pointer
        else {
            ptr -= 11;											//If the pointer isn't less than 11, it's an indirect
            short[] ptrs = getIndirectBlock();					//create a place for the pointers and call getIndirectBlock
            return ptrs[ptr];										//return the pointer
        }
    }
    private short[] getIndirectBlock() {							//Used to get the location within the indirect block
        byte[] data = new byte[Disk.blockSize];					//Create the buffer
        SysLib.rawread(indirect, data);							//Read the indirect data from the disk
        short[] ptrs = new short[Disk.blockSize/2];				//create an array of pointers
        for (int i = 0; i < data.length; i+=2) {					//Load the pointers
            ptrs[i/2] = SysLib.bytes2short(data, i);
        }
        return ptrs;
    }

}
