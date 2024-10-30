package com.example.myapplication;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    public static Button buttonStart;
    public static Button buttonPause;
    public static Button buttonFinish;
    public static Button buttonSpeed;
    public static Button buttonIncline;
    public static boolean isRunning = false;

    public static byte high_speed = 0;
    public static byte low_speed = 0;
    public static byte speed_flag = 3;
    public static byte high_incline = 20;
    public static byte low_incline = 20;

    public static int stratTime = 3000;    //启动时间
    public static int totaltime = 0;    //正计时间
    public static int totalmeter = 0;   //正计距离

    public static byte speed= 0;        //当前速度
    public static byte incline = 0;     //当前坡度

    public static byte tartget_speed= 10;        //目标速度
    public static byte tartget_incline = 10;     //目标坡度

    public static CountDownTimer countDownTimer;
    private static TextView timerTextView; // 显示计时的 TextView
    public static boolean isPaused = false;  // 标识倒计时是否暂停
    private static boolean isTiming = false;  // 标识是否正在计时

    public static TextView textViewSpeed;       //当前速度
    public static TextView textViewIncline;     //当前坡度
    public static TextView textViewTargetSpeed;       //目标速度
    public static TextView textViewTargetIncline;     //目标坡度

    public static Dialog dialog;
    private Serialport serialPort;
    private TreadmillManager treadmillManager;
    private int fd = -1;

    private HandlerThread handlerThread;
    private Handler backgroundHandler;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        dialog = new Dialog(this);
        serialPort = new Serialport();
        treadmillManager = new TreadmillManager("/dev/ttyS2",19200);
        // 创建 HandlerThread
        handlerThread = new HandlerThread("DataHandlerThread");
        handlerThread.start();

        // 创建 Handler 用于处理背景线程的任务
        backgroundHandler = new Handler();
        // 创建主线程 Handler
        mainHandler = new Handler(Looper.getMainLooper());

        //获取速度信息
        treadmillManager.fetchDeviceInfo("getSpeed");
        byte[] frame = new byte[15];
        treadmillManager.receiveprot(frame);

        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                backgroundHandler.postDelayed(this,100);
                treadmillManager.controlDevice("startDevice");
