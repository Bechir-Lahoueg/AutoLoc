# Atelier 4 — IoC, injection de dépendances et couche Service

Packages ajoutés : `tn.esprit.autoloc.exception` (`ResourceNotFoundException`) et `tn.esprit.autoloc.service`
(9 interfaces `I…Service`, 9 implémentations `…ServiceImpl`, et la classe utilitaire `Validations` qui
regroupe les règles de gestion communes).

Au démarrage, l'application affiche toujours `Found 9 JPA repository interfaces`. Les logs du conteneur
(`DefaultListableBeanFactory` en DEBUG) montrent la création des 9 singletons `agenceServiceImpl`,
`clientServiceImpl`, `contratServiceImpl`, … et leur câblage par constructeur
(*Autowiring by type from bean name 'contratServiceImpl' via constructor to bean named 'IContratRepository'*).

## A. Modes d'injection comparés (étape 2)

| Critère | Constructeur (`@RequiredArgsConstructor` + champ `final`) | Attribut (`@Autowired private IContratRepository …`) |
|---|---|---|
| Champ `final` possible ? | Oui | Non : le champ est affecté par réflexion après la construction |
| Dépendance visible dans la signature de la classe ? | Oui, elle fait partie du constructeur | Non, elle est cachée dans un champ privé |
| Utilisable avec `new` hors conteneur sans dépendance `null` ? | Oui : `new ContratServiceImpl(repo)` est obligatoire et complet | Non : `new ContratServiceImpl()` compile mais `contratRepository` reste `null` (NullPointerException au premier appel) |
| Signalé par SonarQube for IDE ? | Non | Oui, règle **java:S6813** *Field dependency injection should be avoided* |
| L'application démarre ? | Oui | Oui (Spring sait injecter dans un champ) |

Conclusion : l'injection par constructeur est retenue pour tous les services AutoLoc.

## B. Services

| Service | Dépendances injectées | Mode | Justification |
|---|---|---|---|
| `ContratServiceImpl` | `IContratRepository` | Constructeur | CRUD du contrat ; refuse un id non `null` à la création et un `montantTotal` négatif ; la suppression supprime les paiements par cascade. |
| `PaiementServiceImpl` | `IPaiementRepository` | Constructeur | Lecture seule (`findById`, `findAll`) : un paiement se crée et se retire via son contrat (composition). |
| `AgenceServiceImpl` | `IAgenceRepository` | Constructeur | Refuse une agence dont le `nom`, la `ville` ou l'`adresse` est vide. |
| `EmployeServiceImpl` | `IEmployeRepository` | Constructeur | Refuse un employé sans `nom`, `prenom` ou `role`. |
| `VehiculeServiceImpl` | `IVehiculeRepository` | Constructeur | Refuse une immatriculation vide et un `tarifJournalier` négatif. |
| `EquipementServiceImpl` | `IEquipementRepository` | Constructeur | Refuse un équipement sans `libelle` (le modèle n'a pas d'attribut quantité). |
| `ClientServiceImpl` | `IClientRepository` | Constructeur | Refuse un client sans `nom`, `prenom`, `email` ou `numPermis`. |
| `ReservationServiceImpl` | `IReservationRepository` | Constructeur | Refuse une `dateFin` antérieure à la `dateDebut`. |
| `MaintenanceServiceImpl` | `IMaintenanceRepository` | Constructeur | Refuse une `dateFin` antérieure à la `dateDebut` (le modèle n'a pas d'attribut coût). |

Pour chaque service :
- une seule dépendance, `final`, typée par l'interface du repository ;
- aucun attribut modifiable (service sans état) ;
- aucun `new` de repository.

Règles communes :
- `create` refuse un identifiant non `null` : sinon `save` ferait un `merge` qui écraserait une ligne existante ;
- `findById` lève `ResourceNotFoundException` quand l'entité est absente ;
- `update` charge l'existant puis recopie les attributs modifiables, sans toucher ni à l'identifiant ni aux collections ;
- `deleteById` vérifie l'existence, puisque le `deleteById` du repository ne lève rien si l'id n'existe pas.

Les liens entre entités (réservation d'un contrat, agence d'un véhicule…) ne sont pas recopiés par `update` :
leur gestion est l'objet du Chapitre 9.

## C. Messages d'erreur (étape 3)

**Message 1 — `required a bean of type 'IContratService' that could not be found`**
- Exception : `NoSuchBeanDefinitionException`, enveloppée dans une `UnsatisfiedDependencyException`.
- Bean concerné : `ContratController`, paramètre 0 de son constructeur.
- Causes probables côté `ContratServiceImpl` :
  1. l'annotation `@Service` a été oubliée, donc la classe n'est pas un bean ;
  2. la classe est placée hors du package racine `tn.esprit.autoloc` (ou de ses sous-packages), donc le scan ne la voit pas.
- Correction : ajouter `@Service` et placer la classe dans `tn.esprit.autoloc.service`.

**Message 2 — `required a single bean, but 2 were found: emailNotificateur, smsNotificateur`**
- Exception : `NoUniqueBeanDefinitionException`. Deux beans implémentent le type demandé par `NotificationController`.
- Correction 1 : poser `@Primary` **sur la classe** choisie par défaut (ex. `@Service @Primary class EmailNotificateur`).
- Correction 2 : poser `@Qualifier("smsNotificateur")` **sur le paramètre du constructeur** de `NotificationController`
  (le nom du bean est celui de la classe avec une minuscule initiale). Avec Lombok `@RequiredArgsConstructor`, il faut
  ajouter `lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier` dans
  `lombok.config`, ou écrire le constructeur à la main.

