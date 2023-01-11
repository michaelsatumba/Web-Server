package server.config;

import java.util.concurrent.ConcurrentHashMap;

public class MimeTypes {
    private static ConcurrentHashMap<String[], String> mimeTypesMap;

    public MimeTypes(ConcurrentHashMap<String[], String> mimeTypes) {
        mimeTypesMap = mimeTypes;
    }

    public ConcurrentHashMap<String[], String> getMap() {
        return mimeTypesMap;
    }

}
