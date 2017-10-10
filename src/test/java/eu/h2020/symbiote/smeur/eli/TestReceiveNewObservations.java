package eu.h2020.symbiote.smeur.eli;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.enablerlogic.EnablerLogic;
import eu.h2020.symbiote.smeur.StreetSegmentList;
import eu.h2020.symbiote.smeur.eli.persistance.PersistenceManager;

public class TestReceiveNewObservations {
	
	
	InterpolatorLogic il;
	EnablerLogic elMock;
	PersistenceManager pmMock;
	InterpolationManager imMock;
	
	@Before
	public void setUp() throws Exception {
		il=new InterpolatorLogic();
		elMock=mock(EnablerLogic.class);
		pmMock=mock(PersistenceManager.class);
		imMock=mock(InterpolationManager.class);
		
		il.setPersistenceManager(pmMock);
		il.setInterpolationManager(imMock);
		il.initialization(elMock);
	}


	@Test
	public void testErrorBehavior() {

		// Simply ignore malformed packages
		// Obvious, what's wrong here, isn't it?
		il.measurementReceived(null);
		verifyZeroInteractions(pmMock);


		reset(pmMock);
		// Malformed ID. ELI will only send out taskIDs with a certain string format.
		// In more detail: The ID must contain a colon. The part in front of the colon must be an sslID. the rest explains what has been queried.
		EnablerLogicDataAppearedMessage eldam=new EnablerLogicDataAppearedMessage();
		eldam.setTaskId("What the fuck");
		il.measurementReceived(eldam);
		verifyZeroInteractions(pmMock);

		
		reset(pmMock);
		// unknown ID
		eldam.setTaskId("This_has:a_colon");
		when(pmMock.yRegionExists("This_has")).thenReturn(false);
		
		il.measurementReceived(eldam);

		verify(pmMock, never()).persistObservations(Mockito.anyString(), Mockito.anyObject());
		verifyZeroInteractions(imMock);
		
	}

	@Test
	public void testNormalBehaviorFirstTime() {
		
		il.useCutoff(false);
		
		EnablerLogicDataAppearedMessage eldam=new EnablerLogicDataAppearedMessage();
		
		Observation obs=new Observation(null, null, null, null, null);
		
		List<Observation> observations=new ArrayList<Observation>();
		eldam.setObservations(observations);
		eldam.setTaskId("SomeID:fixed");
		
		observations.add(obs);
		
		
		RegionInformation regInfo=new RegionInformation();
		regInfo.theList=new StreetSegmentList();
		regInfo.yWantsPushing=false;

		
		// This is to control behavior:
		when(pmMock.yRegionExists("SomeID")).thenReturn(true);
		when(pmMock.retrieveObservations("SomeID")).thenReturn(null);
		when(pmMock.retrieveRegionInformation("SomeID")).thenReturn(regInfo);
		ArgumentCaptor<List> obsCapture = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<String> idCapture = ArgumentCaptor.forClass(String.class);

		// This is what we want to achieve
		doNothing().when(pmMock).persistObservations(idCapture.capture(), obsCapture.capture());
		

		// 3, 2, 1...fire
		il.measurementReceived(eldam);
		
		
		
		// Verify
		assertEquals(1, obsCapture.getAllValues().size());
		List obsCaptured=obsCapture.getValue();
		assertEquals(observations, obsCaptured);

		verify(imMock, times(1)).startInterpolation(Mockito.anyObject(), Mockito.anyList(), anyObject());

	}

	@Test
	public void testNormalBehaviorSecondTime() {
		
		il.useCutoff(false);
		
		Observation obs=new Observation("rID1", null, null, null, null);
		List<Observation> observations=new ArrayList<Observation>();
		observations.add(obs);

		EnablerLogicDataAppearedMessage eldam=new EnablerLogicDataAppearedMessage();		
		eldam.setObservations(observations);
		eldam.setTaskId("SomeID:fixed");
		

		Observation olderObs=new Observation(null, null, null, null, null);
		List<Observation> olderObservations=new ArrayList<Observation>();
		olderObservations.add(olderObs);

		RegionInformation regInfo=new RegionInformation();
		regInfo.theList=new StreetSegmentList();
		
		
		// This is to control behavior:
		when(pmMock.yRegionExists("SomeID")).thenReturn(true);
		when(pmMock.retrieveObservations("SomeID")).thenReturn(olderObservations);
		when(pmMock.retrieveRegionInformation("SomeID")).thenReturn(regInfo);
		
		ArgumentCaptor<List> obsCapture = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<String> idCapture = ArgumentCaptor.forClass(String.class);

		ArgumentCaptor<RegionInformation> regInfoCapture=ArgumentCaptor.forClass(RegionInformation.class);
		ArgumentCaptor<List> observationCapture=ArgumentCaptor.forClass(List.class);

		// This is what we want to achieve
		doNothing().when(pmMock).persistObservations(idCapture.capture(), obsCapture.capture());
		doNothing().when(imMock).startInterpolation(regInfoCapture.capture(), observationCapture.capture(), anyObject());
		

		// 3, 2, 1...fire
		il.measurementReceived(eldam);
		
		assertEquals(obsCapture.getAllValues().size(), 1);
		List<Observation> obsCaptured=obsCapture.getValue();
		assertEquals(2, obsCaptured.size());
		
		verify(imMock, times(1)).startInterpolation(Mockito.anyObject(), Mockito.anyList(), Mockito.anyObject());
	}

	
	/**
	 * Test some border cases.
	 */
	@Test
	public void testMergeObservationsNonStandardBehavior() {
		List<Observation> merged=il.mergeObservations(null, null, null);
		assertNull(merged);

		
		Observation obs=new Observation(null, null, null, null, null);
		List<Observation> observations=new ArrayList<Observation>();
		observations.add(obs);

		merged=il.mergeObservations(observations, null, null);
		assertEquals(merged, observations);
		
		merged=il.mergeObservations(null, observations, null);
		assertEquals(merged, observations);
		
	}

