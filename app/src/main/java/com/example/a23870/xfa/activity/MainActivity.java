package com.example.a23870.xfa.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.arcsoft.face.ActiveFileInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.example.a23870.xfa.R;
import com.example.a23870.xfa.commom.Constants;
import com.example.a23870.xfa.util.ConfigUtil;

import java.io.File;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private final static String DEFAULT_KEY_NAME = "MyFingerPrint";
    private static final String TAG = "Init Activity";

    private Toast toast = null;
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.READ_PHONE_STATE
    };
    private FaceEngine faceEngine = new FaceEngine();

    private Button weChat;
    private Button aliPay;
    private int request;
    //声明密钥库
    private KeyStore keyStore;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        weChat = (Button) findViewById(R.id.WeChat);
        weChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                request = 1024;
                Intent intent = new Intent(MainActivity.this, SelectPicPopupWindow.class);
                startActivityForResult(intent, request);

            }
        });


        aliPay = (Button) findViewById(R.id.AliPay);
        aliPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                request = 2048;
                Intent intent = new Intent(MainActivity.this, SelectPicPopupWindow.class);
                startActivityForResult(intent, request);
            }
        });


        //设置视频模式下的人脸优先检测方向
        ConfigUtil.setFtOrient(this, FaceEngine.ASF_OP_0_HIGHER_EXT);

        activeEngine();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if (requestCode == 1002) {

                /**
                 * 人脸识别成功返回，打开之前选择的界面
                 */
                goMainActivity();

            } else {

                int type = data.getIntExtra("type", 0);
                if (type == 3) {
                    /**
                     * 选择了指纹识别
                     */
                    if (isSupportFingerPrint()) {
                        keyInit();
                        cipherInit();
                    }
                } else if (type == 1) {
                    /**
                     * 选择了人脸识别
                     */
                    startActivityForResult(new Intent(this, RegisterAndRecognizeActivity.class), 1002);
                } else if (type == 2) {
                    /**
                     * 选择了声纹识别
                     */
                    startActivityForResult(new Intent(this, VocalVerifyDemo.class), 1002);
                }
            }
        }

    }

    private void openWechat() {

        try {
            Intent LaunchIntent = getPackageManager()
                    .getLaunchIntentForPackage("com.tencent.mm");
            startActivity(LaunchIntent);
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "未安装微信", Toast.LENGTH_LONG).show();
        }

    }

    private void openAlipay() {

        try {
            Intent LaunchIntent = getPackageManager()
                    .getLaunchIntentForPackage("com.eg.android.AlipayGphone");
            startActivity(LaunchIntent);
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "未安装支付宝", Toast.LENGTH_LONG).show();
        }

    }


    /**
     * 检查设备是否支持指纹解锁功能
     *
     * @return
     */
    private boolean isSupportFingerPrint() {
        if (Build.VERSION.SDK_INT < 23) {
            Toast.makeText(mContext, "您的系统版本过低，不支持指纹功能", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            //键盘管理器
            KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
            FingerprintManager fingerprintManager = getSystemService(FingerprintManager.class);
            if (!fingerprintManager.isHardwareDetected()) {
                Toast.makeText(mContext, "您的手机不支持指纹功能", Toast.LENGTH_SHORT).show();
                return false;
            } else if (!keyguardManager.isKeyguardSecure()) {
                Toast.makeText(mContext, "您还未设置锁屏，请先设置锁屏并添加一个指纹", Toast.LENGTH_SHORT).show();
                return false;
            } else if (!fingerprintManager.hasEnrolledFingerprints()) {
                Toast.makeText(mContext, "您至少需要在系统设置中添加一个指纹", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    /**
     * 初始化Keystore
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void keyInit() {
        try {
            //初始化密码库
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            //加载密码库
            keyStore.load(null);
            //获取密钥生成器
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            //创建一个参数集
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(DEFAULT_KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);
            //用指定的参数集初始化此密钥
            keyGenerator.init(builder.build());
            //生成一个密钥
            keyGenerator.generateKey();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 初始化密码
     */
    private void cipherInit() {
        try {
            SecretKey key = (SecretKey) keyStore.getKey(DEFAULT_KEY_NAME, null);
            Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            showFingerPrintDialog(cipher);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 显示指纹验证对话框
     *
     * @param cipher
     */
    private void showFingerPrintDialog(Cipher cipher) {
        FingerprintDialogFragment fragment = new FingerprintDialogFragment();
        fragment.setCipher(cipher);
        fragment.show(getFragmentManager(), "fingerprint");
        fragment.setListener(new FingerprintDialogFragment.FingerprintListener() {
            @Override
            public void onSuccess() {
                goMainActivity();
            }
        });
    }

    private boolean isRegistered() {
        String ROOT_PATH = this.getFilesDir().getAbsolutePath();
        String SAVE_FEATURE_DIR = "register" + File.separator + "features";
        File featureDir = new File(ROOT_PATH + File.separator + SAVE_FEATURE_DIR);
        if (!featureDir.exists() || !featureDir.isDirectory()) {
            return false;
        }
        File[] featureFiles = featureDir.listFiles();
        if (featureFiles == null || featureFiles.length == 0) {
            return false;
        }
        return true;
    }


    private boolean checkPermissions(String[] neededPermissions) {
        if (neededPermissions == null || neededPermissions.length == 0) {
            return true;
        }
        boolean allGranted = true;
        for (String neededPermission : neededPermissions) {
            allGranted &= ContextCompat.checkSelfPermission(this, neededPermission) == PackageManager.PERMISSION_GRANTED;
        }
        return allGranted;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            boolean isAllGranted = true;
            for (int grantResult : grantResults) {
                isAllGranted &= (grantResult == PackageManager.PERMISSION_GRANTED);
            }
            if (isAllGranted) {
                activeEngine();
            } else {
                showToast(getString(R.string.permission_denied));
            }
        }
    }

    private void showToast(String s) {
        if (toast == null) {
            toast = Toast.makeText(this, s, Toast.LENGTH_SHORT);
            toast.show();
        } else {
            toast.setText(s);
            toast.show();
        }
    }

    /**
     * 进入主界面
     */
    public void goMainActivity() {


        if (request == 1024) {
            openWechat();
        } else if (request == 2048) {
            openAlipay();
        }

    }


    /**
     * 激活引擎
     */
    public void activeEngine() {
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
            return;
        }
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                int activeCode = faceEngine.activeOnline(MainActivity.this, Constants.APP_ID, Constants.SDK_KEY);
                emitter.onNext(activeCode);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Integer activeCode) {
                        if (activeCode == ErrorInfo.MOK) {
                            showToast(getString(R.string.active_success));
                        } else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
//                            showToast(getString(R.string.already_activated));
                        } else {
                            showToast(getString(R.string.active_failed, activeCode));
                        }

                        ActiveFileInfo activeFileInfo = new ActiveFileInfo();
                        int res = faceEngine.getActiveFileInfo(MainActivity.this, activeFileInfo);
                        if (res == ErrorInfo.MOK) {
                            Log.i(TAG, activeFileInfo.toString());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

}

