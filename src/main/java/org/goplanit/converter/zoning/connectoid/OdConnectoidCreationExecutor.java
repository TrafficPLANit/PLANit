package org.goplanit.converter.zoning.connectoid;

import org.goplanit.converter.zoning.ZoningConverterCommonData;
import org.goplanit.converter.zoning.ZoningConverterModifierUtils;
import org.goplanit.utils.geo.PlanitEntityGeoUtils;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.algorithms.StronglyConnectedComponents;
import org.goplanit.utils.id.ExternalIdAbleUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.LinkSegmentUtils;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.physical.NodeUtils;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.utils.zoning.connectoid.ZoneConnectoidType;
import org.goplanit.zoning.Zoning;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.linearref.LinearLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Creates OD connectoids for the zones of a zoning that have none, spread around each zone on nodes the modes asked
 * for can use, see {@link OdConnectoidCreationSettings}
 * <p>
 * A zoning read without connectoids cannot be reached from a network: a shortest path search or an assignment needs a
 * way in for each zone and mode. Joining every zone to its single nearest node is the plainest way, but it ties a
 * zone's trips to one point and often to a node only one mode can use, or to a motorway or an island. This creates,
 * per zone and layer, the number of connectoids asked for, shared by the layer's modes: first the nearest candidates
 * until every mode has one, then the nearest candidates in the directions furthest from those already made, all
 * within a preferred distance scaled to the zone's size, and never beyond an absolute maximum. A candidate is usable
 * by a mode when the mode can enter and leave the network there over link segments no faster than a maximum speed,
 * on the largest strongly connected part of the network for that mode.
 * </p>
 * <p>
 * The same zoning, network and settings always give the same connectoids: candidates are ordered by distance, then
 * node id, then link id, and links are broken only after every zone has chosen, in order along each link.
 * </p>
 *
 * @author markr
 */
public class OdConnectoidCreationExecutor {

  /** A place a zone's connectoid may go: a node, or a point inside a link where the link would be broken */
  private static final class Candidate {

    /** order by distance from the zone's point, then node id, then link id, nodes before points */
    private static final Comparator<Candidate> BY_DISTANCE_NODE_LINK =
        Comparator.comparingDouble((Candidate c) -> c.distanceMetres)
            .thenComparingLong(c -> c.node != null ? c.node.getId() : Long.MAX_VALUE)
            .thenComparingLong(c -> c.link != null ? c.link.getId() : -1);

    /** the node, null for a point inside a link */
    private final Node node;

    /** the link a point lies inside, null for a node */
    private final MacroscopicLink link;

    /** where a point lies along its link's geometry, null for a node */
    private final LinearLocation locationAlongLink;

    /** where the candidate lies */
    private final Coordinate location;

    /** straight line distance from the zone's point */
    private final double distanceMetres;

    /** compass bearing from the zone's point, 0 to 360 degrees */
    private final double bearingDegrees;

    /** the modes asked for that can use it, never empty */
    private final SortedSet<Mode> modes;

    /**
     * Constructor
     *
     * @param node the node, null for a point inside a link
     * @param link the link a point lies inside, null for a node
     * @param locationAlongLink where a point lies along its link's geometry, null for a node
     * @param location where the candidate lies
     * @param distanceMetres from the zone's point
     * @param bearingDegrees from the zone's point
     * @param modes that can use it
     */
    private Candidate(Node node, MacroscopicLink link, LinearLocation locationAlongLink, Coordinate location,
        double distanceMetres, double bearingDegrees, SortedSet<Mode> modes) {
      this.node = node;
      this.link = link;
      this.locationAlongLink = locationAlongLink;
      this.location = location;
      this.distanceMetres = distanceMetres;
      this.bearingDegrees = bearingDegrees;
      this.modes = modes;
    }

    /**
     * Verify if another candidate lies at the same place, the same node or a point inside the same link, as each link
     * gives at most one point
     *
     * @param other to compare with
     * @return true when at the same place, false otherwise
     */
    private boolean isAtSamePlaceAs(Candidate other) {
      return node != null ? node == other.node : (other.node == null && link == other.link);
    }
  }

  /** The modes asked for on one layer, with what tells which nodes and links each can use */
  private static final class LayerModes {

