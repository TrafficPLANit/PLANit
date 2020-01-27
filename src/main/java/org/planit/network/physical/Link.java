package org.planit.network.physical;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
import org.planit.network.EdgeImpl;
import org.planit.utils.misc.IdGenerator;

/**
 * Link class connecting two nodes via some geometry. Each link has one or two
 * underlying link segments in a particular direction which may carry additional
 * information for each particular direction of the link.
 * 
 * @author markr
 *
 */
public class Link extends EdgeImpl {

    // Protected

    /**
     * unique internal identifier
     */
    protected final long linkId;

    /**
     * External Id of the physical link
     */
    protected long externalId;

    /**
     * generate unique link id
     * 
     * @return linkId
     */
    protected static long generateLinkId() {
        return IdGenerator.generateId(Link.class);
    }

    // Public

    /**
     * Constructor which injects link length directly
     * 
     * @param nodeA
     *            the first node in the link
     * @param nodeB
     *            the second node in the link
     * @param length
     *            the length of the link
     * @throws PlanItException
     *             thrown if there is an error
     */
    public Link(@Nonnull Node nodeA, @Nonnull Node nodeB, double length) throws PlanItException {
        super(nodeA, nodeB, length);
        this.linkId = generateLinkId();
    }

    /**
     * Register linkSegment. If there already exists a linkSegment for that
     * direction it is replaced and returned
     * 
     * @param linkSegment
     *            the link segment to be registered
     * @param directionAB
     *            direction of travel
     * @return the replaced LinkSegment
     * @throws PlanItException
     *             thrown if there is an error
     */
    public LinkSegment registerLinkSegment(LinkSegment linkSegment, boolean directionAB) throws PlanItException {
        return (LinkSegment) registerEdgeSegment(linkSegment, directionAB);
    }

    // Getters-Setters

    public long getLinkId() {
        return linkId;
    }

    public void setExternalId(long externalId) {
        this.externalId = externalId;
    }

    public long getExternalId() {
        return externalId;
    }
}
