package filesystem;

import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class FileSystemTest {

    @Test
    void write() {
    }

    @Test
    void close() {
    }

    @Test
    public void testRead() throws IOException {
        String fileName = "testFile.txt";
        String content = "This is a test content.";

        int fd = fileSystem.create(fileName);
        fileSystem.write(fd, content);
        fileSystem.close(fd);

        fd = fileSystem.open(fileName);
        String readContent = fileSystem.read(fd);
        fileSystem.close(fd);

        assertEquals(content, readContent);
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