package org.planit.network.physical.macroscopic;

import java.util.Map;

import org.planit.graph.EdgesImpl;
import org.planit.network.physical.LinkImpl;
import org.planit.network.physical.NodeImpl;
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
public class MacroscopicPhysicalNetworkBuilderImpl implements MacroscopicPhysicalNetworkBuilder {

  /**
   * Contiguous id generation within this group id token for all instances created with factory methods in this class
   */
  protected IdGroupingToken groupId;
  
  /**
   * Remove any id gaps present in the passed in links by updating their ids if the edges are of the implementation compatible with this builder
   * 
   * @param links
   */
  @SuppressWarnings("unchecked")
  protected void removeIdGaps(Edges<Link> links) {
    
    /* only proceed when implementation of edges and link is compatible with this builder */
    if( !links.isEmpty() && links instanceof EdgesImpl && 
        links.iterator().next() instanceof MultiIdSetter<?>) {
      
      Link prevAvailableLink = null;
      long previousId = -1;
      long currId = 0;
      while(currId < links.size()) {
        Link currLink = links.get(currId);
        
        if(prevAvailableLink.getId() != previousId && currLink!=null) {
          /* unregister link before re-registering with updated ids */
          links.remove(currLink);
          /* update internal id and link id, and re-register based on overwritten ids */
          ((MultiIdSetter<Long>)currLink).overwriteIds(prevAvailableLink.getId()+1, prevAvailableLink.getLinkId()+1);
          ((EdgesImpl<Link>)links).register(currLink);          
        }
                
        if(links.get(currId)!=null) {
          /* not missing, update prevAvailableLink */
          prevAvailableLink = currLink;           
        }
        previousId = currId;
        ++currId;
      }
    }
  }  
  
  /**
   * Remove any id gaps present in the passed in nodes by updating their ids if the edges are of the implementation compatible with this builder
   * 
   * @param nodes
   */  
  protected void removeIdGaps(Vertices<Node> nodes) {
    TODO
  }  
  
  /**
   * Remove any id gaps present in the passed in macroscopic link segments by updating their ids if the edges are of the implementation compatible with this builder
   * 
   * @param vertices
   */   
  protected void removeIdGaps(EdgeSegments<MacroscopicLinkSegment> linkSegments) {
    TODO
  }  

  /**
   * {@inheritDoc}
   */
  @Override
  public Node createVertex() {
    return new NodeImpl(groupId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Link createEdge(Vertex nodeA, Vertex nodeB, final double length) throws PlanItException {
    return new LinkImpl(groupId, (Node) nodeA, (Node) nodeB, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegment createEdgeSegment(Edge parentLink, boolean directionAB) throws PlanItException {
    return new MacroscopicLinkSegmentImpl(groupId, (Link) parentLink, directionAB);
  }

  /**
   * {@inheritDoc}
   */
  public MacroscopicLinkSegmentType createLinkSegmentType(String name, double capacity, double maximumDensity, Object externalId,
      Map<Mode, MacroscopicModeProperties> modeProperties) {
    return new MacroscopicLinkSegmentTypeImpl(groupId, name, capacity, maximumDensity, externalId, modeProperties);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegmentType createLinkSegmentType(String name, double capacity, double maximumDensity, Object externalId) {
    return new MacroscopicLinkSegmentTypeImpl(groupId, name, capacity, maximumDensity, externalId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setIdGroupingToken(IdGroupingToken groupId) {
    this.groupId = groupId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IdGroupingToken getIdGroupingToken() {
    return this.groupId;
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public void removeIdGaps(DirectedGraph<Node, Link, MacroscopicLinkSegment> directedGraph) {
    this.removeIdGaps((Graph<Node, Link>)directedGraph);
    removeIdGaps(directedGraph.getEdgeSegments());
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public void removeIdGaps(Graph<Node, Link> graph) {
    removeIdGaps(graph.getEdges());
    removeIdGaps(graph.getVertices());
  }

}
