package org.planit.network.virtual;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
import org.planit.network.physical.Node;

/**
 * Model free virtual network which is part of the zoning and holds all the
 * virtual infrastructure connecting the zones to the physical road network.
 * 
 * @author markr
 */
public class VirtualNetwork {

    // INNER CLASSES

    /**
     * Internal class for all Connectoid specific code
     *
     */
 	public class Connectoids {

        /**
         * Add connectoid to the internal container
         * 
         * If new connectoid overrides an existing connectoid, the removed connectoid is
         * returned
         * 
         * @param connectoid the connectoid to be registered
         * @return connectoid added
         */
        protected Connectoid registerConnectoid(@Nonnull Connectoid connectoid) {
            return connectoidMap.put(connectoid.getId(), connectoid);
        }

        /**
         * Create new connectoid to from a specified centroid to a specified node
         * 
         * @param centroid centroid at one end of the connectoid
         * @param node node at other end of the connectoid
         * @param length length of connectiod
         * @param externalId external Id of connectoid (null if not set in the input file)
         * @return Connectoid object created and registered
         * @throws PlanItException  thrown if there is an error
         */
        public Connectoid registerNewConnectoid(Centroid centroid, Node node, double length, BigInteger externalId) throws PlanItException {
            Connectoid newConnectoid = new Connectoid(centroid, node, length, externalId);
            registerConnectoid(newConnectoid);
            return newConnectoid;
        }

        /**
         * Get connectoid by id
         * 
         * @param id
         *            the id of this connectoid
         * @return the retrieved connectoid
         */
        public Connectoid getConnectoid(long id) {
            return connectoidMap.get(id);
        }

        /**
         * Return number of connectoids on the network
         * 
         * @return the number of connectoids
         */
        public int getNumberOfConnectoids() {
            return connectoidMap.size();
        }
        
        /**
         * Returns a List of registered connectoids
         * 
         * @return a List of registered connectoids
         */
        public List<Connectoid> toList() {
        	return new ArrayList<Connectoid>(connectoidMap.values());
        }
    }

    /**
     * Internal class for non-physical LinkSegment specific code, i.e. connectoid
     * segments (physical link segments are placed in the network)
     *
     */
    public class ConnectoidSegments {

        /**
         * Register a connectid segment on the zoning
         * 
         * @param connectoidSegment ConnectoidSegment to be registered
         * @return the registered connectoid segment
         */
        protected ConnectoidSegment registerConnectoidSegment(@Nonnull ConnectoidSegment connectoidSegment) {
            return connectoidSegmentMap.put(connectoidSegment.getId(), connectoidSegment);
        }

        /**
         * Return a List of registered ConnectoidSegment objects 
         * 
         * @return a List of registered ConnectoidSegment objects 
         */
        public List<ConnectoidSegment> toList() {
        	return new ArrayList<ConnectoidSegment>(connectoidSegmentMap.values());
        }
        
        /**
         * Create and register connectoid segment in AB direction on virtual network
         * 
         * @param parentConnectoid the connectoid which will contain this connectoid segment
         * @param externalId the external Id of the connectoid segment (can be null, in which case the external Id was not set in the input files
         * @param directionAB direction of travel
         * @return created connectoid segment
         * @throws PlanItException thrown if there is an error
         */
        public ConnectoidSegment createAndRegisterConnectoidSegment(@Nonnull Connectoid parentConnectoid, boolean directionAB) throws PlanItException {
            ConnectoidSegment connectoidSegment = new ConnectoidSegment(parentConnectoid, directionAB);
            parentConnectoid.registerConnectoidSegment(connectoidSegment, directionAB);
            registerConnectoidSegment(connectoidSegment);
            return connectoidSegment;
        }

        /**
         * Get connectoid segment by id
         * 
         * @param id the id of this connectoid segment
         * @return retrieved ConnectoidSegment object
         */
        public ConnectoidSegment getConnectoidSegment(long id) {
            return connectoidSegmentMap.get(id);
        }

        /**
         * Return the number of registered connectoid segments
         * 
         * @return the number of registered connectoid segments
         */
        public int getNumberOfConnectoidSegments() {
            return connectoidSegmentMap.size();
        }
    }

    /**
     * Internal class for all Centroid specific code
     *
     */
    public class Centroids  {

        /**
         * Add centroid to the internal container
         * 
         * If centroid overrides an existing centroid, the removed centroid is returned
         * 
         * @param centroid
         *            centroid to be registered
         * @return registered centroid
         */
        public Centroid registerCentroid(@Nonnull Centroid centroid) {
            return centroidMap.put(centroid.getId(), centroid);
        }

        /**
         * Return List of Centroids
         * 
         * @return List of Centroids
         */
        public List<Centroid> toList() {
        	return new ArrayList<Centroid>(centroidMap.values());
        }

        /**
         * Return number of registered centroids
         * 
         * @return number of registered centroids
         */
        public int getNumberOfCentroids() {
            return centroidMap.size();
        }
    }

    // Protected

    // for now use tree map to ensure non-duplicate keys until we add functionality
    // to account for this (treemap is slower than hashmap)
    protected Map<Long, Connectoid> connectoidMap = new TreeMap<Long, Connectoid>();
    protected Map<Long, ConnectoidSegment> connectoidSegmentMap = new TreeMap<Long, ConnectoidSegment>();
    protected Map<Long, Centroid> centroidMap = new TreeMap<Long, Centroid>();

    // PUBLIC

    /**
     * internal class instance containing all connectoid specific functionality
     */
    public final Connectoids connectoids = new Connectoids();
    /**
     * internal class instance containing all connectoid segment specific
     * functionality
     */
    public final ConnectoidSegments connectoidSegments = new ConnectoidSegments();
    /**
     * internal class instance containing all centroid specific functionality
     */
    public final Centroids centroids = new Centroids();

}
