package eu.h2020.symbiote.smeur.eli;

import static org.junit.Assert.*;

import eu.h2020.symbiote.model.cim.Property;
import eu.h2020.symbiote.smeur.StreetSegmentList;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

public class TestRegionInfo {

	@Test
	public void testEquals() {
		
		RegionInformation regInfo=new RegionInformation();
		
		regInfo.regionID="regID";
		
		regInfo.dateRegistered="someDate";
		
		regInfo.properties=new HashSet<Property>();
		regInfo.properties.add(new Property("NO", "http://neverla.nd/some/semantic", Arrays.asList(new String[] {"NitrogenOxyde"})));
		
		regInfo.theList=new StreetSegmentList();
		
		regInfo.yWantsPushing=true;
		
		
		
		RegionInformation regInfo2=new RegionInformation(regInfo);
		assertEquals(regInfo, regInfo2);
		
		assertEquals(regInfo, regInfo);
		assertNotEquals(regInfo, null);
		assertNotEquals(regInfo, "SomeString");

		regInfo2.regionID=null;
		assertNotEquals(regInfo, regInfo2);
		
		regInfo2.regionID=regInfo.regionID;
		regInfo2.properties=null;
		assertNotEquals(regInfo, regInfo2);
		
		regInfo2.properties=regInfo.properties;
		regInfo2.theList=null;
		assertNotEquals(regInfo, regInfo2);
		
		regInfo2.theList=regInfo.theList;
		regInfo2.dateRegistered=null;
		assertNotEquals(regInfo, regInfo2);
		
		regInfo2.dateRegistered=regInfo.dateRegistered;
		regInfo2.yWantsPushing=!regInfo.yWantsPushing;
		assertNotEquals(regInfo, regInfo2);
		
		
	}

}
