package server.config;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Base64;
import java.nio.charset.Charset;
import java.security.MessageDigest;

import java.io.IOException;
import java.io.*;
import java.util.*;

public class Htpassword extends Configuration {
  private HashMap<String, String> passwords;
  private String htpasswordFile;

  public Htpassword(String filename) throws IOException {
    super(filename);
    System.out.println("Password file: " + filename);

    this.passwords = new HashMap<String, String>();
    htpasswordFile = "";
    this.load();
  }

  protected void parseLine(String line) {
    String[] tokens = line.split(":");

    if (tokens.length == 2) {
      passwords.put(tokens[0], tokens[1].replace("{SHA}", "").trim());
    }
  }

  public boolean isAuthorized(String authInfo) {
    // authInfo is provided in the header received from the client
    // as a Base64 encoded string.
    String credentials = new String(Base64.getDecoder().decode(authInfo), Charset.forName("UTF-8"));

    // * Parse and put htAccess items in the hash map
    System.out.println("⏳ Loading file...");
    try {
      // * Read in .htpassword
      htpasswordFile = getHtpwdFile();
      System.out.println("htpassword file: " + htpasswordFile);
      FileInputStream fis = new FileInputStream(htpasswordFile);
      DataInputStream dis = new DataInputStream(fis);
      BufferedReader br = new BufferedReader(new InputStreamReader(dis));
      String line;
      while ((line = br.readLine()) != null) {
        parseLine(line);
      }
      System.out.println("passwords keyset: " + passwords.keySet());
      br.close();
      dis.close();
      fis.close();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      System.out.println("✅ Successfully read in " + getHtpwdFile() + "\n");
    }
    String username = getKeyByValue(htpwdMap, "notUsed");
    if(passwords.isEmpty() || verifyPassword(username, passwords.get(username))) {
      return false;
    }
    return true;

  }

  private boolean verifyPassword(String username, String password) {
    // encrypt the password, and compare it to the password stored
    // in the password file (keyed by username)
    // TODO: implement this - note that the encryption step is provided as a method,
    // below
    String encPwd = encryptClearPassword(password);
    if (encPwd == passwords.get(username)) {
      return true;
    }
    return false;
  }

  private String encryptClearPassword(String password) {
    // Encrypt the cleartext password (that was decoded from the Base64 String
    // provided by the client) using the SHA-1 encryption algorithm
    try {
      MessageDigest mDigest = MessageDigest.getInstance("SHA-1");
      byte[] result = mDigest.digest(password.getBytes());

      return Base64.getEncoder().encodeToString(result);
    } catch (Exception e) {
      return "";
    }
  }

  // * Get absolute path of .htpassword
  public String getAuthUserFile() {
    return htpwdMap.get("AuthUserFile");
  }

  // * Only need to support "Basic"
  public String getAuthType() {
    return htpwdMap.get("AuthType");
  }

  // * Get the name provided in the authentication window provided by clients
  public String getAuthName() {
    return htpwdMap.get("AuthName");
  }

  // * Get the user or group that can access a resource ("valid-user")
  public String getReq() {
    return htpwdMap.get("Require");
  }

  public static String getKeyByValue(HashMap<String, String> map, String key) {
    boolean flag = true;
    for (Entry<String, String> entry : map.entrySet()) {
        // if (Objects.equals(key, entry.getValue())) {
          if(flag) {
            flag = false;
            return entry.getKey();
          } 
          
        // }
    }
    return null;
  }
}