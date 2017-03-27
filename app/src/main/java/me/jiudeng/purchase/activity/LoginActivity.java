package me.jiudeng.purchase.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import me.jiudeng.purchase.R;
import me.jiudeng.purchase.network.HttpUtil;
import me.jiudeng.purchase.utils.NetWorkStateUtil;
import me.jiudeng.purchase.listener.OnResponseListener;

public class LoginActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "LoginActivity";
    /**
     * Views
     */
    private Button mBtnLogin;
    private EditText mEtUser, mEtPwd;

    /**
     * Values
     */
    private String mUser, mPwd;
    private ProgressDialog mProgDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.layout_login);

        getAccountToLogin();
        initViews();
    }

    private void initViews() {
        mBtnLogin = (Button) findViewById(R.id.btn_login);
        mBtnLogin.setOnClickListener(this);
        mBtnLogin.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startActivity(new Intent(LoginActivity.this, MainActivity2.class));
                LoginActivity.this.finish();
                return false;
            }
        });

        mEtUser = (EditText) findViewById(R.id.et_user);
        mEtPwd = (EditText) findViewById(R.id.et_pwd);

    }

    @Override
    public void onClick(View v) {
        if (checkAccountLogic()) {
            /** TODO 验证完成，跳转; 否则，弹出弹框dialog*/
            saveAccountMsg(mUser, mPwd);
            login(mUser, mPwd);
        }
    }

    /**
     * 检查网络，登录
     *
     * @param usr
     * @param pwd
     */
    private void login(String usr, String pwd) {
        if (NetWorkStateUtil.isNetworkAvailable(LoginActivity.this)) {
            checkAccountService(usr, pwd);
        } else {
            Toast.makeText(LoginActivity.this, "对不起，请检查网络连接！", Toast.LENGTH_SHORT).show();
            dissmissProgressDialog();
        }
    }

    private void checkAccountService(String usr, String pwd) {
        final Message message = new Message();
        HttpUtil.sendHttpRequestPost(HttpUtil.addressUsr, usr, pwd, new OnResponseListener() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject object = new JSONObject(response.toString());
                    //response = {"Code":320000,"Data":null,"Message":"账号格式错误"}
                    //response = {"Code":0,"Data":null,"Message":""}
                    if (object.getInt("Code") == 0) {
                        message.what = 1;
                        message.obj = response;
                        mHandler.sendMessage(message);
                    } else {
                        message.what = 2;
                        message.obj = object.getString("Message");
                        mHandler.sendMessage(message);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Exception error) {
                message.what = 2;
                message.obj = error;
                mHandler.sendMessage(message);
            }
        });
    }

    /**
     * 验证输入是否有逻辑错误
     */
    private boolean checkAccountLogic() {
        mUser = mEtUser.getText().toString().trim();
        mPwd = mEtPwd.getText().toString().trim();
        if (mUser == null || mUser.length() <= 0) {
            mEtUser.requestFocus();
            Toast.makeText(LoginActivity.this, "对不起，用户名不能为空", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (mPwd == null || mPwd.length() <= 0) {
            mEtPwd.requestFocus();
            Toast.makeText(LoginActivity.this, "对不起，密码不能为空", Toast.LENGTH_SHORT).show();
            return false;
        }
        showProgDialog();
        return true;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                dissmissProgressDialog();
                // TODO: 2016/4/1 保存账户信息到本地
//                saveAccountMsg(mUser, mPwd);
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                LoginActivity.this.finish();
            } else if (msg.what == 2) {
                dissmissProgressDialog();
                Toast.makeText(LoginActivity.this, "账号或密码错误！", Toast.LENGTH_SHORT).show();
            }
            super.handleMessage(msg);
        }
    };

    /**
     * 保存账户信息到本地
     * @param usr
     * @param pwd
     */
    private boolean saveAccountMsg(String usr, String pwd) {
        SharedPreferences.Editor editor = this.getSharedPreferences("uer_data", MODE_PRIVATE).edit();
        editor.putString("Account", usr);
        editor.putString("Password", pwd);
        return editor.commit();
    }

    private void getAccountToLogin() {
        SharedPreferences preferences = this.getSharedPreferences("uer_data", MODE_PRIVATE);
        String usr = preferences.getString("Account", null);
        String pwd = preferences.getString("Password", null);
        Log.d(TAG, "usr = " + usr + "pwd = " + pwd);
        if (!TextUtils.isEmpty(usr) && !TextUtils.isEmpty(pwd)) {
            login(usr, pwd);
        }
    }


    private void showProgDialog() {
        if (mProgDialog == null)
            mProgDialog = new ProgressDialog(this);
        mProgDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgDialog.setIndeterminate(false);
        mProgDialog.setCancelable(false);
        mProgDialog.setMessage("正在登录...");
        mProgDialog.show();
    }

    /**
     * 隐藏进度框
     */
    private void dissmissProgressDialog() {
        if (mProgDialog != null) {
            mProgDialog.dismiss();
        }
    }
}
