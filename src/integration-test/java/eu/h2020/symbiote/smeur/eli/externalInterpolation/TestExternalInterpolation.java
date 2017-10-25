package eu.h2020.symbiote.smeur.eli.externalInterpolation;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.model.cim.ObservationValue;
import eu.h2020.symbiote.model.cim.Property;
import eu.h2020.symbiote.model.cim.UnitOfMeasurement;
import eu.h2020.symbiote.model.cim.WGS84Location;
import eu.h2020.symbiote.smeur.StreetSegment;
import eu.h2020.symbiote.smeur.StreetSegmentList;
import eu.h2020.symbiote.smeur.eli.RegionInformation;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class TestExternalInterpolation {

	static final File workingDirBase=new File("InterpolatorWorkingDir");	// TODO: Not hardcoded. Set from the config file.
	static final File resourceDir=new File("src/integration-test/resources/eu/h2020/symbiote/smeur/eli/externalInterpolation/testExternalInterpolation");

	
	
	RegionInformation regInfo;
	List<Observation> obs;
	
	@Rule public TestName testName=new TestName();
	
	@Before
	public void beforeTest() throws IOException {
		String tn=testName.getMethodName();
		File testResource=new File(resourceDir, tn);
		
		if (testResource.exists()) {
			FileUtils.copyDirectory(testResource, workingDirBase);
		}
		

		
		StreetSegmentList ssl=new StreetSegmentList();
		StreetSegment s1=new StreetSegment();
		
		s1.id="s1";
		s1.segmentData=new WGS84Location[] {new WGS84Location(1.0, 1.0, 0.0, null, null)};
		s1.comment="Weg1";
		ssl.put("s1", s1);
		
		
		regInfo=new RegionInformation();
		regInfo.regionID="testRegion";
		regInfo.theList=ssl;

		ObservationValue ob1V1=new ObservationValue("3.14", new Property("NO", null), new UnitOfMeasurement("ppm", "ppm", Arrays.asList(new String[] {"no comment"})));
		ObservationValue[] values=new ObservationValue[] {ob1V1};
		
		Observation ob1=new Observation("Sensor1", new WGS84Location(1.1, 1.1, 0.0, null, null), "T1", "T2", Arrays.asList(values));
		
		obs=new ArrayList<Observation>();
		obs.add(ob1);

		
		
	}
	
	@After
	public void afterTest() throws IOException {

		try {
			Thread.sleep(1000); // No idea why this is needed but it makes the test more stable
		} catch (InterruptedException e) {
		}	
		IOFileFilter ageFilter=FileFilterUtils.ageFileFilter(new Date());
		Collection<File> allOldFiles=FileUtils.listFilesAndDirs(workingDirBase, ageFilter, ageFilter);
		for (File f : allOldFiles) {
			if (f.equals(workingDirBase))
				continue;
			if (f.exists())	// It might have been deleted already with it's directory.
				FileUtils.forceDelete(f);
				
		}
	}
	
	/**
	 * Test the preliminaries for an external call, i.e. the creation of the working directory and the creation off all input files
	 */
	@Test
	public void testPrepareCall() {
		InterpolationRunner im = new InterpolationRunner(null);
		
		
		Date dirDate=new Date(0);
		
		// !!!
		File wd=im.prepareCall(regInfo, obs, dirDate);
		
		assertNotNull(wd);
		assertTrue(wd.exists());
		assertTrue(wd.isDirectory());
		Collection<File> allFiles=FileUtils.listFiles(wd, FileFilterUtils.trueFileFilter(), FileFilterUtils.falseFileFilter());
		assertEquals(2, allFiles.size());
		// TODO: Test which files are there
	}

	
	@Test
	public void testCallInterpolation() {
		// We do the test with a dirty trick. The script called here is a fakedInterpolation that just creates some correct output.
		// It's not about testing the script here, it's about calling the correct script with the right parameters
		
		InterpolationRunner im = new InterpolationRunner(null);

		File theExecutable=new File(workingDirBase, "fakeInterpolation.py");
		File workingDir=new File(workingDirBase, "testregion");	// <-- Known because I assembled the resources for this test.

		File output=new File(workingDir, "interpolated.json");
		
		im.runInterpolator(theExecutable, workingDir);
		
		assertTrue(output.exists());
		assertFalse(output.isDirectory());
		assertTrue(output.canRead());
	}
	

	@Test
	public void testReadinResults() {
		
		File workingDir=new File(workingDirBase, "testregion");
		
		InterpolationRunner im = new InterpolationRunner(null);
		
		im.retrieveResults(regInfo, workingDir);
		
		// TODO: Do some meaningful testing here. We will do that once we are more aware of how the output will look
	}

}
