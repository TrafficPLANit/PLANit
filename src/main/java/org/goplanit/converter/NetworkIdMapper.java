package org.goplanit.converter;

import org.goplanit.userclass.TravellerType;
import org.goplanit.userclass.UserClass;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.time.TimePeriod;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * All network id mappers in a convenience class
 */
public class NetworkIdMapper extends PlanitComponentIdMapper{

  /**
   * Constructor
   * @param type to use
   */
  public NetworkIdMapper(IdMapperType type){
    super(type);
    add(Link.class, IdMapperFunctionFactory.createLinkIdMappingFunction(type));
    add(Link.class, IdMapperFunctionFactory.createLinkIdMappingFunction(type));
    add(MacroscopicLinkSegmentType.class, IdMapperFunctionFactory.createLinkSegmentTypeIdMappingFunction(type));
    add(MacroscopicLinkSegment.class, IdMapperFunctionFactory.createLinkSegmentIdMappingFunction(type));
    add(TimePeriod.class,  IdMapperFunctionFactory.createTimePeriodIdMappingFunction(type));
    add(TravellerType.class, IdMapperFunctionFactory.createTravellerTypeIdMappingFunction(type));
    add(UserClass.class, IdMapperFunctionFactory.createUserClassIdMappingFunction(type));
    add(Vertex.class, IdMapperFunctionFactory.createVertexIdMappingFunction(type));
  }

  /** get id mapper for nodes
   * @return id mapper
   */
  public Function<Vertex, String> getVertexIdMapper(){
    return get(Vertex.class);
  }

  /** get id mapper for links
   * @return id mapper
   */
  public Function<Link, String> getLinkIdMapper(){
    return get(Link.class);
  }

  /** get id mapper for link segments
   * @return id mapper
   */
  public Function<MacroscopicLinkSegment, String> getLinkSegmentIdMapper(){
    return get(MacroscopicLinkSegment.class);
  }

  /** get id mapper for link segment types
   * @return id mapper
   */
  public Function<MacroscopicLinkSegmentType, String> getLinkSegmentTypeIdMapper(){
    return get(MacroscopicLinkSegmentType.class);
  }

  /** get id mapper for traveller types
   * @return id mapper
   */
  public Function<TravellerType, String> getTravellerTypeIdMapper(){
    return get(TravellerType.class);
  }

  /** get id mapper for time periods
   * @return id mapper
   */
  public Function<TimePeriod, String> getTimePeriodIdMapper(){
    return get(TimePeriod.class);
  }

  /** get id mapper for user classes
   * @return id mapper
   */
  public Function<UserClass, String> getUserClassIdMapper(){
    return get(UserClass.class);
  }

}
