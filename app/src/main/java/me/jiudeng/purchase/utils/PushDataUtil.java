package me.jiudeng.purchase.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import me.jiudeng.purchase.listener.OnResponseListener;
import me.jiudeng.purchase.network.HttpUtil;

/**
 * Created by Yin on 2016/4/7.
 */
public class PushDataUtil {

    private static final String TAG = "PushDataUtil";

    public static void pushData(Context context){
        final String fileContent = FileUtil.readFile(context);
        Log.d(TAG, "定时上传 当前数据 fileContent =" + fileContent);
        if (!TextUtils.isEmpty(fileContent) && !(fileContent.equals("[]"))) {
            final String data = "PurchaseInfo="+PushDataUtil.createPushData(fileContent);
            HttpUtil.postDataToServer(HttpUtil.addressPushData, data, new OnResponseListener() {
                @Override
                public void onResponse(String response) throws JSONException {
                    Log.d(TAG, "定时上传完毕：" + System.currentTimeMillis() + "data = " + data);
                }

                @Override
                public void onError(Exception error) {
                    Log.d(TAG, "定时上传失败：" + System.currentTimeMillis() + "data = " + data);
                }
            });
        }
    }

    /**
     * 制定上传数据格式
     * @param fileContent
     * @return
     */
    public static String createPushData(String fileContent) {
        JSONArray array1 = null;
        JSONArray array2 = null;
        try {
            array1 = new JSONArray(fileContent);
            array2 = new JSONArray();
            for (int i = 0; i < array1.length(); i++) {
                JSONObject object = (JSONObject) array1.get(i);
                int PurchasedId = object.getInt("InfoId");

                double Price = (double) object.get("BuyPrice");
//                float Price1 = (float) Price*1000;
                float Price1 = (float) Price;
//                float Price = (float) object.get("BuyPrice");

                double Number = (double) object.get("MountPur");
//                float Number = (float) object.getDouble("MountPur");
                float Number1 = (float) Number;

                String Operator = (String) object.get("Operator");

                JSONObject o = new JSONObject();
                o.put("Id", PurchasedId);
                o.put("Price", Price1);
                o.put("Number", Number1);
                o.put("Operator", Operator);
                array2.put(o);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return array2.toString();
    }
}
