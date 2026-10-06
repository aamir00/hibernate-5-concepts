> Research notes collected 2026-10-07 for the upgrade plans. "probe" / "scratchpad/..." references are throwaway programs and jars used during the research; they are not part of this repo.

# Hibernate ORM 5.6.15 (hibernate-core-jakarta) -> 6.6.58.Final migration notes

Evidence types: **[MG x.y]** = official migration guide section; **[javap]** = `javap` on
`hibernate-core-6.6.58.Final.jar`; **[src]** = 6.6.58 `-sources.jar`; **[probe6]/[probe5]** = a throwaway
program run against HSQLDB 2.7.4 in-memory, on 6.6.58 and on 5.6.15 (same entities, same settings:
`hibernate.dialect=HSQLDialect`, `hibernate.temp.use_jdbc_metadata_defaults=false`, hbm2ddl create).
Probe sources: `scratchpad/orm6probe/src/main/java/probe/{Main,Ddl}.java`, `scratchpad/orm5probe/src/probe/Main5.java`.

## (a) Requirements

- **Artifact**: `org.hibernate:hibernate-core-jakarta:5.6.15.Final` -> `org.hibernate.orm:hibernate-core:6.6.58.Final`
  (Maven group changed to `org.hibernate.orm` in 6.x). Transitive deps change: antlr 2.7.7 -> antlr4-runtime 4.13.2,
  hibernate-commons-annotations 5.1.2 -> 7.0.3, jandex 2.4 -> io.smallrye:jandex 3.2.0, byte-buddy 1.12 -> 1.17.8,
  jakarta.persistence-api 3.0.0 -> 3.1.0, jboss-logging 3.4.3 -> 3.5.0 (from `mvn dependency:copy-dependencies`).
- **Java**: baseline Java 11 [MG 6.0 "Java 11"]. hibernate.org/orm/releases/6.6: Java 11, 17, 21, 25 (6.6.40+), 26 (6.6.49+).
- **Jakarta Persistence 3.1**, Jakarta EE 10 (hibernate.org/orm/releases/6.6). `jakarta.*` already used by the 5.6 jakarta artifact, so no package rename needed.
- **HSQLDB**: `HSQLDialect.MINIMUM_VERSION = 2.6.1` [src HSQLDialect.java:73; MG 6.2 "Removal of support for legacy database versions"]. HSQLDB 2.7.4 OK (probe: `doReturningWork` -> `2.7.4`).
- `org.jboss.logging.provider=slf4j` is a jboss-logging system property, not a Hibernate setting; still honoured by jboss-logging 3.5 (UNVERIFIED at runtime — probe used JUL).

## (b) API used -> status in 6.6 -> replacement

