import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;

public class DoublonAnalyser {

    ArrayList<String> wordsList;
    Map<String, ArrayList<Integer>> fileScoresMap;
    public DoublonAnalyser() {
        wordsList = new ArrayList<>();
        fileScoresMap = new HashMap<>();
    }

    public void launch() {
        this.getWords();
        this.browseFiles();
    }

    private void getWords() {
        File file = new File("lexical_data/words");
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String str;
            while ((str = br.readLine()) != null) {
                wordsList.add(str);
            }
    }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void browseFiles() {
        String[] pathNames;
        File file = new File("work_data");
        pathNames = file.list();
        for(String fileName: pathNames) {
            fileScoresMap.put(fileName, this.computeScore(fileName));
        }

    }

    private ArrayList<Integer> computeScore(String fileName) {
        try {
            System.out.println(fileName);
            PdfReader reader = new PdfReader(fileName);
            PdfReaderContentParser parser = new PdfReaderContentParser(reader);
            StringBuilder textFromPdf = new StringBuilder();
            TextExtractionStrategy strategy;
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                strategy = parser.processContent(i, new SimpleTextExtractionStrategy());
                textFromPdf.append(strategy.getResultantText());
            }
            reader.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
