# hibernate-5-concepts

Runnable examples of **every Hibernate class/concept used by `wmstdappdbimpl`**, on the
versions it uses today: `hibernate-core-jakarta` **5.6.15.Final** and `hibernate-tools` **5.6.15.Final**.
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
| `MappingModelMain` | 8–18 | `Identifier`, `QualifiedNameImpl`; walks every reverse-engineered `PersistentClass` → `Table` → columns (`Column`/`Value`/`SimpleValue` incl. id-generator strategy + properties), `PrimaryKey`/`UniqueKey`/`ForeignKey` via the 5.x iterators; `StandardBasicTypes` names |
| `EntityHqlMain` | 19–20 (+29–34) | Two `@Entity`s + HQL: `FetchType.LAZY` vs `EAGER` (SQL printed, statement counts), Hibernate `@Cascade(SAVE_UPDATE, REMOVE)` in action, typed HQL, projection, paging, `join fetch`, bulk update |
| `DialectsMain` | 21–28 | `Dialect` basics, and the 7 dialects wmstdappdbimpl names: `openQuote()/closeQuote()`, superclass, `@Deprecated` |
| `NativeSqlSessionMain` | 29–37 | Entity-less `SessionFactory` (like `DBConnectionUtils`); `Session`, `Transaction` commit/rollback, native `Query` (params, paging, `list`, `uniqueResult`, `executeUpdate`, `scroll`), reflective `AbstractScrollableResults.getResultSet()`, `SessionImpl.connection()` vs `doWork`, exception hierarchy |
| `RevengStrategyMain` | 39–47, 53–54 | `TableIdentifier`, `TableNameQualifier`, `ReverseEngineeringStrategyUtil`, default/delegating strategies, `ReverseEngineeringSettings`, `TableFilter`, `OverrideRepository` + `hibernate.reveng.xml`, `JdbcMetadataDescriptor.createMetadata()` |
| `MetaDataDialectMain` | 38, 48–52 | Metadata-dialect selection, `JDBCMetaDataDialect` calls (tables, columns, PKs, indexes, exported keys), a custom `AbstractMetaDataDialect` subclass, `MySQLMetaDataDialect`, `ResultSetIterator`, `SQLExceptionConverter` |
| `ExporterMain` | 55–58 | `GenericExporter` + FreeMarker template per `POJOClass`, `resolveFilename(POJOClass)` override (and why wmstdappdbimpl's `resolveFilename(String)` is never called), `ConfigurationTask` note |
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
| 11 | `mapping.Table` | `MappingModelMain.mappingModel` (`getColumnIterator/getUniqueKeyIterator/getForeignKeyIterator/getPrimaryKey`) |
| 12 | `mapping.Column` | `MappingModelMain.mappingModel` (`getSqlType()`, `getSqlTypeCode()`, `getSqlType(Dialect, Mapping)`, length/precision/scale) |
| 13 | `mapping.Value` | `MappingModelMain.mappingModel` (`isSimpleValue()`) |
| 14 | `mapping.SimpleValue` | `MappingModelMain.mappingModel` (`getIdentifierGeneratorStrategy/Properties`) |
| 15 | `mapping.PrimaryKey` | `MappingModelMain.mappingModel` |
| 16 | `mapping.UniqueKey` | `MappingModelMain.mappingModel` |
| 17 | `mapping.ForeignKey` | `MappingModelMain.mappingModel` (raw `getReferencedColumns()`), `RevengStrategyMain` (`isOneToOne`) |
| 18 | `type.StandardBasicTypes` | `MappingModelMain.basicTypes`, `RevengSupport.revengXml` |
| 19 | `jakarta.persistence.FetchType` | `EntityHqlMain.fetchTypes`, `entities/*` |
| 20 | `annotations.CascadeType` | `EntityHqlMain.cascadeSaveUpdate/cascadeOnPersistFlush/cascadeRemove`, `entities/Department` |
| 21 | `dialect.Dialect` | `DialectsMain` |
| 22–28 | `DB2Dialect`, `HANACloudColumnStoreDialect`, `HSQLDialect`, `MySQLDialect`, `OracleDialect`, `PostgreSQLDialect`, `SQLServer2012Dialect` | `DialectsMain` |
| 29 | `SessionFactory` | `NativeSqlSessionMain.createSessionFactory`, `EntityHqlMain` |
| 30 | `Session` | `NativeSqlSessionMain.queryApi/jdbcConnectionAccess` (`doWork`, `doReturningWork`) |
| 31 | `internal.SessionImpl` | `NativeSqlSessionMain.jdbcConnectionAccess` (`connection()` + `CallableStatement`) |
| 32 | `Transaction` | `NativeSqlSessionMain.transactions`, `EntityHqlMain` |
| 33 | `query.Query` | `NativeSqlSessionMain.queryApi/scrollableResults`, `EntityHqlMain.hql` |
| 34 | `ScrollableResults` | `NativeSqlSessionMain.scrollableResults` |
| 35 | `internal.AbstractScrollableResults` | `NativeSqlSessionMain.getResultSet` (reflection, same as `QueryProcedureTester`) |
| 36 | `jakarta.persistence.PersistenceException` | `NativeSqlSessionMain.exceptions` |
| 37 | `HibernateException` | `NativeSqlSessionMain.exceptions` |
| 38 | `exception.spi.SQLExceptionConverter` | `MetaDataDialectMain.resultSetIteratorAndConverter/mySqlDialect`, `support/DemoMetaDataDialect` |
| 39 | `tool.api.metadata.MetadataDescriptor` | `RevengStrategyMain.descriptorAndMetadata`, `ExporterMain` |
| 40 | `tool.internal.metadata.JdbcMetadataDescriptor` | `RevengStrategyMain.descriptorAndMetadata`, `support/RevengSupport` |
| 41 | `cfg.reveng.ReverseEngineeringStrategy` | `RevengStrategyMain` (all sections) |
| 42 | `DefaultReverseEngineeringStrategy` | `RevengStrategyMain.defaultStrategyAndSettings` |
| 43 | `DelegatingReverseEngineeringStrategy` | `support/DemoRevengStrategy`, `RevengStrategyMain.delegatingStrategy` |
| 44 | `ReverseEngineeringSettings` | `RevengStrategyMain.defaultStrategyAndSettings`, `support/RevengSupport.strategy` |
| 45 | `OverrideRepository` | `RevengStrategyMain.overridesAndFilters`, `support/RevengSupport.overrideRepository` |
| 46 | `TableFilter` | `RevengStrategyMain.overridesAndFilters` |
| 47 | `TableIdentifier` | `RevengStrategyMain.valueObjectsAndUtilities` |
| 48 | `MetaDataDialect` | `MetaDataDialectMain.selection` (+ `hibernatetool.metadatadialect`) |
| 49 | `AbstractMetaDataDialect` | `MetaDataDialectMain.jdbcMetaDataDialect/customDialect`, `support/DemoMetaDataDialect` |
| 50 | `JDBCMetaDataDialect` | `MetaDataDialectMain.jdbcMetaDataDialect` |
| 51 | `MySQLMetaDataDialect` | `MetaDataDialectMain.mySqlDialect` |
| 52 | `ResultSetIterator` | `MetaDataDialectMain.resultSetIteratorAndConverter`, `support/DemoMetaDataDialect` |
| 53 | `ReverseEngineeringStrategyUtil` | `RevengStrategyMain.valueObjectsAndUtilities`, `support/DemoRevengStrategy` |
| 54 | `tool.util.TableNameQualifier` | `RevengStrategyMain.valueObjectsAndUtilities` |
| 55 | `hbm2x.AbstractExporter` | `ExporterMain` |
| 56 | `hbm2x.GenericExporter` | `ExporterMain.SummaryExporter` |
| 57 | `hbm2x.pojo.POJOClass` | `ExporterMain.SummaryExporter.resolveFilename(POJOClass)`, `templates/entity-summary.ftl` |
| 58 | `hbm2x.ant.ConfigurationTask` | `ExporterMain` (note only — not in Tools 5.6.15) |

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

## Things the runs show (5.6.15 behaviour worth knowing before 6/7)

- **Exceptions:** `Query.list()` on bad SQL throws a plain `jakarta.persistence.PersistenceException` that
  *wraps* the Hibernate `SQLGrammarException`. So `catch (HibernateException)` does not catch it.
  `session.doWork(...)` throws the `HibernateException` subclass directly.
- **Reverse-engineered columns:** the no-arg `Column.getSqlType()` is `null`. `getSqlTypeCode()` holds the
  JDBC type, and `getSqlType(Dialect, Mapping)` gives the resolved SQL type. When length/precision/scale
  are not set they read 255/19/2. They are `int` in 5.x and become nullable `Long`/`Integer` in 6.
- **`@Cascade(SAVE_UPDATE)`:** with the native `Configuration` bootstrap it also cascades for a
  `persist()`-ed entity at flush, not only for `saveOrUpdate()`. It is removed in 7.
- **`FetchType.EAGER` to-one:** HQL loads it with a second `SELECT`. `session.get()` uses an outer join.
- **Tools needs explicit cleanup:** `JdbcMetadataDescriptor.createMetadata()` builds its own service registry,
  and nobody closes it. Destroy it through `MetadataImpl.getBootstrapContext()` (as wmstdappdbimpl does).
- **`hibernate.reveng.xml`:** Tools 5.6 does not bundle the reverse-engineering DTD, so a DOCTYPE line
  makes the JDK parser try to download it. `RevengSupport.revengXml()` leaves it out.
- **`MySQLMetaDataDialect.getSuggestedPrimaryKeyStrategyName`** runs MySQL-only `show table status`. On
  any other DB it fails with an `SQLGrammarException` produced by the `SQLExceptionConverter`.
