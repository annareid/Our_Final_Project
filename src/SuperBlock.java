import java.util.ArrayList;

/**
 * Created by Michael on 7/23/2015.
 */
class SuperBlock {
    public static final int inodesPerBlock = 16;
    private final int defaultInodeBlocks = 64;
    public int totalBlocks; // the number of disk blocks
    public int totalInodes; // the number of inodes
    public int freeList;    // the block number of the free list's head

    public SuperBlock(int diskSize) {
        //read the superblock from disk
        byte[] superBlock = new byte[Disk.blockSize];
        SysLib.rawread(0, superBlock);
        totalBlocks = SysLib.bytes2int(superBlock, 0);
        totalInodes = SysLib.bytes2int(superBlock, 4);
        freeList = SysLib.bytes2int(superBlock, 8);

        if (totalBlocks == diskSize && totalInodes > 0 && freeList >= 2)
            //disk contents are valid
            return;
        else{
            //need to format disk
            totalBlocks = diskSize;
            format(defaultInodeBlocks);
        }
    }

    public void format(int inodeBlocks) // formats the disk, creates superblock & specified # of inodes
    {
        totalInodes = inodeBlocks * inodesPerBlock;
        freeList = 1 + inodeBlocks; // start at 1 because the superblock occupies block 0

        byte[] superBlock = new byte[Disk.blockSize];
        SysLib.int2bytes(totalBlocks, superBlock, 0);
        SysLib.int2bytes(totalInodes, superBlock, 4);
        SysLib.int2bytes(freeList, superBlock, 8);
        SysLib.rawwrite(0, superBlock);

        for (short i = 0; i < totalInodes ; ++i) {
            Inode currentInode = new Inode();
            currentInode.toDisk(i);
        }

        SysLib.sync();
    }

    //Other SuperBlock Methods from Additional Materials

    // Writes back totalBlocks, inodeBlocks, and freeList to disk
    // sync()
    void sync()
    {
        byte[] block = new byte[512];                    //Created a buffer
        SysLib.int2bytes(totalBlocks, block, 0);        //Load total blocks into the buffer as bytes
        SysLib.int2bytes(totalInodes, block, 4);        //Load total Inodes into buffer as bytes
        SysLib.int2bytes(freeList, block, 8);           //Load freelist into buffer as bytes

        SysLib.rawwrite(0, block);                      //Write buffer to disk

    }
    //Dequeues the top block from the free list
    // getFreeBlock()

    //Enqueue a given block to the end of the free list
    //returnBlock(int blockNumber)



}
