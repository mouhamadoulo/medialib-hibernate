# MediaLib — Projet fil rouge Hibernate

Apprendre Hibernate par un domaine métier réel : un réseau de médiathèques municipales.

Chaque notion d'Hibernate y naît d'un besoin métier, pas d'un exemple artificiel. Et chaque
notion est prouvée par un test qui assertionne le **SQL réellement émis**.

**239 tests, 13 modules, tous verts.** 242 avec les tests à grosse volumétrie (`-Pslow`).
Chaque notion est cochée dans [`doc/TASKS.md`](doc/TASKS.md).

---

## Le principe

La difficulté avec Hibernate n'est pas d'écrire du code qui fonctionne. C'est de savoir **ce
qui se passe réellement en base** quand ce code s'exécute. Un `@OneToMany` qui passe au vert
en test peut générer 400 requêtes en production.

Donc ici, un test n'affirme pas « ça marche ». Il affirme **« 1 requête, pas 11 »** :

```java
@Test
void join_fetch_ramene_tout_en_une_seule_requete() {
    sql().reset();

    List<Loan> loans = inTransactionReturning(session -> session.createQuery(
            "select l from Loan l join fetch l.member join fetch l.copy.media", Loan.class)
            .getResultList());

    loans.forEach(l -> l.getMember().getFullName());   // aucun chargement paresseux

    assertThat(sql().selectCount()).isEqualTo(1);      // la preuve
}
```

C'est ça qu'on vient apprendre.

---

## Le domaine : MediaLib

| Concept | Rôle |
|---|---|
| `Media` | Œuvre du catalogue. Abstraite : `Book`, `Dvd`, `Vinyl`, `Magazine` |
| `Copy` | Exemplaire physique d'un `Media`, rattaché à une `Branch`. C'est lui qu'on emprunte |
| `Branch` | Agence du réseau. Sert d'axe de multi-tenancy |
| `Author` | Auteur, réalisateur ou artiste |
| `Publisher` | Éditeur |
| `Member` | Adhérent, avec une `Address` et une `MembershipCard` |
| `Loan` | Emprunt d'une `Copy` par un `Member` |
| `Reservation` | Réservation en file d'attente |
| `Fine` | Amende de retard, montant en `Money` |
| `Review` | Avis d'un adhérent sur un média |

Pourquoi ce domaine : l'héritage (`Book` / `Dvd` / `Vinyl`), la concurrence (deux adhérents
sur le dernier exemplaire), le multi-tenancy (une agence = un schéma), l'audit (qui a
emprunté quoi) et la volumétrie (un catalogue de 100 000 médias) y sont des **besoins**, pas
des prétextes à démonstration.

---

## Prérequis

| | |
|---|---|
| **JDK** | 21 ou supérieur |
| **Maven** | 3.9 ou supérieur |
| **Docker** | **Docker Desktop doit être démarré.** Les tests tournent contre un vrai PostgreSQL via Testcontainers |

Pourquoi pas H2 : il simule les verrous pessimistes, les séquences et les schémas
multi-tenants. Les chapitres 08, 10 et 11 seraient faux.

## Démarrage

```bash
git clone https://github.com/mouhamadoulo/medialib-hibernate.git
```

```bash
docker info
```

```bash
mvn clean verify
```

Le premier lancement télécharge l'image `postgres:17-alpine`.

---

## Les chapitres

Chaque module est **autonome** : on peut ouvrir le chapitre 07 sans avoir lu les six
précédents. Les entités sont volontairement redéfinies d'un chapitre à l'autre — voir
[CLAUDE.md](CLAUDE.md).

