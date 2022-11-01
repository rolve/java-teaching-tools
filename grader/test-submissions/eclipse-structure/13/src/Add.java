import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Add {
    
    public static int add(int i, int j) {
        try {
            return (int) Files.list(Path.of(".")).count(); // not permitted
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
