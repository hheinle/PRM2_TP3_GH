import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        MailAnalyzer mailAnalyzer = new MailAnalyzer();
        try {
            mailAnalyzer.launch();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
