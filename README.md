# Gestion des emprunts de matériel

Application web de suivi des emprunts de matériel pédagogique (vidéoprojecteurs, micros, câbles, marqueurs…) entre le **poste de surveillance** et les **délégués de classe**.

- **Back-end** : Spring Boot 4 (Java 21), Spring Data JPA, PostgreSQL
- **Front-end** : pages HTML statiques + Tailwind (CDN) + JavaScript, servies par Spring Boot

## Fonctionnement

1. Le **délégué** crée son compte, se connecte et envoie une **demande d'emprunt** (salle, heure de retour prévue, matériel). Le matériel est réservé immédiatement.
2. L'**agent** voit la demande dans *Demandes en attente*, remet le matériel et **valide** (ou **refuse** : le matériel est libéré). Le délégué peut aussi **annuler** sa demande tant qu'elle n'est pas validée.
3. Au retour, l'agent déclare l'**état de chaque article durable** (bon état, à vérifier, endommagé, vide/épuisé). Le statut du matériel est mis à jour automatiquement (disponible, à vérifier, maintenance, hors service). Les consommables ne reviennent pas en stock.
4. L'agent gère le **catalogue** (ajout/suppression, catégories, changement de statut, réapprovisionnement des consommables) et consulte l'**historique** complet avec filtres. Les emprunts en retard sont signalés en rouge.

## Lancer le projet

### Prérequis
- Java 21
- PostgreSQL (port 5432)

### 1. Créer la base
```sql
CREATE DATABASE gestion_materiels_db;
```
Par défaut l'application se connecte avec `postgres` / `postgres`. Pour d'autres identifiants, définir les variables d'environnement `DB_URL`, `DB_USER`, `DB_PASSWORD` (ou modifier `src/main/resources/application.properties`).

Les tables sont créées automatiquement au premier démarrage (`ddl-auto=update`), et des données de démonstration sont insérées si la base est vide.

### 2. Démarrer
```bash
# Windows
mvnw.cmd spring-boot:run
# Linux / macOS
./mvnw spring-boot:run
```
Puis ouvrir **http://localhost:8080** (redirige vers la page de connexion).

### Comptes de démonstration

| Profil   | Identifiant  | Mot de passe    |
|----------|--------------|-----------------|
| Agent (administrateur) | `mdaniel`    | `daniel2026`    |
| Agent (surveillant)    | `mguillaume` | `guillaume2026` |
| Délégué  | `pkodjo`     | `pascal2026`    |

## Tests
```bash
mvnw.cmd test      # ou ./mvnw test
```
Les tests utilisent une base **H2 en mémoire** : PostgreSQL n'est pas nécessaire. Ils couvrent le cycle demande → validation → retour, le refus/annulation d'une demande, le contrôle du stock et de la disponibilité.

## API REST (résumé)

| Méthode | Route | Rôle |
|---|---|---|
| POST | `/api/auth/agent/login` | Connexion agent |
| POST | `/api/auth/delegue/login` | Connexion délégué |
| POST | `/api/auth/delegue/inscription` | Création d'un compte délégué |
| GET | `/api/materiels` | Catalogue |
| POST | `/api/materiels` | Ajouter un matériel |
| DELETE | `/api/materiels/{id}` | Supprimer (si jamais emprunté) |
| PATCH | `/api/materiels/{id}/statut` | Changer le statut (`{"statut":"DISPONIBLE"}`) |
| PATCH | `/api/materiels/{id}/stock` | Réapprovisionner un consommable (`{"quantite":10}`) |
| GET / POST / DELETE | `/api/categories` | Gestion des catégories |
| GET | `/api/agents` | Liste des agents |
| POST | `/api/emprunts/demande` | Demande d'emprunt (délégué) |
| POST | `/api/emprunts/valider` | Valider une demande (agent) |
| POST | `/api/emprunts/{id}/refuser` | Refuser une demande en attente (agent) |
| POST | `/api/emprunts/{id}/annuler` | Annuler sa demande (`{"delegueId":1}`) |
| POST | `/api/emprunts/retour` | Enregistrer un retour, avec un état par article durable |
| GET | `/api/emprunts/en-attente` | Demandes à valider |
| GET | `/api/emprunts/actifs` | Emprunts validés non rendus |
| GET | `/api/emprunts/historique` | Historique complet |
| GET | `/api/emprunts/delegue/{id}` | Emprunts d'un délégué |

Toutes les réponses d'action ont la forme `{"success": true|false, "message": "..."}`.

## Limites connues
- Les mots de passe sont stockés en clair et la session est gérée côté navigateur (`localStorage`) : l'API n'est pas protégée côté serveur. Suffisant pour une démonstration, mais à renforcer (Spring Security + mots de passe hachés BCrypt) avant une vraie mise en service.
