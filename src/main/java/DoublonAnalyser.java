import com.itextpdf.text.exceptions.InvalidPdfException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DoublonAnalyser {

    /**
     * Vocabulaire de référence
     * sur les stages.
     */
    ArrayList<String> wordsList;
    HashMap<String, HashMap<String, Integer>> fileScoresMap;

    /**
     *
     */
    public DoublonAnalyser() {
        wordsList = new ArrayList<>();
        fileScoresMap = new HashMap<>();
    }

    /**
     *
     */
    public void launch() {
        this.getReferenceWords();
        this.browseFiles();
    }

    /**
     *
     */
    private void getReferenceWords() {
        try {
            Charset charset = StandardCharsets.UTF_8;
            wordsList = (ArrayList<String>) Files.readAllLines(Paths.get("lexical_data/words"), charset);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    private void browseFiles() {
        Path dir = Paths.get("work_data");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path aFile : stream) {
                System.out.println("========================================");
                String fileString = this.parsePdf(aFile.toFile().toString());
                Map<String, Integer> map = fileString.lines()
                        .flatMap(line -> Stream.of(line.split("\\s+")))//TODO : changer le splittage
                        .map(String::toLowerCase)//TODO : pas de maj dans la wordList
                        .filter(wordsList::contains)
                        .collect(Collectors.toMap(word -> word, word -> 1, Integer::sum));
                // TODO : verifier que la clef n'est pas deja dans la map
                fileScoresMap.put(aFile.getFileName().toString(), (HashMap<String, Integer>) map);
            }
            fileScoresMap.entrySet().forEach(entry -> {
                System.out.println(entry.getKey() + " " + entry.getValue());
            });
            //TODO : enelever les doublons de la map

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param fileName
     * @return
     */
    private String parsePdf(String fileName) {
        StringBuilder textFromPdf = new StringBuilder();
        try {
            PdfReader reader = new PdfReader(fileName);
            PdfReaderContentParser parser = new PdfReaderContentParser(reader);
            TextExtractionStrategy strategy;
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                strategy = parser.processContent(i, new SimpleTextExtractionStrategy());
                textFromPdf.append(" " + strategy.getResultantText());
            }
            reader.close();
        } catch (InvalidPdfException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return textFromPdf.toString();
    }
}
