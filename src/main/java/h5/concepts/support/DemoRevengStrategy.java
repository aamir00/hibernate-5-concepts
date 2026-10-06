package h5.concepts.support;

import java.util.Properties;

import org.hibernate.cfg.reveng.DelegatingReverseEngineeringStrategy;
import org.hibernate.cfg.reveng.ReverseEngineeringStrategy;
import org.hibernate.cfg.reveng.ReverseEngineeringStrategyUtil;
import org.hibernate.cfg.reveng.TableIdentifier;
import org.hibernate.mapping.ForeignKey;

/**
 * [Rows 41, 43, 47, 53] A {@link DelegatingReverseEngineeringStrategy}: every method is forwarded to the
 * wrapped delegate, and only the ones below are customised. wmstdappdbimpl's
 * {@code DefaultRevengNamingStrategy} / {@code MySQLRevengNamingStrategy} etc. follow this pattern and are
 * created through a {@code (ReverseEngineeringStrategy delegate)} constructor.
 */
public class DemoRevengStrategy extends DelegatingReverseEngineeringStrategy {

    public DemoRevengStrategy(ReverseEngineeringStrategy delegate) {
        super(delegate);
    }

    /** Table -> entity class name. Default gives e.g. {@code com.demo.hr.EmployeeDetail}; we add an "Entity" suffix. */
    @Override
    public String tableToClassName(TableIdentifier tableIdentifier) {
        return super.tableToClassName(tableIdentifier) + "Entity";
    }

    /** Column -> property name. Renames the VERSION column and avoids Java keywords. */
    @Override
    public String columnToPropertyName(TableIdentifier table, String columnName) {
        if ("VERSION".equalsIgnoreCase(columnName)) {
            return "rowVersion";
        }
        String name = super.columnToPropertyName(table, columnName);
        return ReverseEngineeringStrategyUtil.isReservedJavaKeyword(name) ? name + "_" : name;
    }

    /**
     * Override point wmstdappdbimpl customises; here it just delegates. The default answers true when the FK
     * columns equal the PK of the owning table (EMPLOYEE_DETAIL.EMP_ID).
     */
    @Override
    public boolean isOneToOne(ForeignKey foreignKey) {
        return super.isOneToOne(foreignKey);
    }

    /** Ends up as {@code SimpleValue.getIdentifierGeneratorStrategy()} on the EMPLOYEE id. */
    @Override
    public String getTableIdentifierStrategyName(TableIdentifier identifier) {
        if ("EMPLOYEE".equalsIgnoreCase(identifier.getName())) {
            return "sequence";
        }
        return super.getTableIdentifierStrategyName(identifier);
    }

    /** Ends up as {@code SimpleValue.getIdentifierGeneratorProperties()} on the EMPLOYEE id. */
    @Override
    public Properties getTableIdentifierProperties(TableIdentifier identifier) {
        if ("EMPLOYEE".equalsIgnoreCase(identifier.getName())) {
            Properties props = new Properties();
            props.setProperty("sequence_name", "EMP_SEQ");
            props.setProperty("schema", DemoDatabase.SCHEMA);
            return props;
        }
        return super.getTableIdentifierProperties(identifier);
    }
}
