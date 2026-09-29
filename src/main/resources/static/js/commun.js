// Fonctions partagées par toutes les pages : appels à l'API, session, en-tête, messages, fenêtres.

const API_BASE = '/api';

// ---------------------------------------------------------------------------
// Appels à l'API
// ---------------------------------------------------------------------------

function lireCookie(nom) {
    const trouve = document.cookie.split('; ').find(c => c.startsWith(nom + '='));
    return trouve ? decodeURIComponent(trouve.substring(nom.length + 1)) : null;
}

/**
 * Appel JSON à l'API. Le cookie de session part automatiquement ; pour les requêtes
 * qui modifient des données, le jeton CSRF (cookie XSRF-TOKEN) est renvoyé en en-tête.
 * Renvoie toujours le JSON reçu (objet ou tableau), ou { success:false, message } en cas d'erreur réseau.
 */
async function appelApi(chemin, options = {}) {
    const methode = options.method || 'GET';
    const config = { method: methode, headers: { 'Accept': 'application/json' }, credentials: 'same-origin' };
    if (methode !== 'GET') {
        const jeton = lireCookie('XSRF-TOKEN');
        if (jeton) config.headers['X-XSRF-TOKEN'] = jeton;
    }
    if (options.body !== undefined) {
        config.headers['Content-Type'] = 'application/json';
        config.body = JSON.stringify(options.body);
    }

    let res;
    try {
        res = await fetch(`${API_BASE}${chemin}`, config);
    } catch (e) {
        return { success: false, message: 'Impossible de joindre le serveur. Vérifiez votre connexion.' };
    }

    // Session expirée : retour à la connexion (sauf pendant la connexion elle-même)
    if (res.status === 401 && !options.sansRedirection) {
        window.location.href = 'login.html?expire=1';
        return new Promise(() => {}); // la page va changer : on ne continue pas
    }

    try {
        return await res.json();
    } catch (e) {
        return { success: false, message: `Erreur serveur (${res.status}).` };
    }
}

// ---------------------------------------------------------------------------
// Session et en-tête
// ---------------------------------------------------------------------------

const NAVIGATION = {
    AGENT: [
        { cle: 'tableau', texte: 'Tableau de bord', lien: 'accueil-agent.html' },
        { cle: 'demandes', texte: 'Demandes', lien: 'emprunt.html' },
        { cle: 'retours', texte: 'Retours', lien: 'retour.html' },
        { cle: 'catalogue', texte: 'Catalogue', lien: 'materiel.html' },
        { cle: 'historique', texte: 'Historique', lien: 'historique.html' },
        { cle: 'parametres', texte: 'Salles & catégories', lien: 'parametres.html' },
        { cle: 'comptes', texte: 'Comptes', lien: 'comptes.html', admin: true }
    ],
    DELEGUE: [
        { cle: 'accueil', texte: 'Accueil', lien: 'accueil-delegue.html' },
        { cle: 'demande', texte: 'Nouvelle demande', lien: 'demande-emprunt.html' },
        { cle: 'mes-emprunts', texte: 'Mes emprunts', lien: 'mes-emprunts.html' }
    ]
};

/**
 * À appeler au chargement de chaque page protégée.
 * Vérifie la session auprès du serveur, redirige si le profil ne convient pas,
 * construit l'en-tête de navigation et renvoie l'utilisateur connecté.
 * @param {{type?: 'AGENT'|'DELEGUE', admin?: boolean, page: string}} options
 */
async function initPage(options) {
    const moi = await appelApi('/auth/moi');
    if (!moi || !moi.success) {
        window.location.href = 'login.html';
        return new Promise(() => {});
    }
    if ((options.type && moi.type !== options.type) || (options.admin && !moi.administrateur)) {
        window.location.href = moi.type === 'AGENT' ? 'accueil-agent.html' : 'accueil-delegue.html';
        return new Promise(() => {});
    }
    construireEntete(moi, options.page);
    return moi;
}

