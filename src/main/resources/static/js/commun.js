// Fonctions partagées par toutes les pages : appels à l'API, session, en-tête, messages, fenêtres.

const API_BASE = '/api';

// Mode sombre : choix enregistré, sinon réglage du système
(function appliquerTheme() {
    let choix = null;
    try { choix = localStorage.getItem('theme'); } catch (e) { /* stockage indisponible */ }
    const sombre = choix ? choix === 'sombre' : window.matchMedia('(prefers-color-scheme: dark)').matches;
    document.documentElement.classList.toggle('dark', sombre);
})();

// Couleurs de Lomé Business School : la palette « blue » de Tailwind est remplacée par le bleu marine du logo,
// ce qui applique la charte à toutes les pages sans toucher à leurs classes.
if (window.tailwind) {
    window.tailwind.config = {
        theme: { extend: { colors: { blue: {
            50: '#eef3f9', 100: '#d9e3f0', 200: '#b6c8e0', 300: '#8aa6cb', 400: '#5d82b2',
            500: '#3a6296', 600: '#24436f', 700: '#1c3659', 800: '#152945', 900: '#0f1d31'
        }, lbs: { rouge: '#a3141c' } } } }
    };
}

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

// Icônes au trait (24×24), dessinées pour l'application
const ICONES = {
    tableau: '<path d="M4 13h6V4H4zM14 20h6v-9h-6zM4 20h6v-4H4zM14 8h6V4h-6z"/>',
    demandes: '<path d="M4 6h16M4 12h10M4 18h7"/><circle cx="18" cy="17" r="3"/>',
    retours: '<path d="M9 14 4 9l5-5"/><path d="M4 9h11a5 5 0 0 1 0 10h-3"/>',
    scan: '<path d="M4 8V5a1 1 0 0 1 1-1h3M16 4h3a1 1 0 0 1 1 1v3M20 16v3a1 1 0 0 1-1 1h-3M8 20H5a1 1 0 0 1-1-1v-3"/><path d="M8 12h8"/>',
    catalogue: '<path d="M21 8 12 3 3 8l9 5 9-5Z"/><path d="M3 8v8l9 5 9-5V8"/><path d="M12 13v8"/>',
    etiquettes: '<rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><path d="M14 14h3v3h-3zM20 14v.01M14 20h.01M17 20h4v-3"/>',
    historique: '<path d="M3 12a9 9 0 1 0 3-6.7L3 8"/><path d="M3 3v5h5"/><path d="M12 7v5l3 2"/>',
    parametres: '<path d="M4 21V14M4 10V3M12 21v-9M12 8V3M20 21v-5M20 12V3M1 14h6M9 8h6M17 16h6"/>',
    comptes: '<circle cx="9" cy="8" r="4"/><path d="M2 21a7 7 0 0 1 14 0"/><path d="M17 11a3 3 0 1 0 0-6M22 21a6 6 0 0 0-4-5.6"/>',
    accueil: '<path d="m3 11 9-7 9 7"/><path d="M5 10v10h14V10"/>',
    demande: '<path d="M12 5v14M5 12h14"/>',
    reservation: '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 11h18"/>',
    emprunts: '<path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01"/>',
    cloche: '<path d="M6 8a6 6 0 0 1 12 0c0 7 3 8 3 8H3s3-1 3-8"/><path d="M10.3 21a1.9 1.9 0 0 0 3.4 0"/>',
    lune: '<path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8Z"/>',
    soleil: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>',
    menu: '<path d="M4 6h16M4 12h16M4 18h16"/>',
    ecran: '<rect x="2" y="4" width="20" height="13" rx="2"/><path d="M8 21h8M12 17v4"/>',
    affluence: '<rect x="3" y="3" width="5" height="5" rx="1"/><rect x="10" y="3" width="5" height="5" rx="1"/><rect x="17" y="3" width="4" height="5" rx="1"/><rect x="3" y="10" width="5" height="5" rx="1"/><rect x="10" y="10" width="5" height="5" rx="1"/><rect x="3" y="17" width="5" height="4" rx="1"/>',
    salle: '<path d="M3 21h18M5 21V4a1 1 0 0 1 1-1h12a1 1 0 0 1 1 1v17"/><path d="M14 12h.01"/>',
    photo: '<path d="M4 7h3l2-3h6l2 3h3a1 1 0 0 1 1 1v11a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V8a1 1 0 0 1 1-1Z"/><circle cx="12" cy="13" r="3.5"/>',
    sortie: '<path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/><path d="m10 17 5-5-5-5M15 12H3"/>'
};

function icone(nom, classe = '') {
    return `<svg class="${classe}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICONES[nom] || ''}</svg>`;
}

