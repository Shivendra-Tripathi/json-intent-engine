package io.github.shivendra.tripathi.wrapper;

import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.MethodParameterInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Wrapper for ClassGraph's {@link MethodInfo} (covers both regular methods and constructors).
 *
 * <p>Stores:
 * <ul>
 *   <li>Method / constructor name and declaring class name</li>
 *   <li>Modifiers and boolean flags ({@code isStatic}, {@code isAbstract}, etc.)</li>
 *   <li>Return type ({@link ScannedTypeRef}; {@code void} for constructors)</li>
 *   <li>Ordered list of {@link ScannedMethodParameter}</li>
 *   <li>Declared checked exceptions</li>
 *   <li>All declared annotations ({@link ScannedDeclaredAnnotation})</li>
 *   <li>Whether this entry represents a constructor</li>
 * </ul>
 *
 * <p>No ClassGraph type is retained after construction.
 */
public final class ScannedMethod extends ScannedAnnotatable {

    private final String             name;
    private final String             declaringClassName;
    private final int                modifiers;
    private final ScannedTypeRef     returnType;
    private final List<ScannedMethodParameter> parameters;
    private final List<String>       exceptionClassNames;
    private final boolean            isConstructor;
    private final boolean            isStatic;
    private final boolean            isAbstract;
    private final boolean            isFinal;
    private final boolean            isSynchronized;
    private final boolean            isBridge;
    private final boolean            isSynthetic;
    private final boolean            isNative;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    private ScannedMethod(String name, String declaringClassName, int modifiers,
                          ScannedTypeRef returnType,
                          List<ScannedMethodParameter> parameters,
                          List<String> exceptionClassNames,
                          boolean isConstructor,
                          List<ScannedDeclaredAnnotation> annotations) {
        super(annotations);
        this.name               = Objects.requireNonNull(name);
        this.declaringClassName = Objects.requireNonNull(declaringClassName);
        this.modifiers          = modifiers;
        this.returnType         = Objects.requireNonNull(returnType);
        this.parameters         = List.copyOf(parameters);
        this.exceptionClassNames = List.copyOf(exceptionClassNames);
        this.isConstructor      = isConstructor;

        this.isStatic      = java.lang.reflect.Modifier.isStatic(modifiers);
        this.isAbstract    = java.lang.reflect.Modifier.isAbstract(modifiers);
        this.isFinal       = java.lang.reflect.Modifier.isFinal(modifiers);
        this.isSynchronized = java.lang.reflect.Modifier.isSynchronized(modifiers);
        this.isBridge      = (modifiers & 0x0040) != 0;
        this.isSynthetic   = (modifiers & 0x1000) != 0;
        this.isNative      = java.lang.reflect.Modifier.isNative(modifiers);
    }

