# hibernate-5-concepts

Runnable examples of **every Hibernate class/concept used by `wmstdappdbimpl`**, ported directly from the
versions it uses today (`hibernate-core-jakarta` 5.6.15.Final + `hibernate-tools` 5.6.15.Final) to
`org.hibernate.orm:hibernate-core` **7.3.13.Final** and `org.hibernate.tool:hibernate-tools-orm` **7.3.13.Final**
(branch `hibernate-5-to-7`, a direct 5 → 7 jump; see [`plans/5to7.md`](plans/5to7.md)).
Every row of the 5.6 inventory is still shown. Where an API was renamed or removed, the Main uses the supported 7.3
replacement and prints a `NOTE: CHANGED in 7: old -> new` / `NOTE: REMOVED in 7: ...` line.
The row numbers below (and in every `=== [Row N] ... ===` header the Mains print) are the rows of
[`../hibernate-classes-and-concepts.md`](../hibernate-classes-and-concepts.md).

The code is plain Hibernate. It does not copy wmstdappdbimpl's classes. Where it helps, a comment names the
wmstdappdbimpl class that uses the same API.

## Run

Requirements: JDK 21 (Hibernate 7 needs Java 17+). Nothing else: the database is in-memory HSQLDB, and dependencies come from
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
| `MappingModelMain` | 8–18 | `Identifier`, `QualifiedNameImpl`; walks every reverse-engineered `PersistentClass` → `Table` → columns (`Column`/`Value`/`SimpleValue`, id-generator strategy + properties via Tools' `EnhancedValue`), `PrimaryKey`/`UniqueKey`/`ForeignKey` via the 7 collection getters; `StandardBasicTypes` names |
| `EntityHqlMain` | 19–20 (+29–34) | Two `@Entity`s + HQL: `FetchType.LAZY` vs `EAGER` (SQL printed, statement counts), JPA `cascade = {PERSIST, MERGE, REMOVE}` in action (`persist`, detached `merge`, `remove`), the deprecated Hibernate `CascadeType` enum (by reflection), typed HQL, projection, paging, `join fetch`, `createMutationQuery` bulk update |
| `DialectsMain` | 21–28 | `Dialect` basics (`getSequenceSupport()`, DDL type names via `DdlTypeRegistry`), and the 7 dialects wmstdappdbimpl names (7 equivalents): `openQuote()/closeQuote()`, superclass, `@Deprecated`, assumed version |
| `NativeSqlSessionMain` | 29–37 | Entity-less `SessionFactory` (like `DBConnectionUtils`); `Session`, `Transaction` commit/rollback, typed native `Query` (params, paging, `list`, `uniqueResult`, `createNativeMutationQuery`, `scroll`), `AbstractScrollableResults.getResultSet()` gone → `doReturningWork` + `ResultSetMetaData`, `SessionImpl` JDBC connection vs `doWork`, exception hierarchy |
| `RevengStrategyMain` | 39–47, 53–54 | `TableIdentifier.create`, `TableNameQualifier`, `NameConverter`, default/delegating strategies, `RevengSettings`, `TableFilter`, `OverrideRepository` + `hibernate.reveng.xml`, `MetadataDescriptorFactory.createReverseEngineeringDescriptor(..).createMetadata()` |
| `MetaDataDialectMain` | 38, 48–52 | `RevengDialectFactory` selection, `JDBCMetaDataDialect` calls (tables, columns, PKs, indexes, exported keys), a custom `AbstractMetaDataDialect` subclass, `MySQLMetaDataDialect`, `ResultSetIterator`, `SQLExceptionConverter` |
| `ExporterMain` | 55–58 | `GenericExporter` (configured through `ExporterConstants` properties) + FreeMarker template per `POJOClass`, `resolveFilename(POJOClass)` override (and why wmstdappdbimpl's `resolveFilename(String)` is never called), `ConfigurationTask` note |
| `RunAllMain` | all | Runs the above in order, prints OK/FAILED + time per Main |

## Row → where it is demonstrated

Class names are the 5.6 ones (the inventory); the 7.3 replacement is given where it differs.

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
| 11 | `mapping.Table` | `MappingModelMain.mappingModel` (`getColumns/getUniqueKeys().values()/getForeignKeyCollection/getPrimaryKey`) |
| 12 | `mapping.Column` | `MappingModelMain.mappingModel` (`getSqlType()`, `getSqlTypeCode()`, `getSqlType(Metadata)`, nullable length/precision/scale) |
| 13 | `mapping.Value` | `MappingModelMain.mappingModel` (`isSimpleValue()`) |
| 14 | `mapping.SimpleValue` | `MappingModelMain.mappingModel` (`getTypeName`; generator strategy/properties now from Tools' `EnhancedValue` on `PersistentClass.getIdentifier()`) |
| 15 | `mapping.PrimaryKey` | `MappingModelMain.mappingModel` |
| 16 | `mapping.UniqueKey` | `MappingModelMain.mappingModel` |
| 17 | `mapping.ForeignKey` | `MappingModelMain.mappingModel` (typed `getReferencedColumns()`), `RevengStrategyMain` (`isOneToOne`) |
| 18 | `type.StandardBasicTypes` | `MappingModelMain.basicTypes`, `RevengSupport.revengXml` |
| 19 | `jakarta.persistence.FetchType` | `EntityHqlMain.fetchTypes`, `entities/*` |
| 20 | `annotations.CascadeType` → JPA `CascadeType` | `EntityHqlMain.cascadePersist/cascadeMerge/cascadeRemove` (+ `hibernateCascadeTypeEnum`, reflection), `entities/Department` |
| 21 | `dialect.Dialect` | `DialectsMain` |
| 22–28 | `DB2Dialect`, `HANACloudColumnStoreDialect` → `HANADialect`, `HSQLDialect`, `MySQLDialect`, `OracleDialect`, `PostgreSQLDialect`, `SQLServer2012Dialect` → `SQLServerDialect` | `DialectsMain` |
| 29 | `SessionFactory` | `NativeSqlSessionMain.createSessionFactory`, `EntityHqlMain` |
| 30 | `Session` | `NativeSqlSessionMain.queryApi/jdbcConnectionAccess` (`doWork`, `doReturningWork`) |
| 31 | `internal.SessionImpl` | `NativeSqlSessionMain.jdbcConnectionAccess` (`getJdbcCoordinator().getLogicalConnection().getPhysicalConnection()` + `CallableStatement`) |
| 32 | `Transaction` | `NativeSqlSessionMain.transactions`, `EntityHqlMain` |
| 33 | `query.Query` | `NativeSqlSessionMain.queryApi/scrollableResults`, `EntityHqlMain.hql` |
| 34 | `ScrollableResults` | `NativeSqlSessionMain.scrollableResults` |
| 35 | `internal.AbstractScrollableResults` → `internal.scrollable.AbstractScrollableResults` | `NativeSqlSessionMain.scrollableResults` (reflection shows `getResultSet()` is gone; `doReturningWork` + `ResultSetMetaData`) |
| 36 | `jakarta.persistence.PersistenceException` | `NativeSqlSessionMain.exceptions` |
| 37 | `HibernateException` | `NativeSqlSessionMain.exceptions` |
| 38 | `exception.spi.SQLExceptionConverter` | `MetaDataDialectMain.resultSetIteratorAndConverter/mySqlDialect`, `support/DemoMetaDataDialect` |
| 39 | `tool.api.metadata.MetadataDescriptor` | `RevengStrategyMain.descriptorAndMetadata`, `ExporterMain` |
| 40 | `tool.internal.metadata.JdbcMetadataDescriptor` → `MetadataDescriptorFactory` / `RevengMetadataDescriptor` | `RevengStrategyMain.descriptorAndMetadata`, `support/RevengSupport` |
| 41 | `cfg.reveng.ReverseEngineeringStrategy` → `tool.api.reveng.RevengStrategy` | `RevengStrategyMain` (all sections) |
| 42 | `DefaultReverseEngineeringStrategy` → `DefaultStrategy` | `RevengStrategyMain.defaultStrategyAndSettings` |
| 43 | `DelegatingReverseEngineeringStrategy` → `DelegatingStrategy` | `support/DemoRevengStrategy`, `RevengStrategyMain.delegatingStrategy` |
| 44 | `ReverseEngineeringSettings` → `RevengSettings` | `RevengStrategyMain.defaultStrategyAndSettings`, `support/RevengSupport.strategy` |
| 45 | `OverrideRepository` (`tool.internal.reveng.strategy`) | `RevengStrategyMain.overridesAndFilters`, `support/RevengSupport.overrideRepository` |
| 46 | `TableFilter` (`tool.internal.reveng.strategy`) | `RevengStrategyMain.overridesAndFilters` |
| 47 | `TableIdentifier` (`tool.api.reveng`, `create(..)`) | `RevengStrategyMain.valueObjectsAndUtilities` |
| 48 | `MetaDataDialect` → `RevengDialect` / `RevengDialectFactory` | `MetaDataDialectMain.selection` (+ `hibernatetool.metadatadialect`) |
| 49 | `AbstractMetaDataDialect` | `MetaDataDialectMain.jdbcMetaDataDialect/customDialect`, `support/DemoMetaDataDialect` |
| 50 | `JDBCMetaDataDialect` | `MetaDataDialectMain.jdbcMetaDataDialect` |
| 51 | `MySQLMetaDataDialect` | `MetaDataDialectMain.mySqlDialect` |
| 52 | `ResultSetIterator` | `MetaDataDialectMain.resultSetIteratorAndConverter`, `support/DemoMetaDataDialect` |
| 53 | `ReverseEngineeringStrategyUtil` → `tool.internal.util.NameConverter` | `RevengStrategyMain.valueObjectsAndUtilities`, `support/DemoRevengStrategy` |
| 54 | `tool.util.TableNameQualifier` → `tool.internal.util.TableNameQualifier` | `RevengStrategyMain.valueObjectsAndUtilities` |
| 55 | `hbm2x.AbstractExporter` → `tool.internal.export.common.AbstractExporter` | `ExporterMain` |
| 56 | `hbm2x.GenericExporter` → `tool.internal.export.common.GenericExporter` | `ExporterMain.SummaryExporter` |
| 57 | `hbm2x.pojo.POJOClass` → `tool.internal.export.java.POJOClass` | `ExporterMain.SummaryExporter.resolveFilename(POJOClass)`, `templates/entity-summary.ftl` |
| 58 | `hbm2x.ant.ConfigurationTask` | `ExporterMain` (note only — not in Tools 5.6.15 nor tools-orm 7.3.13) |

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
| `EMPLOYEE_COUNT(IN, OUT)` | procedure called through the `SessionImpl` JDBC connection / `doWork` |

`EntityHqlMain` uses its own database (`jdbc:hsqldb:mem:entity_demo`), created by `hbm2ddl` from the two entities.

## Changed in this version (5.6.15 → 7.3.13)

"Since" is the version where the change first happened. A direct 5 → 7 jump hits all of them at once: most of the
5.6 APIs below were not deprecated in 5.6, so there was no warning trail.

| Row | 5.6.15 | 7.3.13 | Since |
|---|---|---|---|
| build | `org.hibernate:hibernate-core-jakarta` + `org.hibernate:hibernate-tools` (excl. javax core, Ant, JDT) | `org.hibernate.orm:hibernate-core` + `org.hibernate.tool:hibernate-tools-orm` (excl. Ant, google-java-format) | 6.0 / Tools 6 |
| setting | `hibernate.temp.use_jdbc_metadata_defaults=false` | `hibernate.boot.allow_jdbc_metadata_access=false` | 6.5 |
| 11 | `Table.getColumnIterator()` | `getColumns()` | 6.0 |
| 12 | `Column.getSqlType(Dialect, Mapping)`; `int` length/precision/scale (defaults 255/19/2) | `getSqlType(Metadata)`; `Long`/`Integer`/`Integer`, `null` when unset | 6.0 |
| 14 | `SimpleValue.getIdentifierGeneratorStrategy()/getIdentifierGeneratorProperties()` | removed from core; Tools' `EnhancedValue` on `PersistentClass.getIdentifier()` | 7.0 |
| 15 | `Constraint.getColumnIterator()` | `getColumns()` | 6.0 |
| 16 | `Table.getUniqueKeyIterator()` | `getUniqueKeys().values()` | 7.0 (deprecated 6.0) |
| 17 | `Table.getForeignKeyIterator()`, raw `getReferencedColumns()` | `getForeignKeyCollection()`, `List<Column>` | 7.0 |
| 18 | `StandardBasicTypes.X` is a `Type` (`StringType`) | `BasicTypeReference<T>`; `getName()` unchanged | 6.0 |
| 19 | `session.get(Class, id)` | `session.find(Class, id)` (`get` deprecated for removal) | 7.0 |
| 20 | `@org.hibernate.annotations.Cascade({SAVE_UPDATE, REMOVE})`, `saveOrUpdate`, `delete` | JPA `@OneToMany(cascade = {PERSIST, MERGE, REMOVE})`, `persist` / `merge` / `remove`; the Hibernate enum is deprecated for removal and only inspected by reflection | 7.0 |
| 21 | `Dialect.supportsSequences()/getSequenceNextValString()` | `getSequenceSupport().*` | 6.0 |
| 21 | `Dialect.getTypeName(int, long, int, int)` | `DdlTypeRegistry.getTypeName(int, Size, Type)` from a connection-less `Metadata` | 6.0 |
| 23 | `HANACloudColumnStoreDialect` | `HANADialect` | 7.0 (deprecated 6.0) |
| 28 | `SQLServer2012Dialect` | `SQLServerDialect` | 7.0 (deprecated 6.0) |
| 32/33 | untyped `createQuery(String)` for HQL update | `createMutationQuery(String)` | 6.0 |
| 33 | untyped `createNativeQuery(String)`, raw `Query` | `createNativeQuery(sql, Object[].class / Object.class)`, `createNativeMutationQuery(sql)` for DML | 6.0 |
| 34 | raw `ScrollableResults`, `getRowNumber()` (0-based), `scroll()` | `ScrollableResults<Object[]>` (`AutoCloseable`), `getPosition()` (1-based), `scroll(ScrollMode.FORWARD_ONLY)` | 6.0 / 7.0 |
| 35 | reflective `org.hibernate.internal.AbstractScrollableResults.getResultSet()` | gone (class now in `internal.scrollable`); `session.doReturningWork(..)` + `ResultSetMetaData` | 6.0 / 7.0 |
| 31 | `SessionImpl.connection()` | `getJdbcCoordinator().getLogicalConnection().getPhysicalConnection()` (SPI; public way: `doWork`/`doReturningWork`) | 6.0 |
| 10 | `PersistentClass.getPropertyIterator()` | `getProperties()` | 6.0 |
| 41–47, 53–54 | `org.hibernate.cfg.reveng.*`, `org.hibernate.tool.util.TableNameQualifier` | `org.hibernate.tool.api.reveng.*` (`RevengStrategy`, `RevengSettings`, `TableIdentifier.create`), `org.hibernate.tool.internal.reveng.strategy.*` (`DefaultStrategy`, `DelegatingStrategy`, `OverrideRepository`, `TableFilter`), `org.hibernate.tool.internal.util.{NameConverter, TableNameQualifier}` | Tools 6 |
| 45 | `new SchemaSelection(catalog, schema)`; `addInputStream` closed the stream | `RevengStrategy.SchemaSelection` is an interface (`RevengSupport.schemaSelection`); caller closes the stream | Tools 6 |
| 47 | `new Table("X")` sets the name | sets the *contributor*; `new Table(contributor, "X")` | 6.0 |
| 39/40 | `new JdbcMetadataDescriptor(strategy, props, true)` | `MetadataDescriptorFactory.createReverseEngineeringDescriptor(strategy, props)` + `MetadataConstants.PREFER_BASIC_COMPOSITE_IDS` = `Boolean.TRUE` | Tools 6 |
| 48 | `MetaDataDialectFactory`, `MetaDataDialect.configure(ReverseEngineeringRuntimeInfo)` | `RevengDialectFactory`, `RevengDialect.configure(ConnectionProvider)` | Tools 6 |
| 49–52 | `cfg.reveng.dialect.*`, `getSQLExceptionConverter()`, `ResultSetIterator(.., converter)` | `tool.internal.reveng.dialect.*`; no converter (custom dialect uses core `SqlExceptionHelper`); `ResultSetIterator(stmt, rs)` | Tools 6 |
| 55–57 | `tool.hbm2x.{AbstractExporter, GenericExporter, pojo.POJOClass}` + setters | `tool.internal.export.{common.AbstractExporter, common.GenericExporter, java.POJOClass}`; `getProperties().put(ExporterConstants.X, value)` | Tools 6 |

## Things the runs show (7.3.13 behaviour)

`run-output.txt` is the RunAllMain output on 7.3.13 (timings, timestamps and the project path normalised). Compared
with the 5.6.15 output (`git show HEAD~4:run-output.txt`, the baseline commit):

- **Exceptions:** `Query.list()` on bad SQL now throws the `SQLGrammarException` itself (a `HibernateException`
  *and* a `PersistenceException`); 5.6 wrapped it in a plain `PersistenceException`. Code that unwraps
  `e.getCause()` breaks. The message also changed ("could not prepare statement" → "Could not prepare statement [...]").
- **SQLState 42501** given to the `SQLExceptionConverter` as a plain `SQLException` now becomes the new
  `AuthException` (7.0); a typed `SQLSyntaxErrorException` from the driver is still an `SQLGrammarException`.
- **Native query result types:** `COUNT(*)` is `Long` (was `BigInteger`); a `DATE` column is `java.time.LocalDate`
  (was `java.sql.Date`). `ScrollableResults.getPosition()` is 1-based, so the scroll rows print `row 1..5` (was `0..4`).
- **Scrolling:** a no-arg `scroll()` throws `AssertionFailure: scrollable result sets are not enabled` while
  `hibernate.boot.allow_jdbc_metadata_access=false`; the demo passes `ScrollMode.FORWARD_ONLY`.
- **Reverse-engineered columns:** unset length/precision/scale are `null` (5.6: 255/19/2). `getSqlType(Metadata)` of
  `NOTES` is `clob` (5.6: `clob(255)`) and of `BIO` (`text` → LONGVARCHAR) is `varchar(1000)` (5.6: `longvarchar`).
- **Reverse-engineered values** are Tools' `EnhancedBasicValue` (5.6: `SimpleValue`); the id generator lives there.
  `EMPLOYEE_DETAIL`'s id now reports `idGenerator=foreign` — 5.6 printed `assigned`, the default of the column's
  `OneToOne` value, which in 7 no longer has generator getters.
- **Tools 7 logging:** Tools logs through `java.util.logging`, at INFO for every table it binds (with timestamps, on
  stderr). `support/Out` keeps WARNING and above and prints them to stdout like slf4j-simple, so the remaining
  `WARNING BinderUtils - Binding column twice should not happen` lines (PK/FK columns of `EMPLOYEE` and
  `EMPLOYEE_DETAIL`) are Tools 7 warnings; 5.6 did not log them.
- **`HHH90000025` warning:** 7 reminds that `hibernate.dialect` need not be set. It is kept on purpose: with
  `hibernate.boot.allow_jdbc_metadata_access=false` the dialect is required, and wmstdappdbimpl sets it too.
- **Cascades:** `persist()` cascades at call time with JPA `PERSIST`. The 5.6 demo of `SAVE_UPDATE` cascading at
  flush for a `persist()`-ed entity has no 7 equivalent; the row now shows `merge()` of a detached department
  cascading the new employee (`MERGE`). The Hibernate `CascadeType` enum is `@Deprecated(since="7", forRemoval=true)`
  and has lost `SAVE_UPDATE` and `DELETE`.
- **SQL:** 6+ SQL aliases (`e1_0`), insert column order, `fetch first ? rows only` paging, `left join` instead of
  `left outer join`; `FetchType.EAGER` still costs a second `SELECT` with HQL and one outer join with `find()`.
- **Dialects:** no-arg dialects assume their minimum version (DB2 11.1, HANA 2.0.50, HSQL 2.6.1, MySQL 8.0,
  Oracle 19, PostgreSQL 13, SQL Server 12); all extend `Dialect` (SQL Server via `AbstractTransactSQLDialect`) and
  none is deprecated. 5.6's `OracleDialect`/`PostgreSQLDialect` were deprecated aliases of old versions.
- **Metadata dialects:** `HSQLDialect` maps to `HSQLMetaDataDialect` as before. `MySQLMetaDataDialect` on HSQLDB now
  fails with a plain `RuntimeException` wrapping the `SQLSyntaxErrorException` (5.6: typed `SQLGrammarException`).
- **Exporter:** `POJOClass.getQualifiedDeclarationName()` returns `com.demo.hr.com.demo.hr.XEntity` (Tools 7
  fixed an inverted condition and now prefixes the package of an already qualified name; 5.6 returned
  `com.demo.hr.XEntity`). Use `getPackageName()` + `getDeclarationName()`. The template output is otherwise the same.
- **Unchanged:** `Configuration` bootstrap (not deprecated in 7), registry destroy through
  `MetadataImpl.getBootstrapContext()` (Tools still never closes its registry), `hibernate.reveng.xml` format and the
  missing DTD (keep the DOCTYPE out), `StandardBasicTypes` names, `TableFilter`/strategy behaviour, the
  `getDetectOptimsticLock` typo. `Namespace.Name` is a record now (`Name[...]` instead of `Name{...}`).
