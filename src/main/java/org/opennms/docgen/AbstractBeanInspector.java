package org.opennms.docgen;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Type;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractBeanInspector implements Inspector {

    public static class Property {

        private final String name;

        private final Type type;

        private final String comment;

        private Property(final String name,
                         final Type type,
                         final String comment) {
            this.name = name;
            this.type = type;

            this.comment = comment;
        }

        public String getName() {
            return this.name;
        }

        public Type getType() {
            return this.type;
        }

        public String getComment() {
            return this.comment;
        }
    }

    private static String capitalize(final String name) {
        if (name == null
            || name.length() == 0) {
            return name;
        }

        char chars[] = name.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }

    private static String decapitalize(final String name) {
        if (name == null
            || name.length() == 0) {
            return name;
        }

        char chars[] = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    private Iterator<MethodDoc> findClassMethods(final ClassDoc clazz, final String name) {
        return Iterators.filter(Arrays.asList(clazz.methods()).iterator(),
                                new Predicate<MethodDoc>() {
                                    @Override
                                    public boolean apply(final MethodDoc method) {
                                        return name == null ||
                                               method.name().equals(name);
                                    }
                                });
    }

    private Iterator<ClassDoc> findSuperClasses(final ClassDoc clazz) {
        List<ClassDoc> clazzes = new LinkedList<>();
        
        for (ClassDoc superclazz = clazz;
                superclazz != null;
                superclazz = superclazz.superclass()) {
            clazzes.add(superclazz);
        }

        return clazzes.iterator();
    }

    public Iterator<MethodDoc> findMethods(final ClassDoc clazz, final String name) {
        return Iterators.concat(Iterators.transform(this.findSuperClasses(clazz), new Function<ClassDoc, Iterator<MethodDoc>>() {
            @Override
            public Iterator<MethodDoc> apply(final ClassDoc clazz) {
                return AbstractBeanInspector.this.findClassMethods(clazz, name);
            }
        }));
    }

    public static boolean isSetter(final MethodDoc method) {
        return method.isPublic()
               && method.name().length() > 4
               && method.name().startsWith("set")
               && method.returnType().typeName().equals("void")
               && method.parameters().length == 1;
    }

    public MethodDoc findSetter(final ClassDoc clazz, final String name) {
        return Iterators.find(this.findMethods(clazz, "set" + capitalize(name)),
                              new Predicate<MethodDoc>() {
                                  @Override
                                  public boolean apply(final MethodDoc method) {
                                      return isSetter(method);
                                  }
                              }, null);
    }

    public static boolean isGetter(final MethodDoc method) {
        return method.isPublic()
               && method.name().length() > 4
               && method.name().startsWith("get")
               && !method.returnType().typeName().equals("void")
               && method.parameters().length == 0;
    }

    public MethodDoc findGetter(final ClassDoc clazz, final String name) {
        return Iterators.find(this.findMethods(clazz, "get" + capitalize(name)),
                              new Predicate<MethodDoc>() {
                                  @Override
                                  public boolean apply(final MethodDoc method) {
                                      return isGetter(method);
                                  }
                              }, null);
    }

    public Iterator<Property> findProperties(final ClassDoc clazz) {
        return Iterators.filter(Iterators.transform(this.findMethods(clazz, null),
                                                    new Function<MethodDoc, Property>() {
                                                        @Override
                                                        public Property apply(final MethodDoc setter) {
                                                            if (!isSetter(setter)) {
                                                                return null;
                                                            }

                                                            final String name = decapitalize(setter.name().substring(3));

                                                            return new Property(name,
                                                                                setter.parameters()[0].type(),
                                                                                setter.commentText());

                                                        }
                                                    }),
                                Predicates.notNull());
    }
}
