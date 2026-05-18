public class Ersatzteil {
    String id;
    String name;
    double preis;
    int lieferzeitTage; // 0, wenn auf Lager

    public Ersatzteil(String id, String name, double preis, int lieferzeitTage) {
        this.id = id;
        this.name = name;
        this.preis = preis;
        this.lieferzeitTage = lieferzeitTage;
    }
}