import org.apache.commons.math3.util.FastMath;

public class Add {
    
    public static int add(int i, int j) {
        return FastMath.abs(i + j) * Integer.signum(i + j);
    }
}
