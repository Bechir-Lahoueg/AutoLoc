# Atelier 5 — API REST, tests Postman et documentation Swagger

- URL de base : `http://localhost:8080/api` (préfixe fixé une seule fois par `server.servlet.context-path=/api`).
- Documentation : `http://localhost:8080/api/swagger-ui.html` et `http://localhost:8080/api/v3/api-docs` (SpringDoc 3.2.1, compatible Spring Boot 4).
- Collection Postman : `postman/AutoLoc.postman_collection.json` (10 dossiers, 63 requêtes avec assertions).
  Environnement : `postman/local.postman_environment.json` (`baseUrl = http://localhost:8080`).
  Pour les importer dans Postman : *Import*, puis sélectionner les deux fichiers.
- Résultat de l'exécution complète de la collection : **63 requêtes conformes, 0 en échec**. Elle couvre les
  cas nominaux, les 404, les 400, le 405, la cascade Contrat → Paiements et la remise à zéro finale.

Fichiers ajoutés dans `web.controller` : les 9 contrôleurs, `GlobalExceptionHandler`, `OpenApiConfig` et `SecurityConfig`.
- `GlobalExceptionHandler` : `ResourceNotFoundException` donne 404, `IllegalArgumentException` donne 400, avec le corps `{statut, message, horodatage}`.
- `SecurityConfig` est indispensable parce que `spring-boot-starter-security` est présent depuis l'Atelier 1. Sans lui,
  toute requête reçoit 401, et les POST/PUT/DELETE reçoivent 403 à cause du contrôle CSRF.

## A. Endpoints

Codes d'erreur communs : 404 pour une ressource absente, 400 pour un identifiant fourni à la création ou une règle de
gestion non respectée.

| Contrôleur | Verbe et route | Service appelé | Succès | Erreurs |
|---|---|---|---|---|
| ContratController | GET /api/contrats | `findAll()` | 200 | — |
| ContratController | GET /api/contrats/{id} | `findById(id)` | 200 | 404 |
| ContratController | POST /api/contrats | `create(contrat)` | 201 | 400 (id fourni, réservation absente, montant négatif) |
| ContratController | PUT /api/contrats/{id} | `update(id, contrat)` | 200 | 404, 400 |
| ContratController | DELETE /api/contrats/{id} | `deleteById(id)` | 204 | 404 |
| PaiementController | GET /api/paiements | `findAll()` | 200 | — |
| PaiementController | GET /api/paiements/{id} | `findById(id)` | 200 | 404 |
| PaiementController | POST/PUT/DELETE /api/paiements… | aucun | — | 405 (méthode non exposée) |
| AgenceController | GET /api/agences | `findAll()` | 200 | — |
| AgenceController | GET /api/agences/{id} | `findById(id)` | 200 | 404 |
| AgenceController | POST /api/agences | `create(agence)` | 201 | 400 (nom, ville ou adresse vide) |
| AgenceController | PUT /api/agences/{id} | `update(id, agence)` | 200 | 404, 400 |
| AgenceController | DELETE /api/agences/{id} | `deleteById(id)` | 204 | 404 |
| EmployeController | GET /api/employes | `findAll()` | 200 | — |
| EmployeController | GET /api/employes/{id} | `findById(id)` | 200 | 404 |
| EmployeController | POST /api/employes | `create(employe)` | 201 | 400 (agence absente, nom/prénom/rôle vide) |
| EmployeController | PUT /api/employes/{id} | `update(id, employe)` | 200 | 404, 400 |
| EmployeController | DELETE /api/employes/{id} | `deleteById(id)` | 204 | 404 |
| VehiculeController | GET /api/vehicules | `findAll()` | 200 | — |
| VehiculeController | GET /api/vehicules/{id} | `findById(id)` | 200 | 404 |
| VehiculeController | POST /api/vehicules | `create(vehicule)` | 201 | 400 (agence absente, tarif négatif) |
| VehiculeController | PUT /api/vehicules/{id} | `update(id, vehicule)` | 200 | 404, 400 |
| VehiculeController | DELETE /api/vehicules/{id} | `deleteById(id)` | 204 | 404 |
| EquipementController | GET /api/equipements | `findAll()` | 200 | — |
| EquipementController | GET /api/equipements/{id} | `findById(id)` | 200 | 404 |
| EquipementController | POST /api/equipements | `create(equipement)` | 201 | 400 (libellé vide) |
| EquipementController | PUT /api/equipements/{id} | `update(id, equipement)` | 200 | 404, 400 |
| EquipementController | DELETE /api/equipements/{id} | `deleteById(id)` | 204 | 404 |
| ClientController | GET /api/clients | `findAll()` | 200 | — |
| ClientController | GET /api/clients/{id} | `findById(id)` | 200 | 404 |
| ClientController | POST /api/clients | `create(client)` | 201 | 400 (nom, prénom, e-mail ou permis vide) |
| ClientController | PUT /api/clients/{id} | `update(id, client)` | 200 | 404, 400 |
| ClientController | DELETE /api/clients/{id} | `deleteById(id)` | 204 | 404 |
| ReservationController | GET /api/reservations | `findAll()` | 200 | — |
| ReservationController | GET /api/reservations/{id} | `findById(id)` | 200 | 404 |
| ReservationController | POST /api/reservations | `create(reservation)` | 201 | 400 (client/véhicule absent, fin avant début) |
| ReservationController | PUT /api/reservations/{id} | `update(id, reservation)` | 200 | 404, 400 |
| ReservationController | DELETE /api/reservations/{id} | `deleteById(id)` | 204 | 404 |
| MaintenanceController | GET /api/maintenances | `findAll()` | 200 | — |
| MaintenanceController | GET /api/maintenances/{id} | `findById(id)` | 200 | 404 |
| MaintenanceController | POST /api/maintenances | `create(maintenance)` | 201 | 400 (véhicule absent, fin avant début) |
| MaintenanceController | PUT /api/maintenances/{id} | `update(id, maintenance)` | 200 | 404, 400 |
| MaintenanceController | DELETE /api/maintenances/{id} | `deleteById(id)` | 204 | 404 |

