import org.apache.commons.io.FileUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.jsoup.Jsoup;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Analyse de mails pour déterminer
 * ceux qui sont intéressants.
 */
public class MailAnalyzer {

    public static final String PATH_EML_A_TRIER = "work_data";
    public static final String PATH_CORPUS_STAGE = "lexical_data/words";
    public static final String PATH_CORPUS_SPAM = "lexical_data/spam_words";
    public static final String PATH_MAILS_OK = "ok";
    public static final String PATH_MAILS_KO = "ko";

    /**
     * Vocabulaire de référence
     * sur les stages.
     */
    ArrayList<String> wordsList;
    /**
     * Vocabulaire de référence
     * sur les mail spams.
     */
    ArrayList<String> spamWordsList;
    /**
     * Noms des fichiers .eml considérés
     * comme "normaux"
     */
    ArrayList<String> ok;
    /**
     * Noms des fichiers .eml considérés
     * comme "spams"
     */
    ArrayList<String> ko;

    /**
     * Constructeur.
     */
    public MailAnalyzer() {
        wordsList = new ArrayList<>();
        spamWordsList = new ArrayList<>();
        ok = new ArrayList<>();
        ko = new ArrayList<>();
    }

    /**
     * Lancement de l'analyse.
     */
    public void launch() throws IOException {
        this.cleanOkKoDirectory();
        this.getReferenceWords();
        this.analyseMails();
    }

    /**
     * Nettoyage des répertoires.
     */
    private void cleanOkKoDirectory() throws IOException {
        FileUtils.cleanDirectory(Paths.get(PATH_MAILS_OK).toFile());
        FileUtils.cleanDirectory(Paths.get(PATH_MAILS_KO).toFile());
    }

    /**
     * Récupération des corpus.
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
     * Redirection des mails vers les dossiers
     * "ok" ou "ko".
     */
    private void analyseMails() {
        Path dir = Paths.get(PATH_EML_A_TRIER);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            int count = 0;
            for (Path aFile : stream) {
                System.out.println("Analyse mail n°: " + count++);
                String fileString = this.parseEmail(aFile.toFile().toString());

                // calcul du score "stage"
                Map<String, Integer> mapScoreStage = fileString.lines()
                        .flatMap(line -> Stream.of(line.split("\\s+")))
                        .map(String::toLowerCase)
                        .filter(wordsList::contains)
                        .collect(Collectors.toMap(word -> word, word -> 1, Integer::sum));

                // calcul du score "spam"
                Map<String, Integer> mapScoreSpam = fileString.lines()
                        .flatMap(line -> Stream.of(line.split("\\s+")))
                        .map(String::toLowerCase)
                        .filter(spamWordsList::contains)
                        .collect(Collectors.toMap(word -> word, word -> 1, Integer::sum));
                System.out.println("### Score Stage ###");
                mapScoreStage.entrySet().forEach(entry -> {
                    System.out.println(entry.getKey() + " " + entry.getValue());
                });
                System.out.println("### Score Spam ###");
                mapScoreSpam.entrySet().forEach(entry -> {
                    System.out.println(entry.getKey() + " " + entry.getValue());
                });

                int nbStageWords = mapScoreStage.values().stream().reduce(0, Integer::sum);
                int nbSpamWords = mapScoreSpam.values().stream().reduce(0, Integer::sum);

                if (nbStageWords < 7 || nbSpamWords > 1) {
                    System.out.println("--> KO");
                    ko.add(aFile.getFileName().toString());
                    Files.copy(aFile, Paths.get(PATH_MAILS_KO + "/" + aFile.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    System.out.println("--> OK");
                    ok.add(aFile.getFileName().toString());
                    Files.copy(aFile, Paths.get(PATH_MAILS_OK + "/" + aFile.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
                }
            }

            System.out.println("####### RESULTATS : #######");
            System.out.println("### NB OK : ###");
            System.out.println(ok.size());
            System.out.println("### NB KO : ###");
            System.out.println(ko.size());
            System.out.println("####### FIN RESULTATS : #######");

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
            if (emailText != null) {
                emailText = Jsoup.parse(emailText).text();
            }
            emailText += parser.parse().getPlainContent();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return emailText;
    }
}
