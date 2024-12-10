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
        if (fileDescriptor != this.iNodeNumber || this.iNodeForFile == null) {
            throw new IOException("FileSystem::write: Invalid file descriptor");
        }

        int dataSize = data.length();
        int blocksNeeded = (int) Math.ceil((double) dataSize / Disk.BLOCK_SIZE);

        // Retrieve free block list
        FreeBlockList freeBlockList = new FreeBlockList();
        byte[] currentFreeList = diskDevice.readFreeBlockList();
        freeBlockList.setFreeBlockList(currentFreeList);

        // Check for sufficient space
        int availableBlocks = 0;
        for (int i = 0; i < Disk.NUM_BLOCKS; i++) {
            int blockNum = i / 8;
            int offset = i % 8;
            if ((currentFreeList[blockNum] & (1 << offset)) == 0) {
                availableBlocks++;
            }
        }
        if (blocksNeeded > availableBlocks) {
            throw new IOException("FileSystem::write: Insufficient space");
        }

        // Allocate blocks and write data
        int blockIndex = 0;
        for (int i = 0; i < Disk.NUM_BLOCKS && blockIndex < blocksNeeded; i++) {
            int blockNum = i / 8;
            int offset = i % 8;
            if ((currentFreeList[blockNum] & (1 << offset)) == 0) {
                // Allocate block
                freeBlockList.allocateBlock(i);

                // Write the block data
                int start = blockIndex * Disk.BLOCK_SIZE;
                int end = Math.min(dataSize, start + Disk.BLOCK_SIZE);
                byte[] blockData = data.substring(start, end).getBytes();
                diskDevice.writeDataBlock(blockData, i);

                // Update inode block pointers
                this.iNodeForFile.setBlockPointer(blockIndex, i);
                blockIndex++;
            }
        }

        // Update inode file size and write it to disk
        this.iNodeForFile.setSize(dataSize);
        diskDevice.writeInode(this.iNodeForFile, fileDescriptor);

        // Write updated free block list to disk
        diskDevice.writeFreeBlockList(freeBlockList.getFreeBlockList());
    }


    /**
     * Add your Javadoc documentation for this method
     */


        private int[] allocateBlocksForFile(int iNodeNumber, int numBytes)
            throws IOException {

            // TODO: replace this line with your code
            return null;
        }

    /**
     * Add your Javadoc documentation for this method
     */
    private void deallocateBlocksForFile (int iNodeNumber) {
            try {
                // Retrieve the INode for the file
                INode inode = diskDevice.readInode(iNodeNumber);

                // Iterate through block pointers in the INode
                for (int i = 0; i < INode.NUM_BLOCK_POINTERS; i++) {
                    int blockPointer = inode.getBlockPointer(i);
                    if (blockPointer != -1) { // Check if the block pointer is valid
                        // Deallocate the block
                        FreeBlockList freeBlockList = new FreeBlockList();
                        freeBlockList.setFreeBlockList(diskDevice.readFreeBlockList());
                        freeBlockList.deallocateBlock(blockPointer);
                        diskDevice.writeFreeBlockList(freeBlockList.getFreeBlockList());

                        // Reset the block pointer in the INode
                        inode.setBlockPointer(i, -1);
                    }
                }

                // Write the updated INode back to disk
                diskDevice.writeInode(inode, iNodeNumber);

            } catch (IOException e) {
                System.err.println("Error while deallocating blocks for INode " + iNodeNumber + ": " + e.getMessage());
                e.printStackTrace();
            }
    }

}

