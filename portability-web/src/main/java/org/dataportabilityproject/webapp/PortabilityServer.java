package org.dataportabilityproject.webapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PortabilityServer {

  /** Starts the Portability web server. */
  public static void main(String[] args) {
    SpringApplication.run(PortabilityServer.class, args);
  }

}
