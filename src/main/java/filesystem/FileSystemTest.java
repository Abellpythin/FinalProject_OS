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

        @Test
        public void testCreateFile() throws IOException {
            // Arrange
            String fileName = "testFile.txt";

            // Act
            int fileDescriptor = fileSystem.create(fileName);

            // Assert
            assertNotEquals(fileDescriptor, -1, "File creation failed: file descriptor is -1");
        }

        @Test
        public void testWriteValidFile() throws IOException {
            // Arrange
            String fileName = "testFile.txt";
            int fileDescriptor = fileSystem.create(fileName);
            String data = "Hello, world!";

            // Act
            fileSystem.write(fileDescriptor, data);

            // Assert
            INode inode = fileSystem.diskDevice.readInode(fileDescriptor);
            assertEquals(inode.getSize(), data.length(), "File size should match the data size");
        }

        @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = "FileSystem::write: Invalid file descriptor")
        public void testWriteInvalidFileDescriptor() throws IOException {
            // Arrange
            String fileName = "testFile.txt";
            int fileDescriptor = fileSystem.create(fileName); // Create the file first
            String data = "Hello, world!";

            // Act & Assert
            // Try writing with an invalid file descriptor (not the one returned by create())
            fileSystem.write(fileDescriptor + 1, data); // This should trigger the exception
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

        @Test(expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = "FileSystem::write: Invalid file descriptor")
        public void testWriteClosedFile() throws IOException {
            // Arrange
            String fileName = "testFile.txt";
            int fileDescriptor = fileSystem.create(fileName);
            String data = "Hello, world!";
            fileSystem.close(fileDescriptor);

            // Act & Assert
            // Try writing to a closed file
            fileSystem.write(fileDescriptor, data);
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
        public void testWrite() {
            try {
                String fileName = "testWriteFile.txt";
                String content = "Writing to the file system.";

                int fd = fileSystem.create(fileName);
                fileSystem.write(fd, content);
                fileSystem.close(fd);

                fd = fileSystem.open(fileName);
                String readContent = fileSystem.read(fd);
                fileSystem.close(fd);

                assertEquals(content, readContent);
            } catch (IOException e) {
                fail("IOException occurred: " + e.getMessage());
            }
        }
    }