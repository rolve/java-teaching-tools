public class Adresse {
    private String strasse;
    private int nummer;
    private int plz;
    private String ort;

    public Adresse(String strasse, int nummer, int plz, String ort) {
        this.strasse = strasse;
        this.nummer = nummer;
        this.plz = plz;
        this.ort = ort;
        if (!gueltig()) {
            throw new IllegalArgumentException();
        }
    }

    private boolean gueltig() {
        return strasse != null && !strasse.isEmpty()
                && nummer > 0
                && plz >= 1000 && plz <= 9999
                && ort != null && !ort.isEmpty();
    }

    public String getStrasse() {
        return strasse;
    }

    public void setStrasse(String strasse) {
        if (strasse == null || strasse.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.strasse = strasse;
    }

    public int getNummer() {
        return nummer;
    }

    public void setNummer(int nummer) {
        if (nummer <= 0) {
            throw new IllegalArgumentException();
        }
        this.nummer = nummer;
    }

    public int getPlz() {
        return plz;
    }

    public void setPlz(int plz) {
        if (plz < 1000 || plz > 9999) {
            throw new IllegalArgumentException();
        }
        this.plz = plz;
    }

    public String getOrt() {
        return ort;
    }

    public void setOrt(String ort) {
        if (ort == null || ort.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.ort = ort;
    }

    public String toString() {
        return strasse + " " + nummer + "\n" + plz + " " + ort;
    }
}
