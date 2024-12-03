import filesystem.FileSystem;

import java.io.IOException;

public class Main {
    public static final int NUM_LINES = 100;
    public static final String testData = "This is some text ";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {
          FileSystem fs = new FileSystem();
          String fileNameBase = "file";
          String fileName = null;
          String theMessage = null;

          for (int i= 0; i < NUM_LINES; i++) {
             fileName = new String(fileNameBase + i + "." + "txt");
             int fd = fs.create(fileName);
             theMessage = new String();
             for (int j= 0; j < i+1; j++) {
               theMessage = theMessage.concat(testData + j + ".  ");
             }
             fs.write(fd, theMessage);
             fs.close(fd);
          }

          /**
           * delete every 2nd file
           */
          for (int i= 0 ; i < NUM_LINES; i+=2) {
             fileName = new String(fileNameBase + i + "." + "txt");
             fs.delete(fileName);
          }

          String message = null;
          for (int i= 1; i < NUM_LINES; i+=2) {
            fileName = new String(fileNameBase + i + "." + "txt");
            int fd = fs.open(fileName);
            message = fs.read(fd);
            System.out.println("******************");
            System.out.println("printing file:  " + fileName);
            System.out.println("------------------------------------");
            System.out.println(message);
            System.out.println("******************");
            fs.close(fd);
          }

        } catch (IOException e) {
           System.err.println(e.getMessage());
           e.printStackTrace();
        }
    }

}
