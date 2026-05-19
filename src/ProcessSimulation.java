import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Scanner;

public class ProcessSimulation {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Standard-Dienstleistungskatalog für den PZ-Mitarbeiter
        ServiceDienstleistung[] katalog = {
                new ServiceDienstleistung("Standard Montage & Kalibrierung", 450.00),
                new ServiceDienstleistung("Große Inspektion inkl. Zusatzarbeiten", 950.00)
        };

        // --- SCHRITT 1: Trigger & Datenabruf aus fahrzeuge.csv ---
        System.out.println(">>> Porsche Predictive Maintenance System aktiv.");
        System.out.print("Verschleiß-Event empfangen. Bitte FIN eingeben: ");
        String eingegebeneFin = scanner.nextLine().trim();

        String csvFileFahrzeuge = "fahrzeuge.csv";
        String csvFileLager = "pz_lager.csv";
        String separator = ";";

        Fahrzeug fahrzeug = null;
        String defektesTeilId = "";
        String line;

        try (BufferedReader br = new BufferedReader(new java.io.InputStreamReader(
                new java.io.FileInputStream(csvFileFahrzeuge), java.nio.charset.Charset.defaultCharset()))) {
            br.readLine(); // Header-Zeile überspringen
            while ((line = br.readLine()) != null) {
                String[] columns = line.split(separator);
                if (columns.length >= 7) {
                    String fin = columns[0].trim();
                    if (fin.equalsIgnoreCase(eingegebeneFin)) {
                        String modell = columns[1].trim();
                        String kundeName = columns[2].trim();
                        String email = columns[3].trim();
                        boolean hatWerksvertrag = Boolean.parseBoolean(columns[4].trim());
                        String pzId = columns[5].trim();
                        defektesTeilId = columns[6].trim();

                        Kunde kunde = new Kunde("K-" + fin.substring(Math.max(0, fin.length() - 3)), kundeName, email, hatWerksvertrag);
                        fahrzeug = new Fahrzeug(fin, modell, kunde, pzId);
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Kritischer Fehler beim Lesen von fahrzeuge.csv: " + e.getMessage());
            return;
        }

        if (fahrzeug == null) {
            System.out.println("Abbruch: Fahrzeug mit der FIN '" + eingegebeneFin + "' existiert nicht in der Datenbank.");
            return;
        }

        System.out.println("\n[SCHRITT 1] Fahrzeug- & Kundenbeziehung identifiziert:");
        System.out.println("    Fahrzeug: " + fahrzeug.modell + " | Zuständiges PZ: " + fahrzeug.pzId);
        System.out.println("    Halter: " + fahrzeug.besitzer.name + " | Werksvertrag: " + (fahrzeug.besitzer.hatWerksvertrag ? "Ja" : "Nein"));

        // --- SCHRITT 2: Automatische Bestandsprüfung aus pz_lager.csv ---
        System.out.println("\n[SCHRITT 2] Führe automatisierten Lager-Check im " + fahrzeug.pzId + " aus...");

        Ersatzteil teil = null;
        boolean teilGefunden = false;

        try (BufferedReader br = new BufferedReader(new java.io.InputStreamReader(
                new java.io.FileInputStream(csvFileLager), java.nio.charset.Charset.defaultCharset()))) {
            br.readLine(); // Header-Zeile überspringen
            while ((line = br.readLine()) != null) {
                String[] columns = line.split(separator);
                if (columns.length >= 6) {
                    String pzId = columns[0].trim();
                    String teilId = columns[1].trim();

                    if (pzId.equalsIgnoreCase(fahrzeug.pzId) && teilId.equalsIgnoreCase(defektesTeilId)) {
                        String teilName = columns[2].trim();
                        String preisStr = columns[3].trim();
                        int bestand = Integer.parseInt(columns[4].trim());
                        int lieferzeit = Integer.parseInt(columns[5].trim());

                        // Datenbereinigung für Euro-Format
                        preisStr = preisStr.replace("€", "").replace(".", "").replace(",", ".").trim();
                        double preis = Double.parseDouble(preisStr);

                        if (bestand > 0) {
                            System.out.println("--> Pfad A: Ersatzteil '" + teilName + "' vorrätig (Bestand: " + bestand + "). Komponente wurde blockiert.");
                            teil = new Ersatzteil(teilId, teilName, preis, 0);
                        } else {
                            System.out.println("--> Pfad B: Ersatzteil '" + teilName + "' nicht vorrätig. Automatische Nachbestellung an Logistikzentrum übermittelt.");
                            System.out.println("    Errechnete Lieferzeit laut Lieferant: " + lieferzeit + " Tage.");
                            teil = new Ersatzteil(teilId, teilName, preis, lieferzeit);
                        }
                        teilGefunden = true;
                        break;
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Kritischer Fehler beim Parsen von pz_lager.csv: " + e.getMessage());
            return;
        }

        if (!teilGefunden) {
            System.out.println("Abbruch: Ersatzteil-ID '" + defektesTeilId + "' ist für das " + fahrzeug.pzId + " nicht gelistet.");
            return;
        }

        // --- SCHRITT 3: Service-Katalog & Mitarbeiter-Input ---
        System.out.println("\n[SCHRITT 3] Datenbereitstellung an Serviceberater");
        System.out.println("Verfügbare Dienstleistungen für " + fahrzeug.modell + ":");
        for (int i = 0; i < katalog.length; i++) {
            System.out.println("    " + (i + 1) + " - " + katalog[i].name + " (" + katalog[i].kosten + " EUR)");
        }
        System.out.print("PZ-Mitarbeiter: Bitte Service-Nummer wählen (1 oder 2): ");
        int serviceWahl = Integer.parseInt(scanner.nextLine()) - 1;
        ServiceDienstleistung gewaehlterService = katalog[serviceWahl];

        double basisGesamtkosten = teil.preis + gewaehlterService.kosten;
        System.out.println("--> Vorkalkulation: Teile (" + teil.preis + " EUR) + Service (" + gewaehlterService.kosten + " EUR) = " + basisGesamtkosten + " EUR.");

        // --- SCHRITT 4: Rabattprüfung ---
        System.out.println("\n[SCHRITT 4] Compliance- & Vertragsprüfung");
        double endpreis = basisGesamtkosten;
        if (fahrzeug.besitzer.hatWerksvertrag) {
            System.out.println("--> Pfad A: Aktiver Werksvertrag erkannt. 15% Smart-Repair-Rabatt angewendet.");
            endpreis = basisGesamtkosten * 0.85;
        } else {
            System.out.println("--> Pfad B: Kein Werksvertrag hinterlegt. Standardkonditionen angewendet.");
        }
        System.out.printf("    Finaler Angebotspreis: %.2f EUR\n", endpreis);

        // --- SCHRITT 5: Terminberechnung ---
        System.out.println("\n[SCHRITT 5] Kapazitäts- & Logistik-Terminierung");
        int tageBisTermin = Math.max(14, teil.lieferzeitTage) + 3;
        LocalDate terminDatum = LocalDate.now().plusDays(tageBisTermin);

        if (teil.lieferzeitTage == 0) {
            System.out.println("--> Pfad A (Lagernd): Frühestmöglicher Fixtermin reserviert für: " + terminDatum + " (14 Tage Mindestvorlauf + 3 Tage Puffer)");
        } else {
            System.out.println("--> Pfad B (Zulauf): Termin nach Wareneingang reserviert für: " + terminDatum + " (Lieferzeit " + teil.lieferzeitTage + " Tage + 3 Tage Puffer)");
        }

        // --- SCHRITT 6: Angebot per E-Mail ---
        System.out.println("\n[SCHRITT 6] Automatisierter Angebotsversand");
        System.out.println("    Empfänger: " + fahrzeug.besitzer.email);
        System.out.printf("    Inhalt: Predictive Maintenance - %s | Kosten: %.2f EUR | Reservierter Slot: %s\n", fahrzeug.modell, endpreis, terminDatum);

        // --- SCHRITT 7: Interaktive Kundenentscheidung ---
        System.out.println("\n[SCHRITT 7] Kunden-Gateway (Simulation)");
        System.out.print("Kunde: Angebot annehmen? (j/n): ");
        String kundenAntwort = scanner.nextLine();

        if (kundenAntwort.equalsIgnoreCase("j")) {
            System.out.println("--> Pfad A: Angebot digital signiert.");
            System.out.println("    Werkstattauftrag erfolgreich im ERP-System generiert.");
            System.out.println("    Meldung an " + fahrzeug.pzId + ": Bereite Werkstatt-Slot für FIN " + fahrzeug.fin + " am " + terminDatum + " vor.");
        } else {
            System.out.println("--> Pfad B: Angebot abgelehnt.");
            System.out.println("    Geblockte Ressourcen und Werkstatt-Slot (" + terminDatum + ") freigegeben.");

            System.out.print("System-Frage an Kunde: Wünschen Sie eine manuelle Kontaktaufnahme zwecks Terminänderung? (j/n): ");
            String alternativerTermin = scanner.nextLine();

            if (alternativerTermin.equalsIgnoreCase("j")) {
                System.out.println("--> Pfad B1: Ticket an Serviceberater eskaliert. Rückrufwunsch im CRM hinterlegt.");
            } else {
                System.out.println("--> Pfad B2: Prozess geschlossen. Kundenablehnung im System protokolliert.");
            }
        }

        // --- SCHRITT 8: Prozessende ---
        System.out.println("\n[SCHRITT 8] Prozess erfolgreich beendet.");
        scanner.close();
    }
}