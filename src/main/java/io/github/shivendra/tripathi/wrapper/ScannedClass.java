package io.github.shivendra.tripathi.wrapper;

import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.MethodInfo;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Wrapper for ClassGraph's {@link ClassInfo}.
 *
 * <p>Stores:
 * <ul>
 *   <li>Class name, modifiers, interface / abstract / enum / record / annotation flags</li>
 *   <li>Superclass name and interface names</li>
 *   <li>Declared fields    → {@link ScannedField}</li>
 *   <li>Declared methods   → {@link ScannedMethod} (includes constructors)</li>
 *   <li>Declared annotations on the class itself → {@link ScannedDeclaredAnnotation}</li>
 *   <li>Names of inner / outer classes</li>
 * </ul>
 *
 * <p>No ClassGraph type is retained after construction.
 */
public final class ScannedClass extends ScannedAnnotatable {

    // -----------------------------------------------------------------------
    // Identity
    // -----------------------------------------------------------------------
    private final String  className;          // binary name
    private final String  simpleName;
    private final int     modifiers;

    // -----------------------------------------------------------------------
    // Kind flags
    // -----------------------------------------------------------------------
    private final boolean isInterface;
    private final boolean isAbstract;
    private final boolean isFinal;
    private final boolean isEnum;
    private final boolean isAnnotation;
    private final boolean isRecord;
    private final boolean isAnonymousInnerClass;
    private final boolean isInnerClass;
    private final boolean isSynthetic;

    // -----------------------------------------------------------------------
    // Hierarchy
    // -----------------------------------------------------------------------
    private final String       superclassName;    // null for java.lang.Object
    private final List<String> interfaceNames;
    private final List<String> innerClassNames;
    private final String       outerClassName;    // null if not a nested class

    // -----------------------------------------------------------------------
    // Members
    // -----------------------------------------------------------------------
    private final Map<String, ScannedField>         fieldsByName;
    private final List<ScannedMethod>               methods;           // all declared methods
    private final List<ScannedMethod>               constructors;      // all declared constructors

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    private ScannedClass(
            String className, String simpleName, int modifiers,
            boolean isInterface, boolean isAbstract, boolean isFinal,
            boolean isEnum, boolean isAnnotation, boolean isRecord,
            boolean isAnonymousInnerClass, boolean isInnerClass, boolean isSynthetic,
            String superclassName,
            List<String> interfaceNames, List<String> innerClassNames, String outerClassName,
            Map<String, ScannedField> fieldsByName,
            List<ScannedMethod> methods, List<ScannedMethod> constructors,
            List<ScannedDeclaredAnnotation> annotations) {
        super(annotations);
        this.className            = Objects.requireNonNull(className);
        this.simpleName           = Objects.requireNonNull(simpleName);
        this.modifiers            = modifiers;
        this.isInterface          = isInterface;
        this.isAbstract           = isAbstract;
        this.isFinal              = isFinal;
        this.isEnum               = isEnum;
        this.isAnnotation         = isAnnotation;
        this.isRecord             = isRecord;
        this.isAnonymousInnerClass = isAnonymousInnerClass;
        this.isInnerClass         = isInnerClass;
        this.isSynthetic          = isSynthetic;
        this.superclassName       = superclassName;
        this.interfaceNames       = List.copyOf(interfaceNames);
        this.innerClassNames      = List.copyOf(innerClassNames);
        this.outerClassName       = outerClassName;
        this.fieldsByName         = Collections.unmodifiableMap(fieldsByName);
        this.methods              = List.copyOf(methods);
        this.constructors         = List.copyOf(constructors);
    }

