package smarthome;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class HomeTest {

    @Test
    public void turnAllOff() {
        Home home = createSmallHome();

        home.turnAllOff();

        assertFalse(home.getRooms()[0].getLamps()[0].isOn());
        assertFalse(home.getRooms()[1].getLamps()[0].isOn());
        assertFalse(home.getRooms()[1].getLamps()[1].isOn());
        assertFalse(home.getRooms()[2].getLamps()[0].isOn());
        assertFalse(home.getRooms()[2].getLamps()[1].isOn());
        assertFalse(home.getRooms()[3].getLamps()[0].isOn());

        // turn some on again and test once more
        home.getRooms()[1].getLamps()[1].turnOn();
        home.getRooms()[2].getLamps()[0].turnOn();
        home.getRooms()[2].getLamps()[1].turnOn();

        home.turnAllOff();

        assertFalse(home.getRooms()[0].getLamps()[0].isOn());
        assertFalse(home.getRooms()[1].getLamps()[0].isOn());
        assertFalse(home.getRooms()[1].getLamps()[1].isOn());
        assertFalse(home.getRooms()[2].getLamps()[0].isOn());
        assertFalse(home.getRooms()[2].getLamps()[1].isOn());
        assertFalse(home.getRooms()[3].getLamps()[0].isOn());
    }

    @Test
    public void turnAllOffRoomsWithoutLamps() {
        Home home = createTinyHome();

        home.turnAllOff();

        assertFalse(home.getRooms()[0].getLamps()[0].isOn());
        assertFalse(home.getRooms()[2].getLamps()[0].isOn());
    }

    @Test
    public void turnAllBrightOn() {
        Home home = createSmallHome();
        home.getRooms()[0].getLamps()[0].turnOff();
        home.getRooms()[1].getLamps()[0].turnOff();
        home.getRooms()[1].getLamps()[1].turnOff();
        home.getRooms()[2].getLamps()[0].turnOff();
        home.getRooms()[2].getLamps()[1].turnOff();
        home.getRooms()[3].getLamps()[0].turnOff();

        home.turnAllBright();

        assertTrue(home.getRooms()[0].getLamps()[0].isOn());
        assertTrue(home.getRooms()[1].getLamps()[0].isOn());
        assertTrue(home.getRooms()[1].getLamps()[1].isOn());
        assertTrue(home.getRooms()[2].getLamps()[0].isOn());
        assertTrue(home.getRooms()[2].getLamps()[1].isOn());
        assertTrue(home.getRooms()[3].getLamps()[0].isOn());
    }

    @Test
    public void turnAllBrightBrightness() {
        Home home = createSmallHome();
        home.getRooms()[0].getLamps()[0].setBrightness(0.1);
        home.getRooms()[1].getLamps()[0].setBrightness(0.2);
        home.getRooms()[1].getLamps()[1].setBrightness(0.3);
        home.getRooms()[2].getLamps()[0].setBrightness(0.5);
        home.getRooms()[2].getLamps()[1].setBrightness(0.9);
        home.getRooms()[3].getLamps()[0].setBrightness(0.95);

        home.turnAllBright();

        assertEquals(1.0, home.getRooms()[0].getLamps()[0].getBrightness(), 0.0001);
        assertEquals(1.0, home.getRooms()[1].getLamps()[0].getBrightness(), 0.0001);
        assertEquals(1.0, home.getRooms()[1].getLamps()[1].getBrightness(), 0.0001);
        assertEquals(1.0, home.getRooms()[2].getLamps()[0].getBrightness(), 0.0001);
        assertEquals(1.0, home.getRooms()[2].getLamps()[1].getBrightness(), 0.0001);
        assertEquals(1.0, home.getRooms()[3].getLamps()[0].getBrightness(), 0.0001);
    }

    @Test
    public void turnAllBrightRoomsWithoutLamps() {
        Home home = createTinyHome();
        home.getRooms()[0].getLamps()[0].turnOff();
        home.getRooms()[2].getLamps()[0].turnOff();
        home.getRooms()[0].getLamps()[0].setBrightness(0.2);
        home.getRooms()[2].getLamps()[0].setBrightness(0.8);

        home.turnAllBright();

        assertTrue(home.getRooms()[0].getLamps()[0].isOn());
        assertTrue(home.getRooms()[2].getLamps()[0].isOn());
        assertEquals(1.0, home.getRooms()[0].getLamps()[0].getBrightness(), 0.0001);
        assertEquals(1.0, home.getRooms()[2].getLamps()[0].getBrightness(), 0.0001);
    }

    @Test
    public void allBrighter() {
        Home home = createSmallHome();
        home.getRooms()[0].getLamps()[0].setBrightness(0.1);
        home.getRooms()[1].getLamps()[0].setBrightness(0.2);
        home.getRooms()[1].getLamps()[1].setBrightness(0.3);
        home.getRooms()[2].getLamps()[0].setBrightness(0.5);
        home.getRooms()[2].getLamps()[1].setBrightness(0.9);
        home.getRooms()[3].getLamps()[0].setBrightness(0.95);

        home.allBrighter();

        assertEquals(0.2, home.getRooms()[0].getLamps()[0].getBrightness(), 0.0001);
        assertEquals(0.3, home.getRooms()[1].getLamps()[0].getBrightness(), 0.0001);
        assertEquals(0.4, home.getRooms()[1].getLamps()[1].getBrightness(), 0.0001);
        assertEquals(0.6, home.getRooms()[2].getLamps()[0].getBrightness(), 0.0001);
        assertEquals(1.0, home.getRooms()[2].getLamps()[1].getBrightness(), 0.0001);
        assertEquals(1.0, home.getRooms()[3].getLamps()[0].getBrightness(), 0.0001);
    }

    @Test
    public void allBrighterRoomsWithoutLamps() {
        Home home = createTinyHome();
        home.getRooms()[0].getLamps()[0].setBrightness(0.8);
        home.getRooms()[2].getLamps()[0].setBrightness(0.0);

        home.allBrighter();

        assertEquals(0.9, home.getRooms()[0].getLamps()[0].getBrightness(), 0.0001);
        assertEquals(0.1, home.getRooms()[2].getLamps()[0].getBrightness(), 0.0001);
    }

    @Test
    public void allDarker() {
        Home home = createSmallHome();
        home.getRooms()[0].getLamps()[0].setBrightness(0.3);
        home.getRooms()[1].getLamps()[0].setBrightness(0.35);
        home.getRooms()[1].getLamps()[1].setBrightness(0.4);
        home.getRooms()[2].getLamps()[0].setBrightness(0.5);
        home.getRooms()[2].getLamps()[1].setBrightness(0.9);
        home.getRooms()[3].getLamps()[0].setBrightness(1.0);

        home.allDarker();

        assertEquals(0.2, home.getRooms()[0].getLamps()[0].getBrightness(), 0.0001);
        assertEquals(0.25, home.getRooms()[1].getLamps()[0].getBrightness(), 0.0001);
        assertEquals(0.3, home.getRooms()[1].getLamps()[1].getBrightness(), 0.0001);
        assertEquals(0.4, home.getRooms()[2].getLamps()[0].getBrightness(), 0.0001);
        assertEquals(0.8, home.getRooms()[2].getLamps()[1].getBrightness(), 0.0001);
        assertEquals(0.9, home.getRooms()[3].getLamps()[0].getBrightness(), 0.0001);
    }

    @Test
    public void allDarkerMin20Percent() {
        Home home = createSmallHome();
        home.getRooms()[0].getLamps()[0].setBrightness(0.1);
        home.getRooms()[1].getLamps()[0].setBrightness(0.0);

        home.allDarker();

        assertEquals(0.2, home.getRooms()[0].getLamps()[0].getBrightness(), 0.0001);
        assertEquals(0.2, home.getRooms()[1].getLamps()[0].getBrightness(), 0.0001);
    }

    @Test
    public void allDarkerRoomsWithoutLamps() {
        Home home = createTinyHome();
        home.getRooms()[0].getLamps()[0].setBrightness(0.4);
        home.getRooms()[2].getLamps()[0].setBrightness(0.05);

        home.allDarker();

        assertEquals(0.3, home.getRooms()[0].getLamps()[0].getBrightness(), 0.0001);
        assertEquals(0.2, home.getRooms()[2].getLamps()[0].getBrightness(), 0.0001);
    }

    @Test
    public void saveEnergySmallHome() {
        Home home = createSmallHome();

        home.saveEnergy();

        assertTrue(home.getRooms()[0].getLamps()[0].isOn());
        assertTrue(home.getRooms()[1].getLamps()[0].isOn());
        assertFalse(home.getRooms()[1].getLamps()[1].isOn());
        assertFalse(home.getRooms()[2].getLamps()[0].isOn());
        assertTrue(home.getRooms()[2].getLamps()[1].isOn());
        assertTrue(home.getRooms()[3].getLamps()[0].isOn());

        // test with all lamps initially off
        home.getRooms()[0].getLamps()[0].turnOff();
        home.getRooms()[1].getLamps()[0].turnOff();
        home.getRooms()[1].getLamps()[1].turnOff();
        home.getRooms()[2].getLamps()[0].turnOff();
        home.getRooms()[2].getLamps()[1].turnOff();
        home.getRooms()[3].getLamps()[0].turnOff();

        home.saveEnergy();

        assertTrue(home.getRooms()[0].getLamps()[0].isOn());
        assertTrue(home.getRooms()[1].getLamps()[0].isOn());
        assertFalse(home.getRooms()[1].getLamps()[1].isOn());
        assertFalse(home.getRooms()[2].getLamps()[0].isOn());
        assertTrue(home.getRooms()[2].getLamps()[1].isOn());
        assertTrue(home.getRooms()[3].getLamps()[0].isOn());
    }

    @Test
    public void saveEnergyLargeHome() {
        Home home = createLargeHome();

        home.saveEnergy();

        assertTrue(home.getRooms()[0].getLamps()[0].isOn());

        assertFalse(home.getRooms()[1].getLamps()[0].isOn());
        assertFalse(home.getRooms()[1].getLamps()[1].isOn());
        assertTrue(home.getRooms()[1].getLamps()[2].isOn());

        assertTrue(home.getRooms()[2].getLamps()[0].isOn());
        assertFalse(home.getRooms()[2].getLamps()[1].isOn());

        assertFalse(home.getRooms()[3].getLamps()[0].isOn());
        assertFalse(home.getRooms()[3].getLamps()[1].isOn());
        assertFalse(home.getRooms()[3].getLamps()[2].isOn());
        assertTrue(home.getRooms()[3].getLamps()[3].isOn());
        assertFalse(home.getRooms()[3].getLamps()[4].isOn());

        assertFalse(home.getRooms()[4].getLamps()[0].isOn());
        assertTrue(home.getRooms()[4].getLamps()[1].isOn());

        assertTrue(home.getRooms()[5].getLamps()[0].isOn());

        assertFalse(home.getRooms()[6].getLamps()[0].isOn());
        assertTrue(home.getRooms()[6].getLamps()[1].isOn());
        assertFalse(home.getRooms()[6].getLamps()[2].isOn());
    }

    @Test
    public void saveEnergyBrightness() {
        Home home = createSmallHome();

        home.saveEnergy();

        assertTrue(home.getRooms()[0].getLamps()[0].isOn());
        assertEquals(0.8, home.getRooms()[0].getLamps()[0].getBrightness());
        assertTrue(home.getRooms()[1].getLamps()[0].isOn());
        assertEquals(0.8, home.getRooms()[1].getLamps()[0].getBrightness());
        assertFalse(home.getRooms()[1].getLamps()[1].isOn());
        assertFalse(home.getRooms()[2].getLamps()[0].isOn());
        assertTrue(home.getRooms()[2].getLamps()[1].isOn());
        assertEquals(0.8, home.getRooms()[2].getLamps()[1].getBrightness());
        assertTrue(home.getRooms()[3].getLamps()[0].isOn());
        assertEquals(0.8, home.getRooms()[3].getLamps()[0].getBrightness());
    }

    @Test
    public void findHallway() {
        var home = createSmallHome();
        var hallway = home.findHallway();
        assertSame(home.getRooms()[2], hallway);

        home = createLargeHome();
        hallway = home.findHallway();
        assertSame(home.getRooms()[3], hallway);

        home = createTinyHome();
        hallway = home.findHallway();
        assertSame(home.getRooms()[2], hallway);
    }

    @Test
    public void findBedrooms() {
        var home = createSmallHome();
        var bedrooms = home.findBedrooms();
        assertNotNull(bedrooms);
        assertTrue(bedrooms.length <= 4);
        assertContainsNot(bedrooms, home.getRooms()[0]);
        assertContainsNot(bedrooms, home.getRooms()[1]);
        assertContainsNot(bedrooms, home.getRooms()[2]);
        assertContains(bedrooms, home.getRooms()[3]);

        home = createLargeHome();
        bedrooms = home.findBedrooms();
        assertTrue(bedrooms.length <= 7);
        assertNotNull(bedrooms);
        assertContainsNot(bedrooms, home.getRooms()[0]);
        assertContainsNot(bedrooms, home.getRooms()[1]);
        assertContainsNot(bedrooms, home.getRooms()[2]);
        assertContainsNot(bedrooms, home.getRooms()[3]);
        assertContains(bedrooms, home.getRooms()[4]);
        assertContains(bedrooms, home.getRooms()[5]);
        assertContains(bedrooms, home.getRooms()[6]);
    }

    @Test
    public void findBedroomsZero() {
        var home = createTinyHome();
        var bedrooms = home.findBedrooms();
        assertNotNull(bedrooms);
        assertTrue(bedrooms.length <= 3);
        for (Room element : bedrooms) {
            assertNull(element);
        }
    }

    private void assertContains(Room[] array, Room room) {
        for (Room element : array) {
            if (element == room) {
                return;
            }
        }
        fail(room + " NICHT in Array enthalten (Array-Inhalt: " + Arrays.toString(array) + ")");
    }

    private void assertContainsNot(Room[] array, Room room) {
        for (Room element : array) {
            if (element == room) {
                fail(room + " fälschlicherweise in Array enthalten (Array-Inhalt: " + Arrays.toString(array) + ")");
            }
        }
    }

    @Test
    public void nightModeSmallHome() {
        var home = createSmallHome();

        home.nightMode();

        assertFalse(home.getRooms()[0].getLamps()[0].isOn());
        assertFalse(home.getRooms()[1].getLamps()[0].isOn());
        assertFalse(home.getRooms()[1].getLamps()[1].isOn());
        // one lamp in Hallway should be on, but not both:
        assertTrue(home.getRooms()[2].getLamps()[0].isOn() || home.getRooms()[2].getLamps()[1].isOn());
        assertFalse(home.getRooms()[2].getLamps()[0].isOn() && home.getRooms()[2].getLamps()[1].isOn());
        // only one lamp in Bedroom, which should be on
        assertTrue(home.getRooms()[3].getLamps()[0].isOn());

        // test with all lamps initially off
        home.getRooms()[0].getLamps()[0].turnOff();
        home.getRooms()[1].getLamps()[0].turnOff();
        home.getRooms()[1].getLamps()[1].turnOff();
        home.getRooms()[2].getLamps()[0].turnOff();
        home.getRooms()[2].getLamps()[1].turnOff();
        home.getRooms()[3].getLamps()[0].turnOff();

        home.nightMode();

        assertFalse(home.getRooms()[0].getLamps()[0].isOn());
        assertFalse(home.getRooms()[1].getLamps()[0].isOn());
        assertFalse(home.getRooms()[1].getLamps()[1].isOn());
        assertTrue(home.getRooms()[2].getLamps()[0].isOn() || home.getRooms()[2].getLamps()[1].isOn());
        assertFalse(home.getRooms()[2].getLamps()[0].isOn() && home.getRooms()[2].getLamps()[1].isOn());
        assertTrue(home.getRooms()[3].getLamps()[0].isOn());
    }

    @Test
    public void nightModeLargeHome() {
        var home = createLargeHome();

        home.nightMode();

        assertFalse(home.getRooms()[0].getLamps()[0].isOn());
        assertFalse(home.getRooms()[1].getLamps()[0].isOn());
        assertFalse(home.getRooms()[1].getLamps()[1].isOn());
        assertFalse(home.getRooms()[1].getLamps()[2].isOn());
        assertFalse(home.getRooms()[2].getLamps()[0].isOn());
        assertFalse(home.getRooms()[2].getLamps()[1].isOn());
        // one lamp in Hallway should be on, but not more than one:
        int count = 0;
        for (Lamp lamp : home.getRooms()[3].getLamps()) {
            if (lamp.isOn()) {
                count++;
            }
        }
        assertEquals(1, count);
        // one lamp in Bedroom 1 should be on, but not both:
        assertTrue(home.getRooms()[4].getLamps()[0].isOn() || home.getRooms()[4].getLamps()[1].isOn());
        assertFalse(home.getRooms()[4].getLamps()[0].isOn() && home.getRooms()[4].getLamps()[1].isOn());
        // the only lamp in Bedroom 2 should be on:
        assertTrue(home.getRooms()[5].getLamps()[0].isOn());
        // one lamp in Bedroom 3 should be on, but not more than one:
        count = 0;
        for (Lamp lamp : home.getRooms()[6].getLamps()) {
            if (lamp.isOn()) {
                count++;
            }
        }
        assertEquals(1, count);
    }

    @Test
    public void nightModeNoBedrooms() {
        var home = createTinyHome();

        home.nightMode();

        assertFalse(home.getRooms()[0].getLamps()[0].isOn());
        assertTrue(home.getRooms()[2].getLamps()[0].isOn());
    }

    private Home createTinyHome() {
        Lamp[] kitchenLamps = {
                new Lamp("Ceiling Lamp", 2.5)};
        Room kitchen = new Room("Kitchen", kitchenLamps);
        Room livingRoom = new Room("Living Room", new Lamp[0]);
        Lamp[] hallwayLamps = {
                new Lamp("Ceiling Lamp", 22.4)};
        Room hallway = new Room("Hallway", hallwayLamps);
        Room[] rooms = {kitchen, livingRoom, hallway};
        return new Home(rooms);
    }

    private Home createSmallHome() {
        Lamp[] kitchenLamps = {
                new Lamp("Ceiling Lamp", 2.5)};
        Room kitchen = new Room("Kitchen", kitchenLamps);
        Lamp[] livingRoomLamps = {
                new Lamp("Table Lamp", 7.0),
                new Lamp("Ceiling Lamp", 13.6)};
        Room livingRoom = new Room("Living Room", livingRoomLamps);
        Lamp[] hallwayLamps = {
                new Lamp("Ceiling Lamp", 22.4),
                new Lamp("Mirror Lamp 1", 3.5)};
        Room hallway = new Room("Hallway", hallwayLamps);
        Lamp[] bedroomLamps = {
                new Lamp("Ceiling Lamp", 10.1)};
        Room bedroom = new Room("Bedroom", bedroomLamps);
        Room[] rooms = {kitchen, livingRoom, hallway, bedroom};
        return new Home(rooms);
    }

    private static Home createLargeHome() {
        Lamp[] kitchenLamps = {
                new Lamp("Ceiling Lamp", 2.5)};
        Room kitchen = new Room("Kitchen", kitchenLamps);
        Lamp[] livingRoomLamps = {
                new Lamp("Table Lamp", 7.0),
                new Lamp("Ceiling Lamp", 13.6),
                new Lamp("Niche Lamp", 5.2)};
        Room livingRoom = new Room("Living Room", livingRoomLamps);
        Lamp[] tvRoomLamps = {
                new Lamp("TV Lamp", 8.1),
                new Lamp("Ceiling Lamp", 12.7)};
        Room tvRoom = new Room("TV Room", tvRoomLamps);
        Lamp[] hallwayLamps = {
                new Lamp("Ceiling Lamp", 22.4),
                new Lamp("Sofa Lamp", 7.8),
                new Lamp("Entry Lamp", 6.6),
                new Lamp("Mirror Lamp 1", 3.5),
                new Lamp("Mirror Lamp 2", 3.8)};
        Room hallway = new Room("Hallway", hallwayLamps);
        Lamp[] bedroom1Lamps = {
                new Lamp("Ceiling Lamp", 10.1),
                new Lamp("Desk Lamp", 5.1)};
        Room bedroom1 = new Room("Bedroom 1", bedroom1Lamps);
        Lamp[] bedroom2Lamps = {
                new Lamp("Ceiling Lamp", 12.3)};
        Room bedroom2 = new Room("Bedroom 2", bedroom2Lamps);
        Lamp[] bedroom3Lamps = {
                new Lamp("Ceiling Lamp", 18.0),
                new Lamp("Bed Lamp 1", 2.2),
                new Lamp("Bed Lamp 2", 2.3)};
        Room bedroom3 = new Room("Bedroom 3", bedroom3Lamps);
        Room[] rooms = {
                kitchen, livingRoom, tvRoom, hallway,
                bedroom1, bedroom2, bedroom3};
        return new Home(rooms);
    }
}