| API used (5.6) | 6.6.58 status | Replacement / note | Source |
|---|---|---|---|
| `new Configuration()`, `setProperty(String,String)`, `getProperty`, `getProperties`, `addAnnotatedClass`, `buildSessionFactory(ServiceRegistry)` | present, unchanged (extra typed `setProperty(String, boolean/int/Class/Enum)` overloads added) | — | javap |
| `StandardServiceRegistryBuilder.applySettings(Properties)` / `(Map<String,Object>)`, `build()`, static `destroy(ServiceRegistry)` | present | — | javap |
| `StandardServiceRegistry.getService(ConnectionProvider/JdbcEnvironment/JdbcServices)` | works | `requireService(...)` also available | probe6 |
| `StandardServiceRegistryImpl.isActive()` / `destroy()` | works (`isActive` inherited from `AbstractServiceRegistryImpl`); false after `destroy` | — | probe6 |
| `Metadata.getEntityBindings()`, `getDatabase().getDefaultNamespace()`, `getSessionFactoryBuilder().build()` | works | — | probe6 |
| `MetadataImpl.getBootstrapContext()`, `getTypeConfiguration()` | present | — | javap |
| `BootstrapContext.getServiceRegistry()`, `getJpaCompliance().isJpaQueryComplianceEnabled()` | present (`getJpaCompliance` now returns `MutableJpaCompliance`) | — | javap |
| `SessionFactory.getMetamodel().getEntities()` | works; via `SessionFactory` it is `jakarta.persistence.metamodel.Metamodel` (inherited from `EntityManagerFactory`). `org.hibernate.Metamodel` interface `@Deprecated(since="6.0")`; `SessionFactoryImplementor.getMetamodel()` deprecated | `getMetamodel()` (JPA) is fine; or `unwrap(SessionFactoryImplementor).getMappingMetamodel()` | javap, src Metamodel.java:18, SessionFactoryImplementor.java:222 |
| `SessionFactory.getStatistics()`, `Statistics.getPrepareStatementCount()/clear()` | present | — | javap, probe6 |
| `Identifier(String,boolean)`, `getText`, `render()`, `isQuoted()`, `toIdentifier(String)` | present | — | javap |
| `QualifiedNameImpl(Identifier,Identifier,Identifier)`, `getSchemaName/getObjectName/render` | present | — | javap, probe6 |
| `PersistentClass.getEntityName/getClassName/getIdentifierProperty/getTable/getVersion` | present | — | javap |
| `PersistentClass.getPropertyIterator()` | **removed** | `getProperties()` (List) | javap |
| `Table.getColumnIterator()` | **removed** | `getColumns()` (Collection) | javap |
| `Table.getUniqueKeyIterator()/getForeignKeyIterator()` | present but `@Deprecated(since="6.0", forRemoval=true)` | `getUniqueKeys().values()`, `getForeignKeys().values()` | src Table.java:330,342 |
| `Table.getName/getSchema/getCatalog/getQualifiedTableName/getPrimaryKey/setSchema`, `new Table(String)` | present (`new Table()` no-arg is `@Deprecated(since="6.2", forRemoval=true)`) | — | javap, src Table.java:81 |
| `Constraint/PrimaryKey/UniqueKey/ForeignKey.getColumnIterator()` | **removed** | `getColumns()` (List); PK also `getColumnsInOriginalOrder()` | javap |
| `Constraint.getName/containsColumn`, `ForeignKey.getReferencedColumns()/getReferencedTable()` | present (`getReferencedColumns` returns `List<Column>`; empty when FK targets PK — same as 5.x) | — | javap, probe6 |
| `Column.getName/getValue/isNullable/isUnique` | present | — | javap |
| `Column.getLength/getPrecision/getScale` | present but **types changed** `int -> Long/Integer/Integer` and **defaults gone**: 5.x returned 255/19/2 for unset columns, 6.6 returns `null` | null-check; use `getSqlType(Metadata)` for the effective DDL type | probe5 vs probe6 |
| `Column.getSqlType()` | present (returns explicit `columnDefinition` only, else null — same as 5.x) | — | probe6 |
| `Column.getSqlTypeCode()` | present (`Integer`, may be null e.g. FK column) | `getSqlTypeCode(Mapping)` — `Metadata` is a `Mapping` | javap, probe6 |
| `Column.getSqlType(Dialect, Mapping)` | **removed** | `getSqlType(Metadata)` (preferred); `getSqlType(TypeConfiguration, Dialect, Mapping)` exists but `@Deprecated(since="6.2")` | javap, src Column.java:395-404 |
| `Value.isSimpleValue()`, `Value.getType().getName()` | present | — | javap, probe6 |
| `SimpleValue.getTypeName()` | present but **returns null for basic attributes** in 6.6 (5.x: `java.lang.String`, `java.math.BigDecimal`, `materialized_clob`...) | `getValue().getType().getName()` (`string`, `big_decimal`, ...) | probe5 vs probe6 |
| `SimpleValue.getIdentifierGeneratorStrategy()` | present; still `"identity"` for `@GeneratedValue(IDENTITY)` | — (6.x also has `getCustomIdGeneratorCreator()` for `@IdGeneratorType`) | javap, src GenerationStrategyInterpreter.java:51, probe6 |
| `SimpleValue.getIdentifierGeneratorProperties()` | present, `@Deprecated @Remove`; returns a `Properties` copy of the same keys as 5.x | `getIdentifierGeneratorParameters()` (`Map<String,Object>`) | src SimpleValue.java:461-470, probe6 |
| `StandardBasicTypes.STRING/TEXT/INTEGER/BIG_DECIMAL.getName()` | constants are now `BasicTypeReference<T>` (not `BasicType`); names unchanged: `string`, `text`, `integer`, `big_decimal` | — | probe6 |
| `@org.hibernate.annotations.Cascade` + `CascadeType.SAVE_UPDATE` | `SAVE_UPDATE` `@Deprecated` ("since Session#saveOrUpdate is deprecated"); `REMOVE` OK | JPA `cascade = {PERSIST, MERGE}` (and `REMOVE`) on `@OneToMany` | src annotations/CascadeType.java:103-107 |
| `Dialect.quote`, `openQuote/closeQuote`, `getLimitHandler()`, `getIdentityColumnSupport().supportsIdentityColumns()` | present | — | javap, probe6 |
| `Dialect.supportsSequences()`, `Dialect.getSequenceNextValString(String)` | **removed from Dialect** | `dialect.getSequenceSupport().supportsSequences()` / `.getSequenceNextValString(name)` | javap |
| `Dialect.getTypeName(int,long,int,int)` | **removed** | `typeConfiguration.getDdlTypeRegistry().getTypeName(int, Size)` / `(int, Size, Type)`; `(int, Long, Integer, Integer)` exists but `@Deprecated(since="6.3")` | javap, src DdlTypeRegistry.java:242-246 |
| `DB2Dialect`, `HSQLDialect`, `MySQLDialect`, `OracleDialect`, `PostgreSQLDialect` no-arg ctor | present, not deprecated, instantiable without a DB | — | javap, probe6 |
| `HANACloudColumnStoreDialect()` | present, `@Deprecated(forRemoval=true)` | `new HANADialect(DatabaseVersion.make(4))` | src HANACloudColumnStoreDialect.java:22-24 |
| `SQLServer2012Dialect()` | present, `@Deprecated` | `new SQLServerDialect(DatabaseVersion.make(11))` | src SQLServer2012Dialect.java:14-16 |
| `Session.saveOrUpdate/save/update/delete` | all `@Deprecated(since="6.0")`, still functional | `persist` / `merge` / `remove` | src Session.java:633-792 |
| `Session.persist/get/isOpen/isConnected/beginTransaction/doWork/doReturningWork` | present, not deprecated | — | src, probe6 |
| `Session.createQuery(String)` (untyped, used for HQL update) | `@Deprecated(since="6.0")`, works | `createMutationQuery(String)` for update/delete; `createQuery(String, Class)` / `createSelectionQuery` for selects | src QueryProducer.java:61-66 |
| `Session.createNativeQuery(String)` (untyped) | `@Deprecated(since="6.0")`, returns raw `NativeQuery` (impl `NativeQueryImpl`), works | `createNativeQuery(String, Class)` e.g. `Object[].class`, `Object.class`, `Long.class` | src QueryProducer.java:138-141, probe6 |
| `Query.setParameter/setFirstResult/setMaxResults/list/uniqueResult/executeUpdate/scroll()/scroll(ScrollMode)` | present | see Q2 about `scroll()` | probe6 |
| `ScrollableResults.next/get()/getRowNumber/close` | generic `ScrollableResults<R>`; `get()` returns `R`; **`get(int)` removed**; `getRowNumber()` 0-based (unchanged) | — | javap, src ScrollableResults.java:119-122 |
| `org.hibernate.internal.AbstractScrollableResults` protected `getResultSet()` (reflection) | class still in `org.hibernate.internal`, now generic; **`getResultSet()` removed**; only `getJdbcValues()`, `getRowProcessingState()`, etc. | Run the SQL yourself in `session.doWork/doReturningWork` and read `ResultSetMetaData` | javap |
| `org.hibernate.internal.SessionImpl.connection()` | **removed** (no `connection()` on SessionImpl / AbstractSharedSessionContract / SharedSessionContractImplementor) | `doWork`/`doReturningWork`, or `((SharedSessionContractImplementor) s).getJdbcCoordinator().getLogicalConnection().getPhysicalConnection()` (verified in probe6) | javap, src grep |
| `Transaction.getStatus/commit/rollback`, `Hibernate.isInitialized` | present | — | probe6 |
| `JDBCException.getSQL()/getSQLException()` | present (`org.hibernate.JDBCException`) | — | javap |
| `SQLExceptionConverter.convert(SQLException,String,String)` | present, same package `org.hibernate.exception.spi`; returns `JDBCException` | — | javap |
| `JdbcServices.getSqlExceptionHelper().getSqlExceptionConverter()` | present (`org.hibernate.engine.jdbc.spi.SqlExceptionHelper`); default impl `StandardSQLExceptionConverter`; SQLState 42501 -> `SQLGrammarException` | — | javap, probe6 |