    /** the layer */
    private final MacroscopicNetworkLayer layer;

    /** the modes asked for that lie on the layer */
    private final SortedSet<Mode> modes;

    /** per mode, the link segments it can use for a connectoid: allowed and no faster than the maximum speed */
    private final Map<Mode, Predicate<EdgeSegment>> usableSegmentByMode = new HashMap<>();

    /** per mode, the nodes of the largest strongly connected part of the layer for it; empty when not asked for */
    private final Map<Mode, Set<Node>> mainPartByMode = new HashMap<>();

    /**
     * Constructor, finding the largest strongly connected part per mode when asked for
     *
     * @param layer the modes lie on
     * @param modes asked for on the layer
     * @param maximumLinkSpeedKmh of a usable link segment
     * @param mainNetworkOnly when true, find the largest strongly connected part per mode
     */
    private LayerModes(
        MacroscopicNetworkLayer layer, SortedSet<Mode> modes, double maximumLinkSpeedKmh, boolean mainNetworkOnly) {
      this.layer = layer;
      this.modes = modes;
      for (var mode : modes) {
        usableSegmentByMode.put(mode, LinkSegmentUtils.permitsMode(mode).and(segment ->
            ((MacroscopicLinkSegment) segment).getModelledSpeedLimitKmH(mode) <= maximumLinkSpeedKmh));
        if (mainNetworkOnly) {
          mainPartByMode.put(mode, new HashSet<>(
              StronglyConnectedComponents.execute(layer.getNodes(), LinkSegmentUtils.permitsMode(mode)).getLargest()));
        }
      }
    }

    /**
     * Verify if a node lies on the part of the network a mode may be joined to
     *
     * @param mode to verify for
     * @param node to verify
     * @return true when on the largest strongly connected part for the mode, or when that is not asked for
     */
    private boolean isOnMainPart(Mode mode, Node node) {
      var mainPart = mainPartByMode.get(mode);
      return mainPart == null || mainPart.contains(node);
    }

    /**
     * The modes that can use a node: each can enter and leave it over a usable link segment, on its main part
     *
     * @param node to verify
     * @return the modes, empty when none
     */
    private SortedSet<Mode> findModesUsingNode(Node node) {
      return modes.stream().filter(mode -> isOnMainPart(mode, node) &&
              !NodeUtils.findEntryLinkSegments(node, usableSegmentByMode.get(mode)).isEmpty() &&
              !NodeUtils.findExitLinkSegments(node, usableSegmentByMode.get(mode)).isEmpty())
          .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * The modes that can use a point inside a link: the link has a usable link segment for each, and both its nodes
     * lie on its main part. Broken at the point, the new node is entered by the upstream piece and left by the
     * downstream one, also on a one way link
     *
     * @param link to verify
     * @return the modes, empty when none
     */
    private SortedSet<Mode> findModesUsingPointInside(MacroscopicLink link) {
      return modes.stream().filter(mode -> isOnMainPart(mode, link.getNodeA()) && isOnMainPart(mode, link.getNodeB()) &&
              link.streamEdgeSegments().anyMatch(usableSegmentByMode.get(mode)))
          .collect(Collectors.toCollection(TreeSet::new));
    }
  }

  /** A connectoid chosen for a zone, made once every zone has chosen and links are broken */
  private static final class ChosenConnectoid {

    /** the zone */
    private final OdZone zone;

    /** the zone's point, the length is measured from */
    private final Point zonePoint;

    /** the candidate chosen */
    private final Candidate candidate;

    /** the layer of the candidate */
    private final MacroscopicNetworkLayer layer;

    /** within this of another point of the same link, a point shares that point's node */
    private final double minimumDistanceToNodeMetres;

    /** the node of the connectoid: the candidate's node, or the node made inside its link */
    private Node node;

    /**
     * Constructor
     *
     * @param zone the zone
     * @param zonePoint the zone's point
     * @param candidate chosen
     * @param layer of the candidate
     * @param minimumDistanceToNodeMetres of the zone
     */
    private ChosenConnectoid(OdZone zone, Point zonePoint, Candidate candidate, MacroscopicNetworkLayer layer,
        double minimumDistanceToNodeMetres) {
      this.zone = zone;
      this.zonePoint = zonePoint;
      this.candidate = candidate;
      this.layer = layer;
      this.minimumDistanceToNodeMetres = minimumDistanceToNodeMetres;
      this.node = candidate.node;
    }
  }

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(OdConnectoidCreationExecutor.class.getCanonicalName());

