# hibernate-5-concepts

Runnable examples of **every Hibernate class/concept used by `wmstdappdbimpl`**, ported to
`org.hibernate.orm:hibernate-core` **6.6.58.Final** and `org.hibernate.tool:hibernate-tools-orm` **6.6.58.Final**
(branch `hibernate-6`; `main` has the original 5.6.15 version). The plan and research behind the port are in
[`plans/5to6.md`](plans/5to6.md); what changed per row is in [Changed in this version](#changed-in-this-version-5615--6658).
The row numbers below (and in every `=== [Row N] ... ===` header the Mains print) are the rows of
[`../hibernate-classes-and-concepts.md`](../hibernate-classes-and-concepts.md).

The code is plain Hibernate. It does not copy wmstdappdbimpl's classes. Where it helps, a comment names the
wmstdappdbimpl class that uses the same API.

## Run

Requirements: JDK 21. Nothing else: the database is in-memory HSQLDB, and dependencies come from
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
| `MappingModelMain` | 8–18 | `Identifier`, `QualifiedNameImpl`; walks every reverse-engineered `PersistentClass` → `Table` → columns (`Column`/`Value`/`SimpleValue` incl. id-generator strategy + parameters), `PrimaryKey`/`UniqueKey`/`ForeignKey` via the 6.x collection getters; `StandardBasicTypes` names |
| `EntityHqlMain` | 19–20 (+29–34) | Two `@Entity`s + HQL: `FetchType.LAZY` vs `EAGER` (SQL printed, statement counts), Hibernate `@Cascade(PERSIST, MERGE, REMOVE)` in action (`persist`, detached `merge`, `remove`), typed HQL, projection, paging, `join fetch`, bulk update via `createMutationQuery` |
| `DialectsMain` | 21–28 | `Dialect` basics (`getSequenceSupport()`, DDL type names from the `DdlTypeRegistry`), and the 7 dialects wmstdappdbimpl names (HANA / SQL Server via their 6.x replacements): `openQuote()/closeQuote()`, superclass, `@Deprecated`, assumed DB version |
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
| 11 | `mapping.Table` | `MappingModelMain.mappingModel` (`getColumns/getUniqueKeys/getForeignKeys/getPrimaryKey`) |
| 12 | `mapping.Column` | `MappingModelMain.mappingModel` (`getSqlType()`, `getSqlTypeCode()`, `getSqlType(Metadata)`, length/precision/scale) |
| 13 | `mapping.Value` | `MappingModelMain.mappingModel` (`isSimpleValue()`) |
| 14 | `mapping.SimpleValue` | `MappingModelMain.mappingModel` (`getIdentifierGeneratorStrategy/Parameters`) |
| 15 | `mapping.PrimaryKey` | `MappingModelMain.mappingModel` |
| 16 | `mapping.UniqueKey` | `MappingModelMain.mappingModel` |
| 17 | `mapping.ForeignKey` | `MappingModelMain.mappingModel` (typed `getReferencedColumns()`), `RevengStrategyMain` (`isOneToOne`) |
| 18 | `type.StandardBasicTypes` | `MappingModelMain.basicTypes`, `RevengSupport.revengXml` |
| 19 | `jakarta.persistence.FetchType` | `EntityHqlMain.fetchTypes`, `entities/*` |
| 20 | `annotations.CascadeType` | `EntityHqlMain.cascadePersist/cascadeMerge/cascadeRemove`, `entities/Department` |
| 21 | `dialect.Dialect` | `DialectsMain` |
| 22–28 | `DB2Dialect`, `HANACloudColumnStoreDialect` (→ `HANADialect`), `HSQLDialect`, `MySQLDialect`, `OracleDialect`, `PostgreSQLDialect`, `SQLServer2012Dialect` (→ `SQLServerDialect`) | `DialectsMain` |
| 29 | `SessionFactory` | `NativeSqlSessionMain.createSessionFactory`, `EntityHqlMain` |
| 30 | `Session` | `NativeSqlSessionMain.queryApi/jdbcConnectionAccess` (`doWork`, `doReturningWork`) |
| 31 | `internal.SessionImpl` | `NativeSqlSessionMain.jdbcConnectionAccess` (`getJdbcCoordinator()...getPhysicalConnection()` + `CallableStatement`; `connection()` was removed in 6) |
| 32 | `Transaction` | `NativeSqlSessionMain.transactions`, `EntityHqlMain` |
| 33 | `query.Query` | `NativeSqlSessionMain.queryApi/scrollableResults`, `EntityHqlMain.hql` |
| 34 | `ScrollableResults` | `NativeSqlSessionMain.scrollableResults` |
| 35 | `internal.AbstractScrollableResults` | `NativeSqlSessionMain.scrollableResults` (`getResultSet()` is gone in 6; replacement: `doWork` + `ResultSetMetaData`) |
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
| 58 | `hbm2x.ant.ConfigurationTask` | `ExporterMain` (note only — not in Tools 5.6.15 or 6.6) |

## Changed in this version (5.6.15 → 6.6.58)

Every row still runs; this is what had to change. `run-output.txt` holds the full output of `./gradlew run` on this
branch (timings and the project path normalised), so `git diff main..hibernate-6 -- run-output.txt` shows the
behaviour changes, and `git diff main..hibernate-6 -- src` the code changes.

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

## Things the runs show (6.6.58 behaviour, compared with 5.6.15)

- **Exceptions:** native `Query.list()` on bad SQL now throws the `SQLGrammarException` **directly** (5.x wrapped
  it in a plain `jakarta.persistence.PersistenceException`). `catch (PersistenceException)` still catches it,
  `catch (HibernateException)` now does too, and `getCause()` is the JDBC `SQLException`.
- **Native result types:** `COUNT(*)` is a `Long` (5.x: `BigInteger`); other HSQLDB types are unchanged.
- **Reverse-engineered columns:** the no-arg `Column.getSqlType()` is still `null` and `getSqlTypeCode()` holds
  the JDBC type; `getSqlType(Metadata)` gives the resolved SQL type. Unset length/precision/scale are `null`
  (5.x: 255/19/2). `EMPLOYEE_DETAIL.BIO` (mapped to `text` by `hibernate.reveng.xml`) now resolves to
  `varchar(1000)` (5.x: `longvarchar`), and a `CLOB` to `clob` (5.x: `clob(255)`). Reverse-engineered basic values
  are `BasicValue`s (5.x: `SimpleValue`).
- **Cascades:** `@Cascade(PERSIST)` cascades at `persist()` time; `MERGE` cascades a detached parent's new
  children on `merge()`. 5.x's `SAVE_UPDATE` (deprecated in 6, removed in 7) also cascaded for `persist()` at flush.
- **`FetchType.EAGER` to-one:** unchanged — HQL loads it with a second `SELECT`, `session.get()` with a join.
  SQL aliases are now short (`e1_0`), and paging renders `fetch first ? rows only`.
- **Scrolling:** `query.scroll()` without a `ScrollMode` throws `AssertionFailure` when JDBC metadata access is
  off, because scrollable result-set support is no longer detected; `FORWARD_ONLY` works.
- **Dialects:** each no-arg dialect assumes the oldest DB version it supports (shown in `DialectsMain`).
  Setting `hibernate.dialect` explicitly logs `HHH90000025`, but it is still required here because JDBC
  metadata access is off.
- **Tools needs explicit cleanup:** `createMetadata()` still builds its own service registry that nobody
  closes. Destroy it through `MetadataImpl.getBootstrapContext()` (as wmstdappdbimpl does).
- **`hibernate.reveng.xml`:** Tools 6.6 still does not bundle the reverse-engineering DTD, so a DOCTYPE line
  makes the JDK parser try to download it. `RevengSupport.revengXml()` leaves it out.
- **`MySQLMetaDataDialect.getSuggestedPrimaryKeyStrategyName`** runs MySQL-only `show table status`. On any
  other DB it now fails with a plain `RuntimeException` wrapping the `SQLException` (5.x: a typed
  `SQLGrammarException` with `getSQL()`).
- **Exporter:** `POJOClass.getQualifiedDeclarationName()` repeats the package (`com.demo.hr.com.demo.hr.X`), and
  a `DATE` column becomes `java.sql.Date` in generated code (5.x: `java.util.Date`).
- **Logging:** Tools 6 logs every reverse-engineered table at INFO through java.util.logging; `Out` keeps only its
  warnings (e.g. "Binding column twice should not happen", logged for `EMPLOYEE.DEPT_ID` and `EMPLOYEE_DETAIL.EMP_ID`,
  which are both a property column and a foreign-key column; reverse engineering still completes normally).
