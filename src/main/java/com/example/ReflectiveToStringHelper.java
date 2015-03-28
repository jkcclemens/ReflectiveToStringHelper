package com.example;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class similar to {@link com.google.common.base.MoreObjects.ToStringHelper}, but it auto-populates the String using
 * reflection to get fields.
 */
public class ReflectiveToStringHelper {

    private final Class<?> clazz;
    private final Object instance;
    private final Include include;
    private final StringBuilder sb = new StringBuilder();
    private final Map<String, String> fields = Maps.newHashMap();
    private Comparator<? super Field> fieldComparator;
    private Comparator<? super Entry<String, String>> entryComparator;

    /**
     * Should not be constructed during runtime.
     *
     * @see #of(Object)
     * @see #of(Object, Include)
     */
    private ReflectiveToStringHelper(final Class<?> clazz, final Object instance, final Include include) {
        this.clazz = clazz;
        this.instance = instance;
        this.include = include;
        this.addDeclaredFields();
        this.addCustoms();
    }

    /**
     * Returns a toString String for the given object, only showing public fields.
     * <p>{@code ClassName{field=value,field2=value2,...}}
     *
     * @param o Object to generate toString for
     * @return toString String, ready for returning
     * @see #of(Object, Include)
     */
    public static ReflectiveToStringHelper of(final Object o) {
        return ReflectiveToStringHelper.of(o, Include.create().publics(true));
    }

    /**
     * Returns a toString String for the given object, showing fields specified by {@code include}.
     *
     * @param o       Object to generate toString for
     * @param include Include object
     * @return toString String, ready for returning
     * @see #of(Object)
     */
    public static ReflectiveToStringHelper of(final Object o, final Include include) {
        return new ReflectiveToStringHelper(o.getClass(), o, include);
    }

    /**
     * Appends the custom fields to the StringBuilder.
     */
    private void addCustoms() {
        this.include.custom.forEach((k, v) -> this.fields.put(k, v == null ? "null" : v.toString()));
    }

    /**
     * Appends declared fields to the StringBuilder. This will use {@code includes} to determine which fields to append.
     * <p>Note that this does not add "&#123;" or "&#125;".
     */
    private void addDeclaredFields() {
        final Field[] fieldArray = this.clazz.getDeclaredFields();
        final List<Field> fields;
        if (this.fieldComparator != null) {
            fields = Arrays.stream(fieldArray).sorted(this.fieldComparator).collect(Collectors.toList());
        } else {
            fields = Arrays.asList(fieldArray);
        }
        for (final Field field : fields) {
            final Tuple<Object, Throwable> value = this.getFieldValue(field);
            if (!this.isSafeToInclude(field, value)) continue;
            this.addField(field, value);
        }
    }

    private void addField(final Field field, final Tuple<Object, Throwable> value) {
        final StringBuilder valueString = new StringBuilder();
        if (value.left == null && value.right != null) {
            valueString
                .append("{")
                .append(value.right.getClass().getSimpleName())
                .append(":")
                .append(value.right.getMessage())
                .append("}");
        } else {
            valueString.append(value.left == null ? "null" : value.left.toString());
        }
        this.fields.put(this.getFieldName(field), valueString.toString());
    }

    /**
     * Appends the class name to the StringBuilder. Uses {@link Class#getSimpleName()}.
     */
    private void appendClassName() {
        this.sb.append(this.clazz.getSimpleName());
    }

    /**
     * Appends the given field to the StringBuilder. This uses the format {@code fieldName=fieldValue}. If an exception
     * occurs when getting the value of the field, the format will be {@code fieldName={ExceptionName:Message}}.
     * <p>This does not check with {@code includes} to see if it should be used.
     *
     * @param field Field to append
     */
    private void appendField(final Field field, final Tuple<Object, Throwable> value) {
        this.sb.append(this.getFieldName(field)).append("=");
        if (value.left == null && value.right != null) {
            this.sb
                .append("{")
                .append(value.right.getClass().getSimpleName())
                .append(":")
                .append(value.right.getMessage())
                .append("}");
        } else {
            this.sb.append(value.left == null ? "null" : value.left.toString());
        }
    }