  /** zones listed at most in a warning */
  private static final int MAX_ZONES_LISTED = 20;

  /** the type of the entries of the connectoids made */
  private static final ZoneConnectoidType ENTRY_TYPE = ZoneConnectoidType.ZONE_ACCESS_EGRESS;

  /**
   * Verify if a bearing lies in the middle half of an angle between two bearings, for example between 135 and 225
   * degrees for the angle from 90 to 270
   *
   * @param bearingDegrees to verify
   * @param startDegrees the angle starts at, going clockwise
   * @param widthDegrees of the angle, 360 for the full circle
   * @return true when in its middle half, false otherwise
   */
  private static boolean isInMiddleHalf(double bearingDegrees, double startDegrees, double widthDegrees) {
    double offset = ((bearingDegrees - startDegrees) % 360 + 360) % 360;
    return offset >= widthDegrees / 4 && offset <= 3 * widthDegrees / 4;
  }

  /**
   * Verify if a candidate lies at the place of one chosen already
   *
   * @param candidate to verify
   * @param chosen so far
   * @return true when so
   */
  private static boolean isChosen(Candidate candidate, List<Candidate> chosen) {
    return chosen.stream().anyMatch(candidate::isAtSamePlaceAs);
  }

  /**
   * The nearest candidate not chosen yet that a mode not yet covered can use
   *
   * @param candidates ordered by distance
   * @param chosen so far
   * @param coveredModes so far
   * @return the candidate, null when none
   */
  private static Candidate findNearestCoveringNewMode(
      List<Candidate> candidates, List<Candidate> chosen, Set<Mode> coveredModes) {
    return candidates.stream().filter(c -> !coveredModes.containsAll(c.modes) && !isChosen(c, chosen))
        .findFirst().orElse(null);
  }

  /**
   * The nearest candidate not chosen yet in the middle half of the widest angle between the bearings of those chosen,
   * the full circle when one is chosen, trying the next widest angle when that one has none
   *
   * @param candidates ordered by distance, all within the preferred distance
   * @param chosen so far, at least one
   * @return the candidate, null when no angle has one
   */
  private static Candidate findNearestInWidestFreeDirection(List<Candidate> candidates, List<Candidate> chosen) {
    double[] bearings = chosen.stream().mapToDouble(c -> c.bearingDegrees).sorted().toArray();
    double[] widths = new double[bearings.length];
    for (int index = 0; index < bearings.length; ++index) {
      double next = index + 1 < bearings.length ? bearings[index + 1] : bearings[0] + 360;
      widths[index] = next - bearings[index];
    }
    var angles = new ArrayList<Integer>();
    for (int index = 0; index < bearings.length; ++index) {
      angles.add(index);
    }
    angles.sort(Comparator.comparingDouble((Integer index) -> -widths[index]).thenComparingDouble(i -> bearings[i]));
    for (int angle : angles) {
      for (var candidate : candidates) {
        if (isInMiddleHalf(candidate.bearingDegrees, bearings[angle], widths[angle]) && !isChosen(candidate, chosen)) {
          return candidate;
        }
      }
    }
    return null;
  }

  /** the converter data over the network and zoning, with its spatial index of links and the break link at node */
  private final ZoningConverterCommonData data;

  /** settings of the run in progress */
  private OdConnectoidCreationSettings settings;

  /** result of the run in progress */
  private OdConnectoidCreationResult result;

  /**
   * The radius of a zone: half the mean of the width and height of the bounding box of its polygon, each measured in
   * metres across its middle, so 750 m for a zone 2 km by 1 km; 0 without a polygon
   *
   * @param zone to measure
   * @return the radius in metres
   */
  private double findRadiusMetres(Zone zone) {
    if (!zone.hasGeometry() || zone.getGeometry().getDimension() < 2) {
      return 0;
    }
    var box = zone.getGeometry().getEnvelopeInternal();
    return (getGeoUtils().getWidthInMetres(box) + getGeoUtils().getHeightInMetres(box)) / 4;
  }

