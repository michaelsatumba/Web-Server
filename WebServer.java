import server.*;

public class WebServer {
  public static void main(String[] args) {
    Server server = new Server();
    try {
      server.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}