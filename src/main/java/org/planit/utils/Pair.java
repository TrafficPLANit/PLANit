package org.planit.utils;

import java.util.Objects;

/**
 * Custom pair class similar to C++. By default we compare based on the first value
 * @author markr
 *
 * @param <A>
 * @param <B>
 */
public class Pair<A extends Comparable<A>, B extends Comparable<B>> implements Comparable<Pair<A,B>> {
    	
	protected final A first;
    protected final B second;

    /** Constructor 
     * @param first
     * @param second
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
    	return Objects.hash(first,second);
    }

    /** compare to pairs
     * @see java.lang.Object#equals(java.lang.Object)
     * @param other pair
     */
    public boolean equals(Object other) {
        if (other instanceof Pair) {
            @SuppressWarnings("rawtypes")
			Pair otherPair = (Pair) other;
            return 
            ((  this.first == otherPair.first ||
                ( this.first != null && otherPair.first != null &&
                  this.first.equals(otherPair.first))) &&
             (  this.second == otherPair.second ||
                ( this.second != null && otherPair.second != null &&
                  this.second.equals(otherPair.second))) );
        }
        return false;
    }

    /** convert to string
     * @see java.lang.Object#toString()
     */
    public String toString()
    { 
           return "(" + first + ", " + second + ")"; 
    }
    
    // Getters

    public A getFirst() {
        return first;
    }

    public B getSecond() {
        return second;
    }

	/** Compare based on first entry of pair
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Pair<A, B> o) {
		return getFirst().compareTo(((Pair<A, B>) o).getFirst());
	}

}