| # | Module | Ce qu'on y apprend | Lancer ce chapitre seul |
|---|---|---|---|
| 00 | `00-support` | Socle de test : compteur SQL, conteneur PostgreSQL | `mvn -pl 00-support test` |
| 01 | `01-bootstrap` | SessionFactory, cycle de vie d'une entité, dirty checking, flush | `mvn -pl 01-bootstrap -am test` |
| 02 | `02-mapping-basics` | Identifiants, embeddables, converters, `UserType`, `@Formula` | `mvn -pl 02-mapping-basics -am test` |
| 03 | `03-associations` | Les quatre cardinalités, côté propriétaire, cascades, `equals`/`hashCode` | `mvn -pl 03-associations -am test` |
| 04 | `04-collections` | Bags vs lists, `MultipleBagFetchException`, `@ElementCollection` | `mvn -pl 04-collections -am test` |
| 05 | `05-inheritance` | Les trois stratégies d'héritage, comparées sur les mêmes données | `mvn -pl 05-inheritance -am test` |
| 06 | `06-queries` | HQL, Criteria et métamodèle, SQL natif, projections DTO | `mvn -pl 06-queries -am test` |
| 07 | `07-fetching` | Proxies, `LazyInitializationException`, **N+1 chiffré**, `@EntityGraph` | `mvn -pl 07-fetching -am test` |
| 08 | `08-transactions-concurrency` | `@Version`, verrous pessimistes, **test à deux threads** | `mvn -pl 08-transactions-concurrency -am test` |
| 09 | `09-caching` | Cache L1, L2 Caffeine, cache de requêtes, invalidation | `mvn -pl 09-caching -am test` |
| 10 | `10-advanced` | Envers, soft delete, `@Filter`, interceptors, **multi-tenancy** | `mvn -pl 10-advanced -am test` |
| 11 | `11-performance` | Batching JDBC, `StatelessSession`, Flyway, plans d'exécution | `mvn -pl 11-performance -am test` |
| 12 | `12-spring-boot` | Ce que Spring ajoute — et le piège **OSIV** | `mvn -pl 12-spring-boot test` |

> Le `-am` (*also make*) est obligatoire : il construit `00-support`, dont dépendent tous les
> chapitres. Sans lui, le module ne résout pas. Le module 12 est autonome et s'en passe.

---

## Conventions de test

Tout test de chapitre étend `HibernateTestBase`, qui fournit :

| Méthode | Rôle |
|---|---|
| `entities()` | *(à implémenter)* les entités mappées par le chapitre |
| `extraSettings()` | réglages Hibernate propres au chapitre (cache, batch, tenant) |
| `sessionFactory()` | la `SessionFactory` du chapitre |
| `sql()` | le compteur SQL, remis à zéro avant chaque test |
| `inTransaction(...)` | exécute un bloc dans une transaction |
| `inTransactionReturning(...)` | idem, avec valeur de retour |

Le compteur s'appuie sur le `StatementInspector` d'Hibernate — et non sur un proxy de
`DataSource` — pour ne déformer aucun bootstrap. Il **ignore le DDL** : sans ça, les
`create table` de `hbm2ddl` fausseraient chaque assertion.

### Tests lents

Les tests à grosse volumétrie portent `@Tag(Slow.SLOW)` et sont **exclus du build par
défaut** :

```bash
mvn clean verify -Pslow
```

### Mode *reuse* de Testcontainers (facultatif)

Pour éviter de redémarrer un conteneur à chaque lancement, ajouter dans
`~/.testcontainers.properties` :

```properties
testcontainers.reuse.enable=true
```

Le conteneur reste alors actif entre deux builds. Ce n'est jamais nécessaire pour que le
build passe.

---

## Mesures et performance

**Aucun test n'assertionne un temps d'exécution.** Les benchmarks assertionnent le nombre et
la forme des requêtes SQL — grandeur déterministe — et se contentent de journaliser les
durées. Une assertion temporelle serait instable chez quiconque clone le dépôt, et les
chiffres relevés sur une machine Windows/aarch64 avec Docker sous WSL2 ne se reproduisent
nulle part ailleurs.


---

## Table de correspondance : Hibernate nu ↔ Spring Boot

Le module 12 n'est pas une redite. Il répond à une seule question, chapitre par chapitre :
**qu'est-ce que Spring ajoute, et qu'est-ce qu'il cache ?**

