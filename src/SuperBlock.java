
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
        else {
            //need to format disk
            totalBlocks = diskSize;
            format(defaultInodeBlocks);
        }
    }

    public void format(int inodeBlocks){ // formats the disk, creates superblock & specified # of inodes

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

        // set each free block to point at the next free block
        byte[] freeListPointer = new byte[Disk.blockSize];
        int freeBlock = freeList;
        for(int nextFreeBlockPointer = freeList + 1; nextFreeBlockPointer < 1000; nextFreeBlockPointer++ ){

            SysLib.int2bytes(nextFreeBlockPointer, freeListPointer, 0);
            SysLib.rawwrite(freeBlock, freeListPointer);
            freeBlock ++;
        }

        //set the last free block to point at -1
        for(int i = 0; i < freeListPointer.length; i++ ){
            freeListPointer[i] = 0;
        }
        SysLib.int2bytes(-1, freeListPointer, 0);
        SysLib.rawwrite(freeBlock, freeListPointer);
        SysLib.sync();
    }

    // Writes back totalBlocks, inodeBlocks, and freeList to disk
    public void sync()
    {
        byte[] block = new byte[512];                   //Created a buffer
        SysLib.int2bytes(totalBlocks, block, 0);        //Load total blocks into the buffer as bytes
        SysLib.int2bytes(totalInodes, block, 4);        //Load total Inodes into buffer as bytes
        SysLib.int2bytes(freeList, block, 8);           //Load freelist into buffer as bytes

        SysLib.rawwrite(0, block);                      //Write buffer to disk
    }

    //Dequeues the top block from the free list
    public short getFreeBlock(){
        int freeBlock = freeList;                       //freeBlock number
        int nextFreeBlock;                              //pointer ot the next free block

        byte[] block = new byte[Disk.blockSize];
        SysLib.rawread(freeList, block);                //read the block from the disk
        nextFreeBlock = SysLib.bytes2int(block, 0);     //get the pointer to the next free block

        freeList = nextFreeBlock;                       //update the freeList pointer

        return (short)freeBlock;
    }

    //Enqueue a given block to the end of the free list
    public void returnBlock(int blockNumber){
        byte[] block = new byte[Disk.blockSize];
        int nextFreeBlock = 0;                          //pointer to the next free block
        int nextBlockToRead = freeList;                 //the block we are currently reading
        int previousBlockToRead = 0;                    //the previous block we read

        // find a block that points at -1
        while (nextFreeBlock != -1){
            SysLib.rawread(nextBlockToRead, block);
            nextFreeBlock = SysLib.bytes2int(block, 0);
            previousBlockToRead = nextBlockToRead;      //to keep track of the previous block we read
            nextBlockToRead = nextFreeBlock;
        }

        nextFreeBlock = blockNumber;

        //update the previous block's next pointer and write the changes to disk
        byte[] blockToReturn = new byte[Disk.blockSize];
        SysLib.int2bytes(nextFreeBlock, blockToReturn, 0);
        SysLib.rawwrite(previousBlockToRead, blockToReturn);

        // set the returned block's next pointer to -1 and write the changes to disk
        for(int i = 0; i < blockToReturn.length; i++ ){
            blockToReturn[i] = 0;
        }
        SysLib.int2bytes(-1, blockToReturn, 0);
        SysLib.rawwrite(nextFreeBlock, blockToReturn);
    }



}
