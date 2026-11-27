package org.goplanit.zoning.zonetozone;

import org.goplanit.algorithms.parallel.shortest.ParallelShortestPathDijkstra;
import org.goplanit.algorithms.shortest.ShortestPathResult;
import org.goplanit.cost.CostUtils;
import org.goplanit.cost.physical.FreeFlowLinkTravelTimeCost;
import org.goplanit.cost.physical.PhysicalCost;
import org.goplanit.cost.virtual.SpeedConnectoidTravelTimeCost;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.transport.TransportModelNetworkImpl;
import org.goplanit.network.transport.TransportModelNetworkUtils;
import org.goplanit.output.enums.SkimSubOutputType;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.virtual.VirtualNetworkLayer;
import org.goplanit.utils.network.virtual.VirtualNetworkUtils;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.Zone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.logging.Logger;

/**
 * Builds, for a mode, the zone to zone skims of the types given, the distance and the time along the least-cost path,
 * between every pair of OD zones of a zoning through a network
 * <p>
 * Trip lengths taken from straight lines between zones miss how far the roads actually go. This searches a network the
 * zoning is joined to through its OD connectoids, as an assignment joins it, one-to-all from each zone in parallel, or
 * from each of the zones given, such as those some trip leaves, and adds up the length along each path found. The
 * cost of a link segment is the mode's free-flow travel time, or that of a given physical cost, such as one an
 * assignment has brought to congested times; a connectoid's is its length at the connectoid speed of the settings. A
 * zone reaches the network only through its OD connectoids that allow the mode, and no path passes through a transfer
 * zone. A mode with a proxy mode in the settings is skimmed as that mode. The caller joins the zoning to the network
 * and undoes the join, so one join can serve the skims of several modes; the memory of the searches is released once
 * each build returns.
 * </p>
 * <p>
 * The distance skim holds kilometres and the time skim hours, the unit of the costs. A pair of zones without a path
 * holds NaN, never 0, as does every pair from a zone not searched from, unless the settings have these cells
 * inferred, see {@link OdSkimMatrixUtils#inferUnreachableCells}.
 * </p>
 *
 * @author markr
 */
public class OdSkimBuilder {

  /**
   * Fills, in the thread it belongs to, the row of the skims of each zone searched from, with the distances along the
   * search's tree worked out once per vertex and search
   * <p>
   * The distance to a zone is the sum of the lengths along its path, and the paths to all zones share their first
   * parts, so each vertex's distance is kept in an array of the thread as soon as it is known and used for every
   * other path through it. Each row is written by the thread that searched from its zone, so no two threads write one
   * cell.
   * </p>
   */
  private static final class SkimRowFiller implements BiFunction<DirectedVertex, ShortestPathResult, Integer> {

    /** length by edge segment id, shared by all threads */
    private final double[] lengthKmByEdgeSegmentId;

    /** OD zones by id */
    private final OdZone[] zonesById;

    /** sink vertex of each OD zone by zone id, null when the zone has none */
    private final CentroidVertex[] sinkByZoneId;

    /** distance skim filled, null when not built */
    private final OdSkimMatrix distances;

    /** time skim filled, null when not built */
    private final OdSkimMatrix times;

    /**
     * distance of each vertex in the current search, valid where its search number is the current one, null without a
     * distance skim
     */
    private final double[] distanceKmByVertexId;

    /** the search each vertex's distance was worked out in, null without a distance skim */
    private final int[] searchOfVertexId;

    /**
     * vertices on the way back to one with a known distance, nearest the destination first, null without a distance
     * skim
     */
    private final DirectedVertex[] verticesOnTheWayBack;

    /** number of the current search of this thread */
    private int search = 0;