  /**
   * The geo utilities of the network's CRS
   *
   * @return the utilities
   */
  private PlanitJtsCrsUtils getGeoUtils() {
    return data.getGeoUtils();
  }

  /**
   * The candidates within a distance of a zone's point that a mode asked for on the layer can use, ordered by
   * distance, then node id, then link id. For existing nodes, the nodes of the links found; otherwise the closest point
   * of each link found, or the link's nearer node when the point lies within the minimum distance of it
   * <p>
   * The distance is a parameter so the preferred distance is searched first, which for a small urban zone holds a
   * handful of links, and the maximum only when a mode has nothing nearer: a 5 km search around a city zone holds
   * thousands. Using a link's node when the closest point lies next to it avoids a stub link a few metres long; with
   * a minimum distance of 25 m, a closest point 5 m from a node uses the node.
   * </p>
   *
   * @param zonePoint to search around
   * @param radiusMetres to search within
   * @param minimumDistanceToNodeMetres within which a point of a link uses the link's node
   * @param layerModes the modes asked for on the layer
   * @return the candidates
   */
  private List<Candidate> findCandidates(
      Point zonePoint, double radiusMetres, double minimumDistanceToNodeMetres, LayerModes layerModes) {
    var geoUtils = getGeoUtils();
    var candidates = new ArrayList<Candidate>();
    var nodesSeen = new HashSet<Node>();
    Consumer<Node> addIfUsable = node -> {
      if (!nodesSeen.add(node)) {
        return;
      }
      double distance = geoUtils.getDistanceInMetres(zonePoint, node.getPosition());
      var modes = distance <= radiusMetres ? layerModes.findModesUsingNode(node) : Collections.<Mode>emptySortedSet();
      if (!modes.isEmpty()) {
        candidates.add(new Candidate(node, null, null, node.getPosition().getCoordinate(), distance,
            geoUtils.getAzimuthInDegrees(zonePoint.getCoordinate(), node.getPosition().getCoordinate(), true), modes));
      }
    };

    var links = data.findNearbyModeCompatibleLinks(zonePoint, Collections.emptyList(), layerModes.modes, radiusMetres);
    for (var link : links) {
      if (settings.getAttachment() == OdConnectoidAttachment.EXISTING_NODE) {
        addIfUsable.accept(link.getNodeA());
        addIfUsable.accept(link.getNodeB());
        continue;
      }
      var locationAlongLink =
          PlanitEntityGeoUtils.extractClosestProjectedLinearLocationToGeometryFromEdge(zonePoint, link, geoUtils);
      var location = locationAlongLink.getCoordinate(link.getGeometry());
      Node nearerNode = geoUtils.getDistanceInMetres(location, link.getNodeA().getPosition().getCoordinate()) <=
          geoUtils.getDistanceInMetres(location, link.getNodeB().getPosition().getCoordinate()) ?
          link.getNodeA() : link.getNodeB();
      if (geoUtils.getDistanceInMetres(location, nearerNode.getPosition().getCoordinate()) <=
          minimumDistanceToNodeMetres) {
        addIfUsable.accept(nearerNode);
        continue;
      }
      double distance = geoUtils.getDistanceInMetres(zonePoint.getCoordinate(), location);
      var modes = distance <= radiusMetres ? layerModes.findModesUsingPointInside(link) :
          Collections.<Mode>emptySortedSet();
      if (!modes.isEmpty()) {
        candidates.add(new Candidate(null, link, locationAlongLink, location, distance,
            geoUtils.getAzimuthInDegrees(zonePoint.getCoordinate(), location, true), modes));
      }
    }
    candidates.sort(Candidate.BY_DISTANCE_NODE_LINK);
    return candidates;
  }

