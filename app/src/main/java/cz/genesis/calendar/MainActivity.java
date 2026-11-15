package cz.genesis.calendar;
import android.app.*;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public class MainActivity extends Activity {
 final int BG=0xff101017, SURFACE=0xff1b1b28, TILE=0xff242436, PURPLE=0xffaa8bff, WHITE=0xfff5f3ff, MUTED=0xff9997ae, RED=0xffff747e, AMBER=0xffffbd69, GREEN=0xff7bd8a5;
 LinearLayout root,content; LocalDate selected=LocalDate.now(); YearMonth month=YearMonth.now(); int mode=0; boolean agendaExpanded=false;
 int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 GradientDrawable outline(int color,int radius,int border){GradientDrawable d=shape(color,radius);d.setStroke(dp(1),border);return d;}
 TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
 LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
 LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
 void pad(View v,int a,int b,int c,int d){v.setPadding(dp(a),dp(b),dp(c),dp(d));}
 void gap(LinearLayout l,int h){View v=new View(this);l.addView(v,lp(1,h));}
 TextView center(String s,int size,int color,boolean bold){TextView t=text(s,size,color,bold);t.setGravity(Gravity.CENTER);return t;}
 JSONArray events(){try{return new JSONArray(getPreferences(0).getString("events","[]"));}catch(Exception e){return new JSONArray();}}
 void save(JSONArray a){getPreferences(0).edit().putString("events",a.toString()).apply();}
 int count(LocalDate d){int n=0;JSONArray a=events();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&d.toString().equals(o.optString("date")))n++;}return n;}
 int score(LocalDate d){
  JSONArray a=events();boolean[] busy=new boolean[1440];int n=0,weight=0;
  for(int i=0;i<a.length();i++){
   JSONObject o=a.optJSONObject(i);if(o==null||!d.toString().equals(o.optString("date")))continue;
   n++;weight+=Math.max(1,Math.min(3,o.optInt("priority",1)));
   int start=minutes(o.optString("start","09:00"),540),end=minutes(o.optString("end","10:00"),600);
   if(end<=start)continue;
   for(int m=start;m<end;m++)busy[m]=true;
  }
  if(n==0)return 0;
  int used=0;for(boolean b:busy)if(b)used++;
  // 70 % délka vůči 12 h, 20 % počet událostí vůči 8, 10 % priorita.
  return Math.min(100,(int)Math.round(70.0*used/720.0+20.0*n/8.0+10.0*weight/24.0));
 }
 int minutes(String time,int fallback){
  try{String[] parts=time.split(":");int h=Integer.parseInt(parts[0]),m=Integer.parseInt(parts[1]);return h>=0&&h<24&&m>=0&&m<60?h*60+m:fallback;}catch(Exception e){return fallback;}
 }
 String weekday(LocalDate d){return d.getDayOfWeek().getDisplayName(TextStyle.SHORT,new Locale("cs"));}
 String monthName(){return month.format(DateTimeFormatter.ofPattern("LLLL yyyy",new Locale("cs")));}
 int accent(LocalDate d){JSONArray a=events();int p=0;for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&d.toString().equals(o.optString("date")))p=Math.max(p,o.optInt("priority",1));}return p==3?RED:p==2?AMBER:p==1?GREEN:MUTED;}
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  getWindow().setStatusBarColor(BG);
  getWindow().setNavigationBarColor(BG);
  // Edge-to-edge: system bars are measured, never approximated with a fixed dp offset.
  if(android.os.Build.VERSION.SDK_INT>=30){
   getWindow().setDecorFitsSystemWindows(false);
  }else{
   getWindow().getDecorView().setSystemUiVisibility(
    View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
  }
  draw();
 }
 void draw(){
  root=column();root.setBackgroundColor(BG);
  pad(root,18,15,18,12);
  root.setOnApplyWindowInsetsListener((view,insets)->{
   int topInset,bottomInset;
   if(android.os.Build.VERSION.SDK_INT>=30){
    android.graphics.Insets bars=insets.getInsets(
     WindowInsets.Type.statusBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.navigationBars());
    topInset=bars.top;bottomInset=bars.bottom;
   }else{
    topInset=insets.getSystemWindowInsetTop();
    bottomInset=insets.getSystemWindowInsetBottom();
   }
   view.setPadding(dp(18),topInset+dp(15),dp(18),bottomInset+dp(12));
   return insets;
  });
  setContentView(root);root.requestApplyInsets();
  LinearLayout top=row();root.addView(top,lp(-1,52));
  LinearLayout heading=column();top.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
  heading.addView(text("GENESIS  /  CALENDAR",11,PURPLE,true));
  heading.addView(text("Můj kalendář",25,WHITE,true));
  String[] tools={"⌕","▥","⚙"};String[] descriptions={"Hledat","Statistiky","Nastavení"};
  for(int i=0;i<3;i++){final int index=i;TextView action=center(tools[i],24,PURPLE,true);
   action.setContentDescription(descriptions[i]);action.setBackground(shape(SURFACE,13));
   LinearLayout.LayoutParams p=lp(43,43);p.leftMargin=dp(7);top.addView(action,p);
   action.setOnClickListener(v->{if(index==0)searchEvents();else if(index==1)showStats();else showSettings();});
  }
  View headerGap=new View(this);root.addView(headerGap,lp(1,19));
  LinearLayout controls=row();root.addView(controls,lp(-1,46));
  TextView prev=center("‹",32,WHITE,false);controls.addView(prev,lp(42,44));
  TextView title=center(mode==0?monthName():"Týden "+selected.minusDays(selected.getDayOfWeek().getValue()-1).format(DateTimeFormatter.ofPattern("d. M.")),19,WHITE,true);controls.addView(title,new LinearLayout.LayoutParams(0,-1,1));
  TextView next=center("›",32,WHITE,false);controls.addView(next,lp(42,44));
  prev.setOnClickListener(v->{if(mode==0)month=month.minusMonths(1);else selected=selected.minusWeeks(1);draw();});
  next.setOnClickListener(v->{if(mode==0)month=month.plusMonths(1);else selected=selected.plusWeeks(1);draw();});
  View controlsGap=new View(this);root.addView(controlsGap,lp(1,13));
  LinearLayout tabs=row();tabs.setBackground(shape(SURFACE,18));pad(tabs,4,4,4,4);root.addView(tabs,lp(-1,50));
  String[] names={"Měsíc","Týden 3×3","Seznam"};
  for(int i=0;i<3;i++){final int idx=i;TextView tab=center(names[i],13,i==mode?WHITE:MUTED,i==mode);if(i==mode)tab.setBackground(shape(0xff494064,15));tabs.addView(tab,new LinearLayout.LayoutParams(0,-1,1));tab.setOnClickListener(v->{mode=idx;draw();});}
  View tabsGap=new View(this);root.addView(tabsGap,lp(1,16));
  // Calendar stays above; the daily agenda has its own vertical scroll area.
  ScrollView calendarScroll=new ScrollView(this);calendarScroll.setFillViewport(false);
  int calendarMax=dp(mode==0?340:mode==1?330:230);
  int available=getResources().getDisplayMetrics().heightPixels;
  calendarMax=Math.min(calendarMax,(int)(available*0.70f));
  root.addView(calendarScroll,new LinearLayout.LayoutParams(-1,0,1));
  content=column();calendarScroll.addView(content);
  if(mode==0)monthView();else if(mode==1)weekView();else listView();
  View calendarGap=new View(this);root.addView(calendarGap,lp(1,12));
  // Compact daily load indicator doubles as the agenda expansion handle.
  LinearLayout pullCard=row();pullCard.setGravity(Gravity.CENTER_VERTICAL);
  pullCard.setBackground(shape(SURFACE,19));pad(pullCard,14,7,10,7);
  root.addView(pullCard,lp(-1,66));
  LinearLayout loadInfo=column();pullCard.addView(loadInfo,new LinearLayout.LayoutParams(0,-1,1));
  int[] currentLoad=dayLoad(selected);
  int loadMinutes=currentLoad[1]+currentLoad[2]+currentLoad[3];
  int loadPercent=Math.min(100,(int)Math.round(loadMinutes*100.0/(capacity()*60)));
  loadInfo.addView(text("Denní vytížení  •  "+hours(loadMinutes)+"  ("+loadPercent+" %)",13,WHITE,true),lp(-1,29));
  LinearLayout miniBar=row();miniBar.setBackground(shape(0xff393849,8));miniBar.setClipToOutline(true);
  loadInfo.addView(miniBar,lp(-1,12));
  int barScale=Math.max(loadMinutes,capacity()*60);
  int[] loadColors={GREEN,0xff59a9ff,RED};
  for(int level=1;level<=3;level++){
   if(currentLoad[level]<=0)continue;
   View segment=new View(this);segment.setBackgroundColor(loadColors[level-1]);
   miniBar.addView(segment,new LinearLayout.LayoutParams(0,-1,(float)currentLoad[level]/barScale));
  }
  if(barScale>loadMinutes)miniBar.addView(new View(this),new LinearLayout.LayoutParams(0,-1,(float)(barScale-loadMinutes)/barScale));
  TextView arrow=center(agendaExpanded?"⌄":"⌃",32,PURPLE,true);
  pullCard.addView(arrow,lp(50,52));
  pullCard.setContentDescription(agendaExpanded?"Sbalit události":"Rozbalit události");
  int eventCount=count(selected);
  final int expandedAgendaHeight=Math.min(dp(40+Math.max(1,eventCount)*69), (int)(available*0.68f));
  ScrollView agendaScroll=new ScrollView(this);agendaScroll.setFillViewport(true);
  final View[] hideWhenExpanded={};
  Runnable applyLayout=()->{
   for(View v:hideWhenExpanded)v.setVisibility(agendaExpanded?View.GONE:View.VISIBLE);
   agendaScroll.getLayoutParams().height=agendaExpanded?expandedAgendaHeight:0;
   agendaScroll.requestLayout();
   arrow.setText(agendaExpanded?"⌄":"⌃");
   pullCard.setContentDescription(agendaExpanded?"Sbalit události":"Rozbalit události");
  };
  pullCard.setOnClickListener(v->{agendaExpanded=!agendaExpanded;applyLayout.run();});
  pullCard.setOnTouchListener(new View.OnTouchListener(){
   float startY;
   public boolean onTouch(View v,MotionEvent e){
    if(e.getActionMasked()==MotionEvent.ACTION_DOWN){startY=e.getRawY();return true;}
    if(e.getActionMasked()==MotionEvent.ACTION_UP){
     float delta=e.getRawY()-startY;
     if(Math.abs(delta)<dp(16)||(!agendaExpanded&&delta< -dp(35))||(agendaExpanded&&delta>dp(35))){
      agendaExpanded=!agendaExpanded;applyLayout.run();
     }
     return true;
    }
    return true;
   }
  });
  agendaScroll.setVerticalScrollBarEnabled(true);
  root.addView(agendaScroll,new LinearLayout.LayoutParams(-1,0));
  applyLayout.run();
  content=column();agendaScroll.addView(content);agenda();
  gap(root,10);
  TextView add=center("+   Nová událost",16,0xff171022,true);add.setBackground(shape(PURPLE,19));root.addView(add,lp(-1,54));add.setOnClickListener(v->addEvent());
 }
 void monthView(){
  LinearLayout box=column();box.setBackground(shape(SURFACE,22));pad(box,9,13,9,12);content.addView(box,lp(-1,-2));
  String[] names={"PO","ÚT","ST","ČT","PÁ","SO","NE"};LinearLayout header=row();box.addView(header,lp(-1,32));
  for(String s:names)header.addView(center(s,11,MUTED,true),new LinearLayout.LayoutParams(0,-1,1));
  int offset=month.atDay(1).getDayOfWeek().getValue()-1;int cells=((offset+month.lengthOfMonth()+6)/7)*7;
  for(int r=0;r<cells/7;r++){LinearLayout line=row();box.addView(line,lp(-1,48));
   for(int c=0;c<7;c++){int day=r*7+c-offset+1;if(day<1||day>month.lengthOfMonth()){line.addView(new View(this),new LinearLayout.LayoutParams(0,-1,1));continue;}dayTile(line,month.atDay(day),false);}
  }
 }
 void weekView(){
  LocalDate monday=selected.minusDays(selected.getDayOfWeek().getValue()-1);
  for(int r=0;r<3;r++){LinearLayout line=row();content.addView(line,lp(-1,125));for(int c=0;c<3;c++){int i=r*3+c;if(i<7)dayTile(line,monday.plusDays(i),true);else{LinearLayout tile=column();tile.setGravity(Gravity.CENTER);tile.setBackground(shape(SURFACE,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(115),1);p.setMargins(dp(3),dp(3),dp(3),dp(3));line.addView(tile,p);tile.addView(center(i==7?"TÝDEN":"PŘEHLED",12,MUTED,true));int total=0;for(int j=0;j<7;j++)total+=count(monday.plusDays(j));tile.addView(center(i==7?total+" úkolů":"● priority",16,PURPLE,true));}}}
 }
 void listView(){LocalDate monday=selected.minusDays(selected.getDayOfWeek().getValue()-1);for(int i=0;i<7;i++){LocalDate d=monday.plusDays(i);LinearLayout line=row();line.setBackground(shape(d.equals(selected)?0xff39304f:SURFACE,17));pad(line,15,10,15,10);LinearLayout.LayoutParams p=lp(-1,63);p.bottomMargin=dp(7);content.addView(line,p);TextView day=text(weekday(d)+" "+d.getDayOfMonth()+".",16,WHITE,true);line.addView(day,new LinearLayout.LayoutParams(0,-1,1));line.addView(text(count(d)+" událostí",12,MUTED,false));TextView value=center("  "+score(d)+" %",15,PURPLE,true);line.addView(value);line.setOnClickListener(v->{selected=d;draw();});}}
 void dayTile(LinearLayout line,LocalDate d,boolean large){
  LinearLayout tile=column();tile.setGravity(Gravity.CENTER);tile.setBackground(d.equals(selected)?outline(0xff403455,17,PURPLE):shape(large?SURFACE:0xff20202e,17));
  LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(large?115:43),1);p.setMargins(dp(3),dp(3),dp(3),dp(3));line.addView(tile,p);pad(tile,4,4,4,5);
  if(large)tile.addView(center(weekday(d).toUpperCase(new Locale("cs")),11,MUTED,true));
  LinearLayout digits=row();digits.setGravity(Gravity.CENTER);tile.addView(digits,lp(-1,large?31:21));
  digits.addView(center(""+d.getDayOfMonth(),large?23:15,WHITE,true));
  if(count(d)>0){TextView dot=center(" ●",11,accent(d),true);digits.addView(dot);}
  if(large)tile.addView(center(score(d)+" %",16,PURPLE,true));
  ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setProgress(score(d));bar.setProgressTintList(android.content.res.ColorStateList.valueOf(PURPLE));bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff383348));LinearLayout.LayoutParams bp=lp(-1,4);bp.leftMargin=dp(5);bp.rightMargin=dp(5);bp.topMargin=dp(large?7:2);tile.addView(bar,bp);
  if(large)tile.addView(center(count(d)+" úkolů",11,MUTED,false));
  tile.setOnClickListener(v->{selected=d;month=YearMonth.from(d);draw();});
 }
 int capacity(){return getPreferences(0).getInt("daily_capacity",10);}
 boolean healthVisible(){return getPreferences(0).getBoolean("health_visible",true);}
 int[] dayLoad(LocalDate d){
  // Each minute is assigned to the highest difficulty to avoid double counting.
  int[] timeline=new int[1440];JSONArray a=events();
  for(int i=0;i<a.length();i++){
   JSONObject o=a.optJSONObject(i);
   if(o==null||!d.toString().equals(o.optString("date"))||o.optBoolean("done",false))continue;
   int start=minutes(o.optString("start","09:00"),540),end=minutes(o.optString("end","10:00"),600);
   int level=Math.max(1,Math.min(3,o.optInt("difficulty",o.optInt("priority",1))));
   for(int m=start;m<end;m++)timeline[m]=Math.max(timeline[m],level);
  }
  int[] load=new int[4];for(int level:timeline)if(level>0)load[level]++;
  return load;
 }
 String hours(int minutes){return String.format(Locale.forLanguageTag("cs"),"%.1f h",minutes/60.0);}
 void healthCard(){
  LinearLayout card=column();card.setBackground(shape(SURFACE,20));pad(card,14,12,14,13);content.addView(card,lp(-1,-2));
  LinearLayout header=row();card.addView(header,lp(-1,43));
  TextView heading=text("Denní vytížení",16,WHITE,true);header.addView(heading,new LinearLayout.LayoutParams(0,-1,1));
  boolean visible=healthVisible();TextView arrow=center(visible?"⌄":"›",27,PURPLE,true);header.addView(arrow,lp(44,43));
  header.setOnClickListener(v->{getPreferences(0).edit().putBoolean("health_visible",!healthVisible()).apply();draw();});
  if(!visible)return;
  int[] load=dayLoad(selected);int total=load[1]+load[2]+load[3],cap=capacity()*60;
  int percentage=Math.min(100,(int)Math.round(total*100.0/cap));
  LinearLayout summary=row();card.addView(summary,lp(-1,38));
  summary.addView(text(hours(total)+" naplánováno",17,WHITE,true),new LinearLayout.LayoutParams(0,-1,1));
  summary.addView(text(percentage+" %",22,PURPLE,true));
  gap(card,8);
  LinearLayout bar=row();bar.setBackground(shape(0xff393849,10));bar.setClipToOutline(true);card.addView(bar,lp(-1,20));
  int[] colors={GREEN,0xff59a9ff,RED};
  int scale=Math.max(total,cap);
  for(int level=1;level<=3;level++){if(load[level]==0)continue;
   View section=new View(this);section.setBackgroundColor(colors[level-1]);
   bar.addView(section,new LinearLayout.LayoutParams(0,-1,(float)load[level]/scale));
  }
  if(scale>total){View rest=new View(this);bar.addView(rest,new LinearLayout.LayoutParams(0,-1,(float)(scale-total)/scale));}
  gap(card,12);
  LinearLayout legend=row();card.addView(legend,lp(-1,35));
  String[] labels={"Lehká","Střední","Vysoká"};
  for(int i=0;i<3;i++){
   LinearLayout part=column();legend.addView(part,new LinearLayout.LayoutParams(0,-1,1));
   TextView label=text("● "+labels[i],11,colors[i],true);part.addView(label);
   part.addView(text(hours(load[i+1]),13,WHITE,true));
  }
  gap(card,5);card.addView(text("Obsazený čas z "+capacity()+" h denní kapacity",11,MUTED,false));
 }
 void showSettings(){
  LinearLayout body=column();pad(body,22,10,22,12);
  body.addView(text("Denní plánovací kapacita",15,WHITE,true));gap(body,12);
  TextView value=center(capacity()+" hodin",20,PURPLE,true);body.addView(value);
  SeekBar seek=new SeekBar(this);seek.setMax(10);seek.setProgress(capacity()-6);body.addView(seek);
  seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
   public void onProgressChanged(SeekBar b,int progress,boolean user){value.setText((progress+6)+" hodin");}
   public void onStartTrackingTouch(SeekBar b){}
   public void onStopTrackingTouch(SeekBar b){}
  });
  CheckBox visible=new CheckBox(this);visible.setText("Zobrazovat denní vytížení");visible.setTextColor(WHITE);visible.setChecked(healthVisible());body.addView(visible);
  new AlertDialog.Builder(this).setTitle("Nastavení").setView(body).setNegativeButton("Zrušit",null).setPositiveButton("Uložit",(d,w)->{
   getPreferences(0).edit().putInt("daily_capacity",seek.getProgress()+6).putBoolean("health_visible",visible.isChecked()).apply();draw();
  }).show();
 }
 void showStats(){
  JSONArray a=events();int done=0,all=0;for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;all++;if(o.optBoolean("done",false))done++;}
  new AlertDialog.Builder(this).setTitle("Statistiky").setMessage("Událostí celkem: "+all+"\\nDokončeno: "+done+"\\nZbývá: "+(all-done)+"\\n\\nPodrobné reporty připravujeme.").setPositiveButton("Zavřít",null).show();
 }
 void searchEvents(){
  EditText query=new EditText(this);query.setSingleLine(true);query.setHint("Název události");
  LinearLayout wrap=column();pad(wrap,20,8,20,4);wrap.addView(query);
  new AlertDialog.Builder(this).setTitle("Najít událost").setView(wrap).setNegativeButton("Zrušit",null).setPositiveButton("Hledat",(d,w)->{
   String needle=query.getText().toString().toLowerCase(Locale.ROOT);JSONArray a=events();
   for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&o.optString("title").toLowerCase(Locale.ROOT).contains(needle)){
    try{selected=LocalDate.parse(o.optString("date"));month=YearMonth.from(selected);draw();return;}catch(Exception ignored){}
   }}
   Toast.makeText(this,"Událost nenalezena",Toast.LENGTH_SHORT).show();
  }).show();
 }
 void agenda(){
  LinearLayout heading=row();content.addView(heading,lp(-1,40));heading.addView(text("Program dne",19,WHITE,true),new LinearLayout.LayoutParams(0,-1,1));heading.addView(text(selected.format(DateTimeFormatter.ofPattern("d. M. yyyy")),12,MUTED,false));
  JSONArray a=events();boolean found=false;
  for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null||!selected.toString().equals(o.optString("date")))continue;found=true;final int at=i;
   LinearLayout line=row();line.setBackground(shape(SURFACE,17));pad(line,13,9,10,9);LinearLayout.LayoutParams p=lp(-1,62);p.bottomMargin=dp(7);content.addView(line,p);
   CheckBox completed=new CheckBox(this);completed.setButtonTintList(android.content.res.ColorStateList.valueOf(PURPLE));
   completed.setChecked(o.optBoolean("done",false));completed.setContentDescription("Označit jako splněné");line.addView(completed,lp(43,-1));
   completed.setOnCheckedChangeListener((button,checked)->{try{o.put("done",checked);save(a);draw();}catch(Exception ignored){}});
   TextView bullet=center("●",13,o.optInt("priority",1)==3?RED:o.optInt("priority",1)==2?AMBER:GREEN,true);line.addView(bullet,lp(20,-1));
   LinearLayout info=column();line.addView(info,new LinearLayout.LayoutParams(0,-2,1));
   TextView eventTitle=text(o.optString("title"),15,o.optBoolean("done",false)?MUTED:WHITE,true);
   if(o.optBoolean("done",false))eventTitle.setPaintFlags(eventTitle.getPaintFlags()|android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
   info.addView(eventTitle);
   info.addView(text(o.optString("start","09:00")+"–"+o.optString("end","10:00"),12,MUTED,false));
   TextView del=center("×",23,MUTED,false);line.addView(del,lp(39,-1));del.setOnClickListener(v->{JSONArray updated=new JSONArray();for(int j=0;j<a.length();j++)if(j!=at)updated.put(a.opt(j));save(updated);draw();});
  }
  if(!found){TextView empty=center("Zatím žádné události. Volný den ✨",14,MUTED,false);empty.setBackground(shape(SURFACE,17));content.addView(empty,lp(-1,78));}
 }
 void addEvent(){
  final Dialog dialog=new Dialog(this);
  LinearLayout shell=column();shell.setBackground(shape(SURFACE,26));pad(shell,20,20,20,20);
  TextView eyebrow=text("GENESIS  /  NOVÁ UDÁLOST",11,PURPLE,true);shell.addView(eyebrow);
  gap(shell,8);shell.addView(text("Naplánovat událost",23,WHITE,true));
  gap(shell,4);shell.addView(text(selected.format(DateTimeFormatter.ofPattern("EEEE d. MMMM",new Locale("cs"))),13,MUTED,false));
  gap(shell,22);
  shell.addView(text("NÁZEV",11,MUTED,true));gap(shell,8);
  EditText name=new EditText(this);name.setSingleLine(true);name.setTextColor(WHITE);name.setHintTextColor(MUTED);
  name.setTextSize(16);name.setHint("Co máš v plánu?");name.setBackground(outline(TILE,15,0xff414055));pad(name,14,12,14,12);shell.addView(name,lp(-1,53));
  gap(shell,19);shell.addView(text("ČAS UDÁLOSTI",11,MUTED,true));gap(shell,8);
  LinearLayout times=row();shell.addView(times,lp(-1,74));
  LinearLayout startBox=timeBox("OD","09:00");times.addView(startBox,new LinearLayout.LayoutParams(0,-1,1));
  TextView sep=center("→",20,PURPLE,true);times.addView(sep,lp(35,-1));
  LinearLayout endBox=timeBox("DO","10:00");times.addView(endBox,new LinearLayout.LayoutParams(0,-1,1));
  TextView from=(TextView)startBox.getChildAt(1),to=(TextView)endBox.getChildAt(1);
  startBox.setOnClickListener(v->pickTime(from));endBox.setOnClickListener(v->pickTime(to));
  gap(shell,19);shell.addView(text("PRIORITA",11,MUTED,true));gap(shell,9);
  LinearLayout priorities=row();shell.addView(priorities,lp(-1,46));
  String[] names={"Běžná","Důležitá","Urgentní"};int[] colors={GREEN,AMBER,RED};final int[] chosen={1};
  TextView[] choices=new TextView[3];
  for(int p=0;p<3;p++){final int index=p;TextView t=center("● "+names[p],12,colors[p],true);
   LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-1,1);params.setMargins(dp(2),0,dp(2),0);priorities.addView(t,params);choices[p]=t;
   t.setOnClickListener(v->{chosen[0]=index+1;for(int j=0;j<3;j++)choices[j].setBackground(outline(j==index?0xff403455:TILE,13,j==index?PURPLE:0xff343345));});
  }
  for(int j=0;j<3;j++)choices[j].setBackground(outline(j==0?0xff403455:TILE,13,j==0?PURPLE:0xff343345));
  gap(shell,15);shell.addView(text("NÁROČNOST",11,MUTED,true));gap(shell,9);
  LinearLayout difficulties=row();shell.addView(difficulties,lp(-1,44));
  String[] difficultyNames={"Lehká","Střední","Vysoká"};int[] difficultyColors={GREEN,0xff59a9ff,RED};final int[] difficulty={1};
  TextView[] difficultyButtons=new TextView[3];
  for(int k=0;k<3;k++){final int index=k;TextView chip=center("● "+difficultyNames[k],12,difficultyColors[k],true);
   LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);p.setMargins(dp(2),0,dp(2),0);difficulties.addView(chip,p);difficultyButtons[k]=chip;
   chip.setOnClickListener(v->{difficulty[0]=index+1;for(int j=0;j<3;j++)difficultyButtons[j].setBackground(outline(j==index?0xff403455:TILE,13,j==index?PURPLE:0xff343345));});
  }
  for(int j=0;j<3;j++)difficultyButtons[j].setBackground(outline(j==0?0xff403455:TILE,13,j==0?PURPLE:0xff343345));
  gap(shell,22);
  LinearLayout actions=row();shell.addView(actions,lp(-1,52));
  TextView cancel=center("Zrušit",14,MUTED,true);actions.addView(cancel,new LinearLayout.LayoutParams(0,-1,1));cancel.setOnClickListener(v->dialog.dismiss());
  TextView saveButton=center("Uložit událost",15,0xff171022,true);saveButton.setBackground(shape(PURPLE,16));actions.addView(saveButton,new LinearLayout.LayoutParams(0,-1,2));
  saveButton.setOnClickListener(v->{
   String title=name.getText().toString().trim(),begin=from.getText().toString(),finish=to.getText().toString();
   if(title.isEmpty()){name.setError("Zadej název");return;}
   if(minutes(finish,0)<=minutes(begin,0)){Toast.makeText(this,"Konec musí být později než začátek",Toast.LENGTH_LONG).show();return;}
   try{JSONArray a=events();JSONObject o=new JSONObject();o.put("date",selected.toString());o.put("title",title);o.put("start",begin);o.put("end",finish);o.put("priority",chosen[0]);o.put("difficulty",difficulty[0]);a.put(o);save(a);dialog.dismiss();draw();}
   catch(Exception e){Toast.makeText(this,"Událost se nepodařilo uložit",Toast.LENGTH_SHORT).show();}
  });
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.addView(shell);dialog.setContentView(scroll);
  Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams attrs=window.getAttributes();attrs.dimAmount=.7f;window.setAttributes(attrs);}
  dialog.show();window=dialog.getWindow();if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.setLayout(getResources().getDisplayMetrics().widthPixels-dp(26),WindowManager.LayoutParams.WRAP_CONTENT);}
 }
 LinearLayout timeBox(String label,String value){
  LinearLayout box=column();box.setGravity(Gravity.CENTER);box.setBackground(outline(TILE,15,0xff414055));
  box.addView(center(label,10,MUTED,true));box.addView(center(value,21,WHITE,true));return box;
 }
 void pickTime(TextView target){
  int initial=minutes(target.getText().toString(),540);
  final int[] hour={initial/60},minute={initial%60};
  final Dialog picker=new Dialog(this);
  LinearLayout shell=column();shell.setBackground(shape(SURFACE,26));pad(shell,20,22,20,20);
  LinearLayout heading=row();shell.addView(heading,lp(-1,42));
  heading.addView(text("Vybrat čas",22,WHITE,true),new LinearLayout.LayoutParams(0,-1,1));
  TextView preview=center("",19,PURPLE,true);preview.setBackground(shape(0xff302a42,12));pad(preview,12,6,12,6);heading.addView(preview);
  gap(shell,17);
  LinearLayout wheels=row();shell.addView(wheels,lp(-1,330));
  LinearLayout hours=arrowColumn("HODINY",hour,23,preview,hour,minute);
  LinearLayout mins=arrowColumn("MINUTY",minute,59,preview,hour,minute);
  wheels.addView(hours,new LinearLayout.LayoutParams(0,-1,1));
  TextView colon=center(":",28,PURPLE,true);wheels.addView(colon,lp(28,-1));
  wheels.addView(mins,new LinearLayout.LayoutParams(0,-1,1));
  refreshTime(preview,hour,minute);
  gap(shell,18);
  TextView confirm=center("Potvrdit",16,0xff171022,true);confirm.setBackground(shape(PURPLE,17));shell.addView(confirm,lp(-1,54));
  confirm.setOnClickListener(v->{target.setText(String.format(Locale.ROOT,"%02d:%02d",hour[0],minute[0]));picker.dismiss();});
  picker.setContentView(shell);picker.show();Window w=picker.getWindow();
  if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout(getResources().getDisplayMetrics().widthPixels-dp(38),WindowManager.LayoutParams.WRAP_CONTENT);}
 }
 void refreshTime(TextView preview,int[] hour,int[] minute){
  preview.setText(String.format(Locale.ROOT,"%02d:%02d",hour[0],minute[0]));
 }
 View chevron(boolean up){
  View arrow=new View(this){
   android.graphics.Paint paint=new android.graphics.Paint(3);
   @Override protected void onDraw(android.graphics.Canvas canvas){
    super.onDraw(canvas);paint.setColor(PURPLE);paint.setStyle(android.graphics.Paint.Style.STROKE);
    paint.setStrokeWidth(dp(5));paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);
    float cx=getWidth()/2f,cy=getHeight()/2f,w=dp(15),h=dp(9);
    android.graphics.Path path=new android.graphics.Path();
    if(up){path.moveTo(cx-w,cy+h/2);path.lineTo(cx,cy-h/2);path.lineTo(cx+w,cy+h/2);}
    else{path.moveTo(cx-w,cy-h/2);path.lineTo(cx,cy+h/2);path.lineTo(cx+w,cy-h/2);}
    canvas.drawPath(path,paint);
   }
  };
  arrow.setContentDescription(up?"Zvýšit hodnotu":"Snížit hodnotu");
  return arrow;
 }
 LinearLayout arrowColumn(String label,int[] value,int max,TextView preview,int[] hour,int[] minute){
  LinearLayout col=column();col.setGravity(Gravity.CENTER);col.setBackground(shape(TILE,19));pad(col,8,7,8,7);
  col.addView(center(label,11,MUTED,true),lp(-1,29));
  View up=chevron(true);col.addView(up,lp(-1,58));
  TextView previous=center("",17,0xff77748a,false);col.addView(previous,lp(-1,35));
  TextView number=center("",30,WHITE,true);number.setBackground(shape(0xff493a68,13));col.addView(number,lp(-1,75));
  TextView next=center("",17,0xff77748a,false);col.addView(next,lp(-1,35));
  View down=chevron(false);col.addView(down,lp(-1,58));
  Runnable refresh=()->{
   number.setText(String.format(Locale.ROOT,"%02d",value[0]));
   previous.setText(String.format(Locale.ROOT,"%02d",(value[0]+max)%(max+1)));
   next.setText(String.format(Locale.ROOT,"%02d",(value[0]+1)%(max+1)));
   refreshTime(preview,hour,minute);
  };
  refresh.run();
  // Swipe up/down on the entire wheel, including the number and adjacent values.
  col.setOnTouchListener(new View.OnTouchListener(){
   float startY;boolean moved;
   @Override public boolean onTouch(View v,android.view.MotionEvent event){
    switch(event.getActionMasked()){
     case android.view.MotionEvent.ACTION_DOWN:startY=event.getY();moved=false;return true;
     case android.view.MotionEvent.ACTION_MOVE:
      float delta=event.getY()-startY;
      if(Math.abs(delta)>=dp(24)){
       int steps=Math.max(1,(int)(Math.abs(delta)/dp(24)));
       int direction=delta<0?1:-1;
       value[0]=((value[0]+direction*steps)%(max+1)+(max+1))%(max+1);
       startY=event.getY();moved=true;refresh.run();
      }
      return true;
     case android.view.MotionEvent.ACTION_UP:
      if(!moved){float y=event.getY();if(y<dp(115))value[0]=(value[0]+max)%(max+1);else if(y>dp(210))value[0]=(value[0]+1)%(max+1);refresh.run();}
      return true;
    }
    return true;
   }
  });
  up.setOnClickListener(v->{value[0]=(value[0]+max)%(max+1);refresh.run();});
  down.setOnClickListener(v->{value[0]=(value[0]+1)%(max+1);refresh.run();});
  return col;
 }
}
