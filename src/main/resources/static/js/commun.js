// Fonctions partagées par toutes les pages.

// Adresse relative : fonctionne quel que soit le port ou la machine qui héberge l'application.
const API_BASE = '/api';

/**
 * Renvoie l'utilisateur connecté, ou redirige vers la page de connexion
 * s'il n'y a pas de session ou si le profil ne correspond pas.
 * @param {'AGENT'|'DELEGUE'} typeAttendu
 */
function exigerSession(typeAttendu) {
    let utilisateur = null;
    try {
        utilisateur = JSON.parse(localStorage.getItem('utilisateurConnecte'));
    } catch (e) {
        utilisateur = null;
    }
    if (!utilisateur || (typeAttendu && utilisateur.type !== typeAttendu)) {
        window.location.href = 'login.html';
        throw new Error('Session absente');
    }
    return utilisateur;
}

function seDeconnecter() {
    localStorage.removeItem('utilisateurConnecte');
    window.location.href = 'login.html';
}

/** Échappe le texte avant de l'insérer dans du HTML (évite l'injection de code via un nom ou une désignation). */
function echapper(valeur) {
    if (valeur === null || valeur === undefined) return '';
    return String(valeur)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

/** Appel JSON à l'API. Renvoie toujours un objet (ou un tableau pour les listes simples). */
async function appelApi(chemin, options = {}) {
    const config = { method: options.method || 'GET', headers: {} };
    if (options.body !== undefined) {
        config.headers['Content-Type'] = 'application/json';
        config.body = JSON.stringify(options.body);
    }
    const res = await fetch(`${API_BASE}${chemin}`, config);
    try {
        return await res.json();
    } catch (e) {
        return { success: false, message: `Erreur serveur (${res.status}).` };
    }
}

/** Affiche un message de succès (vert) ou d'erreur (rouge) dans une zone donnée. */
function afficherMessage(zone, texte, succes, taille = 'text-sm') {
    if (typeof zone === 'string') zone = document.getElementById(zone);
    zone.textContent = texte;
    zone.className = `${taille} rounded-lg p-3 ${succes
        ? 'bg-green-50 text-green-700 border border-green-200'
        : 'bg-red-50 text-red-700 border border-red-200'}`;
    zone.classList.remove('hidden');
}

function formaterDate(iso) {
    return iso ? new Date(iso).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' }) : '—';
}

function formaterHeure(heure) {
    return heure ? heure.substring(0, 5) : null;
}

/**
 * Vrai si l'échéance de retour est dépassée : le jour même après l'heure prévue,
 * ou n'importe quel jour ultérieur au jour de sortie.
 */
function estEnRetard(emprunt) {
    if (!emprunt.heureRetourPrevue || emprunt.dateRetour) return false;

    const dateSortie = new Date(emprunt.dateSortie);
    const [heures, minutes] = emprunt.heureRetourPrevue.split(':').map(Number);
    const echeance = new Date(dateSortie);
    echeance.setHours(heures, minutes, 0, 0);

    const maintenant = new Date();
    if (maintenant.toDateString() === dateSortie.toDateString()) {
        return maintenant > echeance;
    }
    return maintenant > dateSortie;
}

/** Statut lisible d'une fiche d'emprunt. */
function statutFiche(emprunt) {
    if (emprunt.statutEmprunt === 'EN_ATTENTE') {
        return { texte: 'En attente', classe: 'bg-amber-100 text-amber-700' };
    }
    if (emprunt.dateRetour) {
        return { texte: 'Rendu', classe: 'bg-slate-200 text-slate-600' };
    }
    if (estEnRetard(emprunt)) {
        return { texte: 'En retard', classe: 'bg-red-600 text-white' };
    }
    return { texte: 'En cours', classe: 'bg-blue-100 text-blue-700' };
}

const LABELS_ETAT_RETOUR = {
    BON_ETAT: { texte: 'Bon état', icone: '🟢' },
    A_VERIFIER: { texte: 'À vérifier', icone: '🟡' },
    ENDOMMAGE: { texte: 'Endommagé', icone: '🔴' },
    VIDE_EPUISE: { texte: 'Vide / épuisé', icone: '⚫' }
};

/** Liste HTML (échappée) des articles d'une fiche. */
function listeArticlesHtml(emprunt) {
    return (emprunt.details || []).map(d => {
        const qte = d.materiel.typeGestion === 'CONSOMMABLE' ? ` × ${d.quantite}` : '';
        const etat = d.etatRetour ? LABELS_ETAT_RETOUR[d.etatRetour] : null;
        return `<div>${etat ? etat.icone + ' ' : ''}${echapper(d.materiel.designation)}${qte}</div>`;
    }).join('');
}
