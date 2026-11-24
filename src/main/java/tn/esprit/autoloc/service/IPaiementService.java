package tn.esprit.autoloc.service;

import tn.esprit.autoloc.domain.Paiement;

import java.util.List;

/** Lecture seule : un paiement est créé ou retiré uniquement à travers son contrat (composition). */
public interface IPaiementService {

    Paiement findById(Long id);

    List<Paiement> findAll();
}
