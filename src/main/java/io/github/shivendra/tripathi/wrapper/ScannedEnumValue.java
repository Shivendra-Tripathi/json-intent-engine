package io.github.shivendra.tripathi.wrapper;

import java.util.Objects;

/**
 * Represents an enum constant referenced inside an annotation parameter,
 * stored as (className, valueName) without retaining any ClassGraph type.
 */
public final class ScannedEnumValue {

    private final String enumClassName;
    private final String valueName;

    public ScannedEnumValue(String enumClassName, String valueName) {
        this.enumClassName = Objects.requireNonNull(enumClassName, "enumClassName");
        this.valueName     = Objects.requireNonNull(valueName,     "valueName");
    }

    /** Binary name of the enum class, e.g. {@code "java.lang.annotation.ElementType"}. */
    public String getEnumClassName() { return enumClassName; }

    /** Simple name of the enum constant, e.g. {@code "METHOD"}. */
    public String getValueName()     { return valueName; }

    /**
     * Resolves and returns the live {@link Enum} constant using the given {@link ClassLoader}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Enum<?> loadEnumConstant(ClassLoader classLoader) {
        try {
            Class<?> enumClass = classLoader.loadClass(enumClassName);
            if (!enumClass.isEnum()) {
                throw new IllegalStateException(enumClassName + " is not an enum");
            }
            return Enum.valueOf((Class<Enum>) enumClass, valueName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot load enum class: " + enumClassName, e);
        }
    }

    @Override
    public String toString() {
        return enumClassName + "." + valueName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScannedEnumValue that)) return false;
        return enumClassName.equals(that.enumClassName) && valueName.equals(that.valueName);
    }

    @Override
    public int hashCode() { return Objects.hash(enumClassName, valueName); }
}
