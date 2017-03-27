package me.jiudeng.purchase.network;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import me.jiudeng.purchase.listener.OnResponseListener;

/**
 * Created by Yin on 2016/3/29.
 * 网络请求工具类
 */
public class HttpUtil {

    private static final String TAG = "HttpUtil";
    public static String addressGetData = "http://10.0.0.165:9999/warehouseorder/OperatorPurchase";  //请求数据服务器地址
    public static String addressPushData = "http://10.0.0.165:9999/warehouseorder/PurchaseTruthInfo";  //提交数据服务器地址
    public static String addressUsr = "http://10.0.0.165:9999/warehouseorder/loginnormal";    //账户验证地址
//    public static String addressGetData = "http://ck.jiudeng.net/warehouseorder/OperatorPurchase";  //请求数据服务器地址
//    public static String addressPushData = "http://ck.jiudeng.net/warehouseorder/PurchaseTruthInfo";  //提交数据服务器地址
//    public static String addressUsr = "http://ck.jiudeng.net/warehouseorder/loginnormal";    //账户验证地址

    /**
     * 请求数据
     *
     * @param address
     * @param usr
     * @param listener
     */
    public static void SendHttpRequest(final String address, final String usr, final OnResponseListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(address);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);

//                    String data = "Operator=123";
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setRequestProperty("Conent-Length", usr.length() + "");

                    DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
                    outputStream.write(usr.getBytes());

                    InputStream inputStream = connection.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader reader = new BufferedReader(inputStreamReader);

                    StringBuilder response = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    if (listener != null) {
                        listener.onResponse(response.toString());
                        Log.d(TAG, "数据请求 response = " + response.toString());
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    if (listener != null) {
                        listener.onError(e);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (listener != null) {
                        listener.onError(e);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }

            }
        }).start();
    }

    /**
     * 请求用户登录
     *
     * @param address
     * @param uer
     * @param pwd
     * @param listener
     */
    public static void sendHttpRequestPost(final String address, final String uer, final String pwd, final OnResponseListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(address);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    connection.setDoOutput(true);

                    String data = "Account=" + uer + "&Password=" + pwd;

                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setRequestProperty("Conent-Length", data.length() + "");

                    DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
//                    outputStream.writeBytes("Account=" + uer + "&Password=" + pwd);
                    outputStream.write(data.getBytes());

                    if (connection.getResponseCode() == 200) {
                        Log.d(TAG, "请求成功！！！");
                        InputStream inputStream = connection.getInputStream();
                        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                        BufferedReader reader = new BufferedReader(inputStreamReader);

                        StringBuilder response = new StringBuilder();
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }

                        if (listener != null) {
                            listener.onResponse(response.toString());
                            Log.d(TAG, "response = " + response.toString());
                        }
                        reader.close();
                        inputStream.close();
                        outputStream.close();
                    } else {
                        Log.d(TAG, "请求失败！！！");
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    if (listener != null) {
                        listener.onError(e);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (listener != null) {
                        listener.onError(e);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }).start();

    }

    /**
     * 上传数据
     *
     * @param address
     * @param data
     * @param listener
     */
    public static void postDataToServer(final String address, final String data, final OnResponseListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(address);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    connection.setDoOutput(true);

                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setRequestProperty("Conent-Length", data.length() + "");

                    DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
                    outputStream.write(data.getBytes());

                    if (connection.getResponseCode() == 200) {
                        Log.d(TAG, "请求成功！！！");
                        InputStream inputStream = connection.getInputStream();
                        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                        BufferedReader reader = new BufferedReader(inputStreamReader);

                        StringBuilder response = new StringBuilder();
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }

                        if (listener != null) {
                            Log.d(TAG, "response = "+response);
                            listener.onResponse(response.toString());
                        }

                        reader.close();
                        inputStream.close();
                        outputStream.close();
                    } else {
                        Log.d(TAG, "请求失败！！！");
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    if (listener != null) {
                        listener.onError(e);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (listener != null) {
                        listener.onError(e);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }

    public static void checkUsrFromService(final String address) {
//       HttpClient
    }
}
