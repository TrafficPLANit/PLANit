package org.planit.network.physical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedEdge;
import org.planit.utils.graph.EdgeSegments;
import org.planit.utils.graph.Vertex;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.LinkSegments;

/**
 * 
 * Links implementation wrapper that simply utilises passed in edges of the desired generic type to delegate registration and creation of its links on
 * 
 * @author markr
 *
 * @param <LS>
 */
public class LinkSegmentsImpl<LS extends LinkSegment> implements LinkSegments<LS> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(LinkSegmentsImpl.class.getCanonicalName());

  /**
   * The edge segments we use to create and register our link segments on
   */
  private final EdgeSegments<LS> edgeSegments;

  /**
   * Map to store all link segments for a given start node Id
   */
  private Map<Long, List<LS>> linkSegmentMapByStartNodeId = new HashMap<Long, List<LS>>();

  /**
   * Register a link segment on the network
   *
   * @param linkSegment the link segment to be registered
   */
  protected void registerStartNode(final LS linkSegment) {
    final Vertex startNode = linkSegment.getUpstreamVertex();
    if (!linkSegmentMapByStartNodeId.containsKey(startNode.getId())) {
      linkSegmentMapByStartNodeId.put(startNode.getId(), new ArrayList<LS>());
    }
    linkSegmentMapByStartNodeId.get(startNode.getId()).add(linkSegment);
  }

  /**
   * Constructor
   * 
   * @param edgeSegments to use to create and register link segments on
   */
  public LinkSegmentsImpl(final EdgeSegments<LS> edgeSegments) {
    this.edgeSegments = edgeSegments;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remove(LS linkSegment) {
    edgeSegments.remove(linkSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remove(long edgeSegmentId) {
    edgeSegments.remove(edgeSegmentId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LS create(DirectedEdge parent, boolean directionAB) throws PlanItException {
    return edgeSegments.create(parent, directionAB);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void register(DirectedEdge parentEdge, LS edgeSegment, boolean directionAB) throws PlanItException {
    edgeSegments.register(parentEdge, edgeSegment, directionAB);
    registerStartNode(edgeSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LS get(long id) {
    return edgeSegments.get(id);
  }
  
  /**
   * Return an edge segment by its Xml id
   * 
   * Note: not an efficient implementation since it loops over all link segments in linear time to identify the correct one, preferably use get instead whenever possible.
   * 
   * @param xmlid the XML id of the edge segment
   * @return the specified edge segment instance
   */
  @Override
  public LS getByXmlId(String xmlId) {
    return edgeSegments.getByXmlId(xmlId);
  }  

  /**
   * {@inheritDoc}
   */
  @Override
  public long size() {
    return edgeSegments.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LS registerUniqueCopyOf(LS edgeSegmentToCopy, DirectedEdge newParent) {
    LS linkSegment = edgeSegments.registerUniqueCopyOf(edgeSegmentToCopy, newParent);
    registerStartNode(linkSegment);
    return linkSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<LS> iterator() {
    return edgeSegments.iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LS registerNew(DirectedEdge parentEdge, boolean directionAb, boolean registerOnNodeAndLink) throws PlanItException {
    LS linkSegment = edgeSegments.registerNew(parentEdge, directionAb, registerOnNodeAndLink);
    registerStartNode(linkSegment);
    return linkSegment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LS getByExternalId(String externalId) {
    for (LS linkSegment : edgeSegments) {
      if (externalId.equals(linkSegment.getExternalId())) {
        return linkSegment;
      }
    }
    return null;
  }


}
