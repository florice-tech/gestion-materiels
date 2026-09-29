# Gestion des emprunts de matériel

Application web de suivi des emprunts de matériel pédagogique (vidéoprojecteurs, micros, câbles, marqueurs…) entre le **poste de surveillance** et les **délégués de classe**.

| Couche | Technologies |
|---|---|
| Back-end | Spring Boot 4 (Java 21), Spring Web MVC, Spring Data JPA, **Spring Security** |
| Base de données | PostgreSQL, schéma versionné avec **Flyway** |
| Front-end | Pages HTML + Tailwind CSS (CDN) + JavaScript, servies par Spring Boot |
| Tests | JUnit 5, MockMvc, base H2 en mémoire |

## Fonctionnalités

**Délégué**
- crée son compte, se connecte ;
- fait une **demande** : salle (liste gérée par les agents), heure de retour, matériel (le matériel est réservé immédiatement) ;
- suit ses demandes (en attente, en cours, en retard, rendue, refusée avec motif, annulée) et peut **annuler** une demande non validée ;
- change son mot de passe.

**Agent de surveillance**
- **tableau de bord** : demandes à traiter, emprunts en cours, retards, sorties/retours du jour, état du parc, stocks bas, matériel le plus emprunté, activité sur 7 jours ;
- **valide** (remise du matériel) ou **refuse** une demande avec un motif ;
- enregistre le **retour** en déclarant l'état de chaque article durable : bon état → disponible, à vérifier, endommagé → maintenance, vide/épuisé → hors service ;
- gère le **catalogue** : ajout, modification, suppression, changement de statut, réapprovisionnement, seuil d'alerte de stock ;
- gère les **salles** et **catégories** ;
- consulte l'**historique** complet (filtres par texte, statut, période) et l'**exporte en CSV** pour Excel.

**Administrateur** (agent avec droit d'administration)
- crée, modifie, désactive/réactive les comptes agents, réinitialise les mots de passe ;
- désactive/réactive les délégués, réinitialise leurs mots de passe.

## Sécurité
- Connexion par **session serveur** (cookie `JSESSIONID`), mots de passe **hachés avec BCrypt**.
- Droits par rôle sur toute l'API : `DELEGUE`, `AGENT`, `ADMIN`. L'agent ou le délégué d'une action est toujours celui de la session, jamais un identifiant envoyé par le navigateur.
- Protection **CSRF** : jeton dans le cookie `XSRF-TOKEN`, renvoyé par les pages dans l'en-tête `X-XSRF-TOKEN`.
- Mots de passe jamais renvoyés par l'API ; texte affiché échappé contre l'injection HTML.
- Une base créée par une ancienne version (mots de passe en clair) est convertie automatiquement en BCrypt au démarrage.

## Lancer le projet

### Prérequis
- Java 21
- PostgreSQL (port 5432)

### 1. Créer la base
```sql
CREATE DATABASE gestion_materiels_db;
```
Par défaut l'application se connecte avec `postgres` / `postgres`. Pour d'autres identifiants, définir les variables d'environnement `DB_URL`, `DB_USER`, `DB_PASSWORD`.

Au démarrage, **Flyway crée ou met à jour les tables** (scripts dans `src/main/resources/db/migration`), puis des données de démonstration sont ajoutées si les tables sont vides. Une base déjà créée par l'ancienne version du projet est reprise sans perte de données.

### 2. Démarrer
```bash
# Windows
mvnw.cmd spring-boot:run
# Linux / macOS
./mvnw spring-boot:run
```
Puis ouvrir **http://localhost:8080**.

### Comptes de démonstration

| Profil | Identifiant | Mot de passe |
|---|---|---|
| Agent administrateur | `mdaniel` | `daniel2026` |
| Agent | `mguillaume` | `guillaume2026` |
| Délégué | `pkodjo` | `pascal2026` |

Pensez à changer ces mots de passe (page **Mon compte**) avant une vraie utilisation.

## Tests
```bash
mvnw.cmd test      # ou ./mvnw test
```
Les tests utilisent H2 en mémoire (PostgreSQL non nécessaire). Ils couvrent le cycle demande → validation → retour, refus et annulation, stock et disponibilité, retards et tableau de bord, comptes et mots de passe, ainsi que la sécurité de l'API (connexion, rôles, CSRF, format des erreurs).

## Base de données

```
agents ─┐                       ┌─ categories
        │ agent_sortie_id       │ categorie_id
        │ agent_retour_id       │
        │ agent_refus_id   materiel ◄── details_emprunt ──► emprunts ◄── delegues
        └────────────────────────────────────────────────────┘ delegue_id
salles (liste proposée dans les demandes ; l'emprunt garde le nom de la salle en texte)
```

| Table | Rôle |
|---|---|
| `agents` | agents de surveillance (`administrateur`, `actif`) |
| `delegues` | délégués (`actif`, `date_creation`) |
| `categories`, `salles` | listes de référence |
| `materiel` | catalogue : durable (code unique, statut) ou consommable (stock, seuil d'alerte) |
| `emprunts` | fiches : statut `EN_ATTENTE` → `EN_COURS` → `RETOURNE`, ou `REFUSEE` / `ANNULEE` ; dates de demande, sortie, retour |
| `details_emprunt` | lignes d'une fiche : matériel, quantité, état au retour |

Migrations : `V1__schema_initial.sql` (schéma d'origine), `V2__comptes_salles_historique.sql` (comptes, salles, historique complet, contraintes et index).

## API REST

Toutes les réponses d'action ont la forme `{"success": true|false, "message": "..."}`.

| Méthode | Route | Accès |
|---|---|---|
| POST | `/api/auth/login` · `/api/auth/inscription` | public |
| GET | `/api/auth/moi` | connecté |
| POST | `/api/auth/logout` · `/api/auth/mot-de-passe` | connecté |
| GET | `/api/materiels` · `/api/categories` · `/api/salles` | connecté |
| POST / PUT / DELETE | `/api/materiels[/{id}]` · `/api/categories[/{id}]` · `/api/salles[/{id}]` | agent |
| PATCH | `/api/materiels/{id}/statut` · `/api/materiels/{id}/stock` | agent |
| POST | `/api/emprunts/demande` · `/api/emprunts/{id}/annuler` | délégué |
| GET | `/api/emprunts/mes-emprunts` | délégué |
| GET | `/api/emprunts/en-attente` · `/en-cours` · `/historique` · `/export` | agent |
| POST | `/api/emprunts/{id}/valider` · `/{id}/refuser` · `/{id}/retour` | agent |
| GET | `/api/statistiques` | agent |
| GET / POST / PUT / PATCH | `/api/admin/agents…` · `/api/admin/delegues…` | administrateur |
