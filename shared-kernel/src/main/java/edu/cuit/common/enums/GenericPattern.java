package edu.cuit.common.enums;

/**
 * 常用正则表达式
 */
public interface GenericPattern {

    //通过不带空格的字符串
    String NOT_CONTAIN_SPACE = "^[^\\s]*$";

    //通过只包含字母和数字的字符串
    String ONLY_LETTER_NUMBER = "^[a-zA-Z0-9]*$";

    //验证中国手机号
    String CHINA_PHONE = "^1(3[0-9]|4[01456879]|5[0-35-9]|6[2567]|7[0-8]|8[0-9]|9[0-35-9])\\d{8}$";

    //提取URI
    String URI_OF_URL = "https?://[^/]+(/[^?#]*)";

    //长度为4位的字符串
    String FLIGHT_NUMBER = "^.{4}$";

    String EMAIL = "^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$";

}
