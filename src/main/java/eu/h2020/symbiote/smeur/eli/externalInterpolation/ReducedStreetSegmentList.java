package eu.h2020.symbiote.smeur.eli.externalInterpolation;

import java.util.Arrays;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.h2020.symbiote.model.cim.WGS84Location;
import eu.h2020.symbiote.smeur.StreetSegment;
import eu.h2020.symbiote.smeur.StreetSegmentList;
import eu.h2020.symbiote.smeur.eli.Utils;

/**
 * Generally the same information as in the StreetSegmentList but only with the information
 * really used by the interpolator and structured in a less complex way.
 * @author DuennebeilG
 *
 */
public class ReducedStreetSegmentList extends HashMap<String, ReducedStreetSegment> {

	private static final Logger log = LoggerFactory.getLogger(ReducedStreetSegmentList.class);

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void addAll(StreetSegmentList newList) {

		log.info("Calculating reduced ssl for interpolation");

		for (Entry<String, StreetSegment> entry : newList.entrySet()) {
			String segmentID=entry.getKey();
			StreetSegment ss=entry.getValue();
			
			log.debug("Segment is {}", Arrays.toString(ss.segmentData));
			WGS84Location center=Utils.calculateCenter(ss.segmentData);
			log.debug("Center is {}", center);
			
			ReducedStreetSegment rss=new ReducedStreetSegment();
			rss.centerLat=center.getLatitude();
			rss.centerLon=center.getLongitude();
			rss.comment=ss.comment;
			
			this.put(segmentID, rss);
		}
	}
	
}


class ReducedStreetSegment {
	public double centerLon;
	public double centerLat;
	public String comment;
}

