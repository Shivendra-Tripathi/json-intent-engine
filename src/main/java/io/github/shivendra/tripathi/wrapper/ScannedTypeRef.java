package io.github.shivendra.tripathi.wrapper;

import io.github.classgraph.ClassRefTypeSignature;
import io.github.classgraph.BaseTypeSignature;
import io.github.classgraph.ArrayTypeSignature;
import io.github.classgraph.HierarchicalTypeSignature;

import java.util.Objects;

/**
 * Lightweight, ClassGraph-free descriptor for a Java type reference.
 *
 * <p>Examples: {@code int}, {@code java.lang.String}, {@code java.util.List<?>},
 * {@code byte[][]}.
 */
public final class ScannedTypeRef {

    /** The raw / erased binary class name (e.g. {@code "java.util.List"}). */
    private final String typeName;

    /**
     * The fully-generic string representation when type-signature information is
     * available (e.g. {@code "java.util.List<java.lang.String>"}), otherwise equal
     * to {@link #typeName}.
     */
    private final String genericString;

    /** Whether this type is an array. */
    private final boolean array;

    /** Array dimension count (0 when not an array). */
    private final int arrayDimensions;

    /** Whether this is a primitive type ({@code int}, {@code boolean}, …). */
    private final boolean primitive;

    public ScannedTypeRef(String typeName, String genericString,
                          boolean array, int arrayDimensions, boolean primitive) {
        this.typeName        = Objects.requireNonNull(typeName);
        this.genericString   = genericString != null ? genericString : typeName;
        this.array           = array;
        this.arrayDimensions = arrayDimensions;
        this.primitive       = primitive;
    }

    // -- factories --------------------------------------------------------

    /** Build from a raw class-name string (no generic info). */
    public static ScannedTypeRef of(String binaryName) {
        boolean prim   = isPrimitiveName(binaryName);
        boolean arr    = binaryName.endsWith("[]");
        int     dims   = 0;
        String  base   = binaryName;
        while (base.endsWith("[]")) { dims++; base = base.substring(0, base.length() - 2); }
        return new ScannedTypeRef(binaryName, binaryName, arr, dims, prim);
    }

    /**
     * Build from a ClassGraph {@link HierarchicalTypeSignature}.
     * This is the only place ClassGraph types are referenced.
     */
    public static ScannedTypeRef fromSignature(HierarchicalTypeSignature sig) {
        if (sig == null) return of("java.lang.Object");

        if (sig instanceof ArrayTypeSignature arraySig) {
            String generic    = arraySig.toString();
            //String erasedName = arraySig.toString(); // best we have without full parsing
            int dims = 0;
            HierarchicalTypeSignature elem = arraySig.getElementTypeSignature();
            String baseName = resolveBaseName(elem);
            dims = arraySig.getNumDimensions();
            String rawName = baseName + "[]".repeat(dims);
            return new ScannedTypeRef(rawName, generic, true, dims, false);
        }

        if (sig instanceof BaseTypeSignature primSig) {
            String name = primSig.getTypeStr();
            return new ScannedTypeRef(name, name, false, 0, true);
        }

        if (sig instanceof ClassRefTypeSignature classSig) {
            String base    = classSig.getBaseClassName();
            String generic = classSig.toString();
            return new ScannedTypeRef(base, generic, false, 0, false);
        }

        // Wildcard / TypeVariable / etc.
        String str = sig.toString();
        return new ScannedTypeRef(str, str, false, 0, false);
    }

    // -- helpers ----------------------------------------------------------

    private static String resolveBaseName(HierarchicalTypeSignature elem) {
        if (elem instanceof BaseTypeSignature b) return b.getTypeStr();
        if (elem instanceof ClassRefTypeSignature c) return c.getBaseClassName();
        return elem.toString();
    }

    private static boolean isPrimitiveName(String name) {
        return switch (name) {
            case "boolean","byte","short","int","long","float","double","char","void" -> true;
            default -> false;
        };
    }

    // -- accessors --------------------------------------------------------

    /** Erased / raw binary class name. */
    public String getTypeName()      { return typeName; }

    /** Generic string representation (may equal {@link #getTypeName()} if no info available). */
    public String getGenericString() { return genericString; }

    public boolean isArray()         { return array; }
    public int getArrayDimensions()  { return arrayDimensions; }
    public boolean isPrimitive()     { return primitive; }

    /**
     * Load the erased type using the given {@link ClassLoader}.
     * Works for primitives, arrays, and object types.
     */
    public Class<?> load(ClassLoader classLoader) throws ClassNotFoundException {
        return loadByName(typeName, classLoader);
    }

    private static Class<?> loadByName(String name, ClassLoader cl)
            throws ClassNotFoundException {
        // Primitives
        if (isPrimitiveName(name)) {
            return switch (name) {
                case "boolean" -> boolean.class;
                case "byte"    -> byte.class;
                case "short"   -> short.class;
                case "int"     -> int.class;
                case "long"    -> long.class;
                case "float"   -> float.class;
                case "double"  -> double.class;
                case "char"    -> char.class;
                case "void"    -> void.class;
                default        -> throw new ClassNotFoundException(name);
            };
        }
        return Class.forName(name, false, cl);
    }

    @Override
    public String toString() { return genericString; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScannedTypeRef that)) return false;
        return typeName.equals(that.typeName);
    }

    @Override
    public int hashCode() { return typeName.hashCode(); }
}
