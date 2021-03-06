package me.jiudeng.purchase.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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
import me.jiudeng.purchase.listener.OnResponseListener;
import me.jiudeng.purchase.module.PurchaseData;
import me.jiudeng.purchase.network.HttpUtil;
import me.jiudeng.purchase.utils.CharacterParser;
import me.jiudeng.purchase.utils.FileUtil;
import me.jiudeng.purchase.utils.GetFirstAlp;
import me.jiudeng.purchase.utils.PinyinComparator;
import me.jiudeng.purchase.utils.SaveFileUtil;
import me.jiudeng.purchase.utils.TimeTaskUtil;
import me.jiudeng.purchase.view.SideBar;

/**
 * Created by Yin on 2016/3/29.
 * RecyclerView 版本
 */
public class MainActivity2 extends BaseActivity implements View.OnClickListener {

    private static String TAG = "MainActivity";
    private static final int REFRESH_DATA = 0;
    private static final int SEND_SUCCESS = 1;
    private static final int SEND_FAIL = 2;
    private static final int PRICE_PUR = 0;
    private static final int MOUNT_PUR = 1;

    /**
     * Views
     */
    private Button mBtnLogOut, mBtnSend;
    private TextView mTvPaySum;     //采购金额合计
    private TextView mTvCurDialog;  //显示当前字母
    private SideBar mSideBar;
    private RecyclerView mRecyclerView;

    /**
     * Values
     */
    private List<PurchaseData> mPurchaseList = new ArrayList<>();
    private CharacterParser mCharacterParser; //汉字转换成拼音的类
    private PinyinComparator mPinyinComparator; //根据拼音来排列ListView里面的数据类
    private RecyclerViewAdapter mRecyclerAdapter;
    private LinearLayoutManager mLayoutManager;

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
        mTvCurDialog = (TextView) findViewById(R.id.tv_dialog);
        mSideBar = (SideBar) findViewById(R.id.side_bar);
        mSideBar.setTextView(mTvCurDialog);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycle_view);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
//        //如果可以确定每个item的高度是固定的，设置这个选项可以提高性能
        mRecyclerView.setHasFixedSize(true);
        mRecyclerAdapter = new RecyclerViewAdapter(MainActivity2.this);
        mRecyclerView.setAdapter(mRecyclerAdapter);