    /**
     * Distance from the origin along the search's tree, worked out by going back to the nearest vertex with a known
     * distance and adding up the lengths forward from there, keeping each vertex's distance on the way
     *
     * @param vertex reached by the search
     * @param result of the search
     * @return the distance
     */
    private double distanceKmTo(DirectedVertex vertex, ShortestPathResult result) {
      int onTheWayBack = 0;
      DirectedVertex current = vertex;
      while (searchOfVertexId[(int) current.getId()] != search) {
        verticesOnTheWayBack[onTheWayBack++] = current;
        current = result.getNextEdgeSegmentForVertex(current).getUpstreamVertex();
      }
      double distanceKm = distanceKmByVertexId[(int) current.getId()];
      while (onTheWayBack > 0) {
        current = verticesOnTheWayBack[--onTheWayBack];
        distanceKm += lengthKmByEdgeSegmentId[(int) result.getNextEdgeSegmentForVertex(current).getId()];
        distanceKmByVertexId[(int) current.getId()] = distanceKm;
        searchOfVertexId[(int) current.getId()] = search;
      }
      return distanceKm;
    }

    /**
     * Constructor
     *
     * @param numberOfVertices of the joined network
     * @param lengthKmByEdgeSegmentId length by edge segment id, null without a distance skim
     * @param zonesById OD zones by id
     * @param sinkByZoneId sink vertex of each OD zone by zone id
     * @param skims to fill, by type, without a value in any cell
     */
    private SkimRowFiller(int numberOfVertices, double[] lengthKmByEdgeSegmentId, OdZone[] zonesById,
        CentroidVertex[] sinkByZoneId, Map<SkimSubOutputType, OdSkimMatrix> skims) {
      this.lengthKmByEdgeSegmentId = lengthKmByEdgeSegmentId;
      this.zonesById = zonesById;
      this.sinkByZoneId = sinkByZoneId;
      this.distances = skims.get(SkimSubOutputType.DISTANCE);
      this.times = skims.get(SkimSubOutputType.TRAVEL_TIME);
      boolean withDistances = distances != null;
      this.distanceKmByVertexId = withDistances ? new double[numberOfVertices] : null;
      this.searchOfVertexId = withDistances ? new int[numberOfVertices] : null;
      this.verticesOnTheWayBack = withDistances ? new DirectedVertex[numberOfVertices] : null;
    }

    /**
     * Fill the row of the zone searched from, leaving the cells of the zones it does not reach without a value
     *
     * @param origin source vertex of the zone searched from
     * @param result of its one-to-all search
     * @return the number of other zones reached
     */
    @Override
    public Integer apply(DirectedVertex origin, ShortestPathResult result) {
      ++search;
      if (distances != null) {
        distanceKmByVertexId[(int) origin.getId()] = 0;
        searchOfVertexId[(int) origin.getId()] = search;
      }
      Zone originZone = ((CentroidVertex) origin).getParent().getParentZone();

      int reached = 0;
      for (int zoneId = 0; zoneId < zonesById.length; ++zoneId) {
        var sink = sinkByZoneId[zoneId];
        double cost = sink == null ? Double.MAX_VALUE : result.getCostToReach(sink);
        if (!(cost < Double.MAX_VALUE)) {
          continue;
        }
        if (distances != null) {
          distances.setValue(originZone, zonesById[zoneId], distanceKmTo(sink, result));
        }
        if (times != null) {
          times.setValue(originZone, zonesById[zoneId], cost);
        }
        reached += zonesById[zoneId] == originZone ? 0 : 1;
      }
      return reached;
    }
  }

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(OdSkimBuilder.class.getCanonicalName());

  /** network and zoning searched, joined through the zoning's OD connectoids */
  private final TransportModelNetworkImpl transportNetwork;

  /** settings of the search */
  private final OdSkimBuilderSettings settings;

  /** skims built */
  private final Set<SkimSubOutputType> skimTypes;

