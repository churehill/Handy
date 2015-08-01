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
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;


public class MainActivity extends Activity {
	
    private SQLiteDatabase db;
    private ListView recordsListView;
    private Switch statusSwitch;
    private Intent serviceIntent;
    
    private boolean isForeground = true;
    private boolean needFlush = false;
    
    private View dialogLayout;
   
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initDatabase();
        
        serviceIntent = new Intent(this, SmsService.class);
        startService(serviceIntent);
               
        recordsListView = (ListView)findViewById(R.id.records);
        recordsListView.setOnItemClickListener(recordItemClickListener);
        
        statusSwitch = (Switch)findViewById(R.id.status_swith);
        statusSwitch.setOnCheckedChangeListener(statusListener);
    }

    @Override 
    protected void onStart() {
    	super.onStart();
        flushRecords();   
        needFlush = false;
    	LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, new IntentFilter(SmsService.UPDATE_RECORD));
    }
    
    @Override
    protected void onStop() {
    	LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver);
    	super.onStop();
    }
    
    @Override
    protected void onPause() {
    	isForeground = false;
    	super.onPause();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	isForeground = true;
    	if (needFlush) {
    		flushRecords();
    		needFlush = false;
    	}
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
    			startService(serviceIntent);
    		else 
    			stopService(serviceIntent);
    	}
    };
    
    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
    	@Override
    	public void onReceive(Context context, Intent intent){
    		if (isForeground) {
    			flushRecords();
    			needFlush = false;
    		}
    		else
    			needFlush = true;
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
		Cursor cursor = db.query(true, "records", new String[] {"number", "content", "_id"}, null, null, null, null, "_id desc", null, null);
		
		while(cursor.moveToNext()) {
			Map<String, Object> mp = new HashMap<String, Object>(); 
			mp.put("number", cursor.getString(cursor.getColumnIndex("number")));
			mp.put("content", cursor.getString(cursor.getColumnIndex("content")));
			mp.put("_id", cursor.getString(cursor.getColumnIndex("_id")));
			list.add(mp);
		}
		return list;
	}
    
    private void flushRecords() {
         SimpleAdapter adapter = new SimpleAdapter(this, getData(), R.layout.record_item,
         		new String[] {"number", "content", "_id"}, new int[] {R.id.record_title, R.id.record_info, R.id.hided_record_id});
         recordsListView.setAdapter(adapter);
    }
    
    private String getRecordDate(int _id) {
    	Cursor cursor = db.query("records", new String[] {"date"}, "_id=(?)", new String[] {_id + ""}, null, null, null);
    	if (cursor.moveToNext()) {
    		return cursor.getString(cursor.getColumnIndex("date"));
    	}
    	else 
    		return "";
    }
    
    private OnItemClickListener recordItemClickListener = new OnItemClickListener() {
    	@Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
    		String title = ((TextView)view.findViewById(R.id.record_title)).getText().toString();
    		String info = ((TextView)view.findViewById(R.id.record_info)).getText().toString();
    		int _id = Integer.parseInt(((TextView)view.findViewById(R.id.hided_record_id)).getText().toString());
            //Toast.makeText(getBaseContext(), title + "\n" + info, Toast.LENGTH_SHORT).show();
            dialogLayout = getLayoutInflater().inflate(R.layout.record_dialog, (ViewGroup)findViewById(R.id.record_dialog), false);
            ((TextView)dialogLayout.findViewById(R.id.record_phonenumber_textedit)).setText(title);
            ((TextView)dialogLayout.findViewById(R.id.record_date_textedit)).setText(getRecordDate(_id));
            ((TextView)dialogLayout.findViewById(R.id.record_content_textedit)).setText(info);
            
            new AlertDialog.Builder(MainActivity.this).setTitle("Record Detail").setPositiveButton("OK", null).setNegativeButton("Repeat", repeatOnClickListener).setView(dialogLayout).show();
	
    	}
	};
	
	private DialogInterface.OnClickListener repeatOnClickListener = new DialogInterface.OnClickListener() {
		
		@Override
		public void onClick(DialogInterface arg0, int arg1) {
			Intent serviceIntent = new Intent(MainActivity.this, SmsService.class);
			String source = ((EditText)dialogLayout.findViewById(R.id.record_phonenumber_textedit)).getText().toString();
			String msg = ((EditText)dialogLayout.findViewById(R.id.record_content_textedit)).getText().toString();
			serviceIntent.putExtra("source", source);
			serviceIntent.putExtra("msg", msg);
			startService(serviceIntent);
			arg0.cancel();
		}
	};
    
    
}


