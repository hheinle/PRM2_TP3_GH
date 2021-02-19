import com.itextpdf.text.exceptions.InvalidPdfException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.jsoup.Jsoup;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.mail.*;
import javax.mail.internet.MimeMessage;

public class MailAnalyzer {

    public static final String PATH_EML_A_TRIER = "work_data";
    public static final String PATH_MAILS_OK = "ok";
    public static final String PATH_MAILS_KO = "ko";
    public static final String PATH_CORPUS_STAGE = "lexical_data/words";
    public static final String PATH_CORPUS_SPAM = "lexical_data/spam_words";

    /**
     * Vocabulaire de référence
     * sur les stages.
     */
    ArrayList<String> wordsList;
    ArrayList<String> spamWordsList;
    HashMap<String, HashMap<String, Integer>> fileScoresMap;
    HashMap<String, HashMap<String, Integer>> ok;
    HashMap<String, HashMap<String, Integer>> ko;
    HashMap<String, HashMap<String, Integer>> cleanData;

    /**
     * Constructor.
     */
    public MailAnalyzer() {
        wordsList = new ArrayList<>();
        fileScoresMap = new HashMap<>();
        ok = new HashMap<>();
        cleanData = new HashMap<>();
    }

    /**
     * Launch the analyse.
     */
    public void launch() throws IOException {
        this.cleanOkKoDirectory();
        this.getReferenceWords();
        this.browseFiles();
    }

    /**
     * Clear the directory that will contains the doubons.
     */
    private void cleanOkKoDirectory() throws IOException {
        FileUtils.cleanDirectory(Paths.get(PATH_MAILS_OK).toFile());
        FileUtils.cleanDirectory(Paths.get(PATH_MAILS_KO).toFile());
    }

    /**
     * Get the list of words that will be the corpus.
     */
    private void getReferenceWords() {
        try {
            Charset charset = StandardCharsets.UTF_8;
            wordsList = (ArrayList<String>) Files.readAllLines(Paths.get(PATH_CORPUS_STAGE), charset);
            spamWordsList = (ArrayList<String>) Files.readAllLines(Paths.get(PATH_CORPUS_SPAM), charset);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Dedoublonnage.
     */
    private void browseFiles() {
        Path dir = Paths.get(PATH_EML_A_TRIER);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            int count = 0;
            for (Path aFile : stream) {
                System.out.println("Analyse fichier n°: " + count++);
                String fileString = this.parseEmail(aFile.toFile().toString());
                Map<String, Integer> map = fileString.lines()
                        .flatMap(line -> Stream.of(line.split("\\s+")))
                        .map(String::toLowerCase)
                        .filter(wordsList::contains)
                        .collect(Collectors.toMap(word -> word, word -> 1, Integer::sum));

                Optional<Map.Entry<String, HashMap<String, Integer>>> optionalOriginal = fileScoresMap.entrySet().stream()
                        .filter(stringIntegerHashMap -> (!map.isEmpty()) && (aFile.toFile().length() != 0))
                        .filter(stringIntegerHashMap -> stringIntegerHashMap.getValue().equals(map))
                        .findFirst();

                if (optionalOriginal.isPresent()) {
                    // putting the second file
                    ok.put(aFile.getFileName().toString(), (HashMap<String, Integer>) map);
                    // putting the "original" file
                    ok.put(optionalOriginal.get().getKey(), optionalOriginal.get().getValue());
//                    Files.copy(aFile, Paths.get(PATH_DOUBLONS + "/" + aFile.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
//                    Files.copy(Path.of(PATH_EML_A_TRIER + "/" + Path.of(optionalOriginal.get().getKey())),
//                            Paths.get(PATH_DOUBLONS + "/" + Path.of(optionalOriginal.get().getKey())), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    cleanData.put(aFile.getFileName().toString(), (HashMap<String, Integer>) map);
                }
                fileScoresMap.put(aFile.getFileName().toString(), (HashMap<String, Integer>) map);
            }

//            System.out.println("####### SCORES : #######");
//            fileScoresMap.entrySet().forEach(entry -> {
//                System.out.println(entry.getKey() + " " + entry.getValue());
//            });
//            System.out.println("####### FIN SCORES : #######");
//
//            System.out.println("####### CLEAN DATA : #######");
//            cleanData.entrySet().forEach(entry -> {
//                System.out.println(entry.getKey() + " " + entry.getValue());
//            });
//            System.out.println("####### FIN CLEAN DATA : #######");

            System.out.println("####### DOUBLONS : #######");
//            System.out.println("Taille liste " + PATH_DOUBLONS + " = " + doublons.size());
            ok.entrySet().forEach(entry -> {
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
    private String parseEmail(String fileName) {
        Properties props = System.getProperties();
        props.put("mail.host", "smtp.dummydomain.com");
        props.put("mail.transport.protocol", "smtp");

        String emailText = "";
        try {
            Session mailSession = Session.getDefaultInstance(props, null);
            InputStream source = new FileInputStream(fileName);
            MimeMessage message = new MimeMessage(mailSession, source);
            MimeMessageParser parser = new MimeMessageParser(message);
            emailText = parser.parse().getHtmlContent();
            if(emailText != null) {
                emailText = Jsoup.parse(emailText).text();
            }
            emailText+=parser.parse().getPlainContent();


        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (MessagingException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return emailText;
    }
}
