package de.erdbeerbaerlp.dcintegration.common.util;

import java.util.ArrayList;
import java.util.List;

/**
     * A module's version string.
     *
     * <p> A version string has three components: The version number itself, an
     * optional pre-release version, and an optional build version.  Each
     * component is a sequence of tokens; each token is either a non-negative
     * integer or a string.  Tokens are separated by the punctuation characters
     * {@code '.'}, {@code '-'}, or {@code '+'}, or by transitions from a
     * sequence of digits to a sequence of characters that are neither digits
     * nor punctuation characters, or vice versa.  Consecutive repeated
     * punctuation characters are treated as a single punctuation character.
     *
     * <ul>
     *
     *   <li> The <i>version number</i> is a sequence of tokens separated by
     *   {@code '.'} characters, terminated by the first {@code '-'} or {@code
     *   '+'} character. </li>
     *
     *   <li> The <i>pre-release version</i> is a sequence of tokens separated
     *   by {@code '.'} or {@code '-'} characters, terminated by the first
     *   {@code '+'} character. </li>
     *
     *   <li> The <i>build version</i> is a sequence of tokens separated by
     *   {@code '.'}, {@code '-'}, or {@code '+'} characters.
     *
     * </ul>
     *
     * <p> When comparing two version strings, the elements of their
     * corresponding components are compared in pointwise fashion.  If one
     * component is longer than the other, but otherwise equal to it, then the
     * first component is considered the greater of the two; otherwise, if two
     * corresponding elements are integers then they are compared as such;
     * otherwise, at least one of the elements is a string, so the other is
     * converted into a string if it is an integer and the two are compared
     * lexicographically.  Trailing integer elements with the value zero are
     * ignored.
     *
     * <p> Given two version strings, if their version numbers differ then the
     * result of comparing them is the result of comparing their version
     * numbers; otherwise, if one of them has a pre-release version but the
     * other does not then the first is considered to precede the second,
     * otherwise the result of comparing them is the result of comparing their
     * pre-release versions; otherwise, the result of comparing them is the
     * result of comparing their build versions.
     *
     * @see ModuleDescriptor#version()
     * @since 9
     */

    public final class Version
        implements Comparable<Version>
    {

        private final String version;

        // If Java had disjunctive types then we'd write List<Integer|String> here
        //
        private final List<Object> sequence;
        private final List<Object> pre;
        private final List<Object> build;

        // Take a numeric token starting at position i
        // Append it to the given list
        // Return the index of the first character not taken
        // Requires: s.charAt(i) is (decimal) numeric
        //
        private static int takeNumber(String s, int i, List<Object> acc) {
            char c = s.charAt(i);
            int d = (c - '0');
            int n = s.length();
            while (++i < n) {
                c = s.charAt(i);
                if (c >= '0' && c <= '9') {
                    d = d * 10 + (c - '0');
                    continue;
                }
                break;
            }
            acc.add(d);
            return i;
        }

        // Take a string token starting at position i
        // Append it to the given list
        // Return the index of the first character not taken
        // Requires: s.charAt(i) is not '.'
        //
        private static int takeString(String s, int i, List<Object> acc) {
            int b = i;
            int n = s.length();
            while (++i < n) {
                char c = s.charAt(i);
                if (c != '.' && c != '-' && c != '+' && !(c >= '0' && c <= '9'))
                    continue;
                break;
            }
            acc.add(s.substring(b, i));
            return i;
        }

        // Syntax: tok+ ( '-' tok+)? ( '+' tok+)?
        // First token string is sequence, second is pre, third is build
        // Tokens are separated by '.' or '-', or by changes between alpha & numeric
        // Numeric tokens are compared as decimal integers
        // Non-numeric tokens are compared lexicographically
        // A version with a non-empty pre is less than a version with same seq but no pre
        // Tokens in build may contain '-' and '+'
        //
        private Version(String v) {

            if (v == null)
                throw new IllegalArgumentException("Null version string");
            int n = v.length();
            if (n == 0)
                throw new IllegalArgumentException("Empty version string");

            int i = 0;
            char c = v.charAt(i);
            if (!(c >= '0' && c <= '9'))
                throw new IllegalArgumentException(v
                                                   + ": Version string does not start"
                                                   + " with a number");

            List<Object> sequence = new ArrayList<>(4);
            List<Object> pre = new ArrayList<>(2);
            List<Object> build = new ArrayList<>(2);

            i = takeNumber(v, i, sequence);

            while (i < n) {
                c = v.charAt(i);
                if (c == '.') {
                    i++;
                    continue;
                }
                if (c == '-' || c == '+') {
                    i++;
                    break;
                }
                if (c >= '0' && c <= '9')
                    i = takeNumber(v, i, sequence);
                else
                    i = takeString(v, i, sequence);
            }

            if (c == '-' && i >= n)
                throw new IllegalArgumentException(v + ": Empty pre-release");

            while (i < n) {
                c = v.charAt(i);
                if (c == '.' || c == '-') {
                    i++;
                    continue;
                }
                if (c == '+') {
                    i++;
                    break;
                }
                if (c >= '0' && c <= '9')
                    i = takeNumber(v, i, pre);
                else
                    i = takeString(v, i, pre);
            }

            if (c == '+' && i >= n)
                throw new IllegalArgumentException(v + ": Empty pre-release");

            while (i < n) {
                c = v.charAt(i);
                if (c == '.' || c == '-' || c == '+') {
                    i++;
                    continue;
                }
                if (c >= '0' && c <= '9')
                    i = takeNumber(v, i, build);
                else
                    i = takeString(v, i, build);
            }

            this.version = v;
            this.sequence = sequence;
            this.pre = pre;
            this.build = build;
        }

        /**
         * Parses the given string as a version string.
         *
         * @param  v
         *         The string to parse
         *
         * @return The resulting {@code Version}
         *
         * @throws IllegalArgumentException
         *         If {@code v} is {@code null}, an empty string, or cannot be
         *         parsed as a version string
         */
        public static Version parse(String v) {
            return new Version(v);
        }

        @SuppressWarnings("unchecked")
        private int cmp(Object o1, Object o2) {
            return ((Comparable)o1).compareTo(o2);
        }

        private int compareTokens(List<Object> ts1, List<Object> ts2) {
            int n = Math.min(ts1.size(), ts2.size());
            for (int i = 0; i < n; i++) {
                Object o1 = ts1.get(i);
                Object o2 = ts2.get(i);
                if ((o1 instanceof Integer && o2 instanceof Integer)
                    || (o1 instanceof String && o2 instanceof String))
                {
                    int c = cmp(o1, o2);
                    if (c == 0)
                        continue;
                    return c;
                }
                // Types differ, so convert number to string form
                int c = o1.toString().compareTo(o2.toString());
                if (c == 0)
                    continue;
                return c;
            }
            List<Object> rest = ts1.size() > ts2.size() ? ts1 : ts2;
            int e = rest.size();
            for (int i = n; i < e; i++) {
                Object o = rest.get(i);
                if (o instanceof Integer && ((Integer)o) == 0)
                    continue;
                return ts1.size() - ts2.size();
            }
            return 0;
        }

        /**
         * Compares this module version to another module version. Module
         * versions are compared as described in the class description.
         *
         * @param that
         *        The module version to compare
         *
         * @return A negative integer, zero, or a positive integer as this
         *         module version is less than, equal to, or greater than the
         *         given module version
         */
        @Override
        public int compareTo(Version that) {
            int c = compareTokens(this.sequence, that.sequence);
            if (c != 0) return c;
            if (this.pre.isEmpty()) {
                if (!that.pre.isEmpty()) return +1;
            } else {
                if (that.pre.isEmpty()) return -1;
            }
            c = compareTokens(this.pre, that.pre);
            if (c != 0) return c;
            return compareTokens(this.build, that.build);
        }

        /**
         * Tests this module version for equality with the given object.
         *
         * <p> If the given object is not a {@code Version} then this method
         * returns {@code false}. Two module version are equal if their
         * corresponding components are equal. </p>
         *
         * <p> This method satisfies the general contract of the {@link
         * java.lang.Object#equals(Object) Object.equals} method. </p>
         *
         * @param   ob
         *          the object to which this object is to be compared
         *
         * @return  {@code true} if, and only if, the given object is a module
         *          reference that is equal to this module reference
         */
        @Override
        public boolean equals(Object ob) {
            if (!(ob instanceof Version))
                return false;
            return compareTo((Version)ob) == 0;
        }

        /**
         * Computes a hash code for this module version.
         *
         * <p> The hash code is based upon the components of the version and
         * satisfies the general contract of the {@link Object#hashCode
         * Object.hashCode} method. </p>
         *
         * @return The hash-code value for this module version
         */
        @Override
        public int hashCode() {
            return version.hashCode();
        }

        /**
         * Returns the string from which this version was parsed.
         *
         * @return The string from which this version was parsed.
         */
        @Override
        public String toString() {
            return version;
        }

    }