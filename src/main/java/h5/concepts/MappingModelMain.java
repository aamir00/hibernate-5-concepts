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
 * {@code HibernateColumnMetaProvider} do (collections, casts, generator strategy and properties).
 * The 5.x iterator getters are gone in 7; each row notes the replacement.
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
                    // The generator getters left SimpleValue in 7; Tools' reverse-engineered id values keep them.
                    if (persistentClass.getIdentifier() instanceof EnhancedValue enhancedId
                            && enhancedId.getIdentifierGeneratorStrategy() != null && table.getPrimaryKey().containsColumn(column)) {
                        generator += " idGenerator=" + enhancedId.getIdentifierGeneratorStrategy()
                            + " params=" + enhancedId.getIdentifierGeneratorProperties();
                    }
                }
                // wmstdappdbimpl (HibernateColumnMetaProvider) reads the no-arg getSqlType()/getSqlTypeCode(), which the
                // Tools JDBC binder fills from DatabaseMetaData; getSqlType(Metadata) resolves via the dialect.
                System.out.printf("    %-11s getSqlType()=%-10s getSqlTypeCode()=%-5s getSqlType(md)=%-14s len=%-6s prec=%-4s scale=%-4s nullable=%-5s unique=%-5s%s%n",
                    column.getName(), column.getSqlType(), column.getSqlTypeCode(), column.getSqlType(metadata),
                    column.getLength(), column.getPrecision(), column.getScale(),
                    column.isNullable(), column.isUnique(), generator);
            }
            Out.note("For reverse-engineered columns the no-arg getSqlType() is null and getSqlTypeCode() holds the JDBC type.");
            Out.note("CHANGED in 7: Table.getColumnIterator() -> getColumns() (removed in 6.0); "
                + "Column.getSqlType(Dialect, Mapping) -> getSqlType(Metadata) (removed in 6.0); "
                + "getLength()/getPrecision()/getScale() int -> Long/Integer/Integer, unset ones are null instead of 255/19/2 (6.0).");
            Out.note("REMOVED in 7: SimpleValue.getIdentifierGeneratorStrategy()/getIdentifierGeneratorProperties() (7.0, no core "
                + "boot-model equivalent) -> read them from the entity's identifier (PersistentClass.getIdentifier()), which for "
                + "reverse-engineered metadata is Tools' EnhancedBasicValue implementing org.hibernate.tool.internal.reveng.util.EnhancedValue. "
                + "(Column.getValue() of a PK column that is also a one-to-one FK is the OneToOne, which never carries a generator in 7.)");

            Out.row("15", "PrimaryKey via table.getPrimaryKey().getColumns()");
            PrimaryKey primaryKey = table.getPrimaryKey();
            if (primaryKey != null) {
                Out.kv(primaryKey.getName(), columnNames(primaryKey.getColumns()));
            }
            Out.note("CHANGED in 7: Constraint.getColumnIterator() -> getColumns() (removed in 6.0).");

            Out.row("16", "UniqueKey via table.getUniqueKeys().values()");
            Collection<UniqueKey> uniqueKeys = table.getUniqueKeys().values();
            if (uniqueKeys.isEmpty()) {
                Out.line("(none)");
            }
            for (UniqueKey uniqueKey : uniqueKeys) {
                Out.kv(uniqueKey.getName(), columnNames(uniqueKey.getColumns()));
            }
            Out.note("CHANGED in 7: Table.getUniqueKeyIterator() -> getUniqueKeys().values() (iterator removed in 7.0).");

            Out.row("17", "ForeignKey via table.getForeignKeyCollection()");
            Collection<ForeignKey> foreignKeys = table.getForeignKeyCollection();
            if (foreignKeys.isEmpty()) {
                Out.line("(none)");
            }
            for (ForeignKey foreignKey : foreignKeys) {
                List<Column> referenced = foreignKey.getReferencedColumns(); // typed List<Column> since 6
                String target = referenced.isEmpty()
                    ? "(PK of referenced table)"
                    : referenced.get(0).getName() + (referenced.size() > 1 ? ",..." : "");
                Out.kv(foreignKey.getName(), columnNames(foreignKey.getColumns()) + " -> "
                    + foreignKey.getReferencedTable().getName() + "." + target);
            }
            Out.note("CHANGED in 7: Table.getForeignKeyIterator() -> getForeignKeyCollection() (iterator removed in 7.0; "
                + "getForeignKeys() is deprecated for removal in 7); ForeignKey.getReferencedColumns() is a typed List<Column>.");
        }
    }

    static void basicTypes() {
        Out.row("18", "org.hibernate.type.StandardBasicTypes — type names used in hibernate.reveng.xml");
        Out.kv("STRING.getName()", StandardBasicTypes.STRING.getName());
        Out.kv("TEXT.getName()", StandardBasicTypes.TEXT.getName());
        Out.kv("INTEGER.getName()", StandardBasicTypes.INTEGER.getName());
        Out.kv("BIG_DECIMAL.getName()", StandardBasicTypes.BIG_DECIMAL.getName());
        Out.kv("STRING class", Out.simpleName(StandardBasicTypes.STRING));
        Out.note("CHANGED in 7: the constants are BasicTypeReference<T> (since 6.0), not Type instances; getName() is unchanged.");
        Out.line("The reveng.xml used by RevengSupport maps VARCHAR(1000) -> \"" + StandardBasicTypes.TEXT.getName()
            + "\", so EMPLOYEE_DETAIL.BIO above shows typeName=text instead of string (no override touches EMPLOYEE.NOTES, so the CLOB stays clob).");
    }

    private static String columnNames(Collection<Column> columns) {
        StringBuilder sb = new StringBuilder("(");
        columns.forEach(c -> sb.append(sb.length() > 1 ? ", " : "").append(c.getName()));
        return sb.append(")").toString();
    }
}
