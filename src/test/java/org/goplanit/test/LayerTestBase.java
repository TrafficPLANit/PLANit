package org.goplanit.test;

import java.util.ArrayList;
import java.util.List;

import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.physical.Node;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.locationtech.jts.geom.Coordinate;

/**
 * Base for tests building a small macroscopic network layer by hand: nodes by index, links between them, and the
 * segments running between two nodes. Ids are reset before and after each test
 */
public abstract class LayerTestBase {

  /** layer the test builds on, set by the test's own set up */
  protected MacroscopicNetworkLayer layer;

  /** nodes of the layer by index, in the order they were created */
  protected final List<Node> nodes = new ArrayList<>();

  /**
   * Reset the id generation before each test, ahead of the test's own set up
   */
  @BeforeEach
  public void resetIdsBefore() {
    IdGenerator.reset();
  }

  /**
   * Reset the id generation after each test
   */
  @AfterEach
  public void resetIdsAfter() {
    IdGenerator.reset();
  }

  /**
   * Register nodes on the layer, appended to the nodes by index
   *
   * @param count number of nodes to register
   */
  protected void createNodes(int count) {
    for (int i = 0; i < count; ++i) {
      nodes.add(layer.getNodes().getFactory().registerNew());
    }
  }

  /**
   * Register nodes positioned on a line, node i at (i, 0), so links between them can be given straight geometries
   *
   * @param count number of nodes to register
   */
  protected void createNodesOnLine(int count) {
    createNodes(count);
    for (int i = 0; i < count; ++i) {
      nodes.get(i).setPosition(PlanitJtsUtils.createPoint(i, 0));
    }
  }

  /**
   * Connect node a to node b with a segment in the a-&gt;b direction only, without segment type
   *
   * @param a index of node A
   * @param b index of node B
   * @return the link
   */
  protected MacroscopicLink oneWay(int a, int b) {
    var link = layer.getLinks().getFactory().registerNew(nodes.get(a), nodes.get(b), 1, true);
    layer.getLinkSegments().getFactory().registerNew(link, true, true);
    return link;
  }

  /**
   * Connect nodes a and b with a segment in each direction, without segment type
   *
   * @param a index of node A
   * @param b index of node B
   * @return the link
   */
  protected MacroscopicLink twoWay(int a, int b) {
    var link = oneWay(a, b);
    layer.getLinkSegments().getFactory().registerNew(link, false, true);
    return link;
  }

  /**
   * Connect nodes a and b with a segment in each direction, both of the given type
   *
   * @param a index of node A
   * @param b index of node B
   * @param type of both segments
   * @return the link
   */
  protected MacroscopicLink twoWay(int a, int b, MacroscopicLinkSegmentType type) {
    return link(a, b, type, type);
  }

  /**
   * Connect node a to node b with a segment per direction given a type, each with its own type
   *
   * @param a index of node A
   * @param b index of node B
   * @param typeAb type of the a-&gt;b segment, no such segment when null
   * @param typeBa type of the b-&gt;a segment, no such segment when null
   * @return the link
   */
  protected MacroscopicLink link(int a, int b, MacroscopicLinkSegmentType typeAb, MacroscopicLinkSegmentType typeBa) {
    var link = layer.getLinks().getFactory().registerNew(nodes.get(a), nodes.get(b), 1, true);
    if (typeAb != null) {
      layer.getLinkSegments().getFactory().registerNew(link, true, true).setLinkSegmentType(typeAb);
    }
    if (typeBa != null) {
      layer.getLinkSegments().getFactory().registerNew(link, false, true).setLinkSegmentType(typeBa);
    }
    return link;
  }

  /**
   * Connect node a to node b one way, with a geometry passing through every node on the line between them, see
   * {@link #createNodesOnLine(int)}
   *
   * @param a index of node A
   * @param b index of node B, greater than a
   * @return the link
   */
  protected MacroscopicLink breakableOneWay(int a, int b) {
    var link = oneWay(a, b);
    var coordinates = new Coordinate[b - a + 1];
    for (int i = a; i <= b; ++i) {
      coordinates[i - a] = new Coordinate(i, 0);
    }
    link.setGeometry(PlanitJtsUtils.createLineString(coordinates));
    return link;
  }

  /**
   * The registered link segment running from node a to node b
   *
   * @param a index of the upstream node
   * @param b index of the downstream node
   * @return the segment
   */
  protected MacroscopicLinkSegment segment(int a, int b) {
    return layer.getLinkSegments().stream().filter(
        ls -> ls.getUpstreamVertex() == nodes.get(a) && ls.getDownstreamVertex() == nodes.get(b)).findFirst().orElseThrow();
  }
}
