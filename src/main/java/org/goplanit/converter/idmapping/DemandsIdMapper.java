package org.goplanit.converter.idmapping;

import org.goplanit.userclass.TravellerType;
import org.goplanit.userclass.UserClass;
import org.goplanit.utils.id.IdMapperType;
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

}
