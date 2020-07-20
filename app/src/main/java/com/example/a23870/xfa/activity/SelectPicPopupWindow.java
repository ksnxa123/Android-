package com.example.a23870.xfa.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.a23870.xfa.R;

public class SelectPicPopupWindow extends Activity implements View.OnClickListener {
    private Button btn_face, btn_speech, btn_finger, btn_cancel;
    private LinearLayout pop_layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tan);
        btn_face = (Button) this.findViewById(R.id.btn_face);
        btn_speech = (Button) this.findViewById(R.id.btn_speech);
        btn_finger = (Button) this.findViewById(R.id.btn_finger);
        btn_cancel = (Button) this.findViewById(R.id.btn_cancel);

        pop_layout = (LinearLayout) findViewById(R.id.pop_layout);

        //添加选择窗口范围监听可以优先获取触点，即不再执行onTouchEvent()函数，点击其他地方时执行onTouchEvent()函数销毁Activity
        pop_layout.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // TODO Auto-generated method stub
                Toast.makeText(getApplicationContext(), "提示：点击窗口外部关闭窗口！",
                        Toast.LENGTH_SHORT).show();
            }
        });
        //添加按钮监听
        btn_face.setOnClickListener(this);
        btn_speech.setOnClickListener(this);
        btn_finger.setOnClickListener(this);
        btn_cancel.setOnClickListener(this);
    }

    //实现onTouchEvent触屏函数但点击屏幕时销毁本Activity
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        finish();
        return true;
    }

    public void onClick(View v) {

        Intent intent = new Intent();

        switch (v.getId()) {
            case R.id.btn_face:
                intent.putExtra("type", 1);
                setResult(RESULT_OK, intent);
                break;
            case R.id.btn_speech:
                intent.putExtra("type", 2);
                setResult(RESULT_OK, intent);
                break;
            case R.id.btn_finger:
                intent.putExtra("type", 3);
                setResult(RESULT_OK, intent);
                break;
            case R.id.btn_cancel:
                setResult(RESULT_CANCELED);
                break;
            default:
                break;
        }
        finish();
    }


}
