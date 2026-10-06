package h5.concepts;

import java.sql.Types;
import java.util.Iterator;
import java.util.Properties;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.reveng.DefaultReverseEngineeringStrategy;
import org.hibernate.cfg.reveng.OverrideRepository;
import org.hibernate.cfg.reveng.ReverseEngineeringSettings;
import org.hibernate.cfg.reveng.ReverseEngineeringStrategy;
import org.hibernate.cfg.reveng.ReverseEngineeringStrategyUtil;
import org.hibernate.cfg.reveng.TableFilter;
import org.hibernate.cfg.reveng.TableIdentifier;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.metadata.JdbcMetadataDescriptor;
import org.hibernate.tool.util.TableNameQualifier;

import h5.concepts.support.DemoDatabase;
import h5.concepts.support.DemoMetaDataDialect;
import h5.concepts.support.DemoRevengStrategy;
import h5.concepts.support.Out;
import h5.concepts.support.RevengSupport;

/**
 * Rows 39–47, 53–54: Hibernate Tools reverse engineering — turning live DB tables into a boot
 * {@link Metadata}. The strategy chain is the same one wmstdappdbimpl's {@code ConfigurationBuilder} builds
 * (see {@link RevengSupport}); this Main walks through each piece, then runs the whole thing.
 */
public class RevengStrategyMain {

    public static void main(String[] args) {
        Out.banner("RevengStrategyMain — TableIdentifier, strategies, settings, OverrideRepository, TableFilter, JdbcMetadataDescriptor");
        DemoDatabase.ensureCreated();
        valueObjectsAndUtilities();
        defaultStrategyAndSettings();
        overridesAndFilters();
        delegatingStrategy();
        descriptorAndMetadata();
    }

    static void valueObjectsAndUtilities() {
        Out.row("47", "TableIdentifier — catalog + schema + name key passed to strategy methods");
        TableIdentifier byName = new TableIdentifier("EMPLOYEE");
        TableIdentifier qualified = new TableIdentifier(null, "PUBLIC", "EMPLOYEE");
        Out.kv("new TableIdentifier(\"EMPLOYEE\")", byName);
        Out.kv("new TableIdentifier(null, \"PUBLIC\", \"EMPLOYEE\")", qualified);
        Out.kv("byName.equals(qualified)", byName.equals(qualified));
        Table table = new Table("DEPARTMENT");
        table.setSchema("PUBLIC");
        Out.kv("TableIdentifier.create(Table)", TableIdentifier.create(table));

        Out.row("54", "TableNameQualifier.qualify(catalog, schema, table)");
        Out.kv("qualify(null, \"PUBLIC\", \"EMPLOYEE\")", TableNameQualifier.qualify(null, "PUBLIC", "EMPLOYEE"));
        Out.kv("qualify(\"HR\", \"PUBLIC\", \"EMPLOYEE\")", TableNameQualifier.qualify("HR", "PUBLIC", "EMPLOYEE"));

        Out.row("53", "ReverseEngineeringStrategyUtil — naming helpers");
        Out.kv("toUpperCamelCase(\"EMPLOYEE_DETAIL\")", ReverseEngineeringStrategyUtil.toUpperCamelCase("EMPLOYEE_DETAIL"));
        Out.kv("simplePluralize(\"employee\")", ReverseEngineeringStrategyUtil.simplePluralize("employee"));
        Out.kv("simplePluralize(\"category\")", ReverseEngineeringStrategyUtil.simplePluralize("category"));
        Out.kv("simplePluralize(\"address\")", ReverseEngineeringStrategyUtil.simplePluralize("address"));
        Out.kv("isReservedJavaKeyword(\"class\")", ReverseEngineeringStrategyUtil.isReservedJavaKeyword("class"));
    }

    static void defaultStrategyAndSettings() {
        Out.row("41/42", "DefaultReverseEngineeringStrategy — built-in naming and type mapping");
        DefaultReverseEngineeringStrategy strategy = new DefaultReverseEngineeringStrategy();
        TableIdentifier detail = new TableIdentifier(null, "PUBLIC", "EMPLOYEE_DETAIL");
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

        Out.row("44", "ReverseEngineeringSettings — package, detect many-to-many / one-to-one / optimistic lock");
        ReverseEngineeringSettings settings = new ReverseEngineeringSettings(strategy)
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
    }