## (c) Answers to the 15 questions

1. **Native result typing.** `createNativeQuery(sql).list()` returns `Object[]` per row for multi-column and the bare
   scalar for single-column — same shape as 5.x. Java types on HSQLDB (probe5 -> probe6):
   BIGINT id `BigInteger` -> **`Long`**; `COUNT(*)` `BigInteger` -> **`Long`**; DECIMAL/NUMERIC `BigDecimal` -> `BigDecimal`;
   INTEGER `Integer` -> `Integer`; DATE `java.sql.Date` -> `java.sql.Date`; VARCHAR `String`; null CLOB `null`.
   Guide: [MG 6.0 "SQL BIGINT/count() mapping changes"] — "native queries ... returning a count() ... will now convert that element to Long, instead of BigInteger".
2. **ScrollableResults.** `ScrollableResults<R>`; for an untyped native query R is `Object`: `get()` returns `Object[]` for a multi-column row (same as 5.x) but the **bare scalar for a single-column row** (5.x returned `Object[]{"Ann"}`, 6.6 returns `"Ann"`) [probe5 vs probe6]. `get(int)` is gone [javap]. `getRowNumber()` still there, 0-based (0 after first `next()`).
   Scroll still works on native queries, **but** `scroll()` with no args uses `Dialect.defaultScrollMode()` = `SCROLL_INSENSITIVE` [src Dialect.java:4275] and throws `org.hibernate.AssertionFailure: scrollable result sets are not enabled` [src StatementPreparerImpl.java:137-139] when JDBC metadata access is disabled (our `hibernate.temp.use_jdbc_metadata_defaults=false`), because `hibernate.jdbc.use_scrollable_resultset` is then not auto-detected. 5.6.15 with the same setting did not throw [probe5]. Fixes (each verified in probe6): set `hibernate.jdbc.use_scrollable_resultset=true`, or allow metadata access, or use `scroll(ScrollMode.FORWARD_ONLY)` (always works).
