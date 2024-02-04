public class Adresse {
    private String strasse;
    private int nummer;
    private int plz;
    private String ort;

    public Adresse(String strasse, int nummer, int plz, String ort) {
        setStrasse(strasse);
        setNummer(nummer);
        setPlz(plz);
        setOrt(ort);
    }

    public String getStrasse() {
        return strasse;
    }

    public void setStrasse(String strasse) {
        if (strasse == null || strasse.length() == 0) {
            throw new IllegalArgumentException();
        }
        this.strasse = strasse;
    }

    public int getNummer() {
        return nummer;
    }

    public void setNummer(int nummer) {
        if (nummer < 1) {
            throw new IllegalArgumentException();
        }
        this.nummer = nummer;
    }

    public int getPlz() {
        return plz;
    }

    public void setPlz(int plz) {
        if (plz <= 999 || plz >= 10_000) {
            throw new IllegalArgumentException();
        }
        this.plz = plz;
    }

    public String getOrt() {
        return ort;
    }

    public void setOrt(String ort) {
        if (ort == null || ort.length() == 0) {
            throw new IllegalArgumentException();
        }
        this.ort = ort;
    }

    public String toString() {
        return strasse + " " + nummer + "\n" + plz + " " + ort;
    }
}