  /**
   * Choose a zone's connectoids on each layer: first cover the modes, then spread, see the class description. Records
   * the zone's outcome, and the modes left without a connectoid
   * <p>
   * Covering first makes sure each mode can reach the zone at all, even when that takes more connectoids than asked
   * for or one beyond the preferred distance. Spreading then puts the rest where the zone has no way in yet, so trips
   * leave in several directions rather than all from one corner. With two asked for and usable nodes 100 m and 120 m
   * east and 300 m west, the first goes 100 m east; the widest angle is then the whole circle, its middle half faces
   * west, so the second goes 300 m west and the node 120 m east is not used.
   * </p>
   *
   * @param zone to choose for
   * @param layers the modes asked for, per layer
   * @param chosenConnectoids to add the choices to
   */
  private void chooseForZone(OdZone zone, List<LayerModes> layers, List<ChosenConnectoid> chosenConnectoids) {
    Point zonePoint = zone.getCentroid().hasPosition() ?
        zone.getCentroid().getPosition() : PlanitJtsUtils.extractCentre(zone.getGeometry());
    if (zonePoint == null) {
      result.registerZoneOutcome(OdConnectoidZoneOutcome.WITHOUT_POINT);
      return;
    }
    double preferredMetres = settings.getPreferredDistanceZoneRadiusFactor() * findRadiusMetres(zone);
    double maximumMetres = settings.getMaximumDistanceMetres();
    double minimumDistanceToNodeMetres = settings.getMinimumDistanceToNodeFactor() * preferredMetres;

    var outcome = OdConnectoidZoneOutcome.ALL_ASKED_FOR;
    for (var layerModes : layers) {
      var near = findCandidates(
          zonePoint, Math.min(preferredMetres, maximumMetres), minimumDistanceToNodeMetres, layerModes);
      List<Candidate> far = null;
      var chosen = new ArrayList<Candidate>();
      var coveredModes = new TreeSet<Mode>();

      /* cover the modes: the nearest within the preferred distance for a mode not yet covered, else within the
       * maximum, which is only searched when needed as it holds many more links */
      while (coveredModes.size() < layerModes.modes.size()) {
        var next = findNearestCoveringNewMode(near, chosen, coveredModes);
        if (next == null) {
          far = far != null ? far : findCandidates(zonePoint, maximumMetres, minimumDistanceToNodeMetres, layerModes);
          next = findNearestCoveringNewMode(far, chosen, coveredModes);
        }
        if (next == null) {
          break;
        }
        chosen.add(next);
        coveredModes.addAll(next.modes);
      }

      /* spread: in the directions furthest from those made, within the preferred distance only */
      while (!chosen.isEmpty() && chosen.size() < settings.getNumberOfConnectoidsPerZone()) {
        var next = findNearestInWidestFreeDirection(near, chosen);
        if (next == null) {
          break;
        }
        chosen.add(next);
      }

      OdConnectoidZoneOutcome layerOutcome;
      if (coveredModes.size() < layerModes.modes.size()) {
        layerOutcome = OdConnectoidZoneOutcome.NONE_FOR_A_MODE;
        layerModes.modes.stream().filter(mode -> !coveredModes.contains(mode))
            .forEach(mode -> result.registerZoneWithoutConnectoid(mode, zone, OdConnectoidZoneOutcome.NONE_FOR_A_MODE));
      } else if (near.isEmpty()) {
        layerOutcome = OdConnectoidZoneOutcome.NONE_WITHIN_PREFERRED_DISTANCE;
      } else if (chosen.size() < settings.getNumberOfConnectoidsPerZone()) {
        layerOutcome = OdConnectoidZoneOutcome.FEWER_THAN_ASKED_FOR;
      } else {
        layerOutcome = OdConnectoidZoneOutcome.ALL_ASKED_FOR;
      }
      outcome = layerOutcome.ordinal() < outcome.ordinal() ? layerOutcome : outcome;
      chosen.forEach(candidate -> chosenConnectoids.add(new ChosenConnectoid(
          zone, zonePoint, candidate, layerModes.layer, minimumDistanceToNodeMetres)));
    }
    result.registerZoneOutcome(outcome);
  }