//        //设置右侧触摸监听
//        mSideBar.setOnTouchingLetterChangedListener(new SideBar.OnTouchingLetterChangedListener() {
//
//            @Override
//            public void onTouchingLetterChanged(String s) {
//                //该字母首次出现的位置
//                int position = mListAdapter.getPositionForSection(s.charAt(0));
////                int position = mRecyclerAdapter.
//                if (position != -1) {
//                    mListView.setSelection(position);
//
//                }
//            }
//        });
    }

    private void initData() {
        // TODO: 2016/4/5 先判断本地数据是否存在，存在则加载本地数据，否则请求网络数据
        String fileContent = FileUtil.readFile(MainActivity2.this);
        final Message message = new Message();
        Log.d(TAG, "初始化数据...");
        if (!TextUtils.isEmpty(fileContent)) {
            try {
                paraseJsonData(fileContent.toString());
                Log.d(TAG, "成功读取本地文件数据，加载...");
                message.what = REFRESH_DATA;
                mHandler.sendMessage(message);
//                mRecyclerAdapter.notifyDataSetChanged();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
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
                    Log.d(TAG, "排序前的 mPurchaseList = " + mPurchaseList.toString());
                    mCharacterParser = CharacterParser.getInstance();
                    mPinyinComparator = new PinyinComparator();
                    // 根据a-z进行排序源数据
                    Collections.sort(mPurchaseList, mPinyinComparator);
                    Log.d(TAG, "排序后的 mPurchaseList = " + mPurchaseList.toString());

//                    mRecyclerAdapter = new RecyclerViewAdapter(MainActivity2.this);
//                    mRecyclerView.setAdapter(mRecyclerAdapter);
                    mRecyclerAdapter.notifyDataSetChanged();
//                    saveDataToFile();//请求成功后将数据保存到本地
//                    new TimeTaskUtil(MainActivity.this).startTimeTask();   //开启定时上传功能
                    break;

                case SEND_SUCCESS:

                    Toast.makeText(MainActivity2.this, "上传完毕！", Toast.LENGTH_SHORT).show();
                    break;

                case SEND_FAIL:

                    Toast.makeText(MainActivity2.this, "上传失败，请重新上传！", Toast.LENGTH_SHORT).show();
                    break;

                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_logout:
                // TODO: 2016/3/30  退出当前账号，弹出弹框；需要删除本地保存的用户信息，并退出到登录界面
//                showDialogOld();
                showDialogIos();
                break;

            case R.id.btn_send:
                // TODO: 2016/3/30 上传数据
                postCurDataToServer();
                break;
            default:
                break;
        }
    }

    private void postCurDataToServer() {
        if (mPurchaseList != null) {
            String data = new Gson().toJson(mPurchaseList);
            final Message message = new Message();
            HttpUtil.postDataToServer(HttpUtil.addressPushData, data, new OnResponseListener() {
                @Override
                public void onResponse(String response) throws JSONException {
                    message.what = SEND_SUCCESS;
                    message.obj = response;
                    mHandler.sendMessage(message);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity2.this);
        final Dialog dialog = builder.create();
        LinearLayout layout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.layout_ios_dialog, null);
        dialog.show();
        dialog.getWindow().setContentView(layout);

        layout.findViewById(R.id.btn_positive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 2016/3/30 删除账号信息
                logOut();
                startActivity(new Intent(MainActivity2.this, LoginActivity.class));
                MainActivity2.this.finish();
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
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity2.this);
        builder.setMessage("确定退出？");
        builder.setCancelable(false);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO: 2016/3/30 删除账号信息

                startActivity(new Intent(MainActivity2.this, LoginActivity.class));
                MainActivity2.this.finish();
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

    int mCurTouchIndex = -1;
    int mCurTouchIndex2 = -1;

    class PurchaseListAdapter extends BaseAdapter implements SectionIndexer {

        private Context mContext;
        private LayoutInflater inflater;
        private List<PurchaseData> mDataList;
        private int order = 0;

        private class ViewHolder {
            private TextView tvOrder;                  //序号
            private TextView tvProdName, tvProdMount;   //商品名称 & 规格
            private TextView tvPriceLine;       //线上价格
            private EditText etPricePur;        //采购价格
            private TextView tvMountDing;       //订购量
            private EditText etMountPur;        //采购量
            private TextView tvMoneyLine, tvMoneyPur;   //线上金额 & 采购金额
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
                holder.etPricePur.setInputType(EditorInfo.TYPE_CLASS_PHONE);    //数字键盘
//                holder.etPricePur.setOnTouchListener(new CustomOnTouchListener(position, 1));
//                holder.etPricePur.addTextChangedListener(new CustomTextWatcher(holder.etPricePur, position, PRICE_PUR));
                holder.tvMountDing = (TextView) convertView.findViewById(R.id.tv_mount_ding);
                holder.etMountPur = (EditText) convertView.findViewById(R.id.et_mount_pur);
                holder.etMountPur.setInputType(EditorInfo.TYPE_CLASS_PHONE);
//                holder.etMountPur.setOnTouchListener(new CustomOnTouchListener(position, 2));
//                holder.etMountPur.addTextChangedListener(new CustomTextWatcher(holder.etMountPur, position, MOUNT_PUR));
                holder.tvMoneyLine = (TextView) convertView.findViewById(R.id.tv_money_line);
                holder.tvMoneyPur = (TextView) convertView.findViewById(R.id.tv_money_pur);
                convertView.setTag(holder);
            }
            holder.etPricePur.clearFocus();
            holder.etMountPur.clearFocus();
            if (mCurTouchIndex == position) {
                // 如果当前的行下标和点击事件中保存的index一致，手动为EditText设置焦点。
                holder.etPricePur.requestFocus();
                holder.etPricePur.setSelection(holder.etPricePur.getText().length());
                mCurTouchIndex = -1;
            }
            if (mCurTouchIndex2 == position) {
                holder.etMountPur.requestFocus();
                holder.etMountPur.setSelection(holder.etMountPur.getText().length());
                mCurTouchIndex2 = -1;
            }
//            holder.etPricePur.setSelection( holder.etPricePur .getText().length());

            // TODO 从数据list中取出数据并对应的设置
            int order = position + 1;
            PurchaseData purchaseData = mPurchaseList.get(position);
            holder.tvOrder.setText(String.valueOf(order));
            holder.tvProdName.setText(purchaseData.getItemName());
            holder.tvProdMount.setText(purchaseData.getUnit());
            holder.tvPriceLine.setText(String.valueOf(purchaseData.getSellPrice() / 1000));
            if (!(String.valueOf(purchaseData.getBuyPrice()) == null)) {
                holder.etPricePur.setText(String.valueOf(purchaseData.getBuyPrice()));
            } else {
                holder.etPricePur.setText("0");
            }
            holder.tvMountDing.setText(String.valueOf(purchaseData.getNeedNumbre()));

            float moneyLine = purchaseData.getSellPrice() / 1000 * purchaseData.getNeedNumbre();
            holder.tvMoneyLine.setText(String.valueOf(moneyLine));

            holder.etMountPur.setText(String.valueOf(purchaseData.getMountPur()));

            float mountPur = Float.valueOf(holder.etMountPur.getText().toString());
            float moneyPur = purchaseData.getBuyPrice() * mountPur;

            holder.tvMoneyPur.setText(String.valueOf(moneyPur));
            purchaseData.setMoneyPur(moneyPur);

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
        private RecyclerViewAdapter.CustomViewHolder holder;
        private String textBefore;

        public CustomTextWatcher(EditText e, int flag, RecyclerViewAdapter.CustomViewHolder viewHolder) {
            this.mEt = e;
            this.flag = flag;
            this.holder = viewHolder;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            textBefore = s.toString();
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
            if (s.toString() == textBefore) {
                return;
            }
            else {
                if (flag == PRICE_PUR) {
                    mPurchaseList.get(holder.key).setBuyPrice(Float.valueOf(s.toString()));
//                    View view = (View) mEt.getParent().getParent().getParent();
//                    if (view != null) {
//                        view.setBackgroundColor(getResources().getColor(R.color.colorSelec));
//                    }
                }
                if (flag == MOUNT_PUR) {
                    Log.d(TAG, " holder.key = " + holder.key);
                    Log.d(TAG, " txtBefore = " + textBefore);
                    Log.d(TAG, " S = " + s + "\n");
                    mPurchaseList.get(holder.key).setMountPur(Float.valueOf(s.toString()));
                }
//                saveDataToFile();
            }
            float sumMoney = 0;
            for (PurchaseData data : mPurchaseList) {
                sumMoney += data.getMoneyPur();
            }
            mTvPaySum.setText(String.valueOf(sumMoney));
        }
    }

    public void saveDataToFile() {
        if (mPurchaseList != null) {
            Log.d(TAG, "存储的 mPurchaseList = " + mPurchaseList.toString());
            String jsonObject = new Gson().toJson(mPurchaseList);
//            String data = mPurchaseList.toString();
            Log.d(TAG, "存储的 mPurchaseList 转化后 = " + jsonObject.toString());
            new SaveFileUtil(MainActivity2.this, jsonObject).saveDataToFile();
        }
    }

    class CustomOnTouchListener implements View.OnTouchListener {

        private int position;
        //        private EditText text;
        private int flag;

        public CustomOnTouchListener(int position, int flag) {
            this.position = position;
//            this.text = text;
            this.flag = flag;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (flag == 1) {
                    mCurTouchIndex = position;
                }
                if (flag == 2) {
                    mCurTouchIndex2 = position;
                }
//                text.requestFocus();
            }
            return false;
        }
    }


    /**
     * RecyclerView 适配器
     */
    class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.CustomViewHolder> {

        private Context mContext;
        private List<PurchaseData> mDataList;

        public RecyclerViewAdapter(Context mContext, List<PurchaseData> list) {
            this.mContext = mContext;
            this.mDataList = list;
        }

        public RecyclerViewAdapter(Context mContext) {
            this.mContext = mContext;
        }

        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.layout_item, parent, false);
            CustomViewHolder holder = new CustomViewHolder(view);
            return holder;
        }

        @Override
        public void onBindViewHolder(CustomViewHolder viewHolder, int position) {
            PurchaseData data = mPurchaseList.get(position);
            int order = position + 1;
            viewHolder.key = position;
            viewHolder.tvOrder.setText(String.valueOf(order));
            viewHolder.tvProdName.setText(data.getItemName());
            viewHolder.tvProdMount.setText(data.getUnit());
            viewHolder.tvPriceLine.setText(String.valueOf(data.getSellPrice() / 1000));
            viewHolder.etPricePur.setText(String.valueOf(data.getBuyPrice()));
//            viewHolder.etPricePur.addTextChangedListener(new CustomTextWatcher(viewHolder.etPricePur, PRICE_PUR, viewHolder));
            viewHolder.tvMountDing.setText(String.valueOf(data.getNeedNumbre()));

            float moneyLine = data.getSellPrice() / 1000 * data.getNeedNumbre();
            viewHolder.tvMoneyLine.setText(String.valueOf(moneyLine));

            viewHolder.etMountPur.setText(String.valueOf(data.getMountPur()));
//            viewHolder.etMountPur.addTextChangedListener(new CustomTextWatcher(viewHolder.etMountPur, MOUNT_PUR, viewHolder));

            float mountPur = Float.valueOf(viewHolder.etMountPur.getText().toString());
            float moneyPur = data.getBuyPrice() * mountPur;

            viewHolder.tvMoneyPur.setText(String.valueOf(moneyPur));
            data.setMoneyPur(moneyPur);
        }

        @Override
        public int getItemCount() {
            return mPurchaseList.size();
        }

        //CustomViewHolder，用于缓存，提高效率
        public class CustomViewHolder extends RecyclerView.ViewHolder {
            private TextView tvOrder;                  //序号
            private TextView tvProdName, tvProdMount;   //商品名称 & 规格
            private TextView tvPriceLine;       //线上价格
            private EditText etPricePur;        //采购价格
            private TextView tvMountDing;       //订购量
            private EditText etMountPur;        //采购量
            private TextView tvMoneyLine, tvMoneyPur;   //线上金额 & 采购金额

            private int key;    //给holder添加标签

            public CustomViewHolder(View itemView) {
                super(itemView);
                tvOrder = (TextView) itemView.findViewById(R.id.tv_orde);
                tvProdName = (TextView) itemView.findViewById(R.id.tv_pord_name);
                tvProdMount = (TextView) itemView.findViewById(R.id.tv_prod_mount);
                tvPriceLine = (TextView) itemView.findViewById(R.id.tv_price_line);
                etPricePur = (EditText) itemView.findViewById(R.id.et_price_pur);
                etPricePur.setInputType(EditorInfo.TYPE_CLASS_PHONE);    //数字键盘
//            etPricePur.setOnTouchListener(new CustomOnTouchListener(position, 1));
//                etPricePur.addTextChangedListener(new CustomTextWatcher(etPricePur, PRICE_PUR,CustomViewHolder.this));
                tvMountDing = (TextView) itemView.findViewById(R.id.tv_mount_ding);
                etMountPur = (EditText) itemView.findViewById(R.id.et_mount_pur);
                etMountPur.setInputType(EditorInfo.TYPE_CLASS_PHONE);
//            etMountPur.setOnTouchListener(new CustomOnTouchListener(position, 2));
//                etMountPur.addTextChangedListener(new CustomTextWatcher(etMountPur, MOUNT_PUR, CustomViewHolder.this));
                tvMoneyLine = (TextView) itemView.findViewById(R.id.tv_money_line);
                tvMoneyPur = (TextView) itemView.findViewById(R.id.tv_money_pur);
            }
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
        new TimeTaskUtil(MainActivity2.this).stopTimeTask();
        super.onDestroy();
    }
}
