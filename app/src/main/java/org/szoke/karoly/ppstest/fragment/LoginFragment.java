package org.szoke.karoly.ppstest.fragment;

import android.app.Activity;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.szoke.karoly.ppstest.MainActivity;
import org.szoke.karoly.ppstest.R;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by Adam on 2016. 10. 01..
 */

public class LoginFragment extends Fragment {
    OnLoginButtonClickListener onLoginButtonClickListener;

    private EditText edEmail;
    private EditText edPassword;
    private EditText edDevice;

    private Button btnLogin;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_login, container, false);

        edEmail = (EditText) rootView.findViewById(R.id.edEmail);
        edPassword = (EditText) rootView.findViewById(R.id.edPassword);
        edDevice = (EditText) rootView.findViewById(R.id.edDevice);
        edDevice.setText(Build.MODEL);

        btnLogin = (Button) rootView.findViewById(R.id.btLogin);
        setButtonClickListener();

        return rootView;
    }

    private void setButtonClickListener(){
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edEmail.getText() == null) {
                    edEmail.setError(getString(R.string.ed_email_error));
                    return;
                }
                if (edPassword.getText() == null) {
                    edPassword.setError(getString(R.string.ed_password_error));
                    return;
                }
                if (edDevice.getText() == null) {
                    edDevice.setError(getString(R.string.ed_device_error));
                    return;
                }
                String params = null;
                try {
                    params = "email=" + URLEncoder.encode(edEmail.getText().toString(), "UTF-8")
                            + "&password=" + URLEncoder.encode(edPassword.getText().toString(), "UTF-8") +
                            "&name=" + URLEncoder.encode(edDevice.getText().toString(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                if (onLoginButtonClickListener != null){
                    onLoginButtonClickListener.onClick(params);
                }
            }
        });
    }

    public interface OnLoginButtonClickListener{
        public void onClick(String params);
    }

    public void setOnLoginButtonClickListener(OnLoginButtonClickListener onLoginButtonClickListener){
        this.onLoginButtonClickListener = onLoginButtonClickListener;
    }
}
