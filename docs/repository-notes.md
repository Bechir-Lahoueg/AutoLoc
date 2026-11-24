# Atelier 3 — Couche Repository (Spring Data JPA)

## 1. Choix d'interface

Toutes les interfaces sont dans `tn.esprit.autoloc.repository`, préfixées `I`, et étendent
`JpaRepository<Entité, Long>` (la clé primaire de chaque entité est un `Long` généré en `IDENTITY`).
Au démarrage : `Found 9 JPA repository interfaces`.

| Interface | Étend | Justification |
|---|---|---|
| `IContratRepository` | `JpaRepository<Contrat, Long>` | CRUD complet, `findAll` renvoie une `List`, `saveAndFlush` disponible. Les suppressions passent par `deleteById`/`delete` pour que la cascade et l'`orphanRemoval` sur les paiements s'appliquent. |
| `IPaiementRepository` | `JpaRepository<Paiement, Long>` | Lecture des paiements (listes, tri par date ou montant). Composition : la création et le retrait d'un paiement passent par le contrat (`Contrat.ajouterPaiement` / `retirerPaiement`). |
| `IAgenceRepository` | `JpaRepository<Agence, Long>` | CRUD et listes des agences ; tri (ex. par ville) utile pour l'affichage. |
| `IEmployeRepository` | `JpaRepository<Employe, Long>` | CRUD des employés ; `getReferenceById` permet d'affecter une agence sans la charger. |
| `IVehiculeRepository` | `JpaRepository<Vehicule, Long>` | Catalogue potentiellement volumineux : la pagination et le tri (`findAll(Pageable)`) sont nécessaires. |
| `IEquipementRepository` | `JpaRepository<Equipement, Long>` | CRUD simple sur une table de référence, résultats en `List`. |
| `IClientRepository` | `JpaRepository<Client, Long>` | CRUD, pagination de la liste des clients. |
| `IReservationRepository` | `JpaRepository<Reservation, Long>` | CRUD, tri par date ; `getReferenceById` pour lier un contrat à sa réservation sans `SELECT`. |
| `IMaintenanceRepository` | `JpaRepository<Maintenance, Long>` | CRUD et historique trié par date de début. |

### Pourquoi `JpaRepository` plutôt que les autres interfaces ?

- `Repository` : interface marqueur, aucune méthode ; il faudrait recopier chaque signature.
- `CrudRepository` : CRUD de base, mais `findAll`, `findAllById` et `saveAll` renvoient un `Iterable`
  (pas de `size()`, `get(i)` ni `stream()` directs).
- `ListCrudRepository` : même CRUD, mais avec des `List` ; aucune méthode supplémentaire.
- `PagingAndSortingRepository` : tri et pagination seulement. Depuis Spring Data 3 elle **n'étend plus**
  `CrudRepository`, il faudrait donc déclarer les deux.
- `JpaRepository` réunit `ListCrudRepository`, `ListPagingAndSortingRepository` et `QueryByExampleExecutor`,
  et y ajoute `flush`, `saveAndFlush`, `getReferenceById` et les suppressions en lot.

**Attention aux suppressions en lot** (`deleteAllInBatch`, `deleteAllByIdInBatch`) : elles exécutent un seul
`DELETE` sans passer par le contexte de persistance, donc **sans cascade ni orphanRemoval**. Constaté avec la
démonstration (étape 8) : `delete c1_0 from contrat c1_0 where c1_0.id_contrat in (?)` échoue avec
`Cannot delete or update a parent row: a foreign key constraint fails (... paiement ...)`.

## 2. Observations CRUD (programme `ContratCrudDemo`, profil `demo`)

Lancement : `mvnw spring-boot:run -Dspring-boot.run.profiles=demo` (ou, dans IntelliJ, *Active profiles* = `demo`).

