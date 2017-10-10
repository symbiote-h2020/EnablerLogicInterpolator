package eu.h2020.symbiote.smeur.eli.externalInterpolation;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import eu.h2020.symbiote.cloud.model.data.observation.Location;
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

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void addAll(StreetSegmentList newList) {
		for (Entry<String, StreetSegment> entry : newList.entrySet()) {
			String segmentID=entry.getKey();
			StreetSegment ss=entry.getValue();
			
			Location center=Utils.calculateCenter(ss.segmentData);
			
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

