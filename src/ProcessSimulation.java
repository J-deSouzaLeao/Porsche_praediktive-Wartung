import java.time.LocalDate;
import java.util.Scanner;

public class ProcessSimulation {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // --- MOCK-DATEN SETUP ---
        System.out.println(">>> System fährt hoch. Lade Testdaten...");
        Kunde kunde = new Kunde("K-001", "Max Mustermann", "max@mustermann.de", true);
        Fahrzeug fahrzeug = new Fahrzeug("WP0ZZZ99Z123456", "911 Carrera GTS", kunde, "PZ-Stuttgart");

        ServiceDienstleistung[] katalog = {
                new ServiceDienstleistung("Bremsenwechsel Achse", 450.00),
                new ServiceDienstleistung("Große Inspektion", 950.00)
        };

        // --- SCHRITT 1: Trigger & Datenabruf ---
        System.out.println("\n[SCHRITT 1] Prädiktive Wartung ausgelöst!");
        System.out.println("Verschleiß erkannt an Fahrzeug FIN: " + fahrzeug.fin + " (" + fahrzeug.modell + ")");
        System.out.println("Kunde: " + fahrzeug.besitzer.id + " | Zuständiges PZ: " + fahrzeug.pzId);

        // --- SCHRITT 2: Bestandsprüfung ---
        System.out.println("\n[SCHRITT 2] Lagerbestand wird geprüft.");
        System.out.print("System-Frage: Ist das benötigte Ersatzteil (Bremsbeläge) im PZ vorrätig? (j/n): ");
        String lagerInput = scanner.nextLine();

        Ersatzteil teil;
        if (lagerInput.equalsIgnoreCase("j")) {
            System.out.println("--> Pfad A: Ersatzteil ist vorhanden. Wird für Montage reserviert.");
            teil = new Ersatzteil("E-911", "Bremsbeläge", 380.00, 0);
        } else {
            System.out.print("--> Pfad B: Nicht vorhanden. Bestellung wird ausgelöst. Lieferzeit in Tagen eingeben: ");
            int lieferzeit = Integer.parseInt(scanner.nextLine());
            teil = new Ersatzteil("E-911", "Bremsbeläge", 380.00, lieferzeit);
            System.out.println("    Ersatzteil bestellt. Lieferdatum in " + lieferzeit + " Tagen.");
        }

        // --- SCHRITT 3: Service-Katalog & PZ-Mitarbeiter Input ---
        System.out.println("\n[SCHRITT 3] PZ-Mitarbeiter Auswahl");
        System.out.println("Dienstleistungskatalog für " + fahrzeug.modell + ":");
        for (int i = 0; i < katalog.length; i++) {
            System.out.println((i + 1) + " - " + katalog[i].name + " (" + katalog[i].kosten + " EUR)");
        }
        System.out.print("PZ-Mitarbeiter: Bitte Service-Nummer wählen (1 oder 2): ");
        int serviceWahl = Integer.parseInt(scanner.nextLine()) - 1;
        ServiceDienstleistung gewaehlterService = katalog[serviceWahl];

        double basisGesamtkosten = teil.preis + gewaehlterService.kosten;
        System.out.println("--> Kosten addiert: Teile (" + teil.preis + " EUR) + Service (" + gewaehlterService.kosten + " EUR) = " + basisGesamtkosten + " EUR (Netto/Basis).");

        // --- SCHRITT 4: Rabattprüfung (Werksvertrag) ---
        System.out.println("\n[SCHRITT 4] Vertragsprüfung");
        double endpreis = basisGesamtkosten;

        if (fahrzeug.besitzer.hatWerksvertrag) {
            System.out.println("--> Pfad A: Kunde hat einen Werksvertrag. 15% Rabatt werden gewährt.");
            endpreis = basisGesamtkosten * 0.85;
        } else {
            System.out.println("--> Pfad B: Kein Werksvertrag vorhanden. Kein Rabatt.");
        }
        System.out.println("    Kalkulierter Endpreis: " + endpreis + " EUR");

        // --- SCHRITT 5: Terminberechnung ---
        System.out.println("\n[SCHRITT 5] Termin wird reserviert");
        int tageBisTermin = Math.max(14, teil.lieferzeitTage) + 3;
        LocalDate terminDatum = LocalDate.now().plusDays(tageBisTermin);

        if (teil.lieferzeitTage == 0) {
            System.out.println("--> Pfad A (Teil vorhanden): Nächstmöglicher Termin reserviert für: " + terminDatum);
        } else {
            System.out.println("--> Pfad B (Teil bestellt): Termin nach Lieferzeit + Frist reserviert für: " + terminDatum);
        }

        // --- SCHRITT 6: Angebot an Kunden senden ---
        System.out.println("\n[SCHRITT 6] Angebot wird an Kunde gesendet");
        System.out.println("    E-Mail an: " + fahrzeug.besitzer.email);
        System.out.println("    Inhalt: Reparaturangebot (" + fahrzeug.modell + "), Kosten: " + endpreis + " EUR, Termin: " + terminDatum);

        // --- SCHRITT 7: Kundenentscheidung ---
        System.out.println("\n[SCHRITT 7] Kunde prüft das Angebot");
        System.out.print("Kunde: Nimmst du das Angebot an? (j/n): ");
        String kundenAntwort = scanner.nextLine();

        if (kundenAntwort.equalsIgnoreCase("j")) {
            System.out.println("--> Pfad A: Angebot angenommen.");
            System.out.println("    Werkstattauftrag wird im System angelegt.");
            System.out.println("    E-Mail an PZ-Mitarbeiter: Auftrag für FIN " + fahrzeug.fin + " bestätigt. Termin: " + terminDatum);
        } else {
            System.out.println("--> Pfad B: Angebot abgelehnt.");
            System.out.println("    Reservierter PZ-Termin (" + terminDatum + ") wird freigegeben.");

            System.out.print("System-Frage an Kunde: Möchten Sie einen anderen Termin und einen Rückruf vom PZ? (j/n): ");
            String alternativerTermin = scanner.nextLine();

            if (alternativerTermin.equalsIgnoreCase("j")) {
                System.out.println("--> Pfad B1: E-Mail an PZ-Mitarbeiter geschickt mit Bitte um Kontaktaufnahme zwecks Terminfindung.");
            } else {
                System.out.println("--> Pfad B2: E-Mail an PZ-Mitarbeiter: Kunde hat Angebot endgültig abgelehnt.");
            }
        }

        // --- SCHRITT 8: Prozessende ---
        System.out.println("\n[SCHRITT 8] Ende des Prozesses.");
        scanner.close();
    }
}