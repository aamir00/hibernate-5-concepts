package h5.concepts;

import java.util.Collection;
import java.util.List;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedNameImpl;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.mapping.Value;
import org.hibernate.tool.internal.reveng.util.EnhancedValue;
import org.hibernate.type.StandardBasicTypes;

import h5.concepts.support.Out;
import h5.concepts.support.RevengSupport;

/**
 * Rows 8–18: relational naming, the boot-time mapping model and basic types.
 * <p>
 * The demo HR schema is reverse engineered into a {@link Metadata}, and then each {@link PersistentClass} is
 * walked the way wmstdappdbimpl's {@code DataModelExporter} / {@code RelationsMapper} /
 * {@code HibernateColumnMetaProvider} do (columns, keys, generator strategy and parameters). On 6.6 the 5.x
 * iterator getters are replaced by collection getters.
 */
public class MappingModelMain {

    public static void main(String[] args) {
        Out.banner("MappingModelMain — Identifier, QualifiedNameImpl, PersistentClass/Table/Column/keys, StandardBasicTypes");
        relationalNaming();
        Metadata metadata = RevengSupport.reverseEngineer();
        try {
            mappingModel(metadata);
        } finally {
            ((StandardServiceRegistryImpl) ((MetadataImpl) metadata).getBootstrapContext().getServiceRegistry()).destroy();
        }
        basicTypes();
    }

    static void relationalNaming() {
        Out.row("8", "org.hibernate.boot.model.naming.Identifier — new Identifier(text, quoted)");
        Identifier plain = new Identifier("EMPLOYEE", false);
        Identifier quoted = new Identifier("Employee", true);
        Out.kv("plain.getText() / render()", plain.getText() + " / " + plain.render());
        Out.kv("quoted.isQuoted() / render()", quoted.isQuoted() + " / " + quoted.render());
        Out.kv("Identifier.toIdentifier(\"`Mixed`\")", Identifier.toIdentifier("`Mixed`") + " (quoted=" + Identifier.toIdentifier("`Mixed`").isQuoted() + ")");

        Out.row("9", "org.hibernate.boot.model.relational.QualifiedNameImpl — catalog + schema + object name");
        QualifiedNameImpl name = new QualifiedNameImpl(null /* no catalog */,
            new Identifier("PUBLIC", false), new Identifier("EMPLOYEE", false));
        Out.kv("getSchemaName() / getObjectName()", name.getSchemaName() + " / " + name.getObjectName());
        Out.kv("render()", name.render());
        Out.note("wmstdappdbimpl (WMMySQLMetaDataDialect) builds one only to put a readable table name in error messages.");
    }

