public class PhoneNumber {
    public static String formatPhoneNumber(String raw) {
        String clean = raw;

        // remove leading
        while (clean.length() > 0 && !Character.isDigit(clean.charAt(0))) {
            clean = clean.substring(1);
        }

        // remove trailing
        while (clean.length() > 0 && !Character.isDigit(clean.charAt(clean.length() - 1))) {
            clean = clean.substring(0, clean.length() - 1);
        }

        // remove between
        for (int i = 1; i < clean.length() - 1; i++) {
            if (!Character.isDigit(clean.charAt(i))) {
                clean = clean.substring(0, i) + clean.substring(i + 1);
                i--;
            }
        }

        if (clean.length() < 10 || clean.length() > 10) {
            return "ungültig";
        }
        return clean.substring(0, 3) + ' ' + clean.substring(3, 6) + ' '
               + clean.substring(6, 8) + ' ' + clean.substring(8);
    }
}
