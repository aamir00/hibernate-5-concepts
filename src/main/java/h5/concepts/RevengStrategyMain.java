package h5.concepts;

import java.sql.Types;
import java.util.Properties;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.metadata.MetadataConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.RevengSettings;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.hibernate.tool.internal.reveng.strategy.OverrideRepository;
import org.hibernate.tool.internal.reveng.strategy.TableFilter;
import org.hibernate.tool.internal.util.NameConverter;
import org.hibernate.tool.internal.util.TableNameQualifier;

import h5.concepts.support.DemoDatabase;
import h5.concepts.support.DemoMetaDataDialect;
import h5.concepts.support.DemoRevengStrategy;
import h5.concepts.support.Out;
import h5.concepts.support.RevengSupport;

/**
 * Rows 39–47, 53–54: Hibernate Tools reverse engineering — turning live DB tables into a boot
 * {@link Metadata}. The strategy chain is the same one wmstdappdbimpl's {@code ConfigurationBuilder} builds
 * (see {@link RevengSupport}); this Main walks through each piece, then runs the whole thing.
 * <p>
 * Tools 7 moved the 5.6 {@code org.hibernate.cfg.reveng.*} / {@code org.hibernate.tool.util.*} classes to
 * {@code org.hibernate.tool.api.reveng.*} and {@code org.hibernate.tool.internal.*}; each row notes old -> new.
 */
public class RevengStrategyMain {

    public static void main(String[] args) {
        Out.banner("RevengStrategyMain — TableIdentifier, strategies, settings, OverrideRepository, TableFilter, MetadataDescriptor");
        DemoDatabase.ensureCreated();
        valueObjectsAndUtilities();
        defaultStrategyAndSettings();
        overridesAndFilters();
        delegatingStrategy();
        descriptorAndMetadata();
    }

    static void valueObjectsAndUtilities() {
        Out.row("47", "TableIdentifier — catalog + schema + name key passed to strategy methods");
        TableIdentifier byName = TableIdentifier.create(null, null, "EMPLOYEE");
        TableIdentifier qualified = TableIdentifier.create(null, "PUBLIC", "EMPLOYEE");
        Out.kv("TableIdentifier.create(null, null, \"EMPLOYEE\")", byName);
        Out.kv("TableIdentifier.create(null, \"PUBLIC\", \"EMPLOYEE\")", qualified);
        Out.kv("byName.equals(qualified)", byName.equals(qualified));
        Table table = new Table("orm", "DEPARTMENT");
        table.setSchema("PUBLIC");
        Out.kv("TableIdentifier.create(Table)", TableIdentifier.create(table));
        Out.kv("new Table(\"DEPARTMENT\").getName()", new Table("DEPARTMENT").getName());
        Out.note("CHANGED in 7: org.hibernate.cfg.reveng.TableIdentifier -> org.hibernate.tool.api.reveng.TableIdentifier; "
            + "its constructors are private -> TableIdentifier.create(catalog, schema, name) / create(Table).");
        Out.note("CHANGED in 7: core's new Table(String) sets the *contributor* since 6.0, not the name (null above) "
            + "-> new Table(contributor, name).");

        Out.row("54", "TableNameQualifier.qualify(catalog, schema, table)");
        Out.kv("qualify(null, \"PUBLIC\", \"EMPLOYEE\")", TableNameQualifier.qualify(null, "PUBLIC", "EMPLOYEE"));
        Out.kv("qualify(\"HR\", \"PUBLIC\", \"EMPLOYEE\")", TableNameQualifier.qualify("HR", "PUBLIC", "EMPLOYEE"));
        Out.note("CHANGED in 7: org.hibernate.tool.util.TableNameQualifier -> org.hibernate.tool.internal.util.TableNameQualifier.");

        Out.row("53", "NameConverter (was ReverseEngineeringStrategyUtil) — naming helpers");
        Out.kv("toUpperCamelCase(\"EMPLOYEE_DETAIL\")", NameConverter.toUpperCamelCase("EMPLOYEE_DETAIL"));
        Out.kv("simplePluralize(\"employee\")", NameConverter.simplePluralize("employee"));
        Out.kv("simplePluralize(\"category\")", NameConverter.simplePluralize("category"));
        Out.kv("simplePluralize(\"address\")", NameConverter.simplePluralize("address"));
        Out.kv("isReservedJavaKeyword(\"class\")", NameConverter.isReservedJavaKeyword("class"));
        Out.note("CHANGED in 7: org.hibernate.cfg.reveng.ReverseEngineeringStrategyUtil -> "
            + "org.hibernate.tool.internal.util.NameConverter (same static methods).");
    }