  /**
   * The cost of each edge segment of the joined network for the mode: the given physical cost on the layer's link
   * segments the mode may use, a connectoid's length at the connectoid speed on the connectoid segments searched,
   * and {@code Double.MAX_VALUE}, which the search never takes, on every other
   *
   * @param mode searched for
   * @param layer of the mode
   * @param linkSegmentCost cost of a link segment
   * @param virtualLayer of the joined network
   * @return costs by edge segment id
   */
  private double[] createEdgeSegmentCosts(Mode mode, MacroscopicNetworkLayer layer,
      PhysicalCost<MacroscopicLinkSegment> linkSegmentCost, VirtualNetworkLayer virtualLayer) {
    double[] costs = CostUtils.createEmptyLinkSegmentCostArray(
        transportNetwork.getInfrastructureNetwork(), transportNetwork.getZoning());
    var connectoidCost = new SpeedConnectoidTravelTimeCost(IdGroupingToken.collectGlobalToken());
    connectoidCost.setConnectoidSpeed(settings.getConnectoidSpeedKmh());
    CostUtils.populateModalVirtualLinkSegmentCosts(mode, connectoidCost, transportNetwork.getVirtualNetwork(), costs);
    CostUtils.populateModalPhysicalLinkSegmentCosts(mode, linkSegmentCost, layer.getLinkSegments(), costs);

    for (var linkSegment : layer.getLinkSegments()) {
      if (!linkSegment.isModeAllowed(mode)) {
        costs[(int) linkSegment.getId()] = Double.MAX_VALUE;
      }
    }
    /* a connectoid is searched only when it allows the mode and joins an OD zone, so no path passes through a
     * transfer zone: a path passes through one only to switch modes, and a skim is of one mode */
    for (var connectoidSegment : virtualLayer.getConnectoidSegments()) {
      if (!connectoidSegment.isModeAllowed(mode)
          || !(connectoidSegment.getParent().getCentroidVertex().getParent().getParentZone() instanceof OdZone)) {
        costs[(int) connectoidSegment.getId()] = Double.MAX_VALUE;
      }
    }
    return costs;
  }

  /**
   * Fill the rows of the zones searched from that have a connectoid for the mode, by one one-to-all search from each
   * in parallel, leaving the cells of the pairs without a path without a value
   * <p>
   * A zone without a connectoid for the mode reaches nothing, so it is not searched. When two or more zones have a
   * connectoid for the mode and not one zone searched from reaches another, the zoning is not joined to the network as
   * it should be, and every value would be missing or inferred: that is refused rather than passed on. The lengths
   * along the paths are only added up when the distance skim is built.
   * </p>
   *
   * @param mode to skim
   * @param layer of the mode
   * @param linkSegmentCost cost of a link segment for the mode, in hours
   * @param searched by zone id, true for a zone to search from
   * @param zonesById OD zones by id
   * @param skims to fill, by type, without a value in any cell
   */
  private void fillSearchedRows(Mode mode, MacroscopicNetworkLayer layer,
      PhysicalCost<MacroscopicLinkSegment> linkSegmentCost, boolean[] searched, OdZone[] zonesById,
      Map<SkimSubOutputType, OdSkimMatrix> skims) {
    long start = System.nanoTime();
    var virtualLayer = transportNetwork.getVirtualNetwork().getLayer();
    double[] costs = createEdgeSegmentCosts(mode, layer, linkSegmentCost, virtualLayer);
    double[] lengthsKm = skims.containsKey(SkimSubOutputType.DISTANCE)
        ? TransportModelNetworkUtils.createIdIndexedEdgeSegmentLengthsKmAllLayers(
            transportNetwork.getInfrastructureNetwork(), transportNetwork.getVirtualNetwork()) : null;
    var verticesById = transportNetwork.createIdIndexedVerticesAllLayers();

    var sources = VirtualNetworkUtils.createZoneToCentroidVertexMapping(virtualLayer, true, true, false);
    var sinkByZoneId = new CentroidVertex[zonesById.length];
    VirtualNetworkUtils.createZoneToCentroidVertexMapping(virtualLayer, false, true, false).forEach(
        (zone, sink) -> sinkByZoneId[(int) zone.getId()] = sink);
    int zonesWithConnectoid = 0;
    List<CentroidVertex> sourceVertices = new ArrayList<>();
    for (var zone : zonesById) {
      var source = sources.get(zone);
      if (source == null
          || source.streamExitSegments().noneMatch(segment -> costs[(int) segment.getId()] < Double.MAX_VALUE)) {
        continue;
      }
      ++zonesWithConnectoid;
      if (searched[(int) zone.getId()]) {
        sourceVertices.add(source);
      }
    }

    List<Integer> reachedPerOrigin = new ParallelShortestPathDijkstra(
        costs, verticesById, settings.getNumberOfThreads()).executeOneToAll(sourceVertices,
        () -> new SkimRowFiller(verticesById.length, lengthsKm, zonesById, sinkByZoneId, skims));
    long pairs = (long) reachedPerOrigin.size() * (zonesById.length - 1);
    long pairsWithPath = reachedPerOrigin.stream().mapToLong(Integer::longValue).sum();
    PlanItRunTimeException.throwIf(zonesWithConnectoid > 1 && pairs > 0 && pairsWithPath == 0,
        "Not one of %d zones searched from reaches another zone, while %d zones have OD connectoids allowing mode %s: "
            + "the zoning is not joined to the network", reachedPerOrigin.size(), zonesWithConnectoid,
        mode.getXmlId());

    LOGGER.info(String.format("Skimmed %s from %d of %d zones on %d threads in %.1f s", mode.getXmlId(),
        reachedPerOrigin.size(), zonesById.length, settings.getNumberOfThreads(), (System.nanoTime() - start) / 1e9));
    if (pairsWithPath < pairs) {
      LOGGER.info(String.format("%d of %d zone pairs from the zones searched have no path for mode %s, %d of these "
              + "zones reach no other zone", pairs - pairsWithPath, pairs, mode.getXmlId(),
          reachedPerOrigin.stream().filter(reached -> reached == 0).count()));
    }
  }