// Menus groupés par usage
const NAVIGATION = {
    AGENT: [
        { groupe: 'Opérations', liens: [
            { cle: 'tableau', texte: 'Tableau de bord', lien: 'accueil-agent.html', icone: 'tableau' },
            { cle: 'demandes', texte: 'Demandes et réservations', lien: 'emprunt.html', icone: 'demandes' },
            { cle: 'retours', texte: 'Retours', lien: 'retour.html', icone: 'retours' },
            { cle: 'scan', texte: 'Scanner un matériel', lien: 'scan.html', icone: 'scan' },
            { cle: 'kiosque', texte: 'Écran du poste', lien: 'kiosque.html', icone: 'ecran' }
        ] },
        { groupe: 'Matériel', liens: [
            { cle: 'catalogue', texte: 'Catalogue', lien: 'materiel.html', icone: 'catalogue' },
            { cle: 'etiquettes', texte: 'Étiquettes QR', lien: 'etiquettes.html', icone: 'etiquettes' },
            { cle: 'historique', texte: 'Historique', lien: 'historique.html', icone: 'historique' }
        ] },
        { groupe: 'Administration', liens: [
            { cle: 'parametres', texte: 'Salles et catégories', lien: 'parametres.html', icone: 'parametres' },
            { cle: 'comptes', texte: 'Comptes et confiance', lien: 'comptes.html', icone: 'comptes', admin: true },
            { cle: 'affluence', texte: 'Heures chargées', lien: 'affluence.html', icone: 'affluence', admin: true }
        ] }
    ],
    DELEGUE: [
        { groupe: 'Mon espace', liens: [
            { cle: 'accueil', texte: 'Accueil', lien: 'accueil-delegue.html', icone: 'accueil' },
            { cle: 'scan', texte: 'Scanner un matériel', lien: 'scan.html', icone: 'scan' },
            { cle: 'demande', texte: 'Demander du matériel', lien: 'demande-emprunt.html', icone: 'demande' },
            { cle: 'reservation', texte: 'Réserver à l\'avance', lien: 'reservation.html', icone: 'reservation' },
            { cle: 'mes-emprunts', texte: 'Mes emprunts', lien: 'mes-emprunts.html', icone: 'emprunts' }
        ] }
    ]
};

let UTILISATEUR = null;

/**
 * À appeler au chargement de chaque page protégée.
 * Vérifie la session auprès du serveur, redirige si le profil ne convient pas,
 * construit le menu et renvoie l'utilisateur connecté.
 * @param {{type?: 'AGENT'|'DELEGUE', admin?: boolean, page: string}} options
 */