    private void appendFields() {
        Stream<Entry<String, String>> stream = this.fields.entrySet().stream();
        if (this.entryComparator != null) {
            stream = stream.sorted(this.entryComparator);
        }
        this.sb.append(
            stream
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(","))
        );
    }

    /**
     * Gets the name of this field, using mapped names from {@code includes} if necessary.
     *
     * @param field Field to get name of
     * @return Name of field
     */
    private String getFieldName(final Field field) {
        final String fieldName = field.getName();
        return this.include.mappedNames.entrySet().stream()
            .filter(e -> e.getKey().equals(fieldName))
            .map(Entry::getValue)
            .findFirst()
            .orElse(fieldName);
    }

    /**
     * Gets the value of a field. If there was any sort of throwable in retrieving the value, the right side of the
     * tuple will contain the exception, and the left side will be null.
     * <p>If all goes well, the value will be contained on the left side, and null will be contained on the right side.
     *
     * @param field Field to get value of
     * @return Tuple, as described above
     */
    private Tuple<Object, Throwable> getFieldValue(final Field field) {
        final boolean wasAccessible = field.isAccessible();
        field.setAccessible(true);
        try {
            return new Tuple<>(field.get(this.instance), null);
        } catch (final IllegalAccessException ex) {
            field.setAccessible(wasAccessible);
            return new Tuple<>(null, ex);
        }
    }

    /**
     * Checks to see if this field complies with {@code includes}.
     *
     * @param field Field to check
     * @return true if it complies, false if not
     */
    private boolean isSafeToInclude(final Field field, final Tuple<Object, Throwable> value) {
        final String name = field.getName();
        if (value.right == null && this.include.excludeValues.contains(value.left)) {
            return false;
        }
        if (this.include.omitNullValues && value.left == null) {
            return false;
        }
        if (this.include.excludeNames.contains(name) || this.include.excludeClasses.contains(field.getType())) {
            return false;
        }
        if (this.matchesExcludeNameAndClassOrValue(field, value)) {
            return false;
        }
        if (value.right == null && this.include.ensureValues.contains(value.left)) {
            return true;
        }
        if (this.include.ensureNames.contains(name) || this.include.ensureClasses.contains(field.getType())) {
            return true;
        }
        if (this.matchesEnsureNameAndClassOrValue(field, value)) {
            return true;
        }
        final int mods = field.getModifiers();
        boolean include = this.include.privates && Modifier.isPrivate(mods);
        include = include || (this.include.packages && !Modifier.isPrivate(mods) && !Modifier.isPublic(mods) && !Modifier.isProtected(mods));
        include = include || (this.include.protecteds && Modifier.isProtected(mods));
        include = include || (this.include.publics && Modifier.isPublic(mods));
        include = include && (this.include.keepFinals || !Modifier.isFinal(mods));
        include = include && (this.include.keepTransients || !Modifier.isTransient(mods));
        include = include && (this.include.keepVolatiles || !Modifier.isVolatile(mods));
        return include;
    }

    /**
     * Checks if this field and its value match any ensues.
     *
     * @param field Field to check
     * @param value Value to check
     * @return true if ensured, false if not
     */
    private boolean matchesEnsureNameAndClassOrValue(final Field field, final Tuple<Object, Throwable> value) {
        final Class<?> type = this.include.ensureNamesAndClasses.get(field.getName());
        final Tuple<Class<?>, Object> includeValue = this.include.ensureNamesClassesAndValues.get(field.getName());
        final boolean matchesType = type == field.getType();
        final boolean matchesTypeAndValue = includeValue != null && value.right == null && includeValue.left == field.getType() && Objects.equals(includeValue.right, value.left);
        return matchesType || matchesTypeAndValue;
    }

    /**
     * Checks if this field and its value match any excludes.
     *
     * @param field Field to check
     * @param value Value to check
     * @return true if excluded, false if not
     */
    private boolean matchesExcludeNameAndClassOrValue(final Field field, final Tuple<Object, Throwable> value) {
        final Class<?> type = this.include.excludeNamesAndClasses.get(field.getName());
        final Tuple<Class<?>, Object> includeValue = this.include.excludeNamesClassesAndValues.get(field.getName());
        final boolean matchesType = type == field.getType();
        final boolean matchesTypeAndValue = includeValue != null && value.right == null && includeValue.left == field.getType() && Objects.equals(includeValue.right, value.left);
        return matchesType || matchesTypeAndValue;
    }

    /**
     * Provides a comparator for the declared and custom fields, represented by Entries. The key is the name of the
     * field, and the value is the String representation of the value.
     *
     * @param entryComparator Comparator for declared and custom fields
     * @return this
     */
    public ReflectiveToStringHelper entryComparator(final Comparator<? super Entry<String, String>> entryComparator) {
        this.entryComparator = entryComparator;
        return this;
    }

    /**
     * Provides a comparator for the declared fields. To unset, use null.
     *
     * @param fieldComparator Comparator for declared fields
     * @return this
     */
    public ReflectiveToStringHelper fieldComparator(final Comparator<? super Field> fieldComparator) {
        this.fieldComparator = fieldComparator;
        return this;
    }

    /**
     * Produces the String for use with a toString method. This will populate the internal StringBuilder and then clear
     * it.
     *
     * @return Generated string
     */
    public String generate() {
        this.appendClassName();
        this.sb.append("{");
        this.appendFields();
        this.sb.append("}");
        final String generated = this.sb.toString();
        this.sb.setLength(0);
        return generated;
    }

    /**
     * Returns the result of {@link #generate()}.
     *
     * @return Generated toString
     * @see #generate()
     */
    @Override
    public String toString() {
        return this.generate();
    }

    /**
     * Class indicating what should be included by {@link ReflectiveToStringHelper}.
     */
    public static class Include {

        private final List<String> ensureNames = Lists.newArrayList();
        private final List<String> excludeNames = Lists.newArrayList();
        private final List<Class<?>> ensureClasses = Lists.newArrayList();
        private final List<Class<?>> excludeClasses = Lists.newArrayList();
        private final List<Object> ensureValues = Lists.newArrayList();
        private final List<Object> excludeValues = Lists.newArrayList();
        private final Map<String, Class<?>> ensureNamesAndClasses = Maps.newHashMap();
        private final Map<String, Class<?>> excludeNamesAndClasses = Maps.newHashMap();
        private final Map<String, Tuple<Class<?>, Object>> ensureNamesClassesAndValues = Maps.newHashMap();
        private final Map<String, Tuple<Class<?>, Object>> excludeNamesClassesAndValues = Maps.newHashMap();
        private final Map<String, String> mappedNames = Maps.newHashMap();
        private final Map<String, Object> custom = Maps.newHashMap();
        private boolean omitNullValues;
        /**
         * Should public fields be included?
         */
        private boolean publics;
        /**
         * Should protected fields be included?
         */
        private boolean protecteds;
        /**
         * Should package-local fields be included?
         */
        private boolean packages;
        /**
         * Should private fields be included?
         */
        private boolean privates;
        /**
         * Should final fields be kept? Defaults to {@code true}.
         * <p>Note that this only ever removes inclusion. If something does not specifically include fields, this will
         * have no effect.
         */
        private boolean keepFinals = true;
        /**
         * Should transient fields be kept? Default to {@code true}.
         * <p>Note that this only ever removes inclusion. If something does not specifically include fields, this will
         * have no effect.
         */
        private boolean keepTransients = true;
        /**
         * Should volatile fields be kept? Default to {@code true}.
         * <p>Note that this only ever removes inclusion. If something does not specifically include fields, this will
         * have no effect.
         */
        private boolean keepVolatiles = true;

        /**
         * Creates a new, default Include set.
         */
        public Include() {
        }

        /**
         * Creates a new Include set.
         *
         * @param publics    Should public fields be included?
         * @param protecteds Should protected fields be included?
         * @param packages   Should package-local fields be included?
         * @param privates   Should private fields be included?
         * @see ReflectiveToStringHelper
         */
        public Include(final boolean publics, final boolean protecteds, final boolean packages, final boolean privates) {
            this.publics = publics;
            this.protecteds = protecteds;
            this.packages = packages;
            this.privates = privates;
        }

        /**
         * Creates an empty (default) Include set. This includes nothing until told to.
         *
         * @return Empty Include set
         */
        public static Include create() {
            return new Include();
        }

        /**
         * Convenience method to set all visibilities to one value.
         *
         * @param allVisibilities Status of all visibilities
         * @return this
         * @see #publics(boolean)
         * @see #protecteds(boolean)
         * @see #packages(boolean)
         * @see #privates(boolean)
         */
        public Include allVisibilities(final boolean allVisibilities) {
            this.publics = this.protecteds = this.packages = this.privates = allVisibilities;
            return this;
        }

        /**
         * Adds a custom value to the toString.
         *
         * @param key   Name (key) of custom value
         * @param value Value custom key
         * @return this
         * @see #remove(String)
         */
        public Include custom(final String key, final Object value) {
            this.custom.put(key, value);
            return this;
        }

        /**
         * Ensure that any field with the given name, class, and value is shown in the toString.
         *
         * @param name  Name of field to ensure
         * @param clazz Class of field to ensure
         * @param value Required value of field to ensure
         * @return this
         */
        public Include ensure(final String name, final Class<?> clazz, final Object value) {
            this.ensureNamesClassesAndValues.put(name, new Tuple<>(clazz, value));
            return this;
        }

        /**
         * Ensure that any field matching the given class is shown in the toString.
         *
         * @param clazz Class of fields to ensure
         * @return this
         */
        public Include ensure(final Class<?> clazz) {
            this.ensureClasses.add(clazz);
            return this;
        }

        /**
         * Ensures that any field with the given name is shown in the toString.
         *
         * @param name Name of field to ensure
         * @return this
         */
        public Include ensure(final String name) {
            this.ensureNames.add(name);
            return this;
        }

        /**
         * Ensures that any field with the given name and class is shown in the toString.
         *
         * @param name  Name of field to ensure
         * @param clazz Class of field to ensure
         * @return this
         */
        public Include ensure(final String name, final Class<?> clazz) {
            this.ensureNamesAndClasses.put(name, clazz);
            return this;
        }

        /**
         * Ensures that any field with the given value is shown in the toString.
         *
         * @param value Required value to ensure
         * @return this
         */
        public Include ensureValue(final Object value) {
            this.ensureValues.add(value);
            return this;
        }

        /**
         * Excludes any field with the given name, class, and value from the toString.
         *
         * @param name  Name of field to exclude
         * @param clazz Class of field to exclude
         * @param value Required value of field to exclude
         * @return this
         */
        public Include exclude(final String name, final Class<?> clazz, final Object value) {
            this.excludeNamesClassesAndValues.put(name, new Tuple<>(clazz, value));
            return this;
        }

        /**
         * Excludes any field with the given class from the toString.
         *
         * @param clazz Class of fields to exclude
         * @return this
         */
        public Include exclude(final Class<?> clazz) {
            this.excludeClasses.add(clazz);
            return this;
        }

        /**
         * Excludes any field with the given name from the toString.
         *
         * @param name Name of field to exclude
         * @return this
         */
        public Include exclude(final String name) {
            this.excludeNames.add(name);
            return this;
        }

        /**
         * Excludes any field with the given name and class from the toString.
         *
         * @param name  Name of field to exclude
         * @param clazz Class of field to exclude
         * @return this
         */
        public Include exclude(final String name, final Class<?> clazz) {
            this.excludeNamesAndClasses.put(name, clazz);
            return this;
        }

        /**
         * Excludes any field with the given field from the toString.
         *
         * @param value Required value to exclude
         * @return this
         */
        public Include excludeValue(final Object value) {
            this.excludeValues.add(value);
            return this;
        }

        /**
         * Ignores (or forgets) any field with the given class.
         * <p>This negates the effect of the matching {@code ensure} method.
         *
         * @param clazz Class to ignore
         * @return this
         */
        public Include ignore(final Class<?> clazz) {
            this.ensureClasses.remove(clazz);
            this.excludeClasses.remove(clazz);
            return this;
        }

        /**
         * Ignores (or forgets) any field with the given name.
         * <p>This negates the effect of the matching {@code ensure} method.
         *
         * @param name Name to ignore
         * @return this
         */
        public Include ignore(final String name) {
            this.ensureNames.remove(name);
            this.excludeNames.remove(name);
            this.ensureNamesAndClasses.remove(name);
            this.excludeNamesAndClasses.remove(name);
            return this;
        }

        /**
         * Ignores (or forgets) any field with the given value.
         * <p>This negates the effect of the matching {@code ensure} method.
         *
         * @param value Required value to ignore
         * @return this
         */
        public Include ignoreValue(final Object value) {
            this.ensureValues.remove(value);
            this.excludeValues.remove(value);
            return this;
        }

        /**
         * Handles the processing of final fields.
         * <p>If finals are to be kept (true), no discernible change will occur in the toString.
         * <p>If finals are not to be kept (false), any final field that would have appeared will now be excluded.
         *
         * @param keepFinals Status of keeping final fields
         * @return this
         */
        public Include keepFinals(final boolean keepFinals) {
            this.keepFinals = keepFinals;
            return this;
        }

        /**
         * Handles the processing of transient fields.
         * <p>If transients are to be kept (true), no discernible change will occur in the toString.
         * <p>If transients are not to be kept (false), any transient field that would have appeared will now be
         * excluded.
         *
         * @param keepTransients Status of keeping transient fields
         * @return this
         */
        public Include keepTransients(final boolean keepTransients) {
            this.keepTransients = keepTransients;
            return this;
        }

        /**
         * Handles the processing of volatile fields.
         * <p>If volatiles are to be kept (true), no discernible change will occur in the toString.
         * <p>If volatiles are not to be kept (false), any volatile field that would have appeared will now be excluded.
         *
         * @param keepVolatiles Status of keeping final fields
         * @return this
         */
        public Include keepVolatiles(final boolean keepVolatiles) {
            this.keepVolatiles = keepVolatiles;
            return this;
        }

        /**
         * Maps the name of a field to something new in the toString.
         *
         * @param originalName Name of the field to map to something else
         * @param newName      New name to appear in the toString
         * @return this
         */
        public Include map(final String originalName, final String newName) {
            this.mappedNames.put(originalName, newName);
            return this;
        }

        /**
         * Handles the appropriate omission of fields with values that are null.
         * <p>If null-value fields are to be omitted (true), any field which has a value equating to null will not
         * appear in the toString.
         * <p>If null-value fields are to be kept (false, default), the toString will have no discernible change.
         *
         * @param omitNullValues Status of omitting null-value fields
         * @return this
         */
        public Include omitNullValues(final boolean omitNullValues) {
            this.omitNullValues = omitNullValues;
            return this;
        }

        /**
         * Handles the processing of package-local fields.
         * <p>If package-local fields are to be included (true), they will appear in the toString.
         * <p>If package-local fields are to be excluded (false), they will <em>not</em> appear in the toString.
         *
         * @param packages Status of including package-local fields
         * @return this
         */
        public Include packages(final boolean packages) {
            this.packages = packages;
            return this;
        }

        /**
         * Handles the processing of private fields.
         * <p>If private fields are to be included (true), they will appear in the toString.
         * <p>If private fields are to be excluded (false), they will <em>not</em> appear in the toString.
         *
         * @param privates Status of including private fields
         * @return this
         */
        public Include privates(final boolean privates) {
            this.privates = privates;
            return this;
        }

        /**
         * Handles the processing of protected fields.
         * <p>If protected fields are to be included (true), they will appear in the toString.
         * <p>If protected fields are to be excluded (false), they will <em>not</em> appear in the toString.
         *
         * @param protecteds Status of including package-local fields
         * @return this
         */
        public Include protecteds(final boolean protecteds) {
            this.protecteds = protecteds;
            return this;
        }

        /**
         * Handles the processing of public fields.
         * <p>If public fields are to be included (true), they will appear in the toString.
         * <p>If public fields are to be excluded (false), they will <em>not</em> appear in the toString.
         *
         * @param publics Status of including package-local fields
         * @return this
         */
        public Include publics(final boolean publics) {
            this.publics = publics;
            return this;
        }

        /**
         * The functional opposite of {@link #custom(String, Object)}. This removes a custom value.
         *
         * @param key Key to remove
         * @return this
         * @see #custom(String, Object)
         */
        public Include remove(final String key) {
            this.custom.remove(key);
            return this;
        }

        /**
         * Removes the mapping of the name of a field.
         *
         * @param originalName Name of the field that was mapped
         * @return this
         */
        public Include unmap(final String originalName) {
            this.mappedNames.remove(originalName);
            return this;
        }

    }

    private static class Tuple<L, R> {

        private final L left;
        private final R right;

        private Tuple(final L left, final R right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Tuple)) return false;
            final Tuple other = (Tuple) obj;
            return Objects.equals(this.left, other.left) && Objects.equals(this.right, other.right);
        }
    }


}
