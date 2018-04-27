package utils;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerAcquisitionStartResponse;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTaskInfoResponse;

public class CustomizedToStringTest {

	@Test
	public void test() {
		
		ResourceManagerAcquisitionStartResponse r=new ResourceManagerAcquisitionStartResponse();
		
		ResourceManagerTaskInfoResponse tir=new ResourceManagerTaskInfoResponse();
		
		List<ResourceManagerTaskInfoResponse> tasks=new ArrayList<ResourceManagerTaskInfoResponse>();
		tasks.add(tir);
		
		r.setTasks(tasks);

		String s=CustomizedToString.toString(r);
		assertNotNull(s);
	}

}
