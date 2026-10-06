package h5.concepts.support;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.hibernate.boot.Metadata;
import org.hibernate.cfg.reveng.DefaultReverseEngineeringStrategy;
import org.hibernate.cfg.reveng.OverrideRepository;
import org.hibernate.cfg.reveng.ReverseEngineeringSettings;
import org.hibernate.cfg.reveng.ReverseEngineeringStrategy;
import org.hibernate.cfg.reveng.SchemaSelection;
import org.hibernate.cfg.reveng.TableFilter;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.metadata.JdbcMetadataDescriptor;
import org.hibernate.type.StandardBasicTypes;

/**
 * Builds the reverse-engineering pipeline the same way wmstdappdbimpl's {@code ConfigurationBuilder} does:
 *
 * <pre>
 * DefaultReverseEngineeringStrategy          (innermost delegate)
 *   -> OverrideRepository strategy           (schema selection, TableFilter, hibernate.reveng.xml type mappings)
 *     -> DemoRevengStrategy (Delegating..)   (naming / id-generator customisation)
 * + ReverseEngineeringSettings set on both the default and the outer strategy
 * -> JdbcMetadataDescriptor(strategy, properties, preferBasicCompositeIds).createMetadata()
 * </pre>
 *
 * The Mains use this helper; {@code RevengStrategyMain} walks through each step with output.
 */
public final class RevengSupport {

    public static final String PACKAGE = "com.demo.hr";
    /** Tools property that picks the MetaDataDialect (row 48). */
    public static final String METADATA_DIALECT_PROPERTY = "hibernatetool.metadatadialect";

    private RevengSupport() {
    }

    /**
     * The hibernate.reveng.xml content. wmstdappdbimpl's {@code Reveng} writes the same kind of file and uses
     * {@code StandardBasicTypes.X.getName()} for the hibernate-type values (row 18).
     * <p>
     * The DOCTYPE ({@code hibernate-reverse-engineering-3.0.dtd}) is left out on purpose: Tools 5.6 doesn't
     * bundle the DTD, so the JDK parser would try to download it.
     */
    public static String revengXml() {
        return "<hibernate-reverse-engineering>\n"
            + "  <type-mapping>\n"
            // Without this, VARCHAR(1000) (EMPLOYEE_DETAIL.BIO) would become "string".
            + "    <sql-type jdbc-type=\"VARCHAR\" length=\"1000\" hibernate-type=\"" + StandardBasicTypes.TEXT.getName() + "\"/>\n"
            + "    <sql-type jdbc-type=\"NVARCHAR\" hibernate-type=\"" + StandardBasicTypes.STRING.getName() + "\"/>\n"
            + "  </type-mapping>\n"
            + "</hibernate-reverse-engineering>\n";
    }

    public static OverrideRepository overrideRepository() {
        OverrideRepository repository = new OverrideRepository();
        // Only read the PUBLIC schema (otherwise HSQLDB's INFORMATION_SCHEMA etc. are read too).
        repository.addSchemaSelection(new SchemaSelection(null, DemoDatabase.SCHEMA));
        // Exclude AUDIT_LOG.
        TableFilter auditFilter = new TableFilter();
        auditFilter.setMatchSchema(DemoDatabase.SCHEMA);
        auditFilter.setMatchName("AUDIT_LOG");
        auditFilter.setExclude(true);
        repository.addTableFilter(auditFilter);
        repository.addInputStream(new ByteArrayInputStream(revengXml().getBytes(StandardCharsets.UTF_8)));
        return repository;
    }

    public static ReverseEngineeringStrategy strategy() {
        DefaultReverseEngineeringStrategy defaultStrategy = new DefaultReverseEngineeringStrategy();
        ReverseEngineeringStrategy strategy = overrideRepository().getReverseEngineeringStrategy(defaultStrategy);
        strategy = new DemoRevengStrategy(strategy);
        ReverseEngineeringSettings settings = new ReverseEngineeringSettings(strategy)
            .setDefaultPackageName(PACKAGE)
            .setDetectManyToMany(true)
            .setDetectOneToOne(true)
            .setDetectOptimisticLock(true);
        defaultStrategy.setSettings(settings);
        strategy.setSettings(settings);
        return strategy;
    }

    /** Connection properties + the metadata dialect to use for reverse engineering. */
    public static Properties properties(Class<?> metaDataDialect) {
        Properties props = DemoDatabase.hibernateProperties();
        props.setProperty(METADATA_DIALECT_PROPERTY, metaDataDialect.getName());
        return props;
    }

    public static MetadataDescriptor descriptor(ReverseEngineeringStrategy strategy, Properties props) {
        return new JdbcMetadataDescriptor(strategy, props, true);
    }

    /** One-call reverse engineering of the demo schema into a boot {@link Metadata}. */
    public static Metadata reverseEngineer() {
        DemoDatabase.ensureCreated();
        return descriptor(strategy(), properties(DemoMetaDataDialect.class)).createMetadata();
    }
}
