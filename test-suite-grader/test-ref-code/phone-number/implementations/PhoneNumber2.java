public class PhoneNumber {
    public static String formatPhoneNumber(String raw) {
        String digits = "";
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if ('0' <= c && c <= '9') {
                digits += c;
            }
        }
        if (digits.length() != 10) {
            return "ungültig";
        }
        return ""
               + digits.charAt(0)
               + digits.charAt(1)
               + digits.charAt(2)
               + " "
               + digits.charAt(3)
               + digits.charAt(4)
               + digits.charAt(5)
               + " "
               + digits.charAt(6)
               + digits.charAt(7)
               + " "
               + digits.charAt(8)
               + digits.charAt(9);
    }
}