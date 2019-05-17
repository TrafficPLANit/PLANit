package org.planit.utils;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Custom pair class similar to C++. By default we compare based on the first
 * value
 * 
 * @author markr
 *
 * @param <A>
 *            first object in pair
 * @param <B>
 *            second object in pair
 */
public class Pair<A, B> {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(Pair.class.getName());

    /**
     * The first object in this Pair
     */
    protected final A first;

    /**
     * The second object in this Pair
     */
    protected final B second;

    /**
     * Constructor
     * 
     * @param first
     *            first object in pair
     * @param second
     *            second object in pair
     */
    public Pair(A first, B second) {
        super();
        this.first = first;
        this.second = second;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return Objects.hash(first, second);
    }

    /**
     * Compare to another pair
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     * @param other
     *            pair being compared to
     */
    public boolean equals(Object other) {
        if (other instanceof Pair) {
            @SuppressWarnings("rawtypes")
            Pair otherPair = (Pair) other;
            return ((this.first == otherPair.first
                    || (this.first != null && otherPair.first != null && this.first.equals(otherPair.first)))
                    && (this.second == otherPair.second || (this.second != null && otherPair.second != null
                            && this.second.equals(otherPair.second))));
        }
        return false;
    }

    /**
     * Convert to string
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "(" + first + ", " + second + ")";
    }

    // Getters

    /**
     * Get first object in pair
     * 
     * @return first object in pair
     */
    public A getFirst() {
        return first;
    }

    /**
     * Get second object in pair
     * 
     * @return second object in pair
     */
    public B getSecond() {
        return second;
    }

}