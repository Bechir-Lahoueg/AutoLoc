package tn.esprit.autoloc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.autoloc.domain.Client;
import tn.esprit.autoloc.exception.ResourceNotFoundException;
import tn.esprit.autoloc.repository.IClientRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements IClientService {

    private static final String RESSOURCE = "Client";

    private final IClientRepository clientRepository;

    @Override
    public Client create(Client client) {
        Validations.exigerNouveau(client.getIdClient(), RESSOURCE);
        valider(client);
        return clientRepository.save(client);
    }

    @Override
    public Client findById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESSOURCE, id));
    }

    @Override
    public List<Client> findAll() {
        return clientRepository.findAll();
    }

    @Override
    public Client update(Long id, Client client) {
        valider(client);
        Client existant = findById(id);
        existant.setNom(client.getNom());
        existant.setPrenom(client.getPrenom());
        existant.setEmail(client.getEmail());
        existant.setTelephone(client.getTelephone());
        existant.setNumPermis(client.getNumPermis());
        existant.setDateInscription(client.getDateInscription());
        return clientRepository.save(existant);
    }

    @Override
    public void deleteById(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new ResourceNotFoundException(RESSOURCE, id);
        }
        clientRepository.deleteById(id);
    }

    private static void valider(Client client) {
        Validations.exigerTexte(client.getNom(), "nom");
        Validations.exigerTexte(client.getPrenom(), "prenom");
        Validations.exigerTexte(client.getEmail(), "email");
        Validations.exigerTexte(client.getNumPermis(), "numPermis");
    }
}
