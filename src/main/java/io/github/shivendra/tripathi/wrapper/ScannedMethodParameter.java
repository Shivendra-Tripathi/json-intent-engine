package io.github.shivendra.tripathi.wrapper;

import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.MethodParameterInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Wrapper for ClassGraph's {@link MethodParameterInfo}.
 *
 * <p>Stores:
 * <ul>
 *   <li>Parameter name (may be {@code null} if class was compiled without debug info)</li>
 *   <li>Modifiers</li>
 *   <li>Type (erased and generic via {@link ScannedTypeRef})</li>
 *   <li>All annotations on this parameter ({@link ScannedDeclaredAnnotation})</li>
 * </ul>
 *
 * <p>No ClassGraph type is retained after construction.
 */
public final class ScannedMethodParameter extends ScannedAnnotatable {

    private final String  name;           // may be null
    private final int     modifiers;
    private final ScannedTypeRef type;
    private final int     index;          // 0-based position in parameter list

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    private ScannedMethodParameter(String name, int modifiers, ScannedTypeRef type,
                                   int index, List<ScannedDeclaredAnnotation> annotations) {
        super(annotations);
        this.name      = name;
        this.modifiers = modifiers;
        this.type      = Objects.requireNonNull(type);
        this.index     = index;
    }

    /**
     * Factory: converts a ClassGraph {@link MethodParameterInfo} plus its 0-based index
     * into a {@code ScannedMethodParameter}.  This is the only method that touches ClassGraph types.
     */
    public static ScannedMethodParameter from(MethodParameterInfo mpi, int index) {
        // Name
        String name = mpi.getName();

        // Modifiers
        int modifiers = mpi.getModifiers();

        // Type – prefer type-signature if available
        ScannedTypeRef type;
        try {
            var typeSig = mpi.getTypeSignatureOrTypeDescriptor();
            type = ScannedTypeRef.fromSignature(typeSig);
        } catch (Exception e) {
            // Fallback: use descriptor string
            type = ScannedTypeRef.of(mpi.getTypeDescriptor().toString());
        }

        // Annotations
        List<ScannedDeclaredAnnotation> annotations = new ArrayList<>();
        if (mpi.getAnnotationInfo() != null) {
            for (AnnotationInfo ai : mpi.getAnnotationInfo()) {
                annotations.add(ScannedDeclaredAnnotation.from(ai));
            }
        }

        return new ScannedMethodParameter(name, modifiers, type, index, annotations);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Parameter name as present in the bytecode debug table, or {@code null} if
     * the class was compiled without debug information.
     */
    public String getName()      { return name; }

    /** 0-based position of this parameter in the enclosing method's parameter list. */
    public int getIndex()        { return index; }

    /** Java modifiers bitmask (see {@link java.lang.reflect.Modifier}). */
    public int getModifiers()    { return modifiers; }

    /** The declared type of this parameter. */
    public ScannedTypeRef getType() { return type; }

    /** Convenience: erased type name. */
    public String getTypeName()  { return type.getTypeName(); }

    // -------------------------------------------------------------------------
    // Reflection
    // -------------------------------------------------------------------------

    /**
     * Loads the parameter's erased type using the given {@link ClassLoader}.
     *
     * @throws ClassNotFoundException if the type cannot be resolved
     */
    public Class<?> loadType(ClassLoader classLoader) throws ClassNotFoundException {
        return type.load(classLoader);
    }

    @Override
    public String toString() {
        return type.getGenericString() + (name != null ? " " + name : " arg" + index);
    }
}
