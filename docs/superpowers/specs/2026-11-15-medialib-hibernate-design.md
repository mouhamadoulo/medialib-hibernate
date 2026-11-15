# MediaLib — Projet fil rouge Hibernate

**Date** : 2026-11-15
**Auteur** : Mouhamadou Moustapha LO
**Statut** : en attente de validation

---

## 1. Intention et motivation

Construire un projet Java unique, progressif et exécutable, qui couvre l'intégralité des
notions d'Hibernate — du bootstrap de la `SessionFactory` jusqu'au multi-tenancy, au cache
de second niveau et aux optimisations de performance.

Le besoin exprimé : « comprendre tout ce qu'il y a à savoir sur Hibernate ». Pas une
collection de snippets, pas un tutoriel lu passivement : un dépôt où chaque notion est
**mise en scène par un besoin métier réel**, **implémentée**, puis **prouvée par un test
qui vérifie le SQL réellement émis**.

### Le principe pédagogique central

La difficulté avec Hibernate n'est pas d'écrire du code qui fonctionne. C'est de savoir
**ce qui se passe réellement en base** quand le code s'exécute. Un `@OneToMany` qui marche
en test peut générer 400 requêtes en production.

Donc chaque module contient des tests de la forme :

```java
@Test
void fetch_join_ramene_tout_en_une_seule_requete() {
    sqlCounter.reset();

    List<Loan> loans = em.createQuery(
        "select l from Loan l join fetch l.member join fetch l.copy.media", Loan.class)
        .getResultList();

    loans.forEach(l -> l.getMember().getFullName());   // pas de lazy loading

    assertThat(sqlCounter.selectCount()).isEqualTo(1); // la preuve
}
```

Le test n'affirme pas « ça marche ». Il affirme « **1 requête, pas 11** ». C'est ça qu'on
vient apprendre.

---

## 2. Le domaine : MediaLib

Réseau de médiathèques municipales multi-agences.

### Concepts métier

| Concept | Description |
|---|---|
| `Media` | Œuvre du catalogue. Abstraite. Décline en `Book`, `Dvd`, `Vinyl`, `Magazine` |
| `Copy` | Exemplaire physique d'un `Media`, rattaché à une `Branch`. C'est lui qu'on emprunte |
| `Branch` | Agence du réseau. Sert d'axe de multi-tenancy |
| `Author` | Auteur / réalisateur / artiste. Lié à plusieurs `Media`, et réciproquement |
| `Publisher` | Éditeur. Un `Media` a un éditeur ; un éditeur a plusieurs `Media` |
| `Member` | Adhérent. Possède une `Address` (valeur), une `MembershipCard` (1-1) |
| `Loan` | Emprunt d'une `Copy` par un `Member`. Daté, rendu ou non |
| `Reservation` | Réservation d'un `Media` en file d'attente |
| `Fine` | Amende de retard. Montant en `Money`, statut, rattachée à un `Loan` |
| `Review` | Avis d'un adhérent sur un média, note + commentaire |

### Pourquoi ce domaine plutôt qu'un autre

Chaque notion Hibernate y trouve une justification métier naturelle, pas un prétexte :

- **Héritage** : `Book` a un ISBN et un nombre de pages, `Dvd` une durée et une région,
  `Vinyl` un nombre de faces et un RPM. Les trois sont des `Media` qu'on cherche ensemble
  dans le catalogue. C'est le cas d'école de l'héritage, et il permet de comparer
  `SINGLE_TABLE`, `JOINED` et `TABLE_PER_CLASS` sur **les mêmes données**.
- **Concurrence réelle** : deux adhérents cliquent « emprunter » sur le dernier exemplaire
  disponible à la même seconde. Verrou optimiste (`@Version`) et pessimiste
  (`PESSIMISTIC_WRITE`) deviennent des réponses à un problème, pas des annotations à
  mémoriser.
- **Multi-tenancy** : chaque agence veut ses données isolées. Un schéma PostgreSQL par
  agence, avec résolution dynamique du tenant.
- **Audit** : qui a emprunté quoi et quand, historique inaltérable → Envers.
- **Volumétrie** : un catalogue de 100 000 médias avec auteurs et exemplaires rend le
  problème N+1 **visible et mesurable**, pas théorique.
