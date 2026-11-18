package com.example.southindianplanner;
import android.content.*;import android.app.*;import android.os.*;import android.media.RingtoneManager;import java.util.*;
public class ReminderReceiver extends BroadcastReceiver {
 public void onReceive(Context c,Intent i){
  String slot=i.getStringExtra("slot");if(slot==null)slot="evening";
  if("month_end".equals(slot)||"month_start".equals(slot)){handleMonthlyReport(c,slot);return;}
  boolean morning=slot.equals("morning");
  SharedPreferences p=c.getSharedPreferences("planner_v3",0);if(!p.getBoolean(slot+"_enabled",true))return;
  Calendar now=Calendar.getInstance(),start=Calendar.getInstance();start.setTimeInMillis(p.getLong("cycle_start",System.currentTimeMillis()));
  midnight(start);midnight(now);int day=(int)(Math.max(0,(now.getTimeInMillis()-start.getTimeInMillis())/86400000L)%30);int target=morning?day:(day+1)%30;
  String[][] menu={
   {"Idli + sambar","Rice + keerai kootu + carrot poriyal","Dosa + egg omelette"},{"Vegetable poha","Lemon rice + curd + potato fry","Same lemon rice + curd"},{"Masala dosa + chutney","Rice + sambar + beans poriyal","Egg dosa"},{"Tomato rice","Rice + rasam + cabbage poriyal","Same rice + rasam"},{"Ragi dosa + chutney","Rice + mor kuzhambu + beetroot poriyal","Wheat dosa + dal"},{"Poori + potato masala","Rice + sambar + avial","Same sambar rice"},{"Adai + avial","Rice + rasam + beans poriyal","Chapati + egg gravy"},{"Vegetable uthappam","Rice + keerai dal + carrot poriyal","Dosa + coconut chutney"},{"Lemon rice + curd","Rice + sambar + cabbage poriyal","Same rice + sambar"},{"Idli + sambar","Bisibele bath + curd","Egg omelette + dosa"},{"Vegetable poha","Rice + rasam + potato fry","Same rice + rasam"},{"Pesarattu + chutney","Rice + mor kuzhambu + beans poriyal","Tomato dosa + chutney"},{"Poori + potato masala","Rice + sambar + avial","Same sambar rice"},{"Adai + avial","Rice + paruppu + carrot poriyal","Chapati + paneer gravy"},{"Kal dosa + chutney","Rice + spinach dal + carrot poriyal","Egg dosa"},{"Coconut rice + curd","Rice + sambar + beans poriyal","Same rice + sambar"},{"Ragi idli + sambar","Tomato rice + curd","Egg bhurji + dosa"},{"Vegetable poha","Rice + mor kuzhambu + beetroot poriyal","Same rice + mor kuzhambu"},{"Onion dosa + chutney","Rice + keerai kootu + potato fry","Vegetable uthappam"},{"Poori + potato masala","Rice + sambar + avial","Same sambar rice"},{"Adai + avial","Rice + rasam + beans poriyal","Chapati + egg curry"},{"Idli + sambar","Rice + cabbage kootu + carrot poriyal","Dosa + egg omelette"},{"Tamarind rice + curd","Rice + rasam + potato poriyal","Same rice + rasam"},{"Vegetable uthappam","Rice + keerai dal + beans poriyal","Egg dosa"},{"Lemon rice + curd","Rice + mor kuzhambu + cabbage poriyal","Same rice + mor kuzhambu"},{"Pesarattu + chutney","Vegetable pulao + curd","Dosa + tomato chutney"},{"Poori + potato masala","Rice + sambar + avial","Same sambar rice"},{"Adai + avial","Rice + paruppu + poriyal","Chapati + egg gravy"},{"Vegetable poha","Rice + sambar + spinach poriyal","Same sambar rice"},{"Tomato dosa","Vegetable pulao + curd","Egg omelette + dosa"}};
  String[] labels={"Breakfast","Lunch","Dinner"};StringBuilder body=new StringBuilder();
  for(int k=0;k<3;k++){body.append(labels[k]).append(": ").append(p.getString("meal_"+target+"_"+k,menu[target][k]));if(k<2)body.append("\n");}
  body.append(morning?"\n500 ml milk is included in today's checklist.":"\nTomorrow's missing ingredients:");
  if(!morning){ArrayList<String> missing=new ArrayList<>();for(Map.Entry<String,?> en:p.getAll().entrySet())if(en.getKey().startsWith("stock_")&&!("available".equals(String.valueOf(en.getValue()))))missing.add(en.getKey().substring(6).replace('_',' '));if(missing.isEmpty())body.append(" None marked low or finished.");else body.append(" ").append(android.text.TextUtils.join(", ",missing));}
  if(Build.VERSION.SDK_INT>=26){NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(nm.getNotificationChannel("ooru_meals_reminders")==null){NotificationChannel ch=new NotificationChannel("ooru_meals_reminders","NSV reminders",NotificationManager.IMPORTANCE_HIGH);ch.enableVibration(true);ch.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build());nm.createNotificationChannel(ch);}}
  Intent open=new Intent(c,MainActivity.class);open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);PendingIntent content=PendingIntent.getActivity(c,morning?100:101,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder n=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,"ooru_meals_reminders"):new Notification.Builder(c);
  n.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle((morning?"Good morning • Today's":"Good evening • Tomorrow's")+" meals • Day "+(target+1)).setContentText(menu[target][0]+" • "+menu[target][1]).setStyle(new Notification.BigTextStyle().bigText(body.toString())).setContentIntent(content).setAutoCancel(true).setCategory(Notification.CATEGORY_REMINDER).setPriority(Notification.PRIORITY_HIGH).setDefaults(Notification.DEFAULT_SOUND|Notification.DEFAULT_VIBRATE);
  ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(morning?100:101,n.build());scheduleNext(c,slot);
 }
 static String money(double n){return "₹"+String.format(Locale.US,"%.2f",n);}
 static String monthKey(Calendar c){return new java.text.SimpleDateFormat("yyyy-MM",Locale.US).format(c.getTime());}
 static String category(String item){
  String x=item.toLowerCase(Locale.US);
  if(x.equals("milk"))return "Milk";
  if(x.contains("rice")||x.contains("poha")||x.contains("wheat")||x.contains("dal")||x.contains("flour"))return "Rice & grains";
  if(x.contains("onion")||x.contains("tomato")||x.contains("carrot")||x.contains("beans")||x.contains("vegetable")||x.contains("keerai")||x.contains("spinach")||x.contains("chilli")||x.contains("lemon")||x.contains("potato")||x.contains("cabbage")||x.contains("beetroot")||x.contains("curry leaves")||x.contains("coriander")||x.contains("mint"))return "Vegetables & fruits";
  return "Groceries & staples";
 }
 static String buildMonthReport(SharedPreferences p,Calendar period,boolean opening){
  String key=monthKey(period);TreeMap<String,Double> items=new TreeMap<>(),cats=new TreeMap<>();double total=0;
  for(Map.Entry<String,?> en:p.getAll().entrySet())if(en.getKey().startsWith("purchases_")){
   String name=en.getKey().substring(10).replace('_',' ');
   try{JSONArray a=new JSONArray(String.valueOf(en.getValue()));for(int i=0;i<a.length();i++){
    JSONObject o=a.getJSONObject(i);if(o.optString("date").startsWith(key)){double v=o.optDouble("total",0);total+=v;items.put(name,items.getOrDefault(name,0.0)+v);String cat=category(name);cats.put(cat,cats.getOrDefault(cat,0.0)+v);}
   }}catch(Exception ignored){}
  }
  String budgetText=p.contains("budget")?money(Double.parseDouble(p.getString("budget","0"))):"Not set";
  StringBuilder b=new StringBuilder();
  b.append(opening?"PREVIOUS MONTH CLOSING REPORT":"MONTH-END GROCERY REPORT").append("\n").append(key).append("\n\n");
  b.append("Total spent: ").append(money(total)).append("\nMonthly budget: ").append(budgetText).append("\n");
  if(p.contains("budget"))b.append("Remaining: ").append(money(Double.parseDouble(p.getString("budget","0"))-total)).append("\n");
  b.append("\nCATEGORY TOTALS\n");
  if(cats.isEmpty())b.append("No purchases recorded\n");else for(Map.Entry<String,Double> e:cats.entrySet())b.append(e.getKey()).append(": ").append(money(e.getValue())).append("\n");
  b.append("\nINDIVIDUAL INGREDIENT EXPENSES\n");
  if(items.isEmpty())b.append("No purchases recorded this month.");else for(Map.Entry<String,Double> e:items.entrySet())b.append(e.getKey()).append(": ").append(money(e.getValue())).append("\n");
  if(opening){
   Calendar previous=(Calendar)period.clone();previous.add(Calendar.MONTH,-1);
   b.append("\nPrevious month before that: ").append(monthKey(previous)).append(" — ").append(money(monthTotal(p,previous))).append("\n");
   b.append("New month budget: ").append(budgetText).append("\nNew month starts with ₹0 recorded spending; new purchases will be tracked from today.");
  }
  return b.toString();
 }
 static double monthTotal(SharedPreferences p,Calendar period){
  String key=monthKey(period);double total=0;
  for(Map.Entry<String,?> en:p.getAll().entrySet())if(en.getKey().startsWith("purchases_"))try{JSONArray a=new JSONArray(String.valueOf(en.getValue()));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);if(o.optString("date").startsWith(key))total+=o.optDouble("total",0);}}catch(Exception ignored){}
  return total;
 }
 static void handleMonthlyReport(Context c,String slot){
  SharedPreferences p=c.getSharedPreferences("planner_v3",0);if(!p.getBoolean("monthly_reports_enabled",true))return;
  boolean opening="month_start".equals(slot);Calendar period=Calendar.getInstance();period.set(Calendar.DAY_OF_MONTH,1);midnight(period);if(opening)period.add(Calendar.MONTH,-1);
  String report=buildMonthReport(p,period,opening);
  NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
  if(Build.VERSION.SDK_INT>=26&&nm.getNotificationChannel("ooru_meals_reminders")==null){
   NotificationChannel ch=new NotificationChannel("ooru_meals_reminders","NSV reminders",NotificationManager.IMPORTANCE_HIGH);ch.enableVibration(true);
   ch.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build());nm.createNotificationChannel(ch);
  }
  Intent open=new Intent(c,MainActivity.class);open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
  int id=opening?103:102;PendingIntent pi=PendingIntent.getActivity(c,id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder n=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,"ooru_meals_reminders"):new Notification.Builder(c);
  n.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(opening?"NSV • New month budget summary":"NSV • Month-end budget report")
   .setContentText("Total grocery spending: "+money(monthTotal(p,period)))
   .setStyle(new Notification.BigTextStyle().bigText(report)).setContentIntent(pi).setAutoCancel(true)
   .setCategory(Notification.CATEGORY_STATUS).setPriority(Notification.PRIORITY_HIGH).setDefaults(Notification.DEFAULT_SOUND|Notification.DEFAULT_VIBRATE);
  nm.notify(id,n.build());scheduleNext(c,slot);
 }
 static void midnight(Calendar c){c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);}
 static void scheduleNext(Context c,String slot){
  SharedPreferences p=c.getSharedPreferences("planner_v3",0);
  boolean monthly="month_end".equals(slot)||"month_start".equals(slot);
  if(monthly&&!p.getBoolean("monthly_reports_enabled",true))return;
  if(!monthly&&!p.getBoolean(slot+"_enabled",true))return;
  int id="morning".equals(slot)?100:("evening".equals(slot)?101:("month_end".equals(slot)?102:103));
  Intent i=new Intent(c,ReminderReceiver.class);i.putExtra("slot",slot);PendingIntent pi=PendingIntent.getBroadcast(c,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Calendar t=Calendar.getInstance();
  if(monthly){
   boolean start="month_start".equals(slot);t.set(Calendar.DAY_OF_MONTH,1);
   t.set(Calendar.HOUR_OF_DAY,p.getInt(start?"month_start_hour":"month_end_hour",start?8:20));
   t.set(Calendar.MINUTE,p.getInt(start?"month_start_min":"month_end_min",start?30:0));
   if(!start)t.set(Calendar.DAY_OF_MONTH,t.getActualMaximum(Calendar.DAY_OF_MONTH));
   t.set(Calendar.SECOND,0);t.set(Calendar.MILLISECOND,0);
   if(t.getTimeInMillis()<=System.currentTimeMillis()){t.add(Calendar.MONTH,1);t.set(Calendar.DAY_OF_MONTH,1);if(!start)t.set(Calendar.DAY_OF_MONTH,t.getActualMaximum(Calendar.DAY_OF_MONTH));}
  }else{
   boolean morning="morning".equals(slot);t.set(Calendar.HOUR_OF_DAY,p.getInt(slot+"_hour",morning?8:19));t.set(Calendar.MINUTE,p.getInt(slot+"_min",0));t.set(Calendar.SECOND,0);t.set(Calendar.MILLISECOND,0);if(t.getTimeInMillis()<=System.currentTimeMillis())t.add(Calendar.DATE,1);
  }
  AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(Build.VERSION.SDK_INT>=31&&am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,t.getTimeInMillis(),pi);else if(Build.VERSION.SDK_INT>=23)am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,t.getTimeInMillis(),pi);else am.set(AlarmManager.RTC_WAKEUP,t.getTimeInMillis(),pi);
 }
}