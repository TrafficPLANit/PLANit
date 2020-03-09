package org.planit.network.physical;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
import org.planit.graph.EdgeImpl;
import org.planit.utils.misc.IdGenerator;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Node;

/**
 * Link class connecting two nodes via some geometry. Each link has one or two
 * underlying link segments in a particular direction which may carry additional
 * information for each particular direction of the link.
 *
 * @author markr
 *
 */
public class LinkImpl extends EdgeImpl implements Link {

    // Protected

    /** generated UID */
	 private static final long serialVersionUID = 2360017879557363410L;

	/**
     * unique internal identifier
     */
    protected final long linkId;

    /**
     * External Id of the physical link
     */
    protected Object externalId;

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
     * @param nodeA the first node in the link
     * @param nodeB the second node in the link
     * @param length the length of the link
     * @param name the name of the link
     * @throws PlanItException thrown if there is an error
     */
    public LinkImpl(@Nonnull final Node nodeA, @Nonnull final Node nodeB, final double length, final String name) throws PlanItException {
        super(nodeA, nodeB, length);
        this.linkId = generateLinkId();
    }

    /**
     * Register linkSegment. If there already exists a linkSegment for that
     * direction it is replaced and returned
     *
     * @param linkSegment the link segment to be registered
     * @param directionAB direction of travel
     * @return the replaced LinkSegment
     * @throws PlanItException thrown if there is an error
     */
    @Override
    public LinkSegment registerLinkSegment(final LinkSegment linkSegment, final boolean directionAB) throws PlanItException {
          return (LinkSegment) registerEdgeSegment(linkSegment, directionAB);
    }

    // Getters-Setters

    @Override
  	public long getLinkId() {
          return linkId;
    }

    @Override
  	public void setExternalId(final Object externalId) {
          this.externalId = externalId;
    }

    @Override
  	public Object getExternalId() {
          return externalId;
    }
    
    @Override
    public boolean hasExternalId() {
      return (externalId != null);
    }

}