- **Soft delete** : on ne supprime jamais un média retiré du catalogue, on l'archive →
  `@SQLDelete` + `@SQLRestriction` + `@Filter`.

---

## 3. Objectifs, non-objectifs, anti-objectifs

### Objectifs

1. Couvrir exhaustivement Hibernate : mapping, associations, héritage, requêtes, fetching,
   transactions, concurrence, cache, audit, multi-tenancy, performance.
2. Chaque module est **autonome et exécutable isolément**. On peut ouvrir le module 07 sans
   avoir lu les six précédents.
3. Chaque **notion structurante** est prouvée par un test qui assertionne le **SQL
   réellement émis** (nombre de requêtes, présence d'un `join`, d'un `for update`, d'un
   `insert` batché). Cible : **80 à 120 tests** sur l'ensemble du projet. Les variantes
   mineures d'une même notion sont regroupées dans un test unique ou signalées en
   commentaire plutôt que dupliquées ; `doc/TASKS.md` suit les **notions**, pas les tests.
4. Les pièges sont **reproduits volontairement avant d'être corrigés** : un test qui
   démontre le N+1, puis un test qui le corrige. Un test qui déclenche
   `LazyInitializationException`, puis la bonne pratique.
5. Le `README.md` sert de parcours d'apprentissage ordonné et de référence consultable.

### Non-objectifs

- Pas d'interface graphique ni de frontend.
- Pas d'API REST exposée (sauf un contrôleur minimal au module 12, pour démontrer le piège
  OSIV — Open Session In View).
- Pas de déploiement, pas de CI/CD, pas de conteneurisation applicative.
- Pas de couverture de JPA « en général » : le focus est Hibernate, ses extensions
  propriétaires incluses, et ce qui le distingue du standard.

### Anti-objectifs (ce qui ferait échouer le projet même s'il compile)

- **Un projet qui marche sans qu'on comprenne pourquoi.** Si un module passe au vert sans
  rien enseigner sur le SQL généré, il a échoué.
- **Du code trop abstrait.** Pas de couche repository générique, pas de framework maison,
  pas d'abstraction qui masque l'appel Hibernate. Le `Session` et l'`EntityManager` doivent
  rester visibles.
- **Des exemples jouets** (`Foo`, `Bar`, `Person`/`Address`). Le domaine métier doit porter
  la notion.
- **Spring Data JPA dès le départ.** Les repositories cachent le cycle de vie de la session,
  le flush, le dirty checking. On ne peut pas apprendre Hibernate à travers eux.

---

## 4. Contraintes

| Contrainte | Valeur | Raison |
|---|---|---|
| JDK | 21 (LTS) | installé sur le poste, records + pattern matching utiles pour les DTO |
| Build | Maven multi-modules 3.9.13 | installé, et le multi-modules matérialise les chapitres |
| Base de données | PostgreSQL via Testcontainers 1.21.4 | Docker 29.7.2 disponible. H2 fausserait verrous pessimistes, séquences, multi-schémas |
| Hibernate | 7.4.16.Final | dernière GA. 8.0.0.CR1 écarté : release candidate |
| Spring Boot | 4.1.2 | module 12 uniquement |
| Git | commits au seul nom de Mouhamadou Moustapha LO | demande explicite. **Aucune ligne `Co-Authored-By`, aucune mention d'outil d'IA dans les messages de commit ni dans l'historique** |
| Remote | GitHub public sous `mouhamadoulo` | `gh` déjà authentifié |

---

## 5. Approches considérées

### Approche A — Un seul module, packages par thème *(écartée)*

Un `src/main/java` unique avec `com.medialib.inheritance`, `com.medialib.caching`, etc.

- **Pour** : mise en place immédiate, un seul `pom.xml`, un seul modèle de domaine.
- **Contre** : impossible d'avoir trois stratégies d'héritage concurrentes sur les mêmes
  entités. Une `persistence-unit` unique charge toutes les entités, donc les configurations
  entrent en conflit (cache activé/désactivé, multi-tenancy on/off, dialecte, batch size).
  Et on ne peut plus ouvrir un chapitre isolément.

