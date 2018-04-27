package eu.h2020.symbiote.smeur.eli;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.h2020.symbiote.model.cim.WGS84Location;

/**
 * A small collection of things that come handy.
 * @author DuennebeilG
 *
 */
public class Utils {
	
	
	private static final Logger log = LoggerFactory.getLogger(Utils.class);

	
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
	

	static DateTimeFormatterBuilder dtfb=new DateTimeFormatterBuilder();
	
	static DateTimeFormatter[] formatters=new DateTimeFormatter[] {
			dtfb.appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").toFormatter()
	};
	
	
	
	public static Instant parseMultiFormat(String theTimeString) {
		log.info("Parsing the String \"{}\"", theTimeString);
		
		for (DateTimeFormatter formatter : formatters) {
			try {
				log.info("Trying to parse with {}", formatter.toString());
				Instant someTime=formatter.parse(theTimeString, Instant::from);
				return someTime;
			} catch(DateTimeParseException dte) {
				log.info("Parsing failed for {}", formatter.toString(), dte);				
			}
		}

		throw new DateTimeParseException("Unable to parse with any of the given formats:", theTimeString, 0);
	}
	
}