async function initPage(options) {
    const moi = await appelApi('/auth/moi');
    if (!moi || !moi.success) {
        const retour = encodeURIComponent(location.pathname.replace(/^\//, '') + location.search);
        window.location.href = 'login.html?retour=' + retour;
        return new Promise(() => {});
    }
    if ((options.type && moi.type !== options.type) || (options.admin && !moi.administrateur)) {
        window.location.href = moi.type === 'AGENT' ? 'accueil-agent.html' : 'accueil-delegue.html';
        return new Promise(() => {});
    }
    UTILISATEUR = moi;
    construireEntete(moi, options.page);
    return moi;
}

function construireEntete(moi, pageActive) {
    const conteneur = document.getElementById('entete');
    if (!conteneur) return;
    document.body.classList.add('avec-menu');

    const groupes = NAVIGATION[moi.type].map(g => {
        const liens = g.liens.filter(l => !l.admin || moi.administrateur);
        if (!liens.length) return '';
        return `<div class="groupe">${g.groupe}</div>` + liens.map(l =>
            `<a href="${l.lien}" class="lien" ${l.cle === pageActive ? 'aria-current="page"' : ''}>${icone(l.icone)}<span>${l.texte}</span></a>`).join('');
    }).join('');

    const sousTitre = moi.type === 'AGENT'
        ? `${moi.role}${moi.administrateur ? ', administrateur' : ''}`
        : moi.filiereNiveau;
    const initiales = moi.nom.split(/\s+/).filter(m => /[A-Za-zÀ-ÿ]/.test(m[0] || '')).slice(-2).map(m => m[0]).join('').toUpperCase();

    conteneur.innerHTML = `
        <div id="voile" class="fixed inset-0 bg-slate-900/40 z-30 hidden lg:hidden"></div>
        <aside id="menuLateral" class="menu-lateral fixed inset-y-0 left-0 w-64 z-40 flex flex-col" aria-label="Menu principal">
            <a href="${NAVIGATION[moi.type][0].liens[0].lien}" class="flex items-center gap-3 px-5 pt-5 pb-3">
                ${logoHtml('h-9')}
                <span class="leading-tight"><span class="block text-white font-semibold">Gestion du matériel</span>
                <span class="block text-xs" style="color:#8ea3c4">Lomé Business School</span></span>
            </a>
            <nav class="flex-1 overflow-y-auto px-3 pb-4">${groupes}</nav>
            <div class="border-t px-4 py-3 flex items-center gap-3" style="border-color:rgba(255,255,255,.1)">
                <a href="mon-compte.html" class="flex items-center gap-3 min-w-0 flex-1 group" title="Mon compte">
                    <span class="w-9 h-9 rounded-full grid place-items-center text-sm font-bold shrink-0" style="background:#2c4a78;color:#fff">${echapper(initiales || '?')}</span>
                    <span class="min-w-0 leading-tight">
                        <span class="block text-sm font-semibold text-white truncate group-hover:underline">${echapper(moi.nom)}</span>
                        <span class="block text-xs truncate" style="color:#8ea3c4">${echapper(sousTitre)}</span>
                    </span>
                </a>
                <button id="btnDeconnexion" class="bouton-icone" style="color:#b9c6da" title="Se déconnecter" aria-label="Se déconnecter">${icone('sortie', 'w-5 h-5')}</button>
            </div>
        </aside>
        <header class="barre-haut sticky z-20 pas-impression" style="top:env(safe-area-inset-top,0px)">
            <div class="max-w-7xl mx-auto px-4 md:px-6 h-14 flex items-center gap-2">
                <button id="btnMenu" class="bouton-icone lg:hidden" aria-label="Ouvrir le menu" aria-controls="menuLateral" aria-expanded="false">${icone('menu', 'w-5 h-5')}</button>
                <span class="lg:hidden flex items-center gap-2 font-semibold" style="color:var(--lbs-marine)">${logoHtml('h-7')}</span>
                <div class="flex-1"></div>
                <a href="scan.html" class="hidden sm:inline-flex items-center gap-2 h-9 px-3 rounded-lg text-sm font-medium text-white bg-blue-600 hover:bg-blue-700">${icone('scan', 'w-4 h-4')} Scanner</a>
                <button id="btnTheme" class="bouton-icone" aria-label="Changer de thème" title="Mode clair / sombre"></button>
                <div class="relative">
                    <button id="btnNotifs" class="bouton-icone" aria-label="Notifications" aria-expanded="false" aria-haspopup="true">${icone('cloche', 'w-5 h-5')}<span id="nbNotifs" class="pastille hidden"></span></button>
                    <div id="panneauNotifs" class="panneau-notifs hidden absolute right-0 mt-2 bg-white border border-slate-200 rounded-xl shadow-sm overflow-hidden z-50">
                        <div class="flex items-center justify-between px-4 py-3 border-b border-slate-200">
                            <strong class="text-sm text-slate-800">Notifications</strong>
                            <button id="btnToutLu" class="text-xs text-blue-600 hover:underline">Tout marquer comme lu</button>
                        </div>
                        <div id="listeNotifs" class="overflow-y-auto" style="max-height:calc(70vh - 3rem)"></div>
                    </div>
                </div>
            </div>
        </header>`;

    document.getElementById('btnDeconnexion').addEventListener('click', seDeconnecter);
    const menu = document.getElementById('menuLateral'), voile = document.getElementById('voile'), btnMenu = document.getElementById('btnMenu');
    const basculerMenu = ouvrir => {
        menu.classList.toggle('ouvert', ouvrir); voile.classList.toggle('hidden', !ouvrir);
        btnMenu.setAttribute('aria-expanded', ouvrir);
    };
    btnMenu.addEventListener('click', () => basculerMenu(!menu.classList.contains('ouvert')));
    voile.addEventListener('click', () => basculerMenu(false));

    const btnTheme = document.getElementById('btnTheme');
    const majIconeTheme = () => { btnTheme.innerHTML = icone(document.documentElement.classList.contains('dark') ? 'soleil' : 'lune', 'w-5 h-5'); };
    majIconeTheme();
    btnTheme.addEventListener('click', () => {
        const sombre = !document.documentElement.classList.contains('dark');
        document.documentElement.classList.toggle('dark', sombre);
        try { localStorage.setItem('theme', sombre ? 'sombre' : 'clair'); } catch (e) { /* stockage indisponible */ }
        majIconeTheme();
    });

    initialiserNotifications();
}

// ---------------------------------------------------------------------------
// Notifications (cloche)
// ---------------------------------------------------------------------------

const STYLE_NOTIF = {
    DEMANDE: '#22406e', RESERVATION: '#22406e', VALIDATION: '#1f7a4d', RETOUR: '#1f7a4d',
    REFUS: '#a3141c', RETARD: '#a3141c', TRANSFERT: '#b26a00', INFO: '#5a6478'
};

function initialiserNotifications() {
    const bouton = document.getElementById('btnNotifs'), panneau = document.getElementById('panneauNotifs');
    bouton.addEventListener('click', e => {
        e.stopPropagation();
        const ouvrir = panneau.classList.contains('hidden');
        panneau.classList.toggle('hidden', !ouvrir);
        bouton.setAttribute('aria-expanded', ouvrir);
        if (ouvrir) chargerNotifications();
    });
    document.addEventListener('click', e => {
        if (!panneau.contains(e.target)) { panneau.classList.add('hidden'); bouton.setAttribute('aria-expanded', false); }
    });
    document.getElementById('btnToutLu').addEventListener('click', async () => {
        await appelApi('/notifications/tout-lu', { method: 'POST' });
        chargerNotifications();
    });
    chargerNotifications();
    setInterval(chargerNotifications, 30000);
}

async function chargerNotifications() {
    const r = await appelApi('/notifications');
    if (!r || !r.success) return;
    const nb = document.getElementById('nbNotifs');
    nb.textContent = r.nonLues > 9 ? '9+' : r.nonLues;
    nb.classList.toggle('hidden', !r.nonLues);
    document.title = document.title.replace(/^\(\d+\+?\) /, '');
    if (r.nonLues) document.title = `(${r.nonLues > 9 ? '9+' : r.nonLues}) ${document.title}`;

    const liste = document.getElementById('listeNotifs');
    if (!r.data.length) { liste.innerHTML = etatVide('Aucune notification pour le moment.'); return; }
    liste.innerHTML = r.data.map(n => `
        <button data-id="${n.id}" data-lien="${echapper(n.lien || '')}" class="notif w-full text-left px-4 py-3 border-b border-slate-100 hover:bg-slate-50 flex gap-3 ${n.lue ? 'opacity-70' : ''}">
            <span class="mt-1.5 w-2 h-2 rounded-full shrink-0" style="background:${n.lue ? 'transparent' : (STYLE_NOTIF[n.categorie] || '#5a6478')}"></span>
            <span class="min-w-0">
                <span class="block text-sm ${n.lue ? 'text-slate-600' : 'font-semibold text-slate-800'}">${echapper(n.titre)}</span>
                <span class="block text-xs text-slate-600 mt-0.5">${echapper(n.message)}</span>
                <span class="block text-[11px] text-slate-400 mt-1">${formaterDepuis(n.dateCreation)}</span>
            </span>
        </button>`).join('');
    liste.querySelectorAll('.notif').forEach(b => b.addEventListener('click', async () => {
        await appelApi(`/notifications/${b.dataset.id}/lue`, { method: 'POST' });
        if (b.dataset.lien) window.location.href = b.dataset.lien; else chargerNotifications();
    }));
}

/** « il y a 5 min », « hier à 14:30 »... */
function formaterDepuis(iso) {
    const d = new Date(iso), s = (Date.now() - d.getTime()) / 1000;
    if (s < 60) return 'à l\'instant';
    if (s < 3600) return `il y a ${Math.floor(s / 60)} min`;
    if (s < 86400 && d.getDate() === new Date().getDate()) return `aujourd'hui à ${d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}`;
    return formaterDate(iso);
}

async function seDeconnecter() {
    await appelApi('/auth/logout', { method: 'POST', sansRedirection: true });
    window.location.href = 'login.html';
}

/**
 * Logo de l'école (img/logo-lbs.png), sur fond blanc pour rester lisible sur l'en-tête bleu.
 * Si le fichier est absent, un badge « LBS » le remplace.
 */
function logoHtml(hauteur) {
    return `<span class="rounded-lg px-1.5 py-1 inline-flex items-center shrink-0" style="background:#fff">
        <img src="img/logo-lbs.png" alt="Lomé Business School" class="${hauteur} w-auto"
             onerror="this.replaceWith(Object.assign(document.createElement('span'), {className: 'text-blue-700 font-bold text-sm px-1', textContent: 'LBS'}))">
    </span>`;
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

/** "lun. 28" : libellé court pour les axes de graphique. */
function formaterJourCourt(iso) {
    if (!iso) return '—';
    const d = new Date(iso + 'T00:00:00');
    return `${d.toLocaleDateString('fr-FR', { weekday: 'short' })} ${d.getDate()}`;
}

function formaterHeure(heure) {
    return heure ? heure.substring(0, 5) : null;
}

const STATUTS_FICHE = {
    RESERVEE: { texte: 'Réservée', classe: 'bg-blue-100 text-blue-800' },
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

// ---------------------------------------------------------------------------
// QR codes : chaque matériel a le sien (lien vers scan.html?code=...)
// ---------------------------------------------------------------------------

let _baseQr = null, _bibliothequeQr = null;

/** Adresse que les téléphones utiliseront (APP_URL si défini, sinon l'adresse de la page). */
async function adresseQr() {
    if (_baseQr) return _baseQr;
    const config = await appelApi('/config');
    _baseQr = (config && config.urlPublique) || location.origin;
    return _baseQr;
}

function lienScan(base, code) {
    return `${base}/scan.html?code=${encodeURIComponent(code)}`;
}

function adresseInjoignable(base) {
    return /\/\/(localhost|127\.0\.0\.1)(:|\/|$)/.test(base);
}

function chargerBibliothequeQr() {
    if (window.QRCode) return Promise.resolve(true);
    if (!_bibliothequeQr) {
        _bibliothequeQr = new Promise(resolve => {
            const script = document.createElement('script');
            script.src = 'https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js';
            script.onload = () => resolve(true);
            script.onerror = () => resolve(false);
            document.head.appendChild(script);
        });
    }
    return _bibliothequeQr;
}

/** Dessine le QR code du matériel dans l'élément (canvas + image générés par qrcodejs). */
async function dessinerQr(element, code, taille = 220) {
    const ok = await chargerBibliothequeQr();
    const base = await adresseQr();
    element.innerHTML = '';
    if (!ok || !window.QRCode) {
        element.innerHTML = '<span class="text-[10px] text-slate-400">QR indisponible hors ligne</span>';
        return;
    }
    new QRCode(element, { text: lienScan(base, code), width: taille, height: taille,
                          colorDark: '#152a49', colorLight: '#ffffff', correctLevel: QRCode.CorrectLevel.M });
}

/** Étiquette PNG prête à imprimer : QR code, désignation, code et nom de l'école. */
function telechargerEtiquette(conteneurQr, materiel) {
    const source = conteneurQr.querySelector('canvas');
    if (!source) return;
    const largeur = 600, marge = 40, cote = largeur - 2 * marge;
    const toile = document.createElement('canvas');
    toile.width = largeur; toile.height = cote + 2 * marge + 170;
    const ctx = toile.getContext('2d');
    ctx.fillStyle = '#ffffff'; ctx.fillRect(0, 0, toile.width, toile.height);
    ctx.imageSmoothingEnabled = false;
    ctx.drawImage(source, marge, marge, cote, cote);
    ctx.textAlign = 'center'; ctx.fillStyle = '#152a49';
    ctx.font = '600 30px "Public Sans", Arial, sans-serif';
    ctx.fillText(materiel.designation.slice(0, 34), largeur / 2, cote + marge + 55);
    ctx.font = '800 54px "Public Sans", Arial, sans-serif';
    ctx.fillText(materiel.codeUnique, largeur / 2, cote + marge + 115);
    ctx.fillStyle = '#5a6478'; ctx.font = '22px "Public Sans", Arial, sans-serif';
    ctx.fillText('Lomé Business School · scannez pour emprunter', largeur / 2, cote + marge + 155);
    const lien = document.createElement('a');
    lien.download = `QR-${materiel.codeUnique}.png`;
    lien.href = toile.toDataURL('image/png');
    lien.click();
}

/** Fenêtre avec le QR code d'un matériel : télécharger, imprimer, ouvrir la fiche. */
async function ouvrirQr(materiel, titre) {
    const fond = document.createElement('div');
    fond.className = 'fixed inset-0 bg-slate-900/50 z-50 flex items-center justify-center p-4';
    const durable = materiel.typeGestion === 'DURABLE';
    fond.innerHTML = `
        <div class="bg-white rounded-xl shadow-xl w-full max-w-sm p-5 space-y-4 text-center" role="dialog" aria-modal="true" aria-labelledby="titreQr">
            <div>
                <h2 id="titreQr" class="text-lg font-bold text-slate-800">${echapper(titre || materiel.designation)}</h2>
                ${titre ? `<p class="text-sm text-slate-500">${echapper(materiel.designation)}</p>` : ''}
            </div>
            <div class="zone-qr mx-auto bg-white p-3 rounded-lg border border-slate-200" style="width:min(260px,100%)"></div>
            <div>
                <div class="text-2xl font-extrabold tracking-wider text-slate-800">${echapper(materiel.codeUnique)}</div>
                <p class="text-xs text-slate-500 mt-1">${durable
                    ? 'Le délégué scanne ce code pour récupérer, rendre ou demander le matériel.'
                    : 'Le délégué scanne ce code pour demander cette fourniture au poste.'}</p>
                <p class="avert-qr hidden text-xs text-amber-800 bg-amber-50 border border-amber-200 rounded-lg p-2 mt-2 text-left"></p>
            </div>
            <div class="grid grid-cols-2 gap-2">
                <button data-png class="px-3 py-2.5 text-sm rounded-lg bg-blue-600 hover:bg-blue-700 text-white font-medium">Télécharger</button>
                <a href="etiquettes.html?code=${encodeURIComponent(materiel.codeUnique)}" class="px-3 py-2.5 text-sm rounded-lg border border-slate-300 text-slate-700 hover:bg-slate-50" style="text-decoration:none">Imprimer</a>
                ${materiel.id ? `<a href="fiche-materiel.html?id=${materiel.id}" class="col-span-2 px-3 py-2.5 text-sm rounded-lg border border-slate-300 text-slate-700 hover:bg-slate-50" style="text-decoration:none">${durable ? 'Voir le parcours' : 'Voir la fiche'}</a>` : ''}
            </div>
            <button data-fermer class="text-sm text-slate-500 hover:text-slate-800">Fermer</button>
        </div>`;
    const fermer = () => { fond.remove(); document.removeEventListener('keydown', echap); };
    const echap = e => { if (e.key === 'Escape') fermer(); };
    fond.addEventListener('click', e => { if (e.target === fond) fermer(); });
    fond.querySelector('[data-fermer]').addEventListener('click', fermer);
    document.addEventListener('keydown', echap);
    document.body.appendChild(fond);
    const zone = fond.querySelector('.zone-qr');
    await dessinerQr(zone, materiel.codeUnique, 512);
    zone.querySelectorAll('img, canvas').forEach(el => { el.style.width = '100%'; el.style.height = 'auto'; });
    fond.querySelector('[data-png]').addEventListener('click', () => telechargerEtiquette(zone, materiel));
    const base = await adresseQr();
    if (adresseInjoignable(base)) {
        const av = fond.querySelector('.avert-qr');
        av.textContent = `Ce QR code pointe vers ${base}, que les téléphones ne peuvent pas joindre. Lancez le serveur avec APP_URL (adresse réseau du PC) avant d'imprimer.`;
        av.classList.remove('hidden');
    }
    fond.querySelector('[data-png]').focus();
}

/** Accord simple : pluriel(3, 'emprunt') → « 3 emprunts ». */
function pluriel(n, mot, motPluriel) {
    return `${n} ${n > 1 ? (motPluriel || mot + 's') : mot}`;
}

/** Temps restant avant une échéance : « dans 1 h 20 », « dans 5 min », « en retard de 35 min ». */
function tempsRestant(iso) {
    if (!iso) return { texte: '', retard: false };
    const minutes = Math.round((new Date(iso).getTime() - Date.now()) / 60000);
    const duree = m => {
        m = Math.abs(m);
        if (m < 60) return `${m} min`;
        if (m < 1440) return `${Math.floor(m / 60)} h${m % 60 ? ' ' + String(m % 60).padStart(2, '0') : ''}`;
        return pluriel(Math.floor(m / 1440), 'jour');
    };
    if (minutes < 0) return { texte: `en retard de ${duree(minutes)}`, retard: true };
    if (minutes === 0) return { texte: 'maintenant', retard: false };
    return { texte: `dans ${duree(minutes)}`, retard: false, bientot: minutes <= 30 };
}

/** « Câble HDMI », « Câble HDMI et Micro », « Câble HDMI, Micro et 2 autres ». */
function resumeArticles(details) {
    const noms = (details || []).map(d => d.materiel.designation + (d.materiel.typeGestion === 'CONSOMMABLE' ? ` × ${d.quantite}` : ''));
    if (noms.length <= 2) return noms.join(' et ');
    return `${noms[0]}, ${noms[1]} et ${pluriel(noms.length - 2, 'autre')}`;
}

/** Date de réservation lisible : « aujourd'hui », « demain », « samedi 3 octobre ». */
function formaterJourReservation(jourIso) {
    const jour = new Date(jourIso + 'T00:00');
    const auj = new Date(); auj.setHours(0, 0, 0, 0);
    const ecart = Math.round((jour - auj) / 86400000);
    if (ecart === 0) return 'aujourd\'hui';
    if (ecart === 1) return 'demain';
    return jour.toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' });
}

/** Moment compact sur deux lignes : { jour: "aujourd'hui" | "hier" | "1 oct.", heure: "11:26" }. */
function momentCourt(iso) {
    const d = new Date(iso), auj = new Date();
    const hier = new Date(); hier.setDate(auj.getDate() - 1);
    const memeJour = (a, b) => a.toDateString() === b.toDateString();
    return {
        jour: memeJour(d, auj) ? 'auj.' : memeJour(d, hier) ? 'hier' : d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' }),
        heure: d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
    };
}

// ---------------------------------------------------------------------------
// WhatsApp, confiance, photos
// ---------------------------------------------------------------------------

/** Lien WhatsApp avec le message déjà écrit (aucun abonnement : wa.me ouvre l'appli). */
function lienWhatsApp(telephone, message) {
    return `https://wa.me/${encodeURIComponent(telephone)}?text=${encodeURIComponent(message)}`;
}

/** Message de rappel poli, adapté au retard ou à l'échéance proche. */
function messageRappel({ delegue, designation, code, heure, enRetard }) {
    const prenom = (delegue || '').split(/\s+/)[0];
    const quoi = `${designation}${code ? ` (${code})` : ''}`;
    return enRetard
        ? `Bonjour ${prenom}, ici le poste de surveillance de Lomé Business School. Le matériel ${quoi} était attendu${heure ? ` à ${heure}` : ''}. Merci de le rapporter au poste dès que possible.`
        : `Bonjour ${prenom}, ici le poste de surveillance de Lomé Business School. Petit rappel : le matériel ${quoi} est à rendre${heure ? ` avant ${heure}` : ''}. Merci !`;
}

/** Bouton WhatsApp (vert) ou rien si le délégué n'a pas donné de numéro. */
function boutonWhatsApp(telephone, message, compact) {
    if (!telephone) return '';
    return `<a href="${lienWhatsApp(telephone, message)}" target="_blank" rel="noopener" class="bouton-whatsapp ${compact ? 'compact' : ''}" title="Prévenir sur WhatsApp" style="text-decoration:none">
        <svg viewBox="0 0 24 24" class="w-4 h-4" aria-hidden="true" fill="currentColor"><path d="M12 2a10 10 0 0 0-8.6 15.1L2 22l5-1.3A10 10 0 1 0 12 2Zm0 18.2a8.2 8.2 0 0 1-4.2-1.2l-.3-.2-3 .8.8-2.9-.2-.3A8.2 8.2 0 1 1 12 20.2Zm4.5-6.1c-.2-.1-1.5-.7-1.7-.8-.2-.1-.4-.1-.6.1l-.8 1c-.1.2-.3.2-.5.1a6.7 6.7 0 0 1-3.3-2.9c-.3-.4.2-.4.7-1.3.1-.2 0-.3 0-.4l-.8-1.8c-.2-.5-.4-.4-.6-.4h-.5a1 1 0 0 0-.7.3 3 3 0 0 0-.9 2.2 5.2 5.2 0 0 0 1.1 2.8 11.9 11.9 0 0 0 4.6 4c1.7.7 2.4.8 3.2.6.5-.1 1.5-.6 1.7-1.2.2-.6.2-1.1.2-1.2-.1-.1-.3-.2-.5-.3Z"/></svg>
        ${compact ? '<span class="sr-only">WhatsApp</span>' : 'WhatsApp'}</a>`;
}

const NIVEAUX_CONFIANCE = {
    FIABLE: { texte: 'Fiable', classe: 'bg-green-100 text-green-700' },
    BON: { texte: 'Bon', classe: 'bg-blue-100 text-blue-700' },
    A_SURVEILLER: { texte: 'À surveiller', classe: 'bg-amber-100 text-amber-800' },
    NOUVEAU: { texte: 'Nouveau', classe: 'bg-slate-100 text-slate-600' }
};

function badgeConfiance(c) {
    if (!c) return '';
    if (c.bloque) return badge({ texte: 'Réservations en pause', classe: 'bg-red-100 text-red-700' });
    const n = NIVEAUX_CONFIANCE[c.niveau];
    return badge({ texte: n.texte, classe: n.classe });
}

function urlPhoto(id) {
    return `/api/photos/${id}`;
}

/** Réduit une photo dans le navigateur (1280 px au plus, JPEG) avant l'envoi : rapide même en 3G. */
function reduireImage(fichier, cote = 1280, qualite = 0.82) {
    return new Promise((resolve, reject) => {
        if (!fichier.type.startsWith('image/')) { reject(new Error('Ce fichier n\'est pas une image.')); return; }
        const lecteur = new FileReader();
        lecteur.onerror = () => reject(new Error('Lecture de la photo impossible.'));
        lecteur.onload = () => {
            const img = new Image();
            img.onerror = () => reject(new Error('Photo illisible.'));
            img.onload = () => {
                const echelle = Math.min(1, cote / Math.max(img.width, img.height));
                const toile = document.createElement('canvas');
                toile.width = Math.round(img.width * echelle);
                toile.height = Math.round(img.height * echelle);
                const ctx = toile.getContext('2d');
                ctx.fillStyle = '#fff'; ctx.fillRect(0, 0, toile.width, toile.height);
                ctx.drawImage(img, 0, 0, toile.width, toile.height);
                resolve(toile.toDataURL('image/jpeg', qualite));
            };
            img.src = lecteur.result;
        };
        lecteur.readAsDataURL(fichier);
    });
}

/** Visionneuse plein écran pour une série de photos. */
function ouvrirVisionneuse(ids, depart = 0) {
    let i = depart;
    const fond = document.createElement('div');
    fond.className = 'fixed inset-0 z-50 flex items-center justify-center p-4';
    fond.style.background = 'rgba(10,18,32,.92)';
    fond.setAttribute('role', 'dialog'); fond.setAttribute('aria-modal', 'true');
    fond.innerHTML = `
        <img class="max-w-full max-h-[85vh] rounded-lg shadow-2xl" alt="Photo">
        <button data-f class="absolute top-4 right-4 text-white text-sm px-3 py-2 rounded-lg" style="background:rgba(255,255,255,.12)">Fermer</button>
        ${ids.length > 1 ? `<button data-p class="absolute left-3 top-1/2 -translate-y-1/2 text-white text-2xl w-11 h-11 rounded-full" style="background:rgba(255,255,255,.12)" aria-label="Précédente">‹</button>
        <button data-n class="absolute right-3 top-1/2 -translate-y-1/2 text-white text-2xl w-11 h-11 rounded-full" style="background:rgba(255,255,255,.12)" aria-label="Suivante">›</button>
        <span data-c class="absolute bottom-4 left-1/2 -translate-x-1/2 text-white text-xs"></span>` : ''}`;
    const img = fond.querySelector('img');
    const montrer = () => { img.src = urlPhoto(ids[i]); const c = fond.querySelector('[data-c]'); if (c) c.textContent = `${i + 1} / ${ids.length}`; };
    const fermer = () => { fond.remove(); document.removeEventListener('keydown', clavier); };
    const aller = d => { i = (i + d + ids.length) % ids.length; montrer(); };
    const clavier = e => { if (e.key === 'Escape') fermer(); if (e.key === 'ArrowLeft') aller(-1); if (e.key === 'ArrowRight') aller(1); };
    fond.addEventListener('click', e => { if (e.target === fond) fermer(); });
    fond.querySelector('[data-f]').addEventListener('click', fermer);
    fond.querySelector('[data-p]')?.addEventListener('click', () => aller(-1));
    fond.querySelector('[data-n]')?.addEventListener('click', () => aller(1));
    document.addEventListener('keydown', clavier);
    document.body.appendChild(fond);
    montrer();
}

/** Carte « Ma confiance » du délégué : jauge, badge, détail et ce que ça lui permet. */
function carteConfianceHtml(c) {
    const n = NIVEAUX_CONFIANCE[c.niveau];
    const score = c.score ?? 0;
    const couleur = c.bloque ? '#a3141c' : c.niveau === 'FIABLE' ? '#1f7a4d' : c.niveau === 'A_SURVEILLER' ? '#d99a2b' : '#22406e';
    const tour = 2 * Math.PI * 34;
    const avantage = c.bloque ? c.motifBlocage
        : c.niveau === 'FIABLE' ? 'Badge « Fiable » : vous pouvez réserver jusqu\'à 60 jours à l\'avance.'
        : c.niveau === 'NOUVEAU' ? 'Rendez votre matériel à l\'heure : après 3 retours parfaits, vous obtenez le badge « Fiable ».'
        : c.niveau === 'A_SURVEILLER' ? `Trop de retards : réservations limitées à ${c.delaiReservationJours} jours à l'avance.`
        : `Encore quelques retours à l'heure pour le badge « Fiable » (réservations à 60 jours).`;
    return `
        <div class="flex items-center gap-4">
            <svg viewBox="0 0 80 80" class="w-20 h-20 shrink-0" role="img" aria-label="Score ${c.score ?? 'non calculé'} sur 100">
                <circle cx="40" cy="40" r="34" fill="none" stroke="var(--ligne)" stroke-width="7"/>
                <circle cx="40" cy="40" r="34" fill="none" stroke="${couleur}" stroke-width="7" stroke-linecap="round"
                        stroke-dasharray="${tour}" stroke-dashoffset="${tour * (1 - score / 100)}" transform="rotate(-90 40 40)"/>
                <text x="40" y="45" text-anchor="middle" font-size="18" font-weight="700" fill="currentColor">${c.score ?? '—'}</text>
            </svg>
            <div class="min-w-0 space-y-1">
                <div class="flex flex-wrap items-center gap-2"><span class="font-semibold text-slate-800">Score de confiance</span>${badgeConfiance(c)}</div>
                <p class="text-xs text-slate-500">${c.retours ? `${pluriel(c.retours, 'retour')} sur 90 jours : ${c.aLHeure} à l'heure, ${c.enRetard} en retard${c.endommages ? `, ${c.endommages} endommagé${c.endommages > 1 ? 's' : ''}` : ''}.` : 'Pas encore de retour enregistré.'}</p>
                <p class="text-sm ${c.bloque ? 'text-red-700 font-medium' : 'text-slate-700'}">${echapper(avantage)}</p>
            </div>
        </div>`;
}

// ---------------------------------------------------------------------------
// Photos du matériel : grille de 6 emplacements (ajouter, agrandir, supprimer)
// ---------------------------------------------------------------------------

const PHOTOS_MAX = 6;

/**
 * Affiche et gère les photos d'un matériel dans `conteneur`.
 * options.modifiable : boutons d'ajout et de suppression (agents) ; options.apresChangement(ids)
 */
async function gestionnairePhotos(conteneur, materielId, options = {}) {
    const modifiable = options.modifiable !== false;
    let ids = await appelApi(`/materiels/${materielId}/photos`);
    if (!Array.isArray(ids)) ids = [];

    const message = texte => {
        let zone = conteneur.querySelector('.message-photos');
        if (!zone) return;
        zone.textContent = texte || '';
        zone.classList.toggle('hidden', !texte);
    };

    const rendre = () => {
        const cases = ids.map((id, i) => `
            <div class="case-photo group">
                <button type="button" data-voir="${i}" class="w-full h-full" aria-label="Agrandir la photo ${i + 1}"><img src="${urlPhoto(id)}" alt="" loading="lazy"></button>
                ${modifiable ? `<button type="button" data-suppr="${id}" class="suppr-photo" aria-label="Supprimer la photo ${i + 1}" title="Supprimer">×</button>` : ''}
            </div>`).join('');
        const ajout = modifiable && ids.length < PHOTOS_MAX ? `
            <label class="case-photo case-ajout" title="Ajouter des photos">
                <input type="file" accept="image/*" multiple class="sr-only">
                ${icone('photo', 'w-6 h-6')}<span class="text-[11px] mt-1">Ajouter</span>
            </label>` : '';
        conteneur.innerHTML = `
            <div class="grille-photos">${cases}${ajout}</div>
            <p class="text-xs text-slate-500 mt-2">${ids.length
                ? `${pluriel(ids.length, 'photo')} sur ${PHOTOS_MAX}${modifiable && ids.length < PHOTOS_MAX ? ' · vous pouvez en ajouter ' + (PHOTOS_MAX - ids.length) : ''}.`
                : modifiable ? `Aucune photo. Ajoutez-en jusqu'à ${PHOTOS_MAX} pour que chacun reconnaisse ce matériel.` : 'Pas encore de photo.'}</p>
            <p class="message-photos hidden text-xs text-red-700 mt-1"></p>`;

        conteneur.querySelectorAll('[data-voir]').forEach(b => b.addEventListener('click', () => ouvrirVisionneuse(ids, parseInt(b.dataset.voir))));
        conteneur.querySelectorAll('[data-suppr]').forEach(b => b.addEventListener('click', async () => {
            if (!await confirmer('Supprimer la photo', 'Cette photo sera retirée de la fiche du matériel.', 'Supprimer', true)) return;
            const r = await appelApi(`/photos/${b.dataset.suppr}`, { method: 'DELETE' });
            if (!r.success) { message(r.message); return; }
            ids = ids.filter(x => String(x) !== b.dataset.suppr);
            rendre(); options.apresChangement?.(ids);
        }));
        const champ = conteneur.querySelector('input[type=file]');
        if (champ) champ.addEventListener('change', async () => {
            const fichiers = [...champ.files].slice(0, PHOTOS_MAX - ids.length);
            if (champ.files.length > fichiers.length) message(`Seules ${pluriel(fichiers.length, 'photo')} ont été gardées : ${PHOTOS_MAX} au plus par matériel.`);
            const ajoutCase = conteneur.querySelector('.case-ajout');
            if (ajoutCase) ajoutCase.innerHTML = '<span class="text-[11px]">Envoi…</span>';
            for (const f of fichiers) {
                try {
                    const donnees = await reduireImage(f);
                    const r = await appelApi(`/materiels/${materielId}/photos`, { method: 'POST', body: { donnees } });
                    if (r.success) ids.push(r.photoId); else { message(r.message); break; }
                } catch (e) { message(e.message); }
            }
            rendre(); options.apresChangement?.(ids);
        });
    };
    rendre();
    return ids;
}

/** Fenêtre « Photos de … » ouverte depuis le catalogue. */
function ouvrirPhotos(materiel, apresChangement) {
    const fond = document.createElement('div');
    fond.className = 'fixed inset-0 bg-slate-900/50 z-50 flex items-center justify-center p-4';
    fond.innerHTML = `
        <div class="bg-white rounded-xl shadow-xl w-full max-w-lg p-5 space-y-4" role="dialog" aria-modal="true" aria-labelledby="titrePhotos">
            <div class="flex items-start justify-between gap-3">
                <div><h2 id="titrePhotos" class="text-lg font-bold text-slate-800">Photos</h2>
                    <p class="text-sm text-slate-500">${echapper(materiel.designation)} · ${echapper(materiel.codeUnique || '')}</p></div>
                <button data-fermer class="text-sm text-slate-500 hover:text-slate-800 px-2 py-1">Fermer</button>
            </div>
            <div class="zone-photos"></div>
        </div>`;
    const fermer = () => { fond.remove(); document.removeEventListener('keydown', echap); };
    const echap = e => { if (e.key === 'Escape' && !document.querySelector('[aria-modal][role=dialog] img[alt=Photo]')) fermer(); };
    fond.addEventListener('click', e => { if (e.target === fond) fermer(); });
    fond.querySelector('[data-fermer]').addEventListener('click', fermer);
    document.addEventListener('keydown', echap);
    document.body.appendChild(fond);
    gestionnairePhotos(fond.querySelector('.zone-photos'), materiel.id, { apresChangement });
}
