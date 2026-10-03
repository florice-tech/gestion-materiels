package com.example.gestionmateriels.service;

import com.example.gestionmateriels.model.*;
import com.example.gestionmateriels.model.Notification.Categorie;
import com.example.gestionmateriels.repository.*;
import com.example.gestionmateriels.securite.UtilisateurConnecte;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Matériel manquant. Une ligne par besoin : chaque nouvelle demande du même matériel
 * augmente son compteur, ce qui fait remonter en tête ce qui manque le plus souvent.
 * Elle reste dans la liste jusqu'à l'ajout au catalogue ou au réapprovisionnement
 * (réglés automatiquement), ou jusqu'à ce qu'un agent la règle ou l'abandonne.
 */
@Service
@Transactional
public class ManqueService {

    /** Ce qu'affiche la page « Matériel manquant ». */
    public record Vue(Long id, String designation, Long materielId, String code, String categorie,
                      Integer quantiteSouhaitee, Integer nombreDemandes, long nombreDelegues, List<String> demandeurs,
                      Manque.Statut statut, Manque.Origine origine, LocalDateTime dateCreation,
                      LocalDateTime dateDerniereDemande, LocalDateTime dateResolution, String resoluPar,
                      String noteResolution, List<Signalement> signalements) {
    }

    public record Signalement(String auteur, String salle, String commentaire, LocalDateTime date) {
    }

    /** Un signalement du délégué connecté, avec ce qu'il est devenu. */
    public record MonSignalement(Long manqueId, String designation, LocalDateTime date, Manque.Statut statut,
                                 String noteResolution, Integer nombreDemandes) {
    }

    static final String AUTEUR_SYSTEME = "Système (stock épuisé)";

    private final ManqueRepository manqueRepository;
    private final SignalementManqueRepository signalementRepository;
    private final MaterielRepository materielRepository;
    private final CategorieRepository categorieRepository;
    private final DelegueRepository delegueRepository;
    private final NotificationService notifications;

    public ManqueService(ManqueRepository manqueRepository, SignalementManqueRepository signalementRepository,
                         MaterielRepository materielRepository, CategorieRepository categorieRepository,
                         DelegueRepository delegueRepository, NotificationService notifications) {
        this.manqueRepository = manqueRepository;
        this.signalementRepository = signalementRepository;
        this.materielRepository = materielRepository;
        this.categorieRepository = categorieRepository;
        this.delegueRepository = delegueRepository;
        this.notifications = notifications;
    }

    // =====================================================================
    // Lecture
    // =====================================================================

    @Transactional(readOnly = true)
    public List<Vue> lister() {
        return manqueRepository.findAllByOrderByDateDerniereDemandeDesc().stream()
                .sorted(Comparator.comparing((Manque m) -> m.getStatut() != Manque.Statut.OUVERT)
                        .thenComparing(Manque::getNombreDemandes, Comparator.reverseOrder())
                        .thenComparing(Manque::getDateDerniereDemande, Comparator.reverseOrder()))
                .map(this::vue).toList();
    }

    @Transactional(readOnly = true)
    public List<MonSignalement> mesSignalements(Long delegueId) {
        Map<Long, MonSignalement> parManque = new LinkedHashMap<>();
        for (SignalementManque s : signalementRepository.findByDelegueIdOrderByDateSignalementDesc(delegueId)) {
            Manque m = s.getManque();
            parManque.putIfAbsent(m.getId(), new MonSignalement(m.getId(), m.getDesignation(), s.getDateSignalement(),
                    m.getStatut(), m.getNoteResolution(), m.getNombreDemandes()));
        }
        return new ArrayList<>(parManque.values());
    }

    @Transactional(readOnly = true)
    public long nombreOuverts() {
        return manqueRepository.countByStatut(Manque.Statut.OUVERT);
    }

    // =====================================================================
    // Signaler
    // =====================================================================

