package com.pengxh.autodingding.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.aihook.alertview.library.AlertView;
import com.aihook.alertview.library.OnItemClickListener;
import com.jzxiang.pickerview.TimePickerDialog;
import com.jzxiang.pickerview.data.Type;
import com.jzxiang.pickerview.listener.OnDateSetListener;
import com.pengxh.app.multilib.base.DoubleClickExitActivity;
import com.pengxh.app.multilib.utils.BroadcastManager;
import com.pengxh.app.multilib.utils.ColorUtil;
import com.pengxh.app.multilib.utils.SaveKeyValues;
import com.pengxh.app.multilib.widget.EasyToast;
import com.pengxh.app.multilib.widget.dialog.InputDialog;
import com.pengxh.autodingding.R;
import com.pengxh.autodingding.service.AutoDingdingService;
import com.pengxh.autodingding.utils.BroadcastAction;
import com.pengxh.autodingding.utils.Utils;

import butterknife.BindView;
import butterknife.OnClick;

public class MainActivity extends DoubleClickExitActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private AlertView alertView;

    @BindView(R.id.startTimeBtn)
    Button startTimeBtn;
    @BindView(R.id.endTimeBtn)
    Button endTimeBtn;
    @BindView(R.id.amTime)
    TextView amTime;
    @BindView(R.id.pmTime)
    TextView pmTime;
    private BroadcastManager broadcastManager;

    @Override
    public void initView() {
        setContentView(R.layout.activity_main);
    }

    @Override
    public void initData() {
        broadcastManager = BroadcastManager.getInstance(this);
        String amKaoQin = (String) SaveKeyValues.getValue("amKaoQin", "");
        String pmKaoQin = (String) SaveKeyValues.getValue("pmKaoQin", "");
        if (!amKaoQin.equals("")) {
            amTime.setText("将在" + amKaoQin + "自动打卡");
        } else {
            amTime.setText("上班打卡时间未设置");
        }
        if (!pmKaoQin.equals("")) {
            pmTime.setText("将在" + pmKaoQin + "自动打卡");
        } else {
            pmTime.setText("下班打卡时间未设置");
        }
    }

    @Override
    public void initEvent() {
        if (Utils.isAppAvailable(this, BroadcastAction.DINGDING)) {
            startService(new Intent(this, AutoDingdingService.class));
        } else {
            alertView = new AlertView("温馨提示", "手机没有安装钉钉软件，无法自动打卡",
                    null, new String[]{"确定"}, null, this, AlertView.Style.Alert,
                    new OnItemClickListener() {
                        @Override
                        public void onItemClick(Object o, int position) {
                            alertView.dismiss();
                            MainActivity.this.finish();
                        }
                    }).setCancelable(false);
            alertView.show();
        }
        broadcastManager.addAction(BroadcastAction.ACTION_UPDATE_AM, new BroadcastReceiver() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onReceive(Context context, Intent intent) {
                //更新UI
                String action = intent.getAction();
                if (action != null && action.equals(BroadcastAction.ACTION_UPDATE_AM)) {
                    String data = intent.getStringExtra("data");
//                    mTextViewStart.setText("倒计时：" + data + "秒");
                }
            }
        });
        broadcastManager.addAction(BroadcastAction.ACTION_UPDATE_PM, new BroadcastReceiver() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onReceive(Context context, Intent intent) {
                //更新UI
                String action = intent.getAction();
                if (action != null && action.equals(BroadcastAction.ACTION_UPDATE_PM)) {
                    String data = intent.getStringExtra("data");
//                    mTextViewEnd.setText("倒计时：" + data + "秒");
                }
            }
        });
    }

    @OnClick({R.id.startTimeBtn, R.id.endTimeBtn})
    @Override
    public void onClick(View v) {
        TimePickerDialog.Builder builder = new TimePickerDialog.Builder();
        switch (v.getId()) {
            case R.id.startTimeBtn:
                //设置上班时间
                builder.setThemeColor(ColorUtil.getRandomColor())
                        .setType(Type.MONTH_DAY_HOUR_MIN)
                        .setCallBack(new OnDateSetListener() {
                            @Override
                            public void onDateSet(TimePickerDialog timePickerView, long millSeconds) {
                                String amKaoQin = Utils.timestampToDate(millSeconds);
                                Log.d(TAG, "onDateSet: " + amKaoQin);
                                SaveKeyValues.putValue("amKaoQin", amKaoQin);
                                amTime.setText("将在" + amKaoQin + "自动打卡");
                                //计算时间差
                                long deltaTime = Utils.deltaTime(millSeconds / 1000);
                                if (deltaTime == 0) {
                                    return;
                                }
                                broadcastManager.sendBroadcast(BroadcastAction.ACTION_KAOQIN_AM, String.valueOf(deltaTime));
                            }
                        });
                break;
            case R.id.endTimeBtn:
                //设置下班时间
                builder.setThemeColor(ColorUtil.getRandomColor())
                        .setType(Type.MONTH_DAY_HOUR_MIN)
                        .setCallBack(new OnDateSetListener() {
                            @Override
                            public void onDateSet(TimePickerDialog timePickerView, long millSeconds) {
                                String pmKaoQin = Utils.timestampToDate(millSeconds);
                                Log.d(TAG, "onDateSet: " + pmKaoQin);
                                SaveKeyValues.putValue("pmKaoQin", pmKaoQin);
                                pmTime.setText("将在" + pmKaoQin + "自动打卡");
                                //计算时间差
                                long deltaTime = Utils.deltaTime(millSeconds / 1000);
                                if (deltaTime == 0) {
                                    return;
                                }
                                broadcastManager.sendBroadcast(BroadcastAction.ACTION_KAOQIN_PM, String.valueOf(deltaTime));
                            }
                        });
                break;
        }
        builder.build().show(getSupportFragmentManager(), "month_day_hour_minute");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_setting) {
            new InputDialog.Builder().setContext(this)
                    .setTitle("设置邮箱")
                    .setNegativeButton("取消")
                    .setPositiveButton("确定")
                    .setCancelable(false)
                    .setOnDialogClickListener(new InputDialogClickListener())
                    .build().show();
        }
        return super.onOptionsItemSelected(item);
    }

    class InputDialogClickListener implements InputDialog.onDialogClickListener {

        @Override
        public void onConfirmClick(Dialog dialog, String input) {
            if (!input.isEmpty()) {
                EasyToast.showToast(input, EasyToast.SUCCESS);
                dialog.dismiss();
            } else {
                EasyToast.showToast("什么都还没输入呢", EasyToast.ERROR);
            }
        }

        @Override
        public void onCancelClick(Dialog dialog) {
            dialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        broadcastManager.destroy(BroadcastAction.ACTION_UPDATE_AM, BroadcastAction.ACTION_UPDATE_PM);
    }
}