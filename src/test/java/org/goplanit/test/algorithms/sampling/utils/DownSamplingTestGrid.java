package org.goplanit.test.algorithms.sampling.utils;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.output.enums.SkimSubOutputType;
import org.goplanit.utils.geo.HilbertCurveUtils;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.Modes;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.zonetozone.OdSkimMatrix;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.ToDoubleBiFunction;

/**
 * A square grid of zones 1 km apart for the down-sampling tests, each zone placed by its centroid, with the modes car,
 * bus and pedestrian, the zones in their order along the Hilbert curve as the down-sampling table takes them, and one
 * complete distance skim shared by every mode
 *
 * @author markr
 */
public final class DownSamplingTestGrid {

  /** metres between the centres of neighbouring zones */
  private static final double ZONE_SPACING_METRES = 1000;

  /** the modes car, bus and pedestrian */
  private final Modes modes;

  /** zoning of the grid */
  private final Zoning zoning;

  /** the zones in their order along the Hilbert curve */
  private final List<OdZone> curveOrder;

  /** the distance skims, one shared by every mode */
  private final Map<Mode, OdSkimMatrix> distances;

  /**
   * Constructor
   *
   * @param token to create the network and zoning with
   * @param side number of zones along each side
   * @param firstCentreMetres x and y of the centre of the zone in the bottom left corner
   * @param nameOfZone XML id of a zone from its column and its row, both counted from the bottom left
   * @param distanceKm distance in km between two zones, also asked for a zone and itself
   */
  public DownSamplingTestGrid(IdGroupingToken token, int side, double firstCentreMetres,
      BiFunction<Integer, Integer, String> nameOfZone, ToDoubleBiFunction<OdZone, OdZone> distanceKm) {
    var network = new MacroscopicNetwork(token);
    modes = network.getModes();
    modes.getFactory().registerNew(PredefinedModeType.CAR);
    modes.getFactory().registerNew(PredefinedModeType.BUS);
    modes.getFactory().registerNew(PredefinedModeType.PEDESTRIAN);
    var layer = network.getTransportLayers().getFactory().registerNew();
    zoning = new Zoning(token, layer.getLayerIdGroupingToken());
    zoning.setCoordinateReferenceSystem(PlanitJtsCrsUtils.CARTESIANCRS);
    for (int x = 0; x < side; ++x) {
      for (int y = 0; y < side; ++y) {
        var zone = zoning.getOdZones().getFactory().registerNew();
        zone.setXmlId(nameOfZone.apply(x, y));
        zone.getCentroid().setPosition(PlanitJtsUtils.createPoint(
            firstCentreMetres + x * ZONE_SPACING_METRES, firstCentreMetres + y * ZONE_SPACING_METRES));
      }
    }
    curveOrder = HilbertCurveUtils.sortAlongCurve(
        zoning.getOdZones(), zone -> zone.getGeometryCentreOrCentroidPosition().getCoordinate());

    var skim = new OdSkimMatrix(zoning.getOdZones(), SkimSubOutputType.DISTANCE);
    for (var origin : zoning.getOdZones()) {
      for (var destination : zoning.getOdZones()) {
        skim.setValue(origin, destination, distanceKm.applyAsDouble(origin, destination));
      }
    }
    distances = Map.of(getMode(PredefinedModeType.CAR), skim, getMode(PredefinedModeType.BUS), skim,
        getMode(PredefinedModeType.PEDESTRIAN), skim);
  }

  /**
   * The mode of the given type
   *
   * @param type car, bus or pedestrian
   * @return the mode
   */
  public Mode getMode(PredefinedModeType type) {
    return modes.get(type);
  }

  /**
   * The zoning of the grid
   *
   * @return the zoning
   */
  public Zoning getZoning() {
    return zoning;
  }

  /**
   * The zones in their order along the Hilbert curve
   *
   * @return the zones
   */
  public List<OdZone> getCurveOrder() {
    return curveOrder;
  }

  /**
   * The distance skims, one shared by every mode
   *
   * @return the skims by mode
   */
  public Map<Mode, OdSkimMatrix> getDistances() {
    return distances;
  }
}
