import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(OrderAnnotation.class)
public class JahreszeitTest {

    // astronomische Jahreszeiten https://de.wikipedia.org/wiki/Jahreszeit

    /**
     * `getJahreszeit` mit verschiedenen Tagen im April und Mai aufrufen und
     * prüfen, dass "Frühling" zurückgegeben wird.
     */
    @Order(1)
    @Test
    void fruehlingEinfach() {
        assertEquals("Frühling", Jahreszeit.getJahreszeit(4, 1));
        assertEquals("Frühling", Jahreszeit.getJahreszeit(4, 15));
        assertEquals("Frühling", Jahreszeit.getJahreszeit(4, 30));
        assertEquals("Frühling", Jahreszeit.getJahreszeit(5, 1));
        assertEquals("Frühling", Jahreszeit.getJahreszeit(5, 18));
        assertEquals("Frühling", Jahreszeit.getJahreszeit(5, 31));
    }

    /**
     * `getJahreszeit` mit verschiedenen Tagen im Juli und August aufrufen und
     * prüfen, dass "Sommer" zurückgegeben wird.
     */
    @Order(2)
    @Test
    void sommerEinfach() {
        assertEquals("Sommer", Jahreszeit.getJahreszeit(7, 1));
        assertEquals("Sommer", Jahreszeit.getJahreszeit(7, 17));
        assertEquals("Sommer", Jahreszeit.getJahreszeit(7, 31));
        assertEquals("Sommer", Jahreszeit.getJahreszeit(8, 1));
        assertEquals("Sommer", Jahreszeit.getJahreszeit(8, 9));
        assertEquals("Sommer", Jahreszeit.getJahreszeit(8, 31));
    }

    /**
     * `getJahreszeit` mit verschiedenen Tagen im Oktober und November aufrufen
     * und prüfen, dass "Herbst" zurückgegeben wird.
     */
    @Order(3)
    @Test
    void herbstEinfach() {
        assertEquals("Herbst", Jahreszeit.getJahreszeit(10, 1));
        assertEquals("Herbst", Jahreszeit.getJahreszeit(10, 14));
        assertEquals("Herbst", Jahreszeit.getJahreszeit(10, 31));
        assertEquals("Herbst", Jahreszeit.getJahreszeit(11, 1));
        assertEquals("Herbst", Jahreszeit.getJahreszeit(11, 21));
        assertEquals("Herbst", Jahreszeit.getJahreszeit(11, 30));
    }

    /**
     * `getJahreszeit` mit verschiedenen Tagen im Januar und Februar aufrufen und
     * prüfen, dass "Winter" zurückgegeben wird.
     */
    @Order(4)
    @Test
    void winterEinfach() {
        assertEquals("Winter", Jahreszeit.getJahreszeit(1, 1));
        assertEquals("Winter", Jahreszeit.getJahreszeit(1, 12));
        assertEquals("Winter", Jahreszeit.getJahreszeit(1, 31));
        assertEquals("Winter", Jahreszeit.getJahreszeit(2, 1));
        assertEquals("Winter", Jahreszeit.getJahreszeit(2, 19));
        assertEquals("Winter", Jahreszeit.getJahreszeit(2, 28));
    }

    /**
     * `getJahreszeit` mit verschiedenen Tagen im März aufrufen und prüfen, dass
     * "Winter" zurückgegeben wird, wenn der Tag kleiner als 20 ist, und "Frühling",
     * wenn der Tag grösser oder gleich 20 ist.
     */
    @Order(5)
    @Test
    void maerz() {
        assertEquals("Winter", Jahreszeit.getJahreszeit(3, 1));
        assertEquals("Winter", Jahreszeit.getJahreszeit(3, 18));
        assertEquals("Winter", Jahreszeit.getJahreszeit(3, 19));
        assertEquals("Frühling", Jahreszeit.getJahreszeit(3, 20));
        assertEquals("Frühling", Jahreszeit.getJahreszeit(3, 21));
        assertEquals("Frühling", Jahreszeit.getJahreszeit(3, 31));
    }

    /**
     * `getJahreszeit` mit verschiedenen Tagen im Juni aufrufen und prüfen, dass
     * "Frühling" zurückgegeben wird, wenn der Tag kleiner als 21 ist, und "Sommer",
     * wenn der Tag grösser oder gleich 21 ist.
     */
    @Order(6)
    @Test
    void juni() {
        assertEquals("Frühling", Jahreszeit.getJahreszeit(6, 1));
        assertEquals("Frühling", Jahreszeit.getJahreszeit(6, 19));
        assertEquals("Frühling", Jahreszeit.getJahreszeit(6, 20));
        assertEquals("Sommer", Jahreszeit.getJahreszeit(6, 21));
        assertEquals("Sommer", Jahreszeit.getJahreszeit(6, 22));
        assertEquals("Sommer", Jahreszeit.getJahreszeit(6, 30));
    }

    /**
     * `getJahreszeit` mit verschiedenen Tagen im September aufrufen und prüfen, dass
     * "Sommer" zurückgegeben wird, wenn der Tag kleiner als 22 ist, und "Herbst",
     * wenn der Tag grösser oder gleich 22 ist.
     */
    @Order(7)
    @Test
    void september() {
        assertEquals("Sommer", Jahreszeit.getJahreszeit(9, 1));
        assertEquals("Sommer", Jahreszeit.getJahreszeit(9, 20));
        assertEquals("Sommer", Jahreszeit.getJahreszeit(9, 21));
        assertEquals("Herbst", Jahreszeit.getJahreszeit(9, 22));
        assertEquals("Herbst", Jahreszeit.getJahreszeit(9, 23));
        assertEquals("Herbst", Jahreszeit.getJahreszeit(9, 30));
    }

    /**
     * `getJahreszeit` mit verschiedenen Tagen im Dezember aufrufen und prüfen, dass
     * "Herbst" zurückgegeben wird, wenn der Tag kleiner als 21 ist, und "Winter",
     * wenn der Tag grösser oder gleich 21 ist.
     */
    @Order(8)
    @Test
    void dezember() {
        assertEquals("Herbst", Jahreszeit.getJahreszeit(12, 1));
        assertEquals("Herbst", Jahreszeit.getJahreszeit(12, 20));
        assertEquals("Herbst", Jahreszeit.getJahreszeit(12, 21));
        assertEquals("Winter", Jahreszeit.getJahreszeit(12, 22));
        assertEquals("Winter", Jahreszeit.getJahreszeit(12, 23));
        assertEquals("Winter", Jahreszeit.getJahreszeit(12, 31));
    }
}
