# Whats Ubb

Android-app (Kotlin + Jetpack Compose) voor afdeling UBB2.

- **Bewoners** stellen anoniem vragen over de afspraken en regels van de afdeling.
- **Begeleiders** (met PIN) maken met AI werkplannen, dagrapportages, gespreksverslagen,
  incidentmeldingen en signaleringsplannen, en kunnen vragen stellen over afspraken en doelgroep.
- Antwoorden komen van Google Gemini, gevoed met de kennisbank van de afdeling.

## Privacy

- Tekst wordt vóór verzending geanonimiseerd (telefoon, e-mail, datum, adres, postcode, BSN, IBAN en opgegeven namen).
- Gesprekken en documenten worden niet opgeslagen. Back-ups staan uit.
- Dit is een vangnet, geen garantie: schrijf altijd zonder namen. Gebruik voor echt gebruik een **betaalde** Gemini API
  (bij de gratis versie mag Google ingestuurde tekst gebruiken) en stem het gebruik af met de privacyfunctionaris.

## Kennisbank

De kennis van de afdeling staat in [`app/src/main/assets/ubb2_kennis.md`](app/src/main/assets/ubb2_kennis.md).
Begeleiders kunnen die ook in de app aanpassen via Instellingen. Zet hier nooit gegevens van bewoners in.

## API-sleutel

Maak een sleutel op <https://aistudio.google.com/apikey> en kies één van beide:

1. In de app: Begeleider → Instellingen → API-sleutel, of
2. Bij het bouwen: voeg `GEMINI_API_KEY=...` toe aan `local.properties` (staat in `.gitignore`).

Een ingebouwde sleutel kan uit de APK worden gehaald: deel zo'n APK niet.

## Bouwen

Open het project in Android Studio, of:

```
./gradlew assembleDebug
```

Het originele logo staat in `design/`.