3. **save/saveOrUpdate/update/delete**: all `@Deprecated(since = "6.0")` but fully functional in 6.6 (probe: `saveOrUpdate` assigned id, `delete` worked). Replacements per javadoc: `save` -> `persist`; `saveOrUpdate` -> `merge` or `persist`; `update` -> `merge`; `delete` -> `remove` [src Session.java:633-792]. (Removed in 7.0 — out of scope here.)
4. **SAVE_UPDATE cascade**: `@Deprecated` in 6.6 [src CascadeType.java:103-107]. Its cascade style only fires for the `SAVE_UPDATE` action and `CHECK_ON_FLUSH` [src CascadeStyles.java:68-73], so **it does not cascade at `persist()` time**. But with native bootstrap (not JPA-bootstrap, JPA cascade compliance off) the flush-time cascade action is `SAVE_UPDATE` [src AbstractFlushingEventListener.java:214-218], so `persist(dept)` + flush still saved the child employee in probe6. Replace with JPA `@OneToMany(mappedBy=..., cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})` (drop the Hibernate `@Cascade`).
5. **Dialects** (all in `org.hibernate.dialect`, core jar; all no-arg constructible without a connection — probe6):
   | Class | 6.6 status | No-arg version assumed | getLimitHandler | seq / identity |
   |---|---|---|---|---|
   | DB2Dialect | OK | 10.5 | LegacyDB2LimitHandler | true / true |
   | HANACloudColumnStoreDialect | `@Deprecated(forRemoval=true)` -> `HANADialect(DatabaseVersion.make(4))` | 4.0 | LimitOffsetLimitHandler | true / true |
   | HSQLDialect | OK, min 2.6.1 | 2.6.1 | OffsetFetchLimitHandler | true / true |
   | MySQLDialect | OK, min 8.0 (raised from 5.7 in 6.2 table) | 8.0 | LimitLimitHandler | false / true |
   | OracleDialect | OK, min 19 | 19.0 | Oracle12LimitHandler | true / true |
   | PostgreSQLDialect | OK, min 12 | 12.0 | OffsetFetchLimitHandler | true / true |
   | SQLServer2012Dialect | `@Deprecated` -> `SQLServerDialect(DatabaseVersion.make(11))` | 11.0 | SQLServer2012LimitHandler | true / true |
   No-arg dialects assume `getMinimumSupportedVersion()`. `Dialect.getTypeName(int,long,int,int)` removed; DDL names now come from `TypeConfiguration.getDdlTypeRegistry()` which is only populated after a dialect contributes types (i.e. after bootstrap). Verified approach without a DB: build a registry with `hibernate.dialect=<X>` + `hibernate.boot.allow_jdbc_metadata_access=false`, `new MetadataSources(r).buildMetadata().getDatabase().getTypeConfiguration().getDdlTypeRegistry().getTypeName(Types.VARCHAR, Size.length(100L))` (note: pass `Size.nil()`, not `null`, or NPE). Results e.g. Oracle `varchar2(100 char)`, `number(10,2)`, `number(10,0)`; SQLServer CLOB `varchar(max)`; Postgres CLOB `oid`; HANA `nvarchar(100)` [probe6 Ddl.java]. `supportsSequences/getSequenceNextValString` moved to `getSequenceSupport()`. `hibernate.dialect` is now optional when metadata access is allowed: 6.6 logs `HHH90000025: HSQLDialect does not need to be specified explicitly` [probe6 log]; it is **required** when metadata access is disabled [MG 6.5 "hibernate.boot.allow_jdbc_metadata_access"]. Version-specific dialects deprecated [MG 6.0 "Version-specific ... dialects are deprecated"]; legacy-version dialects moved to `hibernate-community-dialects` [MG 6.2 "Removal of support for legacy database versions"].