  /**
   * Make the nodes of the points chosen inside links: each link broken at its points in order along it, a point
   * within the larger of the two zones' minimum distances of the point before sharing its node. Each point after the
   * first lies on one of the two links the previous break left, the one nearest it
   * <p>
   * Breaking only once every zone has chosen means every zone chooses on the same network, so the outcome does not
   * depend on the order of the zones, and zones either side of one road can share it: two zones choosing points 300 m
   * apart on one link break it twice, into three links, and two choosing points 5 m apart share one new node.
   * </p>
   *
   * @param chosenConnectoids all choices, those at points given their node
   */
  private void makeNodesInsideLinks(List<ChosenConnectoid> chosenConnectoids) {
    var pointsByLink = new TreeMap<MacroscopicLink, List<ChosenConnectoid>>();
    chosenConnectoids.stream().filter(c -> c.node == null)
        .forEach(c -> pointsByLink.computeIfAbsent(c.candidate.link, l -> new ArrayList<>()).add(c));

    var geoUtils = getGeoUtils();
    for (var entry : pointsByLink.entrySet()) {
      var points = entry.getValue();
      points.sort(Comparator.comparing((ChosenConnectoid c) -> c.candidate.locationAlongLink));
      Collection<MacroscopicLink> pieces = List.of(entry.getKey());
      ChosenConnectoid previous = null;
      for (var point : points) {
        var location = point.candidate.location;
        if (previous != null && geoUtils.getDistanceInMetres(previous.candidate.location, location) <=
            Math.max(previous.minimumDistanceToNodeMetres, point.minimumDistanceToNodeMetres)) {
          point.node = previous.node;
          continue;
        }
        var piece = pieces.stream().min(Comparator.comparingDouble(
            (MacroscopicLink l) -> geoUtils.getClosestDistanceInMeters(location, l.getGeometry()))).orElseThrow();
        var nodeAndBroken = ZoningConverterModifierUtils.extractNodeByLinkGeometryLocation(
            data, PlanitJtsUtils.createPoint(location), piece, point.layer);
        point.node = nodeAndBroken.first();
        if (nodeAndBroken.second()) {
          result.registerLinkBroken();
          pieces = point.node.getLinks();
        }
        previous = point;
      }
    }
  }

  /**
   * Log the counts, and per mode the zones left without a connectoid
   */
  private void logResult() {
    LOGGER.info(String.format("Created %d OD connectoids, %d links broken; zones by outcome: %s",
        result.getNumberOfConnectoidsCreated(), result.getNumberOfLinksBroken(),
        Arrays.stream(OdConnectoidZoneOutcome.values())
            .map(outcome -> outcome + " " + result.getNumberOfZones(outcome)).collect(Collectors.joining(", "))));
    for (var mode : result.getModesWithZonesWithoutConnectoid()) {
      var zones = result.getZonesWithoutConnectoid(mode, OdConnectoidZoneOutcome.NONE_FOR_A_MODE);
      if (!zones.isEmpty()) {
        LOGGER.warning(String.format("%d zones without an OD connectoid for mode %s within %.0f m: %s", zones.size(),
            mode.getXmlId(), settings.getMaximumDistanceMetres(), ExternalIdAbleUtils.toIdsAsString(zones,
                MAX_ZONES_LISTED)));
      }
      var keptZones = result.getZonesWithoutConnectoid(mode, OdConnectoidZoneOutcome.KEPT_EXISTING);
      if (!keptZones.isEmpty()) {
        LOGGER.warning(String.format(
            "%d zones kept their own OD connectoids, none allowing mode %s, as replacing them was not asked for: %s",
            keptZones.size(), mode.getXmlId(), ExternalIdAbleUtils.toIdsAsString(keptZones, MAX_ZONES_LISTED)));
      }
    }
    if (!result.getModesLeftWithoutConnectoidsByReplacing().isEmpty()) {
      LOGGER.warning(String.format(
          "Replacing removed every OD connectoid, also those allowing modes %s, which were not asked for and have no "
              + "OD connectoid left", result.getModesLeftWithoutConnectoidsByReplacing().stream()
              .map(Mode::getXmlId).collect(Collectors.joining(", "))));
    }
  }

  /**
   * Constructor
   *
   * @param data the converter data over the network and zoning to create the connectoids for
   */
  public OdConnectoidCreationExecutor(ZoningConverterCommonData data) {
    this.data = data;
  }

