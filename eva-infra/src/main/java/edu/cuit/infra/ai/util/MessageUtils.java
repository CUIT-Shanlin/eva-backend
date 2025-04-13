package edu.cuit.infra.ai.util;

import java.util.List;

public class MessageUtils {

    public static String convertToList(List<String> strings) {
        StringBuilder builder = new StringBuilder();
        for (String str : strings) {
            builder.append("- ").append(str).append("\n");
        }
        return builder.toString();
    }

}
