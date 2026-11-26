-- V5 : matériel manquant (signalé par les délégués ou les agents, ou stock épuisé)
--      et matériel gâté (pannes : déclarées au retour, au scan ou depuis le catalogue, puis réparées).

CREATE TABLE manques (
    id                     BIGSERIAL PRIMARY KEY,
    designation            VARCHAR(150) NOT NULL,
    materiel_id            BIGINT REFERENCES materiel (id) ON DELETE SET NULL,
    categorie_id           BIGINT REFERENCES categories (id) ON DELETE SET NULL,
    quantite_souhaitee     INTEGER      NOT NULL DEFAULT 1 CHECK (quantite_souhaitee >= 1),
    nombre_demandes        INTEGER      NOT NULL DEFAULT 1 CHECK (nombre_demandes >= 1),
    statut                 VARCHAR(20)  NOT NULL CHECK (statut IN ('OUVERT', 'RESOLU', 'ABANDONNE')),
    origine                VARCHAR(20)  NOT NULL CHECK (origine IN ('DELEGUE', 'AGENT', 'STOCK_EPUISE', 'INDISPONIBLE')),
    date_creation          TIMESTAMP    NOT NULL,
    date_derniere_demande  TIMESTAMP    NOT NULL,
    date_resolution        TIMESTAMP,
    resolu_par             VARCHAR(255),
    note_resolution        VARCHAR(500)
);
CREATE INDEX idx_manques_statut ON manques (statut);

CREATE TABLE signalements_manque (
    id           BIGSERIAL PRIMARY KEY,
    manque_id    BIGINT       NOT NULL REFERENCES manques (id) ON DELETE CASCADE,
    delegue_id   BIGINT       REFERENCES delegues (id) ON DELETE SET NULL,
    auteur       VARCHAR(255) NOT NULL,
    salle        VARCHAR(255),
    commentaire  VARCHAR(500),
    date_signalement TIMESTAMP NOT NULL
);
CREATE INDEX idx_signalements_manque ON signalements_manque (manque_id);

CREATE TABLE pannes (
    id                 BIGSERIAL PRIMARY KEY,
    materiel_id        BIGINT       NOT NULL REFERENCES materiel (id) ON DELETE CASCADE,
    description        VARCHAR(500) NOT NULL,
    origine            VARCHAR(20)  NOT NULL CHECK (origine IN ('RETOUR', 'SCAN', 'CATALOGUE')),
    statut             VARCHAR(20)  NOT NULL CHECK (statut IN ('EN_PANNE', 'REPAREE', 'HORS_SERVICE')),
    date_declaration   TIMESTAMP    NOT NULL,
    declaree_par       VARCHAR(255) NOT NULL,
    detail_emprunt_id  BIGINT       REFERENCES details_emprunt (id) ON DELETE SET NULL,
    delegue_concerne   VARCHAR(255),
    date_reparation    TIMESTAMP,
    reparee_par        VARCHAR(255),
    note_reparation    VARCHAR(500),
    cout_reparation    INTEGER CHECK (cout_reparation IS NULL OR cout_reparation >= 0)
);
CREATE INDEX idx_pannes_materiel ON pannes (materiel_id);
CREATE INDEX idx_pannes_statut ON pannes (statut);

-- Le matériel déjà en maintenance ou hors service apparaît tout de suite dans « Matériel gâté »
INSERT INTO pannes (materiel_id, description, origine, statut, date_declaration, declaree_par)
SELECT id, 'Déjà en maintenance avant le suivi des pannes', 'CATALOGUE', 'EN_PANNE', CURRENT_TIMESTAMP, 'Système'
FROM materiel WHERE statut = 'MAINTENANCE';
INSERT INTO pannes (materiel_id, description, origine, statut, date_declaration, declaree_par)
SELECT id, 'Déjà hors service avant le suivi des pannes', 'CATALOGUE', 'HORS_SERVICE', CURRENT_TIMESTAMP, 'Système'
FROM materiel WHERE statut = 'HS';
