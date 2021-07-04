package org.planit.network.layer.physical;

import java.util.logging.Logger;

import org.planit.graph.DirectedGraphBuilderImpl;
import org.planit.graph.VerticesImpl;
import org.planit.graph.directed.EdgeSegmentsImpl;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.Vertices;
import org.planit.utils.graph.directed.EdgeSegments;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.physical.Link;
import org.planit.utils.network.layer.physical.LinkSegment;
import org.planit.utils.network.layer.physical.Node;
import org.planit.utils.network.layer.physical.PhysicalNetworkLayerBuilder;

/**
 * Create network entities for a physical network simulation model
 * 
 * @author markr
 *
 */
public class PhysicalLayerBuilderImpl implements PhysicalNetworkLayerBuilder<Node, Link, LinkSegment> {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PhysicalLayerBuilderImpl.class.getCanonicalName());

  /** hold an implementation of directed graph builder to use its overlapping functionality */
  protected DirectedGraphBuilderImpl directedGraphBuilderImpl;

  // Public methods

  /**
   * Constructor
   * 
   * @param tokenId to use for id generation
   */
  public PhysicalLayerBuilderImpl(IdGroupingToken tokenId) {
    this.directedGraphBuilderImpl = new DirectedGraphBuilderImpl(tokenId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NodeImpl createVertex() {
    return new NodeImpl(getIdGroupingToken());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(EdgeSegments<? extends EdgeSegment> linkSegments) {
    directedGraphBuilderImpl.recreateIds(linkSegments);

    /* conduct linkIds ourselves since it is a physical network add-on */
    if (linkSegments instanceof EdgeSegmentsImpl<?>) {
      /* remove gaps by simply resetting and recreating all node ids */
      IdGenerator.reset(getIdGroupingToken(), LinkSegment.class);

      for (EdgeSegment linkSegment : linkSegments) {
        if (linkSegment instanceof LinkSegmentImpl) {
          ((LinkSegmentImpl) linkSegment).setLinkSegmentId(LinkSegmentImpl.generateLinkSegmentId(getIdGroupingToken()));
        } else {
          LOGGER.severe(String.format("Attempting to reset id on link segment (%s) that is not compatible with the node implementation generated by this builder, ignored",
              linkSegment.getClass().getCanonicalName()));
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(Vertices<?> nodes) {
    /* delegate for vertex ids */
    directedGraphBuilderImpl.recreateIds(nodes);

    /* conduct linkIds ourselves since it is a physical network add-on */
    if (nodes instanceof VerticesImpl<?>) {
      /* remove gaps by simply resetting and recreating all node ids */
      IdGenerator.reset(getIdGroupingToken(), Vertex.class);

      for (Vertex node : nodes) {
        if (node instanceof NodeImpl) {
          ((NodeImpl) node).setNodeId(NodeImpl.generateNodeId(getIdGroupingToken()));
        } else {
          LOGGER.severe(String.format("attempting to reset id on node (%s) that is not compatible with the node implementation generated by this builder, ignored",
              node.getClass().getCanonicalName()));
        }
      }
    } else {
      LOGGER.severe("expected the Vertices implementation to be compatible with graph builder, this is not the case: unable to correctly remove subnetwork and update ids");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LinkSegmentImpl createUniqueCopyOf(LinkSegment linkSegmentToCopy, Link parentLink) {
    LinkSegmentImpl copy = (LinkSegmentImpl) directedGraphBuilderImpl.createUniqueCopyOf(linkSegmentToCopy, parentLink);

    /* make unique copy by updating link segment id */
    copy.setLinkSegmentId(LinkSegmentImpl.generateLinkSegmentId(getIdGroupingToken()));
    return copy;
  }

}
