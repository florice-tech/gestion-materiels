package com.example.gestionmateriels.service;

import com.example.gestionmateriels.model.DetailEmprunt;
import com.example.gestionmateriels.model.Materiel;
import com.example.gestionmateriels.model.Photo;
import com.example.gestionmateriels.repository.MaterielRepository;
import com.example.gestionmateriels.repository.PhotoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Photos : jusqu'à 6 par matériel (ajoutées par un agent) et une photo jointe au retour
 * d'un matériel abîmé. Les images arrivent du navigateur déjà réduites, en « data URL ».
 */
@Service
@Transactional
public class PhotoService {

    public static final int PHOTOS_MAX_PAR_MATERIEL = 6;
    static final int TAILLE_MAX_OCTETS = 2_500_000;

    private static final Pattern DATA_URL = Pattern.compile("^data:(image/(?:jpeg|png|webp));base64,([A-Za-z0-9+/=\\s]+)$");

    private final PhotoRepository photoRepository;
    private final MaterielRepository materielRepository;

    public PhotoService(PhotoRepository photoRepository, MaterielRepository materielRepository) {
        this.photoRepository = photoRepository;
        this.materielRepository = materielRepository;
    }

    public Photo ajouterAuMateriel(Long materielId, String dataUrl, String auteur) {
        Materiel materiel = materielRepository.findById(materielId)
                .orElseThrow(() -> OperationException.introuvable("Matériel introuvable (id=" + materielId + ")."));
        if (photoRepository.countByMaterielId(materielId) >= PHOTOS_MAX_PAR_MATERIEL) {
            throw new OperationException("Ce matériel a déjà " + PHOTOS_MAX_PAR_MATERIEL
                    + " photos : supprimez-en une avant d'en ajouter une autre.");
        }
        Photo photo = decoder(dataUrl, auteur);
        photo.setMateriel(materiel);
        return photoRepository.save(photo);
    }

    /** Photo prise par le délégué en rendant le matériel (preuve de l'état). */
    public Photo joindreAuRetour(DetailEmprunt ligne, String dataUrl, String auteur) {
        Photo photo = decoder(dataUrl, auteur);
        photo.setDetailEmprunt(ligne);
        return photoRepository.save(photo);
    }

    public void supprimer(Long id) {
        Photo photo = lire(id);
        if (photo.getMateriel() == null) {
            throw new OperationException("Une photo de retour sert de preuve : elle ne peut pas être supprimée.");
        }
        photoRepository.delete(photo);
    }

    @Transactional(readOnly = true)
    public Photo lire(Long id) {
        return photoRepository.findById(id).orElseThrow(() -> OperationException.introuvable("Photo introuvable."));
    }

    @Transactional(readOnly = true)
    public List<Long> idsDuMateriel(Long materielId) {
        return photoRepository.idsDuMateriel(materielId);
    }

    /** materielId → identifiants de ses photos, pour afficher le catalogue sans charger les images. */
    @Transactional(readOnly = true)
    public Map<Long, List<Long>> idsParMateriel() {
        Map<Long, List<Long>> resultat = new LinkedHashMap<>();
        for (Object[] ligne : photoRepository.idsParMateriel()) {
            resultat.computeIfAbsent((Long) ligne[0], k -> new ArrayList<>()).add((Long) ligne[1]);
        }
        return resultat;
    }

    static Photo decoder(String dataUrl, String auteur) {
        if (dataUrl == null || dataUrl.isBlank()) {
            throw new OperationException("Aucune photo reçue.");
        }
        Matcher m = DATA_URL.matcher(dataUrl.trim());
        if (!m.matches()) {
            throw new OperationException("Format de photo non accepté : utilisez une image JPEG, PNG ou WebP.");
        }
        byte[] octets;
        try {
            octets = Base64.getMimeDecoder().decode(m.group(2));
        } catch (IllegalArgumentException e) {
            throw new OperationException("La photo est illisible.");
        }
        if (octets.length == 0) {
            throw new OperationException("La photo est vide.");
        }
        if (octets.length > TAILLE_MAX_OCTETS) {
            throw new OperationException("La photo est trop lourde (2,5 Mo au plus).");
        }
        if (!signatureImage(octets)) {
            throw new OperationException("Le fichier envoyé n'est pas une image.");
        }
        return new Photo(m.group(1), octets, auteur);
    }

    /** Vérifie les premiers octets : JPEG (FF D8), PNG (89 50 4E 47) ou WebP (RIFF....WEBP). */
    private static boolean signatureImage(byte[] o) {
        if (o.length >= 3 && (o[0] & 0xFF) == 0xFF && (o[1] & 0xFF) == 0xD8) return true;
        if (o.length >= 4 && (o[0] & 0xFF) == 0x89 && o[1] == 'P' && o[2] == 'N' && o[3] == 'G') return true;
        return o.length >= 12 && o[0] == 'R' && o[1] == 'I' && o[2] == 'F' && o[3] == 'F'
                && o[8] == 'W' && o[9] == 'E' && o[10] == 'B' && o[11] == 'P';
    }
}
