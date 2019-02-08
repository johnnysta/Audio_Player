package hu.example.jani.ap_all;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

/**
 * Created by Janó on 2017.01.31..
 *
 * Broadcast receiver to pause playing if an incoming call arrives.
 */

public class IncomingCallBR extends BroadcastReceiver {

    static Listener listener;
    @Override
    public void onReceive(Context context, Intent intent) {
        TelephonyManager tm = (TelephonyManager)context.getSystemService(Service.TELEPHONY_SERVICE);
        if (listener != null) {
            if ((tm.getCallState()) == TelephonyManager.CALL_STATE_RINGING)
                listener.onIncomingCall();
            else if ((tm.getCallState()) == TelephonyManager.CALL_STATE_IDLE)
                listener.onCallEnded();
        }
    }


    public interface Listener {
        public void onIncomingCall();
        public void onCallEnded();
    }

    //The object received in the argument will contain
    // the implementation of Listener (i.e. the onIncomingCall() function):
    public static void setListener(Listener aListener) {
        listener = aListener;
    }
}
