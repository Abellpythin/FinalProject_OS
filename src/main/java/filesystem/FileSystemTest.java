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
    }


