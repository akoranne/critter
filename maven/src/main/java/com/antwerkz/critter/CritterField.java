package com.antwerkz.critter;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import static java.lang.String.*;
import static java.lang.String.format;
import org.jboss.forge.roaster.model.Field;
import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.FieldEndImpl;
import org.mongodb.morphia.query.QueryImpl;

public class CritterField implements Comparable<CritterField> {
  private String shortParameterType;

  private String fullParameterType;

  private Field<JavaClassSource> source;

  private String fullType;

  private CritterContext context;

  private JavaClassSource javaClass;

  public static final List<String> NUMERIC_TYPES = new ArrayList<>();

  static {
    NUMERIC_TYPES.add("java.lang.Float");
    NUMERIC_TYPES.add("java.lang.Double");
    NUMERIC_TYPES.add("java.lang.Long");
    NUMERIC_TYPES.add("java.lang.Integer");
    NUMERIC_TYPES.add("java.lang.Byte");
    NUMERIC_TYPES.add("java.lang.Short");
    NUMERIC_TYPES.add("java.lang.Number");
  }

  public CritterField(final CritterContext context, final JavaClassSource javaClass,
      final Field<JavaClassSource> field) {
    this.context = context;
    this.javaClass = javaClass;
    source = field;
    fullType = field.getType().getQualifiedName();
    if (field.getType().isParameterized()) {
      final List<Type<JavaClassSource>> typeArguments = field.getType().getTypeArguments();
      shortParameterType = typeArguments.get(0).getName();
      fullParameterType = typeArguments.get(0).getQualifiedName();
    }
  }

  public void buildField(final JavaClassSource criteriaClass) {
    final String qualifiedName = javaClass.getQualifiedName();
    criteriaClass.addImport(qualifiedName);
    criteriaClass.addImport(Criteria.class);
    String name = "\"" + source.getName() + "\"";
    if(getSource().getOrigin().hasAnnotation(Embedded.class) || context.isEmbedded(getSource().getOrigin())) {
      name = "prefix + " + name;
    }
    criteriaClass.addMethod()
        .setPublic()
        .setName(source.getName())
        .setReturnType(format("%s<%s, %s, %s>", TypeSafeFieldEnd.class.getName(), criteriaClass.getQualifiedName(),
            javaClass.getQualifiedName(), fullType))
        .setBody(format("return new TypeSafeFieldEnd<%s, %s, %s>(this, query, %s);",
            criteriaClass.getQualifiedName(), javaClass.getQualifiedName(), fullType, name));

    criteriaClass.addImport(FieldEndImpl.class);
    criteriaClass.addImport(QueryImpl.class);
    final MethodSource<JavaClassSource> method = criteriaClass.addMethod()
        .setName(source.getName())
        .setPublic()
        .setReturnType(Criteria.class);
    method.setBody(format("return new FieldEndImpl<QueryImpl>((QueryImpl)query, %s, (QueryImpl)query, false).equal(value);",
        name));

    method.addParameter(source.getType().getQualifiedName(), "value");
  }

  public void buildEmbed(final JavaClassSource criteriaClass) {
    criteriaClass.addImport(Criteria.class);
    String criteriaType;
    if(fullParameterType != null) {
      criteriaType = shortParameterType + "Criteria";
      criteriaClass.addImport(criteriaClass.getPackage() + "." + criteriaType);
    } else {
      final Type<JavaClassSource> type = source.getType();
      criteriaType = type.getQualifiedName() + "Criteria";
      criteriaClass.addImport(source.getType().getQualifiedName());
    }
    final MethodSource<JavaClassSource> method = criteriaClass.addMethod()
        .setPublic()
        .setName(source.getName())
        .setReturnType(criteriaType);
    method
        .setBody(format("return new %s(query, \"%s\");", method.getReturnType().getName(), source.getName()));
  }

  public void buildReference(final JavaClassSource criteriaClass) {
    criteriaClass.addMethod()
        .setPublic()
        .setName(source.getName())
        .setReturnType(criteriaClass)
        .setBody(format("query.filter(\"%s = \", reference);\n"
            + "return this;", source.getName()))
        .addParameter(source.getType().getQualifiedName(), "reference");
  }

  @Override
  public int compareTo(final CritterField other) {
    return source.getName().compareTo(other.source.getName());
  }

  public String getName() {
    return source.getName();
  }

  public String getFullType() {
    return fullType;
  }

  public boolean hasAnnotation(final Class<? extends Annotation> aClass) {
    return source.hasAnnotation(aClass);
  }

  public Boolean isContainer() {
    final String qualifiedName = source.getType().getQualifiedName();
    return qualifiedName.equals("java.util.List")
        || qualifiedName.equals("java.util.Set");
  }

  public boolean isNumeric() {
    return NUMERIC_TYPES.contains(source.getType().getQualifiedName());
  }

  public String getParameterType() {
    return fullParameterType;
  }

  public Field<JavaClassSource> getSource() {
    return source;
  }

  public String getFullyQualifiedType() {
    final String qualifiedName = source.getType().getQualifiedName();
    final List<Type<JavaClassSource>> typeArguments = source.getType().getTypeArguments();
    String types = typeArguments.isEmpty()
        ? ""
        : "<" + join(",", typeArguments.stream().map(Type::getQualifiedName).collect(Collectors.toList())) + ">";
    final String name = format("%s%s", qualifiedName, types);
    return name;
  }
}