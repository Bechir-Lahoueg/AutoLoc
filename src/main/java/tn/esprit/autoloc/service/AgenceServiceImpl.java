package tn.esprit.autoloc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.autoloc.domain.Agence;
import tn.esprit.autoloc.exception.ResourceNotFoundException;
import tn.esprit.autoloc.repository.IAgenceRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgenceServiceImpl implements IAgenceService {

    private static final String RESSOURCE = "Agence";

    private final IAgenceRepository agenceRepository;

    @Override
    public Agence create(Agence agence) {
        Validations.exigerNouveau(agence.getIdAgence(), RESSOURCE);
        valider(agence);
        return agenceRepository.save(agence);
    }

    @Override
    public Agence findById(Long id) {
        return agenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESSOURCE, id));
    }

    @Override
    public List<Agence> findAll() {
        return agenceRepository.findAll();
    }

    @Override
    public Agence update(Long id, Agence agence) {
        valider(agence);
        Agence existant = findById(id);
        existant.setNom(agence.getNom());
        existant.setVille(agence.getVille());
        existant.setAdresse(agence.getAdresse());
        existant.setTelephone(agence.getTelephone());
        return agenceRepository.save(existant);
    }

    @Override
    public void deleteById(Long id) {
        if (!agenceRepository.existsById(id)) {
            throw new ResourceNotFoundException(RESSOURCE, id);
        }
        agenceRepository.deleteById(id);
    }

    private static void valider(Agence agence) {
        Validations.exigerTexte(agence.getNom(), "nom");
        Validations.exigerTexte(agence.getVille(), "ville");
        Validations.exigerTexte(agence.getAdresse(), "adresse");
    }
}
