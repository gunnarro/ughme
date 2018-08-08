package com.gunnarro.android.ughme.service;

import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.gunnarro.android.ughme.AppPreferences;
import com.gunnarro.android.ughme.ListAppPreferencesImpl;
import com.gunnarro.android.ughme.location.MyLocationManager;
import com.gunnarro.android.ughme.mail.MailSender;
import com.gunnarro.android.ughme.sms.SMS;
import com.gunnarro.android.ughme.sms.SMSHandler;
import com.gunnarro.android.ughme.sms.SMSMsg;
import com.gunnarro.android.ughme.sms.SMSReader;
import com.gunnarro.android.ughme.sms.SMSSender;
import com.gunnarro.android.ughme.location.Position;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * To create a application to run in the background of other current activities,
 * one needs to create a Service. The Service can run indefinitely (unbounded)
 * or can run at the lifespan of the calling activity(bounded).
 *
 * @author gunnarro
 */
public class UghmeIntentService extends IntentService {

    private static final String TAG = UghmeIntentService.class.getName();
    private MediaPlayer player;
    private Handler handler;

    /**
     * Default constructor
     */
    public UghmeIntentService() {
        super(TAG);
    }

    private void log(final String txt, boolean isToast) {
        Log.i("", txt);
        if (isToast) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(UghmeIntentService.this, txt, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        log(TAG + " onHandleIntent", false);
        if (intent.getExtras() != null) {
            if (intent.getExtras().getString(SMSHandler.KEY_MOBILE_NUMBER) == null) {
                log(TAG + " missing mobile number in extras", false);
                return;
            }
            handleReceivedSMS(new SMSMsg(intent.getExtras().getString(SMSHandler.KEY_MOBILE_NUMBER), intent.getExtras().getString(SMSHandler.KEY_SMS_MSG)),
                    this);
        } else {
            log(TAG + " extras is null !", false);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        log(TAG + "Created", false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log(TAG + " Stopped", false);
        handler = null;
        if (player != null) {
            player.stop();
            player = null;
        }
    }

    private void alertUser(String msg) {
        log(msg, true);
        // player = MediaPlayer.create(this, R.raw.starwars);
        // player.setLooping(false); // Set looping
        // player.start();
    }

    private void handleReceivedSMS(SMSMsg receivedSMSMsg, Context context) {
        if (receivedSMSMsg.isTraceSMS()) {
            handleTraceSMS(receivedSMSMsg, context);
        } else if (receivedSMSMsg.isForwardSMS()) {
            handleForwardSMS(receivedSMSMsg, context);
        } else {
            log("Ordinary sms: " + receivedSMSMsg.toString(), false);
        }
    }

    /**
     *
     * @param receivedSMSMsg
     * @param context
     */
    private void handleTraceSMS(SMSMsg receivedSMSMsg, Context context) {
        String from;
        //if (new ListAppPreferencesImpl(context, AppPreferences.AUTHENTICATED_USERS).listContains(receivedSMSMsg.getToMobilePhoneNumber())) {
        log(receivedSMSMsg.getToMobilePhoneNumber() + " is authenticated", false);
        // give the mobile owner a signal that he has been traced.
        // get the mobile location if available
        Position traceLog = null;
        try {
            traceLog = MyLocationManager.getLocationLastKnown(context, receivedSMSMsg.getToMobilePhoneNumber());
        } catch (Exception e) {
            e.printStackTrace();
        }
        from = lookupContactName(receivedSMSMsg.getToMobilePhoneNumber());
        if (from == null) {
            from = receivedSMSMsg.getToMobilePhoneNumber();
        }
        alertUser("Sent trace sms to: " + from + ", url: " + traceLog);
        SimpleDateFormat sd = new SimpleDateFormat("dd-MM-yyyy hh:ss:mm");
        StringBuilder msg = new StringBuilder();
        msg.append("time: ").append(sd.format(traceLog.getTime())).append("\n");
        msg.append("mobile:").append(traceLog.getMobileNumber()).append("\n");
        msg.append(traceLog.createGoogleMapUrl());
        new SMSSender().sendSMS(receivedSMSMsg.getToMobilePhoneNumber(), msg.toString());
    }

    private void handleForwardSMS(SMSMsg receivedSMSMsg, Context context) {
        boolean onlyUnread = true;
        if (new ListAppPreferencesImpl(context, AppPreferences.AUTHENTICATED_USERS).listContains(receivedSMSMsg.getToMobilePhoneNumber())) {
            MailSender mailSender = new MailSender("username@gmail.com", "userpass");
            try {
                SMSReader smsReader = new SMSReader(context);
                List<SMS> unreadSms = smsReader.getSMSInbox(onlyUnread, "");
                String missesCalls = "";//CallRegister.getMissedCalls(context);
                StringBuilder msg = new StringBuilder();
                msg.append("-------------------------------------------\n");
                msg.append("Call and sms log for ").append(getDevicePhoneNumber()).append("\n");
                msg.append("-------------------------------------------\n");
                msg.append("Misses calls:\n");
                msg.append("-------------------------------------------\n");
                msg.append(missesCalls).append("\n\n");
                msg.append("Unread sms:\n");
                msg.append("-------------------------------------------\n");
                for (SMS sms : unreadSms) {
                    msg.append(sms.toString()).append("\n");
                }
                mailSender.sendMail("Call and SMS log", msg.toString(), "gunnar.ronneberg@gmail.com", "gunnar.ronneberg@gmail.com");
                alertUser("Forwarded call log and sms inbox to email address gunnar.ronneberg");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            log("Ingnore this request, the requester was not authenticated to use the sms forward service, mobile number: "
                    + receivedSMSMsg.getToMobilePhoneNumber(), true);
        }
    }

    private String lookupContactName(String mobilePhonenumber) {
        String contactDisplayName = null;
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(mobilePhonenumber));
        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};
        // Query the filter URI
        Cursor cursor = this.getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                contactDisplayName = cursor.getString(0);
            }
            cursor.close();
        }
        return contactDisplayName;
    }

    private boolean isAuthenticated(String mobilePhoneNumber) {
        return mobilePhoneNumber.equals(getDevicePhoneNumber());
    }

    private String getDevicePhoneNumber() {
        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return "na";
        }
        return tm.getLine1Number();
    }
}
