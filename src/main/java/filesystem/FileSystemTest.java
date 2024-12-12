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

    @Test
    void testAllocateBlocksForFile_Success() throws IOException {
        // Test successful allocation of blocks.
        int iNodeNumber = 0; // Assume the inode number is valid and initialized.
        int numBytes = 1024; // Request allocation for 1 KB (2 blocks)

        // Allocate blocks for the file
        int[] allocatedBlocks = FileSystem.allocateBlocksForFile(iNodeNumber, numBytes);

        // Validate the allocated blocks
        assertNotNull(allocatedBlocks, "Allocated blocks should not be null.");
        assertEquals(allocatedBlocks.length, 2, "The number of allocated blocks should match the required blocks.");

        // Check that the blocks are indeed allocated (i.e., no free)
        FreeBlockList freeBlockList = FileSystem.getDisk().readFreeBlockList();
        for (int block : allocatedBlocks) {
            assertTrue(freeBlockList.isBlockFree(block), "Block should be allocated but found free.");
        }
    }

    @Test
    void testAllocateBlocksForFile_InsufficientBlocks() {
        // Test if IOException is thrown when not enough free blocks are available.
        int iNodeNumber = 0;
        int numBytes = Disk.NUM_BLOCKS * Disk.BLOCK_SIZE; // Request allocation for all blocks (unlikely to succeed)

        try {
            FileSystem.allocateBlocksForFile(iNodeNumber, numBytes);
            fail("Expected IOException when trying to allocate more blocks than available.");
        } catch (IOException e) {
            assertEquals(e.getMessage(), "FileSystem::allocateBlocksForFile: Not enough free blocks available.");
        }
    }

    @Test
    void testAllocateBlocksForFile_BoundaryConditions() throws IOException {
        // Test allocation with 1 byte (edge case: should allocate 1 block)
        int iNodeNumber = 0;
        int numBytes = 1; // Allocate for 1 byte, which should result in 1 block allocation

        int[] allocatedBlocks = FileSystem.allocateBlocksForFile(iNodeNumber, numBytes);
        assertNotNull(allocatedBlocks, "Allocated blocks should not be null.");
        assertEquals(allocatedBlocks.length, 1, "Should allocate exactly 1 block for 1 byte.");

        // Ensure that a block has been allocated and it's the first available one
        FreeBlockList freeBlockList = FileSystem.getDisk().readFreeBlockList();
        assertTrue(freeBlockList.isBlockFree(allocatedBlocks[0]), "Allocated block should be free.");

        // Test allocating for a large file that requires more than one block
        numBytes = Disk.BLOCK_SIZE * 10; // Allocate for 10 blocks worth of data
        allocatedBlocks = FileSystem.allocateBlocksForFile(iNodeNumber, numBytes);

        assertNotNull(allocatedBlocks, "Allocated blocks should not be null.");
        assertEquals(allocatedBlocks.length, 10, "Should allocate exactly 10 blocks for 10 blocks worth of data.");
    }

    @Test
    void testAllocateBlocksForFile_ZeroBytes() throws IOException {
        // Test allocating 0 bytes, should not allocate any blocks
        int iNodeNumber = 0;
        int numBytes = 0;

        int[] allocatedBlocks = FileSystem.allocateBlocksForFile(iNodeNumber, numBytes);

        assertNotNull(allocatedBlocks, "Allocated blocks should not be null.");
        assertEquals(allocatedBlocks.length, 0, "No blocks should be allocated for 0 bytes.");
    }
}


// Updated
