import org.junit.jupiter.api.Test;

import static java.lang.reflect.Modifier.isPrivate;
import static org.junit.jupiter.api.Assertions.*;

public class AdresseTest {

    @Test
    public void testKonstruktor() {
        Adresse adresse = new Adresse("Bahnhofstrasse", 6, 5210, "Windisch");
        assertNotNull(adresse);
    }

    @Test
    public void testToString() {
        Adresse adresse = new Adresse("Bahnhofstrasse", 6, 5210, "Windisch");
        assertEquals("Bahnhofstrasse 6\n5210 Windisch", adresse.toString());
        adresse = new Adresse("Birsweg", 120, 4000, "Basel");
        assertEquals("Birsweg 120\n4000 Basel", adresse.toString());
    }

    @Test
    public void testKonstruktorUndGetter() {
        Adresse adresse = new Adresse("Bahnhofstrasse", 6, 5210, "Windisch");
        assertEquals("Bahnhofstrasse", adresse.getStrasse());
        assertEquals(6, adresse.getNummer());
        assertEquals(5210, adresse.getPlz());
        assertEquals("Windisch", adresse.getOrt());

        adresse = new Adresse("Traugott-Meyer-Strasse", 13, 4147, "Aesch");
        assertEquals("Traugott-Meyer-Strasse", adresse.getStrasse());
        assertEquals(13, adresse.getNummer());
        assertEquals(4147, adresse.getPlz());
        assertEquals("Aesch", adresse.getOrt());
    }

    @Test
    public void testAlleAttributePrivate() {
        var attribute = Adresse.class.getDeclaredFields();
        for (var attr : attribute) {
            assertTrue(isPrivate(attr.getModifiers()),
                    "Attribut '" + attr.getName() + "' ist nicht richtig gekapselt");
        }
    }

    @Test
    public void testKonstruktorExceptionNullOderLeer() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Adresse(null, 6, 5210, "Windisch");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new Adresse("Bahnhofstrasse", 6, 5210, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new Adresse("", 6, 5210, "Windisch");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new Adresse("Bahnhofstrasse", 6, 5210, "");
        });

        // teste auch eine gültige - keine Exception
        new Adresse("Bahnhofstrasse", 6, 5210, "Windisch");
    }

    @Test
    public void testKonstruktorExceptionNummer() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Adresse("Bahnhofstrasse", -1, 5210, "Windisch");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new Adresse("Bahnhofstrasse", -100, 5210, "Windisch");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new Adresse("Bahnhofstrasse", 0, 5210, "Windisch");
        });

        // teste auch eine gültige - keine Exception
        new Adresse("Bahnhofstrasse", 6, 5210, "Windisch");
    }

    @Test
    public void testKonstruktorExceptionPlz() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Adresse("Bahnhofstrasse", 6, 0, "Windisch");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new Adresse("Bahnhofstrasse", 6, -1, "Windisch");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new Adresse("Bahnhofstrasse", 6, -1000, "Windisch");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new Adresse("Bahnhofstrasse", 6, -5210, "Windisch");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new Adresse("Bahnhofstrasse", 6, 999, "Windisch");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new Adresse("Bahnhofstrasse", 6, 20, "Windisch");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new Adresse("Bahnhofstrasse", 6, 10000, "Windisch");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new Adresse("Bahnhofstrasse", 6, Integer.MAX_VALUE, "Windisch");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new Adresse("Bahnhofstrasse", 6, Integer.MIN_VALUE, "Windisch");
        });

        // teste auch gültige - keine Exception
        new Adresse("Bahnhofstrasse", 6, 5210, "Windisch");
        new Adresse("Bahnhofstrasse", 6, 1000, "Lausanne");
        new Adresse("Bahnhofstrasse", 6, 2525, "Le Landeron");
        new Adresse("Bahnhofstrasse", 6, 9000, "St. Gallen");
        new Adresse("Bahnhofstrasse", 6, 9999, "Musterstadt");
    }

    @Test
    public void testSetStrasse() {
        Adresse adresse = new Adresse("Bahnhofstrasse", 6, 5210, "Windisch");
        assertEquals("Bahnhofstrasse", adresse.getStrasse());
        adresse.setStrasse("Bahnhofweg");
        assertEquals("Bahnhofweg", adresse.getStrasse());
        adresse.setStrasse("Musterstrasse");
        assertEquals("Musterstrasse", adresse.getStrasse());
    }

    @Test
    public void testSetStrasseException() {
        Adresse adresse = new Adresse("Bahnhofstrasse", 6, 5210, "Windisch");
        assertThrows(IllegalArgumentException.class, () -> {
            adresse.setStrasse(null);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            adresse.setStrasse("");
        });
    }

    @Test
    public void testSetNummer() {
        Adresse adresse = new Adresse("Bahnhofstrasse", 6, 5210, "Windisch");
        assertEquals(6, adresse.getNummer());
        adresse.setNummer(1);
        assertEquals(1, adresse.getNummer());
        adresse.setNummer(10001);
        assertEquals(10001, adresse.getNummer());
    }

    @Test
    public void testSetNummerException() {
        Adresse adresse = new Adresse("Bahnhofstrasse", 6, 5210, "Windisch");
        assertThrows(IllegalArgumentException.class, () -> {
            adresse.setNummer(-1);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            adresse.setNummer(-100);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            adresse.setNummer(0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            adresse.setNummer(Integer.MIN_VALUE);
        });
    }

    @Test
    public void testSetPlz() {
        Adresse adresse = new Adresse("Bahnhofstrasse", 6, 5210, "Windisch");
        assertEquals(5210, adresse.getPlz());
        adresse.setPlz(4053);
        assertEquals(4053, adresse.getPlz());
        adresse.setPlz(1000);
        assertEquals(1000, adresse.getPlz());
        adresse.setPlz(9999);
        assertEquals(9999, adresse.getPlz());
    }

    @Test
    public void testSetPlzException() {
        Adresse adresse = new Adresse("Bahnhofstrasse", 6, 5210, "Windisch");
        assertThrows(IllegalArgumentException.class, () -> {
            adresse.setPlz(0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            adresse.setPlz(-1);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            adresse.setPlz(23);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            adresse.setPlz(999);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            adresse.setPlz(10000);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            adresse.setPlz(Integer.MIN_VALUE);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            adresse.setPlz(Integer.MAX_VALUE);
        });
    }

    @Test
    public void testSetOrt() {
        Adresse adresse = new Adresse("Bahnhofstrasse", 6, 5210, "Windisch");
        assertEquals("Windisch", adresse.getOrt());
        adresse.setOrt("Basel");
        assertEquals("Basel", adresse.getOrt());
        adresse.setOrt("Bern");
        assertEquals("Bern", adresse.getOrt());
    }

    @Test
    public void testSetOrtException() {
        Adresse adresse = new Adresse("Bahnhofstrasse", 6, 5210, "Windisch");
        assertThrows(IllegalArgumentException.class, () -> {
            adresse.setOrt(null);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            adresse.setOrt("");
        });
    }
}
