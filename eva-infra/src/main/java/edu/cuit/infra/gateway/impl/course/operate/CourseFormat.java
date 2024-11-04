package edu.cuit.infra.gateway.impl.course.operate;

public class CourseFormat {
    public static String toFormat(String str){
        StringBuffer string = new StringBuffer();
        int flag=0;
        for (char c : str.toCharArray()) {
            if(c=='[') flag=1;
            if(c==']') flag=0;
            if(flag==1&&c=='"') string.append("\\");
            string.append(c);
        }
        return string.toString();
    }
}
