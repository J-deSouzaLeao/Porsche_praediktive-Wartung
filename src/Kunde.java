public class Kunde {
    String id;
    String name;
    String email;
    boolean hatWerksvertrag;

    public Kunde(String id, String name, String email, boolean hatWerksvertrag) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.hatWerksvertrag = hatWerksvertrag;
    }
}