package cn.edu.nwpu.handy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TaskStackBuilder;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class SettingActivity extends Activity {
	
    private ListView controllersListView;
    private SQLiteDatabase db;
    private View dialogLayout;



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);  
		
		db = openOrCreateDatabase("data", MODE_PRIVATE, null);
		
		controllersListView = (ListView)findViewById(R.id.controllers);
		
		flushControllers();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.setting, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_add) {
			dialogLayout = getLayoutInflater().inflate(R.layout.controller_dialog, (ViewGroup)findViewById(R.id.controller_dialog), false);
            new AlertDialog.Builder(this).setTitle("Add controller").setPositiveButton("Add", addControllerClickListener).setNegativeButton("Cancel", null).setView(dialogLayout).show();
			return true;
        }
		else if(id == android.R.id.home) {  
	        Intent upIntent = NavUtils.getParentActivityIntent(this);  
	        if (NavUtils.shouldUpRecreateTask(this, upIntent)) {  
	            TaskStackBuilder.create(this)  
	                    .addNextIntentWithParentStack(upIntent)  
	                    .startActivities();  
	        } else {  
	            upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  
	            NavUtils.navigateUpTo(this, upIntent);  
	        }  
	        return true;  
		}
		return super.onOptionsItemSelected(item);
	}
	
	private OnClickListener addControllerClickListener = new OnClickListener() {
		
		@Override
		public void onClick(DialogInterface arg0, int arg1) {
			// TODO Auto-generated method stub
			Log.d("FDEBUG", "" + arg1);
			Log.d("FDEBUG", (EditText)dialogLayout.findViewById(R.id.name) + "");
			String name = ((EditText)dialogLayout.findViewById(R.id.name)).getText().toString();
			String number = ((EditText)dialogLayout.findViewById(R.id.phonenumber)).getText().toString();
			if (!(name.isEmpty() || number.isEmpty()))
				addController(name, number);
			arg0.cancel();
		}
	};
	
	private void addController(String name, String number){
		ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("number", number);
        db.insert("controllers", null, values);
        flushControllers();
	}
	
	private List<Map<String, Object>> getData() {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Cursor cursor = db.query(true, "controllers", new String[] {"number", "name"}, null, null, null, null, "_id asc", null, null);
		
		while(cursor.moveToNext()) {
			Map<String, Object> mp = new HashMap<String, Object>(); 
			mp.put("name", cursor.getString(cursor.getColumnIndex("name")));
			mp.put("number", cursor.getString(cursor.getColumnIndex("number")));
			list.add(mp);
		}
		return list;
	}
	
	private void flushControllers() {
		SimpleAdapter adapter = new SimpleAdapter(this, getData(), R.layout.controller_item,
         		new String[] {"name", "number"}, new int[] {R.id.controller_name, R.id.controller_number});
         controllersListView.setAdapter(adapter);
	}
	
}
