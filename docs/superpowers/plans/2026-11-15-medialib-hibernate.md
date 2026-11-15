# MediaLib — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construire un projet Maven multi-modules où chaque notion d'Hibernate est mise en scène par un besoin métier de médiathèque et prouvée par un test qui assertionne le SQL réellement émis.

**Architecture:** Un `pom.xml` parent figeant toutes les versions, un module `00-support` portant le socle de test (conteneur PostgreSQL, compteur SQL basé sur `StatementInspector`, classe de base JUnit), puis douze modules-chapitres indépendants. Les entités ne sont **jamais** partagées entre modules : leur redéfinition d'un chapitre à l'autre est la leçon. Chaque module a son propre conteneur, donc sa propre base, donc aucune collision de schéma.

**Tech Stack:** Java 21 · Maven 3.9.13 · Hibernate 7.4.16.Final · PostgreSQL 42.7.14 via Testcontainers 1.21.4 · JUnit 6.1.3 · AssertJ 3.27.7 · Caffeine 3.3.1 · Flyway 13.12.2 (module 11) · datasource-proxy 1.11.1 (module 11) · Spring Boot 4.1.2 (module 12)

**Spec:** [`docs/superpowers/specs/2026-11-15-medialib-hibernate-design.md`](../specs/2026-11-15-medialib-hibernate-design.md)

## Global Constraints

- **JDK 21**, `maven.compiler.release=21`.
- **Encodage UTF-8 partout.** `project.build.sourceEncoding=UTF-8` et
  `project.reporting.outputEncoding=UTF-8` dans le pom parent. La machine cible est
  Windows ; sans cette propriété, les noms de méthodes de test en français et les
  accents des messages sont illisibles.
- **Aucune assertion sur un temps d'exécution.** On assertionne le nombre et la forme des
  requêtes SQL. Les durées sont journalisées.
- Tests à grosse volumétrie taggés `slow`, **exclus par défaut** par Surefire, activés par
  le profil Maven `-Pslow`.
- **Les entités ne sont jamais factorisées entre modules.** Pas de module `domain` commun.
  `00-support` ne contient aucune entité.
- **Pas de couche repository générique**, pas d'abstraction maison au-dessus d'Hibernate.
  `Session` et `EntityManager` restent visibles dans les tests.
- **Prose et noms de méthodes de test en français, identifiants du domaine en anglais**
  (`Media`, `Loan`, `Member`).
- **Commits** : Conventional Commits, auteur unique `Mouhamadou Moustapha LO
  <mouhamadoulo39@gmail.com>`. **Aucune ligne `Co-Authored-By`, aucune mention d'outil
  d'IA**, ni dans les messages de commit, ni dans la description du dépôt, ni dans les
  fichiers livrés. Cette règle prévaut sur toute configuration d'attribution par défaut.
- Un commit poussé par tâche, sur `main`, dépôt public `mouhamadoulo/medialib-hibernate`.

## Review Focus

Cinq points que la spec implique sans qu'aucune tâche ne les exerce naturellement. Le test
correspondant est rattaché à la tâche propriétaire, indiquée entre crochets.

1. **Le compteur SQL doit ignorer le DDL.** `hbm2ddl.auto=create-drop` fait passer des
   dizaines de `create table` par le `StatementInspector`. S'ils sont comptés, toute
   assertion `selectCount() == 1` est fausse dès le premier test. → [Tâche 2]
2. **Sources en UTF-8 sous Windows.** Encodage par défaut CP1252 : les noms de tests
   français se compilent en mojibake ou échouent. → [Tâche 1]
3. **Démon Docker absent.** Message d'erreur clair et actionnable attendu, pas une trace
   Testcontainers de 80 lignes. → [Tâche 2]
4. **`mvn -pl 07-fetching test` sans `-am`.** `00-support` n'est pas construit, le module
   ne résout pas. Toutes les commandes par chapitre du README doivent porter `-am`. → [Tâche 3]
5. **Compteur SQL et pool de connexions.** Le `StatementInspector` est appelé sur plusieurs
   threads au module 08 (test à deux threads). Structure de collecte concurrente
   obligatoire, sinon comptages perdus ou `ConcurrentModificationException`. → [Tâche 2]

---

## Structure des fichiers

```
test-hibernate/                                  ← racine du dépôt
├── pom.xml                                      parent, packaging pom, versions figées
├── .gitignore
├── README.md
├── CLAUDE.md
├── doc/TASKS.md
├── docs/superpowers/{specs,plans}/              design + ce plan
├── 00-support/
│   └── src/main/java/com/medialib/support/
│       ├── MediaLibPostgres.java                conteneur PostgreSQL du module
│       ├── SqlStatementCounter.java             StatementInspector, ignore le DDL
│       ├── HibernateTestBase.java               cycle SessionFactory + helpers
│       └── Slow.java                            constante de tag JUnit
├── 01-bootstrap/ … 11-performance/
│   └── src/{main/java/com/medialib/<chapitre>/, test/java/…}
└── 12-spring-boot/                              autonome, BOM Boot, pas de 00-support
```

