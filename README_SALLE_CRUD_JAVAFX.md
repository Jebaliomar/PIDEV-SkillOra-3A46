# Salle CRUD JavaFX

Ce module ajoute uniquement le CRUD Salle dans le style admin existant.

## Fichiers ajoutes

- `src/main/resources/views/event/SalleDashboard.fxml`
- `src/main/resources/styles/salle.css`
- `src/main/java/tn/esprit/controlles/SalleDashboardController.java`
- `src/main/java/tn/esprit/mains/SalleMainFx.java`

## Fichiers modifies

- `src/main/resources/views/event/EventDashboard.fxml` (bouton `Salles`)
- `src/main/java/tn/esprit/controlles/EventDashboardController.java` (navigation `onOpenSalles`)

## CRUD Salle inclus

- Create: bouton `New Salle` + formulaire
- Read/List: tableau des salles
- Update: bouton `Edit` (selection requise)
- Delete: bouton `Delete` + confirmation

## Lancer dans IntelliJ

1. Ouvrir la classe `tn.esprit.mains.MainFx` pour tester la navigation depuis `Events` vers `Salles`.
2. Ou lancer directement `tn.esprit.mains.SalleMainFx` pour le module Salle uniquement.

## Maven (optionnel)

```powershell
cd C:\Users\jebal\Desktop\pidev\Skillora
mvn clean compile
```