Dans les corps JSON, les objets liés sont désignés par leur identifiant, avec le nom exact de l'attribut du modèle :
`{"agence": {"idAgence": 1}}`, `{"client": {"idClient": 1}, "vehicule": {"idVehicule": 1}}`,
`{"reservation": {"idReservation": 1}}`.

## B. Annotations Jackson

| Association | Annotation retenue | Justification |
|---|---|---|
| `Contrat.paiements` / `Paiement.contrat` | `@JsonManagedReference("contrat-paiements")` / `@JsonBackReference("contrat-paiements")` | Composition : les paiements sont créés dans le JSON du contrat (cascade ALL). À la lecture du corps, Jackson renseigne `paiement.contrat`, ce qui donne une clé étrangère non nulle. À l'écriture, `contrat` n'est pas répété dans chaque paiement, donc pas de boucle. |
| `Reservation.contrat` (côté inverse du `@OneToOne`) | `@JsonIgnore` | Sinon on a la boucle Contrat → Reservation → Contrat. Le lien se renseigne depuis le contrat. |
| `Agence.employes`, `Agence.vehicules` | `@JsonIgnore` | Côtés inverses : l'employé ou le véhicule désigne son agence par `{"idAgence": …}`. |
| `Client.reservations`, `Vehicule.reservations` | `@JsonIgnore` | Côtés inverses : la réservation désigne son client et son véhicule. |
| `Equipement.vehicules` (inverse du `@ManyToMany`) | `@JsonIgnore` | Le lien est porté par `Vehicule.equipements`. |
| `Agence`, `Vehicule`, `Client`, `Reservation`, `Contrat` | `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})` | Ce sont les cibles d'un `@ManyToOne`/`@OneToOne` LAZY. Le proxy Hibernate expose ces propriétés techniques, sur lesquelles Jackson échoue (*No serializer found … ByteBuddyInterceptor*). |

Le côté propriétaire des associations « enfant créé seul » (`Employe.agence`, `Vehicule.agence`, `Reservation.client`,
`Reservation.vehicule`, `Maintenance.vehicule`, `Contrat.reservation`) **ne reçoit aucune annotation** : il doit
rester lisible dans le corps d'un POST.

**`@JsonBackReference` ou `@JsonIgnore` ?**
- Les deux suppriment la propriété du JSON **produit**.
- `@JsonBackReference` fonctionne **en paire** avec un `@JsonManagedReference`. Quand Jackson lit le parent, il
  recopie automatiquement la référence du parent dans chaque enfant de la liste (`paiement.setContrat(contrat)`).
  C'est exactement ce qu'il faut pour une composition créée en un seul POST.
- `@JsonIgnore` ignore la propriété **dans les deux sens** et ne renseigne rien. On l'utilise sur le côté inverse
  d'une association dont l'enfant est créé seul.
- Poser `@JsonBackReference` sur `Reservation.client` ferait ignorer le client à la lecture du POST : on créerait
  une réservation sans client.

## C. Postman et Swagger

| Outil | Ce qu'il apporte | Exemple rencontré dans l'atelier |
|---|---|---|
| Postman | Tester. Les requêtes sont enregistrées dans une collection, rejouables et chaînées par des variables (`{{contratId}}` mémorisé après le POST). Chaque réponse est vérifiée par une assertion (`pm.response.to.have.status(201)`). | Le scénario de cascade se rejoue en un clic : POST d'un contrat avec 2 paiements, GET `/paiements` (ils sont là), PUT du contrat (ils restent), DELETE du contrat, puis GET `/paiements/{id}`, qui rend **404**. Le cas « Créer un paiement (refusé) » vérifie le **405**. |
| Swagger UI | Documenter et explorer. Le contrat OpenAPI est généré à partir du code au démarrage : il est donc toujours synchronisé avec les contrôleurs. On peut aussi essayer un endpoint sans rien installer (*Try it out*, puis *Execute*, avec la commande `curl` affichée). | SpringDoc détecte les routes à partir des seules annotations Spring MVC (18 chemins dans `/v3/api-docs`). `@Tag`, `@Operation` et `@ApiResponses` ajoutent ensuite les 9 groupes (Contrats, Paiements…) avec leurs résumés et leurs codes 200/201/204/400/404. La section *Schemas* décrit `Contrat` d'après l'entité, avec son champ `idContrat` et sa liste `paiements`. |

