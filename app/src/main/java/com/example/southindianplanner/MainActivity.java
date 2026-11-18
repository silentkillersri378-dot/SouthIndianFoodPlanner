package com.example.southindianplanner;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.provider.Settings;
import java.util.*;
import java.text.SimpleDateFormat;
import java.io.*;
import org.json.*;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.android.gms.tasks.Tasks;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    int page=0;
    LinearLayout root, body, nav;
    TextView header;
    SharedPreferences prefs;
    final int green=Color.rgb(35,115,76), ink=Color.rgb(35,45,39), muted=Color.rgb(105,115,108), pale=Color.rgb(250,252,249);
    interface SaveFields { void save(String[] values); }
    static final int REQ_BACKUP_EXPORT=4101, REQ_BACKUP_IMPORT=4102, REQ_BILL_SCAN=4103, REQ_BILL_CAMERA=4104;
    android.net.Uri billCameraUri;

    final String[][] meals={
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
      {"Tomato dosa","Vegetable pulao + curd","Egg omelette + dosa"}
    };
    final String[] reuse={
      "", "Prepare extra lemon rice for dinner.", "", "Prepare extra rice and rasam for dinner.", "",
      "Prepare extra sambar rice for dinner.", "", "", "Prepare extra sambar rice for dinner.", "",
      "Prepare extra rasam rice for dinner.", "", "Prepare extra sambar rice for dinner.", "", "",
      "", "", "", "", "", "", "", "", "", "", "", "", "", "Prepare extra sambar rice for dinner.", ""
    };

    public void onCreate(Bundle b){
      super.onCreate(b);
      prefs=getSharedPreferences("planner_v3",0);
      getWindow().setStatusBarColor(pale);
      getWindow().setNavigationBarColor(Color.WHITE);
      if(Build.VERSION.SDK_INT>=23)getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
      Calendar c=Calendar.getInstance(); c.set(Calendar.DAY_OF_MONTH,1); midnight(c);
      if(!prefs.contains("cycle_start"))prefs.edit().putLong("cycle_start",c.getTimeInMillis()).apply();
      if(!prefs.getBoolean("v3_final_migration",false)){
        prefs.edit().putString("milk_qty","500").putBoolean("morning_enabled",true).putBoolean("evening_enabled",true)
          .putInt("morning_hour",8).putInt("morning_min",0).putInt("evening_hour",19).putInt("evening_min",0)
          .remove("budget").putBoolean("v3_final_migration",true).apply();
      }
      loadSavedMeals();
      build();
      createNotificationChannel();
      if(Build.VERSION.SDK_INT>=33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=0)
        requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},10);
      scheduleAllReminders();
    }

    void midnight(Calendar c){c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);}
    GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(24);return d;}
    TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    String money(double v){return "₹"+String.format(Locale.US,"%.2f",v);}
    String date(long ms){return new SimpleDateFormat("dd MMM yyyy",Locale.getDefault()).format(new Date(ms));}
    int dayFor(Calendar date){
      Calendar start=Calendar.getInstance();start.setTimeInMillis(prefs.getLong("cycle_start",System.currentTimeMillis()));midnight(start);
      Calendar d=(Calendar)date.clone();midnight(d);
      long diff=(d.getTimeInMillis()-start.getTimeInMillis())/86400000L;
      if(diff<0)diff=0;
      return (int)(diff%30);
    }
    void build(){
      root=col();root.setBackgroundColor(pale);setContentView(root);
      root.setOnApplyWindowInsetsListener((v,in)->{
        int l,t,r,b;
        if(Build.VERSION.SDK_INT>=30){android.graphics.Insets x=in.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());l=x.left;t=x.top;r=x.right;b=x.bottom;}
        else{l=in.getSystemWindowInsetLeft();t=in.getSystemWindowInsetTop();r=in.getSystemWindowInsetRight();b=in.getSystemWindowInsetBottom();}
        root.setPadding(16+l,10+t,16+r,6+b);return in;
      });
      root.addView(text("🥥 NSV SMART KITCHEN  •  2 ADULTS",17,green,true));
      header=text("",25,ink,true);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=12;hp.bottomMargin=10;root.addView(header,hp);
      ScrollView sc=new ScrollView(this);sc.setFillViewport(false);body=col();sc.addView(body);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
      nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setBackgroundColor(Color.WHITE);nav.setElevation(8);
      String[] tabs={"Home","Calendar","Shopping","Recipes","Settings"};
      for(int i=0;i<5;i++){final int p=i;TextView t=text(tabs[i],11,muted,true);t.setGravity(Gravity.CENTER);t.setPadding(1,15,1,15);nav.addView(t,new LinearLayout.LayoutParams(0,-2,1));t.setOnClickListener(v->{page=p;draw();});}
      root.addView(nav,new LinearLayout.LayoutParams(-1,-2));root.requestApplyInsets();draw();
    }
    LinearLayout card(String title,String desc){
      LinearLayout c=col();c.setPadding(15,14,15,14);c.setBackground(shape(Color.WHITE));
      LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=10;body.addView(c,p);
      c.addView(text(title,16,ink,true));
      if(desc!=null&&!desc.isEmpty()){TextView d=text(desc,14,muted,false);d.setPadding(0,6,0,0);c.addView(d);}
      return c;
    }
    void button(String label,Runnable action){
      Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setBackground(shape(green));
      LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=8;body.addView(b,p);b.setOnClickListener(v->action.run());
    }
    EditText edit(String value,String hint){
      EditText e=new EditText(this);e.setSingleLine(true);e.setText(value==null?"":value);e.setHint(hint);e.setTextSize(16);return e;
    }
    void dialogFields(String title,String[] labels,String[] values,SaveFields save){
      LinearLayout box=col();box.setPadding(8,4,8,0);EditText[] fields=new EditText[labels.length];
      for(int i=0;i<labels.length;i++){box.addView(text(labels[i],13,muted,true));fields[i]=edit(values[i],"Enter "+labels[i]);box.addView(fields[i]);}
      new AlertDialog.Builder(this).setTitle(title).setView(box).setNegativeButton("Cancel",null)
        .setPositiveButton("Save",(d,w)->{String[] v=new String[fields.length];for(int i=0;i<v.length;i++)v[i]=fields[i].getText().toString().trim();save.save(v);}).show();
    }
    String mealText(int d){return "Breakfast: "+meals[d][0]+"\nLunch: "+meals[d][1]+"\nDinner: "+meals[d][2];}
    void loadSavedMeals(){for(int d=0;d<30;d++)for(int i=0;i<3;i++){String x=prefs.getString("meal_"+d+"_"+i,null);if(x!=null)meals[d][i]=x;}}
    void saveMeals(){SharedPreferences.Editor e=prefs.edit();for(int d=0;d<30;d++)for(int i=0;i<3;i++)e.putString("meal_"+d+"_"+i,meals[d][i]);e.apply();}
    void draw(){
      body.removeAllViews();String[] titles={"Your meal plan","30-day calendar","Today's shopping list","Meal recipes","Settings"};header.setText(titles[page]);
      for(int i=0;i<5;i++)((TextView)nav.getChildAt(i)).setTextColor(i==page?green:muted);
      Calendar now=Calendar.getInstance();int today=dayFor(now);
      if(page==0){
        String todayText=new SimpleDateFormat("EEEE, dd MMMM yyyy",Locale.getDefault()).format(now.getTime());
        card(todayText,"TODAY  •  DAY "+(today+1)+" OF 30\n"+mealText(today));
        int next=(today+1)%30;card("TOMORROW  •  DAY "+(next+1),mealText(next));
        if(!reuse[today].isEmpty())card("COOK ONCE, SAVE TIME",reuse[today]);
        double spent=monthSpend();String budgetText=prefs.contains("budget")?"Budget: "+money(num(prefs.getString("budget","0")))+"\nRemaining: "+money(num(prefs.getString("budget","0"))-spent):"Budget not set yet — tracking actual spending for the first month.";
        card("THIS MONTH'S GROCERY SPENDING","Actual purchases: "+money(spent)+"\n"+budgetText);
        button("See today's combined ingredients",()->{page=2;draw();});
        button("Open 30-day calendar",()->{page=1;draw();});
      }
      if(page==1){
        card("YOUR ORIGINAL 30-DAY PLAN","Calendar follows your phone date and selected start date. Edit any day's breakfast, lunch or dinner.");
        for(int i=0;i<30;i++){
          final int n=i;LinearLayout c=card("DAY "+(i+1)+(i==today?"  •  TODAY":""),mealText(i));
          if(i==today)c.setBackground(shape(Color.rgb(232,246,237)));
          Button e=new Button(this);e.setText("Edit this day's menu");e.setAllCaps(false);c.addView(e);e.setOnClickListener(v->editMeal(n));
        }
      }
      if(page==2)drawShopping(today);
      if(page==3){
        card("MEAL & INGREDIENT GUIDE","The PDF gives the 30-day menu and leftover notes, not measured recipes. Ingredient suggestions below are editable; no grocery quantities or prices are assumed.");
        for(int i=0;i<30;i++){final int n=i;LinearLayout c=card("DAY "+(i+1),mealText(i));Button e=new Button(this);e.setText("Edit meals");e.setAllCaps(false);c.addView(e);e.setOnClickListener(v->editMeal(n));}
      }
      if(page==4)drawSettings();
    }

    void drawShopping(int day){
      LinkedHashSet<String> list=ingredientsForDay(day);
      card("TODAY • DAY "+(day+1),mealText(day));
      card("ONE COMBINED SHOPPING CHECKLIST","Ingredients for breakfast, lunch and dinner are combined below. Tick items you already have at home. Buy only what remains unchecked. Milk requirement: "+prefs.getString("milk_qty","500")+" ml today.");
      button("Scan grocery bill / invoice",()->scanGroceryBill());
      button("Edit today's ingredient list",()->editDayIngredients(day));
      for(String item:list){
        final String id=key(item);String status=stockStatus(id);
        if("available".equals(status))continue;
        LinearLayout itemCard=card(item,item.equals("Milk")?"Daily requirement: "+prefs.getString("milk_qty","500")+" ml\\n"+priceSummary(id):priceSummary(id));
        Button stock=new Button(this);stock.setAllCaps(false);stock.setText("Stock: "+statusLabel(status)+" • Change");itemCard.addView(stock);
        stock.setOnClickListener(v->chooseStockStatus(item));
        Button buy=new Button(this);buy.setText("I bought this • Enter quantity & price");buy.setAllCaps(false);itemCard.addView(buy);buy.setOnClickListener(v->recordPurchase(item));
      }
      card("MONTHLY SPENDING","Actual purchases this month: "+money(monthSpend())+"\nRice is recorded only in the month you buy it (for example, your occasional ₹1,500 purchase). No automatic monthly rice charge.");
      button("View monthly spending history",()->showMonthlyHistory());
      button("View all purchase history",()->showHistory());
      button("Add another grocery item",()->addCustomIngredient());
    }

    LinkedHashSet<String> ingredientsForDay(int d){
      String custom=prefs.getString("day_ingredients_"+d,null);
      LinkedHashSet<String> out=new LinkedHashSet<>();
      if(custom!=null){for(String x:custom.split(","))if(!x.trim().isEmpty())out.add(x.trim());}
      else{
        for(String meal:meals[d])mapDishIngredients(meal,out);
      }
      out.add("Milk");
      for(Map.Entry<String,?> en:prefs.getAll().entrySet())if(en.getKey().startsWith("custom_item_"))out.add(String.valueOf(en.getValue()));
      return out;
    }
    void add(LinkedHashSet<String> s,String... values){for(String v:values)s.add(v);}
    void mapDishIngredients(String meal,LinkedHashSet<String> s){
      String m=meal.toLowerCase(Locale.US);
      if(m.contains("same ")) { // leftover meal: don't list duplicate ingredients as new shopping needs
        if(m.contains("lemon"))add(s,"Rice","Lemon","Peanuts","Curry leaves","Mustard seeds","Urad dal","Chana dal","Turmeric powder");
        else if(m.contains("rasam"))add(s,"Rice","Tomato","Tamarind","Rasam powder","Cumin seeds","Pepper","Curry leaves");
        else if(m.contains("mor kuzhambu"))add(s,"Rice","Curd","Coconut","Green chilli","Cumin seeds","Turmeric powder");
        else if(m.contains("sambar"))add(s,"Rice","Toor dal","Sambar powder","Tamarind","Onion","Tomato","Mixed vegetables");
        else add(s,"Rice","Onion","Tomato","Cooking oil","Salt");
        return;
      }
      if(m.contains("rice")||m.contains("pulao")||m.contains("bisibele"))add(s,"Rice");
      if(m.contains("idli")||m.contains("dosa")||m.contains("uthappam"))add(s,"Idli/dosa batter or rice + urad dal","Cooking oil");
      if(m.contains("poha"))add(s,"Poha","Onion","Potato","Peanuts","Green chilli","Lemon","Curry leaves");
      if(m.contains("lemon rice"))add(s,"Lemon","Peanuts","Urad dal","Chana dal","Curry leaves","Mustard seeds","Turmeric powder");
      if(m.contains("tomato rice"))add(s,"Tomato","Onion","Ginger","Garlic","Chilli powder","Coriander leaves");
      if(m.contains("coconut rice"))add(s,"Coconut","Peanuts","Urad dal","Chana dal","Curry leaves","Mustard seeds");
      if(m.contains("tamarind rice"))add(s,"Tamarind","Peanuts","Sesame seeds","Urad dal","Chana dal","Curry leaves");
      if(m.contains("vegetable pulao"))add(s,"Mixed vegetables","Onion","Tomato","Ginger","Garlic","Mint leaves","Coriander leaves","Garam masala");
      if(m.contains("bisibele"))add(s,"Toor dal","Mixed vegetables","Tamarind","Bisibele bath masala","Peanuts");
      if(m.contains("sambar"))add(s,"Toor dal","Sambar powder","Tamarind","Onion","Tomato","Drumstick / mixed vegetables","Curry leaves");
      if(m.contains("rasam"))add(s,"Tomato","Tamarind","Rasam powder","Cumin seeds","Pepper","Garlic","Coriander leaves");
      if(m.contains("mor kuzhambu"))add(s,"Curd","Coconut","Green chilli","Cumin seeds","Toor dal","Turmeric powder");
      if(m.contains("keerai"))add(s,"Keerai / spinach","Moong dal","Onion","Garlic","Cumin seeds");
      if(m.contains("kootu"))add(s,"Toor dal","Coconut","Cumin seeds","Green chilli");
      if(m.contains("dal")||m.contains("paruppu"))add(s,"Toor dal","Onion","Tomato","Garlic","Turmeric powder");
      if(m.contains("poriyal")){if(m.contains("carrot"))add(s,"Carrot");if(m.contains("beans"))add(s,"Beans");if(m.contains("cabbage"))add(s,"Cabbage");if(m.contains("beetroot"))add(s,"Beetroot");if(m.contains("potato"))add(s,"Potato");add(s,"Coconut","Mustard seeds","Urad dal","Curry leaves");}
      if(m.contains("potato"))add(s,"Potato","Onion","Green chilli","Turmeric powder");
      if(m.contains("avial"))add(s,"Mixed vegetables","Coconut","Curd","Cumin seeds","Green chilli","Curry leaves");
      if(m.contains("adai"))add(s,"Toor dal","Chana dal","Urad dal","Rice","Onion","Green chilli");
      if(m.contains("pesarattu"))add(s,"Whole green gram","Ginger","Green chilli","Onion");
      if(m.contains("chapati"))add(s,"Wheat flour","Oil");
      if(m.contains("poori"))add(s,"Wheat flour","Potato","Onion","Cooking oil");
      if(m.contains("egg"))add(s,"Eggs","Onion","Green chilli","Coriander leaves");
      if(m.contains("paneer"))add(s,"Paneer","Onion","Tomato","Ginger","Garlic","Garam masala");
      if(m.contains("curd"))add(s,"Curd");
      if(m.contains("chutney"))add(s,"Coconut","Green chilli","Ginger","Roasted gram","Curry leaves","Mustard seeds");
      if(m.contains("wheat dosa"))add(s,"Wheat flour","Onion","Green chilli");
      if(m.contains("onion dosa"))add(s,"Onion","Idli/dosa batter or rice + urad dal");
      if(m.contains("tomato dosa"))add(s,"Tomato","Idli/dosa batter or rice + urad dal");
      add(s,"Salt","Cooking oil","Mustard seeds","Curry leaves");
    }

    String stockStatus(String id){String s=prefs.getString("stock_"+id,null);if(s!=null)return s;return prefs.getBoolean("have_"+id,false)?"available":"finished";}
    String statusLabel(String s){return "available".equals(s)?"Available":("low".equals(s)?"Running low":"Finished");}
    void chooseStockStatus(String item){
      String id=key(item);String[] labels={"Available - do not buy","Running low - remind me","Finished - add to shopping list"};String[] values={"available","low","finished"};String cur=stockStatus(id);int selected="available".equals(cur)?0:("low".equals(cur)?1:2);
      new AlertDialog.Builder(this).setTitle(item+" stock status").setSingleChoiceItems(labels,selected,(d,w)->{String next=values[w];prefs.edit().putString("stock_"+id,next).putBoolean("have_"+id,"available".equals(next)).apply();d.dismiss();if("low".equals(next))sendLowStockAlert(item);draw();}).setNegativeButton("Cancel",null).show();
    }
    void sendLowStockAlert(String item){try{createNotificationChannel();Notification.Builder n=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"ooru_meals_reminders"):new Notification.Builder(this);Intent open=new Intent(this,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(this,900+Math.abs(key(item).hashCode()%500),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);n.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("NSV - Low stock").setContentText(item+" is running low. Add it to your shopping trip.").setContentIntent(pi).setAutoCancel(true).setPriority(Notification.PRIORITY_HIGH);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(900+Math.abs(key(item).hashCode()%500),n.build());}catch(Exception ignored){}}
    void editDayIngredients(int day){
      String current=prefs.getString("day_ingredients_"+day,joinItems(ingredientsForDayDefault(day)));
      dialogFields("Edit Day "+(day+1)+" ingredients",new String[]{"Ingredients (separate with commas)"},new String[]{current},v->{
        prefs.edit().putString("day_ingredients_"+day,v[0]).apply();draw();
      });
    }
    LinkedHashSet<String> ingredientsForDayDefault(int d){
      String old=prefs.getString("day_ingredients_"+d,null);
      if(old!=null){LinkedHashSet<String>x=new LinkedHashSet<>();for(String a:old.split(","))x.add(a.trim());return x;}
      LinkedHashSet<String> x=new LinkedHashSet<>();for(String m:meals[d])mapDishIngredients(m,x);x.add("Milk");return x;
    }

    void editMeal(int d){
      dialogFields("Edit Day "+(d+1),new String[]{"Breakfast","Lunch","Dinner"},meals[d],a->{
        for(int i=0;i<3;i++)meals[d][i]=a[i];saveMeals();
        prefs.edit().remove("day_ingredients_"+d).apply();draw();
      });
    }
    void drawSettings(){
      card("PLAN START DATE","Current start date: "+date(prefs.getLong("cycle_start",System.currentTimeMillis()))+"\nThe calendar uses the real phone date. Set the start date to the first day of your plan.");
      button("Change 30-day plan start date",()->pickStartDate());
      card("DAILY MILK","Daily requirement: "+prefs.getString("milk_qty","500")+" ml. Actual milk purchases are entered in Shopping and counted only when you buy them.");
      button("Change daily milk requirement",()->dialogFields("Daily milk requirement",new String[]{"Millilitres per day"},new String[]{prefs.getString("milk_qty","500")},v->{double ml=num(v[0]);if(ml>0)prefs.edit().putString("milk_qty",String.valueOf(ml)).apply();draw();}));
      double spent=monthSpend();
      card("MONTHLY GROCERY BUDGET","First month: tracking your actual purchases before setting a budget.\nSpent this month: "+money(spent)+(prefs.contains("budget")?"\nCurrent budget: "+money(num(prefs.getString("budget","0"))):"\nNo budget set yet.")+"\n\nPrevious months:\n"+monthHistorySummary());
      card("ADVANCED FINANCE DASHBOARD",financeDashboard()+"\n\nShopping history and price comparisons are available in Shopping.");
      button(prefs.contains("budget")?"Change monthly budget":"Set budget after reviewing spending",()->dialogFields("Monthly grocery budget",new String[]{"Monthly budget (₹)"},new String[]{prefs.getString("budget","")},v->{if(v[0].isEmpty())prefs.edit().remove("budget").apply();else if(num(v[0])>0)prefs.edit().putString("budget",v[0]).apply();draw();}));
      card("MONTHLY BUDGET REPORTS","Get a full grocery budget summary automatically on the last day and first day of every month.");
      addMonthlyReportControls();
      card("MORNING NOTIFICATION","Today's breakfast, lunch and dinner. Separate time and enable switch.");
      addReminderControls(true);
      card("EVENING NOTIFICATION","Tomorrow's breakfast, lunch, dinner and reminder to check the combined ingredient list.");
      addReminderControls(false);
      button("Check Android notification permission",()->{if(Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},10);else Toast.makeText(this,"Notification permission is managed by Android settings.",Toast.LENGTH_LONG).show();});
      if(Build.VERSION.SDK_INT>=31)button("Allow exact-time reminders in Android",()->{try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));}catch(Exception e){startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName())));}});
      card("FOOD PLAN RULES FROM YOUR PDF","No upma, pongal, appam, idiyappam or evening snacks. Poha and variety rice included. Poori and chapati on weekends only. Some lunch dishes are repeated for dinner to reduce cooking.");
      card("LEFTOVER FOOD SAFETY","Refrigerate cooked rice and curries promptly (within 1 hour in hot weather). Do not leave cooked rice at room temperature until dinner. Reheat thoroughly before serving.");
      card("BACKUP & RESTORE","Export a copy of your menus, purchases, stock statuses, price history, budget and reminder settings. Restore it on this phone when needed.");
      button("Export app data backup",()->exportBackup());
      button("Restore app data from backup",()->confirmRestoreBackup());
            card("DATA","Your menu edits, purchases, quantities, prices and settings are saved on this phone and remain after closing the app.");
    }
    void exportBackup(){
      try{
        JSONObject rootJson=new JSONObject();rootJson.put("app","NSV Smart Kitchen");rootJson.put("backupVersion",1);rootJson.put("exportedAt",new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",Locale.US).format(new Date()));
        JSONObject data=new JSONObject();
        for(Map.Entry<String,?> entry:prefs.getAll().entrySet()){
          Object value=entry.getValue();JSONObject item=new JSONObject();
          if(value instanceof Boolean){item.put("type","boolean");item.put("value",(Boolean)value);}
          else if(value instanceof Integer){item.put("type","int");item.put("value",(Integer)value);}
          else if(value instanceof Long){item.put("type","long");item.put("value",(Long)value);}
          else if(value instanceof Float){item.put("type","float");item.put("value",((Float)value).doubleValue());}
          else {item.put("type","string");item.put("value",String.valueOf(value));}
          data.put(entry.getKey(),item);
        }
        rootJson.put("preferences",data);
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/json");intent.putExtra(Intent.EXTRA_TITLE,"NSV-Smart-Kitchen-Backup-"+new SimpleDateFormat("yyyyMMdd-HHmm",Locale.US).format(new Date())+".json");
        pendingBackupJson=rootJson.toString(2);startActivityForResult(intent,REQ_BACKUP_EXPORT);
      }catch(Exception e){Toast.makeText(this,"Could not prepare backup: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    String pendingBackupJson;
    void confirmRestoreBackup(){
      new AlertDialog.Builder(this).setTitle("Restore NSV backup?")
        .setMessage("Restoring replaces the app's current saved data with the selected backup. Export a fresh backup first if you want to keep the current data.")
        .setNegativeButton("Cancel",null).setPositiveButton("Choose backup",(d,w)->{
          Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/json");startActivityForResult(intent,REQ_BACKUP_IMPORT);
        }).show();
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
      super.onActivityResult(requestCode,resultCode,data);
      if(resultCode!=RESULT_OK)return;
      if(requestCode==REQ_BILL_CAMERA){if(billCameraUri!=null)processBillUri(billCameraUri);else Toast.makeText(this,"Camera did not return a photo.",Toast.LENGTH_LONG).show();return;}
      if(data==null||data.getData()==null)return;
      if(requestCode==REQ_BILL_SCAN){processBillUri(data.getData());return;}
      if(requestCode==REQ_BACKUP_EXPORT){
        try(OutputStream out=getContentResolver().openOutputStream(data.getData())){
          if(out==null)throw new IOException("Unable to open the selected destination");
          out.write(pendingBackupJson.getBytes("UTF-8"));out.flush();Toast.makeText(this,"Backup exported successfully.",Toast.LENGTH_LONG).show();
        }catch(Exception e){Toast.makeText(this,"Backup export failed: "+e.getMessage(),Toast.LENGTH_LONG).show();}
      }else if(requestCode==REQ_BACKUP_IMPORT){
        try(InputStream in=getContentResolver().openInputStream(data.getData())){
          if(in==null)throw new IOException("Unable to read selected backup");
          ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);
          JSONObject rootJson=new JSONObject(new String(bytes.toByteArray(),"UTF-8"));
          if(!rootJson.has("preferences")||rootJson.optInt("backupVersion",0)!=1)throw new IOException("This is not a supported NSV backup file");
          JSONObject saved=rootJson.getJSONObject("preferences");SharedPreferences.Editor editor=prefs.edit().clear();
          Iterator<String> keys=saved.keys();
          while(keys.hasNext()){
            String key=keys.next();JSONObject item=saved.getJSONObject(key);String type=item.optString("type");Object value=item.opt("value");
            if("boolean".equals(type))editor.putBoolean(key,Boolean.parseBoolean(String.valueOf(value)));
            else if("int".equals(type))editor.putInt(key,Integer.parseInt(String.valueOf(value)));
            else if("long".equals(type))editor.putLong(key,Long.parseLong(String.valueOf(value)));
            else if("float".equals(type))editor.putFloat(key,Float.parseFloat(String.valueOf(value)));
            else if(value!=null&&!JSONObject.NULL.equals(value))editor.putString(key,String.valueOf(value));
          }
          editor.apply();loadSavedMeals();scheduleAllReminders();draw();
          Toast.makeText(this,"Backup restored successfully. Your saved app data has been replaced with the backup.",Toast.LENGTH_LONG).show();
        }catch(Exception e){Toast.makeText(this,"Restore failed. Your existing data was not intentionally changed. Check that you selected a valid NSV backup. "+e.getMessage(),Toast.LENGTH_LONG).show();}
      }
    }

    void addReminderControls(boolean morning){
      String prefix=morning?"morning":"evening";
      boolean enabled=prefs.getBoolean(prefix+"_enabled",true);
      int h=prefs.getInt(prefix+"_hour",morning?8:19),m=prefs.getInt(prefix+"_min",0);
      LinearLayout c=card(morning?"Morning reminder":"Evening reminder",String.format(Locale.US,"Current time: %02d:%02d",h,m));
      CheckBox sw=new CheckBox(this);sw.setText("Enable this notification");sw.setChecked(enabled);c.addView(sw);
      sw.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean(prefix+"_enabled",on).apply();scheduleAllReminders();draw();});
      Button time=new Button(this);time.setText("Change "+(morning?"morning":"evening")+" time");time.setAllCaps(false);c.addView(time);
      time.setOnClickListener(v->new TimePickerDialog(this,(tp,hh,mm)->{
        prefs.edit().putInt(prefix+"_hour",hh).putInt(prefix+"_min",mm).putBoolean(prefix+"_enabled",true).apply();
        scheduleAllReminders();draw();
      },h,m,false).show());
    }
    void pickStartDate(){
      Calendar c=Calendar.getInstance();c.setTimeInMillis(prefs.getLong("cycle_start",System.currentTimeMillis()));
      new DatePickerDialog(this,(v,y,m,d)->{Calendar x=Calendar.getInstance();x.set(y,m,d);midnight(x);prefs.edit().putLong("cycle_start",x.getTimeInMillis()).apply();scheduleAllReminders();draw();},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
    }

    String key(String s){return s.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+","_");}
    double num(String s){try{return Double.parseDouble(s);}catch(Exception e){return 0;}}
    JSONArray purchases(String id){try{return new JSONArray(prefs.getString("purchases_"+id,"[]"));}catch(Exception e){return new JSONArray();}}
    String priceSummary(String id){
      JSONArray a=purchases(id);if(a.length()==0)return "No purchase recorded yet";
      try{
        JSONObject last=a.getJSONObject(a.length()-1);double p=last.optDouble("unitPrice",0);
        String s="Last bought: "+money(p)+" per "+last.optString("unit","unit");
        if(a.length()>1){JSONObject prev=a.getJSONObject(a.length()-2);double old=prev.optDouble("unitPrice",p);s+="\nPrevious: "+money(old)+" per "+prev.optString("unit","unit");}
        return s;
      }catch(Exception e){return "Purchase history available";}
    }
    void recordPurchase(String item){
      dialogFields("Record purchase • "+item,new String[]{"Quantity bought (e.g. 1)","Unit (kg, litre, packet, piece etc.)","Total amount paid (₹)"},new String[]{"","",""},v->{
        double q=num(v[0]),total=num(v[2]);if(q<=0||total<0||v[1].trim().isEmpty()){Toast.makeText(this,"Enter a valid quantity, unit and price.",Toast.LENGTH_LONG).show();return;}
        String id=key(item);JSONArray a=purchases(id);JSONObject o=new JSONObject();
        try{o.put("date",new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date()));o.put("qty",q);o.put("unit",v[1].trim());o.put("total",total);o.put("unitPrice",total/q);a.put(o);
          prefs.edit().putString("purchases_"+id,a.toString()).putBoolean("have_"+id,true).putString("stock_"+id,"available").apply();
          Toast.makeText(this,"Purchase saved. Price history updated.",Toast.LENGTH_SHORT).show();draw();
        }catch(Exception e){Toast.makeText(this,"Could not save purchase.",Toast.LENGTH_SHORT).show();}
      });
    }
    String joinItems(Collection<String> items){StringBuilder b=new StringBuilder();for(String x:items){if(b.length()>0)b.append(", ");b.append(x);}return b.toString();}
    Map<String,Double> monthTotals(){
      Map<String,Double> totals=new TreeMap<>();
      for(Map.Entry<String,?> en:prefs.getAll().entrySet())if(en.getKey().startsWith("purchases_")){
        try{JSONArray a=new JSONArray(String.valueOf(en.getValue()));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);String m=o.optString("date");if(m.length()>=7){m=m.substring(0,7);totals.put(m,totals.getOrDefault(m,0.0)+o.optDouble("total",0));}}}catch(Exception ignored){}
      }
      return totals;
    }
    String monthHistorySummary(){
      Map<String,Double> totals=monthTotals();if(totals.isEmpty())return "No completed purchase months yet.";
      StringBuilder b=new StringBuilder();for(Map.Entry<String,Double> e:totals.entrySet())b.append(e.getKey()).append(": ").append(money(e.getValue())).append("\n");return b.toString().trim();
    }
    String categoryFor(String item){String x=item.toLowerCase(Locale.US);if(x.equals("milk"))return "Milk";if(x.contains("rice")||x.contains("poha")||x.contains("wheat")||x.contains("dal")||x.contains("flour"))return "Rice & grains";if(x.contains("onion")||x.contains("tomato")||x.contains("carrot")||x.contains("beans")||x.contains("vegetable")||x.contains("keerai")||x.contains("spinach")||x.contains("chilli")||x.contains("lemon")||x.contains("potato")||x.contains("cabbage")||x.contains("beetroot")||x.contains("curry leaves")||x.contains("coriander")||x.contains("mint"))return "Vegetables & fruits";return "Groceries & staples";}
    double spendForMonth(String month){double sum=0;for(Map.Entry<String,?> en:prefs.getAll().entrySet())if(en.getKey().startsWith("purchases_"))try{JSONArray a=new JSONArray(String.valueOf(en.getValue()));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);if(o.optString("date").startsWith(month))sum+=o.optDouble("total",0);}}catch(Exception ignored){}return sum;}
    String categoryExpenseSummary(String month){TreeMap<String,Double> totals=new TreeMap<>();for(Map.Entry<String,?> en:prefs.getAll().entrySet())if(en.getKey().startsWith("purchases_"))try{JSONArray a=new JSONArray(String.valueOf(en.getValue()));String item=en.getKey().substring(10).replace("_"," ");for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);if(o.optString("date").startsWith(month)){String cat=categoryFor(item);totals.put(cat,totals.getOrDefault(cat,0.0)+o.optDouble("total",0));}}}catch(Exception ignored){}if(totals.isEmpty())return "No category expenses recorded yet.";StringBuilder b=new StringBuilder();for(Map.Entry<String,Double> e:totals.entrySet())b.append(e.getKey()).append(": ").append(money(e.getValue())).append("\\n");return b.toString().trim();}
    String financeDashboard(){Calendar now=Calendar.getInstance();String month=new SimpleDateFormat("yyyy-MM",Locale.US).format(now.getTime());double spent=spendForMonth(month);int day=now.get(Calendar.DAY_OF_MONTH),days=now.getActualMaximum(Calendar.DAY_OF_MONTH);double forecast=day>0?spent/day*days:spent;Calendar prev=(Calendar)now.clone();prev.add(Calendar.MONTH,-1);String pm=new SimpleDateFormat("yyyy-MM",Locale.US).format(prev.getTime());double previous=spendForMonth(pm);String budget=prefs.contains("budget")?"\\nBudget: "+money(num(prefs.getString("budget","0")))+"\\nRemaining: "+money(num(prefs.getString("budget","0"))-spent):"\\nBudget not set yet";String compare=previous>0?"\\nPrevious month: "+money(previous)+"\\nChange: "+(spent-previous>=0?"+":"")+money(spent-previous):"\\nPrevious month: No recorded purchases";return "Month-to-date spending: "+money(spent)+budget+"\\nEstimated month-end: "+money(forecast)+compare+"\\n\\nCATEGORY EXPENSES\\n"+categoryExpenseSummary(month);}
    String ingredientMonthSummary(){
      String month=new SimpleDateFormat("yyyy-MM",Locale.US).format(new Date());
      TreeMap<String,Double> totals=new TreeMap<>();
      for(Map.Entry<String,?> en:prefs.getAll().entrySet())if(en.getKey().startsWith("purchases_")){
        try{JSONArray a=new JSONArray(String.valueOf(en.getValue()));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);if(o.optString("date").startsWith(month)){String name=en.getKey().substring(10).replace("_"," ");totals.put(name,totals.getOrDefault(name,0.0)+o.optDouble("total",0));}}}catch(Exception ignored){}
      }
      if(totals.isEmpty())return "No purchases recorded this month.";
      StringBuilder b=new StringBuilder();for(Map.Entry<String,Double> e:totals.entrySet())b.append(e.getKey()).append(": ").append(money(e.getValue())).append("\n");return b.toString().trim();
    }
    void showMonthlyHistory(){new AlertDialog.Builder(this).setTitle("Monthly grocery spending").setMessage("Month totals:\n"+monthHistorySummary()+"\n\nThis month's ingredient expenses:\n"+ingredientMonthSummary()).setPositiveButton("Close",null).show();}
    double monthSpend(){
      String month=new SimpleDateFormat("yyyy-MM",Locale.US).format(new Date());double sum=0;
      for(Map.Entry<String,?> en:prefs.getAll().entrySet())if(en.getKey().startsWith("purchases_")){
        try{JSONArray a=new JSONArray(String.valueOf(en.getValue()));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);if(o.optString("date").startsWith(month))sum+=o.optDouble("total",0);}}catch(Exception ignored){}
      }
      return sum;
    }
    void showHistory(){
      StringBuilder s=new StringBuilder();String month=new SimpleDateFormat("yyyy-MM",Locale.US).format(new Date());
      for(Map.Entry<String,?> en:prefs.getAll().entrySet())if(en.getKey().startsWith("purchases_")){
        try{JSONArray a=new JSONArray(String.valueOf(en.getValue()));for(int i=a.length()-1;i>=0;i--){JSONObject o=a.getJSONObject(i);s.append(en.getKey().substring(10).replace('_',' ')).append(" • ").append(o.optString("date")).append(" • ").append(o.optDouble("qty")).append(" ").append(o.optString("unit")).append(" • ").append(money(o.optDouble("total"))).append("\n");}}catch(Exception ignored){}
      }
      if(s.length()==0)s.append("No purchases recorded yet.");
      new AlertDialog.Builder(this).setTitle("Purchase history").setMessage(s.toString()).setPositiveButton("Close",null).show();
    }
        void scanGroceryBill(){
      new AlertDialog.Builder(this).setTitle("Scan grocery bill")
        .setItems(new String[]{"Take a photo with camera","Choose photo or PDF invoice"},(d,which)->{if(which==0)takeBillPhoto();else chooseBillFile();}).show();
    }
    void takeBillPhoto(){
      try{
        android.content.ContentValues values=new android.content.ContentValues();
        values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,"NSV_bill_"+System.currentTimeMillis()+".jpg");
        values.put(android.provider.MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
        if(Build.VERSION.SDK_INT>=29)values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,"Pictures/NSV Bills");
        billCameraUri=getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        if(billCameraUri==null)throw new IOException("Could not prepare a photo location");
        Intent camera=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);camera.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,billCameraUri);
        camera.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if(camera.resolveActivity(getPackageManager())==null)throw new IOException("No camera app is available");
        startActivityForResult(camera,REQ_BILL_CAMERA);
      }catch(Exception e){Toast.makeText(this,"Could not open camera: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    void chooseBillFile(){
      Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("*/*");
      pick.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/jpeg","image/png","image/webp","image/heic","image/heif","image/heic-sequence","image/heif-sequence","application/pdf"});
      startActivityForResult(pick,REQ_BILL_SCAN);
    }
    android.graphics.Bitmap enhanceReceiptBitmap(android.graphics.Bitmap source){
      int maxSide=Math.max(source.getWidth(),source.getHeight());
      float scale=Math.min(2.0f,3600.0f/Math.max(1,maxSide));
      int w=Math.max(1,Math.round(source.getWidth()*scale)),h=Math.max(1,Math.round(source.getHeight()*scale));
      android.graphics.Bitmap scaled=android.graphics.Bitmap.createScaledBitmap(source,w,h,true);
      int[] pixels=new int[w*h];scaled.getPixels(pixels,0,w,0,0,w,h);
      for(int i=0;i<pixels.length;i++){
        int c=pixels[i],r=(c>>16)&255,g=(c>>8)&255,b=c&255;
        double gray=0.299*r+0.587*g+0.114*b;
        int v=(int)Math.max(0,Math.min(255,(gray-128.0)*1.75+142.0));
        pixels[i]=android.graphics.Color.rgb(v,v,v);
      }
      android.graphics.Bitmap enhanced=android.graphics.Bitmap.createBitmap(w,h,android.graphics.Bitmap.Config.ARGB_8888);
      enhanced.setPixels(pixels,0,w,0,0,w,h);
      if(scaled!=source)scaled.recycle();
      return enhanced;
    }
    com.google.mlkit.vision.text.Text recognizeReceipt(android.graphics.Bitmap bitmap)throws Exception{
      com.google.mlkit.vision.text.TextRecognizer recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
      com.google.mlkit.vision.text.Text original=Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap,0)));
      android.graphics.Bitmap enhanced=enhanceReceiptBitmap(bitmap);
      try{
        com.google.mlkit.vision.text.Text improved=Tasks.await(recognizer.process(InputImage.fromBitmap(enhanced,0)));
        String a=original.getText()==null?"":original.getText(),b=improved.getText()==null?"":improved.getText();
        int scoreA=a.replaceAll("[^A-Za-z0-9]","").length(),scoreB=b.replaceAll("[^A-Za-z0-9]","").length();
        return scoreB>scoreA?improved:original;
      }finally{enhanced.recycle();}
    }
    String billOcrText(com.google.mlkit.vision.text.Text result){
      ArrayList<com.google.mlkit.vision.text.Text.Line> lines=new ArrayList<>();
      for(com.google.mlkit.vision.text.Text.TextBlock block:result.getTextBlocks())lines.addAll(block.getLines());
      Collections.sort(lines,(a,b)->{android.graphics.Rect x=a.getBoundingBox(),y=b.getBoundingBox();int cy=x==null?0:(x.top+x.bottom)/2,dy=y==null?0:(y.top+y.bottom)/2;return cy==dy?(x==null?0:x.left)-(y==null?0:y.left):cy-dy;});
      ArrayList<ArrayList<com.google.mlkit.vision.text.Text.Line>> rows=new ArrayList<>();
      for(com.google.mlkit.vision.text.Text.Line line:lines){
        android.graphics.Rect r=line.getBoundingBox();int cy=r==null?0:(r.top+r.bottom)/2;ArrayList<com.google.mlkit.vision.text.Text.Line> target=null;
        for(ArrayList<com.google.mlkit.vision.text.Text.Line> row:rows){android.graphics.Rect first=row.get(0).getBoundingBox();int ry=first==null?0:(first.top+first.bottom)/2;int tolerance=Math.max(18,first==null?18:first.height());if(Math.abs(cy-ry)<=tolerance){target=row;break;}}
        if(target==null){target=new ArrayList<>();rows.add(target);}target.add(line);
      }
      StringBuilder out=new StringBuilder();
      for(ArrayList<com.google.mlkit.vision.text.Text.Line> row:rows){
        Collections.sort(row,(a,b)->{android.graphics.Rect x=a.getBoundingBox(),y=b.getBoundingBox();return (x==null?0:x.left)-(y==null?0:y.left);});
        StringBuilder lineText=new StringBuilder();for(com.google.mlkit.vision.text.Text.Line line:row){if(lineText.length()>0)lineText.append(" ");lineText.append(line.getText());}
        if(lineText.length()>0)out.append(lineText).append("\n");
      }
      return out.toString().trim();
    }
    void processBillUri(android.net.Uri uri){
      Toast.makeText(this,"Reading bill…",Toast.LENGTH_SHORT).show();
      Executors.newSingleThreadExecutor().execute(()->{
        try{
          String mime=getContentResolver().getType(uri);StringBuilder allText=new StringBuilder();
          if("application/pdf".equalsIgnoreCase(mime)||uri.toString().toLowerCase(Locale.US).endsWith(".pdf")){
            android.os.ParcelFileDescriptor fd=getContentResolver().openFileDescriptor(uri,"r");if(fd==null)throw new IOException("Cannot open this PDF invoice");
            android.graphics.pdf.PdfRenderer renderer=new android.graphics.pdf.PdfRenderer(fd);
            try{for(int pageNo=0;pageNo<renderer.getPageCount();pageNo++){
              android.graphics.pdf.PdfRenderer.Page page=renderer.openPage(pageNo);android.graphics.Bitmap bm=android.graphics.Bitmap.createBitmap(page.getWidth()*2,page.getHeight()*2,android.graphics.Bitmap.Config.ARGB_8888);
              try{page.render(bm,null,null,android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);allText.append("\n").append(billOcrText(recognizeReceipt(bm))).append("\n");}
              finally{bm.recycle();page.close();}
            }}finally{renderer.close();fd.close();}
          }else{
            android.graphics.Bitmap bitmap;
            if(Build.VERSION.SDK_INT>=28){android.graphics.ImageDecoder.Source source=android.graphics.ImageDecoder.createSource(getContentResolver(),uri);bitmap=android.graphics.ImageDecoder.decodeBitmap(source);}
            else{java.io.InputStream stream=getContentResolver().openInputStream(uri);bitmap=android.graphics.BitmapFactory.decodeStream(stream);if(stream!=null)stream.close();}
            if(bitmap==null)throw new IOException("Unsupported image format. Please choose a JPG or PNG copy.");
            try{allText.append(billOcrText(recognizeReceipt(bitmap)));}finally{bitmap.recycle();}
          }
          String raw=allText.toString().trim();if(raw.isEmpty())throw new IOException("No readable text found. Try a sharper, well-lit photo or a text-based PDF.");
          runOnUiThread(()->reviewBillText(raw));
        }catch(Exception e){runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Could not read bill").setMessage("Try a clearer, well-lit photo or PDF. "+e.getMessage()).setPositiveButton("OK",null).show());}
      });
    }
    String canonicalBillItem(String rawName){
      String n=rawName.toLowerCase(Locale.US).replaceAll("[^a-z0-9 ]"," ").replaceAll("\\s+"," ").trim();
      if(n.contains("carrot"))return "Carrot";
      if(n.contains("nandini")&&n.contains("milk"))return "Milk";
      if(n.equals("milk")||n.contains("standard milk"))return "Milk";
      if(n.contains("chana")&&n.contains("dal"))return "Chana dal";
      if(n.contains("coconut"))return "Coconut";
      if(n.contains("pineapple"))return "Pineapple";
      return rawName.trim().replaceAll("\\s+"," ");
    }
    String billUnit(String name,String rawName){
      String n=(name+" "+rawName).toLowerCase(Locale.US);
      if(n.contains("milk"))return "500 ml";
      if(n.contains("carrot")||n.contains("chana dal")||n.contains("pineapple")||n.contains("coconut"))return "kg";
      return "item";
    }
    boolean billNumber(String value){
      return value!=null&&value.replace("₹","").replace(",","").matches("[0-9]+(?:\\.[0-9]{1,3})?");
    }
    String billCleanNumber(String value){return value.replace("₹","").replace(",","").trim();}
    String[] parseBillRow(String source){
      String line=source.trim().replaceAll("\\s+"," ");if(line.isEmpty())return null;
      ArrayList<String> t=new ArrayList<>(Arrays.asList(line.split(" ")));
      if(t.size()>1&&t.get(0).matches("\\d{1,2}"))t.remove(0);
      if(t.size()<4)return null;
      String total=t.get(t.size()-1).replaceAll("^[^0-9]*","").replaceAll("[^0-9.].*$","");
      if(!total.matches("[0-9,]+\\.[0-9]{2}"))return null;
      t.remove(t.size()-1);
      if(!t.isEmpty()&&t.get(t.size()-1).matches("[0-9]+(?:\\.[0-9]+)?%"))t.remove(t.size()-1);
      if(t.size()<3)return null;
      String unitPrice=t.get(t.size()-1);if(!unitPrice.matches("[0-9,]+\\.[0-9]{2}"))return null;t.remove(t.size()-1);
      String quantity=t.get(t.size()-1);if(!billNumber(quantity))return null;t.remove(t.size()-1);
      if(!t.isEmpty()&&t.get(t.size()-1).matches("[0-9,]+\\.[0-9]{2}"))t.remove(t.size()-1);
      String name=String.join(" ",t).trim();if(name.isEmpty())return null;
      return new String[]{name,billCleanNumber(quantity),billCleanNumber(total)};
    }
    String prepareBillRows(String raw){
      StringBuilder rows=new StringBuilder();String pending="";
      for(String source:raw.split("\\r?\\n")){
        String line=source.trim().replaceAll("\\s+"," ");if(line.isEmpty())continue;
        String lower=line.toLowerCase(Locale.US);
        if(lower.startsWith("hsn")||lower.contains("invoice no")||lower.contains("order no")||lower.contains("gstin")||lower.contains("phone no")||
          lower.contains("site no")||lower.contains("taxable")||lower.startsWith("cgst")||lower.startsWith("sgst")||lower.startsWith("subtotal")||
          lower.equals("total")||lower.startsWith("thank you")||lower.startsWith("s.no")||lower.startsWith("items")||lower.startsWith("tax %")||
          lower.startsWith("operator")||lower.startsWith("payment method")||lower.contains("kpn fresh")||lower.contains("tax invoice"))continue;
        String candidate=(pending+" "+line).trim();String[] parsed=parseBillRow(candidate);
        if(parsed!=null){
          String name=canonicalBillItem(parsed[0]);double qty=num(parsed[1]),total=num(parsed[2]);
          if(!name.isEmpty()&&qty>0&&total>=0)rows.append(name).append(" | ").append(parsed[1]).append(" | ").append(billUnit(name,parsed[0])).append(" | ").append(String.format(Locale.US,"%.2f",total)).append("\\n");
          pending="";continue;
        }
        if(!line.matches("^[0-9\\s.,%₹]+$")&&!lower.matches(".*\\b\\d{6,}\\b.*")&&
          !lower.startsWith("invoice")&&!lower.startsWith("order")&&!lower.startsWith("phone")&&!lower.startsWith("gstin"))pending=candidate;
      }
      return rows.toString().trim();
    }
    void reviewBillText(String raw){
      String prepared=prepareBillRows(raw);
      LinearLayout box=col();box.setPadding(12,4,12,4);
      box.addView(text(prepared.isEmpty()?"Receipt items could not be separated automatically. Enter each item as: Item | Quantity | Unit | Line total.":"Items detected from the receipt. Check every quantity and printed line total before saving.",13,muted,false));
      EditText edit=new EditText(this);edit.setGravity(Gravity.TOP);edit.setMinLines(10);edit.setTextSize(14);
      edit.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
      edit.setText(prepared.isEmpty()?raw:prepared);box.addView(edit);ScrollView sc=new ScrollView(this);sc.addView(box);
      new AlertDialog.Builder(this).setTitle("Review scanned bill").setView(sc).setNegativeButton("Cancel",null).
        setPositiveButton("Save purchases",(d,w)->saveBillLines(edit.getText().toString())).show();
    }
    void saveBillLines(String content){
      String normalized=content.contains("|")?content:prepareBillRows(content);
      int saved=0,skipped=0;SharedPreferences.Editor editor=prefs.edit();
      for(String line:normalized.split("\\r?\\n")){
        String[] p=line.split("\\|",-1);
        if(p.length<4){if(!line.trim().isEmpty())skipped++;continue;}
        String original=p[0].trim(),name=canonicalBillItem(original),unit=p[2].trim();
        double qty=num(p[1].trim().replace(",","")),total=num(p[3].trim().replace(",",""));
        if(name.isEmpty()||unit.isEmpty()||qty<=0||p[3].trim().isEmpty()||total<0){skipped++;continue;}
        String id=key(name);JSONArray history=purchases(id);JSONObject purchase=new JSONObject();
        try{
          purchase.put("date",new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date()));
          purchase.put("qty",qty);purchase.put("unit",unit);purchase.put("total",total);
          purchase.put("unitPrice",total/qty);purchase.put("source","bill_scan");purchase.put("billItemName",original);
          history.put(purchase);
          editor.putString("purchases_"+id,history.toString()).putBoolean("have_"+id,true).putString("stock_"+id,"available");
          saved++;
        }catch(Exception e){skipped++;}
      }
      if(saved>0)editor.apply();
      String message=saved+" item(s) saved to purchase history, price history and stock.";
      if(skipped>0)message+=" "+skipped+" line(s) skipped; check the item format.";
      if(saved==0)message="0 items saved. No valid item rows found. Use: Item | Quantity | Unit | Line total.";
      Toast.makeText(this,message,Toast.LENGTH_LONG).show();draw();
    }