function construireEntete(moi, pageActive) {
    const conteneur = document.getElementById('entete');
    if (!conteneur) return;

    const liens = NAVIGATION[moi.type]
        .filter(l => !l.admin || moi.administrateur)
        .map(l => `<a href="${l.lien}" class="px-3 py-1.5 rounded-lg text-sm font-medium transition ${
            l.cle === pageActive ? 'bg-white text-blue-700 shadow-sm' : 'text-blue-50 hover:bg-blue-500'}">${l.texte}</a>`)
        .join('');

    const sousTitre = moi.type === 'AGENT'
        ? `${moi.role}${moi.administrateur ? ' · administrateur' : ''}`
        : moi.filiereNiveau;

    conteneur.innerHTML = `
        <header class="bg-blue-600 text-white shadow-md">
            <div class="max-w-6xl mx-auto px-4 md:px-6 py-3 flex flex-wrap items-center gap-3 justify-between">
                <a href="${NAVIGATION[moi.type][0].lien}" class="flex items-center gap-2 font-bold text-lg">
                    <span class="bg-white text-blue-600 rounded-lg w-8 h-8 grid place-items-center text-base">GM</span>
                    Gestion Matériel
                </a>
                <nav class="flex flex-wrap gap-1 order-3 md:order-2 w-full md:w-auto">${liens}</nav>
                <div class="flex items-center gap-3 order-2 md:order-3">
                    <a href="mon-compte.html" class="text-right leading-tight hover:underline" title="Mon compte">
                        <div class="text-sm font-semibold">${echapper(moi.nom)}</div>
                        <div class="text-xs text-blue-100">${echapper(sousTitre)}</div>
                    </a>
                    <button id="btnDeconnexion" class="bg-blue-700 hover:bg-blue-800 text-xs px-3 py-2 rounded-lg font-medium">Déconnexion</button>
                </div>
            </div>
        </header>`;
    document.getElementById('btnDeconnexion').addEventListener('click', seDeconnecter);
}

async function seDeconnecter() {
    await appelApi('/auth/logout', { method: 'POST', sansRedirection: true });
    window.location.href = 'login.html';
}

// ---------------------------------------------------------------------------
// Affichage
// ---------------------------------------------------------------------------

