> Research notes collected 2026-10-07 for the upgrade plans. "probe" / "scratchpad/..." references are throwaway programs and jars used during the research; they are not part of this repo.

# Hibernate ORM 6.6.58 → 7.3.13 → 7.4.12 (and 5.6.15 → 7.3.13) migration notes

How these were checked:
- **guide**: the official migration guide (`migration-guide.adoc` on branches 7.0–7.4, plus 6.0–6.6 for the 5.6 jump).
- **javap**: run against `hibernate-core-{6.6.58,7.3.13,7.4.12}.Final.jar` and `hibernate-core-jakarta-5.6.15.Final.jar`. This includes reading the `@Deprecated(since, forRemoval)` attributes.
- **runtime**: a probe program compiled and run against each version with HSQLDB 2.7.4. Probe sources are in `scratchpad/rt/src/demo/Probe.java` and `scratchpad/rt/src5/demo/P5.java`.

Anything not checked by one of these is marked UNVERIFIED.

## (a) Requirements per series (hibernate.org/orm/releases/7.x)

| Series | Java | JPA | Status (as of 2026-10) | Notes |
|---|---|---|---|---|
| 6.6 | 11+ | 3.1 | limited-support | |
| 7.0 | 17, 21, 23 | **3.2** | EOL | Apache License 2.0. Hibernate Models replaces HCANN (hibernate-commons-annotations). |
| 7.1 | 17, 21, 25 | 3.2 | EOL | |
| 7.2 | 17, 21, 25 | 3.2 | limited-support | Building Hibernate itself needs Java 25 and Gradle 9. The published artifacts still run on Java 17. |
| 7.3 | 17, 21, 25, 26 | 3.2 | **EOL** | Classmate dependency dropped. |
| 7.4 | 17, 21, 25, 26 | 3.2 | latest stable | Jakarta EE 11, Spring Boot 4.1. |

Runtime dependencies, taken from the POMs:
- **7.3.13**: jakarta.persistence-api 3.2.0, jboss-logging 3.6.1, hibernate-models 1.1.1, byte-buddy 1.18.8, antlr4 4.13.2, jaxb 4.0.6. **No** classmate, jandex or hibernate-commons-annotations.
- **7.4.12**: the same, except hibernate-models 1.1.2 and jaxb 4.0.7.
- **6.6.58**: JPA 3.1.0, jboss-logging 3.5.0, hibernate-commons-annotations 7.0.3, jandex 3.2.0, classmate 1.5.1, byte-buddy 1.17.8.

