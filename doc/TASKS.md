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
| [ ] | Bag vs list indexée (`@OrderColumn`) | `BagVsListTest` |
| [ ] | `@OrderBy` vs `SortedSet` | `OrderByTest` |
| [ ] | `MultipleBagFetchException` | `MultipleBagFetchTest` |
| [ ] | `@ElementCollection` | `ElementCollectionTest` |
| [ ] | `@MapKeyColumn` | `MapTest` |
| [ ] | `@BatchSize` sur collection | `BatchSizeTest` |

## 05-inheritance — Héritage

| | Notion | Test |
|---|---|---|
| [ ] | `SINGLE_TABLE` et discriminant | `SingleTableTest` |
| [ ] | `SINGLE_TABLE` : colonnes forcément nullables | `SingleTableTest` |
| [ ] | `JOINED` | `JoinedTest` |
| [ ] | `TABLE_PER_CLASS` et `union all` | `TablePerClassTest` |
| [ ] | Comparatif des trois stratégies | `ComparatifTest` |
| [ ] | `@MappedSuperclass` | `MappedSuperclassTest` |
| [ ] | Requête polymorphe et `type()` | `RequetePolymorpheTest` |

## 06-queries — Interroger

| | Notion | Test |
|---|---|---|
| [ ] | HQL : jointures | `HqlTest` |
| [ ] | HQL : sous-requête corrélée | `HqlTest` |
| [ ] | Criteria et métamodèle statique | `CriteriaTest` |
| [ ] | SQL natif et `@SqlResultSetMapping` | `NativeQueryTest` |
| [ ] | `@NamedQuery` | `NamedQueryTest` |
| [ ] | Projection par record | `ProjectionTest` |
| [ ] | `Tuple` | `ProjectionTest` |
| [ ] | Pagination | `PaginationTest` |
| [ ] | Injection SQL et paramètres nommés | `InjectionSqlTest` |

## 07-fetching — Le chargement

| | Notion | Test |
|---|---|---|
| [ ] | `LAZY` / `EAGER` par défaut | `LazyEagerTest` |
| [ ] | Proxies Hibernate | `ProxyTest` |
| [ ] | `LazyInitializationException` | `LazyInitializationTest` |
| [ ] | `Hibernate.initialize()` | `LazyInitializationTest` |
| [ ] | **N+1 reproduit et chiffré** | `NPlusUnTest` |
| [ ] | `join fetch` | `NPlusUnTest` |
| [ ] | `@EntityGraph` | `NPlusUnTest` |
| [ ] | `@BatchSize` | `NPlusUnTest` |
| [ ] | Projection DTO | `NPlusUnTest` |
| [ ] | Pagination + `join fetch` (`HHH90003004`) | `PaginationFetchTest` |

## 08-transactions-concurrency — Transactions et concurrence

| | Notion | Test |
|---|---|---|
| [ ] | Niveaux d'isolation | `IsolationTest` |
| [ ] | `@Version` | `VersionTest` |
| [ ] | `OptimisticLockException` | `VersionTest` |
| [ ] | `OPTIMISTIC_FORCE_INCREMENT` | `VersionTest` |
| [ ] | `PESSIMISTIC_WRITE` et `for update` | `PessimisticTest` |
| [ ] | `NOWAIT` | `PessimisticTest` |
| [ ] | `SKIP LOCKED` | `PessimisticTest` |
| [ ] | **Emprunt concurrent à deux threads** | `EmpruntConcurrentTest` |
| [ ] | Interblocage et ordre d'acquisition | `InterblocageTest` |

## 09-caching — Les caches

| | Notion | Test |
|---|---|---|
| [ ] | Cache L1 et identité d'instance | `CacheNiveau1Test` |
| [ ] | Cache L1 non désactivable | `CacheNiveau1Test` |
| [ ] | Cache L2 via JCache + Caffeine | `CacheNiveau2Test` |
| [ ] | `Statistics` : hits et misses | `CacheNiveau2Test` |
| [ ] | `READ_ONLY` | `StrategiesTest` |
| [ ] | `READ_WRITE` | `StrategiesTest` |
| [ ] | `TRANSACTIONAL` — limite documentée | `StrategiesTest` |
| [ ] | Cache de collection | `CacheCollectionTest` |
| [ ] | Cache de requêtes | `CacheRequeteTest` |
| [ ] | Invalidation et `bulk update` | `InvalidationTest` |

## 10-advanced — Fonctionnalités avancées

| | Notion | Test |
|---|---|---|
| [ ] | Envers : révisions | `EnversTest` |
| [ ] | Envers : `AuditReader` | `EnversTest` |
| [ ] | Envers : `RevisionEntity` personnalisée | `EnversTest` |
| [ ] | Soft delete (`@SQLDelete`) | `SoftDeleteTest` |
| [ ] | `@SQLRestriction` | `SoftDeleteTest` |
| [ ] | `@Filter` paramétré | `FilterTest` |
| [ ] | Listener `PreInsert` | `InterceptorTest` |
| [ ] | `@NaturalId` et son cache | `NaturalIdTest` |
| [ ] | `@DynamicUpdate` | `DynamicUpdateTest` |
| [ ] | **Multi-tenancy par schéma** | `MultiTenancyTest` |

## 11-performance — Performance

| | Notion | Test |
|---|---|---|
| [ ] | Sans `batch_size` : un aller-retour par insertion | `BatchingTest` |
| [ ] | `hibernate.jdbc.batch_size` | `BatchingTest` |
| [ ] | `IDENTITY` désactive le batching | `BatchingTest` |
| [ ] | `order_inserts` | `BatchingTest` |
| [ ] | `StatelessSession` | `StatelessSessionTest` |
| [ ] | `update`/`delete` en masse HQL | `BulkTest` |
| [ ] | Contexte périmé après un bulk | `BulkTest` |
| [ ] | `stream` sur gros volume | `StreamTest` |
| [ ] | Flyway : migrations versionnées | `FlywayTest` |
| [ ] | `hbm2ddl.auto=validate` | `FlywayTest` |
| [ ] | 100 000 insertions (`@Tag("slow")`) | `GrosVolumeTest` |

## 12-spring-boot — Ce que Spring ajoute et cache

| | Notion | Test |
|---|---|---|
| [ ] | `@Transactional(readOnly)` | `TransactionalTest` |
| [ ] | Auto-invocation et proxy (piège) | `TransactionalTest` |
| [ ] | `REQUIRES_NEW` | `TransactionalTest` |
| [ ] | **OSIV actif** | `OsivTest` |
| [ ] | **OSIV désactivé** | `OsivTest` |
| [ ] | Requête dérivée Spring Data | `SpringDataTest` |
| [ ] | `Specification` | `SpringDataTest` |
| [ ] | Projection par interface | `SpringDataTest` |
| [ ] | `@DataJpaTest` et rollback automatique (piège) | `DataJpaTestPiegeTest` |
| [ ] | `EntityManager` sous les repositories | `EntityManagerAccessibleTest` |
| [ ] | Table de correspondance des 11 chapitres | `README.md` |

---

## Vérification finale

- [ ] `mvn clean verify` passe
- [ ] `mvn clean verify -Pslow` passe
- [ ] Chaque module a au moins un test assertant le SQL émis
- [ ] Aucune assertion ne porte sur un temps d'exécution
- [ ] `git log --format='%an <%ae>' | sort -u` rend exactement une ligne
