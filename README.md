# hibernate-5-concepts

Runnable examples of **every Hibernate class/concept used by `wmstdappdbimpl`**, ported to
`org.hibernate.orm:hibernate-core` **7.4.12.Final** and `org.hibernate.orm:hibernate-reveng` **7.4.12.Final**
(Hibernate Tools, which moved into the ORM project in 7.4). Branch `hibernate-7.4`, built on top of
`hibernate-6-to-7` (7.3.13) and `hibernate-6` (6.6.58); `main` has the original 5.6.15 version. The plans and
research behind the port are in [`plans/`](plans); what changed per row is in
[Changed in this version](#changed-in-this-version-7313--7412),
[Changed 6.6.58 → 7.3.13](#changed-6658--7313-branch-hibernate-6-to-7) and
[Changed 5.6.15 → 6.6.58](#changed-5615--6658-branch-hibernate-6).
The row numbers below (and in every `=== [Row N] ... ===` header the Mains print) are the rows of
[`../hibernate-classes-and-concepts.md`](../hibernate-classes-and-concepts.md).

The code is plain Hibernate. It does not copy wmstdappdbimpl's classes. Where it helps, a comment names the
wmstdappdbimpl class that uses the same API.

## Run

Requirements: JDK 21 (Hibernate 7 needs 17+). Nothing else: the database is in-memory HSQLDB, and dependencies come from
`~/.m2` / Maven Central.

```bash
./gradlew run                                              # RunAllMain: every Main + a summary
./gradlew run -PmainClass=h5.concepts.NativeSqlSessionMain # one Main
```

Or open the folder in IntelliJ (Gradle import) and click the run arrow next to any `main`.
`ExporterMain` writes to `build/export-out/` relative to the working directory (the project root by default).

## Mains

| Main | Rows | What it shows |
|---|---|---|
| `BootstrapMain` | 1–7 | `Configuration` + `StandardServiceRegistryBuilder` → `SessionFactory`; reveng `Metadata` → `MetadataImpl.getBootstrapContext()` → `StandardServiceRegistryImpl.destroy()` |
| `MappingModelMain` | 8–18 | `Identifier`, `QualifiedNameImpl`; walks every reverse-engineered `PersistentClass` → `Table` → columns (`Column`/`Value`/`SimpleValue` incl. id-generator strategy + parameters), `PrimaryKey`/`UniqueKey`/`ForeignKey` via the collection getters (`getForeignKeyCollection()` in 7); `StandardBasicTypes` names |
| `EntityHqlMain` | 19–20 (+29–34) | Two `@Entity`s + HQL: `FetchType.LAZY` vs `EAGER` (SQL printed, statement counts), JPA `cascade = {PERSIST, MERGE, REMOVE}` in action (`persist`, detached `merge`, `remove`; Hibernate's `@Cascade` is deprecated for removal in 7), `find`, typed HQL, projection, paging, `join fetch`, bulk update via `createMutationQuery` |
| `DialectsMain` | 21–28 | `Dialect` basics (`getSequenceSupport()`, DDL type names from the `DdlTypeRegistry`), and the 7 dialects wmstdappdbimpl names (HANA / SQL Server via their replacements, the old classes are removed in 7): `openQuote()/closeQuote()`, superclass, `@Deprecated`, assumed DB version |
| `NativeSqlSessionMain` | 29–37 | Entity-less `SessionFactory` (like `DBConnectionUtils`); `Session`, `Transaction` commit/rollback, typed native `Query` (params, paging, `list`, `uniqueResult`, `createNativeMutationQuery`, `scroll`), `AbstractScrollableResults.getResultSet()` gone → `doWork` + `ResultSetMetaData`, `SessionImpl.connection()` gone → `getJdbcCoordinator()` vs `doWork`, exception hierarchy |
| `RevengStrategyMain` | 39–47, 53–54 | `TableIdentifier.create`, `TableNameQualifier`, `NameConverter`, `DefaultStrategy`/`DelegatingStrategy`, `RevengSettings`, `TableFilter`, `OverrideRepository` + `hibernate.reveng.xml`, `MetadataDescriptorFactory.createReverseEngineeringDescriptor(..).createMetadata()` |
| `MetaDataDialectMain` | 38, 48–52 | `RevengDialectFactory` selection, `JDBCMetaDataDialect` calls (tables, columns, PKs, indexes, exported keys), a custom `AbstractMetaDataDialect` subclass, `MySQLMetaDataDialect`, `ResultSetIterator`, `SQLExceptionConverter` |
| `ExporterMain` | 55–58 | `GenericExporter` (configured through `ExporterConstants` properties) + FreeMarker template per `POJOClass`, `resolveFilename(POJOClass)` override (and why wmstdappdbimpl's `resolveFilename(String)` is never called), `ConfigurationTask` note |
| `RunAllMain` | all | Runs the above in order, prints OK/FAILED + time per Main |

## Row → where it is demonstrated

| # | Class | Demonstrated in |
|---|---|---|
| 1 | `cfg.Configuration` | `BootstrapMain.legacyConfigurationBootstrap`, `NativeSqlSessionMain.createSessionFactory`, `EntityHqlMain.createSessionFactory` |
| 2 | `StandardServiceRegistryBuilder` | `BootstrapMain.legacyConfigurationBootstrap` |
| 3 | `StandardServiceRegistry` | `BootstrapMain.legacyConfigurationBootstrap` (`getService(...)`) |
| 4 | `StandardServiceRegistryImpl` | `BootstrapMain.metadataBootstrapContextAndDestroy` (`destroy()`) |
| 5 | `boot.Metadata` | `BootstrapMain.metadataBootstrapContextAndDestroy`, `MappingModelMain` |
| 6 | `boot.internal.MetadataImpl` | `BootstrapMain.metadataBootstrapContextAndDestroy` |
| 7 | `boot.spi.BootstrapContext` | `BootstrapMain.metadataBootstrapContextAndDestroy` |
| 8 | `naming.Identifier` | `MappingModelMain.relationalNaming` |
| 9 | `QualifiedNameImpl` | `MappingModelMain.relationalNaming` |
| 10 | `mapping.PersistentClass` | `MappingModelMain.mappingModel`, `RevengStrategyMain.descriptorAndMetadata` |
| 11 | `mapping.Table` | `MappingModelMain.mappingModel` (`getColumns/getUniqueKeys/getForeignKeyCollection/getPrimaryKey`) |
| 12 | `mapping.Column` | `MappingModelMain.mappingModel` (`getSqlType()`, `getSqlTypeCode()`, `getSqlType(Metadata)`, length/precision/scale) |
| 13 | `mapping.Value` | `MappingModelMain.mappingModel` (`isSimpleValue()`) |
| 14 | `mapping.SimpleValue` | `MappingModelMain.mappingModel` (generator getters removed in 7 → Tools' `EnhancedValue.getIdentifierGeneratorStrategy/Properties`) |
| 15 | `mapping.PrimaryKey` | `MappingModelMain.mappingModel` |
| 16 | `mapping.UniqueKey` | `MappingModelMain.mappingModel` |
| 17 | `mapping.ForeignKey` | `MappingModelMain.mappingModel` (typed `getReferencedColumns()`), `RevengStrategyMain` (`isOneToOne`) |
| 18 | `type.StandardBasicTypes` | `MappingModelMain.basicTypes`, `RevengSupport.revengXml` |
| 19 | `jakarta.persistence.FetchType` | `EntityHqlMain.fetchTypes`, `entities/*` |
| 20 | `annotations.CascadeType` | `EntityHqlMain.cascadePersist/cascadeMerge/cascadeRemove/hibernateCascadeTypeStatus`, `entities/Department` (JPA `cascade`; the Hibernate enum is deprecated for removal in 7) |
| 21 | `dialect.Dialect` | `DialectsMain` |
| 22–28 | `DB2Dialect`, `HANACloudColumnStoreDialect` (→ `HANADialect`), `HSQLDialect`, `MySQLDialect`, `OracleDialect`, `PostgreSQLDialect`, `SQLServer2012Dialect` (→ `SQLServerDialect`) | `DialectsMain` |
| 29 | `SessionFactory` | `NativeSqlSessionMain.createSessionFactory`, `EntityHqlMain` |
| 30 | `Session` | `NativeSqlSessionMain.queryApi/jdbcConnectionAccess` (`doWork`, `doReturningWork`) |
| 31 | `internal.SessionImpl` | `NativeSqlSessionMain.jdbcConnectionAccess` (`getJdbcCoordinator()...getPhysicalConnection()` + `CallableStatement`; `connection()` was removed in 6) |
| 32 | `Transaction` | `NativeSqlSessionMain.transactions`, `EntityHqlMain` |
| 33 | `query.Query` | `NativeSqlSessionMain.queryApi/scrollableResults`, `EntityHqlMain.hql` |
| 34 | `ScrollableResults` | `NativeSqlSessionMain.scrollableResults` |
| 35 | `internal.AbstractScrollableResults` (`internal.scrollable` in 7) | `NativeSqlSessionMain.scrollableResults` (`getResultSet()` is gone since 6; replacement: `doWork` + `ResultSetMetaData`) |
| 36 | `jakarta.persistence.PersistenceException` | `NativeSqlSessionMain.exceptions` |
| 37 | `HibernateException` | `NativeSqlSessionMain.exceptions` |
| 38 | `exception.spi.SQLExceptionConverter` | `MetaDataDialectMain.resultSetIteratorAndConverter/mySqlDialect`, `support/DemoMetaDataDialect` |
| 39 | `tool.api.metadata.MetadataDescriptor` | `RevengStrategyMain.descriptorAndMetadata`, `ExporterMain` |
| 40 | `tool.internal.metadata.JdbcMetadataDescriptor` (→ `MetadataDescriptorFactory`) | `RevengStrategyMain.descriptorAndMetadata`, `support/RevengSupport.descriptor` |
| 41 | `cfg.reveng.ReverseEngineeringStrategy` (→ `RevengStrategy`) | `RevengStrategyMain` (all sections) |
| 42 | `DefaultReverseEngineeringStrategy` (→ `DefaultStrategy`) | `RevengStrategyMain.defaultStrategyAndSettings` |
| 43 | `DelegatingReverseEngineeringStrategy` (→ `DelegatingStrategy`) | `support/DemoRevengStrategy`, `RevengStrategyMain.delegatingStrategy` |
| 44 | `ReverseEngineeringSettings` (→ `RevengSettings`) | `RevengStrategyMain.defaultStrategyAndSettings`, `support/RevengSupport.strategy` |
| 45 | `OverrideRepository` | `RevengStrategyMain.overridesAndFilters`, `support/RevengSupport.overrideRepository` |
| 46 | `TableFilter` | `RevengStrategyMain.overridesAndFilters` |
| 47 | `TableIdentifier` | `RevengStrategyMain.valueObjectsAndUtilities` |
| 48 | `MetaDataDialect` (→ `RevengDialect`) | `MetaDataDialectMain.selection` (+ `hibernatetool.metadatadialect`) |
| 49 | `AbstractMetaDataDialect` | `MetaDataDialectMain.jdbcMetaDataDialect/customDialect`, `support/DemoMetaDataDialect` |
| 50 | `JDBCMetaDataDialect` | `MetaDataDialectMain.jdbcMetaDataDialect` |
| 51 | `MySQLMetaDataDialect` | `MetaDataDialectMain.mySqlDialect` |
| 52 | `ResultSetIterator` | `MetaDataDialectMain.resultSetIteratorAndConverter`, `support/DemoMetaDataDialect` |
| 53 | `ReverseEngineeringStrategyUtil` (→ `NameConverter`) | `RevengStrategyMain.valueObjectsAndUtilities`, `support/DemoRevengStrategy` |
| 54 | `tool.util.TableNameQualifier` | `RevengStrategyMain.valueObjectsAndUtilities` |
| 55 | `hbm2x.AbstractExporter` | `ExporterMain` |
| 56 | `hbm2x.GenericExporter` | `ExporterMain.SummaryExporter` |
| 57 | `hbm2x.pojo.POJOClass` | `ExporterMain.SummaryExporter.resolveFilename(POJOClass)`, `templates/entity-summary.ftl` |
| 58 | `hbm2x.ant.ConfigurationTask` | `ExporterMain` (note only — not in Tools 5.6.15, 6.6, 7.3 or 7.4) |

## Changed in this version (7.3.13 → 7.4.12)

Every row still runs. `run-output.txt` holds the full output of `./gradlew run` on this branch (timings and the
project path normalised): `git diff hibernate-6-to-7..hibernate-7.4` shows the 7.3 → 7.4 changes,
`git diff main..hibernate-7.4` the whole 5 → 7.4 path. Core 7.4 only added APIs; the changes are in Tools.

| # | 7.3.13 | 7.4.12 |
|---|---|---|
| build | `org.hibernate.tool:hibernate-tools-orm:7.3.13.Final` | `org.hibernate.orm:hibernate-reveng:7.4.12.Final` (released with core; core declared explicitly because reveng has it at runtime scope; exclude JDT) |
| 39–57 | `org.hibernate.tool.api.reveng.*`, `tool.api.{export,metadata}.*` | `org.hibernate.tool.reveng.api.core.*`, `tool.reveng.api.{export,metadata}.*` |
| 42–52 | `org.hibernate.tool.internal.reveng.{strategy,dialect,util}.*`, `tool.internal.{util,export}.*` | `org.hibernate.tool.reveng.internal.core.{strategy,dialect,util}.*`, `tool.reveng.internal.{util,export}.*` |
| 55/56 | after `exporter.start()` the caller destroys the exporter's service registry | `start()` ends with `stop()`, which closes it (`start(false)` to keep it) |
| 22–28 | `PostgreSQLDialect()` assumes 13 | assumes 14 |

Same API and same behaviour otherwise; `ExporterConstants`/`MetadataConstants` key strings are unchanged.

## Changed 6.6.58 → 7.3.13 (branch `hibernate-6-to-7`)

Tools 7.3.13 has the same API as Tools 6.6.58, so all these changes are in core ORM.

| # | 6.6.58 | 7.3.13 |
|---|---|---|
| build | core + tools-orm 6.6.58, Java 11+, JPA 3.1 | core + tools-orm 7.3.13, **Java 17+**, **JPA 3.2** |
| 14 | `SimpleValue.getIdentifierGeneratorStrategy()/getIdentifierGeneratorParameters()` | **removed**; reverse-engineered ids are Tools' `EnhancedBasicValue` → `EnhancedValue.getIdentifierGeneratorStrategy()/getIdentifierGeneratorProperties()`; a shared-PK one-to-one id (`OneToOne` value) has no generator getters |
| 11/17 | `Table.getForeignKeys().values()` (iterators deprecated) | `getForeignKeyCollection()` (`getForeignKeys()` deprecated for removal; `getUniqueKeyIterator()/getForeignKeyIterator()` removed) |
| 20 | `@org.hibernate.annotations.Cascade({PERSIST, MERGE, REMOVE})` | JPA `@OneToMany(cascade = {PERSIST, MERGE, REMOVE})` — `@Cascade` and Hibernate's `CascadeType` are deprecated for removal; `SAVE_UPDATE`/`DELETE` removed |
| 20/30 | `session.get(Class, id)` | `session.find(Class, id)` (`get` deprecated for removal); `save/saveOrUpdate/update/delete/load` removed |
| 23/28 | `HANADialect(DatabaseVersion.make(4))`, `SQLServerDialect(DatabaseVersion.make(11))` (old classes deprecated) | no-arg `HANADialect()` / `SQLServerDialect()`; `HANACloudColumnStoreDialect` and `SQLServer2012Dialect` removed (not in community dialects either) |
| 34 | `ScrollableResults.getRowNumber()` (0-based), `Closeable` | `getPosition()` (1-based; `getRowNumber()` deprecated for removal), only `AutoCloseable` |
| 35 | `org.hibernate.internal.AbstractScrollableResults` | `org.hibernate.internal.scrollable.AbstractScrollableResults` |
| 33/34 | native `DATE` → `java.sql.Date` | → `java.time.LocalDate` (`hibernate.query.native.prefer_jdbc_datetime_types=true` restores `java.sql.*`) |
| 38 | SQLState 42501 → `SQLGrammarException` | → new `AuthException` (standard converter) |

## Changed 5.6.15 → 6.6.58 (branch `hibernate-6`)

| # | 5.6.15 | 6.6.58 |
|---|---|---|
| build | `org.hibernate:hibernate-core-jakarta`, `org.hibernate:hibernate-tools` | `org.hibernate.orm:hibernate-core`, `org.hibernate.tool:hibernate-tools-orm` |
| setting | `hibernate.temp.use_jdbc_metadata_defaults` | `hibernate.boot.allow_jdbc_metadata_access` (old key deprecated) |
| 10 | `PersistentClass.getPropertyIterator()` | `getProperties()` |
| 11 | `Table.getColumnIterator()` / `getUniqueKeyIterator()` / `getForeignKeyIterator()` | `getColumns()` / `getUniqueKeys().values()` / `getForeignKeys().values()` |
| 12 | `Column.getSqlType(Dialect, Mapping)`; `int getLength/getPrecision/getScale` (255/19/2 when unset) | `getSqlType(Metadata)`; `Long`/`Integer`/`Integer`, `null` when unset |
| 14 | `SimpleValue.getIdentifierGeneratorProperties()` | `getIdentifierGeneratorParameters()` (Map; the old one is deprecated) |
| 15–17 | `Constraint.getColumnIterator()`, raw `ForeignKey.getReferencedColumns()` | `getColumns()`, `List<Column>` |
| 18 | `StandardBasicTypes.X` is a `Type` (`StringType`, …) | a `BasicTypeReference<T>`; `getName()` unchanged |
| 20 | `@Cascade({SAVE_UPDATE, REMOVE})`, `saveOrUpdate()`, `delete()` | `@Cascade({PERSIST, MERGE, REMOVE})`, `persist()` / `merge()`, `remove()` (`SAVE_UPDATE` and the old Session methods are deprecated) |
| 21 | `Dialect.supportsSequences()/getSequenceNextValString()`, `getTypeName(int, long, int, int)` | `getSequenceSupport().…`, `TypeConfiguration.getDdlTypeRegistry().getTypeName(int, Size, Type)` |
| 23 | `HANACloudColumnStoreDialect` | `HANADialect(DatabaseVersion.make(4))` (old class deprecated for removal) |
| 26/27 | `OracleDialect`, `PostgreSQLDialect` deprecated aliases of old versions | the single version-aware dialects (not deprecated) |
| 28 | `SQLServer2012Dialect` | `SQLServerDialect(DatabaseVersion.make(11))` (old class deprecated) |
| 31 | `SessionImpl.connection()` | removed → `getJdbcCoordinator().getLogicalConnection().getPhysicalConnection()` (internal) or `doWork` (public) |
| 33 | untyped `createNativeQuery(String)`, raw `Query`; DML through `createNativeQuery`; HQL update through `createQuery(String)` | `createNativeQuery(sql, Class)`, `Query<T>`; `createNativeMutationQuery(sql)`; `createMutationQuery(hql)` |
| 34 | raw `ScrollableResults`, `get()` → `Object[]` | `ScrollableResults<R>`; `scroll()` without a mode fails with JDBC metadata access off → `scroll(ScrollMode.FORWARD_ONLY)` |
| 35 | `AbstractScrollableResults.getResultSet()` (reflection) | removed → run the SQL in `doWork` and read `ResultSetMetaData` |
| 40 | `new JdbcMetadataDescriptor(strategy, props, preferBasicCompositeIds)` | `MetadataDescriptorFactory.createReverseEngineeringDescriptor(strategy, props)` + `MetadataConstants.PREFER_BASIC_COMPOSITE_IDS` (a `Boolean`) |
| 41–47, 53–54 | `org.hibernate.cfg.reveng.*`, `tool.util.TableNameQualifier` | `org.hibernate.tool.api.reveng.*` (`RevengStrategy`, `RevengSettings`, `TableIdentifier`), `org.hibernate.tool.internal.reveng.strategy.*` (`DefaultStrategy`, `DelegatingStrategy`, `OverrideRepository`, `TableFilter`), `tool.internal.util.NameConverter` / `TableNameQualifier` |
| — | `new SchemaSelection(catalog, schema)` | `RevengStrategy.SchemaSelection` is an interface → implemented in `RevengSupport` |
| 45 | `OverrideRepository.addInputStream` closes the stream | it doesn't → caller closes it |
| 47 | `new TableIdentifier(..)`; `new Table("NAME")` | `TableIdentifier.create(catalog, schema, name)`; `new Table(contributor, "NAME")` (core 6's one-arg constructor takes the contributor) |
| 48 | `MetaDataDialectFactory`, `MetaDataDialect.configure(ReverseEngineeringRuntimeInfo)` | `RevengDialectFactory`, `RevengDialect.configure(ConnectionProvider)` |
| 49–52 | `cfg.reveng.dialect.*`; `getSQLExceptionConverter()`; `ResultSetIterator(.., converter)` | `tool.internal.reveng.dialect.*`; removed (custom dialect uses `SqlExceptionHelper`); `ResultSetIterator(stmt, rs)` |
| 55–57 | `tool.hbm2x.*` exporters with setters | `tool.internal.export.common.*` / `export.java.POJOClass`, configured with `getProperties().put(ExporterConstants.X, ..)` |

## Demo database (`support/DemoDatabase`)

In-memory HSQLDB `jdbc:hsqldb:mem:hr_demo`, schema `PUBLIC`, created once per JVM:

| Object | Why it is there |
|---|---|
| `DEPARTMENT` | identity PK, unique key `UK_DEPT_NAME` |
| `EMPLOYEE` | PK filled from sequence `EMP_SEQ`, FK → `DEPARTMENT`, unique `EMAIL`, `VERSION` column (→ optimistic lock), `CLOB` column |
| `EMPLOYEE_DETAIL` | PK = FK → `EMPLOYEE` → reverse-engineered as **one-to-one** |
| `PROJECT`, `EMPLOYEE_PROJECT` | composite-PK join table → reverse-engineered as **many-to-many** |
| `AUDIT_LOG` | excluded by a `TableFilter` |
| `HIGH_EARNERS` (view) | listed by `JDBCMetaDataDialect`, skipped by `DemoMetaDataDialect` |
| `EMPLOYEE_COUNT(IN, OUT)` | procedure called through `SessionImpl.connection()` / `doWork` |

`EntityHqlMain` uses its own database (`jdbc:hsqldb:mem:entity_demo`), created by `hbm2ddl` from the two entities.

## Things the runs show (7.4.12 behaviour, compared with 5.6.15)

7.4-specific (vs 7.3): only the Tools package names, `PostgreSQLDialect()` assuming version 14, and the exporter
closing its own service registry (see above). SQL, statement counts and results are identical to 7.3.

7.3-specific (vs 6.6):
- **Dialects:** no-arg dialects assume newer minimum versions (DB2 11.1, PostgreSQL 13, SQL Server 12, HANA 2.0.50);
  `HANADialect` now extends `Dialect` directly.
- **Exceptions:** messages start with a capital letter ("Could not prepare statement"); JDBC errors are logged as
  `HHH000247` warnings; the standard converter maps SQLState 42501 to `AuthException`.
- **Small API shapes:** `getDefaultNamespace()` prints as a record (`Name[...]`), the default connection provider is
  `DriverManagerConnectionProvider`, scrollable results live in `org.hibernate.internal.scrollable`, and
  reverse-engineered values are Tools' `EnhancedBasicValue`.
- **Native date/time values:** a `DATE` column comes back as `java.time.LocalDate` (5.x/6.x: `java.sql.Date`).
- **Generated code:** a `DATE` property is `java.util.Date` again (Tools 6.6: `java.sql.Date`).
- No change in SQL, statement counts, cascades or query results compared with 6.6.

Since 5.6 (unchanged from 6.6):

- **Exceptions:** native `Query.list()` on bad SQL throws the `SQLGrammarException` **directly** (5.x wrapped
  it in a plain `jakarta.persistence.PersistenceException`). `catch (PersistenceException)` still catches it,
  `catch (HibernateException)` now does too, and `getCause()` is the JDBC `SQLException`.
- **Native result types:** `COUNT(*)` is a `Long` (5.x: `BigInteger`); other HSQLDB types are unchanged.
- **Reverse-engineered columns:** the no-arg `Column.getSqlType()` is still `null` and `getSqlTypeCode()` holds
  the JDBC type; `getSqlType(Metadata)` gives the resolved SQL type. Unset length/precision/scale are `null`
  (5.x: 255/19/2). `EMPLOYEE_DETAIL.BIO` (mapped to `text` by `hibernate.reveng.xml`) now resolves to
  `varchar(1000)` (5.x: `longvarchar`), and a `CLOB` to `clob` (5.x: `clob(255)`). Reverse-engineered basic values
  are `BasicValue`s (5.x: `SimpleValue`).
- **Cascades:** `PERSIST` cascades at `persist()` time; `MERGE` cascades a detached parent's new children on
  `merge()`. 5.x's `SAVE_UPDATE` (deprecated in 6, removed in 7) also cascaded for `persist()` at flush.
- **`FetchType.EAGER` to-one:** unchanged — HQL loads it with a second `SELECT`, `session.find()` with a join.
  SQL aliases are now short (`e1_0`), and paging renders `fetch first ? rows only`.
- **Scrolling:** `query.scroll()` without a `ScrollMode` throws `AssertionFailure` when JDBC metadata access is
  off, because scrollable result-set support is no longer detected; `FORWARD_ONLY` works.
- **Dialects:** each no-arg dialect assumes the oldest DB version it supports (shown in `DialectsMain`).
  Setting `hibernate.dialect` explicitly logs `HHH90000025`, but it is still required here because JDBC
  metadata access is off.
- **Tools needs explicit cleanup:** `MetadataDescriptor.createMetadata()` still builds its own service registry
  that nobody closes. Destroy it through `MetadataImpl.getBootstrapContext()` (as wmstdappdbimpl does). Exporters
  close theirs themselves from 7.4.
- **`hibernate.reveng.xml`:** Tools 6.6/7.3/7.4 still do not bundle the reverse-engineering DTD, so a DOCTYPE line
  makes the JDK parser try to download it. `RevengSupport.revengXml()` leaves it out.
- **`MySQLMetaDataDialect.getSuggestedPrimaryKeyStrategyName`** runs MySQL-only `show table status`. On any
  other DB it now fails with a plain `RuntimeException` wrapping the `SQLException` (5.x: a typed
  `SQLGrammarException` with `getSQL()`).
- **Exporter:** `POJOClass.getQualifiedDeclarationName()` repeats the package (`com.demo.hr.com.demo.hr.X`) in
  Tools 6.6, 7.3 and 7.4.
- **Logging:** Tools 6/7 log every reverse-engineered table at INFO through java.util.logging; `Out` keeps only its
  warnings (e.g. "Binding column twice should not happen", logged for `EMPLOYEE.DEPT_ID` and `EMPLOYEE_DETAIL.EMP_ID`,
  which are both a property column and a foreign-key column; reverse engineering still completes normally).
