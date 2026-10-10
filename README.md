# Whats Ubb

Android-app (Kotlin + Jetpack Compose) voor afdeling UBB2.

- **Bewoners** stellen anoniem vragen over de afspraken en regels van de afdeling.
- **Begeleiders** (met PIN) maken met AI werkplannen, dagrapportages, gespreksverslagen,
  incidentmeldingen en signaleringsplannen (te downloaden als PDF), en kunnen vragen stellen over afspraken en doelgroep.
  Bij een gespreksverslag kan het gesprek worden opgenomen (AAC) en door Gemini worden uitgeschreven.
- **Beheerder** (aparte beheerders-PIN, via Begeleider → Beheer) beheert de kennisbank, de AI-sleutel en de codes,
  en kan documenten met regels, afspraken en werkwijze uploaden (PDF, Word .docx, tekst).
- Antwoorden komen van Google Gemini, gevoed met de kennisbank van de afdeling.

## Privacy

- Tekst wordt vóór verzending geanonimiseerd (telefoon, e-mail, datum, adres, postcode, BSN, IBAN en opgegeven namen).
- Gesprekken en documenten worden niet opgeslagen. Back-ups staan uit.
- Dit is een vangnet, geen garantie: schrijf altijd zonder namen. Gebruik voor echt gebruik een **betaalde** Gemini API
  (bij de gratis versie mag Google ingestuurde tekst gebruiken) en stem het gebruik af met de privacyfunctionaris.

## Kennisbank

De kennis van de afdeling staat in [`app/src/main/assets/ubb2_kennis.md`](app/src/main/assets/ubb2_kennis.md).
De beheerder kan die ook in de app aanpassen via Beheer. Zet hier nooit gegevens van bewoners in.

Geüploade documenten worden eenmalig omgezet naar tekst (Word en tekst op het toestel, PDF via Gemini) en lokaal
bewaard. Die tekst gaat bij elke vraag mee als kennis. Let op: alles staat per toestel; op een ander toestel moet je
de documenten opnieuw toevoegen.

## API-sleutel

Maak een sleutel op <https://aistudio.google.com/apikey> en kies één van beide:

1. In de app: Begeleider → Beheer → API-sleutel, of
2. Bij het bouwen: voeg `GEMINI_API_KEY=...` toe aan `local.properties` (staat in `.gitignore`).

Een ingebouwde sleutel kan uit de APK worden gehaald: deel zo'n APK niet.

## Bouwen

Open het project in Android Studio, of:

```
./gradlew assembleDebug
```

Het originele logo staat in `design/`.
