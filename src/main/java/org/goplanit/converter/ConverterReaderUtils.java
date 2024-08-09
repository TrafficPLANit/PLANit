package org.goplanit.converter;

import org.goplanit.utils.geo.PlanitCrsUtils;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.misc.StringUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.logging.Logger;

/**
 * Utilities that may be helpful for converter readers
 *
 * @author markr
 */
public class ConverterReaderUtils {

  private static final Logger LOGGER = Logger.getLogger(ConverterReaderUtils.class.getCanonicalName());

  /**
   * Create CRS based on string and fallback option. If neither works, assume CARTESIAN
   *
   * @param crsString to use
   * @param fallBackAssumedCrs to use if crsString fails, may be null
   * @return created CRS based on first choice, or second choice if first choice failed, or cartesian if both failed
   */
  public static CoordinateReferenceSystem createCoordinateReferenceSystemCartesianIfFail(
          String crsString, String fallBackAssumedCrs) {

    CoordinateReferenceSystem crs = null;
    if(!StringUtils.isNullOrBlank(crsString)) {
      crs = PlanitCrsUtils.createCoordinateReferenceSystem(crsString);
    }

    if(crs == null){
      LOGGER.warning(String.format("%s CRS not available, trying fallback CRS %s", crsString, fallBackAssumedCrs));
      if(!StringUtils.isNullOrBlank(fallBackAssumedCrs)){
        crs = PlanitCrsUtils.createCoordinateReferenceSystem(fallBackAssumedCrs);
      }
    }

    if(crs == null){
      LOGGER.warning(String.format("%s CRS (fallback) also not available, reverting to assumed default CARTESIAN", fallBackAssumedCrs));
      crs = PlanitJtsCrsUtils.CARTESIANCRS;
    }

    return crs;
  }
}