6. **Column.getSqlType(Dialect, Mapping)**: removed. Use `Column.getSqlType(Metadata)`; `getSqlType(TypeConfiguration, Dialect, Mapping)` exists but deprecated since 6.2 [javap; src Column.java:395-404]. Probe: `getSqlType(md)` -> `bigint`, `varchar(255)`, `numeric(10,2)`, `integer`, `date`, `clob` (identical strings to 5.x `getSqlType(dialect, md)`).
7. **Iterators**: `Table.getColumnIterator()` removed -> `getColumns()`; `Table.getUniqueKeyIterator()/getForeignKeyIterator()` still present but `@Deprecated(since="6.0", forRemoval=true)` -> `getUniqueKeys().values()/getForeignKeys().values()`; `Constraint.getColumnIterator()` removed -> `getColumns()`; `PersistentClass.getPropertyIterator()` removed -> `getProperties()` [javap, src].
8. **SimpleValue generator API**: `getIdentifierGeneratorStrategy()` present, not deprecated, returns `"identity"` for IDENTITY. `getIdentifierGeneratorProperties()` present but `@Deprecated @Remove` -> `getIdentifierGeneratorParameters()` (`Map<String,Object>`); same keys as 5.x (`identifier_normalizer`, `target_table`, `target_column`, `GENERATOR_NAME`) [javap, src, probe5/6].
9. **StandardBasicTypes**: constants are `org.hibernate.type.BasicTypeReference<T>`; `getName()` -> `string`, `text`, `integer`, `big_decimal` [probe6]. Code that assigns them to `BasicType`/`Type` variables won't compile (resolve via `typeConfiguration.getBasicTypeRegistry().resolve(ref)`).
10. **SessionFactory.getMetamodel()**: still there; on `SessionFactory` it returns `jakarta.persistence.metamodel.Metamodel` (5.x: `org.hibernate.Metamodel`). `getEntities()` works (`[Department, Employee]`). `org.hibernate.Metamodel` is deprecated since 6.0 [src Metamodel.java:14-18]; prefer `getMetamodel()` (JPA) or `getMappingMetamodel()` on the implementor.
11. **Settings**: `hibernate.temp.use_jdbc_metadata_defaults` -> `hibernate.boot.allow_jdbc_metadata_access` (added 6.5) [MG 6.5]; old name still honoured but logs `HHH90000021: Encountered deprecated setting` and is `@Deprecated(since="6", forRemoval=true)` [src JdbcEnvironmentInitiator.java:94; probe6]. With it off, Hibernate assumes the dialect's minimum version (HSQL 2.6.1 shown in probe) [MG 6.5]; you may set `jakarta.persistence.database-product-version`. `hibernate.connection.autocommit` unchanged (`JdbcSettings.AUTOCOMMIT`) [src JdbcSettings.java:264]. Other settings in use (driver_class/url/username/password, show_sql, globally_quoted_identifiers, hbm2ddl.auto, generate_statistics) unchanged.
12. **Session.isConnected / raw queries**: `isConnected()` present (`SharedSessionContract`). `createNativeQuery(String)` returns raw `NativeQuery` (deprecated method); `createQuery(String)` returns raw `Query` (deprecated). `list()` on raw works; prefer typed `createNativeQuery(sql, Object[].class)` etc.
13. **Exceptions**: `org.hibernate.JDBCException` (getSQL, getSQLException, getSQLState), `org.hibernate.exception.spi.SQLExceptionConverter`, `org.hibernate.engine.jdbc.spi.SqlExceptionHelper` — same packages as 5.x [javap]. `HibernateException extends jakarta.persistence.PersistenceException`. **Behaviour change**: native `list()` / `executeUpdate()` on a missing table — 5.6.15 threw `jakarta.persistence.PersistenceException` wrapping `SQLGrammarException`; 6.6.58 throws `org.hibernate.exception.SQLGrammarException` **directly** (cause `java.sql.SQLSyntaxErrorException`, message `could not prepare statement [...]`) [probe5 vs probe6]. `catch (PersistenceException)` still catches it, but `e.getCause() instanceof SQLGrammarException` checks break — check `e instanceof JDBCException` instead.
14. **AbstractScrollableResults**: still `org.hibernate.internal.AbstractScrollableResults<R>`, but `getResultSet()` is gone [javap]. Internals are `JdbcValues`/`RowProcessingState`; `JdbcValuesResultSetImpl` exposes no public ResultSet getter. Replacement for reading result-set metadata: execute the SQL via `session.doReturningWork(conn -> ...)` and use `ResultSetMetaData`, or type columns explicitly with `NativeQuery.addScalar(...)`.
15. **SessionImpl.connection()**: removed (javac "cannot find symbol"; no `connection()` on `SessionImpl`, `AbstractSharedSessionContract` or `SharedSessionContractImplementor`) [javap, src grep]. Use `doWork/doReturningWork`, or `((SharedSessionContractImplementor) session).getJdbcCoordinator().getLogicalConnection().getPhysicalConnection()` (verified returns `JDBCConnection` in probe6).

