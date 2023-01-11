package server.config;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;

public class Configuration {
    private String httpdConfFile = "";
    private String mimeTypesFile = "";
    private String htpasswordFile = "";
    static ConcurrentHashMap<String, String> configMap;
    static ConcurrentHashMap<String[], String> mimeTypesMap;
    static HashMap<String, String> htpwdMap;

    public Configuration(String filename) {
        this.htpasswordFile = filename; 
    }

    public Configuration(String httpdConfFile, String mimeTypesFile) {
        this.httpdConfFile = httpdConfFile;
        this.mimeTypesFile = mimeTypesFile;
    }

    public void readHttpdConfig() {
        try {
            System.out.println("⏳ Reading httpd.conf...");
            configMap = new ConcurrentHashMap<>();
            FileInputStream fis = new FileInputStream(httpdConfFile);
            DataInputStream dis = new DataInputStream(fis);
            BufferedReader br = new BufferedReader(new InputStreamReader(dis));
            StringBuilder sb = new StringBuilder();
            String line;
            String directive[];
            
            while((line = br.readLine()) != null) {
                if(!line.startsWith("#") && !line.isBlank()) {
                    directive = line.split(" ", 3);
                    if(directive[0].contains("Alias")) {
                        String aliasDir = directive[0].concat(" ").concat(directive[1]);
                        String path = directive[2].replaceAll("^\"+|\"+$", "");
                        configMap.put(aliasDir.toString().trim(), path.trim());  
                        // System.out.println(configMap.get(aliasDir));
  
                    } else {
                        String path = directive[1].replaceAll("^\"+|\"+$", "");
                        configMap.put(directive[0], path);
                        // System.out.println(configMap.get(directive[0]));
                    }

                    sb.append(line + "\r\n");
                } 
            }
            br.close();
            dis.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("✅ Successfully read in " + httpdConfFile + "\n");
        }
    }

    public void readMimeTypes() {
        try {
            System.out.println("⏳ Reading mime.types...");
            String[] fileExtension;
            mimeTypesMap = new ConcurrentHashMap<>();
            FileInputStream fis = new FileInputStream(mimeTypesFile);
            DataInputStream dis = new DataInputStream(fis);
            BufferedReader br = new BufferedReader(new InputStreamReader(dis));
            StringBuilder sb = new StringBuilder();
            String line;
            String mimeType[];
            while((line = br.readLine()) != null) {
                if(!line.startsWith("#") && !line.isBlank()) {
                    mimeType = line.split("\\s+", 2);                    
                    if(mimeType.length == 2) {                        
                        fileExtension = mimeType[1].trim().split("\\s+");
                        // System.out.println(mimeTypesMap.size() + " : " + mimeType[0] + " " + mimeType[1]);
                        mimeTypesMap.put(fileExtension, mimeType[0]);
                        // System.out.println(Arrays.toString(mimeTypesMap.get(mimeType[0])));
                        sb.append(line + "\r\n");
                    } 
                }
            }
            br.close();
            dis.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("✅ Successfully read in " + mimeTypesFile + "\n");
        }
    }

    public void load() {
        // * Parse and put htAccess items in the hash map
        // System.out.println("⏳ Loading file...");
        // try {
        //     FileInputStream fis = new FileInputStream(htpasswordFile);
        //     DataInputStream dis = new DataInputStream(fis);
        //     BufferedReader br = new BufferedReader(new InputStreamReader(dis));
        //     StringBuilder sb = new StringBuilder();
        //     String line;
        //     String htArr[];
        //     while((line = br.readLine()) != null) {
        //         if(!line.startsWith("#") && !line.isBlank()) {
        //             htArr = line.split(":", 2);    
        //             htArr[1].replace("{*}", "").trim();                
        //             if(htArr.length == 2) {             
        //                 System.out.println(htpwdMap.size() + " : " + htArr[0] + " " + htArr[1]);
        //                 htpwdMap.put(htArr[0], htArr[1]);
        //             } 
        //             sb.append(line + "\r\n");
        //         }
        //     }
        //     br.close();
        //     dis.close();
        //     fis.close();
        // } catch (IOException e) {
        //     e.printStackTrace();
        // } finally {
        //     System.out.println("✅ Successfully read in " + htpasswordFile + "\n");
        // }
    }

    public synchronized ConcurrentHashMap<String, String> getConfigMap() {
        return configMap;
    }

    public synchronized ConcurrentHashMap<String[], String> getMimeTypesMap() {
        return mimeTypesMap;
    }

    public synchronized String getHtpwdFile() {
        return htpasswordFile;
    }

    public String getDirective(String directive) {
        return configMap.get(directive);
    }
}