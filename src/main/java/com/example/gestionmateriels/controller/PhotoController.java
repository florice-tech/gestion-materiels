package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.model.Photo;
import com.example.gestionmateriels.securite.UtilisateurConnecte;
import com.example.gestionmateriels.service.PhotoService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Photos du matériel (6 au plus) et photos jointes aux retours. */
@RestController
@RequestMapping("/api")
public class PhotoController {

    public record PhotoRequest(String donnees) {
    }

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @GetMapping("/photos/{id}")
    public ResponseEntity<byte[]> image(@PathVariable Long id) {
        Photo photo = photoService.lire(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getTypeMime()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate())
                .body(photo.getContenu());
    }

    /** materielId → identifiants de ses photos. */
    @GetMapping("/photos/materiels")
    public Map<Long, List<Long>> parMateriel() {
        return photoService.idsParMateriel();
    }

    @GetMapping("/materiels/{id}/photos")
    public List<Long> duMateriel(@PathVariable Long id) {
        return photoService.idsDuMateriel(id);
    }

    @PostMapping("/materiels/{id}/photos")
    public ResponseEntity<Map<String, Object>> ajouter(@AuthenticationPrincipal UtilisateurConnecte moi,
                                                       @PathVariable Long id, @RequestBody PhotoRequest requete) {
        Photo photo = photoService.ajouterAuMateriel(id, requete.donnees(), moi.getNom());
        return Reponses.ok("Photo ajoutée.", "photoId", photo.getId());
    }

    @DeleteMapping("/photos/{id}")
    public ResponseEntity<Map<String, Object>> supprimer(@PathVariable Long id) {
        photoService.supprimer(id);
        return Reponses.ok("Photo supprimée.");
    }
}
