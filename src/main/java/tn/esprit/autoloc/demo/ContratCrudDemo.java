package tn.esprit.autoloc.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tn.esprit.autoloc.domain.Agence;
import tn.esprit.autoloc.domain.CategorieVehicule;
import tn.esprit.autoloc.domain.Client;
import tn.esprit.autoloc.domain.Contrat;
import tn.esprit.autoloc.domain.ModePaiement;
import tn.esprit.autoloc.domain.Paiement;
import tn.esprit.autoloc.domain.Reservation;
import tn.esprit.autoloc.domain.StatutReservation;
import tn.esprit.autoloc.domain.StatutVehicule;
import tn.esprit.autoloc.domain.Vehicule;
import tn.esprit.autoloc.repository.IAgenceRepository;
import tn.esprit.autoloc.repository.IClientRepository;
import tn.esprit.autoloc.repository.IContratRepository;
import tn.esprit.autoloc.repository.IPaiementRepository;
import tn.esprit.autoloc.repository.IReservationRepository;
import tn.esprit.autoloc.repository.IVehiculeRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Programme de démonstration de l'Atelier 3 (fil rouge Contrat ↔ Paiement).
 * Il ne s'exécute qu'avec le profil "demo" :
 * <pre>mvnw spring-boot:run -Dspring-boot.run.profiles=demo</pre>
 * Chaque étape est précédée d'un titre dans les logs ; les requêtes SQL qui suivent
 * (logs Hibernate) montrent ce que déclenche chaque méthode du repository.
 */