    /**
     * Factory: converts a ClassGraph {@link ClassInfo} into a {@code ScannedClass}.
     * This is the only method that touches ClassGraph types.
     */
    public static ScannedClass from(ClassInfo ci) {
        String className  = ci.getName();
        String simpleName = ci.getSimpleName();
        int    modifiers  = ci.getModifiers();

        boolean isInterface          = ci.isInterface();
        boolean isAbstract           = ci.isAbstract();
        boolean isFinal              = ci.isFinal();
        boolean isEnum               = ci.isEnum();
        boolean isAnnotation         = ci.isAnnotation();
        boolean isRecord             = ci.isRecord();
        boolean isAnonymousInnerClass = ci.isAnonymousInnerClass();
        boolean isInnerClass         = ci.isInnerClass();
        boolean isSynthetic          = ci.isSynthetic();

        // Superclass
        String superclassName = null;
        if (ci.getSuperclass() != null) {
            superclassName = ci.getSuperclass().getName();
        }

        // Interfaces
        List<String> interfaceNames = ci.getInterfaces()
                .stream().map(ClassInfo::getName).collect(Collectors.toList());

        // Inner classes
        List<String> innerClassNames = ci.getInnerClasses()
                .stream().map(ClassInfo::getName).collect(Collectors.toList());

        // Outer class
        String outerClassName = ci.getOuterClasses().isEmpty()
                ? null : ci.getOuterClasses().get(0).getName();

        // Fields
        Map<String, ScannedField> fieldsByName = new LinkedHashMap<>();
        for (FieldInfo fi : ci.getDeclaredFieldInfo()) {
            ScannedField sf = ScannedField.from(fi);
            fieldsByName.put(sf.getName(), sf);
        }

        // Methods and constructors
        List<ScannedMethod> methods      = new ArrayList<>();
        List<ScannedMethod> constructors = new ArrayList<>();
        for (MethodInfo mi : ci.getDeclaredMethodAndConstructorInfo()) {
            ScannedMethod sm = ScannedMethod.from(mi);
            if (sm.isConstructor()) constructors.add(sm);
            else                    methods.add(sm);
        }

        // Annotations on the class
        List<ScannedDeclaredAnnotation> annotations = new ArrayList<>();
        if (ci.getAnnotationInfo() != null) {
            for (AnnotationInfo ai : ci.getAnnotationInfo()) {
                annotations.add(ScannedDeclaredAnnotation.from(ai));
            }
        }

        return new ScannedClass(className, simpleName, modifiers,
                isInterface, isAbstract, isFinal, isEnum, isAnnotation, isRecord,
                isAnonymousInnerClass, isInnerClass, isSynthetic,
                superclassName, interfaceNames, innerClassNames, outerClassName,
                fieldsByName, methods, constructors, annotations);
    }

    // -----------------------------------------------------------------------
    // Identity accessors
    // -----------------------------------------------------------------------

    /** Fully-qualified binary class name. */
    public String getClassName()  { return className; }
    public String getSimpleName() { return simpleName; }
    public int    getModifiers()  { return modifiers; }

    // -----------------------------------------------------------------------
    // Kind flags
    // -----------------------------------------------------------------------

    public boolean isInterface()           { return isInterface; }
    public boolean isAbstract()            { return isAbstract; }
    public boolean isFinal()               { return isFinal; }
    public boolean isEnum()                { return isEnum; }
    public boolean isAnnotation()          { return isAnnotation; }
    public boolean isRecord()              { return isRecord; }
    public boolean isAnonymousInnerClass() { return isAnonymousInnerClass; }
    public boolean isInnerClass()          { return isInnerClass; }
    public boolean isSynthetic()           { return isSynthetic; }
    public boolean isConcrete()            { return !isInterface && !isAbstract; }

    // -----------------------------------------------------------------------
    // Hierarchy accessors
    // -----------------------------------------------------------------------

    /** Binary name of the direct superclass, or {@code null} for {@code java.lang.Object}. */
    public Optional<String> getSuperclassName() { return Optional.ofNullable(superclassName); }

    /** Binary names of all directly implemented / extended interfaces. */
    public List<String> getInterfaceNames()  { return interfaceNames; }

    /** Binary names of all directly declared inner classes. */
    public List<String> getInnerClassNames() { return innerClassNames; }

