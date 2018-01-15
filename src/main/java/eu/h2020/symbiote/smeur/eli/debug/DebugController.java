package eu.h2020.symbiote.smeur.eli.debug;

import javax.ws.rs.QueryParam;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.h2020.symbiote.smeur.eli.InterpolatorLogic;

@RestController

public class DebugController {

	@RequestMapping("/")
	public String index() {

		return "Greetings from interpolator!";

	}

	@RequestMapping("/command")
	public String command(
			@QueryParam("cmd") String cmd,
			@QueryParam("reg") String reg
		) 
	{
		
		
		if (cmd == null) {
			return "please provide a command in form \"?cmd=XXX\"";
		}
		switch (cmd) {
		case "RequerySensors":
			if (reg==null) {
				return "Please provide a region id (\"reg=xxx\")";
			}
			

			InterpolatorLogic il=InterpolatorLogic.theIL;
			try {
				il.requeryStations(reg);
			} catch(IllegalArgumentException iae) {
				return iae.getMessage();
			}
			
			return "Requerying the sensors";
		default:
			return "Command "+cmd+" is unknown"; 
		}
	}
}