	@Test
	public void testMergeObservationsStandard() {

		Observation obs=new Observation("rID1", null, "Result1", null, null);
		List<Observation> observations=new ArrayList<Observation>();
		observations.add(obs);

		Observation obs2=new Observation("rID1", null, "Result2", null, null);
		List<Observation> observations2=new ArrayList<Observation>();
		observations2.add(obs2);

		Observation obs3=new Observation(obs2);
		List<Observation> observations3=new ArrayList<Observation>();
		observations3.add(obs3);
		

		List<Observation> merged = il.mergeObservations(observations, observations2, null);
		assertEquals(2, merged.size());
		
		assertTrue(merged.contains(obs));
		assertTrue(merged.contains(obs2));

		// Test against the merged list, adding a similar observations again
		merged = il.mergeObservations(merged, observations3, null);
		assertEquals(2, merged.size());	// The doubled obs does not occur!

		assertTrue(merged.contains(obs));
		assertTrue(merged.contains(obs2));
		
	}

	
	@Test
	public void testMergeObservationsCutoff() {

		// Note: There is some internal knowledge in this test (It's a grey box test):
		// Cutoff functionality is working when a cutoff time is given.
		// Cutoff is done on the merged result so it is possible to just feed in one list into the merge (see tests above).
		
		String sBaseTimeForTest="2016-11-11T01:00:00Z";	// This is the time we use for the testing
		Instant instBaseTimeForTest=Instant.parse(sBaseTimeForTest);
		
		Instant cutoffTime1=instBaseTimeForTest.minusSeconds(30*60);
		Instant cutoffTime2=instBaseTimeForTest.minusSeconds(60*60);

//		DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME;
//		
//		TemporalAccessor accessor = timeFormatter.parse("2016-11-11T11:11:11Z");	// Basetime for test
//
//		long lBaseTimeForTest=accessor.

		List<Observation> observations=new ArrayList<Observation>();
		
		Observation obs=new Observation("rID1", null, cutoffTime2.toString(), cutoffTime2.toString(), null);
		observations.add(obs);

		Observation obs1=new Observation("rID1", null, cutoffTime1.toString(), cutoffTime1.toString(), null);
		observations.add(obs1);

		List<Observation> merged = il.mergeObservations(observations, null, null);
		assertEquals(2, merged.size());
		
		assertTrue(merged.contains(obs));
		assertTrue(merged.contains(obs1));

		merged = il.mergeObservations(merged, null, cutoffTime2);
		assertEquals(2, merged.size());

		merged = il.mergeObservations(merged, null, cutoffTime1);
		assertEquals(1, merged.size());

		assertTrue(merged.contains(obs1));
		
	}

	
	@Test
	public void testOnInterpolationDone() {
		
		RegionInformation regInfo=new RegionInformation();
		regInfo.regionID="SomeID";
		
		StreetSegmentList ssl=new StreetSegmentList();
		
		// No push
		regInfo.yWantsPushing=false;

		il.OnInterpolationDone(regInfo, ssl);

		verify(pmMock, times(1)).persistInterpolatedValues("SomeID", ssl);
		verify(elMock, times(0)).sendAsyncMessageToEnablerLogic(Mockito.anyString(), Mockito.anyObject());

		reset(elMock, pmMock);
		
		// Push
		regInfo.yWantsPushing=true;
		
		
		il.OnInterpolationDone(regInfo, ssl);

		verify(pmMock, times(1)).persistInterpolatedValues("SomeID", ssl);
		verify(elMock, times(1)).sendAsyncMessageToEnablerLogic(Mockito.anyString(), Mockito.anyObject());
		
	}
	
}
