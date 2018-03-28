package io.harness.network;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.common.annotations.VisibleForTesting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Localhost {
  private static final Logger logger = LoggerFactory.getLogger(Localhost.class);

  public static String getLocalHostAddress() {
    try {
      String hostIp = InetAddress.getLocalHost().getHostAddress();
      if (isBlank(hostIp)) {
        logger.warn("InetAddress host address was empty");
      } else if (hostIp.startsWith("127.")) {
        logger.warn("InetAddress host address was in the 127.0.0.0/8 range");
      } else {
        return hostIp;
      }
    } catch (Exception e) {
      logger.warn("Exception getting InetAddress host address", e);
    }

    try {
      String address = getAddress();
      if (address == null) {
        logger.warn("Didn't find network interface with IPV4 address that is not in 127.0.0.0/8");
      } else {
        return address;
      }
    } catch (Exception e) {
      logger.warn("Exception getting IP address from network interfaces", e);
    }

    return "0.0.0.0";
  }

  public static String getLocalHostName() {
    try {
      String hostname = executeHostname();
      if (isBlank(hostname)) {
        logger.warn("hostname -f command result was empty");
      } else if (hostname.contains(" ") || hostname.equals("localhost")) {
        logger.warn("hostname -f command returned: " + hostname);
      } else {
        return hostname;
      }
    } catch (Exception ex) {
      logger.warn("hostname -f command threw exception", ex);
    }

    try {
      String hostname = InetAddress.getLocalHost().getCanonicalHostName();
      if (isBlank(hostname)) {
        logger.warn("InetAddress canonical hostname was empty");
      } else if (hostname.equals("localhost")) {
        logger.warn("InetAddress canonical hostname was 'localhost'");
      } else {
        return hostname;
      }
    } catch (Exception e) {
      logger.warn("InetAddress canonical hostname threw exception", e);
    }

    try {
      String hostname = executeHostnameShort();
      if (isBlank(hostname)) {
        logger.warn("hostname -s command result was empty");
      } else if (hostname.contains(" ") || hostname.equals("localhost")) {
        logger.warn("hostname -s command returned: " + hostname);
      } else {
        return hostname;
      }
    } catch (Exception ex) {
      logger.warn("hostname -s command threw exception", ex);
    }

    try {
      String hostname = InetAddress.getLocalHost().getHostName();
      if (isBlank(hostname)) {
        logger.warn("InetAddress short hostname was empty");
      } else if (hostname.equals("localhost")) {
        logger.warn("InetAddress short hostname was 'localhost'");
      } else {
        return hostname;
      }
    } catch (Exception e) {
      logger.warn("InetAddress short hostname threw exception", e);
    }

    return "ip-" + getLocalHostAddress().replaceAll("\\.", "-") + ".unknown";
  }

  @VisibleForTesting
  static String getAddress() throws SocketException {
    Enumeration<NetworkInterface> nInterfaces = NetworkInterface.getNetworkInterfaces();
    while (nInterfaces.hasMoreElements()) {
      NetworkInterface networkInterface = nInterfaces.nextElement();
      Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
      while (inetAddresses.hasMoreElements()) {
        InetAddress inetAddress = inetAddresses.nextElement();
        String address = inetAddress.getHostAddress();
        if (inetAddress.getAddress().length != 4) {
          logger.warn("Enumerated inet address is not length 4: " + address);
        } else if (address.startsWith("127.")) {
          logger.warn("Enumerated inet address was in the 127.0.0.0/8 range: " + address);
        } else {
          return address;
        }
      }
    }
    return null;
  }

  @VisibleForTesting
  static String executeHostname() throws IOException, InterruptedException, ExecutionException {
    return new ProcessExecutor()
        .timeout(2, TimeUnit.SECONDS)
        .command("hostname", "-f")
        .readOutput(true)
        .start()
        .getFuture()
        .get()
        .getOutput()
        .getLines()
        .get(0);
  }

  @VisibleForTesting
  static String executeHostnameShort() throws IOException, InterruptedException, ExecutionException {
    return new ProcessExecutor()
        .timeout(2, TimeUnit.SECONDS)
        .command("hostname", "-s")
        .readOutput(true)
        .start()
        .getFuture()
        .get()
        .getOutput()
        .getLines()
        .get(0);
  }
}
