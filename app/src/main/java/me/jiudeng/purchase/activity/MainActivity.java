package me.jiudeng.purchase.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.jiudeng.purchase.R;
import me.jiudeng.purchase.module.PurchaseData;
import me.jiudeng.purchase.utils.CharacterParser;
import me.jiudeng.purchase.utils.FileUtil;
import me.jiudeng.purchase.utils.FormatNumberUtil;
import me.jiudeng.purchase.utils.GetFirstAlp;
import me.jiudeng.purchase.network.HttpUtil;
import me.jiudeng.purchase.listener.OnResponseListener;
import me.jiudeng.purchase.utils.PinyinComparator;
import me.jiudeng.purchase.utils.PushDataUtil;
import me.jiudeng.purchase.utils.SaveFileUtil;
import me.jiudeng.purchase.utils.TimeTaskUtil;
import me.jiudeng.purchase.view.SideBar;

/**
 * Created by Yin on 2016/3/29.
 * ListView 版本
 */
public class MainActivity extends BaseActivity implements View.OnClickListener {

    private static final int REFRESH_SUM = 5;
    private static final int SHOW_MESSAGE = 6;
    private static String TAG = "MainActivity";
    private static final int REFRESH_DATA = 0;
    private static final int SEND_SUCCESS = 1;
    private static final int SEND_FAIL = 2;
    private static final int PRICE_PUR = 3;
    private static final int MOUNT_PUR = 4;

    /**
     * Views
     */
    private ListView mListView;
    private Button mBtnLogOut, mBtnSend;
    private TextView mTvPaySum, mTvLineSum;    //采购金额合计
    private TextView mTvCurDialog;  //显示当前字母
    private SideBar mSideBar;

