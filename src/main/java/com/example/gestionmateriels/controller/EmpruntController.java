package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.dto.DemandeEmpruntRequest;
import com.example.gestionmateriels.dto.RefusRequest;
import com.example.gestionmateriels.dto.RetourRequest;
import com.example.gestionmateriels.model.Emprunt;
import com.example.gestionmateriels.securite.UtilisateurConnecte;
import com.example.gestionmateriels.service.EmpruntService;
import com.example.gestionmateriels.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

/**
 * Emprunts. Le délégué ou l'agent concerné est toujours celui de la session :
 * on ne fait jamais confiance à un identifiant envoyé par le navigateur.
 */
@RestController
@RequestMapping("/api/emprunts")
public class EmpruntController {

    private final EmpruntService empruntService;
    private final ExportService exportService;

    public EmpruntController(EmpruntService empruntService, ExportService exportService) {
        this.empruntService = empruntService;
        this.exportService = exportService;
    }

    // ---------- Délégué ----------

    @PostMapping("/demande")
    public ResponseEntity<Map<String, Object>> demander(@AuthenticationPrincipal UtilisateurConnecte moi,
                                                        @RequestBody DemandeEmpruntRequest requete) {
        Emprunt emprunt = empruntService.demanderEmprunt(moi.getId(), requete.salle(),
                requete.heureRetourPrevue(), requete.articles());
        return Reponses.ok("Demande envoyée ! Présentez-vous au poste de surveillance pour récupérer le matériel.",
                "empruntId", emprunt.getId());
    }

    @PostMapping("/{id}/annuler")
    public ResponseEntity<Map<String, Object>> annuler(@AuthenticationPrincipal UtilisateurConnecte moi,
                                                       @PathVariable Long id) {
        empruntService.annulerDemande(id, moi.getId());
        return Reponses.ok("Votre demande a été annulée.");
    }

    @GetMapping("/mes-emprunts")
    public ResponseEntity<Map<String, Object>> mesEmprunts(@AuthenticationPrincipal UtilisateurConnecte moi) {
        return Reponses.donnees(empruntService.listerEmpruntsDelegue(moi.getId()));
    }

    // ---------- Agent ----------

    @GetMapping("/en-attente")
    public ResponseEntity<Map<String, Object>> enAttente() {
        return Reponses.donnees(empruntService.listerDemandesEnAttente());
    }

    @GetMapping("/en-cours")
    public ResponseEntity<Map<String, Object>> enCours() {
        return Reponses.donnees(empruntService.listerEmpruntsEnCours());
    }

    @GetMapping("/historique")
    public ResponseEntity<Map<String, Object>> historique() {
        return Reponses.donnees(empruntService.listerHistorique());
    }

    @PostMapping("/{id}/valider")
    public ResponseEntity<Map<String, Object>> valider(@AuthenticationPrincipal UtilisateurConnecte moi,
                                                       @PathVariable Long id) {
        empruntService.validerEmprunt(id, moi.getId());
        return Reponses.ok("Emprunt validé : le matériel est remis au délégué.");
    }

    @PostMapping("/{id}/refuser")
    public ResponseEntity<Map<String, Object>> refuser(@AuthenticationPrincipal UtilisateurConnecte moi,
                                                       @PathVariable Long id,
                                                       @RequestBody(required = false) RefusRequest requete) {
        empruntService.refuserDemande(id, moi.getId(), requete != null ? requete.motif() : null);
        return Reponses.ok("Demande refusée : le matériel est de nouveau disponible.");
    }

    @PostMapping("/{id}/retour")
    public ResponseEntity<Map<String, Object>> retour(@AuthenticationPrincipal UtilisateurConnecte moi,
                                                      @PathVariable Long id,
                                                      @RequestBody RetourRequest requete) {
        empruntService.enregistrerRetour(id, moi.getId(), requete.observations(), requete.details());
        return Reponses.ok("Le retour du matériel a bien été enregistré.");
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exporter() {
        String nomFichier = "historique-emprunts-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomFichier + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(exportService.historiqueCsv().getBytes(StandardCharsets.UTF_8));
    }
}
