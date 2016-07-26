/**
 * Created by Michael on 7/23/2015.
 */
class SuperBlock {
    //private final int defaultInodeBlocks = 64;
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
            //format(defaultInodeBlocks);
        }
    }

    //Other SuperBlock Methods from Additional Materials

    // Writes back totalBlocks, inodeBlocks, and freeList to disk
    // sync()


    //Dequeues the top block from the free list
    // getFreeBlock()

    //Enqueue a given block to the end of the fee list
    //returnBlock(int blockNumber)



}
