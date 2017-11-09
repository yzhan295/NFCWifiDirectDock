package com.gc.nfc.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.TextView;

import com.gc.nfc.R;
import com.gc.nfc.WifiAdmin;
import com.gc.nfc.base.BaseNfcActivity;

import java.util.Arrays;

/**
 * Created by gc on 2016/12/8.
 */
public class ReadTextActivity extends BaseNfcActivity {
    private TextView mNfcText;
    private String mTagText;

    private TextView mTvVoltage;
    private TextView mTvTemperature;
    private TextView mTvLevel;
    private TextView mTvStatus;
    private TextView mTvHealth;
    private TextView mTvTechnology;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_text);
        mNfcText = (TextView) findViewById(R.id.tv_nfctext);

        mTvVoltage = (TextView)findViewById(R.id.tv_voltage);
        mTvTemperature = (TextView)findViewById(R.id.tv_temperature);
        mTvLevel = (TextView)findViewById(R.id.tv_level);
        mTvStatus = (TextView)findViewById(R.id.tv_status);
        mTvHealth = (TextView)findViewById(R.id.tv_health);
        mTvTechnology = (TextView)findViewById(R.id.tv_technology);

//        this.registerReceiver(this.mBatteryReceiver, new IntentFilter(
//                Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public void onNewIntent(Intent intent) {
        //1.获取Tag对象
        Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        //2.获取Ndef的实例
        Ndef ndef = Ndef.get(detectedTag);
        if (null == ndef) return;
        mTagText = ndef.getType() + "\nmaxsize:" + ndef.getMaxSize() + "bytes\n\n";
        readNfcTag(intent);
        mNfcText.setText(mTagText);
    }

    /**
     * 读取NFC标签文本数据
     */
    private void readNfcTag(Intent intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                    NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage msgs[] = null;
            int contentSize = 0;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                    contentSize += msgs[i].toByteArray().length;
                }
            }
            try {
                if (msgs != null) {
                    NdefRecord record = msgs[0].getRecords()[0];
                    NdefRecord record1 = msgs[0].getRecords()[1];
//                    String textRecord = parseTextRecord(record);
                    String textRecord1 = parseTextRecord(record1);

                    if (null != textRecord1) {
                        mTagText += textRecord1  + "\n\ntext\n" + contentSize + " bytes";
                        String[] tagArr = textRecord1.split(";");
                        if(tagArr.length == 3) {
                            WifiAdmin wifiAdmin = new WifiAdmin(this);
                            wifiAdmin.openWifi();
                            wifiAdmin.addNetwork(wifiAdmin.CreateWifiInfo(tagArr[0], tagArr[1], 3));

                            Thread.sleep(5000);
                            String wifiInfo = wifiAdmin.getWifiInfo();
                            Intent intent1 = new Intent(ReadTextActivity.this, WebDirectActivity.class);
                            intent1.putExtra("WEB_DIRECT_URL", tagArr[2]);
                            startActivity(intent1);

                        }
                    }

                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * 解析NDEF文本数据，从第三个字节开始，后面的文本数据
     *
     * @param ndefRecord
     * @return
     */
    public static String parseTextRecord(NdefRecord ndefRecord) {
        /**
         * 判断数据是否为NDEF格式
         */
        //判断TNF
        if (ndefRecord.getTnf() != NdefRecord.TNF_WELL_KNOWN) {
            return null;
        }
        //判断可变的长度的类型
        if (!Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
            return null;
        }
        try {
            //获得字节数组，然后进行分析
            byte[] payload = ndefRecord.getPayload();
            //下面开始NDEF文本数据第一个字节，状态字节
            //判断文本是基于UTF-8还是UTF-16的，取第一个字节"位与"上16进制的80，16进制的80也就是最高位是1，
            //其他位都是0，所以进行"位与"运算后就会保留最高位
            String textEncoding = ((payload[0] & 0x80) == 0) ? "UTF-8" : "UTF-16";
            //3f最高两位是0，第六位是1，所以进行"位与"运算后获得第六位
            int languageCodeLength = payload[0] & 0x3f;
            //下面开始NDEF文本数据第二个字节，语言编码
            //获得语言编码
            String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            //下面开始NDEF文本数据后面的字节，解析出文本
            String textRecord = new String(payload, languageCodeLength + 1,
                    payload.length - languageCodeLength - 1, textEncoding);
            return textRecord;
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
    }

    public void toWriteTag(View view){
        startActivity(new Intent(this, RunAppActivity.class));
    }

    private BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            int voltage=arg1.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
            mTvVoltage.setText("电压：" + voltage / 1000 + "." + voltage % 1000 + "V");

            int temperature=arg1.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0);
            mTvTemperature.setText("温度：" + temperature / 10 + "." + temperature % 10 + "℃");
            if (temperature >= 300) {
                mTvTemperature.setTextColor(Color.RED);
            } else {
                mTvTemperature.setTextColor(Color.BLUE);
            }

            int level=arg1.getIntExtra(BatteryManager.EXTRA_LEVEL,0);
            int scale=arg1.getIntExtra(BatteryManager.EXTRA_SCALE,0);
            int levelPercent = (int)(((float)level / scale) * 100);
            mTvLevel.setText("电量：" + levelPercent + "%");
            if (level <= 10) {
                mTvLevel.setTextColor(Color.RED);
            } else {
                mTvLevel.setTextColor(Color.BLUE);
            }

            int status = arg1.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
            String strStatus = "未知状态";
            switch (status) {
                case BatteryManager.BATTERY_STATUS_CHARGING:
                    strStatus = "充电中……";
                    break;
                case BatteryManager.BATTERY_STATUS_DISCHARGING:
                    strStatus = "放电中……";
                    break;
                case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                    strStatus = "未充电";
                    break;
                case BatteryManager.BATTERY_STATUS_FULL:
                    strStatus = "充电完成";
                    break;
            }
            mTvStatus.setText("状态：" + strStatus);

            int health = arg1.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN);
            String strHealth = "未知 :(";;
            switch (status) {
                case BatteryManager.BATTERY_HEALTH_GOOD:
                    strHealth = "好 :)";
                    break;
                case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                    strHealth = "过热！";
                    break;
                case BatteryManager.BATTERY_HEALTH_DEAD: // 未充电时就会显示此状态，这是什么鬼？
                    strHealth = "良好";
                    break;
                case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                    strHealth = "电压过高！";
                    break;
                case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                    strHealth = "未知 :(";
                    break;
                case BatteryManager.BATTERY_HEALTH_COLD:
                    strHealth = "过冷！";
                    break;
            }
            mTvHealth.setText("健康状况：" + strHealth);

            String technology = arg1.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
            mTvTechnology.setText("电池技术：" + technology);
        }
    };
}
