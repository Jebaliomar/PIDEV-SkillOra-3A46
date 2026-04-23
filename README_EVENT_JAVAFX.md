# Module Event JavaFX (CRUD)

Ce module remplace le flux console `EventMain` par une interface JavaFX style front.

## Fonctionnalites

- Liste des events en cards
- Recherche par titre
- Filtre par type
- CRUD event complet
- Ecran details event
- Formulaire create/edit avec gestion de la salle liee

## Fichiers principaux

- `src/main/java/tn/esprit/mains/MainFx.java`
- `src/main/java/tn/esprit/controlles/EventDashboardController.java`
- `src/main/resources/views/event/EventDashboard.fxml`
- `src/main/resources/styles/event.css`
- `src/main/java/tn/esprit/services/EventService.java`

## Lancer l'application

Prerequis:

- Java 17
- Base de donnees configuree via `src/main/resources/db.properties`

Commandes:

```powershell
mvn clean compile
mvn javafx:run
```