    static void defaultStrategyAndSettings() {
        Out.row("41/42", "DefaultStrategy (was DefaultReverseEngineeringStrategy) — built-in naming and type mapping");
        DefaultStrategy strategy = new DefaultStrategy();
        TableIdentifier detail = TableIdentifier.create(null, "PUBLIC", "EMPLOYEE_DETAIL");
        Out.kv("tableToClassName(EMPLOYEE_DETAIL)", strategy.tableToClassName(detail));
        Out.kv("columnToPropertyName(.., \"FIRST_NAME\")", strategy.columnToPropertyName(detail, "FIRST_NAME"));
        Out.kv("columnToHibernateTypeName DECIMAL(10,2)",
            strategy.columnToHibernateTypeName(detail, "SALARY", Types.DECIMAL, 0, 10, 2, true, false));
        Out.kv("columnToHibernateTypeName VARCHAR(1000)",
            strategy.columnToHibernateTypeName(detail, "BIO", Types.VARCHAR, 1000, 0, 0, true, false));
        Out.kv("columnToHibernateTypeName INTEGER not-null",
            strategy.columnToHibernateTypeName(detail, "EMP_ID", Types.INTEGER, 0, 10, 0, false, false));
        Out.kv("excludeTable(EMPLOYEE_DETAIL)", strategy.excludeTable(detail));
        Out.kv("getSchemaSelections()", strategy.getSchemaSelections());
        Out.note("CHANGED in 7: org.hibernate.cfg.reveng.ReverseEngineeringStrategy -> org.hibernate.tool.api.reveng.RevengStrategy, "
            + "DefaultReverseEngineeringStrategy -> org.hibernate.tool.internal.reveng.strategy.DefaultStrategy (same logic).");

        Out.row("44", "RevengSettings (was ReverseEngineeringSettings) — package, detect many-to-many / one-to-one / optimistic lock");
        RevengSettings settings = new RevengSettings(strategy)
            .setDefaultPackageName(RevengSupport.PACKAGE)
            .setDetectManyToMany(true)
            .setDetectOneToOne(true)
            .setDetectOptimisticLock(true);
        strategy.setSettings(settings);
        Out.kv("getDefaultPackageName()", settings.getDefaultPackageName());
        Out.kv("getDetectManyToMany() / OneToOne()", settings.getDetectManyToMany() + " / " + settings.getDetectOneToOne());
        Out.kv("getDetectOptimsticLock() (sic)", settings.getDetectOptimsticLock());
        Out.kv("tableToClassName(EMPLOYEE_DETAIL) now", strategy.tableToClassName(detail));
        Out.note("wmstdappdbimpl sets the same settings object on both the default (innermost) strategy and the outer one.");
        Out.note("CHANGED in 7: org.hibernate.cfg.reveng.ReverseEngineeringSettings -> org.hibernate.tool.api.reveng.RevengSettings "
            + "(same fluent setters; the getDetectOptimsticLock typo is still there).");
    }

    static void overridesAndFilters() {
        Out.row("46", "TableFilter — include/exclude by catalog/schema/name pattern");
        TableFilter filter = new TableFilter();
        filter.setMatchSchema("PUBLIC");
        filter.setMatchName("AUDIT_.*");
        filter.setExclude(true);
        Out.kv("filter", filter);
        Out.kv("exclude(PUBLIC.AUDIT_LOG)", filter.exclude(TableIdentifier.create(null, "PUBLIC", "AUDIT_LOG")));
        Out.kv("exclude(PUBLIC.EMPLOYEE)", filter.exclude(TableIdentifier.create(null, "PUBLIC", "EMPLOYEE"))
            + "   (null = this filter has no opinion)");
        Out.note("CHANGED in 7: org.hibernate.cfg.reveng.TableFilter -> org.hibernate.tool.internal.reveng.strategy.TableFilter (same code).");

        Out.row("45", "OverrideRepository — filters + hibernate.reveng.xml on top of a delegate strategy");
        Out.line("hibernate.reveng.xml used:");
        RevengSupport.revengXml().lines().forEach(l -> Out.line("  " + l));
        OverrideRepository repository = RevengSupport.overrideRepository();
        RevengStrategy overridden = repository.getReverseEngineeringStrategy(new DefaultStrategy());
        TableIdentifier detail = TableIdentifier.create(null, "PUBLIC", "EMPLOYEE_DETAIL");
        Out.kv("strategy class", Out.simpleName(overridden));
        Out.kv("excludeTable(PUBLIC.AUDIT_LOG)", overridden.excludeTable(TableIdentifier.create(null, "PUBLIC", "AUDIT_LOG")));
        Out.kv("columnToHibernateTypeName VARCHAR(1000)",
            overridden.columnToHibernateTypeName(detail, "BIO", Types.VARCHAR, 1000, 0, 0, true, false)
                + "   (default strategy said: string)");
        Out.kv("getSchemaSelections()", overridden.getSchemaSelections().size() + " selection(s), schema "
            + overridden.getSchemaSelections().get(0).getMatchSchema());
        Out.note("CHANGED in 7: org.hibernate.cfg.reveng.OverrideRepository -> org.hibernate.tool.internal.reveng.strategy.OverrideRepository; "
            + "addInputStream(..) no longer closes the stream (caller uses try-with-resources); "
            + "new SchemaSelection(catalog, schema) -> RevengStrategy.SchemaSelection is an interface (RevengSupport.schemaSelection).");
    }

