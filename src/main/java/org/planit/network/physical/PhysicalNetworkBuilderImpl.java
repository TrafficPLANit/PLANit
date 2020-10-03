package org.planit.network.physical;

import java.util.Map;

import org.planit.graph.EdgesImpl;
import org.planit.graph.VerticesImpl;
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
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.physical.macroscopic.MacroscopicModeProperties;

/**
 * Create network entities for a physical network simulation model
 * 
 * @author markr
 *
 */
public class PhysicalNetworkBuilderImpl implements PhysicalNetworkBuilder<Node, Link, LinkSegment> {

  /**
   * Contiguous id generation within this group id token for all instances created with factory methods in this class
   */
  protected IdGroupingToken groupId;

  /**
   * Remove any id gaps present in the passed in links by updating their ids (if the edges are of the implementation compatible with this builder).
   * The working assumption is that while ids might be missing, links are registered as a single blokc, i.e., all edges within the available id range of links are either
   * links or are unused ids. If this is not the case, this method cannot be used to removed idGaps since the underlying edge ids are also updated based on this approach. If
   * edge ids are also used for anythong else than links (connectoids), than these other entities ids should also subsequently be updated and they should not interleave with
   * each other before updating the ids. 
   * 
   * The id generator ids for EDGE and LINK are reset and repopulated while re-registering all links as edges on the network.
   * 
   * @param links to create contiguous ids fro starting with zero
   */
  @SuppressWarnings("unchecked")
  protected void removeIdGaps(Edges<Link> links) {   
    /* only proceed when implementation of edges and link is compatible with this builder */
    if( !links.isEmpty() && links instanceof EdgesImpl && 
        links.iterator().next() instanceof MultiIdSetter<?>) {
      
      /* prep */
      Link prevAvailableLink = links.iterator().next();
      long previousId = prevAvailableLink.getId();
      long currId = previousId+1;
      
      IdGroupingToken token = getIdGroupingToken();      
      boolean firstGap = true;
      /* end prep */
      
      while(currId < links.size()) {
        Link currLink = links.get(currId);
        
        if(currLink!=null) {         
          if(currLink.getId() != prevAvailableLink.getId()+1) {
            
            /* first gap, reset id generator's offset */
            if(firstGap) {
              IdGenerator.resetTo(token, Edge.class, prevAvailableLink.getId());
              IdGenerator.resetTo(token, Link.class, prevAvailableLink.getLinkId());
              firstGap = false;
            }
            
            /* unregister link before re-registering with updated ids */
            links.remove(currLink);
            /* update internal id and link id, and re-register based on overwritten ids */
            
            CONTINUE -> SPLIT IN TWO -> CREATE GRAPHBUILDER THAT ALLOWS FOR REMOVING GAPS FROM EDGES VIA EDGE IMPL -> DO THAT FIRST IN GENERAL ONLY THEN CALL THIS METHOD
            THIS METHOD ONlY UPDATES THE LINKS --> REMOVE IDSETTERS -> JUST MAKE IT PROTECTED SINCE WE CAN ACCESS IT :)
            
            ((MultiIdSetter<Long>)currLink).overwriteIds(LinkImpl.generateEdgeId(token), LinkImpl.generateLinkId(token));
            ((EdgesImpl<Link>)links).register(currLink);          
          }else {
            /* not missing, update prevAvailableLink */
            prevAvailableLink = currLink;  
          }
        }

        previousId = currId;
        ++currId;
      }
    }
  }

  /**
   * Remove any id gaps present in the passed in nodes by updating their ids if the edges are of the implementation compatible with this builder
   * 
   * @param nodes to create contiguous ids for starting from zero
   */
  @SuppressWarnings("unchecked")
  protected void removeIdGaps(Vertices<Node> nodes) {
    // TODO
  }

  /** todo */
  protected void removeIdGaps(EdgeSegments<LinkSegment> edgeSegments) {
    // TODO
  }

  // Public methods

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
  public LinkSegment createEdgeSegment(Edge parentLink, boolean directionAB) throws PlanItException {
    return new LinkSegmentImpl(getIdGroupingToken(), (Link) parentLink, directionAB);
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
  public void removeIdGaps(DirectedGraph<Node, Link, LinkSegment> directedGraph) {
    this.removeIdGaps((Graph<Node, Link>) directedGraph);
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