### Approche B — Un dépôt par notion *(écartée)*

Douze dépôts Git séparés.

- **Pour** : isolation maximale.
- **Contre** : ce n'est plus un fil rouge. Le domaine métier se dilue, le README central
  disparaît, et la demande portait explicitement sur **un** projet fil rouge.

### Approche C — Maven multi-modules, un module par chapitre *(retenue)*

Un module Maven par notion, chacun avec sa propre `persistence.xml` / configuration
programmatique, ses entités, ses migrations Flyway et ses tests.

- **Pour** : chaque chapitre a sa configuration Hibernate propre (c'est indispensable :
  le module héritage a besoin de trois hiérarchies parallèles, le module cache a besoin du
  L2 activé, le module multi-tenancy a besoin d'un `MultiTenantConnectionProvider`).
  `mvn -pl 07-fetching test` lance un seul chapitre. Le `pom.xml` parent centralise les
  versions.
- **Contre** : les entités sont redéfinies par module — environ 60 % de duplication entre
  chapitres voisins.
- **Pourquoi c'est le bon choix malgré ça** : cette duplication est **pédagogique, pas
  accidentelle**. Voir côte à côte le `Media` du chapitre 05 mappé en `SINGLE_TABLE` puis
  en `JOINED`, c'est exactement ce qui fait comprendre la différence. Factoriser détruirait
  la leçon. Le module `00-support` ne partage que l'outillage de test, jamais les entités.

**Décision : approche C.**

### Choix de stack arrêtés avec l'utilisateur

| Question | Décision |
|---|---|
| Domaine métier | MediaLib (médiathèque multi-agences) |
| Stack | Hibernate natif + JPA d'abord (modules 01→11), Spring Boot en dernier (module 12) |
| Base de données | PostgreSQL via Testcontainers |
| Granularité des tests | 1 test par notion structurante, ~80-120 tests au total |
| Portée du module 12 | Sous-ensemble ciblé (4 thèmes) + table de correspondance |
| Rythme de livraison | Push incrémental, un commit par module, sans pause de relecture |

---

## 6. Découpage en modules

```
medialib-hibernate/                 (pom parent, packaging pom)
├── 00-support/                     outillage de test partagé — PAS d'entités
├── 01-bootstrap/
├── 02-mapping-basics/
├── 03-associations/
├── 04-collections/
├── 05-inheritance/
├── 06-queries/
├── 07-fetching/
├── 08-transactions-concurrency/
├── 09-caching/
├── 10-advanced/
├── 11-performance/
└── 12-spring-boot/
```

### 00-support

Socle de test, sans aucune entité. Consommé par les modules 01 à 11 sous forme de
`test-jar`. **Le module 12 ne l'utilise pas** (voir §7, conflit Testcontainers).

- **Un conteneur PostgreSQL par module.** Surefire forke une JVM par module Maven : un
  `static` Java ne donnerait pas un conteneur partagé mais douze, avec en prime des
  migrations Flyway qui se marchent dessus (`media` est mappé d'une façon au module 02, de
  trois façons au module 05, d'une autre au 07). Un conteneur par module = une base par
  module = zéro collision, et les schémas multi-tenants du module 10 vivent tranquillement
  dans le sien. Le mode `reuse` de Testcontainers est **documenté en option** pour qui veut
  accélérer ses allers-retours locaux, jamais requis pour que le build passe.
- Le schéma est généré par `hbm2ddl.auto=create-drop` (voir §10). Le `SqlStatementCounter`
  **ignore le DDL** : il ne compte que le DML, sinon les `create table` de démarrage
  fausseraient toutes les assertions.
- `SqlStatementCounter`, bâti sur le **`StatementInspector` d'Hibernate** + `Statistics`.
  Permet `assertThat(sql.selectCount()).isEqualTo(1)` et
  `assertThat(sql.lastQuery()).contains("for update")`.
  *Pourquoi pas `datasource-proxy` partout* : il n'intercepte le SQL que si Hibernate tire
  ses connexions d'une `DataSource` enveloppée. Or le module 01 doit montrer un bootstrap
  nu par `hibernate.connection.url`, le module 11 doit configurer HikariCP pour de vrai, et
  le module 10 remplace le chemin d'acquisition des connexions par son
  `MultiTenantConnectionProvider`. Imposer la `DataSource` proxy rendrait ces trois
  configurations partiellement artificielles — des artefacts de harnais de test au lieu de
  la vraie leçon. `StatementInspector` est dans Hibernate, se branche sur n'importe quel
  bootstrap et n'en déforme aucun.
- `HibernateStatisticsAssert` : wrapper AssertJ sur `org.hibernate.stat.Statistics`
  (hits/misses du cache L2, requêtes préparées, entités chargées).
- Base JUnit 6 gérant le cycle `EntityManagerFactory` et le nettoyage entre tests.
- Tag JUnit `slow` pour les tests à gros volume, exclus du build par défaut.

### 01-bootstrap — Démarrer et piloter Hibernate

`SessionFactory` par `StandardServiceRegistryBuilder` · `hibernate.cfg.xml` vs configuration
programmatique vs `persistence.xml` · `Session` vs `EntityManager` (et pourquoi `Session`
étend `EntityManager` depuis Hibernate 6) · cycle de vie d'une entité : *transient*,
*managed*, *detached*, *removed* · `persist` / `merge` / `remove` / `find` / `getReference` /
`refresh` / `detach` · le **dirty checking** démontré (modifier un objet managé sans appeler
`save`) · `flush()` et `FlushMode` · unité de travail et contexte de persistance.

### 02-mapping-basics — Mapper une entité

`@Entity`, `@Table`, `@Column` et ses attributs · générateurs d'identifiant : `IDENTITY`,
`SEQUENCE`, `TABLE`, `UUID`, `AUTO` — et leur **impact sur le batching** · `@Embeddable` /
`@Embedded` (`Address`) · `@EmbeddedId` et `@IdClass` (clés composites) · `@Enumerated`
(piège `ORDINAL`) · `AttributeConverter` (`Money`) · `UserType` personnalisé (`Isbn`) ·
types temporels et `java.time` · `@Lob` · `@Formula` (champ calculé en SQL) ·
`@Transient` · stratégies de nommage implicite/physique · `@Generated`, `@ColumnDefault`,
`@Check`.

### 03-associations — Relier les entités

`@ManyToOne` (`Copy` → `Media`) · `@OneToMany` unidirectionnel (join table imposée) vs
bidirectionnel avec `mappedBy` · `@OneToOne` partagé par `@MapsId` (`Member` ↔
`MembershipCard`) · `@ManyToMany` (`Media` ↔ `Author`) et **pourquoi le remplacer par une
entité d'association** dès qu'un attribut s'y ajoute (`Loan` entre `Member` et `Copy`) ·
côté propriétaire et `mappedBy` : qui écrit la clé étrangère · méthodes utilitaires de
synchronisation bidirectionnelle · `cascade` : les six types, lequel est dangereux ·
`orphanRemoval` vs `CascadeType.REMOVE` · `equals`/`hashCode` sur entités — le piège du
`Set` et de l'identifiant généré.

### 04-collections — Collections et éléments de valeur

`List` vs `Set` vs `Map` vs `SortedSet` · *bag* vs *list* indexée (`@OrderColumn`) ·
`@OrderBy` (tri SQL) vs `SortedSet` (tri mémoire) · `MultipleBagFetchException` reproduite
puis résolue · `@ElementCollection` (tags d'un média) · `@MapKeyColumn`, `@MapKeyEnumerated` ·
`@BatchSize` sur collection · coût réel de la modification d'une collection (`delete all` +
`re-insert` sur les bags).

### 05-inheritance — Héritage

Les **trois stratégies implémentées en parallèle** sur la même hiérarchie `Media` :
`SINGLE_TABLE`, `JOINED`, `TABLE_PER_CLASS` · `@MappedSuperclass` (et pourquoi ce n'est pas
de l'héritage persistant) · `@DiscriminatorColumn`, `@DiscriminatorValue`,
`@DiscriminatorFormula` · requêtes polymorphes et `type()` en HQL · contrainte `NOT NULL`
impossible en `SINGLE_TABLE` · `UNION ALL` et absence de clé étrangère en
`TABLE_PER_CLASS` · **comparatif** : SQL émis pour la même requête selon la stratégie —
le **nombre et la forme des requêtes sont assertés**, les temps sont seulement journalisés
(voir §7bis).

### 06-queries — Interroger

HQL/JPQL : `select`, `join`, `left join`, sous-requêtes, fonctions d'agrégat, `case`,
`coalesce` · API Criteria + **métamodèle statique généré** (`hibernate-jpamodelgen`) ·
SQL natif et `@SqlResultSetMapping` · `@NamedQuery` et `@NamedNativeQuery` · projections :
constructeur DTO, `Tuple`, interface de projection, record Java 21 · pagination
(`setFirstResult` / `setMaxResults`) et son coût avec les jointures · `@Subselect` (entité
sur une vue) · `createQuery` typé vs non typé · injection SQL par concaténation —
reproduite puis corrigée par paramètres nommés.

### 07-fetching — Le chargement, cœur du sujet

`LAZY` vs `EAGER` et les défauts implicites de chaque type d'association · proxies Hibernate
et `HibernateProxy` · `LazyInitializationException` provoquée puis traitée (les quatre
solutions, avec leurs défauts) · **le N+1 démontré et chiffré** sur 1 000 médias ·
`join fetch` · `@EntityGraph` nommé et dynamique · `@BatchSize` · `@Fetch(SUBSELECT)` ·
`Hibernate.initialize()` · pourquoi la **projection DTO est souvent la vraie réponse** ·
limite de la pagination avec `join fetch` (`HHH90003004` : pagination en mémoire).

### 08-transactions-concurrency — Transactions et accès concurrent

Démarcation transactionnelle manuelle · `FlushMode` · niveaux d'isolation PostgreSQL et
anomalies (lecture sale, non répétable, fantôme) · **verrouillage optimiste** avec
`@Version` : `OptimisticLockException` reproduite avec deux `EntityManager` ·
`OPTIMISTIC_FORCE_INCREMENT` · **verrouillage pessimiste** : `PESSIMISTIC_READ`,
`PESSIMISTIC_WRITE`, `PESSIMISTIC_FORCE_INCREMENT`, `for update` vérifié dans le SQL
capturé · `LockOptions`, timeout, `NOWAIT`, `SKIP LOCKED` · **test à deux threads** : deux
adhérents empruntent le dernier exemplaire, un seul gagne · interblocage provoqué puis évité
par ordre d'acquisition.

### 09-caching — Les caches

Cache de **premier niveau** : il n'est pas optionnel, démonstration de l'identité d'instance
dans une même session · cache de **second niveau** via JCache + Caffeine · stratégies
`READ_ONLY`, `NONSTRICT_READ_WRITE`, `READ_WRITE`, `TRANSACTIONAL` : laquelle pour quelle
donnée · cache de collections (et sa configuration distincte, souvent oubliée) ·
**cache de requêtes** et pourquoi il est contre-productif par défaut · `Statistics` :
hits, misses, puts · invalidation : le piège des `bulk update` qui contournent le cache ·
`CacheMode` par session.

> **Limite assumée sur `TRANSACTIONAL`.** Cette stratégie exige un cache transactionnel XA
> piloté par un gestionnaire JTA — ni l'un ni l'autre n'est dans le stack figé (JCache +
> Caffeine, transactions *resource-local*). Plutôt que d'ajouter Narayana et Infinispan
> pour une seule démonstration, le chapitre l'enseigne comme **limite documentée** : un
> test montre ce qui se passe réellement quand on la déclare sur ce stack, et le texte
> explique ce qu'il faudrait pour l'obtenir vraiment. Les trois autres stratégies sont
> pleinement démontrées.

### 10-advanced — Fonctionnalités avancées

**Envers** : historisation complète des `Loan` et `Fine`, `@Audited`, tables `_AUD`,
`AuditReader`, requêtes sur révisions, `RevisionEntity` personnalisée ·
**soft delete** : `@SQLDelete` + `@SQLRestriction` · **`@Filter`** paramétré (filtrer par
agence à l'exécution) · **intercepteurs** et **listeners d'événements** (`PreInsert`,
`PostLoad`) · `@SQLInsert` / `@SQLUpdate` personnalisés · procédures stockées
(`@NamedStoredProcedureQuery`) · **multi-tenancy** : un schéma PostgreSQL par agence,
`MultiTenantConnectionProvider` + `CurrentTenantIdentifierResolver` ·
`@DynamicUpdate` / `@DynamicInsert` · `@Immutable` · `@NaturalId`.

### 11-performance — Performance

**Batching JDBC** : `hibernate.jdbc.batch_size`, `order_inserts`, `order_updates` — et
pourquoi `GenerationType.IDENTITY` le désactive silencieusement · `StatelessSession` ·
`update` / `delete` en masse en HQL et leur effet sur le contexte de persistance · `scroll`
et `stream` sur gros volumes · mesure sur 100 000 insertions (test `@Tag("slow")`, temps
journalisé, nombre d'allers-retours JDBC asserté) · pool de connexions
HikariCP et dimensionnement · `EXPLAIN ANALYZE` sur les requêtes générées, index
manquants · **Flyway** pour le schéma vs `hbm2ddl.auto` (et pourquoi `update` est à proscrire
en production) · `hibernate.generate_statistics` et lecture des métriques ·
`SqlStatementCounter` comme garde-fou anti-régression.

### 12-spring-boot — Ce que Spring ajoute, et ce qu'il cache

**Module autonome** : il n'utilise pas `00-support` (voir §7, conflit Testcontainers) et
porte son propre harnais, minimal, bâti sur `@ServiceConnection` de Spring Boot. Cette
autonomie sert le propos du chapitre — on voit exactement ce que Boot apporte en matière
d'outillage de test.

Portée retenue : **code Spring réel sur les quatre points où Spring change vraiment la
donne**, plus une table de correspondance couvrant les onze chapitres.

Code :

1. **Transactions** — `@Transactional` : propagation, `readOnly`, pourquoi l'auto-invocation
   ne marche pas (le proxy n'est pas traversé), comparaison avec la démarcation manuelle du
   module 08.
2. **Fetching et OSIV** — `open-in-view` est à `true` par défaut. Démonstration du problème
   qu'il masque (plus de `LazyInitializationException`) et de celui qu'il crée (requêtes
   émises depuis la couche de présentation, connexion tenue pendant tout le rendu). C'est
   le piège Spring + Hibernate le plus coûteux en production — un contrôleur REST minimal
   existe uniquement pour le rendre visible.
3. **Requêtes** — Spring Data JPA : requêtes dérivées, `@Query`, `Specification`,
   projections par interface — et le SQL que chacune produit réellement, asserté comme
   partout ailleurs.
4. **Tests** — `@DataJpaTest`, le piège du rollback automatique, et `EntityManager` injecté
   directement pour montrer qu'il reste accessible sous les repositories.

Documentation : **table de correspondance** dans le `README.md`, une ligne par chapitre
01→11, donnant l'équivalent Spring et ce qui se passe derrière.

*Pourquoi pas la réimplémentation complète des onze chapitres* : ce module pèserait alors
autant que tout le reste du projet, pour une valeur décroissante — les chapitres mapping,
héritage et collections sont rigoureusement identiques sous Spring, puisque c'est Hibernate
qui fait le travail.

---

## 7. Stack technique figée

| Dépendance | Version | Vérifiée sur Maven Central le 2026-11-15 |
|---|---|---|
| JDK | 21 | installé (21.0.10 LTS) |
| Maven | 3.9.13 | installé |
| `hibernate-core` | 7.4.16.Final | dernière GA |
| `hibernate-envers` | 7.4.16.Final | module 10 |
| `hibernate-jcache` | 7.4.16.Final | module 09 |
| `hibernate-jpamodelgen` | 7.4.16.Final | module 06, métamodèle Criteria |
| `caffeine` + `jcache` | 3.3.1 | module 09 |
| `postgresql` | 42.7.14 | pilote JDBC |
| `HikariCP` | 7.1.0 | module 11 |
| `flyway-core` / `flyway-database-postgresql` | 13.12.2 | migrations |
| `testcontainers` / `postgresql` / `junit-jupiter` | 1.21.4 | modules 00 → 11 |
| `junit-bom` | 6.1.3 | tests |
| `assertj-core` | 3.27.7 | assertions |
| `datasource-proxy` | 1.11.1 | **module 11 uniquement** (frontières de batch JDBC) |
| `logback-classic` | 1.6.7 | logs |
| `spring-boot-dependencies` (BOM importé) | 4.1.2 | module 12 uniquement |

### La frontière du module 12

Le BOM `spring-boot-dependencies:4.1.2` impose **Testcontainers 2.0.5**, et la 2.x a
renommé ses artefacts : `org.testcontainers:testcontainers-postgresql:2.0.5` existe,
`org.testcontainers:postgresql:2.0.5` renvoie un 404. Un simple override de
`<testcontainers.version>` ne résout donc pas. Trois routes possibles ; celle retenue est
la troisième :

1. Tenir 1.21.4 partout et renoncer à `@ServiceConnection` au module 12 — on perdrait
   justement ce que ce chapitre vient montrer.
2. Passer tout le projet en Testcontainers 2.x — contredit le stack figé et impose une
   migration à onze modules qui n'en ont aucun besoin.
3. **Retenue** : modules 00→11 sur 1.21.4, module 12 autonome sur le BOM Boot. Le module
   12 ne dépend pas de `00-support` et porte un harnais minimal. Le coût est une petite
   duplication ; le bénéfice est que ni le cœur du projet ni le chapitre Spring ne sont
   bridés par l'autre.

Le BOM Boot gère aussi Hibernate 7.4.13, Flyway 12.4.0, Logback 1.5.38, Caffeine 3.2.4,
HikariCP 7.0.2 — tous légèrement en retrait du stack figé. **Au module 12, le BOM Boot fait
foi** : on n'écrase pas les versions de Boot, parce qu'un Flyway en écart d'une version
majeure ou un Logback décalé sous l'auto-configuration est une source connue de casse, et
parce que ce chapitre doit montrer Spring Boot tel qu'on le rencontre.

---

## 7bis. Mesures, temps et volumétrie

**Aucune assertion sur un temps d'exécution.** Les benchmarks (module 05 comparatif des
stratégies d'héritage, module 07 chiffrage du N+1, module 11 batching sur 100 000 lignes)
assertionnent le **nombre et la forme des requêtes SQL** — grandeur déterministe et
reproductible — et se contentent de **journaliser** les durées.

Deux raisons :

- Une assertion temporelle est instable chez quiconque clone le dépôt, et ferait échouer
  `mvn clean verify` au hasard — ce qui ruine le critère d'acceptation du §11.
- La machine de développement est en **Windows sur aarch64**, avec Docker sous WSL2. Des
  chiffres produits là ne se reproduisent nulle part ailleurs. Ils valent comme
  observation commentée, pas comme contrat.

Les tests à grosse volumétrie portent le tag JUnit `slow` et sont **exclus du build par
défaut**, activables par le profil Maven `-Pslow`. `mvn clean verify` doit rester de
l'ordre de quelques minutes.

---

## 8. Livrables documentaires

### `README.md` (racine)

Présentation de MediaLib · prérequis (JDK 21, Maven, **Docker Desktop démarré** — le build
échoue sans démon joignable) · démarrage en trois commandes ·
**tableau des 12 chapitres** : notion, module, commande pour le lancer seul · schéma du
modèle de domaine · conventions de test · index des notions Hibernate renvoyant vers le
module et la classe de test qui les démontre.

### `doc/TASKS.md`

Suivi d'avancement : une case à cocher par notion de chaque module, avec son statut
(implémenté / testé / documenté). Sert de feuille de route pendant la construction et de
checklist de révision ensuite.

### `CLAUDE.md` (racine)

Conventions pour toute session d'assistance future sur ce dépôt : structure multi-modules,
règle de non-factorisation des entités entre modules (et pourquoi), format des tests
(assertion sur le SQL obligatoire), versions figées, **règle de commit : auteur unique
Mouhamadou Moustapha LO, aucune ligne de co-auteur, aucune mention d'outil d'IA**.

---

## 9. Git et dépôt distant

- Dépôt local initialisé à la racine du dossier courant.
- Branche principale : `main`.
- Dépôt distant **public** créé via `gh repo create` sous le compte `mouhamadoulo`.
- Nom proposé : **`medialib-hibernate`**.
- `user.name` / `user.email` du dépôt forcés sur `Mouhamadou Moustapha LO` /
  `mouhamadoulo39@gmail.com`.
- **Aucun trailer `Co-Authored-By`, aucune mention d'outil d'IA** dans les messages de
  commit ni dans la description du dépôt. Cette règle prévaut sur toute configuration
  d'attribution par défaut.
- Commits en Conventional Commits, un commit par module.
- `.gitignore` : `target/`, `.idea/`, `*.iml`, `.vscode/`, `*.log`.

### Rythme de livraison

**Push incrémental, sans pause.** Le dépôt distant est créé et poussé dès que le squelette
Maven et les trois documents existent. Ensuite, **un commit poussé par module**, enchaînés
sans attendre de relecture. Le dépôt est donc public et visiblement incomplet pendant la
construction — `doc/TASKS.md` sert d'indicateur d'avancement, et le `README.md` signale
l'état en cours. Tu suis sur GitHub et tu interromps quand tu veux.

Conséquence : `README.md` et `doc/TASKS.md` sont écrits **au premier commit**, comme
contrat de ce qui va être construit, puis mis à jour à chaque module.

---

## 10. Ce qui est laissé à l'implémentation

- Nommage exact des colonnes et tables (convention `snake_case`, naming strategy par défaut).
- Volumétrie précise des jeux de données de test (ordre de grandeur : 1 000 médias pour les
  démonstrations N+1, 100 000 lignes pour le batching).
- Découpage fin des classes de test à l'intérieur d'un module.
- Choix entre `persistence.xml` et configuration programmatique **par module** — les deux
  sont montrés au module 01, ensuite le plus lisible l'emporte.
- Mise en forme exacte des tableaux du `README.md`.

### Décisions de structure déjà arrêtées

- `pom.xml` parent directement à la racine de `test-hibernate/`, `artifactId`
  `medialib-hibernate`. Pas de sous-dossier intermédiaire.
- Spring Boot câblé par **import du BOM** `spring-boot-dependencies`, pas par
  `spring-boot-starter-parent` : le projet a déjà un parent.
- `00-support` écrit dans `src/main/java` et consommé par les autres modules avec
  `<scope>test</scope>`. Un `test-jar` ferait la même chose au prix d'une configuration de
  plugin supplémentaire, sans bénéfice.
- **Génération du schéma** : `hbm2ddl.auto=create-drop` dans les modules 01→10 et 12.
  **Flyway est démontré au module 11**, là où la comparaison `hbm2ddl` vs migrations
  versionnées *est* la leçon. Écrire douze jeux de migrations à la main, qu'il faudrait
  resynchroniser à chaque retouche d'entité, coûterait cher sans rien enseigner ailleurs.
- Les commandes par chapitre annoncées dans le `README.md` incluent `-am`
  (`mvn -pl 07-fetching -am test`), sinon `00-support` n'est pas construit.
- Prose et noms de méthodes de test **en français**, identifiants du domaine **en anglais**
  (`Media`, `Loan`, `Member`) — cohérent avec l'exemple du §1.

---

## 11. Critère d'acceptation

```bash
mvn clean verify
```

passe intégralement, Docker Desktop démarré, en quelques minutes, et :

- **chaque module contient au moins un test qui assertionne le SQL émis**, pas seulement le
  résultat fonctionnel ;
- **chaque notion structurante listée au §6 est cochée dans `doc/TASKS.md`**, avec le test
  ou la section de documentation qui la couvre ;
- aucune assertion ne porte sur un temps d'exécution ;
- les tests `@Tag("slow")` passent aussi sous `mvn clean verify -Pslow`.
