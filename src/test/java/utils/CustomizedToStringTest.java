package utils;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import eu.h2020.symbiote.core.internal.CoreQueryRequest;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerAcquisitionStartResponse;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTaskInfoResponse;

public class CustomizedToStringTest {

	@Test
	@Ignore
	public void test() {
		
		CoreQueryRequest cqr=new CoreQueryRequest();
		ResourceManagerAcquisitionStartResponse r=new ResourceManagerAcquisitionStartResponse();
		
		ResourceManagerTaskInfoResponse tir=new ResourceManagerTaskInfoResponse(null, null, cqr, null, null, null, null, null, null, null, null, null, null, null);
		
		List<ResourceManagerTaskInfoResponse> tasks=new ArrayList<ResourceManagerTaskInfoResponse>();
		tasks.add(tir);
		
		r.setTasks(tasks);

		String s=CustomizedToString.toString(r);
		assertNotNull(s);
	}

}
