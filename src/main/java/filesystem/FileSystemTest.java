package filesystem;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

class FileSystemTest {

    @Test
    void write() {
    }

    @Test
    void close() {
    }

    @Test
    void read() throws IOException {
        fileSystem.create("testFile");
        int fd = fileSystem.open("testFile");
        fileSystem.write(fd, "Hello RAID 0!!!");
        String content = fileSystem.read(fd);
        assertEquals("Hello RAID 0!!!", content, "Read content should match the written content");
    }

    @Test
    void testWrite() {
    }
}
// Updated
