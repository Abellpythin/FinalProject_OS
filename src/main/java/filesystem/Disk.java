package filesystem;

import java.io.IOException;
import java.io.RandomAccessFile;


public class Disk {
  public static final  String RAW_DISK_NAME = "RawDevice.dsk";
  public static final int NUM_BLOCKS = 16384;
  public static final int NUM_INODES = 1024;
  public static final int BLOCK_SIZE = 512;
  public static final int BYTES_IN_FREE_SPACE_LIST = NUM_BLOCKS/8;


  public static final int INODE_SIZE = INode.FILE_NAME_SIZE +
                                       INode.SIZE_FIELD_SIZE +
                                       (INode.NUM_BLOCK_POINTERS * INode.BLOCK_POINTER_SIZE);

  private static String RAW_DISK_MODE = "rw";

  private String diskFileName;
  private RandomAccessFile rawDisk;

  public Disk() {
    this.diskFileName = RAW_DISK_NAME;
  }

  /***
   * Initialize a new disk
   *
   * @throws IOException If an I/O error occurs
   */
  public void format() throws IOException {
    rawDisk = new RandomAccessFile(diskFileName, RAW_DISK_MODE);
    byte[] freeListBytes = new byte[BYTES_IN_FREE_SPACE_LIST];

    INode emptyINode = new INode();

    byte[] emptyDiskBlock = new byte[BLOCK_SIZE];

    /**
     * write an empty free block list
     */
    rawDisk.write(freeListBytes);

    /**
     * write empty inodes
     */
    for(int i= 0; i < NUM_INODES; i++) {
      rawDisk.write(emptyINode.getFileNameBytes());
      rawDisk.write(emptyINode.getSizeBytes());

      for (int blkPtrIndex= 0; blkPtrIndex < INode.NUM_BLOCK_POINTERS; blkPtrIndex++) {
        byte[] blockPtr = emptyINode.getBlockPointerBytes(blkPtrIndex);
        rawDisk.write(blockPtr);
      }
    }

    /**
     * write empty disk data blocks
     */
    for (int blkCount= 0; blkCount < NUM_BLOCKS; blkCount++) {
      rawDisk.write(emptyDiskBlock);
    }
  }

  /***
   * Retrieve the free block list
   *
   * @return Returns an array of bytes representing the free block list
   * @throws IOException If the first byte cannot be read for any reason other than end of file, or if
   *                     the random access file has been closed, or if some other I/O error occurs
   */
  public byte[] readFreeBlockList() throws IOException {
    byte[] freeList = new byte[BYTES_IN_FREE_SPACE_LIST];

    rawDisk.seek((long)0);
    rawDisk.read(freeList);

    return freeList;
  }


  /***
   * Writes free space list to raw disk
   *
   * @param freeBlockList Array of bytes representing an updated free
   *                      byte list
   * @throws IOException If the length of the updated free byte list is not
   *                      the same as the free byte list on the disk.
   */
  public void writeFreeBlockList(byte[] freeBlockList) throws IOException {
    if (freeBlockList.length != BYTES_IN_FREE_SPACE_LIST) {
      throw new IllegalArgumentException("Disk::writeFreeBlockList:  " +
                                       "is "  +  freeBlockList.length +
                                       "bytes long  instead of "  +
                                       BYTES_IN_FREE_SPACE_LIST +
                                       " bytes long");
    }

    rawDisk.seek((long)0);
    rawDisk.write(freeBlockList);
  }


  /***
   * Write an <code>INode</code> instance to the appropriate position
   *
   * @param inode The Inode to be written to disk
   * @param whichInode  The position to which the inode is to be written
   * @throws IOException If an I/O error occurs
   */
  public void writeInode(INode inode, int whichInode) throws IOException {
      byte[] name = inode.getFileNameBytes();
      byte[] size = inode.getSizeBytes();
      byte[][] blockPointers = new byte[INode.NUM_BLOCK_POINTERS][];

      for (int i= 0; i < INode.NUM_BLOCK_POINTERS; i++) {
        blockPointers[i] = inode.getBlockPointerBytes(i);
      }

      int cursor = BYTES_IN_FREE_SPACE_LIST + (INODE_SIZE * whichInode);

      /**
       * Seek to correct position in the raw file
       */
      rawDisk.seek((long)cursor);

      rawDisk.write(name);
      rawDisk.write(size);
      for (int j= 0; j < INode.NUM_BLOCK_POINTERS; j++) {
        rawDisk.write(blockPointers[j]);
      }
  }

