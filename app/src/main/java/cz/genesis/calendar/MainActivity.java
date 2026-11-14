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
 LinearLayout root,content; LocalDate selected=LocalDate.now(); YearMonth month=YearMonth.now(); int mode=0;
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
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);draw();}
 void draw(){
  root=column();root.setBackgroundColor(BG);pad(root,18,15,18,12);setContentView(root);
  LinearLayout top=row();root.addView(top,lp(-1,52));
  LinearLayout heading=column();top.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
  heading.addView(text("GENESIS  /  CALENDAR",11,PURPLE,true));
  heading.addView(text("Můj kalendář",25,WHITE,true));
  TextView today=center("Dnes",13,PURPLE,true);today.setBackground(outline(SURFACE,16,0xff39324e));pad(today,16,10,16,10);top.addView(today);today.setOnClickListener(v->{selected=LocalDate.now();month=YearMonth.now();draw();});
  gap(root,19);
  LinearLayout controls=row();root.addView(controls,lp(-1,46));
  TextView prev=center("‹",32,WHITE,false);controls.addView(prev,lp(42,44));
  TextView title=center(mode==0?monthName():"Týden "+selected.minusDays(selected.getDayOfWeek().getValue()-1).format(DateTimeFormatter.ofPattern("d. M.")),19,WHITE,true);controls.addView(title,new LinearLayout.LayoutParams(0,-1,1));
  TextView next=center("›",32,WHITE,false);controls.addView(next,lp(42,44));
  prev.setOnClickListener(v->{if(mode==0)month=month.minusMonths(1);else selected=selected.minusWeeks(1);draw();});
  next.setOnClickListener(v->{if(mode==0)month=month.plusMonths(1);else selected=selected.plusWeeks(1);draw();});
  gap(root,13);
  LinearLayout tabs=row();tabs.setBackground(shape(SURFACE,18));pad(tabs,4,4,4,4);root.addView(tabs,lp(-1,50));
  String[] names={"Měsíc","Týden 3×3","Seznam"};
  for(int i=0;i<3;i++){final int idx=i;TextView tab=center(names[i],13,i==mode?WHITE:MUTED,i==mode);if(i==mode)tab.setBackground(shape(0xff494064,15));tabs.addView(tab,new LinearLayout.LayoutParams(0,-1,1));tab.setOnClickListener(v->{mode=idx;draw();});}
  gap(root,16);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));content=column();scroll.addView(content);
  if(mode==0)monthView();else if(mode==1)weekView();else listView();
  gap(content,19);agenda();
  gap(root,10);
  TextView add=center("+   Nová událost",16,0xff171022,true);add.setBackground(shape(PURPLE,19));root.addView(add,lp(-1,54));add.setOnClickListener(v->addEvent());
 }
 void monthView(){
  LinearLayout box=column();box.setBackground(shape(SURFACE,22));pad(box,9,13,9,12);content.addView(box,lp(-1,-2));
  String[] names={"PO","ÚT","ST","ČT","PÁ","SO","NE"};LinearLayout header=row();box.addView(header,lp(-1,32));
  for(String s:names)header.addView(center(s,11,MUTED,true),new LinearLayout.LayoutParams(0,-1,1));
  int offset=month.atDay(1).getDayOfWeek().getValue()-1;int cells=((offset+month.lengthOfMonth()+6)/7)*7;
  for(int r=0;r<cells/7;r++){LinearLayout line=row();box.addView(line,lp(-1,68));
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
  LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(large?115:61),1);p.setMargins(dp(3),dp(3),dp(3),dp(3));line.addView(tile,p);pad(tile,4,4,4,5);
  if(large)tile.addView(center(weekday(d).toUpperCase(new Locale("cs")),11,MUTED,true));
  LinearLayout digits=row();digits.setGravity(Gravity.CENTER);tile.addView(digits,lp(-1,large?31:27));
  digits.addView(center(""+d.getDayOfMonth(),large?23:17,WHITE,true));
  if(count(d)>0){TextView dot=center(" ●",11,accent(d),true);digits.addView(dot);}
  if(large)tile.addView(center(score(d)+" %",16,PURPLE,true));
  ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setProgress(score(d));bar.setProgressTintList(android.content.res.ColorStateList.valueOf(PURPLE));bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff383348));LinearLayout.LayoutParams bp=lp(-1,4);bp.leftMargin=dp(5);bp.rightMargin=dp(5);bp.topMargin=dp(large?7:5);tile.addView(bar,bp);
  if(large)tile.addView(center(count(d)+" úkolů",11,MUTED,false));
  tile.setOnClickListener(v->{selected=d;month=YearMonth.from(d);draw();});
 }
 void agenda(){
  LinearLayout heading=row();content.addView(heading,lp(-1,40));heading.addView(text("Program dne",19,WHITE,true),new LinearLayout.LayoutParams(0,-1,1));heading.addView(text(selected.format(DateTimeFormatter.ofPattern("d. M. yyyy")),12,MUTED,false));
  JSONArray a=events();boolean found=false;
  for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null||!selected.toString().equals(o.optString("date")))continue;found=true;final int at=i;
   LinearLayout line=row();line.setBackground(shape(SURFACE,17));pad(line,13,9,10,9);LinearLayout.LayoutParams p=lp(-1,62);p.bottomMargin=dp(7);content.addView(line,p);
   TextView bullet=center("●",16,o.optInt("priority",1)==3?RED:o.optInt("priority",1)==2?AMBER:GREEN,true);line.addView(bullet,lp(23,-1));
   LinearLayout info=column();line.addView(info,new LinearLayout.LayoutParams(0,-2,1));
   info.addView(text(o.optString("title"),15,WHITE,true));
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
  gap(shell,22);
  LinearLayout actions=row();shell.addView(actions,lp(-1,52));
  TextView cancel=center("Zrušit",14,MUTED,true);actions.addView(cancel,new LinearLayout.LayoutParams(0,-1,1));cancel.setOnClickListener(v->dialog.dismiss());
  TextView saveButton=center("Uložit událost",15,0xff171022,true);saveButton.setBackground(shape(PURPLE,16));actions.addView(saveButton,new LinearLayout.LayoutParams(0,-1,2));
  saveButton.setOnClickListener(v->{
   String title=name.getText().toString().trim(),begin=from.getText().toString(),finish=to.getText().toString();
   if(title.isEmpty()){name.setError("Zadej název");return;}
   if(minutes(finish,0)<=minutes(begin,0)){Toast.makeText(this,"Konec musí být později než začátek",Toast.LENGTH_LONG).show();return;}
   try{JSONArray a=events();JSONObject o=new JSONObject();o.put("date",selected.toString());o.put("title",title);o.put("start",begin);o.put("end",finish);o.put("priority",chosen[0]);a.put(o);save(a);dialog.dismiss();draw();}
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
  int m=minutes(target.getText().toString(),540);
  final Dialog picker=new Dialog(this);LinearLayout shell=column();shell.setBackground(shape(SURFACE,24));pad(shell,20,22,20,18);
  shell.addView(text("Vybrat čas",21,WHITE,true));gap(shell,15);
  LinearLayout wheels=row();wheels.setGravity(Gravity.CENTER);shell.addView(wheels,lp(-1,170));
  NumberPicker hour=new NumberPicker(this);hour.setMinValue(0);hour.setMaxValue(23);hour.setValue(m/60);hour.setWrapSelectorWheel(true);wheels.addView(hour,lp(95,160));
  wheels.addView(center(":",25,PURPLE,true),lp(24,160));
  NumberPicker minute=new NumberPicker(this);minute.setMinValue(0);minute.setMaxValue(59);minute.setValue(m%60);minute.setWrapSelectorWheel(true);wheels.addView(minute,lp(95,160));
  gap(shell,15);TextView confirm=center("Potvrdit",16,0xff171022,true);confirm.setBackground(shape(PURPLE,16));shell.addView(confirm,lp(-1,52));
  confirm.setOnClickListener(v->{target.setText(String.format(Locale.ROOT,"%02d:%02d",hour.getValue(),minute.getValue()));picker.dismiss();});
  picker.setContentView(shell);picker.show();Window w=picker.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout(getResources().getDisplayMetrics().widthPixels-dp(48),WindowManager.LayoutParams.WRAP_CONTENT);}
 }
}
