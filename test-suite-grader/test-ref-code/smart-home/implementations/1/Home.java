package smarthome;

import java.util.Random;

/**
 * Die Hauptklasse im SmartHome-System. Enthält eine Sammlung
 * von Räumen (Klasse {@link Room}) und eine Reihe von Methoden,
 * welche die SmartHome-Funktionalität implementieren.
 */
public class Home {
    private Room[] rooms;

    public Home(Room[] rooms) {
        this.rooms = rooms;
    }

    public Room[] getRooms() {
        return rooms;
    }

    /**
     * Schaltet alle Lampen in allen Räumen aus.
     */
    public void turnAllOff() {
        for (Room room : rooms) {
            for (Lamp lamp : room.getLamps()) {
                lamp.turnOff();
            }
        }
    }

    /**
     * Schaltet alle Lampen und allen Räumen ein und setzt die
     * Helligkeit auf 100% (1.0).
     */
    public void turnAllBright() {
        for (Room room : rooms) {
            for (Lamp lamp : room.getLamps()) {
                lamp.setBrightness(1);
                lamp.turnOn();
            }
        }
    }

    /**
     * Erhöht die Helligkeit von allen Lampen im Haus um 0.1.
     * (Es ist egal, ob die Lampen ein- oder ausgeschaltet sind).
     */
    public void allBrighter() {
        for (Room room : rooms) {
            for (Lamp lamp : room.getLamps()) {
                lamp.setBrightness(lamp.getBrightness() + 0.1);
            }
        }
    }

    /**
     * Reduziert die Helligkeit von allen Lampen im Haus um 0.1,
     * bis zu einem Minimum von 0.2. Lampen, die bereits eine
     * weniger hell sind, werden ebenfalls auf eine Helligkeit von
     * 0.2 gesetzt.
     * (Es ist egal, ob die Lampen ein- oder ausgeschaltet sind).
     */
    public void allDarker() {
        for (Room room : rooms) {
            for (Lamp lamp : room.getLamps()) {
                double brightness = lamp.getBrightness() - 0.1;
                lamp.setBrightness(Math.max(0.2, brightness));
            }
        }
    }

    /**
     * Findet in jedem Raum die Lampe mit dem jeweils kleinsten
     * Stromverbrauch. Diese Lampen werden eingeschaltet und auf
     * eine Helligkeit von 0.8 gesetzt; alle anderen Lampen werden
     * ausgeschaltet.
     */
    public void saveEnergy() {
        turnAllOff();
        for (Room room : rooms) {
            Lamp cheapest = room.getLamps()[0];
            for (int i = 1; i < room.getLamps().length; i++) {
                Lamp lamp = room.getLamps()[i];
                if (lamp.getPowerConsumption() < cheapest.getPowerConsumption()) {
                    cheapest = lamp;
                }
            }
            cheapest.setBrightness(0.8);
            cheapest.turnOn();
        }
    }

    /**
     * Findet den Raum im Haus, der den Namen "Hallway" hat und
     * gibt ihn zurück. Es wird davon ausgegangen, dass immer
     * genau eine Hallway existiert.
     */
    public Room findHallway() {
        for (Room room : rooms) {
            if (room.getName().equals("Hallway")) {
                return room;
            }
        }
        return null;
    }

    /**
     * Findet alle Räume im Haus, welche "Bedroom" im Namen
     * enthalten, und gibt diese in einem Array zurück. Das Array
     * darf auch grösser als nötig sein und null-Einträge
     * enthalten. Es soll aber maximal so gross sein wie die
     * Gesamtanzahl der Räume im Haus.
     */
    public Room[] findBedrooms() {
        Room[] bedrooms = new Room[rooms.length];
        for (int i = 0; i < rooms.length; i++) {
            if (rooms[i].getName().contains("Bedroom")) {
                bedrooms[i] = rooms[i];
            }
        }
        return bedrooms;
    }

    /**
     * Schaltet den "Nachtmodus" ein, welcher in allen Bedrooms
     * und in der Hallway je eine (beliebige) Lampe einschaltet
     * und die Helligkeit auf 0.3 setzt. Alle anderen Lampen
     * werden ausgeschaltet.
     * Verwenden Sie die Methoden findHallway und findBedrooms,
     * die Sie zuvor implementiert haben.
     */
    public void nightMode() {
        turnAllOff();
        Room hallway = findHallway();
        hallway.getLamps()[0].setBrightness(0.3);
        hallway.getLamps()[0].turnOn();
        Room[] bedrooms = findBedrooms();
        for (Room bedroom : bedrooms) {
            if (bedroom != null) {
                bedroom.getLamps()[0].setBrightness(0.3);
                bedroom.getLamps()[0].turnOn();
            }
        }
    }
}
