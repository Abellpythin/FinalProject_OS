package filesystem;



import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.testng.Assert.*;


import java.io.IOException;





public class FileSystemTest {

    private FileSystem fileSystem;

    @BeforeMethod
    public void setUp() throws IOException {
        fileSystem = new FileSystem();
    }




    @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = "FileSystem::write: Insufficient space")
    public void testWriteInsufficientSpace() throws IOException {
        // Arrange
        String fileName = "testFile.txt";
        int fileDescriptor = fileSystem.create(fileName);
        String data = new String(new char[Disk.BLOCK_SIZE * Disk.NUM_BLOCKS + 1]).replace('\0', 'X'); // Excessive data to fill disk

        // Act & Assert
        // Try writing data larger than available disk space
        fileSystem.write(fileDescriptor, data);
    }

    @Test
    public void testWriteMultipleBlocks() throws IOException {
        // Arrange
        String fileName = "multiBlockFile.txt";
        int fileDescriptor = fileSystem.create(fileName);
        String data = new String(new char[Disk.BLOCK_SIZE * 2]).replace('\0', 'A'); // Data requiring 2 blocks

        // Act
        fileSystem.write(fileDescriptor, data);

        // Assert
        INode inode = fileSystem.diskDevice.readInode(fileDescriptor);
        assertEquals(inode.getSize(), data.length(), "File size should match the data size");
        // Ensure blocks have been allocated
        for (int i = 0; i < INode.NUM_BLOCK_POINTERS; i++) {
            if (inode.getBlockPointer(i) != -1) {
                byte[] blockData = fileSystem.diskDevice.readDataBlock(inode.getBlockPointer(i));
                assertNotNull(blockData, "Block data should not be null");
            }
        }
    }


    @Test
    public void testWriteEmptyData() throws IOException {
        // Arrange
        String fileName = "emptyDataFile.txt";
        int fileDescriptor = fileSystem.create(fileName);
        String data = ""; // Empty data

        // Act
        fileSystem.write(fileDescriptor, data);

        // Assert
        INode inode = fileSystem.diskDevice.readInode(fileDescriptor);
        assertEquals(inode.getSize(), 0, "File size should be 0 for empty data");
    }
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

    @Test
    void read() {
        try {

            FileSystem fs = new FileSystem();
            String fileName = "testFile.txt";
            int fileDescriptor = fs.create(fileName);
            String dataToWrite = "This is test data for the file system.";
            fs.write(fileDescriptor, dataToWrite);
            String dataRead = fs.read(fileDescriptor);
            assertEquals(dataToWrite, dataRead);

        } catch (IOException ioe) {
            ioe.printStackTrace();
            fail("IOException occurred: " + ioe.getMessage());
        }
    }
    @Test
    public void testDeallocateSingleBlock() throws IOException {
        // Arrange
        String fileName = "singleBlockFile.txt";
        int fileDescriptor = fileSystem.create(fileName);
        String data = new String(new char[Disk.BLOCK_SIZE]).replace('\0', 'A'); // Data for 1 block
        fileSystem.write(fileDescriptor, data);

        // Act
        fileSystem.deallocateBlocksForFile(fileDescriptor);

        // Assert
        INode inode = fileSystem.diskDevice.readInode(fileDescriptor);
        for (int i = 0; i < INode.NUM_BLOCK_POINTERS; i++) {
            assertEquals(inode.getBlockPointer(i), -1, "Block pointer should be -1 after deallocation");
        }
    }

    @Test
    public void testDeallocateMultipleBlocks() throws IOException {
        // Arrange
        String fileName = "multiBlockFile.txt";
        int fileDescriptor = fileSystem.create(fileName);
        String data = new String(new char[Disk.BLOCK_SIZE * 3]).replace('\0', 'B'); // Data for 3 blocks
        fileSystem.write(fileDescriptor, data);

        // Act
        fileSystem.deallocateBlocksForFile(fileDescriptor);

        // Assert
        INode inode = fileSystem.diskDevice.readInode(fileDescriptor);
        for (int i = 0; i < INode.NUM_BLOCK_POINTERS; i++) {
            assertEquals(inode.getBlockPointer(i), -1, "Block pointer should be -1 after deallocation");
        }
    }

    @Test
    public void testDeallocateEmptyFile() throws IOException {
        // Arrange
        String fileName = "emptyFile.txt";
        int fileDescriptor = fileSystem.create(fileName);

        // Act
        fileSystem.deallocateBlocksForFile(fileDescriptor);

        // Assert
        INode inode = fileSystem.diskDevice.readInode(fileDescriptor);
        for (int i = 0; i < INode.NUM_BLOCK_POINTERS; i++) {
            assertEquals(inode.getBlockPointer(i), -1, "Block pointer should be -1 for an empty file");
        }
    }


    @Test
    public void testDeallocateMixedFiles() throws IOException {
        // Arrange
        String fileName1 = "file1.txt";
        String fileName2 = "file2.txt";
        int fd1 = fileSystem.create(fileName1);
        int fd2 = fileSystem.create(fileName2);
        String data1 = new String(new char[Disk.BLOCK_SIZE]).replace('\0', 'X'); // Data for 1 block
        String data2 = new String(new char[Disk.BLOCK_SIZE * 2]).replace('\0', 'Y'); // Data for 2 blocks
        fileSystem.write(fd1, data1);
        fileSystem.write(fd2, data2);

        // Act
        fileSystem.deallocateBlocksForFile(fd1);
        fileSystem.deallocateBlocksForFile(fd2);

        // Assert
        INode inode1 = fileSystem.diskDevice.readInode(fd1);
        INode inode2 = fileSystem.diskDevice.readInode(fd2);
        for (int i = 0; i < INode.NUM_BLOCK_POINTERS; i++) {
            assertEquals(inode1.getBlockPointer(i), -1, "Block pointer for file1 should be -1 after deallocation");
            assertEquals(inode2.getBlockPointer(i), -1, "Block pointer for file2 should be -1 after deallocation");
        }
    }

}
