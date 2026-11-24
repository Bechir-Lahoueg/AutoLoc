package tn.esprit.autoloc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.autoloc.domain.Employe;
import tn.esprit.autoloc.exception.ResourceNotFoundException;
import tn.esprit.autoloc.repository.IEmployeRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeServiceImpl implements IEmployeService {

    private static final String RESSOURCE = "Employe";

    private final IEmployeRepository employeRepository;

    @Override
    public Employe create(Employe employe) {
        Validations.exigerNouveau(employe.getIdEmploye(), RESSOURCE);
        Validations.exigerValeur(employe.getAgence(), "agence");
        valider(employe);
        return employeRepository.save(employe);
    }

    @Override
    public Employe findById(Long id) {
        return employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESSOURCE, id));
    }

    @Override
    public List<Employe> findAll() {
        return employeRepository.findAll();
    }

    @Override
    public Employe update(Long id, Employe employe) {
        valider(employe);
        Employe existant = findById(id);
        existant.setNom(employe.getNom());
        existant.setPrenom(employe.getPrenom());
        existant.setRole(employe.getRole());
        return employeRepository.save(existant);
    }

    @Override
    public void deleteById(Long id) {
        if (!employeRepository.existsById(id)) {
            throw new ResourceNotFoundException(RESSOURCE, id);
        }
        employeRepository.deleteById(id);
    }

    private static void valider(Employe employe) {
        Validations.exigerTexte(employe.getNom(), "nom");
        Validations.exigerTexte(employe.getPrenom(), "prenom");
        Validations.exigerValeur(employe.getRole(), "role");
    }
}
