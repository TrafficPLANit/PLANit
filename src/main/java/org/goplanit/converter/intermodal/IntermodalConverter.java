package org.goplanit.converter.intermodal;

import org.goplanit.converter.PairConverter;
import org.goplanit.converter.PairConverterReader;
import org.goplanit.converter.PairConverterWriter;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.service.routed.RoutedServices;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.misc.Quadruple;
import org.goplanit.zoning.Zoning;

import java.util.logging.Logger;

/**
 * A converter that supports intermodal networks, i.e., a combination of both a network and a zoning with transfer zones
 * 
 * @author markr
 *
 */
public class IntermodalConverter<T extends ServiceNetwork, U extends RoutedServices> extends PairConverter<MacroscopicNetwork, Zoning> {

  /** LOGGER to use */
  private static final Logger LOGGER = Logger.getLogger(IntermodalConverter.class.getCanonicalName());

  /**
   * Constructor
   * 
   * @param reader for intermodal networks
   * @param writer for intermodal networks
   */
  protected IntermodalConverter(PairConverterReader<MacroscopicNetwork, Zoning> reader, PairConverterWriter<MacroscopicNetwork, Zoning> writer) {
    super(reader, writer);
  }

  /**
   * Constructor
   *
   * @param reader for intermodal networks
   * @param writer for intermodal networks
   */
  protected IntermodalConverter(IntermodalReader<T, U> reader, IntermodalWriter<ServiceNetwork, RoutedServices> writer) {
    super(reader, writer);
  }

  /**
   * Support conversion fo PLANit network and zoning without services
   *
   * @throws PlanItException thrown when error
   */
  public void convert() throws PlanItException {
    LOGGER.info("Convering without services, invoke convertWithServices() to include services in this conversion");
    super.convert();
  }

  /**
   * Support conversion fo PLANit network and zoning with services
   *
   * @throws PlanItException thrown when error
   */
  public void convertWithServices() throws PlanItException {
    var reader = ((IntermodalReader<T,U>) getReader());
    LOGGER.info(String.format("****************** [START] INTERMODAL CONVERTER WITH SERVICES: READ %s [START] ********************", reader.getTypeDescription()));
    var quadResult = reader.readWithServices();
    reader.reset();
    LOGGER.info(String.format("****************** [END]   INTERMODAL CONVERTER WITH SERVICES: READ %s [END]   ********************", reader.getTypeDescription()));

    var writer = (IntermodalWriter<T, U>) getWriter();
    LOGGER.info(String.format("****************** [START] INTERMODAL CONVERTER WITH SERVICES: WRITE %s [START] ********************", writer.getTypeDescription()));
    writer.writeWithServices(quadResult.first(), quadResult.second(), quadResult.third(), quadResult.fourth());
    writer.reset();
    LOGGER.info(String.format("****************** [END]   INTERMODAL CONVERTER WITH SERVICES: WRITE %s [END]   ********************", writer.getTypeDescription()));
  }

}
