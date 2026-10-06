# Gestion des emprunts de matériel — Lomé Business School

Application web de suivi des emprunts de matériel (vidéoprojecteurs, câbles, rallonges, fournitures…) entre le **poste de surveillance** et les **délégués de classe**, avec QR code sur chaque matériel.

Le projet est séparé en deux parties :

```
Poejet de florice/
├─ frontend/          ← ce que voit l'utilisateur (navigateur)
│  ├─ *.html          pages : connexion, tableaux de bord, scan, catalogue…
│  ├─ js/commun.js    code partagé : menu, appels au serveur, QR codes, photos, exports
│  ├─ css/theme.css   charte graphique LBS (couleurs, polices, mode sombre)
│  └─ img/            logo de l'école
├─ backend/           ← le serveur (Java, Spring Boot)
│  ├─ pom.xml         dépendances et construction (Maven)
│  ├─ mvnw, mvnw.cmd  lanceur Maven (aucune installation de Maven nécessaire)
│  └─ src/
│     ├─ main/java/com/example/gestionmateriels/
│     │  ├─ controller/   adresses de l'API (/api/...)
│     │  ├─ service/      règles métier
│     │  ├─ repository/   accès à la base
│     │  ├─ model/        tables (entités JPA)
│     │  └─ securite/     connexion et droits
│     ├─ main/resources/
│     │  ├─ application.properties   réglages (base, adresse des QR codes…)
│     │  └─ db/migration/            scripts Flyway V1 à V5
│     └─ test/                       62 tests automatiques
├─ docs/              guides PDF (projet complet, QR codes et scanner pour débutant)
├─ lancer.cmd         démarrage en un double-clic (Windows)
└─ lancer.sh          démarrage (Linux / macOS)
```

Le **backend sert aussi le frontend** : une seule application à lancer, une seule adresse (`http://localhost:8080`). À la compilation, Maven copie le dossier `frontend/` dans l'application (il est donc inclus dans le `.jar`).

| Couche | Technologies |
|---|---|
| Frontend | HTML, JavaScript, Tailwind CSS, qrcodejs (dessin des QR), html5-qrcode (caméra), jsPDF et SheetJS (exports) |
| Backend | Java 21, Spring Boot 4 (Web, Data JPA, Security) |
| Base de données | PostgreSQL, schéma versionné avec Flyway |
| Tests | JUnit, MockMvc, base H2 en mémoire |

## Lancer le projet

### Prérequis
- Java 21
- PostgreSQL (port 5432) avec une base `gestion_materiels_db` :
  ```sql
  CREATE DATABASE gestion_materiels_db;
  ```
  Par défaut : utilisateur `postgres` / mot de passe `postgres`. Sinon, définir `DB_URL`, `DB_USER`, `DB_PASSWORD`.

### Démarrer
- **Windows** : double-cliquer sur `lancer.cmd`, ou dans un terminal :
  ```
  cd backend
  .\mvnw.cmd spring-boot:run
  ```
- **Linux / macOS** : `./lancer.sh`

Puis ouvrir **http://localhost:8080**. Au démarrage, Flyway crée ou met à jour les tables et des données de démonstration sont ajoutées si la base est vide.

Lancé depuis le dossier `backend`, le serveur lit les pages directement dans `../frontend` : une modification d'un fichier HTML, JS ou CSS se voit en rechargeant la page, sans redémarrer.

### QR codes et téléphones
Les QR codes contiennent l'adresse du serveur. Pour que les téléphones puissent l'ouvrir, donner l'adresse du PC sur le réseau (commande `ipconfig`) :
```
set APP_URL=http://192.168.1.20:8080
lancer.cmd
```
ou écrire `app.url-publique=http://192.168.1.20:8080` dans `backend/src/main/resources/application.properties`.

### Comptes de démonstration
| Profil | Identifiant | Mot de passe |
|---|---|---|
| Surveillant général, administrateur | `mdaniel` | `daniel2026` |
| Surveillant | `mguillaume` | `guillaume2026` |
| Délégué | `pkodjo` | `pascal2026` |

## Tests
```
cd backend
.\mvnw.cmd test
```

## Fonctionnalités
- **QR code sur chaque matériel** : le délégué scanne pour récupérer, rendre ou demander à celui qui l'a de le lui passer (transfert avec notification) ; parcours complet de chaque matériel.
- Demandes, **réservations à l'avance** avec créneaux, **bon d'emprunt** imprimable, **notifications**.
- **Tableaux de bord** administrateur, surveillant et délégué ; **écran du poste** (kiosque).
- **Photos** (6 par matériel), **photo au retour** comme preuve.
- **Rappel WhatsApp**, **score de confiance** des délégués, **QR code des portes de salle**, **heures chargées**.
- **Matériel manquant** (signalé par les délégués, classé par nombre de demandes) et **matériel gâté** (pannes, réparation, hors service).
- **Imprimer, PDF et Excel** sur les listes principales.
- Sécurité : session, mots de passe BCrypt, CSRF, droits par rôle (`DELEGUE`, `AGENT`, `ADMIN`).

Le détail de chaque partie, avec les fichiers et les numéros de ligne, est dans les guides du dossier `docs/`.