    /**
     * Values
     */
    private PurchaseListAdapter mListAdapter;
    private List<PurchaseData> mPurchaseList = new ArrayList<>();
    private CharacterParser mCharacterParser; //汉字转换成拼音的类
    private PinyinComparator mPinyinComparator; //根据拼音来排列ListView里面的数据类
    private Message message = new Message();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);
        initViews();
        initData();
    }

    private void initViews() {
        mBtnLogOut = (Button) findViewById(R.id.btn_logout);
        mBtnSend = (Button) findViewById(R.id.btn_send);
        mBtnLogOut.setOnClickListener(this);
        mBtnSend.setOnClickListener(this);
        mTvPaySum = (TextView) findViewById(R.id.tv_pay_sum);
        mTvLineSum = (TextView) findViewById(R.id.tv_moneyLine_sum);
        mTvCurDialog = (TextView) findViewById(R.id.tv_dialog);
        mSideBar = (SideBar) findViewById(R.id.side_bar);
        mSideBar.setTextView(mTvCurDialog);
        mListView = (ListView) findViewById(R.id.list_view);
        //设置右侧触摸监听
        mSideBar.setOnTouchingLetterChangedListener(new SideBar.OnTouchingLetterChangedListener() {

            @Override
            public void onTouchingLetterChanged(String s) {
                //该字母首次出现的位置
                int position = mListAdapter.getPositionForSection(s.charAt(0));
                if (position != -1) {
                    mListView.setSelection(position);
                }
            }
        });
        mListAdapter = new PurchaseListAdapter(MainActivity.this);
        mListView.setAdapter(mListAdapter);
    }

    private void initData() {
        // TODO: 2016/4/5 先判断本地数据是否存在，存在则加载本地数据，否则请求网络数据
        String fileContent = FileUtil.readFile(MainActivity.this);
        Log.d(TAG, "初始化数据...");
        if (!(TextUtils.isEmpty(fileContent)) && !(fileContent == null) && !(fileContent.equals("[]"))) {
            loadLocalData(fileContent);
        } else {
            requestDataFromService();
        }
    }

    private void loadLocalData(String fileContent) {
        try {
            paraseJsonData(fileContent.toString());
            Log.d(TAG, "成功读取本地文件数据，fileContent = " + fileContent);
            message.what = REFRESH_DATA;
            mHandler.sendMessage(message);
            mListAdapter.notifyDataSetChanged();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void requestDataFromService() {
        // TODO 网络请求，刷新界面; ---> 数据加载完毕后，调用定时任务定时上传，并且在关闭时结束任务
        HttpUtil.SendHttpRequest(HttpUtil.addressGetData, "Operator=" + getAccount(), new OnResponseListener() {
            @Override
            public void onResponse(String response) {
                JSONObject object = null;
                try {
                    object = new JSONObject(response);

                    if (object.getInt("Code") == 0) {
                        paraseJsonData(object.getString("Data"));     //解析数据，添加到list中
                        Log.d(TAG, "response = " + response.toString());
//                    addTestData();
                        message.what = REFRESH_DATA;
                        message.obj = response;
                        mHandler.sendMessage(message);
                    }else {
                        message.what = SHOW_MESSAGE;
                        message.obj = object.getString("Message");
                        mHandler.sendMessage(message);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Exception error) {

            }
        });
    }

    private String getAccount() {
        SharedPreferences preferences = this.getSharedPreferences("uer_data", MODE_PRIVATE);
        String usr = preferences.getString("Account", null);
        String pwd = preferences.getString("Password", null);
        Log.d(TAG, "usr = " + usr + "pwd = " + pwd);
        return usr;
    }

    private void paraseJsonData(String data) throws JSONException {
        Gson gson = new Gson();
        mPurchaseList = gson.fromJson(data, new TypeToken<List<PurchaseData>>() {
        }.getType());
        // TODO: 2016/4/1 设置参数

    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH_DATA:
//                    Log.d(TAG, "刷新界面---排序前的 mPurchaseList = " + mPurchaseList.toString());
                    mCharacterParser = CharacterParser.getInstance();
                    mPinyinComparator = new PinyinComparator();
                    // 根据a-z进行排序源数据
                    Collections.sort(mPurchaseList, mPinyinComparator);
//                    Log.d(TAG, "刷新界面---排序后的 mPurchaseList = " + mPurchaseList.toString());
//                    mListAdapter = new PurchaseListAdapter(MainActivity.this,  );

//                    mListView.setAdapter(mListAdapter);
                    mListAdapter.notifyDataSetChanged();
                    saveDataToFile();//请求成功后将数据保存到本地
                    new TimeTaskUtil(MainActivity.this).startTimeTask();   //开启定时上传功能
                    break;

                case SHOW_MESSAGE:
                    Toast.makeText(MainActivity.this, msg.obj.toString(), Toast.LENGTH_LONG).show();
                    break;

                case SEND_SUCCESS:
                    Toast.makeText(MainActivity.this, "上传完毕！", Toast.LENGTH_LONG).show();
                    mBtnSend.setTextColor(getResources().getColor(R.color.colorNormal));
                    mBtnSend.setFocusable(true);
                    break;

                case SEND_FAIL:
                    Toast.makeText(MainActivity.this, "上传失败，请重新上传！", Toast.LENGTH_LONG).show();
                    mBtnSend.setTextColor(getResources().getColor(R.color.colorNormal));
                    mBtnSend.setFocusable(true);
                    break;

                case REFRESH_SUM:
                    setSumData();
                    break;

                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    public void setSumData() {
        float sumMoney = 0;
        float sumLine = 0;
        if (mPurchaseList.size() != 0) {
            for (PurchaseData data : mPurchaseList) {
//                sumMoney += data.getMoneyPur();
                sumMoney += data.getBuyPrice()/1000 * data.getMountPur();
                sumLine += data.getSellPrice() / 1000 * data.getNeedNumbre();
            }
            mTvPaySum.setText("采购金额合计： ￥" + FormatNumberUtil.formatFloatNumber(sumMoney));
//            mTvLineSum.setText("线上金额合计： ￥"+sumLine);
//            mTvLineSum.setText("线上金额合计： ￥"+FormatNumberUtil.formatFloatNumber(sumLine));
            mTvLineSum.setText("线上金额合计： ￥" + FormatNumberUtil.formatFloatNumber(sumLine));
            ////            mTvLineSum.setText("线上金额合计： ￥"+sumLine);
////            mTvLineSum.setText("线上金额合计： ￥"+FormatNumberUtil.formatFloatNumber(sumLine));
//            mTvLineSum.setText("线上金额合计： ￥"+FormatNumberUtil.formatFloatNumber(sumLine));
//            Log.d(TAG, "sumline1 =" + sumLine);
//            Log.d(TAG, "sumline2 ="+FormatNumberUtil.formatFloatNumber(sumLine));
//            Log.d(TAG, "sumline3 ="+FormatNumberUtil.formatFloatNumber(sumLine));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_logout:
                // TODO: 2016/3/30  退出当前账号，弹出弹框；需要删除本地保存的用户信息，并退出到登录界面
                showDialogIos();
                break;

            case R.id.btn_send:
                // TODO: 2016/3/30 上传数据
                postCurDataToServer();
                mBtnSend.setFocusable(false);
                mBtnSend.setTextColor(getResources().getColor(R.color.colorAccent));
                break;
            default:
                break;
        }
    }

    private void postCurDataToServer() {
        if (mPurchaseList != null) {
            String dataContent = new Gson().toJson(mPurchaseList);
            final Message message = new Message();
            String data = "PurchaseInfo=" + PushDataUtil.createPushData(dataContent);
            HttpUtil.postDataToServer(HttpUtil.addressPushData, data, new OnResponseListener() {
                @Override
                public void onResponse(String response) throws JSONException {
                    JSONObject object = new JSONObject(response);
                    if (object.getInt("Code") == 0) {
                        message.what = SEND_SUCCESS;
                        message.obj = response;
                        mHandler.sendMessage(message);
                    } else {
                        message.what = SEND_FAIL;
                        mHandler.sendMessage(message);
                    }
                }

                @Override
                public void onError(Exception error) {
                    message.what = SEND_FAIL;
                    message.obj = error;
                    mHandler.sendMessage(message);
                }
            });
        }
    }

    private void showDialogIos() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        final Dialog dialog = builder.create();
        LinearLayout layout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.layout_ios_dialog, null);
        dialog.show();
        dialog.getWindow().setContentView(layout);

        layout.findViewById(R.id.btn_positive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 2016/3/30 删除账号信息
                logOut();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                MainActivity.this.finish();
            }
        });
        layout.findViewById(R.id.btn_negative).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    /**
     * 删除账号信息,退出登录
     */
    private void logOut() {
        SharedPreferences.Editor editor = this.getSharedPreferences("uer_data", MODE_PRIVATE).edit();
        editor.remove("Account");
        editor.remove("Password");
        editor.commit();
    }

    private void showDialogOld() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("确定退出？");
        builder.setCancelable(false);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO: 2016/3/30 删除账号信息

                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                MainActivity.this.finish();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    int mCurTouchIndex = -2;
    int mCurTouchIndex2 = -2;

    class PurchaseListAdapter extends BaseAdapter implements SectionIndexer {

        private Context mContext;
        private LayoutInflater inflater;
        private List<PurchaseData> mDataList;
        private int order = 0;

        public class ViewHolder {
            private TextView tvOrder;                  //序号
            private TextView tvProdName, tvProdMount;   //商品名称 & 规格
            private TextView tvPriceLine;       //线上价格
            private EditText etPricePur;        //采购价格
            private TextView tvMountDing;       //订购量
            private EditText etMountPur;        //采购量
            private TextView tvMoneyLine, tvMoneyPur;   //线上金额 & 采购金额
            private int key;    //改进 方法，添加次标签，通过标签来操作；

        }

        public PurchaseListAdapter(Context c, List<PurchaseData> mPurchaseList) {
            this.mContext = c;
            this.inflater = LayoutInflater.from(c);
            this.mDataList = mPurchaseList;
        }

        public PurchaseListAdapter(Context c) {
            this.mContext = c;
            this.inflater = LayoutInflater.from(c);
        }

        @Override
        public int getCount() {
            return mPurchaseList.size();
        }

        @Override
        public Object getItem(int position) {
            return mPurchaseList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = inflater.inflate(R.layout.layout_item, null);
                holder.tvOrder = (TextView) convertView.findViewById(R.id.tv_orde);
                holder.tvProdName = (TextView) convertView.findViewById(R.id.tv_pord_name);
                holder.tvProdMount = (TextView) convertView.findViewById(R.id.tv_prod_mount);
                holder.tvPriceLine = (TextView) convertView.findViewById(R.id.tv_price_line);
                holder.etPricePur = (EditText) convertView.findViewById(R.id.et_price_pur);
//                holder.etPricePur.setOnTouchListener(new CustomOnTouchListener(position, holder.etPricePur, 1));    //捕获焦点
                holder.etPricePur.addTextChangedListener(new CustomTextWatcher(holder.etPricePur, position, PRICE_PUR, holder));    //添加文字修改监听器
//                holder.etPricePur.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
                holder.tvMountDing = (TextView) convertView.findViewById(R.id.tv_mount_ding);
                holder.etMountPur = (EditText) convertView.findViewById(R.id.et_mount_pur);
//                holder.etMountPur.setOnTouchListener(new CustomOnTouchListener(position, holder.etMountPur, 2));
                holder.etMountPur.addTextChangedListener(new CustomTextWatcher(holder.etMountPur, position, MOUNT_PUR, holder));
                holder.tvMoneyLine = (TextView) convertView.findViewById(R.id.tv_money_line);
                holder.tvMoneyPur = (TextView) convertView.findViewById(R.id.tv_money_pur);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            // TODO 从数据list中取出数据并对应的设置
            int order = position + 1;
            holder.key = position;
            PurchaseData purchaseData = mPurchaseList.get(position);
            holder.tvOrder.setText(String.valueOf(order));
            holder.tvProdName.setText(purchaseData.getItemName());
            holder.tvProdMount.setText(purchaseData.getUnit());
            holder.tvPriceLine.setText(String.valueOf(purchaseData.getSellPrice() / 1000));
//            Log.d(TAG, "i =" + position + " data = " + purchaseData.getBuyPrice());
            if (!(String.valueOf(purchaseData.getBuyPrice()) == null)) {
                holder.etPricePur.setText(String.valueOf(purchaseData.getBuyPrice()/1000));
            } else {
                holder.etPricePur.setText("0");
            }

//            holder.tvMountDing.setText(String.valueOf(purchaseData.getNeedNumbre()));
            holder.tvMountDing.setText(String.valueOf(FormatNumberUtil.formatFloatNumber(purchaseData.getNeedNumbre())));
            float moneyLine = purchaseData.getSellPrice() / 1000 * purchaseData.getNeedNumbre();
            holder.tvMoneyLine.setText(String.valueOf(moneyLine));
            holder.etMountPur.setText(String.valueOf(purchaseData.getMountPur()));

            float mountPur = Float.valueOf(holder.etMountPur.getText().toString());
            float moneyPur = purchaseData.getBuyPrice()/1000 * mountPur;

            holder.tvMoneyPur.setText(FormatNumberUtil.formatFloatNumber(moneyPur));
            purchaseData.setMoneyPur(moneyPur);

            if (!holder.etPricePur.getText().toString().equals("0.0") && !holder.etMountPur.getText().toString().equals("0.0")) {
                convertView.setBackgroundColor(getResources().getColor(R.color.colorSelectAll));
            } else if (!holder.etPricePur.getText().toString().equals("0.0")) {
                convertView.setBackgroundColor(getResources().getColor(R.color.colorSelect1));
            } else if (!holder.etMountPur.getText().toString().equals("0.0")) {
                convertView.setBackgroundColor(getResources().getColor(R.color.colorSelect2));
            } else {
                convertView.setBackgroundColor(getResources().getColor(R.color.colorNormal));
            }
            setSumData();
            return convertView;
        }

        @Override
        public Object[] getSections() {
            return new Object[0];
        }

        /**
         * 根据首字母的ascii值来获取在该ListView中第一次出现该首字母的位置
         *
         * @param sectionIndex
         * @return
         */
        @Override
        public int getPositionForSection(int sectionIndex) {
            for (int i = 0; i < getCount(); i++) {
                String name = mPurchaseList.get(i).getItemName();
                String sortStr = GetFirstAlp.getFirstAlpha(name);
                char firstChar = sortStr.toUpperCase().charAt(0);
                if (firstChar == sectionIndex) {
                    return i;
                }
            }
            return -1;
        }

        /**
         * 根据ListView的position来获取该位置上面的name的首字母char的ascii值
         *
         * @param position
         * @return
         */
        @Override
        public int getSectionForPosition(int position) {
            String name = mPurchaseList.get(position).getItemName();
            return GetFirstAlp.getFirstAlpha(name).charAt(0);
        }
    }

    class CustomTextWatcher implements TextWatcher {

        private EditText mEt;
        private int flag;
        private PurchaseListAdapter.ViewHolder viewHolder;
        private String textBefore;
        private Message message = new Message();

        public CustomTextWatcher(EditText e, int p, int flag, PurchaseListAdapter.ViewHolder holder) {
            this.mEt = e;
            this.flag = flag;
            this.viewHolder = holder;
            mEt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        if (mEt.getText().length() == 0) {
                            mEt.setText("0.0");
                        }
                    } else {
                        if (mEt.getText().toString().equals("0.0")) {
                            mEt.setText("");
                        }
                    }
                }
            });
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            this.textBefore = s.toString();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        /**
         * 改变之后， 把修改后的数据保存到本地数据
         * 修改一次，保存一次
         *
         * @param s
         */
        @Override
        public void afterTextChanged(Editable s) {
            if (TextUtils.isEmpty(s)) {
                return;
            }
            if (s.toString().equals(textBefore)) {
                return;
            } else {
                Log.d(TAG, " txtBefore = " + textBefore);
                Log.d(TAG, " S = " + s);
                Log.d(TAG, " boolean = " + s.toString().equals(textBefore));
                String pricePur = String.valueOf(mPurchaseList.get(viewHolder.key).getBuyPrice());
                String montPur = String.valueOf(mPurchaseList.get(viewHolder.key).getMountPur());
                View view = (View) mEt.getParent().getParent();
                if (flag == PRICE_PUR) {
                    Log.d(TAG, " flag1 = " + flag);
                    Log.d(TAG, " key = " + viewHolder.key);
                    if (!pricePur.equals(s.toString()) && !s.toString().equals(".")) {
                        mPurchaseList.get(viewHolder.key).setBuyPrice(Float.valueOf(s.toString())*1000);
                        ((View) mEt.getParent().getParent().getParent()).setBackgroundColor(getResources().getColor(R.color.colorSelect1));
//                        message.what = REFRESH_SUM;
//                        mHandler.sendMessage(message);
                        saveDataToFile();
                    }
                }
                if (flag == MOUNT_PUR) {
                    Log.d(TAG, " flag2 = " + flag);
                    if (!montPur.equals(s.toString()) && !s.toString().equals(".")) {
                        Log.d(TAG, " holder.key = " + viewHolder.key);
                        mPurchaseList.get(viewHolder.key).setMountPur(Float.valueOf(s.toString()));
                        ((View) mEt.getParent().getParent()).setBackgroundColor(getResources().getColor(R.color.colorSelect2));
//                        message.what = REFRESH_SUM;
//                        mHandler.sendMessage(message);
                        saveDataToFile();
                    }
                }
            }
            setSumData();
            float money = mPurchaseList.get(viewHolder.key).getBuyPrice()/1000 * mPurchaseList.get(viewHolder.key).getMountPur();
            viewHolder.tvMoneyPur.setText(FormatNumberUtil.formatFloatNumber(money));
        }
    }


    public void saveDataToFile() {
        if (mPurchaseList != null) {
//            Log.d(TAG, "存储的 mPurchaseList = " + mPurchaseList.toString());
            String jsonObject = new Gson().toJson(mPurchaseList);
//            String data = mPurchaseList.toString();
            Log.d(TAG, "存储的 mPurchaseList 转化后 = " + jsonObject.toString());
            new SaveFileUtil(MainActivity.this, jsonObject).saveDataToFile();
        }
    }

    public void addTestData() {
        // 设置数据（测试数据）
        String[] arry = getResources().getStringArray(R.array.date);
//        Log.d(TAG, "arry = " + arry.toString());

        for (int i = 0; i < 30; i++) {
            PurchaseData data = new PurchaseData();
            Log.d(TAG, "arry[" + i + "] = " + arry[i].toString());
            data.setItemName(arry[i]);
            mPurchaseList.add(data);
        }

        Log.d(TAG, "排序前的 mPurchaseList = " + mPurchaseList.toString());

//        String name = mPurchaseList.get(0).getItemName();
//        Log.d(TAG, "排序前的 mPurchaseList[0] = " + name);
//        String firstApl = GetFirstAlp.getFirstAlpha(name);
//        Log.d(TAG, "mPurchaseList[0]的首字母 = " + firstApl.toString());
//
//        mCharacterParser = CharacterParser.getInstance();
//        mPinyinComparator = new PinyinComparator();
//        // 根据a-z进行排序源数据
//        Collections.sort(mPurchaseList, mPinyinComparator);
//        Log.d(TAG, "排序后的 mPurchaseList = " + mPurchaseList.toString());
//        mListAdapter = new PurchaseListAdapter(MainActivity.this, mPurchaseList);
//        mListView.setAdapter(mListAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveDataToFile();
    }

    @Override
    protected void onDestroy() {
        new TimeTaskUtil(MainActivity.this).stopTimeTask();
        super.onDestroy();
    }
}