**Message 3 — cycle `clientServiceImpl` ↔ `reservationServiceImpl`**
- Exception : `BeanCurrentlyInCreationException` (dépendance circulaire).
- Pourquoi l'injection par constructeur rend le cycle impossible à contourner : pour construire `clientServiceImpl`,
  Spring doit d'abord lui passer un `reservationServiceImpl` déjà construit. Mais celui-ci exige à son tour un
  `clientServiceImpl` déjà construit. Aucun des deux objets ne peut exister en premier. Avec l'injection par attribut,
  Spring pourrait créer un objet incomplet puis le compléter, ce qui ne fait que masquer le défaut de conception.
- Pistes de conception :
  - faire dépendre les services des **repositories** plutôt que l'un de l'autre (ex. `ReservationServiceImpl` utilise `IClientRepository`) ;
  - ou extraire la responsabilité commune dans un troisième service dont les deux dépendent ;
  - les dépendances doivent aller dans un seul sens.

## D. Anomalies SonarQube for IDE

| Anomalie | Règle / explication | Correction apportée |
|---|---|---|
| `@Autowired private IContratRepository contratRepository;` (étape 2) | **java:S6813** — *Field dependency injection should be avoided*. La dépendance est cachée, le champ ne peut pas être `final` et l'objet créé par `new` reste incomplet. | Retour à l'injection par constructeur : champ `private final` + `@RequiredArgsConstructor`. |
| `throw new RuntimeException(...)` dans `findById` (étape 4) | **java:S112** — *Generic exceptions should never be thrown*. L'appelant ne peut pas distinguer « contrat absent » d'une autre erreur sans analyser le message. | Exception métier dédiée `ResourceNotFoundException` (traduite en 404 à l'Atelier 5). |
| Littéral `"Contrat"` répété 3 fois (`create`, `findById`, `deleteById`) | **java:S1192** — *String literals should not be duplicated*. Une faute de frappe dans l'une des copies produirait un message incohérent. | Constante `private static final String RESSOURCE = "Contrat";` dans chaque service. |
| Classe utilitaire `Validations` sans constructeur | **java:S1118** — *Utility classes should not have public constructors*. Le constructeur public implicite permettrait d'instancier une classe purement statique. | Classe `final` avec un constructeur `private`. |

## E. Questions de compréhension

1. **Où est le `new` de `ContratServiceImpl` et qui l'exécute ?** Il n'apparaît nulle part dans notre code. C'est le
   conteneur Spring (`ApplicationContext`, créé par `SpringApplication.run`) qui le détecte grâce à `@Service` lors du
   scan, puis appelle par réflexion le constructeur généré par Lombok en lui passant le proxy de `IContratRepository`.
2. **Pourquoi le contrôleur dépendra-t-il de `IContratService` ?** Pour dépendre d'une abstraction et non d'une
   implémentation. On peut changer d'implémentation (ou fournir un double de test) sans modifier le contrôleur, et
   Spring peut injecter un proxy (transactions, sécurité) qui implémente l'interface.
3. **Pourquoi un service singleton doit-il rester sans état ?** Une seule instance est partagée par toutes les requêtes,
   qui s'exécutent dans des threads concurrents. Exemple de bug : un attribut `private Contrat contratCourant;`
   affecté dans `update`. Si deux utilisateurs modifient un contrat en même temps, le second écrase `contratCourant`
   et le premier enregistre le contrat de l'autre.
4. **Pourquoi `update` charge-t-il l'entité existante ?** L'objet reçu (venant du client) est incomplet : sans id,
   et sans les collections ni les associations. Le passer à `save` déclencherait un `merge` qui écraserait toutes les
   colonnes avec ses valeurs, parfois `null`. Avec `orphanRemoval`, sa liste de paiements vide supprimerait aussi tous
   les paiements du contrat. En chargeant l'existant puis en recopiant seulement les champs modifiables, on garde le reste intact.
5. **`@Component` / `@Bean`, `@Primary` / `@Qualifier`, et `IPaiementService` :**
   - `@Component` se pose sur une classe **que l'on écrit** et qui est détectée par le scan. `@Bean` se pose sur une
     méthode d'une classe `@Configuration` qui crée l'objet elle-même : c'est ce qu'on utilise pour une classe de
     bibliothèque qu'on ne peut pas annoter, ou pour une construction particulière.
   - `@Primary` se pose sur la classe et désigne le candidat **par défaut** quand plusieurs beans ont le même type.
     `@Qualifier("nom")` se pose au point d'injection et fait un **choix explicite** ; il l'emporte sur `@Primary`.
   - `IPaiementService` est en lecture seule parce que Paiement est une **composition** de Contrat : un paiement
     n'existe pas sans son contrat. Il est créé par `Contrat.ajouterPaiement`, supprimé par `Contrat.retirerPaiement`
     (orphanRemoval) ou par la cascade quand on supprime le contrat. Exposer ces opérations dans `IPaiementService`
     permettrait de contourner la logique et les règles du contrat.