//                System.out.println("===>gooooooooooo");
//                treadmillManager.controlDevice("setSpeed");
//                treadmillManager.controlDevice("setIncline");
                //处理串口数据的收发
                byte[] frame = new byte[15];
                treadmillManager.receiveprot(frame);
                // 将数据发送到主线程进行UI更新
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        //UI界面更新
                        updateUI();
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        treadmillManager.controlDevice("stopDevice");
        treadmillManager.closeport();
    }

    private void showAdjustDialog(String title, int initialValue) {

        dialog.setContentView(R.layout.dialog_adjust_value);

        TextView dialogTitle = dialog.findViewById(R.id.dialogTitle);
        TextView currentValue = dialog.findViewById(R.id.currentValue);
        Button buttonDecrease = dialog.findViewById(R.id.buttonDecrease);
        Button buttonIncrease = dialog.findViewById(R.id.buttonIncrease);
        Button presetValue1 = dialog.findViewById(R.id.presetValue1);
        Button presetValue2 = dialog.findViewById(R.id.presetValue2);
        Button presetValue3 = dialog.findViewById(R.id.presetValue3);

        dialogTitle.setText("调整" + title);
        currentValue.setText(String.valueOf(initialValue));

        // 增减按钮的点击事件
        buttonIncrease.setOnClickListener(v -> {
            byte value = Byte.parseByte(currentValue.getText().toString());
            if(title.equals("速度")){
                tartget_speed = value;
                if(tartget_speed < high_speed)
                    currentValue.setText(String.valueOf(++tartget_speed));
            }
            if(title.equals("坡度")){
                tartget_incline = value;
                if(tartget_incline < high_incline)
                    currentValue.setText(String.valueOf(++tartget_incline));
            }
            if(value < 20)
                currentValue.setText(String.valueOf(++value));
        });

        buttonDecrease.setOnClickListener(v -> {
            byte value = Byte.parseByte(currentValue.getText().toString());
            if(title.equals("速度")){
                tartget_speed = value;
                if(tartget_speed > low_speed){
                    tartget_speed -= 1;
                    currentValue.setText(String.valueOf(tartget_speed));
                }

            }
            if(title.equals("坡度")){
                tartget_incline = value;
                if(tartget_incline > low_incline){
                    tartget_incline -= 1;
                    currentValue.setText(String.valueOf(tartget_incline));
                }

            }

        });

        // 预设值按钮点击事件
        View.OnClickListener presetClickListener = v -> {


            byte value = Byte.parseByte(((Button) v).getText().toString());
            currentValue.setText(String.valueOf(value));
        };
        presetValue1.setOnClickListener(presetClickListener);
        presetValue2.setOnClickListener(presetClickListener);
        presetValue3.setOnClickListener(presetClickListener);

        // 调用回调，将新值传递给 MainActivity
        dialog.findViewById(R.id.buttonConfirm).setOnClickListener(v -> {
            byte newValue = Byte.parseByte(currentValue.getText().toString());

            // 调用回调，将新值传递给 MainActivity
            onValueChanged(title,newValue);

            dialog.dismiss();
        });

        dialog.show();
    }

    public void updateUI(){
        textViewSpeed = findViewById(R.id.textViewSpeed);
        textViewIncline = findViewById(R.id.textViewIncline);
        textViewTargetSpeed = findViewById(R.id.textViewTargetSpeed);
        textViewTargetIncline = findViewById(R.id.textViewTargertIncline);

        buttonStart = findViewById(R.id.buttonStart);
        buttonPause = findViewById(R.id.buttonPause);
        buttonFinish = findViewById(R.id.buttonStop);

        buttonSpeed = findViewById(R.id.buttonSpeed);
        buttonIncline = findViewById(R.id.buttonIncline);

        timerTextView = findViewById(R.id.timer_text_view); // 假设布局中增加了此 TextView

        // 设置 Start/Pause 按钮的点击事件
        buttonStart.setOnClickListener(v ->{
            handleStartPause();
        });
        //发送启动设备指令，收到启动秒数，将其修改为启动时间，到达启动时间后，发送目标坡度和目标速度。
        buttonPause.setOnClickListener(v -> pauseTimer());

        buttonFinish.setOnClickListener(v -> {
            speed = 0;
            updateSpeedDisplay();
            incline = 0;
            updateInclineDisplay();
            finishOperation();
        });


//        // 读取保存的速度和坡度值
//        SharedPreferences sharedPreferences = getSharedPreferences("TreadmillPrefs", MODE_PRIVATE);
//        speed = sharedPreferences.getAll("speed", 0);    // 读取速度，默认值为 0
//        incline = sharedPreferences.getAll("incline", 0); // 读取坡度，默认值为 0

        updateTargetSpeedDisplay();
        updateTargetInclineDisplay();
        // 设置速度按钮点击事件---此时为targetSpeed
        MainActivity.buttonSpeed.setOnClickListener(v -> {
            showAdjustDialog("速度", MainActivity.tartget_speed);

        });
        // 设置坡度按钮点击事件---此时为targetIncline
        MainActivity.buttonIncline.setOnClickListener(v -> showAdjustDialog("坡度", MainActivity.tartget_incline));
    }
    // 显示倒计时弹窗
    void showCountdownDialog(int time) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ready!");

        // 创建一个 LinearLayout 并设置居中对齐
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER); // 将内容居中
        layout.setPadding(50, 20, 50, 20);

        final TextView countdownText = new TextView(this);
        countdownText.setPadding(50, 20, 50, 20);
        countdownText.setTextSize(36);
        countdownText.setGravity(Gravity.CENTER); // 确保文字在 TextView 中居中
        // 将 TextView 添加到 LinearLayout
        layout.addView(countdownText);

        // 将 LinearLayout 设置为 AlertDialog 的视图
        builder.setView(layout);

        AlertDialog dialog = builder.create();
        dialog.show();

        // 开始倒计时
        startCountdown(countdownText, dialog, time);  // 1000 倒计时 10 秒
    }

    // 开始倒计时
    static void startCountdown(TextView countdownText, AlertDialog dialog, long duration) {
        MainActivity.countDownTimer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                countdownText.setText("Remaining: " + millisUntilFinished / 1000 + "s");
            }

            @Override
            public void onFinish() {
                dialog.dismiss();
                resetButtons();
            }
        }.start();

        buttonStart.setText("Start");
        MainActivity.isPaused = false;
    }

    // 重置按钮状态
    private static void resetButtons() {
        buttonStart.setText("Start");
        MainActivity.isPaused = false;
    }

    public void onValueChanged(String title, byte newValue){
        Context context = null;
        if (title.equals("速度")) {
            MainActivity.tartget_speed = newValue;
            updateTargetSpeedDisplay();
        } else if (title.equals("坡度")) {
            MainActivity.tartget_incline = newValue;
            updateTargetInclineDisplay();
        }

        // 如果需要保存新值到本地，可以使用 SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("TreadmillPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("speed", MainActivity.tartget_speed);
        editor.putInt("incline", MainActivity.tartget_incline);
        editor.apply();
    }

    // 更新当前速度显示
    public  static void updateSpeedDisplay() {
        MainActivity.textViewSpeed.setText("当前速度: " + MainActivity.speed + "Km/h");
    }

    // 更新当前坡度显示
    private static void updateInclineDisplay() {
        MainActivity.textViewIncline.setText("当前坡度: " + MainActivity.incline);
    }
    // 更新目标速度显示
    public  static void updateTargetSpeedDisplay() {
        MainActivity.textViewTargetSpeed.setText("目标速度: " + MainActivity.tartget_speed + "Km/h");
    }

    // 更新目标坡度显示
    private static void updateTargetInclineDisplay() {
        MainActivity.textViewTargetIncline.setText("目标坡度: " + MainActivity.tartget_incline);
    }
    // 更新距离显示
    public  static void updateMeterDisplay() {
        MainActivity.textViewSpeed.setText("Distance:" + MainActivity.totalmeter + "m");
    }

    // 更新时间显示
    private static void updateTimeDisplay() {
        MainActivity.textViewIncline.setText("Elapsed Time: " + MainActivity.totaltime + "s");
    }
    // 开始操作
    private void handleStartPause() {
        if (isTiming) {
            // 如果在计时状态，则暂停或继续计时
//            toggleTimer();    //这里需要不断读取时间、距离等信息
        } else if (isPaused) {
            buttonStart.setText("START");
        } else {
            showCountdownDialog(stratTime);//这里更新不同的底层启动时间
        }
        // TODO: 结束操作的逻辑处理，例如重置计时或其他状态
    }
    // 暂停
    static void pauseTimer() {
        if (isTiming) {
//            toggleTimer();
        } else if (countDownTimer != null) {
            countDownTimer.cancel();
            buttonStart.setText("Continue");
            isPaused = true;
        }
    }
    // 完成操作，重置状态
    public static void finishOperation() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (isTiming) {
//            stopTimer();
        }
        resetButtons();
        // TODO: 结束操作的逻辑处理，例如重置计时或其他状态
    }
}
