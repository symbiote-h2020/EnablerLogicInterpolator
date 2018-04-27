package eu.h2020.symbiote.enabler.messaging.model;

import utils.CustomizedToString;

public class ResourceManagerTaskInfoResponse_toString {

	public static String toString(ResourceManagerTaskInfoResponse tir) {
		
		StringBuffer buffer=new StringBuffer();
		
		buffer.append("A ResourceManagerTaskInfoResponse with:\n");
		buffer.append(CustomizedToString.toString(tir.getResourceDescriptions()));
		
		return buffer.toString();
	}
}
