<#-- FreeMarker template run once per entity by ExporterMain's GenericExporter.
     Context: pojo = POJOClass, clazz = PersistentClass, c2j = Cfg2JavaTool. -->
Entity      : ${pojo.getQualifiedDeclarationName()}
Package     : ${pojo.getPackageName()}
Class name  : ${pojo.getDeclarationName()}
Table       : ${clazz.table.name}
Has id      : ${pojo.hasIdentifierProperty()?c}
Id generator annotation (pojo.generateAnnIdGenerator()):
${pojo.generateAnnIdGenerator()}
Properties:
<#list pojo.getAllPropertiesIterator() as property>
  ${property.name} : ${c2j.getJavaTypeName(property, true)}
</#list>
