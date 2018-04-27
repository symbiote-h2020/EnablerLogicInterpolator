package eu.h2020.symbiote.enabler.messaging.model;

import java.util.List;

import utils.CustomizedToString;

public class ResourceManagerAcquisitionStartResponse_toString {
	
	public static String toString(ResourceManagerAcquisitionStartResponse x) {
		StringBuffer buffer=new StringBuffer();
		
		buffer.append("A ResourceManagerAcquisitionStartResponse with:\n");
		buffer.append("Status:").append(x.getStatus()).append(" / message=").append(x.getMessage()).append("\n");
		
		List<ResourceManagerTaskInfoResponse> tasks=x.getTasks();
		buffer.append("Tasks are:\n");
		
		
		for (ResourceManagerTaskInfoResponse tir : tasks) {
			CustomizedToString.toString(tir);
		}
		
		return buffer.toString();
	}
}
