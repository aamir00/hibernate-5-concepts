package h5.concepts;

import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import h5.concepts.support.DemoDatabase;
import h5.concepts.support.Out;
import h5.concepts.support.RevengSupport;

/**
 * Rows 1–7: bootstrap & service registry.
 * <ul>
 *   <li>Legacy {@link Configuration} + {@link StandardServiceRegistryBuilder} -> {@link SessionFactory}
 *       (wmstdappdbimpl: {@code DBConnectionUtils.createSessionFactory}).</li>
 *   <li>Boot {@link Metadata} -> {@link MetadataImpl#getBootstrapContext()} -> {@link BootstrapContext}
 *       -> {@link StandardServiceRegistryImpl#destroy()} (wmstdappdbimpl: {@code DataModelExporter}).</li>
 * </ul>
 */
public class BootstrapMain {

    public static void main(String[] args) {
        Out.banner("BootstrapMain — Configuration, service registry, Metadata, BootstrapContext");
        DemoDatabase.ensureCreated();
        legacyConfigurationBootstrap();
        metadataBootstrapContextAndDestroy();
    }

    static void legacyConfigurationBootstrap() {
        Out.row("1", "org.hibernate.cfg.Configuration — legacy (native) bootstrap");
        Configuration cfg = new Configuration();
        Properties props = DemoDatabase.hibernateProperties();
        props.forEach((k, v) -> cfg.setProperty((String) k, (String) v));
        Out.kv("cfg.getProperty(hibernate.dialect)", cfg.getProperty("hibernate.dialect"));
        Out.kv("cfg.getProperty(...connection.url)", cfg.getProperty("hibernate.connection.url"));
        Out.line("No addAnnotatedClass/addResource: the SessionFactory is entity-less, it is only used for native SQL.");

        Out.row("2", "StandardServiceRegistryBuilder.applySettings(cfg.getProperties()).build()");
        StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
        StandardServiceRegistry serviceRegistry = builder.applySettings(cfg.getProperties()).build();
        Out.kv("cfg.getProperties().size() applied", cfg.getProperties().size());

        Out.row("3", "StandardServiceRegistry — container of services");
        Out.kv("registry class", Out.simpleName(serviceRegistry));
        Out.kv("getService(ConnectionProvider)", Out.simpleName(serviceRegistry.getService(ConnectionProvider.class)));
        JdbcEnvironment jdbcEnvironment = serviceRegistry.getService(JdbcEnvironment.class);
        Out.kv("getService(JdbcEnvironment).dialect", Out.simpleName(jdbcEnvironment.getDialect()));

        Out.row("1+3", "cfg.buildSessionFactory(serviceRegistry)");
        try (SessionFactory sessionFactory = cfg.buildSessionFactory(serviceRegistry)) {
            Out.kv("sessionFactory.isOpen()", sessionFactory.isOpen());
            Out.kv("entity count (metamodel)", sessionFactory.getMetamodel().getEntities().size());
        }
        Out.line("SessionFactory closed (try-with-resources).");
        StandardServiceRegistryBuilder.destroy(serviceRegistry);
        Out.line("StandardServiceRegistryBuilder.destroy(serviceRegistry) -> public way to release the registry.");
    }

    static void metadataBootstrapContextAndDestroy() {
        Out.row("5", "org.hibernate.boot.Metadata — built here by Hibernate Tools reverse engineering");
        Metadata metadata = RevengSupport.reverseEngineer();
        Out.kv("metadata class", Out.simpleName(metadata));
        Out.kv("getEntityBindings().size()", metadata.getEntityBindings().size());
        metadata.getEntityBindings().forEach(pc -> Out.line("  entity " + pc.getEntityName() + "  <-  table " + pc.getTable().getName()));
        Out.kv("getDatabase().getDefaultNamespace()", metadata.getDatabase().getDefaultNamespace().getName());

        Out.row("6", "org.hibernate.boot.internal.MetadataImpl — cast to the internal impl (as wmstdappdbimpl does)");
        MetadataImpl metadataImpl = (MetadataImpl) metadata;
        try {
            metadata.getSessionFactoryBuilder().build();
        } catch (HibernateException e) {
            Out.kv("getSessionFactoryBuilder().build()", e.getClass().getName() + ": " + e.getMessage());
            Out.note("Expected: reverse-engineered Metadata names entity classes (com.demo.hr.*Entity) that were never "
                + "generated or compiled, so no SessionFactory can be built from it.");
        }
        Out.kv("metadataImpl.getTypeConfiguration()", Out.simpleName(metadataImpl.getTypeConfiguration()));

        Out.row("7", "org.hibernate.boot.spi.BootstrapContext — metadataImpl.getBootstrapContext()");
        BootstrapContext bootstrapContext = metadataImpl.getBootstrapContext();
        Out.kv("bootstrapContext class", Out.simpleName(bootstrapContext));
        Out.kv("getServiceRegistry()", Out.simpleName(bootstrapContext.getServiceRegistry()));
        Out.kv("getJpaCompliance().isJpaQueryComplianceEnabled", bootstrapContext.getJpaCompliance().isJpaQueryComplianceEnabled());

        Out.row("4", "StandardServiceRegistryImpl.destroy() — release the reveng registry (connection pool)");
        StandardServiceRegistryImpl serviceRegistry = (StandardServiceRegistryImpl) bootstrapContext.getServiceRegistry();
        Out.kv("isActive() before destroy", serviceRegistry.isActive());
        serviceRegistry.destroy();
        Out.kv("isActive() after destroy", serviceRegistry.isActive());
        Out.note("Tools builds its own registry inside MetadataDescriptor.createMetadata() (5.6: JdbcMetadataDescriptor, "
            + "7: RevengMetadataDescriptor); nobody else closes it, so wmstdappdbimpl (DataModelExporter) reaches it through "
            + "getBootstrapContext() and destroys it. Unchanged in 7.");
    }
}