    /**
     * Factory: converts a ClassGraph {@link MethodInfo} into a {@code ScannedMethod}.
     * This is the only method that touches ClassGraph types.
     */
    public static ScannedMethod from(MethodInfo methodInfo) {
        String name               = methodInfo.getName();
        String declaringClassName = methodInfo.getClassInfo().getName();
        int    modifiers          = methodInfo.getModifiers();
        boolean isConstructor     = methodInfo.isConstructor();

        // Return type
        ScannedTypeRef returnType;
        try {
            var sig = methodInfo.getTypeSignatureOrTypeDescriptor();
            var retSig = sig.getResultType();
            returnType = ScannedTypeRef.fromSignature(retSig);
        } catch (Exception e) {
            returnType = ScannedTypeRef.of("java.lang.Object");
        }

        // Parameters
        List<ScannedMethodParameter> params = new ArrayList<>();
        MethodParameterInfo[] mpiArr = methodInfo.getParameterInfo();
        if (mpiArr != null) {
            for (int i = 0; i < mpiArr.length; i++) {
                params.add(ScannedMethodParameter.from(mpiArr[i], i));
            }
        }

        // Exceptions
        List<String> exceptions = new ArrayList<>();
        try {
            var sig = methodInfo.getTypeSignatureOrTypeDescriptor();
            if (sig.getThrowsSignatures() != null) {
                for (var ts : sig.getThrowsSignatures()) {
                    exceptions.add(ts.toString());
                }
            }
        } catch (Exception ignored) { /* best-effort */ }

        // Annotations
        List<ScannedDeclaredAnnotation> annotations = new ArrayList<>();
        if (methodInfo.getAnnotationInfo() != null) {
            for (AnnotationInfo ai : methodInfo.getAnnotationInfo()) {
                annotations.add(ScannedDeclaredAnnotation.from(ai));
            }
        }

        return new ScannedMethod(name, declaringClassName, modifiers, returnType,
                                 params, exceptions, isConstructor, annotations);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public String             getName()               { return name; }
    public String             getDeclaringClassName()  { return declaringClassName; }
    public int                getModifiers()           { return modifiers; }
    public ScannedTypeRef     getReturnType()          { return returnType; }
    public List<ScannedMethodParameter> getParameters(){ return parameters; }
    public List<String>       getExceptionClassNames() { return exceptionClassNames; }
    public boolean            isConstructor()          { return isConstructor; }

    public boolean isStatic()       { return isStatic; }
    public boolean isAbstract()     { return isAbstract; }
    public boolean isFinal()        { return isFinal; }
    public boolean isSynchronized() { return isSynchronized; }
    public boolean isBridge()       { return isBridge; }
    public boolean isSynthetic()    { return isSynthetic; }
    public boolean isNative()       { return isNative; }

    // -------------------------------------------------------------------------
    // Parameter helpers
    // -------------------------------------------------------------------------

    /** Returns a single parameter by 0-based index. */
    public ScannedMethodParameter getParameter(int index) {
        return parameters.get(index);
    }

    /** Returns all parameters annotated with the given annotation class name. */
    public List<ScannedMethodParameter> getParametersAnnotatedWith(String annotationClassName) {
        return parameters.stream()
                .filter(p -> p.hasAnnotation(annotationClassName))
                .collect(Collectors.toUnmodifiableList());
    }

    public List<ScannedMethodParameter> getParametersAnnotatedWith(
            Class<? extends Annotation> annotationClass) {
        return getParametersAnnotatedWith(annotationClass.getName());
    }

    // -------------------------------------------------------------------------
    // Reflection
    // -------------------------------------------------------------------------

    /**
     * Loads and returns the reflective {@link Method}.
     * Requires the declaring class to be on the class-loader's path.
     *
     * @throws ClassNotFoundException if the declaring class or a parameter type cannot be found
     * @throws NoSuchMethodException  if the method signature cannot be matched
     */
    public Method loadMethod(ClassLoader classLoader)
            throws ClassNotFoundException, NoSuchMethodException {
        if (isConstructor) {
            throw new IllegalStateException(
                    "This entry represents a constructor; use loadConstructor() instead.");
        }
        Class<?> cls = classLoader.loadClass(declaringClassName);
        Class<?>[] paramTypes = resolveParamTypes(classLoader);
        return cls.getDeclaredMethod(name, paramTypes);
    }

    /**
     * Loads and returns the reflective {@link Constructor}.
     *
     * @throws ClassNotFoundException if the declaring class or a parameter type cannot be found
     * @throws NoSuchMethodException  if the constructor signature cannot be matched
     */
    // @SuppressWarnings("rawtypes")
    public Constructor<?> loadConstructor(ClassLoader classLoader)
            throws ClassNotFoundException, NoSuchMethodException {
        if (!isConstructor) {
            throw new IllegalStateException(
                    "This entry represents a method; use loadMethod() instead.");
        }
        Class<?> cls = classLoader.loadClass(declaringClassName);
        Class<?>[] paramTypes = resolveParamTypes(classLoader);
        return cls.getDeclaredConstructor(paramTypes);
    }

    /**
     * Resolves the erased parameter type array needed for reflective look-up.
     */
    private Class<?>[] resolveParamTypes(ClassLoader classLoader)
            throws ClassNotFoundException {
        Class<?>[] types = new Class[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            types[i] = parameters.get(i).loadType(classLoader);
        }
        return types;
    }

    /**
     * Returns the live annotation from the reflective method, if present.
     *
     * @throws ClassNotFoundException if any class cannot be resolved
     * @throws NoSuchMethodException  if the method cannot be found
     */
    public <A extends Annotation> Optional<A> loadAnnotationFromMethod(
            Class<A> annotationClass, ClassLoader classLoader)
            throws ClassNotFoundException, NoSuchMethodException {
        return Optional.ofNullable(loadMethod(classLoader).getDeclaredAnnotation(annotationClass));
    }

    /** Fully-qualified descriptor for debugging purposes. */
    @Override
    public String toString() {
        String params = parameters.stream()
                .map(p -> p.getType().getGenericString())
                .collect(Collectors.joining(", "));
        return (isConstructor ? "ctor " : returnType.getGenericString() + " ")
                + declaringClassName + (isConstructor ? "" : "#" + name)
                + "(" + params + ")";
    }
}
