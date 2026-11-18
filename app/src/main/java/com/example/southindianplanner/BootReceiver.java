package com.example.southindianplanner;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context,Intent intent){
  String a=intent.getAction();
  if(Intent.ACTION_BOOT_COMPLETED.equals(a)||Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)){
   ReminderReceiver.scheduleNext(context,"morning");
   ReminderReceiver.scheduleNext(context,"evening");
   ReminderReceiver.scheduleNext(context,"month_end");
   ReminderReceiver.scheduleNext(context,"month_start");
  }
 }
}