Logging still goes through **jboss-logging** in 7.x. `org.jboss.logging.provider=slf4j` is still honoured (the provider string was verified in jboss-logging 3.6.1's `LoggerProviders`).

## (b) API status table

Legend: OK = present and unchanged; DEP = deprecated; DEP-R = deprecated forRemoval; GONE = removed. Source column: g = guide, j = javap, r = runtime probe.

| API (demo usage) | 6.6.58 | 7.3.13 | 7.4.12 | Replacement / note | Src |
|---|---|---|---|---|---|
| `new Configuration()`, `setProperty(String,String)`, `getProperty`, `getProperties`, `addAnnotatedClass` | OK | OK, **not deprecated** | OK | `addAnnotatedClass(Class<?>)` is now generic (source-compatible). The javadoc only says `HibernatePersistenceConfiguration` is "a new alternative" (apiNote). | j |
| `Configuration.buildSessionFactory(ServiceRegistry)` | OK | OK, not deprecated | OK | — | j,r |
| `StandardServiceRegistryBuilder.applySettings(Properties/Map)`, `build()`, static `destroy(ServiceRegistry)` | OK | OK | OK | `clearSettings()` removed (not used by the demo). | j,r |
| `StandardServiceRegistry.getService(ConnectionProvider/JdbcEnvironment/JdbcServices)` | OK | OK | OK | In 7.x the default ConnectionProvider class is `DriverManagerConnectionProvider` (was `...ProviderImpl`; both classes still exist). Matters only if code checks the class name. | j,r |
| `StandardServiceRegistryImpl.isActive()/destroy()` | OK | OK | OK | — | j,r |
| `Metadata.getEntityBindings / getDatabase().getDefaultNamespace() / getSessionFactoryBuilder().build()` | OK | OK | OK | `Metadata` now extends `org.hibernate.type.MappingContext` (was `engine.spi.Mapping`). `Namespace.Name` is now a record, so `toString` changed from `Name{..}` to `Name[..]`. | j,r |
| `MetadataImpl.getBootstrapContext()/getTypeConfiguration()` | OK | OK | OK | — | j,r |
| `BootstrapContext.getServiceRegistry()`, `getJpaCompliance().isJpaQueryComplianceEnabled()` | OK | OK | OK | Removed: `getReflectionManager`, `getJandexView`, `getClassmateContext`, `getAttributeConverters`. | j,r |
| `SessionFactory.getMetamodel().getEntities()` | OK (JPA `Metamodel`) | OK | OK | `org.hibernate.Metamodel` and `MetamodelImplementor` were removed. On the `SessionFactory` type, `getMetamodel()` is the JPA one, so the demo usage is unchanged. On `SessionFactoryImpl` the covariant return is now `MappingMetamodel`. | g,j,r |
| `SessionFactory.getStatistics()` | OK | OK | OK | `SessionFactory`, `Session` and `SharedSessionContract` **no longer extend `java.io.Closeable`**; they are only `AutoCloseable`. | j |
| `Identifier.toIdentifier`, `QualifiedNameImpl(Identifier,Identifier,Identifier)` | OK | OK | OK | — | j,r |
| `PersistentClass.getEntityName/getClassName/getIdentifierProperty/getTable/getVersion/getProperties` | OK | OK | OK | — | j,r |
| `new mapping.Table(String)` | param = **contributor** | same | same | **It does not set the name**: `new Table("FOO").getName()` returns null (r). Use `new Table(contributor, name)`, or `setName`. The parameter has meant "contributor" since 6.x; in 5.6 it was the name. | j(LVT),r |
| `Table.getColumns()/getPrimaryKey()/getUniqueKeys()/setSchema()/getQualifiedTableName()` | OK | OK | OK | — | j,r |
| `Table.getForeignKeys()` (Map) | OK | **DEP-R since 7** | DEP-R | `getForeignKeyCollection()` (new in 7) | j |
| `Table.getUniqueKeyIterator()/getForeignKeyIterator()/getCheckConstraintsIterator()` | DEP-R (since 6.0) | **GONE** | GONE | `getUniqueKeys().values()`, `getForeignKeyCollection()`, `getCheckConstraints()` | j |
| `Column.getSqlType()` / `getSqlTypeCode()` (no-arg) | OK | OK | OK | Both return null unless set explicitly (r). | j,r |
| `Column.getSqlType(Metadata)` | OK | OK | OK | — | j,r |
| `Column.getSqlTypeCode(Mapping)` | OK | **signature is now `getSqlTypeCode(MappingContext)`** | same | Passing a `Metadata` still compiles, because `Metadata` extends `MappingContext`. Binary-incompatible, so recompile. `getSqlType(TypeConfiguration,Dialect,Mapping)` is GONE. | j,r |
| `Column.getLength()` Long, `getPrecision()/getScale()` Integer | OK | OK | OK | — | j |
| `SimpleValue.getTypeName()` | OK | OK | OK | Returns null for `@Id Long` (r). | j,r |
| `SimpleValue.getIdentifierGeneratorStrategy()/getIdentifierGeneratorProperties()/getIdentifierGeneratorParameters()` | OK (Properties variant DEP) | **GONE** | GONE | There is no direct boot-model equivalent. Options: `SimpleValue.getCustomIdGeneratorCreator()` (`GeneratorCreator`); `KeyValue.createGenerator(Dialect, RootClass, Property, GeneratorSettings)`; or, after the SessionFactory is built, `sfi.getMappingMetamodel().getEntityDescriptor(name).getGenerator()` and inspect its class (e.g. `SequenceStyleGenerator`). | j |
| `Constraint.getColumns()`, `ForeignKey.getReferencedColumns()/getReferencedTable()` | OK | OK | OK | `getReferencedColumns()` is empty when the FK references the PK (r, same in 6.6). | j,r |
| `StandardBasicTypes.STRING/TEXT/INTEGER/BIG_DECIMAL.getName()` | `string,text,integer,big_decimal` | same | same | 5.6 gives the same names. | r |
| `@org.hibernate.annotations.Cascade` / `CascadeType` | OK (`SAVE_UPDATE`, `DELETE` DEP) | **Both DEP-R since 7**. `SAVE_UPDATE` and `DELETE` are **GONE**. `REMOVE`, `PERSIST`, `MERGE`, `LOCK` and `DELETE_ORPHAN` remain. | same | Use JPA `cascade={CascadeType.PERSIST, CascadeType.MERGE}` for SAVE_UPDATE and `REMOVE` for DELETE. `LOCK` has no JPA equivalent. | g,j |
| `Dialect.quote/openQuote/closeQuote/getLimitHandler/getIdentityColumnSupport/getSequenceSupport()` | OK | OK | OK | `supportsSequences` and `getSequenceNextValString` live on `getSequenceSupport()` (since 6.0). | j,r |
| No-arg dialect constructors | OK | OK | OK | The version defaults to the dialect's minimum (r): DB2 11.1 (was 10.5), PostgreSQL 13 in 7.3 / **14 in 7.4** (was 12), SQL Server 12.0 (was 11), HANA 2.0.50 (was 1.0.120), HSQL 2.6.1, MySQL 8.0, Oracle 19. A DB below the minimum only logs a warning (`unsupportedDatabaseVersion`). | j,r |
| `DB2Dialect, HSQLDialect, MySQLDialect, OracleDialect, PostgreSQLDialect` | OK | OK | OK | — | j |
| `HANACloudColumnStoreDialect` | DEP | **GONE** | GONE | `org.hibernate.dialect.HANADialect`. **Not** in community-dialects 7.x, which only has `HANALegacyDialect`. | j |
| `SQLServer2012Dialect` | DEP | **GONE** | GONE | `SQLServerDialect`. **Not** in community-dialects 7.x, which only has `SQLServerLegacyDialect`. | j |
| `Session.save/saveOrUpdate/update/delete/load` | DEP | **GONE** | GONE | `persist`; `merge` (detached); `remove`; `getReference` | g,j |
| `Session.get(Class,id)` (all overloads) | OK | **DEP-R since 7.0** | DEP-R | `find(Class,id[, FindOption...])` | g,j |
| `createQuery(String hql, Class)` | OK | OK | OK | — | j,r |
| `createQuery(String)` untyped | DEP since 6.0 | still DEP, **not removed** | same | Still works for a single root and for update/delete (r). **Rejected** with no select and more than one root: `IllegalArgumentException(SemanticException)` (g,r). Use `createSelectionQuery` / `createQuery(hql, X.class)` / `createMutationQuery`. | g,j,r |
| `createMutationQuery(String)` | OK | OK | OK | — | j,r |
| `createNativeQuery(String)` untyped | DEP since 6.0 | DEP | DEP | `createNativeQuery(sql, Class)` | j |
| `Query.list/uniqueResult/setFirstResult/setMaxResults/executeUpdate/scroll/getResultList` | OK | OK, **`list` not deprecated** | OK | — | j,r |
| `ScrollableResults<R>.get/next/close` | OK; is `Closeable` | OK; **only `AutoCloseable`** | same | — | j,r |
| `ScrollableResults.getRowNumber()/setRowNumber()` | OK | **DEP-R since 7** | DEP-R | `getPosition()` is **1-based** (0 = before first). `getRowNumber()` is `getPosition()-1`. `position(int)` replaces `setRowNumber`. | j,src,r |
| `AbstractScrollableResults`, `ScrollableResultsImpl` | `org.hibernate.internal` | **`org.hibernate.internal.scrollable`** | same (+`WindowedScrollableResultsImpl`) | — | j,r |
| `SessionImpl.connection()` | **absent already in 6.6** | absent | absent | `doWork` / `doReturningWork`, or `session.unwrap(SessionImplementor.class).getJdbcCoordinator().getLogicalConnection().getPhysicalConnection()`. `connection()` existed only in 5.6. | j |
| `doWork/doReturningWork`, `isConnected` | OK (default methods) | OK (abstract) | OK | — | j,r |
| `Transaction.getStatus/commit/rollback` | OK | OK | OK | `getTimeout()` now returns `Integer` (was `int`), so watch for NPE on unboxing. | g,j,r |
| `Hibernate.isInitialized` | OK | OK | OK | — | j,r |
| `HibernateException extends PersistenceException`, `JDBCException`, `SQLExceptionConverter.convert`, `JdbcServices.getSqlExceptionHelper()` | OK | OK | OK | — | j |
| Native `Query.list()` on a missing table | throws `SQLGrammarException` directly | same | same | Not wrapped (r). Message changed "could not prepare statement" → "**C**ould not prepare statement". | r |
| Settings `hibernate.dialect`, `connection.*`, `show_sql`, `globally_quoted_identifiers`, `hbm2ddl.auto`, `connection.autocommit`, `generate_statistics` | OK | OK | OK | — | j |
| `hibernate.temp.use_jdbc_metadata_defaults` | deprecated alias | still honoured, with a **deprecation warning** | same | `hibernate.boot.allow_jdbc_metadata_access` (since 6.5) | g(6.5),j(bytecode of `JdbcEnvironmentInitiator`) |
| HQL: `from Employee where name=:name`; `... order by e.salary desc`; `select e.name, e.salary ...` (Object[]); `select distinct d ... left join fetch d.employees`; `update Employee e set e.salary = e.salary + 100`; `select count(e) ...` (Long) | OK | OK | OK | All six work unchanged (r). | r |

### Behaviour changes that can hit the demo
- **Native query temporal types (7.0)**: native date/time columns now come back as `java.time.LocalDate`/`LocalTime`/`LocalDateTime` instead of `java.sql.*` (r: `LocalDate` in 7.x vs `java.sql.Date` in 6.6/5.6). Set `hibernate.query.native.prefer_jdbc_datetime_types=true` to get the old behaviour.
- **Persist cascade (7.0)**: with `SAVE_UPDATE` gone, persisting or flushing an entity whose `PERSIST`/`ALL` cascade reaches a *detached* instance throws `EntityExistsException`. `merge()` that instance first.
- **Refresh/lock of detached entities (7.0)** throws `IllegalArgumentException`. `hibernate.allow_refresh_detached_entity` was removed.
- **Bulk update/delete on `@Immutable` entities (7.0)** throws by default. To allow it, set `hibernate.query.immutable_entity_update_query_handling_mode=allow`.
- **Stricter annotation validation (7.0)**: misplaced annotations, mixed access, `SEQUENCE` generation used without `@SequenceGenerator`, converters placed on `@Id`/`@Version`, etc.
- **DDL changes**:
  - 7.0: `char`/`Character` maps to `varchar(1)`; Oracle timestamp precision 9; SQL Server precision 7.
  - 7.3: `@Version` columns are created `not null`.
- **7.3**:
  - `getSingleResult()`/`getSingleResultOrNull()` always throw `NonUniqueResult` on more than one row. Use `ResultListTransformer.uniqueResultTransformer()` to dedupe.
  - Query or lock timeouts that roll back the transaction now throw `PersistenceException`.
  - Schema actions and `import.sql` run even when there are no entities.
  - Collections of read-only entities are read-only.
  - The probe's fetch-join `getSingleResult` still succeeded, because entity fetch-join results are deduped.

## (c) Specifics of jumping directly from 5.6.15 (hibernate-core-jakarta) to 7.3

These are on top of everything in (b). All are verified by javap/runtime against 5.6.15 unless marked.
- **Artifact and baseline**:
  - The artifact moves from `org.hibernate:hibernate-core-jakarta:5.6.15.Final` to `org.hibernate.orm:hibernate-core:7.3.13.Final`.
  - Java 8 → **17**.
  - JPA 3.0 → **3.2**.
  - jboss-logging 3.4.3 → 3.6.1.
  - HCANN, jandex and classmate are gone. Add `hibernate-scan-jandex` only if relying on classpath scanning; `addAnnotatedClass` doesn't need it.
- **Exceptions (important)**: in 5.6, native `list()`/`getResultList()` on a missing table threw `jakarta.persistence.PersistenceException` **wrapping** `SQLGrammarException` (r). In 6+/7 it throws `SQLGrammarException` directly. `SQLGrammarException` is a `PersistenceException` subclass, so `catch (PersistenceException)` still works. Code that unwraps `getCause()` breaks.
- **`new Table(String)`**: in 5.6 this sets the *name* (r: `"FOO"`); in 6+/7 it sets the *contributor* (name null).
- **Removed 5.6 iterator APIs**: `Table.getColumnIterator` → `getColumns()`. `getUniqueKeyIterator` → `getUniqueKeys()`, which was *package-private* in 5.6 and is public now. `getForeignKeyIterator` → `getForeignKeyCollection()`. `PersistentClass.getPropertyIterator` → `getProperties()`. `Constraint.getColumnIterator` → `getColumns()`.
- **Column**:
  - `getLength()`: `int` → `Long`.
  - `getPrecision()/getScale()`: `int` → `Integer`. They can be null, so unboxing risks an NPE.
  - `getSqlType(Dialect, Mapping)` → `getSqlType(Metadata)`.
  - `getSqlTypeCode(Mapping)` → `getSqlTypeCode(MappingContext)`.
- **Dialect**:
  - `supportsSequences()`/`getSequenceNextValString()` → `getSequenceSupport().*`.
  - Versioned dialects (`MySQL8Dialect`, `PostgreSQL10Dialect`, `Oracle12cDialect`, `SQLServer2012Dialect`, `HANACloudColumnStoreDialect`, `HANAColumnStoreDialect`) are all gone from core 7. Use the unversioned ones.
- **Session/Query**:
  - `createSQLQuery` → `createNativeQuery`.
  - `Query#iterate` was removed (6.0).
  - `SessionImpl.connection()` was removed (6.0).
  - `ScrollableResults` became generic. `get()` now returns `R` instead of `Object[]`, and `get(int)` is gone.
  - HQL ordinal parameters are 1-based (`?1`).
  - `from A join b` with no select now returns `List<A>`, not `Object[]`.
  - `distinct` is always passed to SQL, and entity dedup is automatic.
- **IDs (6.0)**: `@GeneratedValue` AUTO now uses a sequence per entity (`<entity>_seq`, allocationSize 50) instead of `hibernate_sequence`. For the old behaviour, set `hibernate.id.db_structure_naming_strategy=legacy`. This matters if the DB or `import.sql` already exists.
- **Types**:
  - 6.x changed the type system (`BasicTypeReference`, by-position reads).
  - `StandardBasicTypes` fields changed from `StringType`/`IntegerType` instances to `BasicTypeReference`. `getName()` gives the same strings.
  - 6.2 brought DDL changes: UUID/enum/JSON types, boolean check constraints, and dropped support for old DB versions.
- **Metamodel**: in 5.6 `sf.getMetamodel()` returned `org.hibernate.Metamodel` (still `getEntities()`). That interface is removed in 7.
- **Settings**: `hibernate.temp.use_jdbc_metadata_defaults` → `hibernate.boot.allow_jdbc_metadata_access`. The `hibernate.ejb.*` names were removed in 6.0.
- `Session.save/update/saveOrUpdate/delete` were *not* deprecated in 5.6, so there is no warning trail. Code goes straight from compiling cleanly to compile errors.

## (d) 7.3 → 7.4 deltas

- **API**: javap diffs of every class the demo uses (Configuration, registry, Metadata/MetadataImpl/BootstrapContext, SessionFactory, PersistentClass, Table, Column, SimpleValue, Constraint, ForeignKey, Dialect, HSQLDialect, Session, SharedSessionContract, Query/SelectionQuery/MutationQuery/NativeQuery, ScrollableResults, Transaction, Hibernate, exceptions) are **purely additive**. Examples of additions: `Table.get/setExtraDeclarations`, `PersistentClass` auxiliary tables, `Dialect.getTemporalTableSupport`. Nothing used by the demo is removed or newly deprecated.
- **Dialect minimum**: `PostgreSQLDialect` MINIMUM_VERSION goes from 13 to **14**. This changes the no-arg dialect's assumed version, and PG 13 now triggers the unsupported-version warning. HSQL stays at 2.6.1, so **HSQLDB 2.7.4 is fine for both 7.3 and 7.4**.
- **Behaviour (guide)**:
  - **Limit with collection fetch is now applied in SQL**, not in memory. This affects `setMaxResults` combined with `left join fetch d.employees`. The old behaviour is available through the query hint `org.hibernate.limitInMemory`.
  - Oracle `current date` → `trunc(current_date)`.
  - The MySQL dialect no longer defaults `max_fetch_depth=2`.
  - Eager `@Any` is now join-fetched.
  - `SpannerPostgreSQLDialect` moved into core.
  - JSON function encoding changed in 7.4.4 (HSQLDB is affected for binary, UUID and temporal values).
- **DDL**: `@ElementCollection` sets get a unique constraint. `@CreationTimestamp`/`@UpdateTimestamp` columns become NOT NULL. Oracle 23+ uses value-based LOBs.
- **New, optional**: core `@Audited`/`@Changelog`, an alternative to Envers.
- **Support status**: 7.3 is already EOL; 7.4 is the latest stable.

## (e) URLs read

- https://raw.githubusercontent.com/hibernate/hibernate-orm/7.0/migration-guide.adoc (also 7.1, 7.2, 7.3, 7.4; and 6.0, 6.2–6.6 for the 5.6 jump)
- https://hibernate.org/orm/releases/7.0/ , /7.1/ , /7.2/ , /7.3/ , /7.4/ (compatibility sections)
- Maven Central:
  - `org/hibernate/orm/hibernate-core/{6.6.58,7.3.13,7.4.12}.Final` (jar, pom, and the 7.4.12 sources jar)
  - `org/hibernate/hibernate-core-jakarta/5.6.15.Final`
  - `org/hibernate/orm/hibernate-community-dialects/{6.6.58,7.3.13,7.4.12}.Final`
  - `jakarta/persistence/jakarta.persistence-api/{3.1.0,3.2.0}`
- Not fetched: the docs.jboss.org HTML rendering of the migration guides. Content was taken from the adoc sources instead, so it is the same text.

UNVERIFIED:
- The exact JPA 3.2 TCK-level semantics beyond what the guides state.
- Whether the 7.4 limit-in-SQL change alters results for multi-department data. The probe only had one department, and 7.3 and 7.4 gave identical results.
