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
      {"Idli + coconut chutney","Rice + drumstick sambar + keerai poriyal + rasam","Onion dosa + tomato chutney"},
      {"Ghee podi dosa","Rice + pumpkin sambar + carrot beans poriyal","Vegetable upma + coconut chutney"},
      {"Ven pongal + gothsu","Rice + carrot beans sambar + cabbage poriyal + egg","Ragi dosa + mint chutney"},
      {"Vegetable poha","Rice + brinjal kara kuzhambu + beetroot poriyal","Rava idli + sambar"},
      {"Rava idli + sambar","Rice + potato gravy + beans poriyal","Tomato dosa + chutney"},
      {"Masala dosa","Rice + ladies finger sambar + carrot poriyal + egg","Onion dosa + chutney"},
      {"Thinai pongal","Rice + mixed vegetable sambar + vazhakkai poriyal","Idli + sambar"},
      {"Onion rava dosa","Rice + drumstick gravy + cabbage poriyal","Idli + podi"},
      {"Vegetable upma","Rice + pumpkin sambar + beans carrot poriyal + egg","Kothu dosa + vegetable salna"},
      {"Kanchipuram idli","Rice + tomato garlic kuzhambu + spinach poriyal","Set dosa + vegetable kurma"},
      {"Poha with peas","Rice + radish sambar + potato roast","Egg dosa + chutney"},
      {"Kal dosa + vadacurry","Rice + brinjal sambar + carrot beans poriyal + egg","Lemon sevai"},
      {"Ven pongal + gothsu","Rice + potato peas gravy + cabbage poriyal","Wheat dosa + onion chutney"},
      {"Mysore masala dosa","Rice + chow chow sambar + beetroot poriyal","Wheat dosa + onion chutney"},
      {"Idli + podi","Rice + drumstick sambar + keerai kootu + egg","Ragi idli + sambar"},
      {"Aval upma","Rice + onion tomato kara kuzhambu + beans poriyal + curd","Ragi dosa + chutney"},
      {"Onion dosa","Rice + pumpkin sambar + potato fry","Dosa + egg omelette"},
      {"Set dosa + vegetable sagu","Rice + carrot beans sambar + cabbage poriyal + egg","Mixed vegetable dosa"},
      {"Rava kichadi","Rice + vendakkai puli kuzhambu + keerai poriyal","Tomato dosa + mint chutney"},
      {"Onion dosa","Rice + drumstick potato gravy + carrot poriyal","Idli + coconut chutney"},
      {"Kambu dosa","Rice + brinjal sambar + beans poriyal + egg","Channa sundal + dosa"},
      {"Mini idli + sambar","Rice + pumpkin sambar + cabbage poriyal","Rava dosa + tomato chutney"},
      {"Lemon poha","Rice + kathirikai kara kuzhambu + beetroot poriyal + egg","Dosa + tomato chutney"},
      {"Ghee dosa","Rice + mixed vegetable kootu + carrot beans poriyal","Potato masala dosa"},
      {"Ragi idli","Rice + drumstick sambar + vazhakkai poriyal","Egg dosa + onion chutney"},
      {"Vegetable pongal","Rice + capsicum potato gravy + spinach poriyal + egg","Dosa + chutney"},
      {"Masala paniyaram","Rice + radish sambar + carrot poriyal","Dosa + sambar"},
      {"Wheat dosa","Rice + pumpkin sambar + beans cabbage poriyal + egg","Ragi idli + chutney"},
      {"Poha with carrots and peas","Rice + drumstick kara kuzhambu + beetroot poriyal","Idli + podi"},
      {"Paper dosa + sambar","Rice + vegetable kurma + cabbage poriyal + egg","Onion dosa + tomato gravy"},
      {"Kuthiraivali pongal","Rice + brinjal drumstick sambar + carrot beans poriyal","Onion dosa + egg omelette"}
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
      if(!prefs.getBoolean("meal_plan_rules_v2",false)){SharedPreferences.Editor reset=prefs.edit();for(int d=0;d<31;d++)for(int i=0;i<3;i++)reset.remove("meal_"+d+"_"+i);reset.putBoolean("meal_plan_rules_v2",true).apply();}
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
      return (int)(diff%planDays());
    }
    int planDays(){Calendar c=Calendar.getInstance();return c.getActualMaximum(Calendar.DAY_OF_MONTH);}
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
    boolean isWeekend(int d){Calendar c=Calendar.getInstance();c.set(Calendar.DAY_OF_MONTH,1);c.add(Calendar.DATE,d);int w=c.get(Calendar.DAY_OF_WEEK);return w==Calendar.SATURDAY||w==Calendar.SUNDAY;}
    String mealAt(int d,int slot){String value=meals[d][slot];if(isWeekend(d)){if(slot==0 && d%2==0)value="Poori + potato masala";if(slot==2 && d%2==1)value="Chapati + vegetable kurma";}return value;}
    String mealText(int d){return "Breakfast: "+mealAt(d,0)+"\nLunch: "+mealAt(d,1)+"\nDinner: "+mealAt(d,2);}
    void loadSavedMeals(){for(int d=0;d<meals.length;d++)for(int i=0;i<3;i++){String x=prefs.getString("meal_"+d+"_"+i,null);if(x!=null && !x.toLowerCase(Locale.US).matches(".*(idiyappam|idiappam|pesarattu|uthappam|adai|avial|appam).*"))meals[d][i]=x;}}
    void saveMeals(){SharedPreferences.Editor e=prefs.edit();for(int d=0;d<meals.length;d++)for(int i=0;i<3;i++)e.putString("meal_"+d+"_"+i,meals[d][i]);e.apply();}
    void draw(){
      body.removeAllViews();String[] titles={"Your meal plan","Monthly calendar","Today's shopping list","Meal recipes","Settings"};header.setText(titles[page]);
      for(int i=0;i<5;i++)((TextView)nav.getChildAt(i)).setTextColor(i==page?green:muted);
      Calendar now=Calendar.getInstance();int today=dayFor(now);
      if(page==0){
        String todayText=new SimpleDateFormat("EEEE, dd MMMM yyyy",Locale.getDefault()).format(now.getTime());
        card(todayText,"TODAY  •  DAY "+(today+1)+" OF "+planDays()+"\n"+mealText(today));
        int next=(today+1)%planDays();card("TOMORROW  •  DAY "+(next+1),mealText(next));
        if(!reuse[today].isEmpty())card("COOK ONCE, SAVE TIME",reuse[today]);
        double spent=monthSpend();String budgetText=prefs.contains("budget")?"Budget: "+money(num(prefs.getString("budget","0")))+"\nRemaining: "+money(num(prefs.getString("budget","0"))-spent):"Budget not set yet — tracking actual spending for the first month.";
        card("THIS MONTH'S GROCERY SPENDING","Actual purchases: "+money(spent)+"\n"+budgetText);
        button("See today's combined ingredients",()->{page=2;draw();});
        button("Open monthly calendar",()->{page=1;draw();});
      }
      if(page==1){
        card("MONTHLY FOOD PLAN","Calendar follows your phone date and selected start date. Edit any day's breakfast, lunch or dinner.");
        for(int i=0;i<planDays();i++){
          final int n=i;LinearLayout c=card("DAY "+(i+1)+(i==today?"  •  TODAY":""),mealText(i));
          if(i==today)c.setBackground(shape(Color.rgb(232,246,237)));
          Button e=new Button(this);e.setText("Edit this day's menu");e.setAllCaps(false);c.addView(e);e.setOnClickListener(v->editMeal(n));
        }
      }
      if(page==2)drawShopping(today);
      if(page==3){
        card("MEAL & INGREDIENT GUIDE","The PDF gives the 30-day menu and leftover notes, not measured recipes. Ingredient suggestions below are editable; no grocery quantities or prices are assumed.");
        for(int i=0;i<planDays();i++){final int n=i;LinearLayout c=card("DAY "+(i+1),mealText(i));Button e=new Button(this);e.setText("Edit meals");e.setAllCaps(false);c.addView(e);e.setOnClickListener(v->editMeal(n));}
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
        for(int i=0;i<3;i++)mapDishIngredients(mealAt(d,i),out);
      }
      out.add("Milk");
      for(Map.Entry<String,?> en:prefs.getAll().entrySet())if(en.getKey().startsWith("custom_item_"))out.add(String.valueOf(en.getValue()));
      return out;
    }
    void add(LinkedHashSet<String> s,String... values){for(String v:values)s.add(v);}
    void mapDishIngredients(String meal,LinkedHashSet<String> s){
      String m=meal.toLowerCase(Locale.US);
      // Leftovers are intentionally mapped once as the same cooked dish.
      if(m.contains("same ")){
        if(m.contains("lemon"))add(s,"Rice","Lemon","Peanuts","Curry leaves","Mustard seeds","Urad dal","Chana dal","Turmeric powder");
        else if(m.contains("rasam"))add(s,"Rice","Tomato","Tamarind","Rasam powder","Cumin seeds","Pepper","Curry leaves");
        else if(m.contains("mor kuzhambu"))add(s,"Rice","Curd","Coconut","Green chilli","Cumin seeds","Turmeric powder");
        else if(m.contains("sambar"))add(s,"Rice","Toor dal","Sambar powder","Tamarind","Onion","Tomato","Mixed vegetables");
        else add(s,"Rice","Onion","Tomato","Cooking oil","Salt");
        return;
      }

      // Staples and dish bases.
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

      // Named vegetables: add only vegetables actually named in the selected meal.
      if(m.contains("drumstick")||m.contains("murungakkai"))add(s,"Drumstick (Murungakkai)","Onion","Tomato","Garlic","Tamarind","Chilli powder","Coriander powder","Turmeric powder","Mustard seeds","Fenugreek seeds","Curry leaves","Cooking oil");
      if(m.contains("poriyal")){
        if(m.contains("carrot"))add(s,"Carrot");
        if(m.contains("beans"))add(s,"Beans");
        if(m.contains("cabbage"))add(s,"Cabbage");
        if(m.contains("beetroot"))add(s,"Beetroot");
        if(m.contains("potato"))add(s,"Potato");
        add(s,"Coconut","Mustard seeds","Urad dal","Curry leaves");
      }
      if(m.contains("carrot"))add(s,"Carrot");
      if(m.contains("beans"))add(s,"Beans");
      if(m.contains("cabbage"))add(s,"Cabbage");
      if(m.contains("beetroot"))add(s,"Beetroot");
      if(m.contains("brinjal")||m.contains("eggplant"))add(s,"Brinjal");
      if(m.contains("okra")||m.contains("ladies finger")||m.contains("vendakkai"))add(s,"Okra (Ladies finger)");
      if(m.contains("cauliflower"))add(s,"Cauliflower");
      if(m.contains("broccoli"))add(s,"Broccoli");
      if(m.contains("capsicum")||m.contains("bell pepper"))add(s,"Capsicum");
      if(m.contains("bottle gourd")||m.contains("sorakkai"))add(s,"Bottle gourd");
      if(m.contains("ridge gourd")||m.contains("peerkangai"))add(s,"Ridge gourd");
      if(m.contains("pumpkin")||m.contains("poosanikai"))add(s,"Pumpkin");
      if(m.contains("radish")||m.contains("mullangi"))add(s,"Radish");
      if(m.contains("cucumber")||m.contains("vellarikai"))add(s,"Cucumber");
      if(m.contains("sweet corn"))add(s,"Sweet corn");
      if(m.contains("potato"))add(s,"Potato","Onion","Green chilli","Turmeric powder");
      if(m.contains("avial"))add(s,"Mixed vegetables","Coconut","Curd","Cumin seeds","Green chilli","Curry leaves");

      // Breakfast, sides and protein.
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

      // Fruit is listed only when the selected meal explicitly includes it.
      if(m.contains("banana"))add(s,"Banana");
      if(m.contains("apple"))add(s,"Apple");
      if(m.contains("orange"))add(s,"Orange");
      if(m.contains("mango"))add(s,"Mango");
      if(m.contains("pineapple"))add(s,"Pineapple");
      if(m.contains("papaya"))add(s,"Papaya");
      if(m.contains("guava"))add(s,"Guava");
      if(m.contains("grape"))add(s,"Grapes");
      if(m.contains("watermelon"))add(s,"Watermelon");
      if(m.contains("fruit salad"))add(s,"Banana","Apple","Orange","Papaya","Grapes");

      // Basic cooking essentials for a cooked meal.
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
      LinkedHashSet<String> x=new LinkedHashSet<>();for(int i=0;i<3;i++)mapDishIngredients(mealAt(d,i),x);x.add("Milk");return x;
    }

    void editMeal(int d){
      dialogFields("Edit Day "+(d+1),new String[]{"Breakfast","Lunch","Dinner"},new String[]{mealAt(d,0),mealAt(d,1),mealAt(d,2)},a->{
        for(int i=0;i<3;i++)meals[d][i]=a[i];saveMeals();
        prefs.edit().remove("day_ingredients_"+d).apply();draw();
      });
    }
    void drawSettings(){
      card("PLAN START DATE","Current start date: "+date(prefs.getLong("cycle_start",System.currentTimeMillis()))+"\nThe calendar uses the real phone date. Set the start date to the first day of your plan.");
      button("Change monthly plan start date",()->pickStartDate());
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
      LinearLayout list=col();list.setPadding(12,8,12,8);int count=0;
      ArrayList<String> keys=new ArrayList<>();
      for(Map.Entry<String,?> en:prefs.getAll().entrySet())if(en.getKey().startsWith("purchases_"))keys.add(en.getKey());
      Collections.sort(keys);
      for(String prefKey:keys){
        try{
          JSONArray a=new JSONArray(prefs.getString(prefKey,"[]"));String item=prefKey.substring(10).replace('_',' ');
          for(int i=a.length()-1;i>=0;i--){
            final int index=i;final String purchaseKey=prefKey;JSONObject o=a.getJSONObject(i);
            LinearLayout row=col();row.setPadding(8,8,8,8);
            row.addView(text(item+" • "+o.optString("date")+"\\n"+o.optDouble("qty")+" "+o.optString("unit")+" • "+money(o.optDouble("total")),14,ink,true));
            Button remove=new Button(this);remove.setText("Remove purchase");remove.setAllCaps(false);row.addView(remove);
            remove.setOnClickListener(v->confirmRemovePurchase(purchaseKey,index,item));
            list.addView(row);View divider=new View(this);divider.setBackgroundColor(Color.LTGRAY);list.addView(divider);count++;
          }
        }catch(Exception ignored){}
      }
      if(count==0)list.addView(text("No purchases recorded yet.",14,muted,false));
      ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(list);
      new AlertDialog.Builder(this).setTitle("Purchase history • "+count+" entries").setView(scroll).setPositiveButton("Close",null).show();
    }
    void confirmRemovePurchase(String prefKey,int index,String item){
      new AlertDialog.Builder(this).setTitle("Remove purchase?")
        .setMessage("Delete this "+item+" purchase from history? Monthly spending and price history will be recalculated.")
        .setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->removePurchase(prefKey,index,item)).show();
    }
    void removePurchase(String prefKey,int index,String item){
      JSONArray old=purchases(prefKey.substring(10));JSONArray updated=new JSONArray();
      for(int i=0;i<old.length();i++)if(i!=index)updated.put(old.opt(i));
      SharedPreferences.Editor editor=prefs.edit().putString(prefKey,updated.toString());
      String id=prefKey.substring(10);
      if(updated.length()==0)editor.putBoolean("have_"+id,false).putString("stock_"+id,"finished");
      editor.apply();
      Toast.makeText(this,"Purchase removed. Spending totals updated.",Toast.LENGTH_SHORT).show();
      showHistory();draw();
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
      android.graphics.Bitmap software=null,scaled=null;
      try{
        // ImageDecoder may return Config.HARDWARE, which does not support getPixels().
        software=source.getConfig()==android.graphics.Bitmap.Config.HARDWARE
          ? source.copy(android.graphics.Bitmap.Config.ARGB_8888,false)
          : source.copy(android.graphics.Bitmap.Config.ARGB_8888,false);
        if(software==null)throw new IllegalStateException("Could not create a software bitmap for receipt OCR.");
        int maxSide=Math.max(software.getWidth(),software.getHeight());
        float scale=Math.min(2.0f,3600.0f/Math.max(1,maxSide));
        int w=Math.max(1,Math.round(software.getWidth()*scale)),h=Math.max(1,Math.round(software.getHeight()*scale));
        scaled=android.graphics.Bitmap.createScaledBitmap(software,w,h,true);
        int[] pixels=new int[w*h];scaled.getPixels(pixels,0,w,0,0,w,h);
        for(int i=0;i<pixels.length;i++){
          int c=pixels[i],r=(c>>16)&255,g=(c>>8)&255,b=c&255;
          double gray=0.299*r+0.587*g+0.114*b;
          int v=(int)Math.max(0,Math.min(255,(gray-128.0)*1.75+142.0));
          pixels[i]=android.graphics.Color.rgb(v,v,v);
        }
        android.graphics.Bitmap enhanced=android.graphics.Bitmap.createBitmap(w,h,android.graphics.Bitmap.Config.ARGB_8888);
        enhanced.setPixels(pixels,0,w,0,0,w,h);
        return enhanced;
      }finally{
        if(scaled!=null&&scaled!=software)scaled.recycle();
        if(software!=null)software.recycle();
      }
    }
    int receiptOcrScore(com.google.mlkit.vision.text.Text result){
      int mixed=0,alphaLines=0,lineCount=0,chars=0;
      for(com.google.mlkit.vision.text.Text.TextBlock block:result.getTextBlocks()){
        for(com.google.mlkit.vision.text.Text.Line line:block.getLines()){
          String value=line.getText()==null?"":line.getText();
          boolean alpha=value.matches("(?s).*\\p{L}.*");
          boolean digit=value.matches("(?s).*\\d.*");
          if(alpha)mixed+=digit?1:0;
          if(alpha)alphaLines++;
          lineCount++;
          chars+=value.replaceAll("[^A-Za-z0-9]","").length();
        }
      }
      // Correctly oriented receipt rows usually contain both a product name and its figures.
      return mixed*1000+alphaLines*35+Math.min(chars,1200)+Math.min(lineCount,40)*3;
    }
    com.google.mlkit.vision.text.Text recognizeReceipt(android.graphics.Bitmap bitmap)throws Exception{
      com.google.mlkit.vision.text.TextRecognizer recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
      com.google.mlkit.vision.text.Text best=null;int bestScore=Integer.MIN_VALUE;
      android.graphics.Bitmap enhanced=enhanceReceiptBitmap(bitmap);
      try{
        // Let ML Kit inspect all four orientations. A sideways receipt otherwise gets read
        // column-by-column, merging product names, HSN codes, quantities and prices.
        android.graphics.Bitmap[] sources=new android.graphics.Bitmap[]{bitmap,enhanced};
        for(android.graphics.Bitmap source:sources){
          for(int rotation:new int[]{0,90,180,270}){
            com.google.mlkit.vision.text.Text candidate=Tasks.await(recognizer.process(InputImage.fromBitmap(source,rotation)));
            int score=receiptOcrScore(candidate);
            if(score>bestScore){best=candidate;bestScore=score;}
          }
        }
        if(best==null)throw new IOException("No readable text found.");
        return best;
      }finally{
        enhanced.recycle();
        recognizer.close();
      }
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
    int spellingDistance(String a,String b){
      int[] prev=new int[b.length()+1],next=new int[b.length()+1];
      for(int j=0;j<=b.length();j++)prev[j]=j;
      for(int i=1;i<=a.length();i++){
        next[0]=i;
        for(int j=1;j<=b.length();j++){
          int cost=a.charAt(i-1)==b.charAt(j-1)?0:1;
          next[j]=Math.min(Math.min(next[j-1]+1,prev[j]+1),prev[j-1]+cost);
        }
        int[] swap=prev;prev=next;next=swap;
      }
      return prev[b.length()];
    }
    String canonicalBillItem(String rawName){
      String cleaned=rawName==null?"":rawName.trim().replaceAll("\\s+"," ");
      String n=cleaned.toLowerCase(Locale.US).replaceAll("[^a-z0-9 ]"," ").replaceAll("\\s+"," ").trim();
      String compact=n.replace(" ","");
      if(n.isEmpty())return "";
      // Known brand/product aliases and frequent OCR confusions.
      if(n.contains("nandini")||n.contains("nandin")||compact.contains("nandini")||n.contains("standard milk")||n.equals("milk")||n.contains("milk 500"))return "Milk";
      if(n.contains("carrot")||compact.contains("carot")||compact.contains("car0t"))return "Carrot";
      if(n.contains("chana")||n.contains("channa")||n.contains("dhal")&&n.contains("chan")||compact.contains("chanadal"))return "Chana dal";
      if(n.contains("coconut")||compact.contains("cocnut")||compact.contains("cocon ut"))return "Coconut";
      if(n.contains("pineapple")||n.contains("pine apple")||compact.contains("pineappl"))return "Pineapple";
      if(n.contains("beetroot")||compact.contains("beetrot"))return "Beetroot";
      if(n.contains("tomato")||compact.contains("tom ato")||compact.contains("tonato")||compact.contains("tonatolocal"))return "Tomato";
      if(n.contains("drumstick")||compact.contains("driumstick"))return "Drumstick (Murungakkai)";
      if(n.contains("potato")||compact.equals("poato")||compact.startsWith("poatolocal"))return "Potato";
      if(n.contains("coconut")||compact.contains("coz0nut")||compact.contains("cozonut")||compact.contains("cox0nut"))return "Coconut";
      if(n.contains("onion")||compact.contains("onoin"))return "Onion";
      // Match OCR text against the app's own grocery vocabulary across all meal plans and custom items.
      LinkedHashSet<String> vocabulary=new LinkedHashSet<>();
      for(int day=0;day<meals.length;day++)vocabulary.addAll(ingredientsForDay(day));
      add(vocabulary,"Rice","Wheat flour","Atta","Ragi flour","Millets","Poha","Potato","Sweet potato","Onion","Tomato","Carrot","Beans","Green beans","Drumstick (Murungakkai)","Brinjal","Okra (Ladies finger)","Cabbage","Cauliflower","Capsicum","Bottle gourd","Ridge gourd","Pumpkin","Radish","Cucumber","Beetroot","Spinach","Broccoli","Sweet corn","Green chilli","Coriander leaves","Mint leaves","Curry leaves","Coconut","Lemon","Banana","Apple","Orange","Mango","Pineapple","Papaya","Guava","Grapes","Watermelon","Pomegranate","Toor dal","Moong dal","Chana dal","Urad dal","Whole green gram","Peanuts","Roasted gram","Sesame seeds","Curd","Milk","Paneer","Eggs","Garlic","Ginger","Tamarind","Cooking oil","Ghee","Salt","Sugar","Jaggery","Tea","Coffee","Mustard seeds","Cumin seeds","Fenugreek seeds","Pepper","Turmeric powder","Chilli powder","Coriander powder","Garam masala","Rasam powder","Sambar powder","Bisibele bath masala");
      String best=null;int bestDistance=Integer.MAX_VALUE;
      for(String candidate:vocabulary){
        String c=candidate.toLowerCase(Locale.US).replaceAll("[^a-z0-9 ]"," ").replaceAll("\\s+"," ").trim();
        if(c.isEmpty())continue;
        int distance=spellingDistance(n,c);
        int allowed=c.length()>=9?2:(c.length()>=5?1:0);
        if(distance<=allowed&&distance<bestDistance){best=candidate;bestDistance=distance;}
      }
      if(best!=null)return best;
      return cleaned.substring(0,1).toUpperCase(Locale.US)+cleaned.substring(1);
    }
    String billSignature(String raw){
      String prepared=prepareBillRows(raw);
      String basis=(prepared.isEmpty()?raw:prepared).toLowerCase(Locale.US).replaceAll("[^a-z0-9]","");
      return Integer.toHexString(basis.hashCode())+"_"+basis.length();
    }
    String billUnit(String name,String rawName){
      String n=(name+" "+rawName).toLowerCase(Locale.US);
      if(n.contains("milk"))return "500 ml";
      if(n.matches("(?s).*\\b(pc|pcs|piece|pieces)\\b.*"))return "pcs";
      if(n.matches("(?s).*\\b(kg|kgs|kilo|kilos)\\b.*"))return "kg";
      if(n.contains("tomato")||n.contains("potato")||n.contains("coconut")||n.contains("carrot")||n.contains("onion")||n.contains("drumstick"))return "kg";
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
    boolean isLikelyBillItemStart(String line){
      if(line==null||!line.matches(".*[A-Za-z].*"))return false;
      java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?<![A-Za-z])\\d+[,.]\\d{1,3}(?![A-Za-z])").matcher(line);
      int count=0;while(m.find())count++;
      return count>=2;
    }
    String[] parseNumberedBillRow(String source){
      String[] parts=source.split("\\r?\\n");
      if(parts.length==0)return null;
      String first=parts[0].trim().replaceAll("\\s+"," ");
      first=first.replaceFirst("^\\d{1,2}\\s*","");
      // HSN is a product classification, not a quantity or price. Remove it before
      // collecting numeric columns; otherwise an 8-digit HSN is mistaken for quantity.
      first=first.replaceAll("(?i)\\bHSN\\s*[:#-]?\\s*\\d{4,12}\\b"," ");
      first=first.replaceAll("(?i)\\bHSN\\b"," ").replaceAll("\\s+"," ").trim();
      java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?<![A-Za-z])(?:₹\\s*)?([0-9][0-9,]*(?:[.,][0-9]{1,3})?)(\\s*%)?(?![A-Za-z])").matcher(first);
      ArrayList<Double> values=new ArrayList<>();
      int nameEnd=-1;
      while(m.find()){
        if(nameEnd<0)nameEnd=m.start();
        if(m.group(2)==null||m.group(2).trim().isEmpty()){
          try{values.add(Double.parseDouble(m.group(1).replace(",","").replace(',','.')));}catch(Exception ignored){}
        }
      }
      if(nameEnd<0||values.size()<3)return null;
      StringBuilder name=new StringBuilder(first.substring(0,nameEnd).trim());
      for(int i=1;i<parts.length;i++){
        String extra=parts[i].trim().replaceAll("\\s+"," ");
        String lower=extra.toLowerCase(Locale.US);
        if(extra.isEmpty()||lower.startsWith("hsn")||lower.matches(".*\\b\\d{6,}\\b.*"))continue;
        if(extra.matches(".*\\d+[,.]\\d{2}.*"))continue;
        if(extra.matches("^[A-Za-z][A-Za-z0-9 /().-]*$"))name.append(" ").append(extra);
      }
      double quantity=values.get(0),total=values.get(values.size()-1);
      // Some tills print the unit price before the quantity (milk: 32.00 1 32.00).
      if(quantity>1.0&&values.size()>2&&values.get(1)>0.0&&values.get(1)<=1.0){
        quantity=values.get(1);
      }
      if(quantity<=0||total<0||name.length()==0)return null;
      return new String[]{name.toString().trim(),String.format(Locale.US,"%.3f",quantity),String.format(Locale.US,"%.2f",total)};
    }
    String prepareBillRows(String raw){
      String normalized=(raw==null?"":raw).replace("\\n","\n").replace("\\r","\r").replace('\r','\n');
      StringBuilder rows=new StringBuilder();
      ArrayList<String> numbered=new ArrayList<>();
      StringBuilder current=new StringBuilder();
      for(String source:normalized.split("\\n")){
        String line=source.trim().replaceAll("\\s+"," ");if(line.isEmpty())continue;
        String lower=line.toLowerCase(Locale.US);
        if(lower.startsWith("hsn")||lower.contains("invoice no")||lower.contains("order no")||lower.contains("gstin")||lower.contains("phone no")||
          lower.contains("site no")||lower.contains("taxable")||lower.startsWith("cgst")||lower.startsWith("sgst")||lower.startsWith("subtotal")||
          lower.equals("total")||lower.startsWith("thank you")||lower.startsWith("s.no")||lower.startsWith("items")||lower.startsWith("tax %")||
          lower.startsWith("operator")||lower.startsWith("payment method")||lower.contains("kpn fresh")||lower.contains("tax invoice"))continue;
        if(line.matches("^\\d{1,2}\\s+.*")||isLikelyBillItemStart(line)){
          if(current.length()>0)numbered.add(current.toString());
          current.setLength(0);current.append(line);
        }else if(current.length()>0){
          current.append("\n").append(line);
        }
      }
      if(current.length()>0)numbered.add(current.toString());
      for(String row:numbered){
        String[] parsed=parseNumberedBillRow(row);
        if(parsed==null)continue;
        String name=canonicalBillItem(parsed[0]);double qty=num(parsed[1]),total=num(parsed[2]);
        if(!name.isEmpty()&&qty>0&&total>=0)rows.append(name).append(" | ").append(parsed[1]).append(" | ").append(billUnit(name,parsed[0])).append(" | ").append(String.format(Locale.US,"%.2f",total)).append("\n");
      }
      // Keep the legacy parser as a fallback for receipts without numbered item rows.
      if(rows.length()==0){
        String pending="";
        for(String source:normalized.split("\\n")){
          String line=source.trim().replaceAll("\\s+"," ");if(line.isEmpty())continue;
          String lower=line.toLowerCase(Locale.US);
          if(lower.startsWith("hsn")||lower.contains("invoice no")||lower.contains("order no")||lower.contains("gstin")||lower.startsWith("cgst")||
             lower.startsWith("sgst")||lower.startsWith("subtotal")||lower.equals("total")||lower.startsWith("thank you")||
             lower.startsWith("operator")||lower.startsWith("payment method"))continue;
          String candidate=(pending+" "+line).trim();String[] parsed=parseBillRow(candidate);
          if(parsed!=null){
            String name=canonicalBillItem(parsed[0]);double qty=num(parsed[1]),total=num(parsed[2]);
            if(!name.isEmpty()&&qty>0&&total>=0)rows.append(name).append(" | ").append(parsed[1]).append(" | ").append(billUnit(name,parsed[0])).append(" | ").append(String.format(Locale.US,"%.2f",total)).append("\n");
            pending="";continue;
          }
          if(!line.matches("^[0-9\\s.,%₹]+$")&&!lower.matches(".*\\b\\d{6,}\\b.*"))pending=candidate;
        }
      }
      return rows.toString().trim();
    }
    void reviewBillText(String raw){
      String prepared=prepareBillRows(raw);
      String signature=billSignature(raw);
      boolean duplicate=prefs.getBoolean("bill_scanned_"+signature,false);
      LinearLayout box=col();box.setPadding(12,4,12,4);
      String guidance=prepared.isEmpty()?"Receipt items could not be separated automatically. Enter each item as: Item | Quantity | Unit | Line total.":"Items detected. Amounts are cross-checked using quantity × unit price; verify any difference against the printed receipt before saving.";
      if(duplicate)guidance="⚠ This bill looks like one you already scanned. Saving again will create duplicate purchases. Review carefully.";
      box.addView(text(guidance,13,duplicate?Color.rgb(180,95,20):muted,false));
      EditText edit=new EditText(this);edit.setGravity(Gravity.TOP);edit.setMinLines(10);edit.setTextSize(14);
      edit.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
      edit.setText(prepared.isEmpty()?raw:prepared);box.addView(edit);ScrollView sc=new ScrollView(this);sc.addView(box);
      new AlertDialog.Builder(this).setTitle(duplicate?"Possible duplicate bill":"Review scanned bill").setView(sc).setNegativeButton("Cancel",null).
        setPositiveButton(duplicate?"Save anyway":"Save purchases",(d,w)->saveBillLines(edit.getText().toString(),signature)).show();
    }
    void saveBillLines(String content){saveBillLines(content,null);}
    void saveBillLines(String content,String signature){
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
      if(saved>0){
        if(signature!=null&&!signature.isEmpty())editor.putBoolean("bill_scanned_"+signature,true);
        editor.apply();
      }
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