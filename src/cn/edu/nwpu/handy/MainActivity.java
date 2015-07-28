package cn.edu.nwpu.handy;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {
	
	final private Uri SMS_INBOX = Uri.parse("content://sms/");
	private SmsObserver smsObserver;  
	private int lastID = 0;
	private boolean runningStatus = true;
    private SQLiteDatabase db;
    private ListView recordsListView;
    private Switch statusSwitch;
   
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initDatabase();
        
        smsObserver = new SmsObserver(this, smsHandler);
        getContentResolver().registerContentObserver(SMS_INBOX, true, smsObserver);
        
        recordsListView = (ListView)findViewById(R.id.records);
        recordsListView.setOnItemClickListener(recordItemClickListener);
        
        statusSwitch = (Switch)findViewById(R.id.status_swith);
        statusSwitch.setOnCheckedChangeListener(statusListener);
        
        flushRecords();

        
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
        	Intent intent = new Intent(this, SettingActivity.class);
        	startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private OnCheckedChangeListener statusListener = new OnCheckedChangeListener() {
    	@Override
    	public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
    		if (isChecked) 
    			runningStatus = true;
    		else 
    			runningStatus = false;
    	}
    };
    
    private void initDatabase() {
    	try {
    		String destPath = "/data/data/" + getPackageName() + "/databases";
    		File f = new File(destPath);
    		if (!f.exists()) {
    			f.mkdirs();
    			f.createNewFile();
    			
    			// Copy initdb to the databases folder
    			InputStream inputStream = getBaseContext().getAssets().open("initdb");
    			OutputStream outputStream = new FileOutputStream(destPath + "/data");
    			byte[] buffer = new byte[1024];
    			int length;
    			while ((length = inputStream.read(buffer)) > 0) {
    				outputStream.write(buffer, 0, length);
    			}
    			inputStream.close();
    			outputStream.close();
    		}
    	}
    	catch (FileNotFoundException e) {
    		e.printStackTrace();
    	}
    	catch (IOException e) {
    		e.printStackTrace();
    	}
    	
    	db = openOrCreateDatabase("data", MODE_PRIVATE, null);
    }
    
    private List<Map<String, Object>> getData() {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Cursor cursor = db.query(true, "records", new String[] {"number", "content"}, null, null, null, null, "_id desc", null, null);
		
		while(cursor.moveToNext()) {
			Map<String, Object> mp = new HashMap<String, Object>(); 
			mp.put("number", cursor.getString(cursor.getColumnIndex("number")));
			mp.put("content", cursor.getString(cursor.getColumnIndex("content")));
			list.add(mp);
		}
		return list;
	}
    
    private List<String> getControllers() {
    	Cursor cursor = db.query(true, "controllers", new String[] {"number"}, null, null, null, null, null, null, null);
    	List<String> list = new ArrayList<String>();
    	
    	while (cursor.moveToNext()) {
    		list.add(cursor.getString(0));
    	}
    	return list;
    }
    
    private void flushRecords() {
         SimpleAdapter adapter = new SimpleAdapter(this, getData(), R.layout.record_item,
         		new String[] {"number", "content"}, new int[] {R.id.record_title, R.id.record_info});
         recordsListView.setAdapter(adapter);
    }
    
    private OnItemClickListener recordItemClickListener = new OnItemClickListener() {
    	@Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
    		String title = ((TextView)view.findViewById(R.id.record_title)).getText().toString();
    		String info = ((TextView)view.findViewById(R.id.record_info)).getText().toString();
            Toast.makeText(getBaseContext(), title + "\n" + info, Toast.LENGTH_SHORT).show();
    	}
	};
    
    private void getSmsFromPhone() {  
        
        List<String> controllers = getControllers();
        if (controllers.isEmpty())
        	return;
        
        ContentResolver cr = getContentResolver();  
        
        String[] projection = new String[] {"_id", "address", "person", "body" };//"_id", "address", "person",, "date", "type  
        String tmp = "address = '" + controllers.get(0) + "'";
        for (int i = 1; i < controllers.size(); i++) {
        	tmp += " or address = '" + controllers.get(i) + "'"; 
        }
        String where = "(" + tmp + ")" +"AND _id > " + lastID +" AND date >  "  
                + (System.currentTimeMillis() - 10 * 1000);  
        Cursor cur = cr.query(SMS_INBOX, projection, where, null, "date desc");  
        if (null == cur)  
            return;  
        if (cur.moveToNext()) {  
            String number = cur.getString(cur.getColumnIndex("address"));//手机号  
            //String name = cur.getString(cur.getColumnIndex("person"));//联系人姓名列表  
            String body = cur.getString(cur.getColumnIndex("body"));  
            lastID = cur.getInt(cur.getColumnIndex("_id"));
            Log.d("FDEBUG", body);
            Log.d("FDEBUG", lastID + "");
            //Toast.makeText(getApplicationContext(), body, Toast.LENGTH_SHORT).show();
//            TextView myview = (TextView) this.findViewById(R.id.myText); 
//            myview.setText(body);  
            if (runningStatus) {
	            ContentValues values = new ContentValues();
	            values.put("number", number);
	            values.put("content", body);
	            db.insert("records", null, values);
	            flushRecords();
            }
        }  
    } 
    
    public Handler smsHandler = new Handler() {  
        //这里可以进行回调的操作  
        //TODO  
  
    };  
    
    class SmsObserver extends ContentObserver {  
    	  
        public SmsObserver(Context context, Handler handler) {  
            super(handler);  
        }  
  
        @Override  
        public void onChange(boolean selfChange) {  
            super.onChange(selfChange);  
            //每当有新短信到来时，使用我们获取短消息的方法  
            Log.d("FDEBUG", "get message");
            getSmsFromPhone();  
        }  
    }  
}