void addCustomIngredient(){
      dialogFields("Add grocery item",new String[]{"Ingredient name"},new String[]{""},v->{if(!v[0].trim().isEmpty()){String name=v[0].trim();prefs.edit().putString("custom_item_"+key(name),name).apply();Toast.makeText(this,"Added. You can record a purchase from the shopping checklist.",Toast.LENGTH_SHORT).show();draw();}});
    }

    void createNotificationChannel(){
      if(Build.VERSION.SDK_INT>=26){
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel ch=new NotificationChannel("ooru_meals_reminders","Ooru Meals reminders",NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("NSV morning, evening and low-stock reminders");ch.enableVibration(true);ch.setVibrationPattern(new long[]{0,300,150,300});
        ch.setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION),new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
        nm.createNotificationChannel(ch);
      }
    }
    void addMonthlyReportControls(){
      LinearLayout c=card("Monthly budget notifications","Month-end: full closing statement. Month-start: previous month summary and new month budget.");
      CheckBox sw=new CheckBox(this);sw.setText("Enable monthly budget notifications");sw.setChecked(prefs.getBoolean("monthly_reports_enabled",true));c.addView(sw);
      sw.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean("monthly_reports_enabled",on).apply();scheduleAllReminders();});
      int eh=prefs.getInt("month_end_hour",20),em=prefs.getInt("month_end_min",0);
      Button end=new Button(this);end.setAllCaps(false);end.setText(String.format(Locale.US,"Month-end report time: %02d:%02d",eh,em));c.addView(end);
      end.setOnClickListener(v->new TimePickerDialog(this,(tp,h,m)->{prefs.edit().putInt("month_end_hour",h).putInt("month_end_min",m).apply();scheduleAllReminders();draw();},eh,em,false).show());
      int sh=prefs.getInt("month_start_hour",8),sm=prefs.getInt("month_start_min",30);
      Button start=new Button(this);start.setAllCaps(false);start.setText(String.format(Locale.US,"Month-start report time: %02d:%02d",sh,sm));c.addView(start);
      start.setOnClickListener(v->new TimePickerDialog(this,(tp,h,m)->{prefs.edit().putInt("month_start_hour",h).putInt("month_start_min",m).apply();scheduleAllReminders();draw();},sh,sm,false).show());
    }
    void scheduleMonthlyReports(){ReminderReceiver.scheduleNext(this,"month_end");ReminderReceiver.scheduleNext(this,"month_start");}
    void scheduleAllReminders(){scheduleReminder(true);scheduleReminder(false);scheduleMonthlyReports();}
    void scheduleReminder(boolean morning){
      String prefix=morning?"morning":"evening";int id=morning?100:101;
      Intent intent=new Intent(this,ReminderReceiver.class);intent.putExtra("slot",morning?"morning":"evening");
      PendingIntent pi=PendingIntent.getBroadcast(this,id,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
      AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
      if(!prefs.getBoolean(prefix+"_enabled",true)){am.cancel(pi);return;}
      Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,prefs.getInt(prefix+"_hour",morning?8:19));c.set(Calendar.MINUTE,prefs.getInt(prefix+"_min",0));c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);
      if(c.getTimeInMillis()<=System.currentTimeMillis())c.add(Calendar.DATE,1);
      if(Build.VERSION.SDK_INT>=31 && am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);
      else if(Build.VERSION.SDK_INT>=23)am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);
      else am.set(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);
    }
    @Override protected void onResume(){super.onResume();if(prefs!=null&&body!=null){loadSavedMeals();draw();}}
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
      super.onRequestPermissionsResult(requestCode,permissions,grantResults);
      if(requestCode==10){if(grantResults.length>0&&grantResults[0]==0){Toast.makeText(this,"Notifications allowed. Morning and evening reminders are enabled.",Toast.LENGTH_LONG).show();}else{Toast.makeText(this,"Please allow notifications in Android Settings for reminders to appear.",Toast.LENGTH_LONG).show();}}
    }
}