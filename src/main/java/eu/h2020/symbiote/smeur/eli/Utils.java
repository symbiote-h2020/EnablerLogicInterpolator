package eu.h2020.symbiote.smeur.eli;

import eu.h2020.symbiote.cloud.model.data.observation.Location;

/**
 * A small collection of things that come handy.
 * @author DuennebeilG
 *
 */
public class Utils {
	
	
	public static Location calculateCenter(Location[] points) {
		double lon=0.0;
		double lat=0.0;

		for (Location p : points) {
			lon+=p.getLongitude();
			lat+=p.getLatitude();
		}
		

		lon/=points.length;
		lat+=points.length;

		Location center=new Location(lon, lat, 0.0, null, null);

		return center;
	}
}
