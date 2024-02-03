public class Jahreszeit {
    public static String getJahreszeit(int monat, int tag) {
        String resultat;
        if (monat == 3 && tag > 19 || monat == 4 || monat == 5 || monat == 6 && tag < 21) {
            resultat = "Frühling";
        } else if (monat == 6 || monat == 7 || monat == 8 || monat == 9 && tag < 22) {
            resultat = "Sommer";
        } else if (monat == 9 || monat == 10 || monat == 11 || monat == 12 && tag < 22) {
            resultat = "Herbst";
        } else {
            resultat = "Winter";
        }
        return resultat;
    }
}
