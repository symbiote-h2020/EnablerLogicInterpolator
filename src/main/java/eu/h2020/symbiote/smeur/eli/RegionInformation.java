package eu.h2020.symbiote.smeur.eli;


import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import java.util.Objects;

import eu.h2020.symbiote.cloud.model.data.observation.Property;
import eu.h2020.symbiote.smeur.StreetSegment;
import eu.h2020.symbiote.smeur.StreetSegmentList;


/**
 * This class is meant to be a container holding all information needed for a region/StreetSegmentList
 * @author DuennebeilG
 *
 */
public class RegionInformation {

	public String regionID;
	public Set<Property> properties;
	public StreetSegmentList theList;
	public boolean yWantsPushing;

	public String dateRegistered;	// Maybe we want to use this one of these days for some house keeping in the database.


	public RegionInformation() {
	}

	
	public RegionInformation(RegionInformation regInfo) {
		
		this.regionID=regInfo.regionID;
		this.properties=new HashSet<Property>(regInfo.properties);
		this.theList=new StreetSegmentList();
		
		for (Entry<String, StreetSegment> entry : regInfo.theList.entrySet()) {
			theList.put(entry.getKey(), entry.getValue());
		}
				
		this.yWantsPushing=regInfo.yWantsPushing;
		
		this.dateRegistered=regInfo.dateRegistered;
		
	}

	
	@Override
	public boolean equals(Object o) {
		if (o==null)
			return false;
		
		if (o==this)
			return true;
		
		if ( !(o instanceof RegionInformation) )
			return false;
		
		RegionInformation or=(RegionInformation)o;
		
		if (!Objects.equals(this.regionID, or.regionID))
			return false;
		
		if (!Objects.equals(this.properties, or.properties))
			return false;
		
		if (!Objects.equals(this.theList, or.theList))
			return false;
		
		if (!Objects.equals(this.dateRegistered, or.dateRegistered))
			return false;
		
		if (this.yWantsPushing!=or.yWantsPushing)
			return false;
		
		return true;
		
	}
	
}


