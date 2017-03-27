package me.jiudeng.purchase.utils;

import android.content.Context;
import android.net.ConnectivityManager;

/**
 * Created by Yin on 2016/3/31.
 * 检查网络连接
 */
public class NetWorkStateUtil {

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager != null) {
            boolean isAivilable = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnectedOrConnecting();
            boolean wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting();

            if (isAivilable | wifi){
                return true;
            }
        }
        return false;
    }

//    public static boolean isWifiEnabled(Context context) {
//        ConnectivityManager mgrConn = (ConnectivityManager) context
//                .getSystemService(Context.CONNECTIVITY_SERVICE);
//        TelephonyManager mgrTel = (TelephonyManager) context
//                .getSystemService(Context.TELEPHONY_SERVICE);
//        return ((mgrConn.getActiveNetworkInfo() != null && mgrConn
//                .getActiveNetworkInfo().getState() == NetworkInfo.State.CONNECTED) || mgrTel
//                .getNetworkType() == TelephonyManager.NETWORK_TYPE_UMTS);
//    }
}