/** Échappe le texte avant de l'insérer dans du HTML (évite l'injection de code). */
function echapper(valeur) {
    if (valeur === null || valeur === undefined) return '';
    return String(valeur)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

/** Message vert (succès) ou rouge (erreur) dans une zone ; disparaît après quelques secondes si succès. */
function afficherMessage(zone, texte, succes) {
    if (typeof zone === 'string') zone = document.getElementById(zone);
    if (!zone) return;
    zone.textContent = texte || (succes ? 'Opération réussie.' : 'Une erreur est survenue.');
    zone.className = `text-sm rounded-lg p-3 ${succes
        ? 'bg-green-50 text-green-700 border border-green-200'
        : 'bg-red-50 text-red-700 border border-red-200'}`;
    clearTimeout(zone._minuteur);
    if (succes) zone._minuteur = setTimeout(() => zone.classList.add('hidden'), 5000);
}

function formaterDate(iso) {
    return iso ? new Date(iso).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' }) : '—';
}

function formaterJour(iso) {
    return iso ? new Date(iso).toLocaleDateString('fr-FR', { weekday: 'short', day: 'numeric', month: 'short' }) : '—';
}

function formaterHeure(heure) {
    return heure ? heure.substring(0, 5) : null;
}

const STATUTS_FICHE = {
    EN_ATTENTE: { texte: 'En attente', classe: 'bg-amber-100 text-amber-800' },
    EN_COURS: { texte: 'En cours', classe: 'bg-blue-100 text-blue-700' },
    RETARD: { texte: 'En retard', classe: 'bg-red-600 text-white' },
    RETOURNE: { texte: 'Rendu', classe: 'bg-green-100 text-green-700' },
    REFUSEE: { texte: 'Refusée', classe: 'bg-slate-200 text-slate-700' },
    ANNULEE: { texte: 'Annulée', classe: 'bg-slate-100 text-slate-500' }
};

/** Statut lisible d'une fiche (le retard est calculé par le serveur : champ enRetard). */
function statutFiche(emprunt) {
    return emprunt.enRetard ? STATUTS_FICHE.RETARD : (STATUTS_FICHE[emprunt.statutEmprunt] || STATUTS_FICHE.EN_ATTENTE);
}

function badge(info) {
    return `<span class="inline-block text-[11px] font-bold px-2 py-0.5 rounded-full whitespace-nowrap ${info.classe}">${info.texte}</span>`;
}

const LABELS_ETAT_RETOUR = {
    BON_ETAT: { texte: 'Bon état', icone: '🟢' },
    A_VERIFIER: { texte: 'À vérifier', icone: '🟡' },
    ENDOMMAGE: { texte: 'Endommagé', icone: '🔴' },
    VIDE_EPUISE: { texte: 'Vide / épuisé', icone: '⚫' }
};

const LABELS_STATUT_MATERIEL = {
    DISPONIBLE: { texte: 'Disponible', classe: 'bg-green-100 text-green-700' },
    EMPRUNTE: { texte: 'Emprunté', classe: 'bg-blue-100 text-blue-700' },
    A_VERIFIER: { texte: 'À vérifier', classe: 'bg-amber-100 text-amber-800' },
    MAINTENANCE: { texte: 'Maintenance', classe: 'bg-red-100 text-red-700' },
    HS: { texte: 'Hors service', classe: 'bg-slate-200 text-slate-600' }
};

/** Liste HTML (échappée) des articles d'une fiche, avec quantité et état de retour. */
function listeArticlesHtml(emprunt) {
    return (emprunt.details || []).map(d => {
        const qte = d.materiel.typeGestion === 'CONSOMMABLE' ? ` × ${d.quantite}` : '';
        const etat = d.etatRetour ? LABELS_ETAT_RETOUR[d.etatRetour] : null;
        return `<div>${etat ? `<span title="${etat.texte}">${etat.icone}</span> ` : ''}${echapper(d.materiel.designation)}${qte}</div>`;
    }).join('');
}

function etatVide(texte) {
    return `<p class="text-sm text-slate-500 italic py-8 text-center">${texte}</p>`;
}

// ---------------------------------------------------------------------------
// Fenêtre de saisie (remplace prompt/confirm)
// ---------------------------------------------------------------------------

/**
 * Ouvre une fenêtre avec un formulaire et renvoie une promesse :
 * les valeurs saisies ({nom: valeur}) si l'utilisateur valide, null s'il annule.
 * champs : [{ nom, label, type: 'text'|'password'|'number'|'select'|'textarea'|'checkbox', valeur, options:[{valeur,texte}], requis, aide }]
 */
function demanderFormulaire({ titre, texte = '', champs = [], bouton = 'Valider', danger = false }) {
    return new Promise(resolve => {
        const fond = document.createElement('div');
        fond.className = 'fixed inset-0 bg-slate-900/50 z-50 flex items-center justify-center p-4';

        const champsHtml = champs.map((c, i) => {
            const id = `champ_${i}`;
            const requis = c.requis ? 'required' : '';
            const classe = 'w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none';
            let saisie;
            if (c.type === 'select') {
                saisie = `<select id="${id}" name="${c.nom}" class="${classe}" ${requis}>${(c.options || []).map(o =>
                    `<option value="${echapper(o.valeur)}" ${String(o.valeur) === String(c.valeur) ? 'selected' : ''}>${echapper(o.texte)}</option>`).join('')}</select>`;
            } else if (c.type === 'textarea') {
                saisie = `<textarea id="${id}" name="${c.nom}" rows="3" class="${classe}" ${requis}>${echapper(c.valeur ?? '')}</textarea>`;
            } else if (c.type === 'checkbox') {
                return `<label class="flex items-center gap-2 text-sm text-slate-700"><input type="checkbox" id="${id}" name="${c.nom}" class="w-4 h-4" ${c.valeur ? 'checked' : ''}> ${echapper(c.label)}</label>`;
            } else {
                saisie = `<input id="${id}" name="${c.nom}" type="${c.type || 'text'}" value="${echapper(c.valeur ?? '')}" class="${classe}" ${requis} ${c.type === 'number' ? 'min="0"' : ''}>`;
            }
            return `<div><label for="${id}" class="block text-xs font-medium text-slate-600 mb-1">${echapper(c.label)}</label>${saisie}${
                c.aide ? `<p class="text-xs text-slate-400 mt-1">${echapper(c.aide)}</p>` : ''}</div>`;
        }).join('');

        fond.innerHTML = `
            <form class="bg-white rounded-xl shadow-xl w-full max-w-md p-5 space-y-4" role="dialog" aria-modal="true">
                <h2 class="text-lg font-bold text-slate-800">${echapper(titre)}</h2>
                ${texte ? `<p class="text-sm text-slate-600">${echapper(texte)}</p>` : ''}
                ${champsHtml}
                <div class="flex justify-end gap-2 pt-2">
                    <button type="button" data-annuler class="px-4 py-2 text-sm rounded-lg border border-slate-300 text-slate-600 hover:bg-slate-50">Annuler</button>
                    <button type="submit" class="px-4 py-2 text-sm rounded-lg text-white font-medium ${danger ? 'bg-red-600 hover:bg-red-700' : 'bg-blue-600 hover:bg-blue-700'}">${echapper(bouton)}</button>
                </div>
            </form>`;

        const formulaire = fond.querySelector('form');
        const fermer = valeur => { fond.remove(); document.removeEventListener('keydown', echap); resolve(valeur); };
        const echap = e => { if (e.key === 'Escape') fermer(null); };

        fond.querySelector('[data-annuler]').addEventListener('click', () => fermer(null));
        fond.addEventListener('click', e => { if (e.target === fond) fermer(null); });
        document.addEventListener('keydown', echap);
        formulaire.addEventListener('submit', e => {
            e.preventDefault();
            const valeurs = {};
            champs.forEach((c, i) => {
                const el = document.getElementById(`champ_${i}`);
                valeurs[c.nom] = c.type === 'checkbox' ? el.checked : el.value;
            });
            fermer(valeurs);
        });

        document.body.appendChild(fond);
        const premier = formulaire.querySelector('input, select, textarea');
        if (premier) premier.focus(); else formulaire.querySelector('button[type=submit]').focus();
    });
}

/** Confirmation simple (Oui / Annuler). */
async function confirmer(titre, texte, bouton = 'Confirmer', danger = false) {
    return (await demanderFormulaire({ titre, texte, bouton, danger })) !== null;
}
