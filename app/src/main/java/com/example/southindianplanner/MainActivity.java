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
import org.json.*;

public class MainActivity extends Activity {
    int page=0;
    LinearLayout root, body, nav;
    TextView header;
    SharedPreferences prefs;
    final int green=Color.rgb(35,115,76), ink=Color.rgb(35,45,39), muted=Color.rgb(105,115,108), pale=Color.rgb(250,252,249);
    interface SaveFields { void save(String[] values); }

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
      root.addView(text("🥥 OORU MEALS  •  2 ADULTS",17,green,true));
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
      button("Edit today's ingredient list",()->editDayIngredients(day));
      for(String item:list){
        final String id=key(item);
        LinearLayout c=card(item,item.equals("Milk")?"Daily requirement: "+prefs.getString("milk_qty","500")+" ml\n"+priceSummary(id):priceSummary(id));
        CheckBox have=new CheckBox(this);have.setText("Already have at home — don't buy");have.setChecked(prefs.getBoolean("have_"+id,false));c.addView(have);
        have.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean("have_"+id,on).apply());
        Button buy=new Button(this);buy.setText("I bought this • Enter quantity & price");buy.setAllCaps(false);c.addView(buy);buy.setOnClickListener(v->recordPurchase(item));
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
      if(m.contains("rice")||m.contains("pulao")||m.contains("bisibele")||m.contains("poha"))add(s,"Rice");
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

    void editDayIngredients(int day){
      String current=prefs.getString("day_ingredients_"+day,joinItems(ingredientsForDayDefault(day)));
      dialogFields("Edit Day "+(day+1)+" ingredients",new String[]{"Ingredients (separate with commas)"},new String[]{current},v->{
        prefs.edit().putString("day_ingredients_"+day,v[0]).apply();draw();
      });
    }
    LinkedHashSet<String> ingredientsForDayDefault(int d){
      String old=prefs.getString("day_ingredients_"+d,null);
      if(old!=null){LinkedHashSet<String>x=new LinkedHashSet<>();for(String a:old.split(","))x.add(a.trim());return x;}
      LinkedHashSet<String> x=new LinkedHashSet<>();for(String m:meals[d])mapDishIngredients(m,x);x.add("Milk (daily 500 ml)");return x;
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
      button(prefs.contains("budget")?"Change monthly budget":"Set budget after reviewing spending",()->dialogFields("Monthly grocery budget",new String[]{"Monthly budget (₹)"},new String[]{prefs.getString("budget","")},v->{if(v[0].isEmpty())prefs.edit().remove("budget").apply();else if(num(v[0])>0)prefs.edit().putString("budget",v[0]).apply();draw();}));
      card("MORNING NOTIFICATION","Today's breakfast, lunch and dinner. Separate time and enable switch.");
      addReminderControls(true);
      card("EVENING NOTIFICATION","Tomorrow's breakfast, lunch, dinner and reminder to check the combined ingredient list.");
      addReminderControls(false);
      button("Check Android notification permission",()->{if(Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},10);else Toast.makeText(this,"Notification permission is managed by Android settings.",Toast.LENGTH_LONG).show();});
      if(Build.VERSION.SDK_INT>=31)button("Allow exact-time reminders in Android",()->{try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));}catch(Exception e){startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName())));}});
      card("FOOD PLAN RULES FROM YOUR PDF","No upma, pongal, appam, idiyappam or evening snacks. Poha and variety rice included. Poori and chapati on weekends only. Some lunch dishes are repeated for dinner to reduce cooking.");
      card("LEFTOVER FOOD SAFETY","Refrigerate cooked rice and curries promptly (within 1 hour in hot weather). Do not leave cooked rice at room temperature until dinner. Reheat thoroughly before serving.");
      card("DATA","Your menu edits, purchases, quantities, prices and settings are saved on this phone and remain after closing the app.");
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
          prefs.edit().putString("purchases_"+id,a.toString()).putBoolean("have_"+id,true).apply();
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
    void showMonthlyHistory(){new AlertDialog.Builder(this).setTitle("Monthly grocery spending").setMessage(monthHistorySummary()).setPositiveButton("Close",null).show();}
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
    void addCustomIngredient(){
      dialogFields("Add grocery item",new String[]{"Ingredient name"},new String[]{""},v->{if(!v[0].trim().isEmpty()){String name=v[0].trim();prefs.edit().putString("custom_item_"+key(name),name).apply();Toast.makeText(this,"Added. You can record a purchase from the shopping checklist.",Toast.LENGTH_SHORT).show();draw();}});
    }

    void createNotificationChannel(){
      if(Build.VERSION.SDK_INT>=26){
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel ch=new NotificationChannel("ooru_meals_reminders","Ooru Meals reminders",NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Morning and evening meal reminders");ch.enableVibration(true);ch.setVibrationPattern(new long[]{0,300,150,300});
        ch.setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION),new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
        nm.createNotificationChannel(ch);
      }
    }
    void scheduleAllReminders(){scheduleReminder(true);scheduleReminder(false);}
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