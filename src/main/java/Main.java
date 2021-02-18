import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        DoublonAnalyser doublonAnalyser = new DoublonAnalyser();
        try {
            doublonAnalyser.launch();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
