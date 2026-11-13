package org.goplanit.test.graph;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.directed.EdgeSegmentUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Tests the geometry the edge segment utilities make of a series of segments
 *
 * @author markr
 */
public class EdgeSegmentUtilsTest extends LayerTestBase {

  /**
   * Set up an empty layer
   */
  @BeforeEach
  public void setUp() {
    var network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    layer = network.getTransportLayers().getFactory().registerNew();
  }

  /**
   * Segments followed from node 0 to node 2, where the first link's geometry is drawn from node 1 back to node 0. In
   * the direction of the segments, that geometry is turned, so the result runs from node 0 to node 2
   * <pre>
   *          (50,10)           (150,10)
   *   0  &lt;----- drawn ------  1  ----- drawn ----->  2
   *  (0,0)                 (100,0)                (200,0)
   *   0  ------------------&gt; 1  ------------------&gt; 2    followed
   * </pre>
   */
  @Test
  public void geometryInSegmentDirectionTurnsAParentGeometryDrawnTheOtherWay() {
    int start = node(0, 0);
    int middle = node(100, 0);
    int end = node(200, 0);
    twoWay(middle, start).setGeometry(PlanitJtsUtils.createLineString(
        new Coordinate(100, 0), new Coordinate(50, 10), new Coordinate(0, 0)));
    oneWay(middle, end).setGeometry(PlanitJtsUtils.createLineString(
        new Coordinate(100, 0), new Coordinate(150, 10), new Coordinate(200, 0)));

    var geometry = EdgeSegmentUtils.createGeometryFrom(
        List.of(segment(start, middle), segment(middle, end)).iterator(), true);

    assertArrayEquals(new Coordinate[] {new Coordinate(0, 0), new Coordinate(50, 10), new Coordinate(100, 0),
        new Coordinate(150, 10), new Coordinate(200, 0)}, geometry.getCoordinates());
  }
}
