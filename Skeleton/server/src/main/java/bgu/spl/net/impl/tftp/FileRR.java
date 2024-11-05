package bgu.spl.net.impl.tftp;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;

/*
 * This class is a Module for reading & writing files.
 * meant for single thread use - Thread 1<-->1 FileRR
 */
public class FileRR {

    private final Path DIRECTORY_PATH;
    
    private byte[] fileToWrite;
    private int ftwNumOfBytes;
    private String ftwName;
    
    public FileRR() { 
        this.DIRECTORY_PATH = Paths.get("./Files");
        fileToWrite = new byte[10000000]; //10MB
        ftwNumOfBytes = 0;
        
    }
    
    // does file with fileName exist in the directory
    public boolean doesFileExist(String fileName) { 
        Path filePath = DIRECTORY_PATH.resolve(fileName);
        return Files.exists(filePath);
    }

    // read file as byte array
    public byte[] readFile(String fileName) { //V
        Path filePath = DIRECTORY_PATH.resolve(fileName);
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            System.out.println("FileRR: error reading file "+fileName+" - "+e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // read all files from directory as byte array, seperated by zero
    public byte[] readDirectory() { 
        LinkedList<byte[]> files = new LinkedList<byte[]>();
        int finalArrSize=0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(DIRECTORY_PATH)) {
            for (Path file : stream) {
                finalArrSize+=file.getFileName().toString().getBytes(StandardCharsets.UTF_8).length + 1; //+1 for the delimiting zero
                files.add(file.getFileName().toString().getBytes(StandardCharsets.UTF_8));
            }
            if(finalArrSize != 0)
                finalArrSize-=1; // -1 because no delimiter for last filename
            byte[] finalDataArr = new byte[finalArrSize]; 
            int finalDataArrIndex = 0;
            for(byte[] file : files){
                for(byte b: file){
                    finalDataArr[finalDataArrIndex] = b;
                    finalDataArrIndex+=1;
                }
                if(files.indexOf(file) != files.size()-1)
                {
                    finalDataArr[finalDataArrIndex] = 0;
                    finalDataArrIndex+=1;
                }
            }
            return finalDataArr;
        } 
        catch (IOException e) {
            System.out.println("FileRR: error reading directory - "+e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // set the name of the upcoming file to write
    public void setFtwName(String name){ 
        this.ftwName = name;
    }

    // recieve block of data to write eventually - saved to buffer
    // the block of data is in the form of a data array of a DATA packet
    // hence we skip the first 4 bytes
    public void recieveBlock(byte[] data , int len) { //V
        for (int i = 0; i < len ; i++) {
            fileToWrite[ftwNumOfBytes] = data[i+4];
            ftwNumOfBytes+=1;
        }
    }

    // write the ftw buffer to a file with a name matching this.ftwName
    // gets called after the protocol recieves the last data packet
    public String writeFile() throws IOException{ 
            byte[] data = new byte[ftwNumOfBytes];
            for(int i = 0 ; i < ftwNumOfBytes ; i++){
                data[i] = fileToWrite[i];
            }
            Path filePath = DIRECTORY_PATH.resolve(this.ftwName);
            Files.write(filePath, data);
            this.cleanData();
            return this.ftwName;
    }

    public void deleteFile(String fileName) throws IOException{ 
        Path filePath = DIRECTORY_PATH.resolve(fileName);
        Files.delete(filePath);
    }

    // simply allows overriding older data
    public void cleanData(){ 
        this.ftwNumOfBytes = 0;
        this.ftwName = "";
    }


    
}

