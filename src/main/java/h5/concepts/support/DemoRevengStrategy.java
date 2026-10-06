package h5.concepts.support;

import java.util.Properties;

import org.hibernate.mapping.ForeignKey;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.strategy.DelegatingStrategy;
import org.hibernate.tool.internal.util.NameConverter;

/**
 * [Rows 41, 43, 47, 53] A {@link DelegatingStrategy} (5.6: {@code DelegatingReverseEngineeringStrategy}): every
 * method is forwarded to the wrapped delegate, and only the ones below are customised. wmstdappdbimpl's
 * {@code DefaultRevengNamingStrategy} / {@code MySQLRevengNamingStrategy} etc. follow this pattern and are
 * created through a {@code (ReverseEngineeringStrategy delegate)} constructor ({@link RevengStrategy} in 7).
 */
public class DemoRevengStrategy extends DelegatingStrategy {

    public DemoRevengStrategy(RevengStrategy delegate) {
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
        return NameConverter.isReservedJavaKeyword(name) ? name + "_" : name;
    }

    /**
     * Override point wmstdappdbimpl customises; here it just delegates. The default answers true when the FK
     * columns equal the PK of the owning table (EMPLOYEE_DETAIL.EMP_ID).
     */
    @Override
    public boolean isOneToOne(ForeignKey foreignKey) {
        return super.isOneToOne(foreignKey);
    }

    /** Ends up as {@code EnhancedValue.getIdentifierGeneratorStrategy()} on the EMPLOYEE id (SimpleValue in 5.6). */
    @Override
    public String getTableIdentifierStrategyName(TableIdentifier identifier) {
        if ("EMPLOYEE".equalsIgnoreCase(identifier.getName())) {
            return "sequence";
        }
        return super.getTableIdentifierStrategyName(identifier);
    }

    /** Ends up as {@code EnhancedValue.getIdentifierGeneratorProperties()} on the EMPLOYEE id (SimpleValue in 5.6). */
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
