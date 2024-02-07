public class PhoneNumber {
    public static String formatPhoneNumber(String raw) {
        String digits = "";
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isDigit(c)) {
                digits += c;
            }
        }
        if (digits.length() != 10) {
            return "ungültig";
        }
        return digits.substring(0, 3) + " " + digits.substring(3, 6) + " "
               + digits.substring(6, 8) + " " + digits.substring(8, 10);
    }
}