  /***
   * Read an <code>INode</code> instance from the appropriate position in the file system
   *
   * @param whichInode The <code>INode</code> position in the file system to be read
   * @return An instance of INode read from the disk.
   * @throws IOException If the first byte cannot be read for any reason other than end of file, or if
   *    *                the random access file has been closed, or if some other I/O error occurs
   */
  public INode readInode(int whichInode) throws IOException {
      INode inode = new INode();
      int cursor = BYTES_IN_FREE_SPACE_LIST + (INODE_SIZE * whichInode);

      rawDisk.seek((long)cursor);

      byte[] fileNameBytes = new byte[INode.FILE_NAME_SIZE];
      int nameReadLen = rawDisk.read(fileNameBytes);

      int fileSizeField = rawDisk.readInt();

      /**
       * read each block pointer
       */
      int[] blockPtrValues = new int[INode.NUM_BLOCK_POINTERS];

      for (int i= 0; i < INode.NUM_BLOCK_POINTERS; i++) {
        blockPtrValues[i]= rawDisk.readInt();
      }

      String fileName = new String(fileNameBytes);

      /**
       * Null string is all 0's but
       * the conversion makes it the empty string.
       */
      int nameSum = 0;
      for (int index = 0; index < fileNameBytes.length; index++) {
        nameSum += fileNameBytes[index];
      }

      if ( nameSum <= 0) {
         inode.setFileName(null);
      } else {
        inode.setFileName(fileName);
      }
      inode.setSize(fileSizeField);

      for (int blkPtrIndex= 0; blkPtrIndex < INode.NUM_BLOCK_POINTERS; blkPtrIndex++) {
        inode.setBlockPointer(blkPtrIndex, blockPtrValues[blkPtrIndex]);
      }
      
      return inode;
  }

  /***
   * Reads a block of data from appropriate location in raw file
   *
   * @param whichBlock The position of the data block to be read
   * @return A block of data at position <code>whichBlock</code>
   * @throws IOException If an I/O error occurs
   */
  public byte[] readDataBlock(int whichBlock) throws IOException {
    int cursor = BYTES_IN_FREE_SPACE_LIST + (INODE_SIZE * NUM_INODES) + (BLOCK_SIZE * whichBlock);
    byte[] blockData = new byte[BLOCK_SIZE];

    rawDisk.seek(cursor);
    rawDisk.read(blockData);

    return blockData;
  }


  /***
   * Writes a block of data to the appropriate location in raw file
   *
   * @param blockData Array of bytes to be written to <code>whichBlock</code>
   * @param whichBlock Block position in the file system
   * @throws IOException If an I/O error occurs
   * @throws IllegalArgumentException If the length of <code>blockData</code> is not equal
   *                                  to the length of a data block size
   */
  public void writeDataBlock(byte[] blockData, int whichBlock) throws IOException, IllegalArgumentException {
    int cursor = BYTES_IN_FREE_SPACE_LIST + (INODE_SIZE * NUM_INODES) + (BLOCK_SIZE *whichBlock);

    if (blockData.length != BLOCK_SIZE) {
       throw new IllegalArgumentException("Disk::writeDataBlock:  "  +
                     "storing block of size " + blockData.length  +
                     "when it should be of size "  + BLOCK_SIZE);
    }

    rawDisk.seek(cursor);
    rawDisk.write(blockData);
  }

  /***
   * Convenience method to convert array of four bytes to an integer value
   * @param fourbytes Array of byte of length 4 that is to be converted to
   *                  an integer.
   * @return
   */
  public static int convertByteArrayToInt(byte[] fourbytes) {
     int val = (fourbytes[0] << 24) | (fourbytes[1] <<16) |
               (fourbytes[1] << 8) | (fourbytes[3]);

     return val;
  }

}