---

## Tâche 1 : Socle Maven et dépôt Git

**Files:**
- Create: `pom.xml`, `.gitignore`

**Interfaces:**
- Produces: propriétés de version (`hibernate.version`, `testcontainers.version`,
  `junit.version`, `assertj.version`, `postgresql.version`), `dependencyManagement`
  important `org.hibernate.orm:hibernate-platform`,
  `org.testcontainers:testcontainers-bom`, `org.junit:junit-bom` ; configuration Surefire
  avec `<excludedGroups>slow</excludedGroups>` ; profil `slow` qui vide `excludedGroups`.
  Tous les modules en héritent.

- [ ] **Étape 1 : Écrire `pom.xml` parent**

`groupId` `com.medialib`, `artifactId` `medialib-hibernate`, `packaging` `pom`,
`version` `1.0.0-SNAPSHOT`. Propriétés : `maven.compiler.release=21`,
`project.build.sourceEncoding=UTF-8`, `project.reporting.outputEncoding=UTF-8`, et une
propriété par version du §7 de la spec. `<modules>` listant les treize modules.
`dependencyManagement` important les trois BOM ci-dessus. `maven-surefire-plugin`
configuré avec `excludedGroups=slow`. Profil `slow` surchargeant `excludedGroups` à vide.

- [ ] **Étape 2 : Écrire `.gitignore`**

`target/`, `.idea/`, `*.iml`, `.vscode/`, `*.log`, `.DS_Store`.

- [ ] **Étape 3 : Vérifier que le parent résout**

