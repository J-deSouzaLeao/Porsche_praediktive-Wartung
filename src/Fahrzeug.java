public class Fahrzeug {
    String fin; // Fahrgestellnummer
    String modell;
    Kunde besitzer;
    String pzId; // ID des zuständigen Porsche Zentrums

    public Fahrzeug(String fin, String modell, Kunde besitzer, String pzId) {
        this.fin = fin;
        this.modell = modell;
        this.besitzer = besitzer;
        this.pzId = pzId;
    }
}