    static void overridesAndFilters() {
        Out.row("46", "TableFilter — include/exclude by catalog/schema/name pattern");
        TableFilter filter = new TableFilter();
        filter.setMatchSchema("PUBLIC");
        filter.setMatchName("AUDIT_.*");
        filter.setExclude(true);
        Out.kv("filter", filter);
        Out.kv("exclude(PUBLIC.AUDIT_LOG)", filter.exclude(new TableIdentifier(null, "PUBLIC", "AUDIT_LOG")));
        Out.kv("exclude(PUBLIC.EMPLOYEE)", filter.exclude(new TableIdentifier(null, "PUBLIC", "EMPLOYEE"))
            + "   (null = this filter has no opinion)");

        Out.row("45", "OverrideRepository — filters + hibernate.reveng.xml on top of a delegate strategy");
        Out.line("hibernate.reveng.xml used:");
        RevengSupport.revengXml().lines().forEach(l -> Out.line("  " + l));
        OverrideRepository repository = RevengSupport.overrideRepository();
        ReverseEngineeringStrategy overridden = repository.getReverseEngineeringStrategy(new DefaultReverseEngineeringStrategy());
        TableIdentifier detail = new TableIdentifier(null, "PUBLIC", "EMPLOYEE_DETAIL");
        Out.kv("strategy class", Out.simpleName(overridden));
        Out.kv("excludeTable(PUBLIC.AUDIT_LOG)", overridden.excludeTable(new TableIdentifier(null, "PUBLIC", "AUDIT_LOG")));
        Out.kv("columnToHibernateTypeName VARCHAR(1000)",
            overridden.columnToHibernateTypeName(detail, "BIO", Types.VARCHAR, 1000, 0, 0, true, false)
                + "   (default strategy said: string)");
        Out.kv("getSchemaSelections()", overridden.getSchemaSelections().size() + " selection(s), schema "
            + overridden.getSchemaSelections().get(0).getMatchSchema());
    }

    static void delegatingStrategy() {
        Out.row("43", "DelegatingReverseEngineeringStrategy — DemoRevengStrategy overrides a few methods");
        ReverseEngineeringStrategy strategy = RevengSupport.strategy();
        TableIdentifier employee = new TableIdentifier(null, "PUBLIC", "EMPLOYEE");
        Out.kv("strategy class", Out.simpleName(strategy));
        Out.kv("tableToClassName(EMPLOYEE)", strategy.tableToClassName(employee));
        Out.kv("columnToPropertyName(EMPLOYEE, VERSION)", strategy.columnToPropertyName(employee, "VERSION"));
        Out.kv("columnToPropertyName(EMPLOYEE, HIRED_ON)", strategy.columnToPropertyName(employee, "HIRED_ON"));
        Out.kv("getTableIdentifierStrategyName(EMPLOYEE)", strategy.getTableIdentifierStrategyName(employee));
        Out.kv("getTableIdentifierProperties(EMPLOYEE)", strategy.getTableIdentifierProperties(employee));
        Out.kv("excludeTable(AUDIT_LOG) (delegated)", strategy.excludeTable(new TableIdentifier(null, "PUBLIC", "AUDIT_LOG")));
        Out.line("Chain: " + DemoRevengStrategy.class.getSimpleName() + " -> OverrideRepository strategy -> DefaultReverseEngineeringStrategy");
    }

    @SuppressWarnings("unchecked")
    static void descriptorAndMetadata() {
        Out.row("39/40", "JdbcMetadataDescriptor(strategy, properties, preferBasicCompositeIds) — a MetadataDescriptor");
        Properties props = RevengSupport.properties(DemoMetaDataDialect.class);
        MetadataDescriptor descriptor = new JdbcMetadataDescriptor(RevengSupport.strategy(), props, true);
        Out.kv("getProperties().hibernatetool.metadatadialect",
            descriptor.getProperties().getProperty(RevengSupport.METADATA_DIALECT_PROPERTY));
        Out.kv("getProperties() also has System/Environment props", descriptor.getProperties().size() + " entries");

        Out.row("39", "descriptor.createMetadata() — connects over JDBC, reads the schema, builds Metadata");
        Metadata metadata = descriptor.createMetadata();
        try {
            for (PersistentClass pc : metadata.getEntityBindings()) {
                Out.line(pc.getEntityName() + "   (table " + pc.getTable().getName()
                    + (pc.getVersion() != null ? ", optimistic-lock version property '" + pc.getVersion().getName() + "'" : "") + ")");
                Out.line("    id: " + (pc.getIdentifierProperty() != null ? pc.getIdentifierProperty().getName() : "(composite)"));
                Iterator<Property> properties = pc.getPropertyIterator();
                while (properties.hasNext()) {
                    Property p = properties.next();
                    Out.line("    " + p.getName() + " : " + p.getValue().getClass().getSimpleName()
                        + (p.getValue().getType() != null ? " -> " + p.getValue().getType().getName() : ""));
                }
            }
            Out.note("AUDIT_LOG (TableFilter) and the HIGH_EARNERS view (DemoMetaDataDialect only lists TABLEs) are absent; "
                + "EMPLOYEE_PROJECT became a many-to-many Set, EMPLOYEE_DETAIL a OneToOne, VERSION a <version>.");

            Out.row("41", "strategy.isOneToOne(ForeignKey) on the reverse-engineered foreign keys");
            ReverseEngineeringStrategy strategy = RevengSupport.strategy();
            for (PersistentClass pc : metadata.getEntityBindings()) {
                Iterator<ForeignKey> fks = pc.getTable().getForeignKeyIterator();
                while (fks.hasNext()) {
                    ForeignKey fk = fks.next();
                    Out.kv(fk.getName() + " isOneToOne", strategy.isOneToOne(fk));
                }
            }
            Out.kv("DemoMetaDataDialect.getTables calls", DemoMetaDataDialect.getTablesCalls);
        } finally {
            ((StandardServiceRegistryImpl) ((MetadataImpl) metadata).getBootstrapContext().getServiceRegistry()).destroy();
        }
    }
}
