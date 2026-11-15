# MediaLib — Projet fil rouge Hibernate

Apprendre Hibernate par un domaine métier réel : un réseau de médiathèques municipales.

Chaque notion d'Hibernate y naît d'un besoin métier, pas d'un exemple artificiel. Et chaque
notion est prouvée par un test qui assertionne le **SQL réellement émis**.

> 🚧 **Projet en construction.** L'avancement chapitre par chapitre est suivi dans
> [`doc/TASKS.md`](doc/TASKS.md).

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

## Documentation

| Fichier | Contenu |
|---|---|
| [`doc/TASKS.md`](doc/TASKS.md) | Avancement, notion par notion |
| [`CLAUDE.md`](CLAUDE.md) | Conventions du dépôt |
| [`docs/superpowers/specs/`](docs/superpowers/specs/) | Document de conception |
| [`docs/superpowers/plans/`](docs/superpowers/plans/) | Plan d'implémentation |
