import java.util.Scanner;

public class PhoneNumber {
    public static String formatPhoneNumber(String raw) {
        String digits = "";
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '0' || c == '1' || c == '2' || c == '3' ||
                c == '4' || c == '5' || c == '6' || c == '7' ||
                c == '8' || c == '9') {
                digits += c;
            }
        }
        if (digits.length() != 10) {
            return "ungültig";
        }
        String result = "";
        for (int i = 0; i < 10; i++) {
            result += digits.charAt(i);
            if (i == 2 || i == 5 || i == 7) {
                result += " ";
            }
        }
        return result;
    }
}