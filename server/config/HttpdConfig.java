package server.config;

import java.util.concurrent.ConcurrentHashMap;

public class HttpdConfig {
    // * Hash Map to hold directives, aliases and paths
    private static ConcurrentHashMap<String,String> httpdConfigMap;

    public HttpdConfig(ConcurrentHashMap<String,String> config) {
        httpdConfigMap = config;
    }

    public int getListen(String listenPort) {
        return Integer.parseInt(httpdConfigMap.get(listenPort));
        // return 0;
    }

    public ConcurrentHashMap<String,String> getMap() {
        return httpdConfigMap;
    }

    public void printAll() {
        System.out.println(httpdConfigMap.entrySet());
    }

    public String getabAlias(String abAlias) {
        return httpdConfigMap.get(abAlias);
    }

    public String getDocumentRoot(String documentRoot) {
        return httpdConfigMap.get(documentRoot);
    }

    public String getDirectoryIndex() {
        return "index.html";
    }

    public String getScriptAlias(String scriptAlias) {
        return httpdConfigMap.get(scriptAlias);
    }

    public String getAccessFile() {
        return httpdConfigMap.get("AccessFile");
    }
}
