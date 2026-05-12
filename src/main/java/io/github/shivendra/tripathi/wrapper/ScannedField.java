package io.github.shivendra.tripathi.wrapper;

import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.FieldInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Wrapper for ClassGraph's {@link FieldInfo}.
 *
 * <p>Stores:
 * <ul>
 *   <li>Field name and declaring class name</li>
 *   <li>Modifiers, {@code isStatic} / {@code isFinal} / {@code isTransient} / {@code isVolatile}</li>
 *   <li>Type (erased and generic via {@link ScannedTypeRef})</li>
 *   <li>Constant value (for compile-time constants)</li>
 *   <li>All declared annotations ({@link ScannedDeclaredAnnotation})</li>
 * </ul>
 *
 * <p>No ClassGraph type is retained after construction.
 */
public final class ScannedField extends ScannedAnnotatable {

    private final String       name;
    private final String       declaringClassName;
    private final int          modifiers;
    private final ScannedTypeRef type;
    private final Object       constantValue;   // null unless compile-time constant

    // modifier flags (duplicated from modifiers for ergonomics)
    private final boolean isStatic;
    private final boolean isFinal;
    private final boolean isTransient;
    private final boolean isVolatile;
    private final boolean isSynthetic;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    private ScannedField(String name, String declaringClassName, int modifiers,
                         ScannedTypeRef type, Object constantValue,
                         List<ScannedDeclaredAnnotation> annotations) {
        super(annotations);
        this.name              = Objects.requireNonNull(name);
        this.declaringClassName = Objects.requireNonNull(declaringClassName);
        this.modifiers         = modifiers;
        this.type              = Objects.requireNonNull(type);
        this.constantValue     = constantValue;

        this.isStatic    = java.lang.reflect.Modifier.isStatic(modifiers);
        this.isFinal     = java.lang.reflect.Modifier.isFinal(modifiers);
        this.isTransient = java.lang.reflect.Modifier.isTransient(modifiers);
        this.isVolatile  = java.lang.reflect.Modifier.isVolatile(modifiers);
        // synthetic bit = 0x1000
        this.isSynthetic = (modifiers & 0x1000) != 0;
    }

    /**
     * Factory: converts a ClassGraph {@link FieldInfo} into a {@code ScannedField}.
     * This is the only method that touches ClassGraph types.
     */
    public static ScannedField from(FieldInfo fieldInfo) {
        String name              = fieldInfo.getName();
        String declaringClassName = fieldInfo.getClassInfo().getName();
        int    modifiers         = fieldInfo.getModifiers();

        // Type
        ScannedTypeRef type;
        try {
            var typeSig = fieldInfo.getTypeSignatureOrTypeDescriptor();
            type = ScannedTypeRef.fromSignature(typeSig);
        } catch (Exception e) {
            type = ScannedTypeRef.of(fieldInfo.getTypeDescriptor().toString());
        }

        // Constant value
        Object constantValue = fieldInfo.getConstantInitializerValue();

        // Annotations
        List<ScannedDeclaredAnnotation> annotations = new ArrayList<>();
        if (fieldInfo.getAnnotationInfo() != null) {
            for (AnnotationInfo ai : fieldInfo.getAnnotationInfo()) {
                annotations.add(ScannedDeclaredAnnotation.from(ai));
            }
        }

        return new ScannedField(name, declaringClassName, modifiers, type,
                                constantValue, annotations);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public String getName()              { return name; }
    public String getDeclaringClassName(){ return declaringClassName; }
    public int    getModifiers()         { return modifiers; }
    public ScannedTypeRef getType()      { return type; }
    public String getTypeName()          { return type.getTypeName(); }
    public Object getConstantValue()     { return constantValue; }

    public boolean isStatic()    { return isStatic; }
    public boolean isFinal()     { return isFinal; }
    public boolean isTransient() { return isTransient; }
    public boolean isVolatile()  { return isVolatile; }
    public boolean isSynthetic() { return isSynthetic; }

    // -------------------------------------------------------------------------
    // Reflection
    // -------------------------------------------------------------------------

    /**
     * Loads the declaring class and returns the reflective {@link Field}.
     *
     * @throws ClassNotFoundException if the declaring class cannot be found
     * @throws NoSuchFieldException   if the field cannot be found (shouldn't normally happen)
     */
    public Field loadField(ClassLoader classLoader)
            throws ClassNotFoundException, NoSuchFieldException {
        Class<?> cls = classLoader.loadClass(declaringClassName);
        return cls.getDeclaredField(name);
    }

    /**
     * Loads the field's declared type.
     *
     * @throws ClassNotFoundException if the type cannot be resolved
     */
    public Class<?> loadType(ClassLoader classLoader) throws ClassNotFoundException {
        return type.load(classLoader);
    }

    /**
     * Returns the live annotation instance from the reflective {@link Field}, if present.
     *
     * @param annotationClass the annotation type to look for
     * @param classLoader     class-loader used for loading the declaring class
     */
    public <A extends Annotation> Optional<A> loadAnnotationFromField(
            Class<A> annotationClass, ClassLoader classLoader)
            throws ClassNotFoundException, NoSuchFieldException {
        return Optional.ofNullable(loadField(classLoader).getDeclaredAnnotation(annotationClass));
    }

    @Override
    public String toString() {
        return declaringClassName + "#" + name + " : " + type.getGenericString();
    }
}
