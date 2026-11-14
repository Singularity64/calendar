package cz.genesis.calendar;
import android.app.*;
import android.os.Bundle;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
public class MainActivity extends Activity {
 final int bg=0xff111218, card=0xff20212b, purple=0xffa78bfa;
 LinearLayout main,body; LocalDate selected=LocalDate.now(); YearMonth month=YearMonth.now(); int mode=0;
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER);return t;}
 JSONArray events(){try{return new JSONArray(getPreferences(0).getString("events","[]"));}catch(Exception e){return new JSONArray();}}
 void save(JSONArray a){getPreferences(0).edit().putString("events",a.toString()).apply();}
 int count(LocalDate d){int n=0;JSONArray a=events();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&d.toString().equals(o.optString("date")))n++;}return n;}
 int score(LocalDate d){return Math.min(100,count(d)*20);}
 @Override public void onCreate(Bundle b){super.onCreate(b);draw();}
 void draw(){
  main=new LinearLayout(this);main.setOrientation(1);main.setBackgroundColor(bg);main.setPadding(dp(12),dp(18),dp(12),dp(10));setContentView(main);
  LinearLayout nav=new LinearLayout(this);main.addView(nav);
  Button prev=new Button(this);prev.setText("‹");nav.addView(prev,new LinearLayout.LayoutParams(dp(54),dp(52)));
  TextView heading=label(mode==0?month.format(DateTimeFormatter.ofPattern("LLLL yyyy",new Locale("cs"))):"Týden od "+selected.minusDays(selected.getDayOfWeek().getValue()-1),19);nav.addView(heading,new LinearLayout.LayoutParams(0,dp(52),1));
  Button next=new Button(this);next.setText("›");nav.addView(next,new LinearLayout.LayoutParams(dp(54),dp(52)));
  prev.setOnClickListener(v->{if(mode==0)month=month.minusMonths(1);else selected=selected.minusWeeks(1);draw();});
  next.setOnClickListener(v->{if(mode==0)month=month.plusMonths(1);else selected=selected.plusWeeks(1);draw();});
  LinearLayout tabs=new LinearLayout(this);main.addView(tabs);String[] names={"Měsíc","Týden 3×3","Seznam"};
  for(int i=0;i<3;i++){final int index=i;Button b=new Button(this);b.setText(names[i]);tabs.addView(b,new LinearLayout.LayoutParams(0,dp(52),1));b.setOnClickListener(v->{mode=index;draw();});}
  ScrollView scroll=new ScrollView(this);main.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));body=new LinearLayout(this);body.setOrientation(1);scroll.addView(body);
  if(mode==0)monthView();else if(mode==1)weekView();else listView();
  TextView title=label("Události: "+selected+"  •  vytížení "+score(selected)+" %",16);body.addView(title,new LinearLayout.LayoutParams(-1,dp(60)));
  JSONArray a=events();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null||!selected.toString().equals(o.optString("date")))continue;
   LinearLayout line=new LinearLayout(this);body.addView(line);TextView item=label(o.optString("title"),16);line.addView(item,new LinearLayout.LayoutParams(0,dp(52),1));
   final int at=i;Button del=new Button(this);del.setText("×");line.addView(del);del.setOnClickListener(v->{JSONArray updated=new JSONArray();for(int j=0;j<a.length();j++)if(j!=at)updated.put(a.opt(j));save(updated);draw();});
  }
  Button add=new Button(this);add.setText("+ Přidat událost");main.addView(add);add.setOnClickListener(v->addEvent());
 }
 void monthView(){int offset=month.atDay(1).getDayOfWeek().getValue()-1;String[] names={"Po","Út","St","Čt","Pá","So","Ne"};LinearLayout header=new LinearLayout(this);body.addView(header);
 for(String name:names)header.addView(label(name,12),new LinearLayout.LayoutParams(0,dp(32),1));
 int cells=((offset+month.lengthOfMonth()+6)/7)*7;
 for(int r=0;r<cells/7;r++){LinearLayout row=new LinearLayout(this);body.addView(row);for(int c=0;c<7;c++){int day=r*7+c-offset+1;if(day<1||day>month.lengthOfMonth()){row.addView(new View(this),new LinearLayout.LayoutParams(0,dp(67),1));continue;}LocalDate d=month.atDay(day);dayTile(row,d,false);}}
 }
 void weekView(){LocalDate monday=selected.minusDays(selected.getDayOfWeek().getValue()-1);for(int r=0;r<3;r++){LinearLayout row=new LinearLayout(this);body.addView(row);for(int c=0;c<3;c++){int i=r*3+c;if(i<7)dayTile(row,monday.plusDays(i),true);else {TextView extra=label(i==7?"Souhrn týdne":"Priority • vytížení",12);row.addView(extra,new LinearLayout.LayoutParams(0,dp(118),1));}}}}
 void listView(){LocalDate monday=selected.minusDays(selected.getDayOfWeek().getValue()-1);for(int i=0;i<7;i++){LocalDate d=monday.plusDays(i);Button b=new Button(this);b.setText(d+"  •  "+count(d)+" událostí  •  "+score(d)+" %");body.addView(b);b.setOnClickListener(v->{selected=d;draw();});}}
 void dayTile(LinearLayout row,LocalDate d,boolean large){LinearLayout tile=new LinearLayout(this);tile.setOrientation(1);tile.setGravity(Gravity.CENTER);tile.setBackgroundColor(d.equals(selected)?0xff49366a:card);
 LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(large?118:67),1);lp.setMargins(dp(2),dp(2),dp(2),dp(2));row.addView(tile,lp);
 tile.addView(label((large?d.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT,new Locale("cs"))+" ":"")+d.getDayOfMonth(),large?16:14));
 tile.addView(label(score(d)+" %",large?17:10));
 ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setProgress(score(d));tile.addView(bar,new LinearLayout.LayoutParams(-1,dp(5)));
 if(large)tile.addView(label(count(d)+" úkolů",12));tile.setOnClickListener(v->{selected=d;month=YearMonth.from(d);draw();});
 }
 void addEvent(){EditText input=new EditText(this);input.setHint("Název události");new AlertDialog.Builder(this).setTitle("Událost • "+selected).setView(input).setNegativeButton("Zrušit",null).setPositiveButton("Uložit",(dialog,which)->{String title=input.getText().toString().trim();if(title.isEmpty())return;try{JSONArray a=events();JSONObject o=new JSONObject();o.put("date",selected.toString());o.put("title",title);a.put(o);save(a);draw();}catch(Exception ignored){}}).show();}
}