    static void mappingModel(Metadata metadata) {
        for (PersistentClass persistentClass : metadata.getEntityBindings()) {
            Out.row("10", "PersistentClass  " + persistentClass.getEntityName());
            Out.kv("getClassName()", persistentClass.getClassName());
            Out.kv("getIdentifierProperty()", persistentClass.getIdentifierProperty() == null
                ? "(none — composite id, see getIdentifier())" : persistentClass.getIdentifierProperty().getName());

            Table table = persistentClass.getTable();
            Out.row("11", "Table  " + table.getName());
            Out.kv("getSchema() / getCatalog()", table.getSchema() + " / " + table.getCatalog());
            Out.kv("getQualifiedTableName()", table.getQualifiedTableName());

            Out.row("12/13/14", "Column / Value / SimpleValue via table.getColumns()");
            for (Column column : table.getColumns()) {
                Value value = column.getValue();
                String generator = "";
                if (value.isSimpleValue()) {
                    SimpleValue simpleValue = (SimpleValue) value;
                    generator = " typeName=" + simpleValue.getTypeName();
                    // 7: the generator getters are gone from core's SimpleValue. Tools' reverse-engineered identifier
                    // (PersistentClass.getIdentifier()) is an EnhancedValue that keeps them. Read them from the identifier,
                    // not the column's value: for EMPLOYEE_DETAIL (PK shared with EMPLOYEE) the column's value is the
                    // OneToOne, whose default "assigned" (what 5.x/6.x printed) is not the real generator ("foreign").
                    if (persistentClass.getIdentifier() instanceof EnhancedValue enhancedId
                            && enhancedId.getIdentifierGeneratorStrategy() != null && table.getPrimaryKey().containsColumn(column)) {
                        generator += " idGenerator=" + enhancedId.getIdentifierGeneratorStrategy()
                            + " params=" + enhancedId.getIdentifierGeneratorProperties();
                    }
                }
                // wmstdappdbimpl (HibernateColumnMetaProvider) reads the no-arg getSqlType()/getSqlTypeCode(), which the
                // Tools JDBC binder fills from DatabaseMetaData; getSqlType(Metadata) resolves the DDL type via the dialect.
                System.out.printf("    %-11s getSqlType()=%-10s getSqlTypeCode()=%-5s getSqlType(md)=%-14s len=%-6s prec=%-4s scale=%-4s nullable=%-5s unique=%-5s%s%n",
                    column.getName(), column.getSqlType(), column.getSqlTypeCode(), column.getSqlType(metadata),
                    column.getLength(), column.getPrecision(), column.getScale(),
                    column.isNullable(), column.isUnique(), generator);
            }
            Out.note("For reverse-engineered columns the no-arg getSqlType() is null and getSqlTypeCode() holds the JDBC "
                + "type. CHANGED in 6: getSqlType(Dialect, Mapping) was removed -> getSqlType(Metadata); "
                + "getLength()/getPrecision()/getScale() are Long/Integer/Integer and null when unset (5.x: int, defaults 255/19/2). "
                + "REMOVED in 7: SimpleValue.getIdentifierGeneratorStrategy()/getIdentifierGeneratorProperties()/Parameters(); "
                + "reverse-engineered values implement Tools' EnhancedValue, which still has the strategy and properties.");

            Out.row("15", "PrimaryKey via table.getPrimaryKey().getColumns()");
            PrimaryKey primaryKey = table.getPrimaryKey();
            if (primaryKey != null) {
                Out.kv(primaryKey.getName(), columnNames(primaryKey.getColumns()));
            }

            Out.row("16", "UniqueKey via table.getUniqueKeys().values()");
            Collection<UniqueKey> uniqueKeys = table.getUniqueKeys().values();
            if (uniqueKeys.isEmpty()) {
                Out.line("(none)");
            }
            for (UniqueKey uniqueKey : uniqueKeys) {
                Out.kv(uniqueKey.getName(), columnNames(uniqueKey.getColumns()));
            }

            Out.row("17", "ForeignKey via table.getForeignKeyCollection()");
            Collection<ForeignKey> foreignKeys = table.getForeignKeyCollection();
            if (foreignKeys.isEmpty()) {
                Out.line("(none)");
            }
            for (ForeignKey foreignKey : foreignKeys) {
                List<Column> referenced = foreignKey.getReferencedColumns(); // typed List<Column> in 6 (raw in 5.x)
                String target = referenced.isEmpty()
                    ? "(PK of referenced table)"
                    : referenced.get(0).getName() + (referenced.size() > 1 ? ",..." : "");
                Out.kv(foreignKey.getName(), columnNames(foreignKey.getColumns()) + " -> "
                    + foreignKey.getReferencedTable().getName() + "." + target);
            }
        }
        Out.note("CHANGED in 6: Table.getColumnIterator() and Constraint.getColumnIterator() were removed -> getColumns(); "
            + "getUniqueKeyIterator()/getForeignKeyIterator() -> getUniqueKeys().values() / getForeignKeys().values(). "
            + "CHANGED in 7: the two iterators are removed and getForeignKeys() is deprecated for removal -> getForeignKeyCollection().");
    }

    static void basicTypes() {
        Out.row("18", "org.hibernate.type.StandardBasicTypes — type names used in hibernate.reveng.xml");
        Out.kv("STRING.getName()", StandardBasicTypes.STRING.getName());
        Out.kv("TEXT.getName()", StandardBasicTypes.TEXT.getName());
        Out.kv("INTEGER.getName()", StandardBasicTypes.INTEGER.getName());
        Out.kv("BIG_DECIMAL.getName()", StandardBasicTypes.BIG_DECIMAL.getName());
        Out.kv("STRING class (a BasicTypeReference in 6)", Out.simpleName(StandardBasicTypes.STRING));
        Out.note("CHANGED in 6: the constants are BasicTypeReference<T> (5.x: Type instances such as StringType); "
            + "getName() returns the same names.");
        Out.line("The reveng.xml used by RevengSupport maps VARCHAR(1000) -> \"" + StandardBasicTypes.TEXT.getName()
            + "\", so EMPLOYEE_DETAIL.BIO above shows typeName=text instead of string (no override touches EMPLOYEE.NOTES, so the CLOB stays clob).");
    }

    private static String columnNames(List<Column> columns) {
        StringBuilder sb = new StringBuilder("(");
        columns.forEach(c -> sb.append(sb.length() > 1 ? ", " : "").append(c.getName()));
        return sb.append(")").toString();
    }
}
