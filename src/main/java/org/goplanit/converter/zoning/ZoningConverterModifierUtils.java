package org.goplanit.converter.zoning;

import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitEntityGeoUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.modifier.event.GraphModifierListener;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.zoning.connectoid.ConnectoidUtils;
import org.goplanit.zoning.modifier.event.handler.UpdateDirectedConnectoidsOnBreakLinkSegmentHandler;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.linearref.LinearLocation;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Utilities that change the network a zoning converter works on, through the network's modifiers and their listeners,
 * keeping the converter's data in line with the change
 * <p>
 * A zoning converter keeps its own view of the network beside it: a spatial index of the links and the transfer
 * connectoids by location. Breaking a link at a node, to join a stop or a zone to the network along it, replaces the
 * link and its segments, so that view, and the connectoids reached over the old segments, would be wrong afterwards.
 * These change the network and bring the converter's data and the connectoids along in one step, as readers placing
 * stops along links and the creation of OD connectoids at new nodes both need.
 * </p>
 *
 * @author markr
 */
public final class ZoningConverterModifierUtils {

  /** utilities, not to be instantiated */
  private ZoningConverterModifierUtils() {
  }

  /**
   * Break links at a node, keeping what refers to them right: each transfer connectoid tracked by the converter data
   * whose access link segment lies on a broken link is moved to the piece ending where its segment ended, and the
   * spatial index of links holds the pieces in place of the links broken
   * <p>
   * Breaking a link replaces each of its link segments by two, so whatever held on to the old ones is wrong afterwards:
   * a vehicle stop would be reached over a segment that no longer ends at its stop, and a search of the spatial index
   * would find a link whose shape has changed. Readers that place stops along links break them this way, and so does
   * the creation of OD connectoids at new nodes. What a caller tracks of its own, such as the source ways of the links,
   * it updates from the pieces returned.
   * </p>
   *
   * @param data of the zoning converter, its spatial index of links and transfer connectoids kept in line
   * @param node to break the links at, registered at its location on them
   * @param networkLayer the node and links lie on
   * @param linksToBreak the links to break
   * @return per id of a link broken, its two pieces
   */
  public static Map<Long, Pair<MacroscopicLink, MacroscopicLink>> breakLinksAtNode(ZoningConverterCommonData data,
      Node node, MacroscopicNetworkLayer networkLayer, List<MacroscopicLink> linksToBreak) {
    var connectoidsOnLinks = ConnectoidUtils.findDirectedConnectoidsReferencingLinks(
        linksToBreak, data.getConnectoidData().getTransferConnectoidsByLocation(networkLayer).values().stream()
            .flatMap(Collection::stream));
    GraphModifierListener listener = connectoidsOnLinks.isEmpty() ? null :
        new UpdateDirectedConnectoidsOnBreakLinkSegmentHandler(connectoidsOnLinks);
    if (listener != null) {
      networkLayer.getLayerModifier().addListener(listener);
    }
    try {
      data.removeLinksFromSpatialLinkIndex(linksToBreak);
      var brokenLinks = networkLayer.getLayerModifier().breakAt(
          linksToBreak, node, data.getReferenceNetwork().getCoordinateReferenceSystem());
      brokenLinks.values().forEach(
          pieces -> data.addLinksToSpatialLinkIndex(networkLayer, pieces.first(), pieces.second()));
      return brokenLinks;
    } finally {
      if (listener != null) {
        networkLayer.getLayerModifier().removeListener(listener);
      }
    }
  }

  /**
   * The node at a location on a link: the link's end node when the location is one, otherwise a new node there with
   * the link broken at it, see {@link #breakLinksAtNode(ZoningConverterCommonData, Node, MacroscopicNetworkLayer,
   * List)}. A location that is not yet a coordinate of the link's geometry is first injected into it, at its closest
   * projection on the link, and the node takes the geometry's coordinate, so node and link meet exactly
   * <p>
   * Whatever joins the network at a point along a link, such as a stop or a zone, needs a node there. Matching the
   * coordinate with a small tolerance keeps a location read back from file with fewer decimals on the link it came
   * from.
   * </p>
   *
   * @param data of the zoning converter, kept in line when the link is broken
   * @param location on or near the link, in the network's CRS
   * @param link the location is on, or nearest to
   * @param networkLayer the link lies on
   * @return the node, and true when it is new and the link was broken, false when it is an end node of the link
   */
  public static Pair<Node, Boolean> extractNodeByLinkGeometryLocation(ZoningConverterCommonData data,
      Point location, final MacroscopicLink link, final MacroscopicNetworkLayer networkLayer) {
    PlanItRunTimeException.throwIfNull(location, "Location to extract node for is null, not allowed");
    PlanItRunTimeException.throwIfNull(link, "Link to extract node on is null, not allowed");

    for (var endNode : List.<Node>of(link.getNodeB(), link.getNodeA())) {
      if (endNode.isPositionEqual2D(location.getCoordinate(), Precision.EPSILON_6)) {
        return Pair.of(endNode, Boolean.FALSE);
      }
    }

    int coordinatePosition = PlanitJtsUtils.findFirstCoordinatePosition(
        location.getCoordinate(), link.getGeometry(), Precision.EPSILON_6).orElse(-1);
    if (coordinatePosition < 0) {
      LinearLocation projectedLocation = PlanitEntityGeoUtils.extractClosestProjectedLinearLocationToGeometryFromEdge(
          location, link, data.getGeoUtils());
      link.updateGeometryInjectCoordinateAtProjectedLocation(projectedLocation);
      coordinatePosition = PlanitJtsUtils.findFirstCoordinatePosition(
          location.getCoordinate(), link.getGeometry(), Precision.EPSILON_6).orElse(-1);
    }
    location = PlanitJtsUtils.createPoint(link.getGeometry().getCoordinateN(coordinatePosition));

    Node node = networkLayer.getNodes().getFactory().registerNew(location, true);
    breakLinksAtNode(data, node, networkLayer, List.of(link));
    return Pair.of(node, Boolean.TRUE);
  }
}