@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class ContratCrudDemo implements CommandLineRunner {

    private final IAgenceRepository agenceRepository;
    private final IVehiculeRepository vehiculeRepository;
    private final IClientRepository clientRepository;
    private final IReservationRepository reservationRepository;
    private final IContratRepository contratRepository;
    private final IPaiementRepository paiementRepository;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(String... args) {
        String suffixe = String.valueOf(System.currentTimeMillis() % 1_000_000);

        titre("0. Données de base : agence, véhicule, client, réservations");
        Agence agence = agenceRepository.save(new Agence(null, "AutoLoc Tunis", "Tunis",
                "Avenue Habib Bourguiba", "71000000", null, null));
        Vehicule vehicule = vehiculeRepository.save(new Vehicule(null, "TN-" + suffixe, "Peugeot", "208",
                CategorieVehicule.CITADINE, new BigDecimal("90.00"), StatutVehicule.DISPONIBLE, agence, null, null));
        Client client = clientRepository.save(new Client(null, "Ben Ali", "Sami", "sami" + suffixe + "@mail.tn",
                "98765432", "P-" + suffixe, LocalDate.now(), null));
        Reservation r1 = creerReservation(vehicule, client, 1);
        Reservation r2 = creerReservation(vehicule, client, 10);
        Reservation r3 = creerReservation(vehicule, client, 20);

        titre("1. CREATE - save() d'un contrat nouveau (id null) => persist : INSERT contrat + INSERT paiements (cascade)");
        Contrat contrat = new Contrat();
        contrat.setDateSignature(LocalDate.now());
        contrat.setMontantTotal(new BigDecimal("450.00"));
        contrat.setValide(true);
        contrat.setReservation(r1);
        contrat.ajouterPaiement(paiement("200.00", ModePaiement.CARTE));
        contrat.ajouterPaiement(paiement("250.00", ModePaiement.ESPECES));
        Contrat enregistre = contratRepository.save(contrat);
        Long idContrat = enregistre.getIdContrat();
        log.info(">>> Identifiant attribué par MariaDB (IDENTITY) : {}", idContrat);

        titre("2. READ - findById() renvoie un Optional, existsById(), count()");
        Optional<Contrat> trouve = contratRepository.findById(idContrat);
        log.info(">>> findById({}) présent ? {}", idContrat, trouve.isPresent());
        log.info(">>> findById(-1) présent ? {}", contratRepository.findById(-1L).isPresent());
        log.info(">>> existsById({}) = {}", idContrat, contratRepository.existsById(idContrat));
        log.info(">>> count() = {}", contratRepository.count());

        titre("3. UPDATE - save() d'un contrat existant (id renseigné) => merge : SELECT puis UPDATE");
        Contrat detache = trouve.orElseThrow();
        detache.setMontantTotal(new BigDecimal("500.00"));
        Contrat misAJour = contratRepository.save(detache);
        log.info(">>> Objet retourné identique à l'objet passé ? {} (toujours utiliser l'objet retourné)",
                misAJour == detache);

        titre("4. orphanRemoval - retirer un paiement de la liste du contrat => DELETE du paiement");
        transactionTemplate.executeWithoutResult(status -> {
            Contrat c = contratRepository.findById(idContrat).orElseThrow();
            c.retirerPaiement(c.getPaiements().get(0));
        });
        log.info(">>> Paiements restants en base : {}", paiementRepository.count());

        titre("5. getReferenceById() - proxy sans SELECT, utile pour renseigner une clé étrangère");
        Contrat contrat2 = nouveauContrat(reservationRepository.getReferenceById(r2.getIdReservation()), "300.00");
        contrat2.ajouterPaiement(paiement("300.00", ModePaiement.VIREMENT));
        contrat2 = contratRepository.save(contrat2);
        Contrat contrat3 = contratRepository.save(
                nouveauContrat(reservationRepository.getReferenceById(r3.getIdReservation()), "120.00"));

        titre("6. JpaRepository - findAll() renvoie une List ; tri et pagination");
        List<Contrat> tous = contratRepository.findAll();
        log.info(">>> findAll() : {} contrat(s) dans une List", tous.size());
        Sort tri = Sort.by("montantTotal").descending();
        Page<Contrat> page = contratRepository.findAll(PageRequest.of(0, 2, tri));
        log.info(">>> Page 0 (taille 2) : {} élément(s) / {} au total, {} page(s)",
                page.getContent().size(), page.getTotalElements(), page.getTotalPages());
        page.forEach(c -> log.info(">>>   contrat {} - montant {}", c.getIdContrat(), c.getMontantTotal()));

        titre("7. DELETE - deleteById() : SELECT puis DELETE des paiements (cascade REMOVE) puis du contrat");
        contratRepository.deleteById(idContrat);
        log.info(">>> existsById({}) après suppression = {}", idContrat, contratRepository.existsById(idContrat));
        contratRepository.deleteById(-1L);
        log.info(">>> deleteById(-1) : aucune exception (Spring Data 3+)");

        titre("8. Limite des suppressions en lot - deleteAllByIdInBatch() contourne la cascade");
        try {
            contratRepository.deleteAllByIdInBatch(List.of(contrat2.getIdContrat()));
        } catch (DataIntegrityViolationException e) {
            log.info(">>> Échec attendu : le paiement référence encore le contrat (contrainte de clé étrangère)");
        }

        titre("9. Nettoyage");
        contratRepository.deleteAll(List.of(contrat2, contrat3));
        log.info(">>> Contrats restants : {}, paiements restants : {}",
                contratRepository.count(), paiementRepository.count());
    }

    private Reservation creerReservation(Vehicule vehicule, Client client, int decalageJours) {
        LocalDate debut = LocalDate.now().plusDays(decalageJours);
        return reservationRepository.save(new Reservation(null, debut, debut.plusDays(3),
                StatutReservation.CONFIRMEE, vehicule, client, null));
    }

    private static Contrat nouveauContrat(Reservation reservation, String montant) {
        Contrat contrat = new Contrat();
        contrat.setDateSignature(LocalDate.now());
        contrat.setMontantTotal(new BigDecimal(montant));
        contrat.setValide(true);
        contrat.setReservation(reservation);
        return contrat;
    }

    private static Paiement paiement(String montant, ModePaiement mode) {
        Paiement paiement = new Paiement();
        paiement.setMontant(new BigDecimal(montant));
        paiement.setDatePaiement(LocalDate.now());
        paiement.setModePaiement(mode);
        return paiement;
    }

    private static void titre(String texte) {
        log.info("==================== {} ====================", texte);
    }
}
