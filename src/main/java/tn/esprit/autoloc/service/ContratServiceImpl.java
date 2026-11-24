package tn.esprit.autoloc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.autoloc.domain.Contrat;
import tn.esprit.autoloc.exception.ResourceNotFoundException;
import tn.esprit.autoloc.repository.IContratRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContratServiceImpl implements IContratService {

    private static final String RESSOURCE = "Contrat";

    private final IContratRepository contratRepository;

    @Override
    public Contrat create(Contrat contrat) {
        Validations.exigerNouveau(contrat.getIdContrat(), RESSOURCE);
        Validations.exigerValeur(contrat.getReservation(), "reservation");
        valider(contrat);
        return contratRepository.save(contrat);
    }

    @Override
    public Contrat findById(Long id) {
        return contratRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESSOURCE, id));
    }

    @Override
    public List<Contrat> findAll() {
        return contratRepository.findAll();
    }

    @Override
    public Contrat update(Long id, Contrat contrat) {
        valider(contrat);
        Contrat existant = findById(id);
        existant.setDateSignature(contrat.getDateSignature());
        existant.setMontantTotal(contrat.getMontantTotal());
        existant.setValide(contrat.isValide());
        // identifiant, réservation et collection de paiements ne sont pas recopiés
        return contratRepository.save(existant);
    }

    @Override
    public void deleteById(Long id) {
        if (!contratRepository.existsById(id)) {
            throw new ResourceNotFoundException(RESSOURCE, id);
        }
        // la cascade REMOVE et l'orphanRemoval suppriment aussi les paiements du contrat
        contratRepository.deleteById(id);
    }

    private static void valider(Contrat contrat) {
        Validations.exigerValeur(contrat.getDateSignature(), "dateSignature");
        Validations.exigerPositifOuNul(contrat.getMontantTotal(), "montantTotal");
    }
}
