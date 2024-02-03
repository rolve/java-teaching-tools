import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PhoneNumberTest {
    @Test
    public void onlyDigits() {
        assertEquals("099 123 45 67", PhoneNumber.formatPhoneNumber("0991234567"));
        assertEquals("012 888 33 77", PhoneNumber.formatPhoneNumber("0128883377"));
        assertEquals("061 061 61 61", PhoneNumber.formatPhoneNumber("0610616161"));
    }

    @Test
    public void spaces() {
        assertEquals("099 123 45 67", PhoneNumber.formatPhoneNumber("0 99123 456 7"));
        assertEquals("099 123 45 67", PhoneNumber.formatPhoneNumber("0 99 1 23 456 7"));
        assertEquals("012 888 33 77", PhoneNumber.formatPhoneNumber("01288 83377"));
        assertEquals("012 888 33 77", PhoneNumber.formatPhoneNumber("0128 8 8 337 7"));
        assertEquals("061 061 61 61", PhoneNumber.formatPhoneNumber("06 10 61616 1"));
        assertEquals("061 061 61 61", PhoneNumber.formatPhoneNumber("0 6 1 0 6 1 6 1 6 1"));
    }

    @Test
    public void otherChars() {
        assertEquals("099 123 45 67", PhoneNumber.formatPhoneNumber("0/99123/456/7"));
        assertEquals("099 123 45 67", PhoneNumber.formatPhoneNumber("0-99-1-23-456-7"));
        assertEquals("012 888 33 77", PhoneNumber.formatPhoneNumber("01288.83377"));
        assertEquals("012 888 33 77", PhoneNumber.formatPhoneNumber("0128_8_8_337_7"));
        assertEquals("061 061 61 61", PhoneNumber.formatPhoneNumber("06-10 61616,1"));
        assertEquals("061 061 61 61", PhoneNumber.formatPhoneNumber("0-6 1|0|6|1|6(161)"));
    }

    @Test
    public void yetOtherChars() {
        assertEquals("099 123 45 67", PhoneNumber.formatPhoneNumber("0:99123:456:7"));
        assertEquals("099 123 45 67", PhoneNumber.formatPhoneNumber("0'99'1'23'456'7"));
        assertEquals("012 888 33 77", PhoneNumber.formatPhoneNumber("01288;83377"));
        assertEquals("012 888 33 77", PhoneNumber.formatPhoneNumber("0128+8+8+337+7"));
        assertEquals("061 061 61 61", PhoneNumber.formatPhoneNumber("06^1061616=1"));
        assertEquals("061 061 61 61", PhoneNumber.formatPhoneNumber("0\\61*0*6*1*6[161]"));
    }

    @Test
    public void leadingTrailingSpaces() {
        assertEquals("099 123 45 67", PhoneNumber.formatPhoneNumber(" 0991234567"));
        assertEquals("099 123 45 67", PhoneNumber.formatPhoneNumber(" 0 99 1 23 456 7"));
        assertEquals("012 888 33 77", PhoneNumber.formatPhoneNumber("01288 83377 "));
        assertEquals("012 888 33 77", PhoneNumber.formatPhoneNumber("0128 8 8 337 7 "));
        assertEquals("061 061 61 61", PhoneNumber.formatPhoneNumber(" 06 10 61616 1 "));
        assertEquals("061 061 61 61", PhoneNumber.formatPhoneNumber(" 0 6 1 0 6 1 6 1 6 1 "));
    }

    @Test
    public void multipleChars() {
        assertEquals("099 123 45 67", PhoneNumber.formatPhoneNumber("0  99123 456  7"));
        assertEquals("099 123 45 67", PhoneNumber.formatPhoneNumber("0   99 1 23 456 7"));
        assertEquals("012 888 33 77", PhoneNumber.formatPhoneNumber("01288--83377"));
        assertEquals("012 888 33 77", PhoneNumber.formatPhoneNumber("0128 (8 8 337    7)"));
        assertEquals("061 061 61 61", PhoneNumber.formatPhoneNumber("06  10-61616   1"));
        assertEquals("061 061 61 61", PhoneNumber.formatPhoneNumber("0_6_1 0  6_1_6      1/6    (1)"));
    }

    @Test
    public void multipleLeadingTrailingSpaces() {
        assertEquals("099 123 45 67", PhoneNumber.formatPhoneNumber("   0  99123 456  7"));
        assertEquals("099 123 45 67", PhoneNumber.formatPhoneNumber("  0   99 1 23 456 7"));
        assertEquals("012 888 33 77", PhoneNumber.formatPhoneNumber("01288  83377     "));
        assertEquals("012 888 33 77", PhoneNumber.formatPhoneNumber("0128   8 8 337    7   "));
        assertEquals("061 061 61 61", PhoneNumber.formatPhoneNumber("  06  10 61616   1      "));
        assertEquals("061 061 61 61", PhoneNumber.formatPhoneNumber("    0  6  1 0  6 1 6      1  6    1  "));
    }

    @Test
    public void invalidLength() {
        assertEquals("ungültig", PhoneNumber.formatPhoneNumber("0"));
        assertEquals("ungültig", PhoneNumber.formatPhoneNumber("999"));
        assertEquals("ungültig", PhoneNumber.formatPhoneNumber("1234"));
        assertEquals("ungültig", PhoneNumber.formatPhoneNumber("010021212"));
        assertEquals("ungültig", PhoneNumber.formatPhoneNumber("01002121212"));
        assertEquals("ungültig", PhoneNumber.formatPhoneNumber("110101001010"));
        assertEquals("ungültig", PhoneNumber.formatPhoneNumber("110101001010101010111000100110101010001"));
    }

    @Test
    public void noDigits() {
        assertEquals("ungültig", PhoneNumber.formatPhoneNumber(""));
        assertEquals("ungültig", PhoneNumber.formatPhoneNumber(" "));
        assertEquals("ungültig", PhoneNumber.formatPhoneNumber("             "));
        assertEquals("ungültig", PhoneNumber.formatPhoneNumber("-"));
        assertEquals("ungültig", PhoneNumber.formatPhoneNumber("( - ) /  -"));
    }
}