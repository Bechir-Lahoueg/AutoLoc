package tn.esprit.autoloc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.autoloc.domain.Reservation;
import tn.esprit.autoloc.exception.ResourceNotFoundException;
import tn.esprit.autoloc.repository.IReservationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements IReservationService {

    private static final String RESSOURCE = "Reservation";

    private final IReservationRepository reservationRepository;

    @Override
    public Reservation create(Reservation reservation) {
        Validations.exigerNouveau(reservation.getIdReservation(), RESSOURCE);
        valider(reservation);
        return reservationRepository.save(reservation);
    }

    @Override
    public Reservation findById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESSOURCE, id));
    }

    @Override
    public List<Reservation> findAll() {
        return reservationRepository.findAll();
    }

    @Override
    public Reservation update(Long id, Reservation reservation) {
        valider(reservation);
        Reservation existant = findById(id);
        existant.setDateDebut(reservation.getDateDebut());
        existant.setDateFin(reservation.getDateFin());
        existant.setStatut(reservation.getStatut());
        return reservationRepository.save(existant);
    }

    @Override
    public void deleteById(Long id) {
        if (!reservationRepository.existsById(id)) {
            throw new ResourceNotFoundException(RESSOURCE, id);
        }
        reservationRepository.deleteById(id);
    }

    private static void valider(Reservation reservation) {
        Validations.exigerValeur(reservation.getDateDebut(), "dateDebut");
        Validations.exigerValeur(reservation.getDateFin(), "dateFin");
        Validations.exigerPeriodeValide(reservation.getDateDebut(), reservation.getDateFin());
    }
}
