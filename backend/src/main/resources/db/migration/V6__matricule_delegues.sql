-- V6 : matricule des délégués (numéro d'étudiant), unique quand il est renseigné.
ALTER TABLE delegues ADD COLUMN matricule VARCHAR(50);
ALTER TABLE delegues ADD CONSTRAINT uq_delegues_matricule UNIQUE (matricule);

-- Verrou optimiste sur le matériel (évite qu'un même matériel soit remis à deux personnes en même temps)
ALTER TABLE materiel ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
