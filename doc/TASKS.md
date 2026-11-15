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
| [x] | `SINGLE_TABLE` : colonnes forcément nullables | `StrategiesTest` |
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
