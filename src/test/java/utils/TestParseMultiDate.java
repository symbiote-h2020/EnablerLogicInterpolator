package utils;

import static org.junit.Assert.*;

import java.time.Instant;

import org.junit.Test;

import eu.h2020.symbiote.smeur.eli.Utils;

public class TestParseMultiDate {

	@Test
	public void test() {
		
		Instant inst=Utils.parseMultiFormat("2017-09-28T09:01:14.4Z");
	}

}
