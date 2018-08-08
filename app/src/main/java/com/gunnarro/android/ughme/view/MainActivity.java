package com.gunnarro.android.ughme.view;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneNumberUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gunnarro.android.ughme.R;
import com.gunnarro.android.ughme.location.MyLocationManager;
import com.gunnarro.android.ughme.location.Position;
import com.gunnarro.android.ughme.mail.MailSender;
import com.gunnarro.android.ughme.service.UghmeIntentService;
import com.gunnarro.android.ughme.sms.SMS;
import com.gunnarro.android.ughme.sms.SMSHandler;
import com.gunnarro.android.ughme.sms.SMSReader;

import java.util.List;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    private static final int PERMISSION_REQUEST_LOCATION = 0;
    private static final int PERMISSION_REQUEST_SMS = 1;
    private static final int PERMISSION_REQUEST_CONTACTS = 2;

    // https://www.google.com/maps/dir/?api=1&origin=Oslo,Norway&destination=Bergen,Norway

    // https://maps.googleapis.com/maps/api/staticmap?center=40.714728,-73.998672&zoom=12&size=400x400&key=YOUR_API_KEY

    // https://www.google.com/maps/dir/?api=1&parameters

    private String smsActionCmd = "trace";
    private CoordinatorLayout snackBarLayout;
    private EditText mobileNumberTxt;
    private Spinner contactsDropDown;
    private TextView logView;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Load the main.xml layout file.
            setContentView(R.layout.main_layout);
            snackBarLayout = findViewById(R.id.coordinatorLayout);

            Spinner contactsDropDown = findViewById(R.id.contacts_spinner);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.getApplicationContext(), R.array.contacts_array, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            contactsDropDown.setAdapter(adapter);
            contactsDropDown.setOnItemSelectedListener(new ContatcsDropdownOnItemSelectedListener());
            contactsDropDown.requestFocus();

            Button myLocationBtn = findViewById(R.id.show_my_location);
            Button testGotchaBtn = findViewById(R.id.test_gotcha);
            Button testSendMailBtn = findViewById(R.id.test_send_mail);
            Button copyToClipboardBtn = findViewById(R.id.copy_to_clipboard);
            mobileNumberTxt = findViewById(R.id.mobile_number);
            mobileNumberTxt.clearFocus();
            logView = findViewById(R.id.log_view);
            logView.setMovementMethod(new ScrollingMovementMethod());

            myLocationBtn.setOnClickListener(this);
            testGotchaBtn.setOnClickListener(this);
            testSendMailBtn.setOnClickListener(this);
            copyToClipboardBtn.setOnClickListener(this);

        //    Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        //    setSupportActionBar(myToolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        findViewById(R.id.mobile_number).clearFocus();
        findViewById(R.id.contacts_spinner).requestFocus();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.test_gotcha:
                if (!PhoneNumberUtils.isGlobalPhoneNumber(mobileNumberTxt.getText().toString()) ) {
                    Snackbar.make(snackBarLayout, "Invalid mobile number: " + mobileNumberTxt.getText().toString() , Snackbar.LENGTH_LONG).show();
                } else {
                    try {
                        Snackbar.make(snackBarLayout, "Trace: " + mobileNumberTxt.getText().toString(), Snackbar.LENGTH_LONG).show();
                        checkAndRequestPermission(this, Manifest.permission.SEND_SMS, "send sms access required", "send sms access denied", "send sms access unavailable", PERMISSION_REQUEST_SMS);
                        checkAndRequestPermission(this, Manifest.permission.READ_CONTACTS, "contacts access required", "contacts access denied", "contacts access unavailable", PERMISSION_REQUEST_CONTACTS);
                        Intent intent = new Intent(this.getApplicationContext(), UghmeIntentService.class);
                        intent.putExtra(SMSHandler.KEY_MOBILE_NUMBER, mobileNumberTxt.getText().toString());
                        intent.putExtra(SMSHandler.KEY_SMS_MSG, smsActionCmd);
                        Snackbar.make(snackBarLayout, "Start trace number: " + smsActionCmd, Snackbar.LENGTH_LONG).show();
                        startService(intent);
                    } catch (Exception e) {
                        Snackbar.make(snackBarLayout, "ERROR" + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                }
                break;
            case R.id.show_my_location:
                displayLocation();
                break;
            case R.id.test_send_mail:
                sendMail();
                break;

            case R.id.copy_to_clipboard:
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("smsinbox", logView.getText().toString());
                if (clipboard != null) clipboard.setPrimaryClip(clip);
                Snackbar.make(snackBarLayout, "Copied sms to clipboard", Snackbar.LENGTH_LONG).show();
                break;
        }
    }

    private void displayLocation() {
        try {
            checkAndRequestPermission(this, Manifest.permission.ACCESS_FINE_LOCATION, "send sms access required", "send sms access denied", "send sms access unavailable", PERMISSION_REQUEST_LOCATION);
            String url= "na";
            Position trace = MyLocationManager.getLocationLastKnown(this, "45465500");
            if (trace != null) {
                url = trace.createGoogleMapUrl();
            }
            Snackbar.make(snackBarLayout, trace.toString(), Snackbar.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this.getApplicationContext(), "ERROR: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void sendMail() {
        MailSender sender = new MailSender("gunnar.ronneberg@gmail.com", "ABcd2o1o");
        try {
            SMSReader smsReader = new SMSReader(this.getApplicationContext());
            List<SMS> inbox = smsReader.getSMSInbox(false, mobileNumberTxt.getText().toString());
            sender.sendMail("Forwared SMS", inbox.toString(), "gunnar.ronneberg@gmail.com", "gunnar.ronneberg@gmail.com");
            Snackbar.make(snackBarLayout, "Sent mail to gunnar.ronneberg, SMSInbox size: " + inbox.size(), Snackbar.LENGTH_LONG).show();
            logView.setText(inbox.toString());
        } catch (Exception e) {
            Snackbar.make(snackBarLayout, "ERROR: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    class ContatcsDropdownOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            mobileNumberTxt.setText(parent.getItemAtPosition(pos).toString());
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_LOCATION || requestCode == PERMISSION_REQUEST_SMS) {
            // Request for permission.
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted. Start Sms Trace Activity.
                Snackbar.make(snackBarLayout, R.string.location_permission_granted, Snackbar.LENGTH_LONG).show();
            } else {
                // Permission request was denied.
                Snackbar.make(snackBarLayout, R.string.location_permission_denied, Snackbar.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Requests the {@link android.Manifest.permission#*} permission.
     * If an additional rationale should be displayed, the user has to launch the request from
     * a SnackBar that includes additional information.
     */
    private void checkAndRequestPermission(final Activity activity, final String permission, String accessRequired, String accessDenied, String unavailable, final int permissionId) {
        if (ActivityCompat.checkSelfPermission(activity.getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            // Permission has not been granted and must be requested.
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                // Provide an additional rationale to the user if the permission was not granted
                // and the user would benefit from additional context for the use of the permission.
                // Display a SnackBar with cda button to request the missing permission.
                Snackbar.make(snackBarLayout, accessRequired, Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ActivityCompat.requestPermissions(activity, new String[]{permission}, permissionId);
                    }
                }).show();
            } else {
                Snackbar.make(snackBarLayout, unavailable, Snackbar.LENGTH_LONG).show();
                // Request the permission. The result will be received in onRequestPermissionResult().
                ActivityCompat.requestPermissions(activity, new String[]{permission}, permissionId);
            }
        }
    }
}
