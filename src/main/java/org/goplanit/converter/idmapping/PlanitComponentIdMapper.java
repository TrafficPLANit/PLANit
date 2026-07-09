package org.goplanit.converter.idmapping;

import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.time.TimePeriod;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PlanitComponentIdMapper {

  private final IdMapperType type;

  private final HashMap<Class<? extends ExternalIdAble>, Function<? extends ExternalIdAble, String>> mappings;

  /**
   * Add entry
   * @param key to use
   * @param value to use
   */
  protected void add(Class<? extends ExternalIdAble> key, Function<? extends ExternalIdAble, String> value){
    mappings.put(key, value);
  }

  protected <T extends ExternalIdAble> Function<T, String> get(Class<T> key){
    return (Function<T, String>) mappings.get(key);
  }

  /**
   * Constructor
   * @param type to use
   */
  protected PlanitComponentIdMapper(IdMapperType type){
    this.mappings = new HashMap<>();
    this.type = type;

    add(Mode.class, IdMapperFunctionFactory.createModeIdMappingFunction(type));
    add(TimePeriod.class,  IdMapperFunctionFactory.createTimePeriodIdMappingFunction(type));
  }

  /** get id mapper for modes
   * @return id mapper
   */
  public Function<Mode, String> getModeIdMapper(){
    return get(Mode.class);
  }

  /** get id mapper for time periods
   * @return id mapper
   */
  public Function<TimePeriod, String> getTimePeriodIdMapper(){
    return get(TimePeriod.class);
  }

  /**
   * Get raw contents of how the mappings are stored (not a copy)
   *
   * @return raw underlying map
   */
  public Map<Class<? extends ExternalIdAble>, Function<? extends ExternalIdAble, String>> getRaw() {
    return mappings;
  }

}