  /**
   * Constructor
   *
   * @param transportNetwork network with a single layer and zoning whose OD zones are skimmed, joined through the
   *                         zoning's OD connectoids with contiguous ids, as
   *                         {@code integrateTransportNetworkViaConnectoids(true)} leaves it
   * @param settings of the search
   * @param skimTypes to build, of distance and of travel time
   */
  public OdSkimBuilder(TransportModelNetworkImpl transportNetwork, OdSkimBuilderSettings settings,
      SkimSubOutputType... skimTypes) {
    PlanItRunTimeException.throwIfNull(transportNetwork, "Transport network to skim is null, not allowed");
    PlanItRunTimeException.throwIfNull(settings, "Settings of the skim are null, not allowed");
    PlanItRunTimeException.throwIf(!(transportNetwork.getInfrastructureNetwork() instanceof MacroscopicNetwork),
        "Network to skim is not a macroscopic network, unable to skim it");
    PlanItRunTimeException.throwIf(transportNetwork.getVirtualNetwork().getLayer().getConnectoidSegments().isEmpty()
            && !transportNetwork.getZoning().getOdConnectoids().isEmpty(),
        "Zoning is not joined to the network through its connectoids, join it before skimming");
    PlanItRunTimeException.throwIf(skimTypes.length == 0, "No skim type to build given, not allowed");
    for (var type : skimTypes) {
      PlanItRunTimeException.throwIf(
          type != SkimSubOutputType.DISTANCE && type != SkimSubOutputType.TRAVEL_TIME,
          "Skim type %s is not (yet) supported, only %s and %s are", type, SkimSubOutputType.DISTANCE,
          SkimSubOutputType.TRAVEL_TIME);
    }
    this.transportNetwork = transportNetwork;
    this.settings = settings;
    this.skimTypes = EnumSet.copyOf(Arrays.asList(skimTypes));
  }

  /**
   * Build the skims of a mode on its free-flow travel times
   *
   * @param mode to skim
   * @return the skims of the types given, by type, distances in km and times in hours
   */
  public Map<SkimSubOutputType, OdSkimMatrix> build(Mode mode) {
    return build(mode, transportNetwork.getZoning().getOdZones().toCollection());
  }

