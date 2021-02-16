import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.itextpdf.text.exceptions.InvalidPdfException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;

public class DoublonAnalyser {

    ArrayList<String> wordsList;
    Map<String, Integer[]> fileScoresMap;
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

    private Integer[] computeScore(String fileName) {
        Integer[] scoreVector = new Integer[wordsList.size()];
        String fileString = this.parsePdf(fileName);
        String[] arr = fileString.split(" ");
        for(String str : arr) {
            str.replaceAll(",|;|:","");
            if(wordsList.contains(str)) {
                int index = wordsList.indexOf(str);
                if(scoreVector[index] != null) {
                    scoreVector[index]++;
                }
                else {
                    scoreVector[index]=0;
                }

            }
        }
        return scoreVector;
    }

    private String parsePdf(String fileName) {
        StringBuilder textFromPdf = new StringBuilder();
        try {
            PdfReader reader = new PdfReader("work_data\\"+fileName);
            PdfReaderContentParser parser = new PdfReaderContentParser(reader);
            TextExtractionStrategy strategy;
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                strategy = parser.processContent(i, new SimpleTextExtractionStrategy());
                textFromPdf.append(" "+strategy.getResultantText());
            }
            reader.close();
        }
        catch (InvalidPdfException e) {
            e.printStackTrace();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return textFromPdf.toString();
    }
}
