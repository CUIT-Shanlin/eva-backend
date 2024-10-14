package edu.cuit.app.util;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * URI相关工具类
 */
public class UriUtils {

    /**
     * 读取uri中的所有query参数
     * @param uri uri
     */
    public static Map<String,String> decodeQueryParams(URI uri) {
        // 读取所有query参数
        Map<String,String> attributes = new HashMap<>();
        String query = uri.getQuery();
        if (query != null) {
            String[] queryParams = query.split("&");
            for (String queryParam : queryParams) {
                String[] keyValue = queryParam.split("=");
                if (keyValue.length == 2) {
                    attributes.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return attributes;
    }

}
