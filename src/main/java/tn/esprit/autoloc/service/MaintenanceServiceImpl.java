package tn.esprit.autoloc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.autoloc.domain.Maintenance;
import tn.esprit.autoloc.exception.ResourceNotFoundException;
import tn.esprit.autoloc.repository.IMaintenanceRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceServiceImpl implements IMaintenanceService {

    private static final String RESSOURCE = "Maintenance";

    private final IMaintenanceRepository maintenanceRepository;

    @Override
    public Maintenance create(Maintenance maintenance) {
        Validations.exigerNouveau(maintenance.getIdMaintenance(), RESSOURCE);
        Validations.exigerValeur(maintenance.getVehicule(), "vehicule");
        valider(maintenance);
        return maintenanceRepository.save(maintenance);
    }

    @Override
    public Maintenance findById(Long id) {
        return maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESSOURCE, id));
    }

    @Override
    public List<Maintenance> findAll() {
        return maintenanceRepository.findAll();
    }

    @Override
    public Maintenance update(Long id, Maintenance maintenance) {
        valider(maintenance);
        Maintenance existant = findById(id);
        existant.setDateDebut(maintenance.getDateDebut());
        existant.setDateFin(maintenance.getDateFin());
        existant.setDescription(maintenance.getDescription());
        return maintenanceRepository.save(existant);
    }

    @Override
    public void deleteById(Long id) {
        if (!maintenanceRepository.existsById(id)) {
            throw new ResourceNotFoundException(RESSOURCE, id);
        }
        maintenanceRepository.deleteById(id);
    }

    private static void valider(Maintenance maintenance) {
        Validations.exigerValeur(maintenance.getDateDebut(), "dateDebut");
        Validations.exigerPeriodeValide(maintenance.getDateDebut(), maintenance.getDateFin());
    }
}
