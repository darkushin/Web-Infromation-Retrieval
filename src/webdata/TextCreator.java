package webdata;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TextCreator {
    public String dir;
    private BufferedReader br;
    private StringBuilder stringBuffer;


    public TextCreator(String inputFile) throws FileNotFoundException {
//        this.dir = dir;
        br = new BufferedReader(new FileReader(inputFile));
        stringBuffer = new StringBuilder();
        loadText();
//        createDir();
//        saveText(inputFile);
    }

    private void loadText(){
        String line;
        int lineNum = 0;
        int numAppear = 0;
        try {
            while((line = br.readLine()) != null) {
                lineNum++;
                if (line.contains("review/text")) {
                    line = line.toLowerCase(Locale.ROOT);
                    if (line.contains("labeled")){
                        String[] tokens = line.split("[^a-zA-Z0-9]");
//                        System.out.println(line);
                        for (String token: tokens){
                            if (token.equals("labeled")){
                                numAppear++;
                            }
                        }
                    }
                }
                if (lineNum % 10000000 == 0){
                    System.out.println("Read: " + lineNum + " lines");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Num Appearances = " + numAppear);
    }


//    private void saveText(String inputFile) {
//        DataLoader dataLoader = null;
//        DataParser dataParser = new DataParser();
//        try {
//            dataLoader = new DataLoader(inputFile);
//        } catch (IOException e) {
//            e.printStackTrace();
//            System.out.println("Error occurred while reading the reviews input file.");
//            System.exit(1);
//        }
//        for (String s : dataLoader) {
//            DataParser.Review review = dataParser.parseReview(s);
//        }
//    }

    private void createDir(){
        Path path = Path.of(this.dir);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        String inputFile = "/Users/darkushin/Downloads/Movies_&_TV.txt";
//        String inputFile = "./100.txt";
        TextCreator textCreator = new TextCreator(inputFile);
    }
}
