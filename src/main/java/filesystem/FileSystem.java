package filesystem;

import java.io.IOException;


public class FileSystem {
    private Disk diskDevice;

    private int iNodeNumber;
    private int fileDescriptor;
    private INode iNodeForFile;

    public FileSystem() throws IOException {
        diskDevice = new Disk();
        diskDevice.format();
    }

    /***
     * Create a file with the name <code>fileName</code>
     *
     * @param fileName - name of the file to create
     * @throws IOException
     */
    public int create(String fileName) throws IOException {
        INode tmpINode = null;

        boolean isCreated = false;

        for (int i = 0; i < Disk.NUM_INODES && !isCreated; i++) {
            tmpINode = diskDevice.readInode(i);
            String name = tmpINode.getFileName();
            if (name.trim().equals(fileName)){
                throw new IOException("FileSystem::create: "+fileName+
                        " already exists");
            } else if (tmpINode.getFileName() == null) {
                this.iNodeForFile = new INode();
                this.iNodeForFile.setFileName(fileName);
                this.iNodeNumber = i;
                this.fileDescriptor = i;
                isCreated = true;
            }
        }
        if (!isCreated) {
            throw new IOException("FileSystem::create: Unable to create file");
        }

        return fileDescriptor;
    }

    /**
     * Removes the file
     *
     * @param fileName
     * @throws IOException
     */
    public void delete(String fileName) throws IOException {
        INode tmpINode = null;
        boolean isFound = false;
        int inodeNumForDeletion = -1;

        /**
         * Find the non-null named inode that matches,
         * If you find it, set its file name to null
         * to indicate it is unused
         */
        for (int i = 0; i < Disk.NUM_INODES && !isFound; i++) {
            tmpINode = diskDevice.readInode(i);

            String fName = tmpINode.getFileName();

            if (fName != null && fName.trim().compareTo(fileName.trim()) == 0) {
                isFound = true;
                inodeNumForDeletion = i;
                break;
            }
        }

        /***
         * If file found, go ahead and deallocate its
         * blocks and null out the filename.
         */
        if (isFound) {
            deallocateBlocksForFile(inodeNumForDeletion);
            tmpINode.setFileName(null);
            diskDevice.writeInode(tmpINode, inodeNumForDeletion);
            this.iNodeForFile = null;
            this.fileDescriptor = -1;
            this.iNodeNumber = -1;
        }
    }


    /***
     * Makes the file available for reading/writing
     *
     * @return
     * @throws IOException
     */
    public int open(String fileName) throws IOException {
        this.fileDescriptor = -1;
        this.iNodeNumber = -1;
        INode tmpINode = null;
        boolean isFound = false;
        int iNodeContainingName = -1;

        for (int i = 0; i < Disk.NUM_INODES && !isFound; i++) {
            tmpINode = diskDevice.readInode(i);
            String fName = tmpINode.getFileName();
            if (fName != null) {
                if (fName.trim().compareTo(fileName.trim()) == 0) {
                    isFound = true;
                    iNodeContainingName = i;
                    this.iNodeForFile = tmpINode;
                }
            }
        }

        if (isFound) {
            this.fileDescriptor = iNodeContainingName;
            this.iNodeNumber = fileDescriptor;
        }

        return this.fileDescriptor;
    }


    /***
     * Closes the file
     *
     * @throws IOException If disk is not accessible for writing
     */
    public void close(int fileDescriptor) throws IOException {
        if (fileDescriptor != this.iNodeNumber){
            throw new IOException("FileSystem::close: file descriptor, "+
                    fileDescriptor + " does not match file descriptor " +
                    "of open file");
        }
        diskDevice.writeInode(this.iNodeForFile, this.iNodeNumber);
        this.iNodeForFile = null;
        this.fileDescriptor = -1;
        this.iNodeNumber = -1;
    }


    /**
     * Add your Javadoc documentation for this method
     */
    public String read(int fileDescriptor) throws IOException {
        // TODO: Replace this line with your code
        return null;
    }


    /**
     * Add your Javadoc documentation for this method
     */
    public void write(int fileDescriptor, String data) throws IOException {

        // TODO: Replace this line with your code

    }


    /**
     * Add your Javadoc documentation for this method
     */
    public int[] allocateBlocksForFile(int iNodeNumber, int numBytes) throws IOException {
        // Calculate how many blocks are needed to store the file
        int numBlocksRequired = (numBytes + Disk.BLOCK_SIZE - 1) / Disk.BLOCK_SIZE; // Round up

        // Initialize an array to store the block pointers for the inode
        int[] allocatedBlocks = new int[numBlocksRequired];

        // Read the free block list from disk
        FreeBlockList freeBlockList = diskDevice.readFreeBlockList();

        // Counter for the number of blocks successfully allocated
        int allocatedCount = 0;

        // Iterate over the free block list to find available blocks
        for (int i = 0; i < Disk.NUM_BLOCKS && allocatedCount < numBlocksRequired; i++) {
            if (freeBlockList.isBlockFree(i)) {
                // Mark the block as allocated
                freeBlockList.allocateBlock(i);
                // Store the allocated block number in the array
                allocatedBlocks[allocatedCount] = i;
                allocatedCount++;
            }
        }

        // If we could not allocate enough blocks, throw an exception
        if (allocatedCount < numBlocksRequired) {
            throw new IOException("FileSystem::allocateBlocksForFile: Not enough free blocks available.");
        }

        // Read the inode to update it with the new block pointers
        INode inode = diskDevice.readInode(iNodeNumber);

        // Set the block pointers in the inode
        inode.setBlockPointers(allocatedBlocks);

        // Write the updated inode back to disk
        diskDevice.writeInode(inode, iNodeNumber);

        // Write the updated free block list back to disk
        diskDevice.writeFreeBlockList(freeBlockList.getFreeBlockList());

        return allocatedBlocks;
    }


    /**
     * Add your Javadoc documentation for this method
     */
    public void deallocateBlocksForFile(int iNodeNumber) {
        // TODO: replace this line with your code 
        try {
            // Retrieve the inode associated with the given inode number
            INode inode = diskDevice.readInode(iNodeNumber);
    
            // Iterate over each block pointer in the inode
            for (int i = 0; i < INode.NUM_BLOCK_POINTERS; i++) {
                int blockPointer = inode.getBlockPointer(i);
    
                // If the block pointer is valid (not equal to -1), deallocate the block
                if (blockPointer != -1) {
                    // Deallocate the block using the FreeBlockList
                    diskDevice.readFreeBlockList();
                    FreeBlockList freeBlockList = new FreeBlockList();
                    freeBlockList.deallocateBlock(blockPointer);
    
                    // Update the free block list on disk
                    diskDevice.writeFreeBlockList(freeBlockList.getFreeBlockList());
                }
            }
        } catch (IOException e) {
            System.err.println("Error deallocating blocks for inode number: " + iNodeNumber);
            e.printStackTrace();
        }           
    }

    // You may add any private method after this comment

}