## D. Anomalies SonarQube for IDE

| Anomalie | Règle / explication | Correction apportée |
|---|---|---|
| `@RequestBody Contrat contrat` (et les autres entités reçues par les contrôleurs) | **java:S4684** — *Persistent entities should not be used as arguments of "@RequestMapping" methods*. Un client peut renseigner n'importe quel champ de l'entité (*mass assignment*), par exemple un identifiant ou une association. | Atténué dans le service : `create` refuse un identifiant non `null`, et `update` ne recopie que les attributs modifiables. La correction définitive, avec des DTO, est prévue au Chapitre 10. |
| `csrf(AbstractHttpConfigurer::disable)` dans `SecurityConfig` | **java:S4502** — *Disabling CSRF protections is security-sensitive* (security hotspot). | Revu et justifié : l'API est sans état (ni session ni cookie d'authentification), donc il n'y a pas de jeton CSRF à vérifier. Le choix est commenté dans le code et sera réexaminé avec l'authentification. |
| Littéral `"Contrat"` répété dans le contrôleur et le service | **java:S1192** — *String literals should not be duplicated*. | Le service utilise une constante `RESSOURCE`. Le contrôleur ne manipule aucun message : il ne fait que déléguer. |

## E. Questions de compréhension

1. **Quel composant reçoit toutes les requêtes ?** Le `DispatcherServlet`, point d'entrée unique de Spring MVC. Il
   interroge le `HandlerMapping`, qui a enregistré au démarrage toutes les méthodes annotées
   (`@RequestMapping("/contrats")` + `@GetMapping("/{id}")`…). Celui-ci retrouve la méthode à partir du **verbe HTTP**
   et du **chemin** (après le context-path `/api`). Les arguments sont ensuite résolus (`@PathVariable`, `@RequestBody`
   converti par Jackson) et la valeur retournée est sérialisée en JSON.
2. **Pourquoi `ContratController` dépend-il de `IContratService` ?** Le contrôleur reste mince : il traduit HTTP en appel
   métier, et les règles (garde sur l'id, recopie, existence) sont dans le service. Il ne doit donc pas parler au
   repository. Dépendre de l'**interface**, et non de `ContratServiceImpl`, garde le couplage faible : l'implémentation
   peut changer, ou être remplacée par un double en test, et Spring peut injecter un proxy (transactions, sécurité).
3. **201, 204, 404 :**
   - 201 *Created* indique qu'une ressource a été créée ; le corps contient la ressource avec son identifiant généré.
   - 204 *No Content* indique un succès sans rien à renvoyer, ce qui est le cas après une suppression.
   - Une ressource absente est une erreur **du client**, qui demande un identifiant inexistant : 404 relève de la famille 4xx.
   - 500 signalerait une panne du serveur et masquerait la vraie cause. C'est pourquoi `GlobalExceptionHandler` traduit `ResourceNotFoundException` en 404.
4. **Pourquoi `PaiementController` n'expose-t-il que des GET ?** Paiement est une composition de Contrat : un paiement se
   crée dans le JSON de son contrat (cascade PERSIST), et disparaît avec lui (cascade REMOVE) ou quand il est retiré de
   sa liste (orphanRemoval). Un `POST /api/paiements` reçoit **405 Method Not Allowed** : la route existe, mais pas pour
   ce verbe. C'est vérifié dans Postman, et le log montre `HttpRequestMethodNotSupportedException`.
5. **Risques de l'exposition directe des entités JPA :**
   - Couplage entre l'API et le schéma : renommer une colonne casse les clients.
   - Champs internes visibles (proxys Hibernate, identifiants).
   - *Mass assignment* (S4684).
   - Boucles de sérialisation et chargements LAZY déclenchés par Jackson, avec la session laissée ouverte jusqu'à la vue (`open-in-view`).

   **Pourquoi `@JsonManagedReference` sur `Contrat.paiements` et `@JsonIgnore` sur `Agence.vehicules` ?**
   `Contrat.paiements` est une composition dont les enfants arrivent **dans le corps du parent** : il faut que Jackson
   relie chaque paiement à son contrat à la lecture, ce que fait la paire managed/back. Les véhicules, eux, sont créés
   **seuls** et désignent leur agence par `{"idAgence": 1}`. La liste `Agence.vehicules` n'a donc qu'à disparaître du JSON.