    /**
     * Un délégué (ou un agent) signale qu'il manque un matériel : un équipement du catalogue
     * indisponible (materielId) ou un matériel qui n'existe pas encore (désignation libre).
     * @return le manque et un message pour l'utilisateur
     */
    public Map.Entry<Manque, String> signaler(Long materielId, String designation, Long categorieId, Integer quantite,
                                              String salle, String commentaire, UtilisateurConnecte moi) {
        Materiel materiel = null;
        String nom;
        if (materielId != null) {
            materiel = materielRepository.findById(materielId)
                    .orElseThrow(() -> OperationException.introuvable("Matériel introuvable."));
            nom = materiel.getDesignation();
        } else {
            nom = Verifications.obligatoire(designation, "Indiquez le matériel qui manque.");
            if (nom.length() < 2 || nom.length() > 150) {
                throw new OperationException("Le nom du matériel doit faire entre 2 et 150 caractères.");
            }
            // Un matériel du catalogue porte déjà ce nom : on le relie
            String cle = normaliser(nom);
            materiel = materielRepository.findAllByOrderByDesignationAsc().stream()
                    .filter(m -> normaliser(m.getDesignation()).equals(cle)).findFirst().orElse(null);
        }
        int qte = quantite == null ? 1 : quantite;
        if (qte < 1 || qte > 500) {
            throw new OperationException("La quantité doit être comprise entre 1 et 500.");
        }

        Delegue delegue = moi.estDelegue() ? delegueRepository.findById(moi.getId()).orElse(null) : null;
        Optional<Manque> existant = trouverOuvert(materiel, nom);
        String message;
        Manque manque;
        if (existant.isPresent()) {
            manque = existant.get();
            boolean dejaSignaleRecemment = delegue != null && signalementRepository
                    .findByManqueIdOrderByDateSignalementDesc(manque.getId()).stream()
                    .anyMatch(s -> s.getDelegue() != null && s.getDelegue().getId().equals(delegue.getId())
                            && s.getDateSignalement().isAfter(LocalDateTime.now().minusHours(12)));
            if (dejaSignaleRecemment) {
                return Map.entry(manque, "Vous l'avez déjà signalé aujourd'hui : c'est bien noté.");
            }
            manque.setNombreDemandes(manque.getNombreDemandes() + 1);
            manque.setQuantiteSouhaitee(Math.max(manque.getQuantiteSouhaitee(), qte));
            manque.setDateDerniereDemande(LocalDateTime.now());
            message = "C'est noté : « " + manque.getDesignation() + " » a maintenant été demandé "
                    + manque.getNombreDemandes() + " fois.";
        } else {
            manque = new Manque();
            manque.setDesignation(nom.trim());
            manque.setMateriel(materiel);
            manque.setCategorie(materiel != null ? materiel.getCategorie()
                    : categorieId != null ? categorieRepository.findById(categorieId).orElse(null) : null);
            manque.setQuantiteSouhaitee(qte);
            manque.setOrigine(moi.estAgent() ? Manque.Origine.AGENT
                    : materiel != null ? Manque.Origine.INDISPONIBLE : Manque.Origine.DELEGUE);
            manque = manqueRepository.save(manque);
            message = "Merci : « " + manque.getDesignation() + " » est ajouté au matériel manquant. Vous serez prévenu quand il sera disponible.";
            notifications.notifierAgents(Categorie.INFO, "Matériel manquant signalé",
                    moi.getNom() + " signale qu'il manque : " + manque.getDesignation()
                            + (qte > 1 ? " (" + qte + ")" : "") + ".", "manquants.html");
        }
        signalementRepository.save(new SignalementManque(manque, delegue, moi.getNom(),
                Verifications.facultatif(salle), longueur(Verifications.facultatif(commentaire))));
        return Map.entry(manque, message);
    }

    /** Une fourniture vient de tomber à zéro : elle passe dans le matériel manquant. */
    void stockEpuise(Materiel materiel) {
        Optional<Manque> existant = manqueRepository.findFirstByStatutAndMaterielId(Manque.Statut.OUVERT, materiel.getId());
        Manque manque;
        if (existant.isPresent()) {
            manque = existant.get();
            manque.setNombreDemandes(manque.getNombreDemandes() + 1);
            manque.setDateDerniereDemande(LocalDateTime.now());
        } else {
            manque = new Manque();
            manque.setDesignation(materiel.getDesignation());
            manque.setMateriel(materiel);
            manque.setCategorie(materiel.getCategorie());
            manque.setQuantiteSouhaitee(Math.max(1, materiel.getSeuilAlerte() * 2));
            manque.setOrigine(Manque.Origine.STOCK_EPUISE);
            manque = manqueRepository.save(manque);
            notifications.notifierAgents(Categorie.INFO, "Stock épuisé",
                    materiel.getDesignation() + " est épuisé : il est ajouté au matériel manquant.", "manquants.html");
        }
        signalementRepository.save(new SignalementManque(manque, null, AUTEUR_SYSTEME, null, null));
    }

    // =====================================================================
    // Régler
    // =====================================================================

    /** Un matériel vient d'être ajouté au catalogue : les manques du même nom sont réglés. */
    void surAjoutAuCatalogue(Materiel materiel) {
        String cle = normaliser(materiel.getDesignation());
        for (Manque m : manqueRepository.findByStatut(Manque.Statut.OUVERT)) {
            if (m.getMateriel() == null && normaliser(m.getDesignation()).equals(cle)) {
                m.setMateriel(materiel);
                cloturer(m, Manque.Statut.RESOLU, "Ajouté au catalogue (" + materiel.getCodeUnique() + ")", "Catalogue");
            }
        }
    }

    /** Une fourniture a été réapprovisionnée : son manque est réglé. */
    void surReapprovisionnement(Materiel materiel) {
        if (materiel.getQuantiteStock() <= 0) {
            return;
        }
        manqueRepository.findFirstByStatutAndMaterielId(Manque.Statut.OUVERT, materiel.getId()).ifPresent(m ->
                cloturer(m, Manque.Statut.RESOLU, "Réapprovisionné : " + materiel.getQuantiteStock() + " en stock", "Catalogue"));
    }