## (d) Other 6.x changes relevant to this project

- HQL `select distinct d from Department d left join fetch d.employees`: still valid; in 6 parent duplicates are always filtered and `distinct` is always passed to SQL [MG 6.0 "DISTINCT"]. `HINT_PASS_DISTINCT_THROUGH` removed.
- HQL `update Employee e set ...` fine; `update from Employee` would now fail [MG 6.0 "FROM token now disallowed for UPDATE"].
- HQL numeric literal typing aligned with Java (`100` is Integer) [MG 6.3]; `= null` comparisons no longer special-cased, use `is null` [MG 6.3]; path comparisons type-checked early, temporal-vs-string literal comparisons fail [MG 6.2 "Query Path comparison"].
- `from A join A.b` without select returns `List<A>` not `Object[]` [MG 6.0 "Result rows"]; `Query#iterate()` removed [MG 6.0]; legacy Criteria API removed [MG 6.0].
- 6.5 validates the declared result class against the query's actual return type -> `TypeMismatchException` [MG 6.5 "Validation of Query Result Type"] (our `Object[].class`, `Long.class`, `Employee.class` uses passed in probe6).
- ID generation: `@GeneratedValue(IDENTITY)` unchanged. If anything uses AUTO/SEQUENCE: per-entity `<entity>_seq` sequences with allocationSize 50 instead of `hibernate_sequence` [MG 6.0 "Implicit Identifier Sequence and Table Name", "Defaults for implicit sequence generators"]; `hibernate.id.db_structure_naming_strategy=legacy` restores old naming. `@GeneratedValue` on non-id field now an error [MG 6.4]. `@GenericGenerator` deprecated [MG 6.5].
- Lazy associations with `@Fetch(JOIN)` now truly lazy on `get()` [MG 6.0 "Association laziness now respected"] — `Hibernate.isInitialized(e.department)` false after `get` in probe6.
- DDL/schema: enums TINYINT/SMALLINT changes [MG 6.1, 6.2]; UUID/Instant/Duration/OffsetTime/timezone storage defaults changed [MG 6.0, 6.2] (not used by demo, matters only for schema-validate). `@Lob String` now `clob` on HSQL; FK/PK/unique names auto-generated as before.
- `jakarta.persistence.*`/`hibernate.*` settings: `hibernate.ejb.*` names removed [MG 6.0 "Configuration property renames"]; `hibernate.hql.bulk_id_strategy` -> `hibernate.query.mutation_strategy` [MG 6.0].
- `org.hibernate.cfg` package split, only `Configuration`, `AvailableSettings`, `Environment` remain API [MG 6.2 "org.hibernate.cfg package"].
- Merge of a detached versioned/generated-id entity whose row was deleted now throws `OptimisticLockException` instead of inserting [MG 6.6 "Merge versioned entity when row is deleted"] — relevant if `saveOrUpdate` is replaced by `merge`.
- `@Table` on a SINGLE_TABLE subclass, or a class with both `@MappedSuperclass` and `@Embeddable`, now fails [MG 6.6].
- 6.x warns `HHH90000025` when `hibernate.dialect` is set to a dialect that would be auto-detected [probe6 log].
- Identifiers no longer need to be `Serializable` (`get(Class, Object)`) [MG 6.0 "Identifier as Object"]; `Interceptor.onSave` signature changed [MG 6.0].
- hbm.xml deprecated [MG 6.0] (demo uses annotations only).

