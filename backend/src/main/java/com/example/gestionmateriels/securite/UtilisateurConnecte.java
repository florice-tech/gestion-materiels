package com.example.gestionmateriels.securite;

import com.example.gestionmateriels.model.Agent;
import com.example.gestionmateriels.model.Delegue;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utilisateur authentifié (agent ou délégué), conservé dans la session serveur.
 * Rôles Spring Security : ROLE_AGENT (+ ROLE_ADMIN pour un administrateur) ou ROLE_DELEGUE.
 */
public class UtilisateurConnecte implements UserDetails {

    public enum Type { AGENT, DELEGUE }

    private final Long id;
    private final Type type;
    private final String identifiant;
    private final String nom;
    private final String motDePasse;
    private final boolean actif;
    private final boolean administrateur;
    // Intitulé du poste pour un agent, filière/niveau pour un délégué
    private final String complement;

    private UtilisateurConnecte(Long id, Type type, String identifiant, String nom, String motDePasse,
                                boolean actif, boolean administrateur, String complement) {
        this.id = id;
        this.type = type;
        this.identifiant = identifiant;
        this.nom = nom;
        this.motDePasse = motDePasse;
        this.actif = actif;
        this.administrateur = administrateur;
        this.complement = complement;
    }

    public static UtilisateurConnecte depuis(Agent a) {
        return new UtilisateurConnecte(a.getId(), Type.AGENT, a.getIdentifiant(), a.getNom(), a.getMotDePasse(),
                a.isActif(), a.isAdministrateur(), a.getRole());
    }

    public static UtilisateurConnecte depuis(Delegue d) {
        return new UtilisateurConnecte(d.getId(), Type.DELEGUE, d.getIdentifiant(), d.getNom(), d.getMotDePasse(),
                d.isActif(), false, d.getFiliereNiveau());
    }

    public boolean estAgent() { return type == Type.AGENT; }
    public boolean estDelegue() { return type == Type.DELEGUE; }

    public Long getId() { return id; }
    public Type getType() { return type; }
    public String getNom() { return nom; }
    public boolean isAdministrateur() { return administrateur; }
    public String getComplement() { return complement; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> roles = new ArrayList<>();
        if (estAgent()) {
            roles.add(new SimpleGrantedAuthority("ROLE_AGENT"));
            if (administrateur) {
                roles.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }
        } else {
            roles.add(new SimpleGrantedAuthority("ROLE_DELEGUE"));
        }
        return roles;
    }

    @Override public String getPassword() { return motDePasse; }
    @Override public String getUsername() { return identifiant; }
    @Override public boolean isEnabled() { return actif; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}