Run: `mvn -N validate`
Expected: `BUILD SUCCESS`. (`-N` : les modules n'existent pas encore.)

- [ ] **Étape 4 : Vérifier l'encodage UTF-8** *(Review Focus 2)*

Run: `mvn -N help:evaluate -Dexpression=project.build.sourceEncoding -q -DforceStdout`
Expected: affiche `UTF-8`.

- [ ] **Étape 5 : Initialiser le dépôt et créer le distant**

```bash
git init -b main
git config user.name "Mouhamadou Moustapha LO"
git config user.email "mouhamadoulo39@gmail.com"
git add pom.xml .gitignore docs/
git commit -m "chore: socle maven multi-modules et versions figees"
gh repo create medialib-hibernate --public --source=. --remote=origin --push
```

La description du dépôt est définie séparément, sans mention d'outil :
`gh repo edit --description "Projet fil rouge Hibernate : apprendre Hibernate par un domaine metier reel, chaque notion prouvee par un test qui assertionne le SQL emis."`

- [ ] **Étape 6 : Vérifier l'auteur du commit** *(contrainte globale)*

Run: `git log -1 --format='%an <%ae>%n%b'`
Expected: `Mouhamadou Moustapha LO <mouhamadoulo39@gmail.com>`, et **corps vide** — aucun
trailer `Co-Authored-By`.

---

## Tâche 2 : `00-support` — socle de test

**Files:**
- Create: `00-support/pom.xml`
- Create: `00-support/src/main/java/com/medialib/support/{MediaLibPostgres,SqlStatementCounter,HibernateTestBase,Slow}.java`
- Test: `00-support/src/test/java/com/medialib/support/SqlStatementCounterTest.java`

**Interfaces:**
- Consumes: le parent de la Tâche 1.
- Produces, utilisé par les modules 01→11 :
  - `SqlStatementCounter implements org.hibernate.resource.jdbc.spi.StatementInspector`
    — `String inspect(String sql)` ; `void reset()` ; `List<String> all()` ;
    `int count()` ; `int selectCount()` ; `int insertCount()` ; `int updateCount()` ;
    `int deleteCount()` ; `String lastQuery()`. **Ignore le DDL** (`create`, `alter`,
    `drop`, `truncate`, `comment on`). Collecte **thread-safe**
    (`CopyOnWriteArrayList` ou `Collections.synchronizedList`).
  - `MediaLibPostgres` — `static PostgreSQLContainer<?> instance()`, image
    `postgres:17-alpine`, démarrage paresseux, `withReuse(true)` honoré seulement si
    l'utilisateur a activé le mode reuse.
  - `HibernateTestBase` — classe abstraite JUnit 6. `protected abstract Class<?>[] entities()` ;
    `protected Map<String,Object> extraSettings()` (défaut : vide) ;
    `protected SessionFactory sessionFactory()` ; `protected SqlStatementCounter sql()` ;
    `protected void inTransaction(Consumer<Session> work)` ;
    `protected <T> T inTransactionReturning(Function<Session,T> work)` ;
    `@BeforeEach` réinitialise le compteur, `@AfterAll` ferme la `SessionFactory`.
    Le `SessionFactory` est bâti par `StandardServiceRegistryBuilder` avec
    `AvailableSettings.STATEMENT_INSPECTOR` pointant sur le compteur et
    `hbm2ddl.auto=create-drop`.
  - `Slow` — `public static final String SLOW = "slow"`, pour `@Tag(Slow.SLOW)`.

- [ ] **Étape 1 : Écrire les tests défaillants**

```java
class SqlStatementCounterTest {

    @Test
    void ignore_le_ddl_pour_ne_compter_que_le_dml() {              // Review Focus 1
        SqlStatementCounter counter = new SqlStatementCounter();
        counter.inspect("create table media (id bigint not null, primary key (id))");
        counter.inspect("alter table copy add constraint fk_media foreign key (media_id) references media");
        counter.inspect("drop table if exists media cascade");
        counter.inspect("select m.id from media m");

        assertThat(counter.count()).isEqualTo(1);
        assertThat(counter.selectCount()).isEqualTo(1);
    }

    @Test
    void inspect_retourne_le_sql_inchange() {
        SqlStatementCounter counter = new SqlStatementCounter();
        String sql = "select m.id from media m";
        assertThat(counter.inspect(sql)).isEqualTo(sql);           // contrat StatementInspector
    }

    @Test
    void classe_les_instructions_par_type() {
        SqlStatementCounter counter = new SqlStatementCounter();
        counter.inspect("select 1");
        counter.inspect("insert into media (id) values (1)");
        counter.inspect("update media set title = 'x'");
        counter.inspect("delete from media");

        assertThat(counter.selectCount()).isEqualTo(1);
        assertThat(counter.insertCount()).isEqualTo(1);
        assertThat(counter.updateCount()).isEqualTo(1);
        assertThat(counter.deleteCount()).isEqualTo(1);
        assertThat(counter.lastQuery()).startsWith("delete");
    }

    @Test
    void reset_vide_le_compteur() {
        SqlStatementCounter counter = new SqlStatementCounter();
        counter.inspect("select 1");
        counter.reset();
        assertThat(counter.count()).isZero();
    }

    @Test
    void collecte_sans_perte_depuis_plusieurs_threads() throws Exception {   // Review Focus 5
        SqlStatementCounter counter = new SqlStatementCounter();
        int threads = 8, parThread = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int t = 0; t < threads; t++) {
            pool.submit(() -> { for (int i = 0; i < parThread; i++) counter.inspect("select 1"); });
        }
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(counter.count()).isEqualTo(threads * parThread);
    }
}
```

- [ ] **Étape 2 : Lancer les tests et vérifier qu'ils échouent**

Run: `mvn -pl 00-support test`
Expected: FAIL — `SqlStatementCounter` n'existe pas.

- [ ] **Étape 3 : Implémenter les quatre classes**

Signatures ci-dessus (bloc Interfaces). Pour `SqlStatementCounter`, la classification se
fait sur le premier mot de l'instruction normalisée (`strip`, `toLowerCase`) ; le DDL est
écarté avant collecte, donc n'apparaît ni dans `all()` ni dans `count()`.

Pour `MediaLibPostgres`, échouer tôt et clairement si le démon Docker est injoignable
*(Review Focus 3)* : capturer l'échec de démarrage du conteneur et relancer une
`IllegalStateException` dont le message dit de démarrer Docker Desktop et donne la commande
de vérification `docker info`.

- [ ] **Étape 4 : Lancer les tests et vérifier qu'ils passent**

Run: `mvn -pl 00-support test`
Expected: PASS, 5 tests. Aucun conteneur démarré — ces tests sont purement unitaires.

- [ ] **Étape 5 : Commit et push**

```bash
git add 00-support
git commit -m "feat(support): compteur sql, conteneur postgres et socle de test"
git push
```

---

## Tâche 3 : Documentation et contrat d'avancement

**Files:**
- Create: `README.md`, `CLAUDE.md`, `doc/TASKS.md`

**Interfaces:**
- Consumes: la liste des modules (Tâche 1), les commandes par chapitre.
- Produces: `doc/TASKS.md` sert de contrat — chaque tâche suivante y coche ses notions.

- [ ] **Étape 1 : Écrire `README.md`**

Présentation de MediaLib et du principe « on assertionne le SQL, pas le résultat » avec
l'exemple de code du §1 de la spec · prérequis : JDK 21, Maven 3.9+, **Docker Desktop
démarré** · démarrage : `git clone`, `docker info` pour vérifier, `mvn clean verify` ·
tableau des 13 modules (notion, module, commande) — **chaque commande par chapitre porte
`-am`** *(Review Focus 4)*, par exemple `mvn -pl 07-fetching -am test` · schéma du modèle
de domaine · section « mode reuse Testcontainers » en option · section « tests lents » et
`-Pslow` · index des notions renvoyant au module et à la classe de test · mention que le
projet est en construction, renvoyant à `doc/TASKS.md`.

- [ ] **Étape 2 : Écrire `doc/TASKS.md`**

Une section par module, une case à cocher par notion structurante du §6 de la spec, avec
une colonne pour la classe de test qui la couvre. Tout est décoché à ce stade sauf les
tâches 1 à 3.

- [ ] **Étape 3 : Écrire `CLAUDE.md`**

Conventions du dépôt : structure multi-modules · **règle de non-factorisation des entités
entre modules, et pourquoi** · format des tests (assertion SQL obligatoire via
`HibernateTestBase.sql()`) · versions figées, ne pas mettre à jour sans raison · tests
`slow` et profil Maven · **règle de commit : auteur unique Mouhamadou Moustapha LO, aucun
trailer de co-auteur, aucune mention d'outil d'IA** · commandes utiles.

- [ ] **Étape 4 : Vérifier les commandes annoncées** *(Review Focus 4)*

Run: `mvn -pl 00-support -am validate`
Expected: `BUILD SUCCESS`, et le réacteur liste le parent **et** `00-support`.

- [ ] **Étape 5 : Commit et push**

```bash
git add README.md CLAUDE.md doc/TASKS.md
git commit -m "docs: readme, conventions et suivi d'avancement"
git push
```

---

## Tâches 4 à 15 : les modules-chapitres

Les douze tâches qui suivent partagent la même forme. Pour chacune :

**Files:** `NN-<nom>/pom.xml`, entités sous
`NN-<nom>/src/main/java/com/medialib/<chapitre>/`, tests sous
`NN-<nom>/src/test/java/com/medialib/<chapitre>/`.

**Interfaces:** Consomme `00-support` (`HibernateTestBase`, `SqlStatementCounter`,
`MediaLibPostgres`) en `<scope>test</scope>`. Ne produit rien pour les autres
modules — chaque chapitre est hermétique.

**Steps (identiques pour les douze) :**

- [ ] **Étape 1** : écrire les entités du chapitre.
- [ ] **Étape 2** : écrire les tests listés ci-dessous, chacun avec au moins une assertion
  sur `sql()`.
- [ ] **Étape 3** : `mvn -pl NN-<nom> -am test` → FAIL.
- [ ] **Étape 4** : implémenter/corriger le mapping jusqu'au vert.
- [ ] **Étape 5** : `mvn -pl NN-<nom> -am test` → PASS.
- [ ] **Étape 6** : cocher les notions du module dans `doc/TASKS.md`, ajouter la ligne du
  module au tableau du `README.md`.
- [ ] **Étape 7** : commit `feat(NN-<nom>): <résumé>` puis `git push`.

---

### Tâche 4 : `01-bootstrap`

**Entités :** `Member` (id, firstName, lastName, email), `Branch` (id, name, city).

**Tests :**

| Classe | Méthode | Assertion clé |
|---|---|---|
| `BootstrapTest` | `demarre_une_session_factory_par_api_programmatique` | `sessionFactory().isOpen()` |
| `BootstrapTest` | `demarre_une_session_factory_par_persistence_xml` | `EntityManagerFactory` ouvert, mêmes entités |
| `CycleDeVieTest` | `une_entite_transient_ne_declenche_aucun_sql` | `sql().count() == 0` avant `persist` |
| `CycleDeVieTest` | `persist_insere_au_flush_pas_a_l_appel` | `insertCount() == 0` après `persist`, `== 1` après `flush` |
| `CycleDeVieTest` | `find_sur_entite_deja_chargee_ne_requete_pas` | 2e `find` → `selectCount()` inchangé |
| `CycleDeVieTest` | `getReference_ne_requete_pas_avant_acces` | `selectCount() == 0`, puis `1` après accès à un champ |
| `DirtyCheckingTest` | `modifier_une_entite_managee_suffit_a_la_mettre_a_jour` | aucun `save` appelé, `updateCount() == 1` |
| `DirtyCheckingTest` | `une_entite_detachee_n_est_plus_suivie` | après `detach`, `updateCount() == 0` |
| `DirtyCheckingTest` | `merge_reattache_et_met_a_jour` | `updateCount() == 1` |
| `FlushModeTest` | `flush_mode_commit_differe_les_ecritures` | aucune écriture avant commit |
| `RemoveTest` | `remove_supprime_au_flush` | `deleteCount() == 1` |

---

### Tâche 5 : `02-mapping-basics`

**Entités :** `Media` (id, title, `isbn`, `format`, `pageCount`), `Member` avec
`@Embedded Address`, `Publisher`, `Fine` portant un `Money` converti.
**Types valeur :** `Address` (`@Embeddable`), `Money` (`AttributeConverter`), `Isbn`
(`UserType`), `MediaFormat` (enum), `LoanKey` (`@EmbeddedId`).

**Tests :**

| Classe | Méthode | Assertion clé |
|---|---|---|
| `IdentifiantsTest` | `identity_desactive_le_batching` | `insertCount() == 3` pour 3 entités, pas de batch |
| `IdentifiantsTest` | `sequence_permet_le_batching` | une seule exécution JDBC pour 3 insertions |
| `IdentifiantsTest` | `uuid_genere_un_identifiant_cote_java` | id non nul avant `flush` |
| `EmbeddableTest` | `une_adresse_est_aplatie_dans_la_table_du_membre` | `lastQuery()` contient `street`, aucun `join` |
| `EmbeddedIdTest` | `une_cle_composite_est_traduite_en_clause_where_a_deux_colonnes` | `lastQuery()` contient les deux colonnes |
| `ConverterTest` | `money_est_persiste_en_centimes` | lecture SQL native → valeur entière attendue |
| `UserTypeTest` | `isbn_est_persiste_normalise` | valeur en base sans tirets |
| `EnumTest` | `ordinal_casse_au_reordonnancement_de_l_enum` | le test **documente le piège** : lecture d'une valeur écrite sous l'ancien ordre |
| `FormulaTest` | `un_champ_formula_est_calcule_en_sql` | `lastQuery()` contient l'expression, pas de 2e requête |

---

### Tâche 6 : `03-associations`

**Entités :** `Media`, `Copy`, `Branch`, `Member`, `MembershipCard`, `Author`, `Loan`.

**Tests :**

| Classe | Méthode | Assertion clé |
|---|---|---|
| `ManyToOneTest` | `copy_vers_media_ecrit_la_cle_etrangere` | `insertCount() == 2`, FK dans l'insert |
| `OneToManyTest` | `unidirectionnel_impose_une_table_de_jonction` | `lastQuery()` cible la table de jonction |
| `OneToManyTest` | `bidirectionnel_avec_mappedby_n_ecrit_qu_une_fois` | `insertCount()` strictement inférieur au cas unidirectionnel |
| `OneToManyTest` | `oublier_de_synchroniser_les_deux_cotes_laisse_la_fk_nulle` | documente le piège |
| `OneToOneTest` | `mapsid_partage_la_cle_primaire` | aucune colonne FK distincte |
| `ManyToManyTest` | `media_et_author_passent_par_une_table_de_jonction` | 3 insertions |
| `ManyToManyTest` | `un_attribut_sur_la_relation_impose_une_entite_d_association` | `Loan` porte `loanedAt` |
| `CascadeTest` | `persist_en_cascade_propage_l_insertion` | `insertCount() == 2` sur un seul `persist` |
| `CascadeTest` | `remove_en_cascade_supprime_les_enfants` | `deleteCount()` attendu |
| `OrphanRemovalTest` | `retirer_de_la_collection_supprime_l_orphelin` | `deleteCount() == 1` |
| `EqualsHashCodeTest` | `un_set_perd_l_entite_si_hashcode_depend_de_l_id_genere` | documente le piège |

---

### Tâche 7 : `04-collections`

**Entités :** `Media` avec `List<Copy>`, `Set<String> tags` (`@ElementCollection`),
`Map<Language,String> titles`, `List<Review>` ordonnée.

**Tests :**

| Classe | Méthode | Assertion clé |
|---|---|---|
| `BagVsListTest` | `modifier_un_bag_supprime_tout_et_reinsere` | `deleteCount() == 1` + `insertCount() == n` |
| `BagVsListTest` | `une_list_indexee_par_ordercolumn_met_a_jour_ciblement` | `updateCount()` ciblé |
| `OrderByTest` | `orderby_trie_en_sql` | `lastQuery()` contient `order by` |
| `OrderByTest` | `sortedset_trie_en_memoire` | `lastQuery()` **ne** contient **pas** `order by` |
| `MultipleBagFetchTest` | `deux_bags_en_join_fetch_levent_une_exception` | `MultipleBagFetchException` attendue |
| `MultipleBagFetchTest` | `passer_les_collections_en_set_resout_le_probleme` | 1 requête |
| `ElementCollectionTest` | `les_tags_vivent_dans_une_table_dediee_sans_entite` | insertions sur la table de tags |
| `MapTest` | `une_map_est_mappee_par_mapkeycolumn` | clé présente dans l'insert |
| `BatchSizeTest` | `batchsize_regroupe_le_chargement_des_collections` | `selectCount()` passe de n+1 à ⌈n/taille⌉+1 |

---

### Tâche 8 : `05-inheritance`

**Trois hiérarchies parallèles**, mêmes champs, stratégies différentes :
`single.*` (`Media`/`Book`/`Dvd`/`Vinyl`), `joined.*`, `tableperclass.*`.
Plus `AbstractAuditable` en `@MappedSuperclass`.

**Tests :**

| Classe | Méthode | Assertion clé |
|---|---|---|
| `SingleTableTest` | `une_seule_table_avec_colonne_discriminante` | 1 insert, `lastQuery()` contient `media_type` |
| `SingleTableTest` | `les_colonnes_des_sous_classes_sont_forcement_nullables` | documente la limite |
| `JoinedTest` | `une_insertion_touche_deux_tables` | `insertCount() == 2` |
| `JoinedTest` | `charger_un_book_fait_une_jointure` | `lastQuery()` contient `join` |
| `TablePerClassTest` | `une_requete_polymorphe_genere_un_union_all` | `lastQuery()` contient `union all` |
| `ComparatifTest` | `la_meme_requete_polymorphe_selon_les_trois_strategies` | compare les formes SQL ; **temps journalisé, non asserté** |
| `MappedSuperclassTest` | `une_mappedsuperclass_n_est_pas_interrogeable` | la requête HQL sur le type échoue |
| `RequetePolymorpheTest` | `type_filtre_sur_le_discriminant` | `lastQuery()` contient le filtre |

---

### Tâche 9 : `06-queries`

**Entités :** `Media`, `Author`, `Loan`, `Member`, `Branch`.
**Ajout pom :** `hibernate-jpamodelgen` en `annotationProcessorPath`.

**Tests :**

| Classe | Méthode | Assertion clé |
|---|---|---|
| `HqlTest` | `une_jointure_explicite_ne_produit_qu_une_requete` | `selectCount() == 1` |
| `HqlTest` | `une_sous_requete_correlee_reste_une_seule_requete` | `selectCount() == 1` |
| `CriteriaTest` | `le_metamodele_statique_est_genere` | `Media_.title` compile et s'utilise |
| `CriteriaTest` | `une_criteria_produit_le_meme_sql_que_le_hql_equivalent` | SQL normalisé identique |
| `NativeQueryTest` | `une_requete_native_est_mappee_par_sqlresultsetmapping` | résultat typé |
| `NamedQueryTest` | `une_namedquery_invalide_echoue_au_demarrage` | documente l'intérêt : validation précoce |
| `ProjectionTest` | `une_projection_record_ne_selectionne_que_les_colonnes_utiles` | `lastQuery()` **ne** contient **pas** `select m.*` |
| `ProjectionTest` | `tuple_donne_un_acces_par_alias` | valeurs attendues |
| `PaginationTest` | `setmaxresults_ajoute_limit_et_offset` | `lastQuery()` contient `limit` |
| `InjectionSqlTest` | `la_concatenation_ouvre_une_injection` | documente la faille |
| `InjectionSqlTest` | `un_parametre_nomme_la_ferme` | la valeur malveillante est traitée comme donnée |

---

### Tâche 10 : `07-fetching`

**Entités :** `Media`, `Author`, `Copy`, `Loan`, `Member`. Jeu de données : 1 000 médias.

**Tests :**

| Classe | Méthode | Assertion clé |
|---|---|---|
| `LazyEagerTest` | `manytoone_est_eager_par_defaut` | `selectCount() == 1` avec jointure implicite |
| `LazyEagerTest` | `onetomany_est_lazy_par_defaut` | 2e requête seulement à l'accès |
| `ProxyTest` | `getreference_rend_un_proxy_non_initialise` | `Hibernate.isInitialized()` faux |
| `LazyInitializationTest` | `acceder_hors_session_leve_lazyinitializationexception` | exception attendue |
| `LazyInitializationTest` | `hibernate_initialize_dans_la_session_resout` | aucune exception |
| `NPlusUnTest` | `le_n_plus_un_est_reproduit_et_chiffre` | **`selectCount() == 101`** sur 100 médias |
| `NPlusUnTest` | `join_fetch_ramene_tout_en_une_requete` | `selectCount() == 1` |
| `NPlusUnTest` | `entitygraph_nomme_resout_aussi` | `selectCount() == 1` |
| `NPlusUnTest` | `batchsize_ramene_a_un_nombre_borne_de_requetes` | `selectCount() == 11` |
| `NPlusUnTest` | `une_projection_dto_est_souvent_la_vraie_reponse` | 1 requête, colonnes réduites |
| `PaginationFetchTest` | `join_fetch_plus_pagination_pagine_en_memoire` | `lastQuery()` sans `limit`, avertissement `HHH90003004` |

---

### Tâche 11 : `08-transactions-concurrency`

**Entités :** `Copy` avec `@Version`, `Loan`, `Member`, `Branch`.

**Tests :**

| Classe | Méthode | Assertion clé |
|---|---|---|
| `IsolationTest` | `lecture_non_repetable_sous_read_committed` | deux lectures, deux valeurs |
| `VersionTest` | `la_colonne_version_est_incrementee_a_chaque_update` | `lastQuery()` contient `version=?` dans le `where` |
| `VersionTest` | `deux_sessions_concurrentes_levent_optimisticlockexception` | exception sur la seconde |
| `VersionTest` | `optimistic_force_increment_incremente_sans_modification` | `updateCount() == 1` |
| `PessimisticTest` | `pessimistic_write_emet_un_for_update` | `lastQuery()` contient `for update` |
| `PessimisticTest` | `nowait_echoue_immediatement_sur_ligne_verrouillee` | exception de verrou |
| `PessimisticTest` | `skip_locked_ignore_les_lignes_verrouillees` | résultat partiel attendu |
| `EmpruntConcurrentTest` | `deux_adherents_sur_le_dernier_exemplaire_un_seul_gagne` | **2 threads**, exactement 1 succès, 1 échec |
| `InterblocageTest` | `l_ordre_d_acquisition_evite_l_interblocage` | documente le remède |

---

### Tâche 12 : `09-caching`

**Entités :** `Branch` (`READ_ONLY`), `Media` (`READ_WRITE`), `Author`, collection
`Media.authors` cachée.
**Ajout pom :** `hibernate-jcache`, `caffeine`, `caffeine-jcache`.

**Tests :**

| Classe | Méthode | Assertion clé |
|---|---|---|
| `CacheNiveau1Test` | `deux_find_dans_la_meme_session_rendent_la_meme_instance` | identité `==`, `selectCount() == 1` |
| `CacheNiveau1Test` | `le_cache_l1_n_est_pas_desactivable` | documente le fait |
| `CacheNiveau2Test` | `une_entite_cachee_n_est_lue_qu_une_fois_entre_deux_sessions` | `selectCount() == 1` sur deux sessions |
| `CacheNiveau2Test` | `les_statistiques_remontent_hit_et_miss` | `Statistics` : 1 miss puis 1 hit |
| `StrategiesTest` | `read_only_refuse_la_modification` | exception attendue |
| `StrategiesTest` | `read_write_invalide_puis_recharge` | miss après update |
| `StrategiesTest` | `transactional_n_est_pas_supporte_sur_ce_stack` | **limite documentée**, le test montre le comportement réel |
| `CacheCollectionTest` | `le_cache_de_collection_se_configure_separement` | sans annotation dédiée, requête à chaque fois |
| `CacheRequeteTest` | `le_cache_de_requete_ajoute_une_lecture_de_region` | documente le coût |
| `InvalidationTest` | `un_bulk_update_contourne_le_cache_et_laisse_des_donnees_perimees` | documente le piège |

---

### Tâche 13 : `10-advanced`

**Entités :** `Loan` et `Fine` `@Audited`, `Media` en soft delete, `Branch` comme tenant.
**Ajout pom :** `hibernate-envers`.

**Tests :**

| Classe | Méthode | Assertion clé |
|---|---|---|
| `EnversTest` | `une_modification_cree_une_revision` | `AuditReader` : 2 révisions |
| `EnversTest` | `on_relit_l_etat_a_une_revision_donnee` | valeur historique attendue |
| `EnversTest` | `la_revision_porte_l_horodatage_et_les_metadonnees_custom` | `RevisionEntity` personnalisée |
| `SoftDeleteTest` | `remove_emet_un_update_pas_un_delete` | `deleteCount() == 0`, `updateCount() == 1` |
| `SoftDeleteTest` | `sqlrestriction_masque_les_lignes_archivees` | `lastQuery()` contient la restriction |
| `FilterTest` | `un_filter_parametre_restreint_a_une_agence` | filtre présent et paramétré |
| `FilterTest` | `le_filter_desactive_ne_restreint_rien` | comparaison |
| `InterceptorTest` | `un_listener_preinsert_horodate_automatiquement` | champ renseigné sans code appelant |
| `NaturalIdTest` | `une_recherche_par_naturalid_passe_par_son_cache` | `selectCount() == 1` sur deux accès |
| `DynamicUpdateTest` | `dynamicupdate_n_ecrit_que_les_colonnes_modifiees` | `lastQuery()` contient 1 seule colonne |
| `MultiTenancyTest` | `chaque_agence_ne_voit_que_son_schema` | données isolées entre deux tenants |

---

### Tâche 14 : `11-performance`

**Entités :** `Media`, `Copy`.
**Ajout pom :** `flyway-core`, `flyway-database-postgresql`, `HikariCP`, `datasource-proxy`.

**Tests :**

| Classe | Méthode | Assertion clé |
|---|---|---|
| `BatchingTest` | `sans_batch_size_chaque_insertion_est_un_aller_retour` | `datasource-proxy` : n exécutions |
| `BatchingTest` | `batch_size_regroupe_les_insertions` | ⌈n/taille⌉ exécutions |
| `BatchingTest` | `identity_desactive_silencieusement_le_batching` | **le piège central du chapitre** |
| `BatchingTest` | `order_inserts_regroupe_les_entites_de_meme_type` | ordre observé |
| `StatelessSessionTest` | `une_stateless_session_n_a_ni_cache_l1_ni_dirty_checking` | modification sans `update` → aucun SQL |
| `BulkTest` | `un_update_hql_en_masse_ne_fait_qu_une_requete` | `updateCount() == 1` pour n lignes |
| `BulkTest` | `le_contexte_de_persistance_devient_perime_apres_un_bulk` | documente le piège |
| `StreamTest` | `stream_ne_charge_pas_tout_en_memoire` | taille du contexte bornée |
| `FlywayTest` | `flyway_applique_les_migrations_versionnees` | table `flyway_schema_history` peuplée |
| `FlywayTest` | `le_schema_flyway_correspond_au_mapping_hibernate` | validation `hbm2ddl.auto=validate` passe |
| `GrosVolumeTest` | `cent_mille_insertions_en_batch` | `@Tag(Slow.SLOW)` — **temps journalisé, non asserté** |

---

### Tâche 15 : `12-spring-boot`

**Autonome.** Ne dépend **pas** de `00-support`. `pom.xml` important
`spring-boot-dependencies:4.1.2` ; **les versions du BOM Boot font foi** pour Hibernate,
Flyway, Logback, Caffeine, HikariCP et Testcontainers (2.0.5, artefact
`org.testcontainers:testcontainers-postgresql`). Conteneur par `@ServiceConnection`.
Compteur SQL local, même principe `StatementInspector`, enregistré via un
`HibernatePropertiesCustomizer`.

**Entités et code :** `Media`, `Copy`, `Loan`, `Member` ; `MediaRepository`,
`LoanRepository` (Spring Data) ; `LoanService` ; `MediaController` (minimal, uniquement
pour démontrer OSIV).

**Tests :**

| Classe | Méthode | Assertion clé |
|---|---|---|
| `TransactionalTest` | `readonly_evite_le_dirty_checking` | aucun `update` émis |
| `TransactionalTest` | `l_auto_invocation_ne_traverse_pas_le_proxy` | **le piège** : pas de transaction ouverte |
| `TransactionalTest` | `requires_new_ouvre_une_transaction_independante` | rollback externe sans effet sur l'interne |
| `OsivTest` | `osiv_actif_masque_la_lazyinitializationexception` | le contrôleur répond, requêtes émises au rendu |
| `OsivTest` | `osiv_desactive_revele_le_probleme_a_corriger` | exception, puis corrigé par `@EntityGraph` |
| `SpringDataTest` | `une_requete_derivee_produit_le_sql_attendu` | forme SQL assertée |
| `SpringDataTest` | `une_specification_compose_les_criteres` | clause `where` attendue |
| `SpringDataTest` | `une_projection_par_interface_reduit_les_colonnes` | `lastQuery()` sans `select m.*` |
| `DataJpaTestPiegeTest` | `le_rollback_automatique_masque_les_erreurs_de_flush` | documente le piège |
| `EntityManagerAccessibleTest` | `l_entitymanager_reste_injectable_sous_les_repositories` | même `SessionFactory` |

- [ ] **Étape finale de la tâche 15** : ajouter au `README.md` la **table de
  correspondance** — une ligne par chapitre 01→11, donnant l'équivalent Spring et ce qui se
  passe derrière.

---

## Tâche 16 : Vérification finale

- [ ] **Étape 1 : Build complet**

Run: `mvn clean verify`
Expected: `BUILD SUCCESS`, 13 modules, en quelques minutes, tests `slow` exclus.

- [ ] **Étape 2 : Build avec les tests lents**

Run: `mvn clean verify -Pslow`
Expected: `BUILD SUCCESS`, tests `@Tag("slow")` exécutés.

- [ ] **Étape 3 : Vérifier le critère d'acceptation du §11**

Chaque module a au moins un test assertant le SQL émis ; chaque notion du §6 est cochée
dans `doc/TASKS.md` ; aucune assertion ne porte sur un temps.

- [ ] **Étape 4 : Vérifier l'historique Git** *(contrainte globale)*

Run: `git log --format='%an <%ae>' | sort -u`
Expected: une seule ligne, `Mouhamadou Moustapha LO <mouhamadoulo39@gmail.com>`.

Run: `git log --format='%B' | grep -ci "co-authored-by\|claude\|anthropic\|generated with"`
Expected: `0`.

- [ ] **Étape 5 : Commit final et push**

```bash
git add README.md doc/TASKS.md
git commit -m "docs: finalisation du suivi et de la table de correspondance"
git push
```