    public Manque resoudre(Long id, String note, String agent) {
        Manque m = trouverOuvert(id);
        cloturer(m, Manque.Statut.RESOLU, Verifications.facultatif(note) != null ? longueur(note.trim()) : "Matériel disponible", agent);
        return m;
    }

    public Manque abandonner(Long id, String motif, String agent) {
        Manque m = trouverOuvert(id);
        String texte = Verifications.obligatoire(motif, "Indiquez pourquoi ce matériel ne sera pas acheté.");
        cloturer(m, Manque.Statut.ABANDONNE, longueur(texte), agent);
        return m;
    }

    /** Rouvrir un manque réglé trop tôt. */
    public Manque rouvrir(Long id) {
        Manque m = manqueRepository.findById(id)
                .orElseThrow(() -> OperationException.introuvable("Matériel manquant introuvable."));
        m.setStatut(Manque.Statut.OUVERT);
        m.setDateResolution(null);
        m.setResoluPar(null);
        m.setNoteResolution(null);
        return m;
    }

    // =====================================================================
    // Outils
    // =====================================================================

    private void cloturer(Manque m, Manque.Statut statut, String note, String agent) {
        m.setStatut(statut);
        m.setDateResolution(LocalDateTime.now());
        m.setResoluPar(agent);
        m.setNoteResolution(note);
        Set<Long> dejaPrevenus = new HashSet<>();
        for (SignalementManque s : signalementRepository.findByManqueIdOrderByDateSignalementDesc(m.getId())) {
            Delegue d = s.getDelegue();
            if (d != null && dejaPrevenus.add(d.getId())) {
                if (statut == Manque.Statut.RESOLU) {
                    notifications.notifierDelegue(d, Categorie.VALIDATION, "Matériel disponible",
                            "« " + m.getDesignation() + " » que vous aviez demandé est maintenant disponible. " + note + ".",
                            "accueil-delegue.html");
                } else {
                    notifications.notifierDelegue(d, Categorie.REFUS, "Matériel non retenu",
                            "« " + m.getDesignation() + " » ne sera pas acheté pour le moment : " + note + ".",
                            "accueil-delegue.html");
                }
            }
        }
    }

    private Optional<Manque> trouverOuvert(Materiel materiel, String nom) {
        if (materiel != null) {
            Optional<Manque> parMateriel = manqueRepository.findFirstByStatutAndMaterielId(Manque.Statut.OUVERT, materiel.getId());
            if (parMateriel.isPresent()) {
                return parMateriel;
            }
        }
        String cle = normaliser(nom);
        return manqueRepository.findByStatut(Manque.Statut.OUVERT).stream()
                .filter(m -> normaliser(m.getDesignation()).equals(cle)).findFirst();
    }

    private Manque trouverOuvert(Long id) {
        Manque m = manqueRepository.findById(id)
                .orElseThrow(() -> OperationException.introuvable("Matériel manquant introuvable."));
        if (m.getStatut() != Manque.Statut.OUVERT) {
            throw new OperationException("Ce matériel manquant a déjà été réglé.");
        }
        return m;
    }

    private Vue vue(Manque m) {
        List<SignalementManque> liste = signalementRepository.findByManqueIdOrderByDateSignalementDesc(m.getId());
        Set<Long> delegues = new HashSet<>();
        LinkedHashSet<String> noms = new LinkedHashSet<>();
        for (SignalementManque s : liste) {
            if (s.getDelegue() != null) {
                delegues.add(s.getDelegue().getId());
            }
            noms.add(s.getAuteur());
        }
        return new Vue(m.getId(), m.getDesignation(),
                m.getMateriel() != null ? m.getMateriel().getId() : null,
                m.getMateriel() != null ? m.getMateriel().getCodeUnique() : null,
                m.getCategorie() != null ? m.getCategorie().getNom() : null,
                m.getQuantiteSouhaitee(), m.getNombreDemandes(), delegues.size(), noms.stream().limit(6).toList(),
                m.getStatut(), m.getOrigine(), m.getDateCreation(), m.getDateDerniereDemande(), m.getDateResolution(),
                m.getResoluPar(), m.getNoteResolution(),
                liste.stream().limit(20).map(s -> new Signalement(s.getAuteur(), s.getSalle(), s.getCommentaire(),
                        s.getDateSignalement())).toList());
    }

    /** « Vidéoprojecteurs portables » = « vidéoprojecteur portable » (accents, majuscules et pluriel simple ignorés). */
    static String normaliser(String texte) {
        String s = Normalizer.normalize(texte == null ? "" : texte, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
        StringBuilder cle = new StringBuilder();
        for (String mot : s.split(" ")) {
            cle.append(mot.length() > 3 && (mot.endsWith("s") || mot.endsWith("x")) ? mot.substring(0, mot.length() - 1) : mot);
        }
        return cle.toString();
    }

    private static String longueur(String texte) {
        return texte == null ? null : texte.length() > 500 ? texte.substring(0, 500) : texte;
    }
}
