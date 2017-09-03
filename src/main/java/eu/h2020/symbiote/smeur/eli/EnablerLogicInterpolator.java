package eu.h2020.symbiote.smeur.eli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * First version of the enabler logic (interpolation)
 * @author DuennebeilG
 * @author Mario Kušek <mario.kusek@fer.hr>
 *
 */
@SpringBootApplication
@EnableDiscoveryClient
public class EnablerLogicInterpolator {

    public static void main(String[] args) {
		SpringApplication.run(EnablerLogicInterpolator.class, args);
	}
}
