
public class Inode {
    private final static int iNodeSize = 32;                    // fix to 32 bytes
    private final static int directSize = 11;                   // # direct pointers

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

}
