package org.goplanit.test.converter;

import org.goplanit.converter.utils.ProjectedBoundingAreaHelper;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the spatial eligibility checks against a projected bounding area, in particular the allowance that keeps
 * entities sited just beyond the boundary eligible
 *
 * @author markr
 */
public class ProjectedBoundingAreaHelperTest {

  private static final String AUSTRALIA = "Australia";

  /** allowance mirroring the default applied to water based infrastructure */
  private static final double ALLOWANCE_M = 2_000;

  /** bounding area of roughly two by two kilometres situated in Sydney */
  private static final double MIN_LON = 151.20;

  private static final double MAX_LON = 151.22;

  private static final double MIN_LAT = -33.87;

  private static final double MAX_LAT = -33.85;

  /** at this latitude a degree of longitude spans about 92.4 km, so this offset is in the order of 500 metres */
  private static final double NEAR_OFFSET_DEG = 0.0054;

  /** in the order of five kilometres, i.e. comfortably beyond the allowance */
  private static final double FAR_OFFSET_DEG = 0.054;

  /**
   * Create the bounding area used throughout
   *
   * @return bounding polygon in WGS84
   */
  private static Polygon createBoundingPolygon() {
    return PlanitJtsUtils.createPolygon(new Coordinate[] {
        new Coordinate(MIN_LON, MIN_LAT),
        new Coordinate(MAX_LON, MIN_LAT),
        new Coordinate(MAX_LON, MAX_LAT),
        new Coordinate(MIN_LON, MAX_LAT),
        new Coordinate(MIN_LON, MIN_LAT)});
  }

  /**
   * Create a helper for the bounding area with the given allowance
   *
   * @param allowanceMeters to apply for entities beyond the boundary
   * @return created helper
   */
  private static ProjectedBoundingAreaHelper createHelper(double allowanceMeters) {
    return ProjectedBoundingAreaHelper.of(
        createBoundingPolygon(), PlanitJtsCrsUtils.DEFAULT_GEOGRAPHIC_CRS, AUSTRALIA, allowanceMeters);
  }

  @Test
  public void withinBoundaryTest() {
    var helper = createHelper(ALLOWANCE_M);
    var inside = PlanitJtsUtils.createPoint(
        (MIN_LON + MAX_LON) / 2, (MIN_LAT + MAX_LAT) / 2);

    assertTrue(helper.isPartlyOrWhollyWithinBoundaryArea(inside, false));
    assertTrue(helper.isNearPartlyOrWhollyWithinBoundaryArea(inside, ALLOWANCE_M, false));
  }

  @Test
  public void withinAllowanceBeyondBoundaryTest() {
    var helper = createHelper(ALLOWANCE_M);
    var justOutside = PlanitJtsUtils.createPoint(
        MAX_LON + NEAR_OFFSET_DEG, (MIN_LAT + MAX_LAT) / 2);

    /* beyond the boundary, so ineligible without an allowance, but within the allowance once one is granted */
    assertFalse(helper.isPartlyOrWhollyWithinBoundaryArea(justOutside, false));
    assertTrue(helper.isNearPartlyOrWhollyWithinBoundaryArea(justOutside, ALLOWANCE_M, false));
  }

  @Test
  public void beyondAllowanceTest() {
    var helper = createHelper(ALLOWANCE_M);
    var farOutside = PlanitJtsUtils.createPoint(
        MAX_LON + FAR_OFFSET_DEG, (MIN_LAT + MAX_LAT) / 2);

    /* an allowance extends eligibility, it does not invert it: further away remains further away */
    assertFalse(helper.isNearPartlyOrWhollyWithinBoundaryArea(farOutside, ALLOWANCE_M, false));
  }

  @Test
  public void noAllowanceTest() {
    var helper = createHelper(ALLOWANCE_M);
    var justOutside = PlanitJtsUtils.createPoint(
        MAX_LON + NEAR_OFFSET_DEG, (MIN_LAT + MAX_LAT) / 2);

    /* without an allowance the boundary is applied strictly */
    assertFalse(helper.isNearPartlyOrWhollyWithinBoundaryArea(justOutside, 0, false));
  }

  @Test
  public void lineStringAllowanceTest() {
    var helper = createHelper(ALLOWANCE_M);
    var midLat = (MIN_LAT + MAX_LAT) / 2;

    var nearBeyond = PlanitJtsUtils.createLineString(
        new Coordinate(MAX_LON + NEAR_OFFSET_DEG, midLat),
        new Coordinate(MAX_LON + NEAR_OFFSET_DEG * 2, midLat));
    var farBeyond = PlanitJtsUtils.createLineString(
        new Coordinate(MAX_LON + FAR_OFFSET_DEG, midLat),
        new Coordinate(MAX_LON + FAR_OFFSET_DEG * 2, midLat));

    assertTrue(helper.isNearPartlyOrWhollyWithinBoundaryArea(nearBeyond, ALLOWANCE_M, false));
    assertFalse(helper.isNearPartlyOrWhollyWithinBoundaryArea(farBeyond, ALLOWANCE_M, false));
  }

  @Test
  public void whollyWithinBoundaryTest() {
    var helper = createHelper(ALLOWANCE_M);
    var midLat = (MIN_LAT + MAX_LAT) / 2;

    var contained = PlanitJtsUtils.createLineString(
        new Coordinate(MIN_LON + 0.001, midLat),
        new Coordinate(MAX_LON - 0.001, midLat));
    var crossing = PlanitJtsUtils.createLineString(
        new Coordinate(MIN_LON + 0.001, midLat),
        new Coordinate(MAX_LON + NEAR_OFFSET_DEG, midLat));

    /* a geometry extending beyond the boundary intersects it without being contained by it */
    assertTrue(helper.isWhollyWithinBoundaryArea(contained, false));
    assertFalse(helper.isWhollyWithinBoundaryArea(crossing, false));
    assertTrue(helper.isPartlyOrWhollyWithinBoundaryArea(crossing, false));
  }

  @Test
  public void noBoundaryTest() {
    var helper = ProjectedBoundingAreaHelper.empty();
    var anyPoint = PlanitJtsUtils.createPoint(MIN_LON, MIN_LAT);

    /* without a boundary every check falls back on the supplied default rather than assuming one way or the other */
    assertTrue(helper.isPartlyOrWhollyWithinBoundaryArea(anyPoint, true));
    assertFalse(helper.isPartlyOrWhollyWithinBoundaryArea(anyPoint, false));
    assertTrue(helper.isWhollyWithinBoundaryArea(anyPoint, true));
    assertFalse(helper.isWhollyWithinBoundaryArea(anyPoint, false));
    assertTrue(helper.isNearPartlyOrWhollyWithinBoundaryArea(anyPoint, ALLOWANCE_M, true));
    assertFalse(helper.isNearPartlyOrWhollyWithinBoundaryArea(anyPoint, ALLOWANCE_M, false));
  }
}