## (e) Migration-guide URLs actually read

- https://raw.githubusercontent.com/hibernate/hibernate-orm/6.0/migration-guide.adoc (= docs.jboss.org/hibernate/orm/6.0/migration-guide/migration-guide.html source)
- https://raw.githubusercontent.com/hibernate/hibernate-orm/6.1/migration-guide.adoc
- https://raw.githubusercontent.com/hibernate/hibernate-orm/6.2/migration-guide.adoc
- https://raw.githubusercontent.com/hibernate/hibernate-orm/6.3/migration-guide.adoc
- https://raw.githubusercontent.com/hibernate/hibernate-orm/6.4/migration-guide.adoc
- https://raw.githubusercontent.com/hibernate/hibernate-orm/6.5/migration-guide.adoc
- https://raw.githubusercontent.com/hibernate/hibernate-orm/6.6/migration-guide.adoc
- https://hibernate.org/orm/releases/6.6/ (compatibility table)
- Jars: https://repo1.maven.org/maven2/org/hibernate/orm/hibernate-core/6.6.58.Final/ (binary + sources), `org.hibernate:hibernate-core-jakarta:5.6.15.Final` via Maven.

UNVERIFIED: slf4j logging provider at runtime; the rendered docs.jboss.org HTML pages themselves (raw adoc sources from the same branches were read instead).
