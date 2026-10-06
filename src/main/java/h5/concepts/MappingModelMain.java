package h5.concepts;

import java.util.Iterator;
import java.util.List;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedNameImpl;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.mapping.Value;
import org.hibernate.type.StandardBasicTypes;

import h5.concepts.support.Out;
import h5.concepts.support.RevengSupport;

/**
 * Rows 8–18: relational naming, the boot-time mapping model and basic types.
 * <p>
 * The demo HR schema is reverse engineered into a {@link Metadata}, and then each {@link PersistentClass} is
 * walked the way wmstdappdbimpl's {@code DataModelExporter} / {@code RelationsMapper} /
 * {@code HibernateColumnMetaProvider} do (raw iterators, casts, generator strategy and properties).
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

    @SuppressWarnings("rawtypes")
    static void mappingModel(Metadata metadata) {
        Dialect dialect = ((MetadataImpl) metadata).getBootstrapContext().getServiceRegistry()
            .getService(JdbcEnvironment.class).getDialect();

        for (PersistentClass persistentClass : metadata.getEntityBindings()) {
            Out.row("10", "PersistentClass  " + persistentClass.getEntityName());
            Out.kv("getClassName()", persistentClass.getClassName());
            Out.kv("getIdentifierProperty()", persistentClass.getIdentifierProperty() == null
                ? "(none — composite id, see getIdentifier())" : persistentClass.getIdentifierProperty().getName());

            Table table = persistentClass.getTable();
            Out.row("11", "Table  " + table.getName());
            Out.kv("getSchema() / getCatalog()", table.getSchema() + " / " + table.getCatalog());
            Out.kv("getQualifiedTableName()", table.getQualifiedTableName());

            Out.row("12/13/14", "Column / Value / SimpleValue via table.getColumnIterator()");
            Iterator columnIterator = table.getColumnIterator();
            while (columnIterator.hasNext()) {
                Column column = (Column) columnIterator.next();
                Value value = column.getValue();
                String generator = "";
                if (value.isSimpleValue()) {
                    SimpleValue simpleValue = (SimpleValue) value;
                    generator = " typeName=" + simpleValue.getTypeName();
                    if (simpleValue.getIdentifierGeneratorStrategy() != null && table.getPrimaryKey().containsColumn(column)) {
                        generator += " idGenerator=" + simpleValue.getIdentifierGeneratorStrategy()
                            + " params=" + simpleValue.getIdentifierGeneratorProperties();
                    }
                }
                // wmstdappdbimpl (HibernateColumnMetaProvider) reads the no-arg getSqlType()/getSqlTypeCode(), which the
                // Tools JDBC binder fills from DatabaseMetaData; the (Dialect, Mapping) forms resolve via the dialect.
                System.out.printf("    %-11s getSqlType()=%-10s getSqlTypeCode()=%-5s getSqlType(d,m)=%-14s len=%-6d prec=%-3d scale=%-2d nullable=%-5s unique=%-5s%s%n",
                    column.getName(), column.getSqlType(), column.getSqlTypeCode(), column.getSqlType(dialect, metadata),
                    column.getLength(), column.getPrecision(), column.getScale(),
                    column.isNullable(), column.isUnique(), generator);
            }
            Out.note("For reverse-engineered columns the no-arg getSqlType() is null and getSqlTypeCode() holds the JDBC "
                + "type. getLength()/getPrecision()/getScale() are int in 5.x; unset ones show the defaults 255/19/2.");

            Out.row("15", "PrimaryKey via table.getPrimaryKey().getColumnIterator()");
            PrimaryKey primaryKey = table.getPrimaryKey();
            if (primaryKey != null) {
                Out.kv(primaryKey.getName(), columnNames(primaryKey.getColumnIterator()));
            }

            Out.row("16", "UniqueKey via table.getUniqueKeyIterator()");
            Iterator<UniqueKey> uniqueKeys = table.getUniqueKeyIterator();
            if (!uniqueKeys.hasNext()) {
                Out.line("(none)");
            }
            while (uniqueKeys.hasNext()) {
                UniqueKey uniqueKey = uniqueKeys.next();
                Out.kv(uniqueKey.getName(), columnNames(uniqueKey.getColumnIterator()));
            }

            Out.row("17", "ForeignKey via table.getForeignKeyIterator()");
            Iterator<ForeignKey> foreignKeys = table.getForeignKeyIterator();
            if (!foreignKeys.hasNext()) {
                Out.line("(none)");
            }
            while (foreignKeys.hasNext()) {
                ForeignKey foreignKey = foreignKeys.next();
                List referenced = foreignKey.getReferencedColumns(); // raw List in 5.x
                String target = referenced.isEmpty()
                    ? "(PK of referenced table)"
                    : ((Column) referenced.get(0)).getName() + (referenced.size() > 1 ? ",..." : "");
                Out.kv(foreignKey.getName(), columnNames(foreignKey.getColumnIterator()) + " -> "
                    + foreignKey.getReferencedTable().getName() + "." + target);
            }
        }
    }

    static void basicTypes() {
        Out.row("18", "org.hibernate.type.StandardBasicTypes — type names used in hibernate.reveng.xml");
        Out.kv("STRING.getName()", StandardBasicTypes.STRING.getName());
        Out.kv("TEXT.getName()", StandardBasicTypes.TEXT.getName());
        Out.kv("INTEGER.getName()", StandardBasicTypes.INTEGER.getName());
        Out.kv("BIG_DECIMAL.getName()", StandardBasicTypes.BIG_DECIMAL.getName());
        Out.kv("STRING class (a Type instance in 5.x)", Out.simpleName(StandardBasicTypes.STRING));
        Out.line("The reveng.xml used by RevengSupport maps VARCHAR(1000) -> \"" + StandardBasicTypes.TEXT.getName()
            + "\", so EMPLOYEE_DETAIL.BIO above shows typeName=text instead of string (no override touches EMPLOYEE.NOTES, so the CLOB stays clob).");
    }

    private static String columnNames(Iterator<Column> columns) {
        StringBuilder sb = new StringBuilder("(");
        columns.forEachRemaining(c -> sb.append(sb.length() > 1 ? ", " : "").append(c.getName()));
        return sb.append(")").toString();
    }
}
