package com.example.a23870.xfa.activity;


import android.annotation.TargetApi;
import android.app.DialogFragment;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.a23870.xfa.R;

import javax.crypto.Cipher;

public class FingerprintDialogFragment extends DialogFragment {

    private TextView tvErrorInfo, tvCancel;

    private FingerprintManager fingerprintManager;
    private CancellationSignal cancellationSignal;
    private Cipher cipher;

    private boolean isUserCancel;
    private FingerprintListener mListener;

    public void setCipher(Cipher cipher) {
        this.cipher = cipher;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fingerprintManager = getContext().getSystemService(FingerprintManager.class);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Translucent_NoTitle);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fingerprint, container);
        tvErrorInfo = view.findViewById(R.id.FingerPrint_tvErrorInfo);
        tvCancel = view.findViewById(R.id.FingerPrint_tvCancel);
        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                stopListener();
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        startListener(cipher);
    }

    @Override
    public void onPause() {
        super.onPause();
        stopListener();
    }

    /**
     * 开始指纹认证监听
     *
     * @param mCipher
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void startListener(Cipher mCipher) {
        isUserCancel = false;
        cancellationSignal = new CancellationSignal();
        fingerprintManager.authenticate(new FingerprintManager.CryptoObject(mCipher), cancellationSignal, 0, new FingerprintManager.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                if (!isUserCancel) {
                    tvErrorInfo.setText(errString);
                    if (errorCode == FingerprintManager.FINGERPRINT_ERROR_LOCKOUT) {
                        Toast.makeText(getActivity(), errString, Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                }
            }

            @Override
            public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                tvErrorInfo.setText(helpString);
            }

            @Override
            public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                dismiss();
                Toast.makeText(getActivity(), "指纹验证成功", Toast.LENGTH_SHORT).show();

                if (mListener != null) {
                    mListener.onSuccess();
                }
            }

            @Override
            public void onAuthenticationFailed() {
                tvErrorInfo.setText("指纹验证失败，请再试一次");
            }
        }, null);
    }

    /**
     * 停止指纹认证监听
     */
    private void stopListener() {
        if (cancellationSignal != null) {
            cancellationSignal.cancel();
            cancellationSignal = null;
            isUserCancel = true;
        }
    }

    public void setListener(FingerprintListener listener) {
        mListener = listener;
    }

    public interface FingerprintListener {

        void onSuccess();


    }
}