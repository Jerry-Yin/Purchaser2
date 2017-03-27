package me.jiudeng.purchase.utils;

import java.text.DecimalFormat;

/**
 * Created by Yin on 2016/4/9.
 */
public class FormatNumberUtil {

    /**
     * 当浮点型数据位数超过10位之后，数据变成科学计数法显示。用此方法可以使其正常显示。
     *
         DecimalFormat df = new DecimalFormat("###.00");
         System.out.println(df.format(double_value));
        这样就是保留小数点后两位小数,如果想保留三位,则为
         DecimalFormat df = new DecimalFormat("###.000");
     * @param value
     * @return Sting
     */
    public static String formatFloatNumber(double value) {
        if(value != 0.00){
            DecimalFormat df = new DecimalFormat("########.00");
            return df.format(value);
        }else{
            return "0.00";
        }
    }
    public static String formatFloatNumber(Double value) {
        if(value != null){
            if(value.doubleValue() != 0.00){
                DecimalFormat df = new DecimalFormat("########.00");
                return df.format(value.doubleValue());
            }else{
                return "0.00";
            }
        }
        return "";
    }

    public static String formatFloatNumber2(double value){
//        DecimalFormat df = new DecimalFormat("0");
        DecimalFormat df = new DecimalFormat("0.00");
        Double d = new Double(value);
        return df.format(d);
    }
}
