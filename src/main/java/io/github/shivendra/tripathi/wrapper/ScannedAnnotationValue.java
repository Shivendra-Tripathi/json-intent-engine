package io.github.shivendra.tripathi.wrapper;

import io.github.classgraph.AnnotationClassRef;
import io.github.classgraph.AnnotationEnumValue;
import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.AnnotationParameterValue;
import io.github.classgraph.AnnotationParameterValueList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single annotation parameter value extracted from ClassGraph.
 *
 * <p>Supports the full set of legal annotation value types:
 * <ul>
 *   <li>Primitives (boxed): Integer, Long, Short, Byte, Char, Float, Double, Boolean</li>
 *   <li>String</li>
 *   <li>Class reference – stored as the binary class name (String)</li>
 *   <li>Enum reference – stored as {@link ScannedEnumValue}</li>
 *   <li>Nested annotation – stored as {@link ScannedDeclaredAnnotation}</li>
 *   <li>Array of any of the above – stored as {@code List<ScannedAnnotationValue>}</li>
 * </ul>
 */
public final class ScannedAnnotationValue {

    public enum Kind {
        PRIMITIVE,      // Boolean, Byte, Short, Char, Integer, Long, Float, Double
        STRING,
        CLASS_REF,      // stored as binary class name String
        ENUM_REF,       // ScannedEnumValue
        ANNOTATION,     // nested ScannedDeclaredAnnotation
        ARRAY           // List<ScannedAnnotationValue>
    }

    private final String parameterName;
    private final Kind kind;
    private final Object value; // actual payload; see Kind docs above

    // -- private constructor, use factory ---------------------------------

    private ScannedAnnotationValue(String parameterName, Kind kind, Object value) {
        this.parameterName = Objects.requireNonNull(parameterName);
        this.kind = Objects.requireNonNull(kind);
        this.value = value;
    }

    // -- factory ----------------------------------------------------------

    /**
     * Builds a {@code ScannedAnnotationValue} from a ClassGraph
     * {@link AnnotationParameterValue}.  This is the only place ClassGraph
     * types are touched; callers never see them afterwards.
     */
    static ScannedAnnotationValue from(AnnotationParameterValue apv) {
        return fromRaw(apv.getName(), apv.getValue());
    }

    /** Recursive helper that converts a raw ClassGraph annotation value. */
    static ScannedAnnotationValue fromRaw(String name, Object raw) {
        if (raw == null) {
            return new ScannedAnnotationValue(name, Kind.STRING, null);
        }

        // Nested annotation
        if (raw instanceof AnnotationInfo nestedInfo) {
            ScannedDeclaredAnnotation nested = ScannedDeclaredAnnotation.from(nestedInfo);
            return new ScannedAnnotationValue(name, Kind.ANNOTATION, nested);
        }

        // Enum
        if (raw instanceof AnnotationEnumValue enumVal) {
            ScannedEnumValue scannedEnum = new ScannedEnumValue(
                    enumVal.getClassName(), enumVal.getValueName());
            return new ScannedAnnotationValue(name, Kind.ENUM_REF, scannedEnum);
        }

        // Class reference
        if (raw instanceof AnnotationClassRef classRef) {
            return new ScannedAnnotationValue(name, Kind.CLASS_REF, classRef.getName());
        }

        // Array (ClassGraph represents annotation arrays as Object[])
        if (raw instanceof Object[] arr) {
            List<ScannedAnnotationValue> items = new ArrayList<>(arr.length);
            for (int i = 0; i < arr.length; i++) {
                items.add(fromRaw(name + "[" + i + "]", arr[i]));
            }
            return new ScannedAnnotationValue(name, Kind.ARRAY,
                    Collections.unmodifiableList(items));
        }

        // Also handle AnnotationParameterValueList (rare but possible)
        if (raw instanceof AnnotationParameterValueList apvList) {
            List<ScannedAnnotationValue> items = new ArrayList<>(apvList.size());
            int i = 0;
            for (AnnotationParameterValue apv : apvList) {
                items.add(fromRaw(name + "[" + (i++) + "]", apv.getValue()));
            }
            return new ScannedAnnotationValue(name, Kind.ARRAY,
                    Collections.unmodifiableList(items));
        }

        // String
        if (raw instanceof String s) {
            return new ScannedAnnotationValue(name, Kind.STRING, s);
        }

        // Everything else is a primitive (boxed)
        return new ScannedAnnotationValue(name, Kind.PRIMITIVE, raw);
    }

    // -- accessors --------------------------------------------------------

    public String getParameterName() { return parameterName; }
    public Kind getKind()            { return kind; }

    /** Raw value – type depends on {@link Kind}. */
    public Object getRawValue() { return value; }

    // Typed convenience getters (throw if kind doesn't match)

    public String asString() {
        requireKind(Kind.STRING);
        return (String) value;
    }

    /** Returns the binary class name for a CLASS_REF value. */
    public String asClassRef() {
        requireKind(Kind.CLASS_REF);
        return (String) value;
    }

    public ScannedEnumValue asEnumRef() {
        requireKind(Kind.ENUM_REF);
        return (ScannedEnumValue) value;
    }

    public ScannedDeclaredAnnotation asNestedAnnotation() {
        requireKind(Kind.ANNOTATION);
        return (ScannedDeclaredAnnotation) value;
    }

    @SuppressWarnings("unchecked")
    public List<ScannedAnnotationValue> asArray() {
        requireKind(Kind.ARRAY);
        return (List<ScannedAnnotationValue>) value;
    }

    /** Convenience: return value cast to primitive wrapper (Boolean, Integer, …). */
    @SuppressWarnings("unchecked")
    public <T> T asPrimitive() {
        requireKind(Kind.PRIMITIVE);
        return (T) value;
    }

    private void requireKind(Kind expected) {
        if (kind != expected) {
            throw new IllegalStateException(
                    "Expected kind " + expected + " but was " + kind +
                    " for parameter '" + parameterName + "'");
        }
    }

    @Override
    public String toString() {
        return "ScannedAnnotationValue{name='" + parameterName +
               "', kind=" + kind + ", value=" + value + '}';
    }
}
