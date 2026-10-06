package h5.concepts.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.hibernate.boot.Metadata;
import org.hibernate.tool.api.metadata.MetadataConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.RevengSettings;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.hibernate.tool.internal.reveng.strategy.OverrideRepository;
import org.hibernate.tool.internal.reveng.strategy.TableFilter;
import org.hibernate.type.StandardBasicTypes;

/**
 * Builds the reverse-engineering pipeline the same way wmstdappdbimpl's {@code ConfigurationBuilder} does:
 *
 * <pre>
 * DefaultStrategy                            (innermost delegate; 5.6: DefaultReverseEngineeringStrategy)
 *   -> OverrideRepository strategy           (schema selection, TableFilter, hibernate.reveng.xml type mappings)
 *     -> DemoRevengStrategy (DelegatingStrategy) (naming / id-generator customisation)
 * + RevengSettings set on both the default and the outer strategy
 * -> MetadataDescriptorFactory.createReverseEngineeringDescriptor(strategy, properties).createMetadata()
 *    (5.6: new JdbcMetadataDescriptor(strategy, properties, preferBasicCompositeIds))
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
     * The DOCTYPE ({@code hibernate-reverse-engineering-3.0.dtd}) is left out on purpose: Tools (5.6 and 7.3)
     * doesn't bundle the DTD, so the JDK parser would try to download it.
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
        repository.addSchemaSelection(schemaSelection(null, DemoDatabase.SCHEMA));
        // Exclude AUDIT_LOG.
        TableFilter auditFilter = new TableFilter();
        auditFilter.setMatchSchema(DemoDatabase.SCHEMA);
        auditFilter.setMatchName("AUDIT_LOG");
        auditFilter.setExclude(true);
        repository.addTableFilter(auditFilter);
        // Tools 7 no longer closes the stream in addInputStream (5.6 did), so the caller closes it.
        try (InputStream xml = new ByteArrayInputStream(revengXml().getBytes(StandardCharsets.UTF_8))) {
            repository.addInputStream(xml);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return repository;
    }

    /**
     * Tools 5.6 had a {@code SchemaSelection(catalog, schema)} class; in 7 {@link RevengStrategy.SchemaSelection} is an
     * interface, so this is a small implementation of it.
     */
    public static RevengStrategy.SchemaSelection schemaSelection(String catalog, String schema) {
        return new RevengStrategy.SchemaSelection() {
            @Override
            public String getMatchCatalog() {
                return catalog;
            }

            @Override
            public String getMatchSchema() {
                return schema;
            }

            @Override
            public String getMatchTable() {
                return null;
            }

            @Override
            public String toString() {
                return "SchemaSelection(" + catalog + "." + schema + ")";
            }
        };
    }

    public static RevengStrategy strategy() {
        DefaultStrategy defaultStrategy = new DefaultStrategy();
        RevengStrategy strategy = overrideRepository().getReverseEngineeringStrategy(defaultStrategy);
        strategy = new DemoRevengStrategy(strategy);
        RevengSettings settings = new RevengSettings(strategy)
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

    /**
     * 5.6: {@code new JdbcMetadataDescriptor(strategy, props, true)}. In 7 the descriptor comes from the factory and
     * preferBasicCompositeIds is a property, which must be a {@code Boolean} (Tools casts it).
     */
    public static MetadataDescriptor descriptor(RevengStrategy strategy, Properties props) {
        props.put(MetadataConstants.PREFER_BASIC_COMPOSITE_IDS, Boolean.TRUE);
        return MetadataDescriptorFactory.createReverseEngineeringDescriptor(strategy, props);
    }

    /** One-call reverse engineering of the demo schema into a boot {@link Metadata}. */
    public static Metadata reverseEngineer() {
        DemoDatabase.ensureCreated();
        return descriptor(strategy(), properties(DemoMetaDataDialect.class)).createMetadata();
    }
}
