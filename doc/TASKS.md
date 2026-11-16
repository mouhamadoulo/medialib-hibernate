# Avancement — MediaLib

Une case par **notion structurante**. Cochée quand la notion est implémentée *et* couverte par
un test qui assertionne le SQL émis.

Référence : [document de conception §6](../docs/superpowers/specs/2026-11-15-medialib-hibernate-design.md)
· [plan d'implémentation](../docs/superpowers/plans/2026-11-15-medialib-hibernate.md)

**Légende** — `[x]` fait · `[ ]` à faire

---

## Socle

- [x] **Tâche 1** — pom parent, versions figées, profil `slow`, dépôt Git public
- [x] **Tâche 2** — `00-support` : `SqlStatementCounter`, `MediaLibPostgres`, `HibernateTestBase`, `Slow`
- [x] **Tâche 3** — `README.md`, `CLAUDE.md`, `doc/TASKS.md`

---

## 01-bootstrap — Démarrer et piloter Hibernate

| | Notion | Test |
|---|---|---|
| [x] | `SessionFactory` par `StandardServiceRegistryBuilder` | `BootstrapTest` |
| [x] | Bootstrap par `persistence.xml` / `EntityManagerFactory` | `BootstrapTest` |
| [x] | `Session` vs `EntityManager` | `BootstrapTest` |
| [x] | État *transient* | `CycleDeVieTest` |
| [x] | `persist` et différé jusqu'au flush | `CycleDeVieTest` |
| [x] | `find` et contexte de persistance | `CycleDeVieTest` |
| [x] | `getReference` et proxy | `CycleDeVieTest` |
| [x] | `remove` | `RemoveTest` |
| [x] | Dirty checking | `DirtyCheckingTest` |
| [x] | État *detached* | `DirtyCheckingTest` |
| [x] | `merge` | `DirtyCheckingTest` |
| [x] | `flush()` et `FlushMode` | `FlushModeTest` |

## 02-mapping-basics — Mapper une entité

| | Notion | Test |
|---|---|---|
| [x] | `IDENTITY` : INSERT immediat, aucun report possible | `IdentifiantsTest` |
| [x] | `SEQUENCE` : INSERT differes au flush | `IdentifiantsTest` |
| [x] | `UUID` | `IdentifiantsTest` |
| [x] | `@Embeddable` / `@Embedded` | `TypesValeurTest` |
| [x] | `@EmbeddedId` (clé composite) | `TypesValeurTest` |
| [x] | `AttributeConverter` (`Money`) | `TypesValeurTest` |
| [x] | `UserType` (`Isbn`) | `TypesValeurTest` |
| [x] | `@Enumerated` et le piège `ORDINAL` | `EnumEtFormulaTest` |
| [x] | `@Formula` | `EnumEtFormulaTest` |
| [x] | `@Transient` | `EnumEtFormulaTest` |
| [x] | Strategie de nommage : le defaut d'Hibernate n'est PAS snake_case | `NommageTest` |
| [x] | `CamelCaseToUnderscoresNamingStrategy` | `NommageSnakeCaseTest` |

## 03-associations — Relier les entités

| | Notion | Test |
|---|---|---|
| [x] | `@ManyToOne` | `CardinalitesTest` |
| [x] | `@OneToMany` unidirectionnel | `CardinalitesTest` |
| [x] | `@OneToMany` bidirectionnel et `mappedBy` | `CardinalitesTest` |
| [x] | Synchronisation bidirectionnelle (piège) | `CardinalitesTest` |
| [x] | `@OneToOne` et `@MapsId` | `CardinalitesTest` |
| [x] | `@ManyToMany` | `CardinalitesTest` |
| [x] | Entité d'association | `CardinalitesTest` |
| [x] | `cascade` | `CascadeTest` |
| [x] | `orphanRemoval` | `CascadeTest` |
| [x] | `equals`/`hashCode` et le piège du `Set` | `EqualsHashCodeTest` |
| [x] | Sans `orphanRemoval`, l'enfant survit orphelin | `CascadeTest` |
| [x] | Cle metier stable comme remede | `EqualsHashCodeTest` |
| [x] | Supprimer une entite vide sa table de jonction `@ManyToMany` | `CascadeTest` |

## 04-collections — Collections et éléments de valeur

| | Notion | Test |
|---|---|---|
| [x] | Bag vs list indexée (`@OrderColumn`) | `BagVsListTest` |
| [x] | `@OrderBy` vs `SortedSet` | `TriEtMapTest` |
| [x] | `MultipleBagFetchException` | `MultipleBagFetchTest` |
| [x] | `@ElementCollection` | `TriEtMapTest` |
| [x] | `@MapKeyColumn` | `TriEtMapTest` |
| [x] | `@BatchSize` sur collection | `BatchSizeTest` |
| [x] | L'ordre preserve par la colonne de position | `BagVsListTest` |
| [x] | Hibernate 6+ dedoublonne le `join fetch` : `distinct` devenu inutile | `MultipleBagFetchTest` |
| [x] | `@BatchSize` sur PostgreSQL : `= any (?)` et non `in (?, ?, ...)` | `BatchSizeTest` |

## 05-inheritance — Héritage

| | Notion | Test |
|---|---|---|
| [x] | `SINGLE_TABLE` et discriminant | `StrategiesTest` |
| [x] | `SINGLE_TABLE` : colonnes de sous-classe forcément nullables | `StrategiesTest` |
| [x] | Exiger `nullable = false` quand même : Hibernate crée la contrainte | `NotNullEnSingleTableTest` |
| [x] | Et c'est une **autre** sous-classe qui devient insérable impossible | `NotNullEnSingleTableTest` |
| [x] | `@Column(nullable = false)` : Hibernate refuse dès le flush, avant la base | `StrategiesTest` |
| [x] | `JOINED` | `StrategiesTest` |
| [x] | `TABLE_PER_CLASS` et `union all` | `ComparatifTest` |
| [x] | Comparatif des trois stratégies | `ComparatifTest` |
| [x] | `@MappedSuperclass` | `ComparatifTest` |
| [x] | Requête polymorphe et `type()` | `ComparatifTest` |
| [x] | `JOINED` autorise le NOT NULL que `SINGLE_TABLE` interdit | `StrategiesTest` |
| [x] | `TABLE_PER_CLASS` ne cree aucune table pour la racine abstraite | `ComparatifTest` |
| [x] | `@MappedSuperclass` recopie ses colonnes | `ComparatifTest` |

## 06-queries — Interroger

| | Notion | Test |
|---|---|---|
| [x] | HQL : jointures | `HqlTest` |
| [x] | HQL : sous-requête corrélée | `HqlTest` |
| [x] | Criteria et métamodèle statique | `CriteriaTest` |
| [x] | SQL natif et `@SqlResultSetMapping` | `ProjectionsEtNativeTest` |
| [x] | `@NamedQuery` | `ProjectionsEtNativeTest` |
| [x] | Projection par record | `ProjectionsEtNativeTest` |
| [x] | `Tuple` | `ProjectionsEtNativeTest` |
| [x] | Pagination | `PaginationEtInjectionTest` |
| [x] | Injection SQL et paramètres nommés | `PaginationEtInjectionTest` |
| [x] | Agregat `group by` / `having` | `HqlTest` |
| [x] | `left join` et `case when` | `HqlTest` |
| [x] | Criteria compose des criteres optionnels | `CriteriaTest` |
| [x] | Une projection n'est pas managee | `ProjectionsEtNativeTest` |
| [x] | Pagination sans `order by` : resultat non deterministe | `PaginationEtInjectionTest` |

## 07-fetching — Le chargement

| | Notion | Test |
|---|---|---|
| [x] | `LAZY` / `EAGER` par défaut | `LazyEagerTest` |
| [x] | Proxies Hibernate | `LazyEagerTest` |
| [x] | `LazyInitializationException` | `LazyEagerTest` |
| [x] | `Hibernate.initialize()` | `LazyEagerTest` |
| [x] | **N+1 reproduit et chiffré** | `NPlusUnTest` |
| [x] | `join fetch` | `NPlusUnTest` |
| [x] | `@EntityGraph` | `NPlusUnTest` |
| [x] | `@BatchSize` | `NPlusUnTest` |
| [x] | Projection DTO | `NPlusUnTest` |
| [x] | Pagination + `join fetch` (`HHH90003004`) | `PaginationFetchTest` |
| [x] | **EAGER ne veut pas dire joint** : en HQL, requete separee | `LazyEagerTest` |
| [x] | Une projection ne peut pas lever `LazyInitializationException` | `LazyEagerTest` |
| [x] | Paginer les identifiants puis charger : la bonne solution | `PaginationFetchTest` |

## 08-transactions-concurrency — Transactions et concurrence

| | Notion | Test |
|---|---|---|
| [x] | Niveaux d'isolation | `IsolationTest` |
| [x] | `@Version` | `VersionTest` |
| [x] | `OptimisticLockException` | `VersionTest` |
| [x] | `OPTIMISTIC_FORCE_INCREMENT` | `VersionTest` |
| [x] | `PESSIMISTIC_WRITE` et `for update` | `PessimisticTest` |
| [x] | `NOWAIT` | `PessimisticTest` |
| [x] | `SKIP LOCKED` | `PessimisticTest` |
| [x] | **Emprunt concurrent à deux threads** | `EmpruntConcurrentTest` |
| [x] | Interblocage et ordre d'acquisition | `InterblocageTest` |
| [x] | `PESSIMISTIC_WRITE` sur PostgreSQL : `for no key update` | `PessimisticTest` |
| [x] | Le cache L1 masque la lecture non repetable | `IsolationTest` |
| [x] | Aucune lecture sale possible | `IsolationTest` |
| [x] | Variante pessimiste de l'emprunt concurrent | `EmpruntConcurrentTest` |

## 09-caching — Les caches

| | Notion | Test |
|---|---|---|
| [x] | Cache L1 et identité d'instance | `CacheNiveau1Test` |
| [x] | Cache L1 non désactivable | `CacheNiveau1Test` |
| [x] | Cache L2 via JCache + Caffeine | `CacheNiveau2Test` |
| [x] | `Statistics` : hits et misses | `CacheNiveau2Test` |
| [x] | `READ_ONLY` | `StrategiesEtInvalidationTest` |
| [x] | `READ_WRITE` met le cache a jour au commit | `CacheNiveau2Test` |
| [x] | `TRANSACTIONAL` — limite documentée | `StrategiesEtInvalidationTest` |
| [x] | Cache de collection | `StrategiesEtInvalidationTest` |
| [x] | Cache de requêtes | `CacheRequeteTest` |
| [x] | Invalidation et `bulk update` | `StrategiesEtInvalidationTest` |
| [x] | `session.clear()` vide le cache L1 | `CacheNiveau1Test` |
| [x] | L'INSERT peuple deja le cache L2 | `CacheNiveau2Test` |
| [x] | Un UPDATE natif invalide TOUTES les regions | `StrategiesEtInvalidationTest` |
| [x] | Une requete non cachee est rejouee | `CacheRequeteTest` |

## 10-advanced — Fonctionnalités avancées

| | Notion | Test |
|---|---|---|
| [x] | Envers : révisions | `EnversTest` |
| [x] | Envers : `AuditReader` | `EnversTest` |
| [x] | Envers : `RevisionEntity` personnalisée | `EnversTest` |
| [x] | Soft delete (`@SQLDelete`) | `SoftDeleteEtFiltresTest` |
| [x] | `@SQLRestriction` | `SoftDeleteEtFiltresTest` |
| [x] | `@Filter` paramétré | `SoftDeleteEtFiltresTest` |
| [x] | Listener `PreInsert` | `ListenerEtOptimisationsTest` |
| [x] | `@NaturalId` et son cache | `ListenerEtOptimisationsTest` |
| [x] | `@DynamicUpdate` | `ListenerEtOptimisationsTest` |
| [x] | **Multi-tenancy par schéma** | `MultiTenancyTest` |
| [x] | `RevisionEntity` ecrite entierement (`DefaultRevisionEntity` est `final`) | `EnversTest` |
| [x] | Seules les entites `@Audited` ont une table `_aud` | `EnversTest` |
| [x] | Le `@Filter` est porte par la session | `SoftDeleteEtFiltresTest` |
| [x] | Le meme identifiant coexiste dans deux agences | `MultiTenancyTest` |

## 11-performance — Performance

| | Notion | Test |
|---|---|---|
| [x] | Sans `batch_size` : un aller-retour par insertion | `SansBatchTest` |
| [x] | `hibernate.jdbc.batch_size` | `BatchingTest` |
| [x] | `IDENTITY` désactive le batching | `BatchingTest` |
| [x] | `order_inserts` | `BatchingTest` |
| [x] | `StatelessSession` | `StatelessEtBulkTest` |
| [x] | `update`/`delete` en masse HQL | `StatelessEtBulkTest` |
| [x] | Contexte périmé après un bulk | `StatelessEtBulkTest` |
| [x] | `stream` sur gros volume | `StatelessEtBulkTest` |
| [x] | Flyway : migrations versionnées | `FlywayTest` |
| [x] | `hbm2ddl.auto=validate` | `FlywayTest` |
| [x] | 100 000 insertions (`@Tag("slow")`) | `GrosVolumeTest` |
| [x] | `allocationSize` economise les appels a la sequence | `BatchingTest` |
| [x] | Le batching des UPDATE aussi | `BatchingTest` |
| [x] | **`StatelessSession` n'emet AUCUN lot** (facteur 17) | `GrosVolumeTest` |
| [x] | `StatelessSession` en lecture streamee : son vrai usage | `GrosVolumeTest` |
| [x] | Flyway journalise aussi la creation du schema | `FlywayTest` |
| [x] | Index cree par migration, absent du mapping | `FlywayTest` |
| [x] | Rejouer Flyway est sans effet | `FlywayTest` |
| [x] | `datasource-proxy` au lieu du `StatementInspector` : pourquoi | `CompteurDExecutions` |

## 12-spring-boot — Ce que Spring ajoute et cache

| | Notion | Test |
|---|---|---|
| [x] | `@Transactional(readOnly)` | `TransactionalTest` |
| [x] | Auto-invocation et proxy (piège) | `TransactionalTest` |
| [x] | `REQUIRES_NEW` | `TransactionalTest` |
| [x] | **OSIV actif** | `OsivActifTest` |
| [x] | **OSIV désactivé** | `OsivDesactiveTest` |
| [x] | Requête dérivée Spring Data | `SpringDataTest` |
| [x] | `Specification` | `SpringDataTest` |
| [x] | Projection par interface | `SpringDataTest` |
| [x] | `@DataJpaTest` et rollback automatique (piège) | `DataJpaTestPiegeTest` |
| [x] | `EntityManager` sous les repositories, même `SessionFactory` | `SpringDataTest` |
| [x] | Une requete native force un auto-flush de la session | `DataJpaTestPiegeTest` |
| [x] | Spring Boot 4 a repackage ses classes d'auto-configuration | `ConfigurationHibernate` |
| [x] | Le compteur SQL dupliqué ici est testé, pour qu'il ne dérive pas | `CompteurSqlTest` |
| [x] | Table de correspondance des 11 chapitres | `README.md` |

---

## Vérification finale

- [x] `mvn clean verify` passe — 13 modules, **203 tests**
- [x] `mvn clean verify -Pslow` passe — **206 tests** (les 3 tests à grosse volumétrie)
- [x] Chaque module a au moins un test assertant le SQL émis
- [x] Aucune assertion ne porte sur un temps d'exécution
- [x] `git log --format='%an <%ae>' | sort -u` rend exactement une ligne

### Mesures relevées (indicatives, jamais assertées)

| Opération | Débit |
|---|---|
| 100 000 insertions, `Session` + batch 500 + flush/clear | 6 159 ms (~16 000 lignes/s) |
| 10 000 insertions, `StatelessSession.insert()` un par un | 14 820 ms (~675 lignes/s) |
| 10 000 lectures, `StatelessSession` en streaming | 279 ms |

L'écart de plus d'un ordre de grandeur entre les deux premières lignes est la leçon du
chapitre 11 : `StatelessSession` sert à **lire** de gros volumes, pas à en **écrire**.