  /**
   * Build the skims of a mode on its free-flow travel times, searched from the given zones only
   *
   * @param mode to skim
   * @param origins OD zones to search from, the rows of every other zone holding NaN
   * @return the skims of the types given, by type, distances in km and times in hours
   */
  public Map<SkimSubOutputType, OdSkimMatrix> build(Mode mode, Collection<? extends OdZone> origins) {
    return build(mode, new FreeFlowLinkTravelTimeCost(IdGroupingToken.collectGlobalToken()), origins);
  }

  /**
   * Build the skims of a mode on the given cost of a link segment, such as the travel times of an assignment, searched
   * from every OD zone
   *
   * @param mode to skim
   * @param linkSegmentCost cost of a link segment for the mode, in hours
   * @return the skims of the types given, by type, distances in km and times in hours
   */
  public Map<SkimSubOutputType, OdSkimMatrix> build(Mode mode, PhysicalCost<MacroscopicLinkSegment> linkSegmentCost) {
    return build(mode, linkSegmentCost, transportNetwork.getZoning().getOdZones().toCollection());
  }

  /**
   * Build the skims of a mode on the given cost of a link segment, searched from the given zones
   * <p>
   * The mode is skimmed as its proxy mode of the settings, on that mode's layer and connectoids. Each search's thread
   * fills its zone's row, so only the skims remain once all have run. With the cells the search cannot reach to be
   * inferred, they are inferred once the search is done, and every cell of a mode without a layer is; without, such a
   * mode is refused.
   * </p>
   *
   * @param mode to skim
   * @param linkSegmentCost cost of a link segment for the mode, in hours
   * @param origins OD zones to search from, the rows of every other zone holding NaN unless inferred
   * @return the skims of the types given, by type, distances in km and times in hours
   */
  public Map<SkimSubOutputType, OdSkimMatrix> build(
      Mode mode, PhysicalCost<MacroscopicLinkSegment> linkSegmentCost, Collection<? extends OdZone> origins) {
    PlanItRunTimeException.throwIfNull(mode, "Mode to skim is null, not allowed");
    PlanItRunTimeException.throwIfNull(linkSegmentCost, "Link segment cost to skim on is null, not allowed");
    PlanItRunTimeException.throwIfNull(origins, "Zones to skim from are null, not allowed");
    var proxyMode = settings.getProxyModeFor(mode);
    var layer = ((MacroscopicNetwork) transportNetwork.getInfrastructureNetwork()).getLayerByMode(proxyMode);
    PlanItRunTimeException.throwIf(layer == null && !settings.isInferUnreachableCells(),
        "Mode %s has no layer in the network, unable to skim it", proxyMode.getXmlId());
    if (settings.isInferUnreachableCells()) {
      skimTypes.forEach(type -> OdSkimMatrixUtils.verifyCellsInferable(type, proxyMode, settings));
    }

    var odZones = transportNetwork.getZoning().getOdZones();
    var skims = new EnumMap<SkimSubOutputType, OdSkimMatrix>(SkimSubOutputType.class);
    /* in the units of the search, kilometres along the lengths of the edge segments and hours of the costs */
    skimTypes.forEach(type -> skims.put(type, OdSkimMatrixUtils.createSkimWithoutValues(odZones, type)));
    var zonesById = new OdZone[odZones.size()];
    odZones.forEach(zone -> zonesById[(int) zone.getId()] = zone);
    var searched = new boolean[zonesById.length];
    origins.forEach(zone -> searched[(int) zone.getId()] = true);

    if (layer == null) {
      LOGGER.info(String.format("Mode %s has no layer in the network, its skims are inferred", proxyMode.getXmlId()));
    } else {
      fillSearchedRows(proxyMode, layer, linkSegmentCost, searched, zonesById, skims);
    }
    if (settings.isInferUnreachableCells()) {
      skims.values().forEach(skim -> OdSkimMatrixUtils.inferUnreachableCells(
          skim, proxyMode, transportNetwork.getZoning(), settings));
    }
    return skims;
  }
}
