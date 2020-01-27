package org.planit.zoning;

import java.util.HashMap;
import java.util.Map;
import org.planit.network.virtual.Centroid;
import org.planit.utils.misc.IdGenerator;

/**
 * Represents a demand generating zone in the network.
 * 
 * @author markr
 *
 */
public class Zone {

    /**
     * Unique identifier for the zone
     */
    protected final long id;

    /**
     * External Id for this zone
     */
    protected final long externalId;

    /**
     * generic input property storage
     */
    protected Map<String, Object> inputProperties = null;

    /**
     * Centroid of the zone
     */
    protected final Centroid centroid;

    /**
     * Generate unique id for this zone
     * 
     * @return id for this zone
     */
    protected static int generateZoneId() {
        return IdGenerator.generateId(Zone.class);
    }

    // Public

    /**
     * Constructor
     * 
     * @param externalId
     *            the external ID of this zone
     */
    public Zone(long externalId) {
        id = generateZoneId();
        this.externalId = externalId;
        this.centroid = new Centroid(this);
    }

    /**
     * Returns the id of this zone
     * 
     * @return id of this zone
     */
    public long getId() {
        return this.id;
    }

    /**
     * Add a property from the original input that is not part of the readily
     * available members
     * 
     * @param key
     *            property key
     * @param value
     *            property value
     */
    public void addInputProperty(String key, Object value) {
        if (inputProperties == null) {
            inputProperties = new HashMap<String, Object>();
        }
        inputProperties.put(key, value);
    }

    /**
     * Get input property by its key
     * 
     * @param key
     *            property key
     * @return property value
     */
    public Object getInputProperty(String key) {
        return inputProperties.get(key);
    }

    /**
     * Returns the centroid of this zone
     * 
     * @return centroid of this zone
     */
    public Centroid getCentroid() {
        return centroid;
    }

    public long getExternalId() {
        return externalId;
    }

}