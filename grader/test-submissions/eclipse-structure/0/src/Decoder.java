import java.io.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Decoder {

    public static String decode(InputStream in) throws IOException {
        var reader = new BufferedReader(new InputStreamReader(in, UTF_8));
        return reader.readLine();
    }
}