| Opération | SQL observé | Interprétation |
|---|---|---|
| `save(contrat)` avec id `null` | `insert into contrat …` puis 2 × `insert into paiement …` | `persist` ; les paiements suivent grâce à `CascadeType.ALL` ; l'id est attribué par MariaDB et renseigné dans l'objet retourné. |
| `findById(id)` | `select … from contrat where id_contrat=?` | Renvoie un `Optional` : `findById(-1)` donne `Optional.empty`, pas de `null` ni d'exception. |
| `existsById(id)` / `count()` | `select count(*) …` | Pas de chargement de l'entité. |
| `save(contrat)` avec id renseigné | `select …` puis `update contrat set …` | `merge` : l'objet retourné **n'est pas** celui passé en paramètre (`==` donne `false`), il faut utiliser le retour. |
| `contrat.retirerPaiement(p)` dans une transaction | `delete from paiement where id_paiement=?` | `orphanRemoval = true` : un paiement retiré de la liste est supprimé. |
| `getReferenceById(id)` | aucun `select` | Proxy paresseux, suffisant pour renseigner la clé étrangère. |
| `findAll(PageRequest.of(0, 2, Sort.by("montantTotal").descending()))` | `select … order by montant_total desc limit ?, ?` puis `select count(id_contrat) from contrat` | `Sort` reçoit le nom de l'**attribut Java** ; `Page` déclenche une requête `count` supplémentaire. |
| `deleteById(id)` | `select` contrat, `select` paiements, `delete from paiement`, `delete from contrat` | La cascade REMOVE s'applique. |
| `deleteById(-1)` | `select` uniquement | Aucune exception si l'id n'existe pas (Spring Data 3 et plus). |

## 3. Anomalies SonarQube for IDE

| Anomalie SonarQube for IDE | Règle / explication | Correction apportée |
|---|---|---|
| Package `tn.esprit.autoloc.Domain` | **java:S120** — *Package names should comply with a naming convention* (`^[a-z_]+(\.[a-z_][a-z0-9_]*)*$`). Une majuscule dans un nom de package est contraire à la convention Java et à l'architecture demandée (`domain`). | Package renommé en `tn.esprit.autoloc.domain` (dossier et déclarations `package`). |
| `import jakarta.persistence.*;` dans les 9 entités | **java:S2208** — *Wildcard imports should not be used* : on ne voit plus quelles classes sont réellement utilisées, avec un risque de conflit de noms. | Imports explicites (`jakarta.persistence.Entity`, `Id`, `Column`, …), uniquement ceux qui sont utilisés. |
| Méthode `contextLoads()` vide dans `AutoLocApplicationTests` | **java:S1186** — *Methods should not be empty* : une méthode vide doit expliquer pourquoi elle l'est. | Ajout d'un commentaire expliquant que le test vérifie le démarrage du contexte (entités + 9 repositories). |

Autres bonnes pratiques respectées :
- Lombok ciblé (`@Getter`, `@Setter`, constructeurs), **pas de `@Data`** sur les entités (un `equals`/`hashCode`/`toString` généré sur des associations bidirectionnelles provoque des boucles et des chargements paresseux involontaires).
- Pas d'annotation `@Repository` : inutile sur une interface Spring Data.
- Injection par constructeur (`@RequiredArgsConstructor`) et logger SLF4J plutôt que `System.out` (règle **java:S106**) dans le programme de démonstration.

## 4. Correction de l'environnement (héritée de l'Atelier 2)

MariaDB de XAMPP est en version **10.4.32**, alors qu'Hibernate 7 en exige au moins la 10.6. Le dialecte par défaut
génère `alter table if exists … add constraint …`, que MariaDB 10.4 refuse : **aucune clé étrangère n'avait été
créée**. Les colonnes enum étaient aussi modifiées (`modify column … enum(…)`) à chaque démarrage.

- `pom.xml` : ajout de `hibernate-community-dialects`, et `application.properties` :
  `spring.jpa.database-platform=org.hibernate.community.dialect.MariaDBLegacyDialect` → les 9 clés étrangères sont créées.
- Enums persistés en `VARCHAR(20)` (`@JdbcTypeCode(SqlTypes.VARCHAR)`) → plus aucun DDL au redémarrage.
