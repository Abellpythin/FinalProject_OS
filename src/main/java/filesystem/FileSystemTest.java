package filesystem;

import org.testng.annotations.Test;
import java.io.IOException;
import static org.testng.Assert.*;

public class FileSystemTest {

    @Test
    void write() {
    }

    @Test
    void close() {
    }

    @Test
    void read() {
    }

    @Test
    void testWrite() {
    }
    // Setup method to initialize file system before each test.


    // Updated
    @Test
    public void testAllocateBlocksForFile_Success() throws IOException {
        // Set up FileSystem and Disk
        FileSystem fs = new FileSystem();
        Disk disk = new Disk();
        disk.format();  // Make sure the disk is formatted and all blocks are free

        // Set up a file size that requires a few blocks (1 KB)
        int fileSize = 1024;  // 1 KB (just 2 blocks, assuming each block is 512 bytes)
        int iNodeNumber = 1;

        // Simulate allocating blocks through the FileSystem, not Disk directly
        int[] allocatedBlocks = fs.allocateBlocksForFile(iNodeNumber, fileSize);  // This should be called on the FileSystem instance

        // Assert that the correct number of blocks were allocated
        assertEquals(2, allocatedBlocks.length);

        // Ensure that the inode has the correct block pointers
        INode inode = disk.readInode(iNodeNumber);
        assertNotNull(inode);
        assertEquals(allocatedBlocks[0], inode.getBlockPointer(0));
        assertEquals(allocatedBlocks[1], inode.getBlockPointer(1));

        // Ensure that the free block list was updated correctly
        byte[] freeBlockList = disk.readFreeBlockList();
        for (int block : allocatedBlocks) {
            assertTrue((freeBlockList[block / 8] & (1 << (block % 8))) != 0);
        }
    }
    @Test
    public void testAllocateBlocksForFile_InsufficientBlocks() throws IOException {
        // Set up FileSystem and Disk
        FileSystem fs = new FileSystem();
        Disk disk = new Disk();
        disk.format();  // Format the disk to clear all blocks

        // Manually occupy all but one block
        byte[] freeBlockList = disk.readFreeBlockList();
        for (int i = 1; i < freeBlockList.length * 8; i++) { // Leave the first block free
            freeBlockList[i / 8] |= (1 << (i % 8));
        }
        disk.writeFreeBlockList(freeBlockList);

        // Try to allocate blocks for a file requiring 2 blocks
        int fileSize = 1024;  // 1 KB (2 blocks required)
        int iNodeNumber = 1;

        // Assert that an IOException is thrown due to insufficient free blocks
        assertThrows(IOException.class, () -> fs.allocateBlocksForFile(iNodeNumber, fileSize));
    }
}