| # | Chapitre | L'équivalent Spring | Ce qui se passe réellement derrière |
|---|---|---|---|
| 01 | `SessionFactory` bâtie à la main | `spring.datasource.*` + auto-configuration | Boot construit la même `SessionFactory`. Un réglage Hibernate se passe par `HibernatePropertiesCustomizer`, et non plus dans un `Map` |
| 01 | `session.beginTransaction()` / `commit()` | `@Transactional` | Un **proxy**. Tout appel qui ne le traverse pas — l'auto-invocation — n'est pas transactionnel |
| 01 | `FlushMode` explicite | `@Transactional(readOnly = true)` | Hibernate passe en `FlushMode.MANUAL` et saute le dirty checking. Une modification est **silencieusement perdue** |
| 02 | Nommage par défaut d'Hibernate | `CamelCaseToUnderscoresNamingStrategy` | Boot l'installe par défaut. C'est lui, pas Hibernate, qui produit le `snake_case` |
| 02 | `hbm2ddl.auto` passé à la main | `spring.jpa.hibernate.ddl-auto` | Même réglage, même danger. `update` reste à proscrire |
| 03 | Mapping des associations | **identique** | Spring ne touche pas au mapping : c'est Hibernate qui travaille |
| 04 | Mapping des collections | **identique** | Idem. `MultipleBagFetchException` se lève exactement pareil |
| 05 | Stratégies d'héritage | **identique** | Idem |
| 06 | `session.createQuery("...")` | Requête dérivée, `@Query`, `Specification` | Spring Data **écrit le HQL** à notre place. Le SQL émis est le même — ce module l'assertionne |
| 06 | Projection par record | Projection par interface | Même `select` réduit, autre syntaxe |
| 06 | Métamodèle statique + Criteria | `Specification` | L'API Criteria, emballée. Utile pour les critères optionnels |
| 07 | `join fetch` / `@EntityGraph` | `@EntityGraph` sur le repository | Même graphe, déclaré sur la méthode |
| 07 | `LazyInitializationException` | **OSIV, activé par défaut** | Un filtre tient la session ouverte jusqu'à la fin du rendu HTTP. L'exception disparaît — et une connexion est retenue pendant tout le rendu, avec des requêtes émises depuis la couche de présentation |
| 08 | `@Version`, verrous pessimistes | **identique** (`@Lock` sur le repository) | Hibernate fait le travail. `@Lock` ne fait que passer le `LockModeType` |
| 08 | Démarcation manuelle | `Propagation.REQUIRES_NEW` | Une transaction indépendante, qui survit au rollback de l'appelante |
| 09 | Cache L2 configuré à la main | `spring.jpa.properties.hibernate.cache.*` | Mêmes propriétés, autre fichier |
| 10 | Envers, `@Filter`, multi-tenancy | **identique** | Spring Data n'y change rien. Le multi-tenancy se branche par les mêmes services |
| 11 | `hibernate.jdbc.batch_size` | `spring.jpa.properties.hibernate.jdbc.batch_size` | Même réglage. **Boot ne l'active pas par défaut** |
| — | `HibernateTestBase` maison | `@DataJpaTest`, `@ServiceConnection` | Boot démarre le conteneur et câble la `DataSource` seul. Mais `@DataJpaTest` rend le test transactionnel et l'annule : aucun commit n'est jamais exercé, et aucune `LazyInitializationException` n'est détectable |

**Ce qu'il faut en retenir** : sur les onze chapitres, sept sont rigoureusement identiques —
c'est Hibernate qui fait le travail. Spring apporte la suppression de la plomberie et les
transactions déclaratives. Il cache le cycle de la session, le moment du flush, et introduit
un piège qui n'existe pas sans lui : l'OSIV.

---

## Documentation

| Fichier | Contenu |
|---|---|
| [`doc/TASKS.md`](doc/TASKS.md) | Avancement, notion par notion |
| [`CLAUDE.md`](CLAUDE.md) | Conventions du dépôt |
| [`docs/superpowers/specs/`](docs/superpowers/specs/) | Document de conception |
| [`docs/superpowers/plans/`](docs/superpowers/plans/) | Plan d'implémentation |