    static void delegatingStrategy() {
        Out.row("43", "DelegatingStrategy (was DelegatingReverseEngineeringStrategy) — DemoRevengStrategy overrides a few methods");
        RevengStrategy strategy = RevengSupport.strategy();
        TableIdentifier employee = TableIdentifier.create(null, "PUBLIC", "EMPLOYEE");
        Out.kv("strategy class", Out.simpleName(strategy));
        Out.kv("tableToClassName(EMPLOYEE)", strategy.tableToClassName(employee));
        Out.kv("columnToPropertyName(EMPLOYEE, VERSION)", strategy.columnToPropertyName(employee, "VERSION"));
        Out.kv("columnToPropertyName(EMPLOYEE, HIRED_ON)", strategy.columnToPropertyName(employee, "HIRED_ON"));
        Out.kv("getTableIdentifierStrategyName(EMPLOYEE)", strategy.getTableIdentifierStrategyName(employee));
        Out.kv("getTableIdentifierProperties(EMPLOYEE)", strategy.getTableIdentifierProperties(employee));
        Out.kv("excludeTable(AUDIT_LOG) (delegated)", strategy.excludeTable(TableIdentifier.create(null, "PUBLIC", "AUDIT_LOG")));
        Out.line("Chain: " + DemoRevengStrategy.class.getSimpleName() + " -> OverrideRepository strategy -> DefaultStrategy");
        Out.note("CHANGED in 7: org.hibernate.cfg.reveng.DelegatingReverseEngineeringStrategy -> "
            + "org.hibernate.tool.internal.reveng.strategy.DelegatingStrategy (constructor takes a RevengStrategy).");
    }

    static void descriptorAndMetadata() {
        Out.row("39/40", "MetadataDescriptorFactory.createReverseEngineeringDescriptor(strategy, properties) — a MetadataDescriptor");
        Properties props = RevengSupport.properties(DemoMetaDataDialect.class);
        props.put(MetadataConstants.PREFER_BASIC_COMPOSITE_IDS, Boolean.TRUE);
        MetadataDescriptor descriptor = MetadataDescriptorFactory.createReverseEngineeringDescriptor(RevengSupport.strategy(), props);
        Out.kv("descriptor class", Out.simpleName(descriptor));
        Out.kv("getProperties().hibernatetool.metadatadialect",
            descriptor.getProperties().getProperty(RevengSupport.METADATA_DIALECT_PROPERTY));
        Out.kv("getProperties() also has System/Environment props", descriptor.getProperties().size() + " entries");
        Out.note("CHANGED in 7: new JdbcMetadataDescriptor(strategy, props, preferBasicCompositeIds) -> "
            + "MetadataDescriptorFactory.createReverseEngineeringDescriptor(strategy, props) (a RevengMetadataDescriptor); "
            + "preferBasicCompositeIds is now the property MetadataConstants.PREFER_BASIC_COMPOSITE_IDS and must be a Boolean.");

        Out.row("39", "descriptor.createMetadata() — connects over JDBC, reads the schema, builds Metadata");
        Metadata metadata = descriptor.createMetadata();
        try {
            for (PersistentClass pc : metadata.getEntityBindings()) {
                Out.line(pc.getEntityName() + "   (table " + pc.getTable().getName()
                    + (pc.getVersion() != null ? ", optimistic-lock version property '" + pc.getVersion().getName() + "'" : "") + ")");
                Out.line("    id: " + (pc.getIdentifierProperty() != null ? pc.getIdentifierProperty().getName() : "(composite)"));
                for (Property p : pc.getProperties()) {
                    Out.line("    " + p.getName() + " : " + p.getValue().getClass().getSimpleName()
                        + (p.getValue().getType() != null ? " -> " + p.getValue().getType().getName() : ""));
                }
            }
            Out.note("AUDIT_LOG (TableFilter) and the HIGH_EARNERS view (DemoMetaDataDialect only lists TABLEs) are absent; "
                + "EMPLOYEE_PROJECT became a many-to-many Set, EMPLOYEE_DETAIL a OneToOne, VERSION a <version>.");
            Out.note("CHANGED in 7: PersistentClass.getPropertyIterator() -> getProperties() (removed in 6.0).");

            Out.row("41", "strategy.isOneToOne(ForeignKey) on the reverse-engineered foreign keys");
            RevengStrategy strategy = RevengSupport.strategy();
            for (PersistentClass pc : metadata.getEntityBindings()) {
                for (ForeignKey fk : pc.getTable().getForeignKeyCollection()) {
                    Out.kv(fk.getName() + " isOneToOne", strategy.isOneToOne(fk));
                }
            }
            Out.kv("DemoMetaDataDialect.getTables calls", DemoMetaDataDialect.getTablesCalls);
        } finally {
            ((StandardServiceRegistryImpl) ((MetadataImpl) metadata).getBootstrapContext().getServiceRegistry()).destroy();
        }
    }
}
