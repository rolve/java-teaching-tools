import java.io.*;

public class Decoder {

    public static String decode(InputStream in) throws IOException {
        var reader = new BufferedReader(new InputStreamReader(in));
        return reader.readLine();
    }
}
