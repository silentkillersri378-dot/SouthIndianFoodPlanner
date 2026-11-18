package com.example.southindianplanner;
import android.app.*;import android.os.*;import android.content.*;import android.graphics.Color;import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;import android.view.*;import android.widget.*;import java.util.*;
public class MainActivity extends Activity{
int day,page;LinearLayout root,body,nav;TextView header;SharedPreferences prefs;
String[][] m={
{"Idli + sambar","Rice + keerai kootu + carrot poriyal","Dosa + egg omelette"},
{"Vegetable poha","Lemon rice + curd + potato fry","Same lemon rice + curd"},
{"Masala dosa + chutney","Rice + sambar + beans poriyal","Egg dosa"},
{"Tomato rice","Rice + rasam + cabbage poriyal","Same rice + rasam"},
{"Ragi dosa + chutney","Rice + mor kuzhambu + beetroot poriyal","Wheat dosa + dal"},
{"Poori + potato masala","Rice + sambar + avial","Same sambar rice"},
{"Adai + avial","Rice + rasam + beans poriyal","Chapati + egg gravy"},
{"Vegetable uthappam","Rice + keerai dal + carrot poriyal","Dosa + coconut chutney"},
{"Lemon rice + curd","Rice + sambar + cabbage poriyal","Same rice + sambar"},
{"Idli + sambar","Bisibele bath + curd","Egg omelette + dosa"},
{"Vegetable poha","Rice + rasam + potato fry","Same rice + rasam"},
{"Pesarattu + chutney","Rice + mor kuzhambu + beans poriyal","Tomato dosa + chutney"},
{"Poori + potato masala","Rice + sambar + avial","Same sambar rice"},
{"Adai + avial","Rice + paruppu + carrot poriyal","Chapati + paneer gravy"},
{"Kal dosa + chutney","Rice + spinach dal + carrot poriyal","Egg dosa"},
{"Coconut rice + curd","Rice + sambar + beans poriyal","Same rice + sambar"},
{"Ragi idli + sambar","Tomato rice + curd","Egg bhurji + dosa"},
{"Vegetable poha","Rice + mor kuzhambu + beetroot poriyal","Same rice + mor kuzhambu"},
{"Onion dosa + chutney","Rice + keerai kootu + potato fry","Vegetable uthappam"},
{"Poori + potato masala","Rice + sambar + avial","Same sambar rice"},
{"Adai + avial","Rice + rasam + beans poriyal","Chapati + egg curry"},
{"Idli + sambar","Rice + cabbage kootu + carrot poriyal","Dosa + egg omelette"},
{"Tamarind rice + curd","Rice + rasam + potato poriyal","Same rice + rasam"},
{"Vegetable uthappam","Rice + keerai dal + beans poriyal","Egg dosa"},
{"Lemon rice + curd","Rice + mor kuzhambu + cabbage poriyal","Same rice + mor kuzhambu"},
{"Pesarattu + chutney","Vegetable pulao + curd","Dosa + tomato chutney"},
{"Poori + potato masala","Rice + sambar + avial","Same sambar rice"},
{"Adai + avial","Rice + paruppu + poriyal","Chapati + egg gravy"},
{"Vegetable poha","Rice + sambar + spinach poriyal","Same sambar rice"},
{"Tomato dosa","Vegetable pulao + curd","Egg omelette + dosa"}};
String[] shop={"Rice","Poha","Dal / paruppu","Tamarind","Curd","Eggs","Onion","Tomato","Potato","Carrot","Beans","Cabbage","Beetroot","Spinach / keerai","Seasonal vegetables","Coconut","Lemon","Coriander leaves","Curry leaves","Chilli","Wheat flour (weekend chapati)","Dosa / idli batter ingredients","Spices and cooking oil"};
String[] reuse={"","Extra lemon rice for dinner","","Extra rice and rasam for dinner","","Extra sambar rice for dinner","","","Extra sambar rice for dinner","","Extra rasam rice for dinner","","Extra sambar rice for dinner","","","Extra sambar rice for dinner","","Extra mor kuzhambu rice for dinner","","","","","Extra rasam rice for dinner","","Extra mor kuzhambu rice for dinner","","Extra sambar rice for dinner","","Extra sambar rice for dinner","Extra sambar rice for dinner"};
int green=Color.rgb(35,115,76),ink=Color.rgb(35,45,39),muted=Color.rgb(105,115,108);
public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("planner",0);day=prefs.getInt("day",0);getWindow().setStatusBarColor(Color.rgb(250,252,249));getWindow().setNavigationBarColor(-1);getWindow().getDecorView().setSystemUiVisibility(8192|16);if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=0)requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},10);build();}
GradientDrawable shape(int c){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(22);return d;}
TextView tx(String s,int z,int c,boolean b){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);if(b)t.setTypeface(null,1);return t;}
LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
String menu(int d){return "Breakfast: "+m[d][0]+"\nLunch: "+m[d][1]+"\nDinner: "+m[d][2];}
void build(){root=col();root.setPadding(18,15,18,6);root.setBackgroundColor(Color.rgb(250,252,249));setContentView(root);root.setOnApplyWindowInsetsListener((v,insets)->{int left,top,right,bottom;if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());left=bars.left;top=bars.top;right=bars.right;bottom=bars.bottom;}else{left=insets.getSystemWindowInsetLeft();top=insets.getSystemWindowInsetTop();right=insets.getSystemWindowInsetRight();bottom=insets.getSystemWindowInsetBottom();}root.setPadding(18+left,15+top,18+right,6+bottom);return insets;});root.addView(tx("🥥 OORU MEALS • 2 ADULTS",17,green,true));header=tx("",25,ink,true);LinearLayout.LayoutParams h=new LinearLayout.LayoutParams(-1,-2);h.topMargin=18;h.bottomMargin=12;root.addView(header,h);ScrollView s=new ScrollView(this);s.setClipToPadding(false);s.setFillViewport(false);body=col();s.addView(body);root.addView(s,new LinearLayout.LayoutParams(-1,0,1));nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER_VERTICAL);nav.setBackgroundColor(Color.WHITE);nav.setElevation(8f);nav.setMinimumHeight((int)(60*getResources().getDisplayMetrics().density+0.5f));String[] tabs={"Home","Calendar","Shopping","Recipes","Settings"};for(int i=0;i<5;i++){final int x=i;TextView t=tx(tabs[i],11,muted,true);t.setGravity(Gravity.CENTER);t.setPadding(1,12,1,12);nav.addView(t,new LinearLayout.LayoutParams(0,-2,1));t.setOnClickListener(v->{page=x;draw();});}root.addView(nav,new LinearLayout.LayoutParams(-1,-2));root.requestApplyInsets();draw();}
void card(String a,String b){LinearLayout c=col();c.setPadding(15,14,15,14);c.setBackground(shape(-1));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=10;body.addView(c,p);c.addView(tx(a,16,ink,true));TextView t=tx(b,14,muted,false);t.setPadding(0,6,0,0);c.addView(t);}
void btn(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(-1);b.setBackground(shape(green));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=8;body.addView(b,p);b.setOnClickListener(v->r.run());}
void draw(){body.removeAllViews();String[] titles={"Your meal plan","30-day calendar","Shopping checklist","Dish index","Settings"};header.setText(titles[page]);for(int i=0;i<5;i++)((TextView)nav.getChildAt(i)).setTextColor(i==page?green:muted);
if(page==0){card("TODAY • DAY "+(day+1),menu(day));card("TOMORROW • DAY "+(((day+1)%30)+1),menu((day+1)%30));if(!reuse[day].isEmpty())card("PREP A LITTLE EXTRA",reuse[day]);card("PLAN RULES","No upma, pongal, appam, idiyappam or evening snacks. Poha and variety rice included. Poori and chapati on weekends only.");btn("Open 30-day calendar",()->{page=1;draw();});btn("Open shopping list",()->{page=2;draw();});btn("Enable 7:00 PM daily reminder",()->remind());}
if(page==1){card("SELECT A DAY","Tap any day to set it as your current day.");for(int i=0;i<30;i++){final int d=i;TextView t=tx("DAY "+(i+1)+"\n"+menu(i),13,ink,false);t.setPadding(13,12,13,12);t.setBackground(shape(-1));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=7;body.addView(t,p);t.setOnClickListener(v->{day=d;prefs.edit().putInt("day",day).apply();page=0;draw();});}}
if(page==2){card("SHOPPING LIST • 2 ADULTS","The PDF gives dishes, not ingredient quantities or prices. These are checklist reminders, not calculated quantities.");for(int i=0;i<shop.length;i++){final int n=i;CheckBox c=new CheckBox(this);c.setText(shop[i]);c.setTextSize(15);c.setChecked(prefs.getBoolean("s"+i,false));c.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean("s"+n,on).apply());body.addView(c);}btn("Clear all ticks",()->{for(int i=0;i<shop.length;i++)prefs.edit().putBoolean("s"+i,false).apply();draw();});}
if(page==3){card("30-DAY DISH INDEX","The PDF contains meal names only, not full recipes, ingredient quantities or cooking steps.");for(int i=0;i<30;i++)card("DAY "+(i+1),m[i][0]+" • "+m[i][1]+" • "+m[i][2]);}
if(page==4){card("YOUR PLAN","30-Day South Indian Food Plan • Tamil Nadu & Karnataka style • Vegetarian + eggs.");card("LEFTOVER SAFETY","PDF guidance: refrigerate cooked rice and curries promptly (within 1 hour in hot weather). Do not leave cooked rice at room temperature until dinner. Reheat thoroughly.");card("BALANCED EATING","Rotate greens, dal, vegetables and curd through the week. Adjust portions to appetite and dietary needs.");btn("Enable / reset 7 PM reminder",()->remind());btn("Turn off reminder",()->{Intent i=new Intent(this,ReminderReceiver.class);PendingIntent p=PendingIntent.getBroadcast(this,1,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);((AlarmManager)getSystemService(ALARM_SERVICE)).cancel(p);Toast.makeText(this,"Reminder off",0).show();});}}
void remind(){AlarmManager a=(AlarmManager)getSystemService(ALARM_SERVICE);Intent i=new Intent(this,ReminderReceiver.class);PendingIntent p=PendingIntent.getBroadcast(this,1,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,19);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);if(c.getTimeInMillis()<=System.currentTimeMillis())c.add(Calendar.DATE,1);a.setInexactRepeating(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),AlarmManager.INTERVAL_DAY,p);Toast.makeText(this,"Daily 7 PM reminder enabled",1).show();}
}