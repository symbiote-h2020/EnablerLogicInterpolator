package eu.h2020.symbiote.smeur.eli;

import eu.h2020.symbiote.model.cim.WGS84Location;

/**
 * A small collection of things that come handy.
 * @author DuennebeilG
 *
 */
public class Utils {
	
	
	public static WGS84Location calculateCenter(WGS84Location[] points) {
		double lon=0.0;
		double lat=0.0;

		for (WGS84Location p : points) {
			lon+=p.getLongitude();
			lat+=p.getLatitude();
		}
		

		lon/=points.length;
		lat/=points.length;

		WGS84Location center=new WGS84Location(lon, lat, 0.0, null, null);

		return center;
	}
}
