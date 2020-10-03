package org.planit.network.physical.macroscopic;

import java.util.Map;

import org.planit.graph.EdgesImpl;
import org.planit.graph.VerticesImpl;
import org.planit.network.physical.LinkImpl;
import org.planit.network.physical.NodeImpl;
import org.planit.network.physical.PhysicalNetworkBuilderImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedGraph;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegments;
import org.planit.utils.graph.Edges;
import org.planit.utils.graph.Graph;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.Vertices;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.IdSetter;
import org.planit.utils.id.MultiIdSetter;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.physical.macroscopic.MacroscopicModeProperties;

/**
 * Create network entities for a macroscopic simulation model
 * 
 * @author markr
 *
 */
public class MacroscopicPhysicalNetworkBuilderImpl extends PhysicalNetworkBuilderImpl implements MacroscopicPhysicalNetworkBuilder {

 
  
  /**
   * Remove any id gaps present in the passed in macroscopic link segments by updating their ids if the edges are of the implementation compatible with this builder
   * 
   * @param vertices
   */   
  protected void removeIdGaps(EdgeSegments<MacroscopicLinkSegment> linkSegments) {
    //TODO
  }  

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegment createEdgeSegment(Edge parentLink, boolean directionAB) throws PlanItException {
    return new MacroscopicLinkSegmentImpl(getIdGroupingToken(), (Link) parentLink, directionAB);
  }

  /**
   * {@inheritDoc}
   */
  public MacroscopicLinkSegmentType createLinkSegmentType(String name, double capacity, double maximumDensity, Object externalId,
      Map<Mode, MacroscopicModeProperties> modeProperties) {
    return new MacroscopicLinkSegmentTypeImpl(getIdGroupingToken(), name, capacity, maximumDensity, externalId, modeProperties);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentType createLinkSegmentType(String name, double capacity, double maximumDensity, Object externalId) {
    return new MacroscopicLinkSegmentTypeImpl(getIdGroupingToken(), name, capacity, maximumDensity, externalId);
  }


  /**
   * {@inheritDoc}
   */  
  @Override
  public void removeIdGaps(DirectedGraph<Node, Link, MacroscopicLinkSegment> directedGraph) {
    this.removeIdGaps((Graph<Node, Link>)directedGraph);
    removeIdGaps(directedGraph.getEdgeSegments());
  }

}