    /** Binary name of the immediately enclosing class, if any. */
    public Optional<String> getOuterClassName() { return Optional.ofNullable(outerClassName); }

    // -----------------------------------------------------------------------
    // Field accessors
    // -----------------------------------------------------------------------

    /** All declared fields. */
    public List<ScannedField> getDeclaredFields() {
        return List.copyOf(fieldsByName.values());
    }

    /** Returns the declared field with the given name, if present. */
    public Optional<ScannedField> getDeclaredField(String fieldName) {
        return Optional.ofNullable(fieldsByName.get(fieldName));
    }

    /** Returns all fields annotated with the given annotation class name. */
    public List<ScannedField> getFieldsAnnotatedWith(String annotationClassName) {
        return fieldsByName.values().stream()
                .filter(f -> f.hasAnnotation(annotationClassName))
                .collect(Collectors.toUnmodifiableList());
    }

    public List<ScannedField> getFieldsAnnotatedWith(Class<? extends Annotation> annotationClass) {
        return getFieldsAnnotatedWith(annotationClass.getName());
    }

    // -----------------------------------------------------------------------
    // Method accessors
    // -----------------------------------------------------------------------

    /** All declared methods (does NOT include constructors). */
    public List<ScannedMethod> getDeclaredMethods() { return methods; }

    /** All declared constructors. */
    public List<ScannedMethod> getDeclaredConstructors() { return constructors; }

    /** Returns all methods with the given simple name. */
    public List<ScannedMethod> getDeclaredMethodsByName(String methodName) {
        return methods.stream()
                .filter(m -> m.getName().equals(methodName))
                .collect(Collectors.toUnmodifiableList());
    }

    /** Returns all methods annotated with the given annotation class name. */
    public List<ScannedMethod> getMethodsAnnotatedWith(String annotationClassName) {
        return methods.stream()
                .filter(m -> m.hasAnnotation(annotationClassName))
                .collect(Collectors.toUnmodifiableList());
    }

    public List<ScannedMethod> getMethodsAnnotatedWith(Class<? extends Annotation> annotationClass) {
        return getMethodsAnnotatedWith(annotationClass.getName());
    }

    // -----------------------------------------------------------------------
    // Reflection
    // -----------------------------------------------------------------------

    /**
     * Loads and returns the {@link Class} object for this scanned class using the
     * provided {@link ClassLoader}.
     *
     * @throws ClassNotFoundException if the class cannot be found on the class-loader's path
     */
    public Class<?> loadClass(ClassLoader classLoader) throws ClassNotFoundException {
        return Class.forName(className, false, classLoader);
    }

    /**
     * Loads the class using the given class-loader and returns the live annotation
     * instance, if present.
     *
     * @param annotationClass the annotation type
     * @param classLoader     class-loader to resolve both the declaring class and the annotation
     * @throws ClassNotFoundException if any class cannot be resolved
     */
    public <A extends Annotation> Optional<A> loadAnnotationFromClass(
            Class<A> annotationClass, ClassLoader classLoader)
            throws ClassNotFoundException {
        Class<?> cls = loadClass(classLoader);
        return Optional.ofNullable(cls.getDeclaredAnnotation(annotationClass));
    }

    /**
     * Loads the superclass using the given class-loader.
     *
     * @throws ClassNotFoundException if the superclass cannot be resolved
     */
    public Optional<Class<?>> loadSuperclass(ClassLoader classLoader)
            throws ClassNotFoundException {
        if (superclassName == null) return Optional.empty();
        return Optional.of(Class.forName(superclassName, false, classLoader));
    }

    // -----------------------------------------------------------------------
    // Utilities
    // -----------------------------------------------------------------------

    @Override
    public String toString() {
        String kind = isInterface ? "interface"
                    : isEnum      ? "enum"
                    : isAnnotation ? "@interface"
                    : "class";
        return kind + " " + className;
    }
}
