import com.itextpdf.text.exceptions.InvalidPdfException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;
import org.apache.commons.io.FileUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DoublonAnalyser {

    public static final String PATH_BUFFER_IN = "work_data_test";
    public static final String PATH_DOUBLONS = "doublons";
    public static final String PATH_CORPUS = "lexical_data/words";

    /**
     * Vocabulaire de référence
     * sur les stages.
     */
    ArrayList<String> wordsList;
    HashMap<String, HashMap<String, Integer>> fileScoresMap;
    HashMap<String, HashMap<String, Integer>> doublons;
    HashMap<String, HashMap<String, Integer>> cleanData;

    /**
     * Constructor.
     */
    public DoublonAnalyser() {
        wordsList = new ArrayList<>();
        fileScoresMap = new HashMap<>();
        doublons = new HashMap<>();
        cleanData = new HashMap<>();
    }

    /**
     * Launch the analyse.
     */
    public void launch() throws IOException {
        this.cleanDoublonsDirectory();
        this.getReferenceWords();
        this.browseFiles();
    }

    /**
     * Clear the directory that will contains the doubons.
     */
    private void cleanDoublonsDirectory() throws IOException {
        FileUtils.cleanDirectory(Paths.get(PATH_DOUBLONS).toFile());
    }

    /**
     * Get the list of words that will be the corpus.
     */
    private void getReferenceWords() {
        try {
            Charset charset = StandardCharsets.UTF_8;
            wordsList = (ArrayList<String>) Files.readAllLines(Paths.get(PATH_CORPUS), charset);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Dedoublonnage.
     */
    private void browseFiles() {
        Path dir = Paths.get(PATH_BUFFER_IN);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            int count = 0;
            for (Path aFile : stream) {
                System.out.println("Analyse fichier n°: " + count++);
                String fileString = this.parsePdf(aFile.toFile().toString());
                Map<String, Integer> map = fileString.lines()
                        .flatMap(line -> Stream.of(line.split("\\s+")))//TODO : changer le splittage
                        .map(String::toLowerCase)//TODO : pas de maj dans la wordList
                        .filter(wordsList::contains)
                        .collect(Collectors.toMap(word -> word, word -> 1, Integer::sum));

                Optional<Map.Entry<String, HashMap<String, Integer>>> optionalOriginal = fileScoresMap.entrySet().stream()
                        .filter(stringIntegerHashMap -> stringIntegerHashMap.getValue().equals(map))
                        .findFirst();

                if (optionalOriginal.isPresent()) {
                    doublons.put(aFile.getFileName().toString() + " doublon de : " + optionalOriginal.get().getKey(),
                            (HashMap<String, Integer>) map);
                    Files.copy(aFile, Paths.get(PATH_DOUBLONS + "/" + aFile.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(Path.of(PATH_BUFFER_IN + "/" + Path.of(optionalOriginal.get().getKey())),
                            Paths.get(PATH_DOUBLONS + "/" + Path.of(optionalOriginal.get().getKey())), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    cleanData.put(aFile.getFileName().toString(), (HashMap<String, Integer>) map);
                }
                fileScoresMap.put(aFile.getFileName().toString(), (HashMap<String, Integer>) map);
            }

            System.out.println("####### SCORES : #######");
            fileScoresMap.entrySet().forEach(entry -> {
                System.out.println(entry.getKey() + " " + entry.getValue());
            });
            System.out.println("####### FIN SCORES : #######");

            System.out.println("####### CLEAN DATA : #######");
            cleanData.entrySet().forEach(entry -> {
                System.out.println(entry.getKey() + " " + entry.getValue());
            });
            System.out.println("####### FIN CLEAN DATA : #######");

            System.out.println("####### DOUBLONS : #######");
            System.out.println("Taille liste " + PATH_DOUBLONS + " = " + doublons.size());
            doublons.entrySet().forEach(entry -> {
                System.out.println(entry.getKey() + " " + entry.getValue());
            });
            System.out.println("####### FIN DOUBLONS : #######");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parsing PDF's file to text.
     *
     * @param fileName PDF's file to parse.
     * @return String with file's content.
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
