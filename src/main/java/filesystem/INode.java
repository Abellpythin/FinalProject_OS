package filesystem;


public class INode {
    public final static int NUM_BLOCK_POINTERS = 32;
    public final static int FILE_NAME_SIZE = 64;
    public final static int SIZE_FIELD_SIZE = Integer.SIZE;
    public final static int BLOCK_POINTER_SIZE = Integer.SIZE;

    /**
     * File name size + size(integer) + size(integer  * number_of_block_addresses
     */
    public final static int INODE_SIZE = FILE_NAME_SIZE +
            Integer.SIZE +
            (Integer.SIZE * NUM_BLOCK_POINTERS);

    private String fileName;
    private int fileSize;
    private int[] blockPointers;


    public INode() {
        fileName = null;
        fileSize = -1;
        blockPointers = new int[NUM_BLOCK_POINTERS];
        for (int i = 0; i < blockPointers.length; i++) {
            blockPointers[i] = -1;
        }
    }

    /**
     * Sets the size of the file in bytes
     *
     * @param size Size of the file in bytes
     */
    public void setSize(int size) {
        this.fileSize = size;
    }

    /**
     * Returns the size of the file in bytes
     * @return Returns the size of the file in bytes
     */
    public int getSize(){
        return this.fileSize;
    }

    /**
     * Returns the size of the file in bytes as a four byte array
     * @return Returns the size of the file in bytes as a four byte array
     */
    public byte[] getSizeBytes() {
        return new byte[]{
                (byte) (this.fileSize >> 24),
                (byte) (this.fileSize >> 16),
                (byte) (this.fileSize >> 8),
                (byte) this.fileSize
        };
    }

    /**
     * Sets the name of the file for this INode
     * @param name Name of the file
     * @throws IllegalArgumentException If the length of the name exceeds the
     *                                  allowed length of 64 bytes
     */
    public void setFileName(String name) throws IllegalArgumentException {
        if (name != null) {
            if (name.length() > INode.FILE_NAME_SIZE) {
                throw new IllegalArgumentException("INode::setFileName:  " +
                        "size exceeds " + INode.FILE_NAME_SIZE + " bytes");
            }
        }
        this.fileName = name;
    }

    /**
     * Returns the name of the file name as a string
     * @return Returns the name of the file name as a string
     */
    public String getFileName() {
        return this.fileName;
    }

    /**
     * Returns the name of the file name as a byte array
     * @return Returns the name of the file name as a byte array
     */
    public byte[] getFileNameBytes() {
        byte[] result = null;
        byte[] contents = null;

        result = new byte[FILE_NAME_SIZE];
        for (int i = 0; i < result.length; i++) {
            result[i] = 0;
        }

        if (fileName != null) {
            contents = fileName.getBytes();
            for (int i = 0; i < contents.length; i++) {
                result[i] = contents[i];
            }
        }

        return result;
    }

    /**
     * Sets the block pointer at position <code>whichOne</code> in the
     * block pointer list to <code>value</code>
     * @param whichOne Position in the block pointer list to set the value
     * @param value Value to be set
     * @throws IllegalArgumentException If <code>whichOne</code> exceeds the
     *                                  size of the block pointer list
     */
    public void setBlockPointer(int whichOne, int value)
            throws IllegalArgumentException {
        int result = -1;
        if (whichOne >= NUM_BLOCK_POINTERS) {
            throw new IllegalArgumentException("INode::setFileName:  " +
                    "block pointer greater than " +
                    NUM_BLOCK_POINTERS);
        }
        blockPointers[whichOne] = value;
    }

    /**
     * Returns the value stored in the block pointer list at position
     * <code>whichOne</code>
     * @param whichOne The position in the block pointer list
     * @return Returns the value stored in the block pointer list at position
     * @throws IllegalArgumentException If <code>whichOne</code> exceeds the last position
     *                                  in the block pointer list
     */
    public int getBlockPointer(int whichOne) throws IllegalArgumentException {
        if (whichOne >= NUM_BLOCK_POINTERS) {
            throw new IllegalArgumentException("INode::setFileName:  " +
                    "block pointer greater than " +
                    NUM_BLOCK_POINTERS);
        }

        return blockPointers[whichOne];
    }

    /**
     * Returns the value stored in the block pointer list at position as
     * an array of byte
     *  @param whichOne The position in the block pointer list
     *  @return Returns the value stored in the block pointer list at position
     *  @throws IllegalArgumentException If <code>whichOne</code> exceeds the last position
     *                                   in the block pointer list
     */
    public byte[] getBlockPointerBytes(int whichOne) throws IllegalArgumentException {
        if (whichOne >= NUM_BLOCK_POINTERS) {
            throw new IllegalArgumentException("INode::setFileName:  " +
                    "block pointer greater than " +
                    NUM_BLOCK_POINTERS);
        }

        int result = getBlockPointer(whichOne);

        return new byte[]{
                (byte) (result >> 24),
                (byte) (result >> 16),
                (byte) (result >> 8),
                (byte) result
        };
    }
}