  /**
   * Create the OD connectoids for the zones of the zoning that have none, or for every zone when replacing is asked
   * for
   * <p>
   * The modes are grouped by the layer they lie on, as a connectoid lies on one layer's node; each layer's modes share
   * connectoids, and each zone gets the number asked for on each layer. All zones choose first, then links are broken
   * and the connectoids made, each allowing the modes that can use its node, with the straight line from the zone's
   * point as its length. A zone with OD connectoids of its own keeps them when replacing is not asked for, also when
   * none of them allows a mode asked for, and is then reported without a connectoid for that mode. Replacing removes
   * every OD connectoid of the zoning first, also those of modes not asked for, which are not made again and are
   * reported, and renumbers the connectoids after.
   * </p>
   *
   * @param settings to use
   * @return what was created
   */
  public OdConnectoidCreationResult execute(OdConnectoidCreationSettings settings) {
    this.settings = settings;
    this.result = new OdConnectoidCreationResult();
    Zoning zoning = data.getReferenceZoning();
    var network = data.getReferenceNetwork();

    /* the spatial index is built only when absent, and the transfer connectoids are indexed so breaking links keeps
     * them right, as for the transfer zones' access and egress */
    data.getConnectoidData().indexExistingTransferConnectoids();
    if (!data.hasSpatiallyIndexedLinks()) {
      data.recreateSpatiallyIndexedLinks();
    }

    var modesByLayer = new TreeMap<MacroscopicNetworkLayer, SortedSet<Mode>>();
    Collection<Mode> modes = settings.getModes().isEmpty() ? network.getModes().valuesAsNewSet() : settings.getModes();
    for (var mode : modes) {
      var layer = network.getLayerByMode(mode);
      if (layer == null) {
        LOGGER.warning(String.format("Mode %s lies on no network layer, no OD connectoids created for it",
            mode.getXmlId()));
        continue;
      }
      modesByLayer.computeIfAbsent(layer, l -> new TreeSet<>()).add(mode);
    }
    var layers = modesByLayer.entrySet().stream().map(entry -> new LayerModes(entry.getKey(), entry.getValue(),
        settings.getMaximumLinkSpeedKmh(), settings.isMainNetworkOnly())).collect(Collectors.toList());

    /* the modes each zone's own OD connectoids allow, every zone with one present even when it allows no mode */
    var allModes = network.getModes().valuesAsNewSet();
    var ownModesByZone = new HashMap<Zone, Set<Mode>>();
    zoning.getOdConnectoids().forEach(connectoid -> connectoid.getAccessZoneEntriesByType().values().forEach(
        entriesByType -> entriesByType.values().forEach(entry -> ownModesByZone.computeIfAbsent(
            entry.getAccessZone(), zone -> new HashSet<>()).addAll(entry.getAllowedModesFrom(allModes)))));

    boolean replaced = settings.isReplaceExistingConnectoids() && !zoning.getOdConnectoids().isEmpty();
    if (replaced) {
      ownModesByZone.values().stream().flatMap(Set::stream).filter(mode -> !modes.contains(mode))
          .forEach(result::registerModeLeftWithoutConnectoidsByReplacing);
      ownModesByZone.clear();
      zoning.getOdConnectoids().clear();
    }

    var modesOnLayers = layers.stream().flatMap(layerModes -> layerModes.modes.stream())
        .collect(Collectors.toCollection(TreeSet::new));
    var chosenConnectoids = new ArrayList<ChosenConnectoid>();
    for (var zone : zoning.getOdZones()) {
      var ownModes = ownModesByZone.get(zone);
      if (ownModes != null) {
        result.registerZoneOutcome(OdConnectoidZoneOutcome.KEPT_EXISTING);
        modesOnLayers.stream().filter(mode -> !ownModes.contains(mode)).forEach(
            mode -> result.registerZoneWithoutConnectoid(mode, zone, OdConnectoidZoneOutcome.KEPT_EXISTING));
        continue;
      }
      chooseForZone(zone, layers, chosenConnectoids);
    }

    makeNodesInsideLinks(chosenConnectoids);
    for (var chosen : chosenConnectoids) {
      var connectoid = zoning.getOdConnectoids().getFactory().registerNewWithUndirectedEntry(chosen.zone, chosen.node,
          ENTRY_TYPE, getGeoUtils().getDistanceInKilometres(chosen.zonePoint, chosen.node.getPosition()));
      connectoid.getAccessZoneEntry(chosen.zone, ENTRY_TYPE).addAllowedModes(chosen.candidate.modes);
      result.registerConnectoidCreated();
    }
    if (replaced) {
      zoning.getZoningModifier().recreateConnectoidIds();
    }

    logResult();
    return result;
  }
}
