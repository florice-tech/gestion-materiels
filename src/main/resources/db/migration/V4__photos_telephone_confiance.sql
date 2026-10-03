-- V4 : photos du matériel (6 au plus) et photo jointe à un retour,
--      numéro WhatsApp du délégué, déblocage des réservations par un agent.

ALTER TABLE delegues ADD COLUMN telephone VARCHAR(30);
ALTER TABLE delegues ADD COLUMN reservations_debloquees_le TIMESTAMP;

CREATE TABLE photos (
    id                 BIGSERIAL PRIMARY KEY,
    materiel_id        BIGINT REFERENCES materiel (id) ON DELETE CASCADE,
    detail_emprunt_id  BIGINT REFERENCES details_emprunt (id) ON DELETE CASCADE,
    type_mime          VARCHAR(40)  NOT NULL,
    taille             INTEGER      NOT NULL,
    contenu            BYTEA        NOT NULL,
    date_ajout         TIMESTAMP    NOT NULL,
    ajoutee_par        VARCHAR(255),
    CONSTRAINT ck_photos_rattachement CHECK (materiel_id IS NOT NULL OR detail_emprunt_id IS NOT NULL)
);

CREATE INDEX idx_photos_materiel ON photos (materiel_id);
CREATE INDEX idx_photos_detail ON photos (detail_emprunt_id);
