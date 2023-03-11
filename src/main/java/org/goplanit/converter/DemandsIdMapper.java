package org.goplanit.converter;

import org.goplanit.userclass.TravellerType;
import org.goplanit.userclass.UserClass;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.time.TimePeriod;

import java.util.function.Function;

/**
 * All demand id mappers in a convenience class
 */
public class DemandsIdMapper extends PlanitComponentIdMapper{

  /**
   * Constructor
   * @param type to use
   */
  public DemandsIdMapper(IdMapperType type){
    super(type);
    add(UserClass.class, IdMapperFunctionFactory.createUserClassIdMappingFunction(type));
    add(TimePeriod.class,  IdMapperFunctionFactory.createTimePeriodIdMappingFunction(type));
    add(TravellerType.class, IdMapperFunctionFactory.createTravellerTypeIdMappingFunction(type));
  }

  /** get id mapper for traveller types
   * @return id mapper
   */
  public Function<TravellerType, String> getTravellerTypeIdMapper(){
    return get(TravellerType.class);
  }

  /** get id mapper for traveller types
   * @return id mapper
   */
  public Function<UserClass, String> getUserClassIdMapper(){
    return get(UserClass.class);
  }

  /** get id mapper for time periods
   * @return id mapper
   */
  public Function<TimePeriod, String> getTimePeriodIdMapper(){
    return get(TimePeriod.class);
  }
}
