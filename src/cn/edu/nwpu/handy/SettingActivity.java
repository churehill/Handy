package cn.edu.nwpu.handy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.TaskStackBuilder;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;

public class SettingActivity extends Activity {
	private final static int SCANNIN_GREQUEST_CODE = 1;	
	
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
		controllersListView.setOnItemClickListener(controllerItemClickListener);
		
		((Button)findViewById(R.id.qr_add_btn)).setOnClickListener(QRAddControllerClickListener); 
		((Button)findViewById(R.id.add_controller_btn)).setOnClickListener(btnAddControllerClickListener);
		
		flushControllers();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.setting, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_add) {
			
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
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
		case SCANNIN_GREQUEST_CODE:
			if(resultCode == RESULT_OK){
				Bundle bundle = data.getExtras();
				//显示扫描到的内容
				//textView.setText(bundle.getString("result"));
				String number = bundle.getString("result");
				Log.d("FDEBUG", number);
				if (!PhoneNumberUtils.isGlobalPhoneNumber(number)) {
					new AlertDialog.Builder(this).setTitle("Fail").setMessage("Phone Number is invalid").setPositiveButton("OK", null).show();
					break;
				}
				
				dialogLayout = getLayoutInflater().inflate(R.layout.controller_dialog, (ViewGroup)findViewById(R.id.controller_dialog), false);
	            ((TextView)dialogLayout.findViewById(R.id.phonenumber)).setText(number);
				new AlertDialog.Builder(SettingActivity.this).setTitle("Add controller").setPositiveButton("Add", addControllerClickListener).setNegativeButton("Cancel", null).setView(dialogLayout).show();
			}
			break;
		}
    }	
	
	private View.OnClickListener QRAddControllerClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View arg0) {
			Intent intent = new Intent();
			intent.setClass(SettingActivity.this, MipcaActivityCapture.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivityForResult(intent, SCANNIN_GREQUEST_CODE);
			
		}
	}; 
	
	private View.OnClickListener btnAddControllerClickListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View arg0) {
			dialogLayout = getLayoutInflater().inflate(R.layout.controller_dialog, (ViewGroup)findViewById(R.id.controller_dialog), false);
            new AlertDialog.Builder(SettingActivity.this).setTitle("Add controller").setPositiveButton("Add", addControllerClickListener).setNegativeButton("Cancel", null).setView(dialogLayout).show();
		}
	};
	
	private OnClickListener addControllerClickListener = new OnClickListener() {
		
		@Override
		public void onClick(DialogInterface arg0, int arg1) {
			Log.d("FDEBUG", "" + arg1);
//			Log.d("FDEBUG", (EditText)dialogLayout.findViewById(R.id.name) + "");
			String name = ((EditText)dialogLayout.findViewById(R.id.name)).getText().toString();
			String number = ((EditText)dialogLayout.findViewById(R.id.phonenumber)).getText().toString();
			
			if (!PhoneNumberUtils.isGlobalPhoneNumber(number)) {
				new AlertDialog.Builder(SettingActivity.this).setTitle("Fail").setMessage("Phone Number is invalid").setPositiveButton("OK", null).show();
				return;
			}
			
			int permission = 0;
			if (((Switch)dialogLayout.findViewById(R.id.permission_call_switch)).isChecked())
				permission |= 1;
			if (((Switch)dialogLayout.findViewById(R.id.permission_open_switch)).isChecked())
				permission |= 2;
			if (((Switch)dialogLayout.findViewById(R.id.permission_system_switch)).isChecked())
				permission |= 4;
			if (((Switch)dialogLayout.findViewById(R.id.permission_gps_switch)).isChecked())
				permission |= 8;
			if (((Switch)dialogLayout.findViewById(R.id.permission_alarm_switch)).isChecked())
				permission |= 16;
			if (((Switch)dialogLayout.findViewById(R.id.permission_contact_switch)).isChecked())
				permission |= 32;
			if (((Switch)dialogLayout.findViewById(R.id.permission_sms_switch)).isChecked())
				permission |= 64;
			if (!(name.isEmpty() || number.isEmpty()))
				addController(name, number, permission);
			arg0.cancel();
		}
	};
	
	private OnClickListener editControllerClickListener = new OnClickListener() {
		
		@Override
		public void onClick(DialogInterface arg0, int arg1) {
			Log.d("FDEBUG", "" + arg1);
			String name = ((EditText)dialogLayout.findViewById(R.id.name)).getText().toString();
			String number = ((EditText)dialogLayout.findViewById(R.id.phonenumber)).getText().toString();
			int permission = 0;
			if (((Switch)dialogLayout.findViewById(R.id.permission_call_switch)).isChecked())
				permission |= 1;
			if (((Switch)dialogLayout.findViewById(R.id.permission_open_switch)).isChecked())
				permission |= 2;
			if (((Switch)dialogLayout.findViewById(R.id.permission_system_switch)).isChecked())
				permission |= 4;
			if (((Switch)dialogLayout.findViewById(R.id.permission_gps_switch)).isChecked())
				permission |= 8;
			if (((Switch)dialogLayout.findViewById(R.id.permission_alarm_switch)).isChecked())
				permission |= 16;
			if (((Switch)dialogLayout.findViewById(R.id.permission_contact_switch)).isChecked())
				permission |= 32;
			if (((Switch)dialogLayout.findViewById(R.id.permission_sms_switch)).isChecked())
				permission |= 64;
			int _id = Integer.parseInt(((TextView)dialogLayout.findViewById(R.id.hided_dialog_controller_id)).getText().toString());
			if (!(name.isEmpty() || number.isEmpty()))
				editController(_id, name, number, permission);
			arg0.cancel();
		}
	};
	
	private OnClickListener deleteControllerClickListener = new OnClickListener() {
		@Override
		public void onClick(DialogInterface arg0, int arg1) {
			int _id = Integer.parseInt(((TextView)dialogLayout.findViewById(R.id.hided_dialog_controller_id)).getText().toString());
			deleteController(_id);
			arg0.cancel();
		}
	};
	
	private OnItemClickListener controllerItemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			int _id = Integer.parseInt(((TextView)view.findViewById(R.id.hided_controller_id)).getText().toString());
			Cursor cursor = db.query(true, "controllers", new String[] {"number", "name", "permission"}, "_id = (?)", new String[] {_id + ""}, null, null, null, null, null);
			if (cursor.moveToNext()) {
				String name = cursor.getString(cursor.getColumnIndex("name"));
				String number = cursor.getString(cursor.getColumnIndex("number"));
				int permission = cursor.getInt(cursor.getColumnIndex("permission"));
				
				dialogLayout = getLayoutInflater().inflate(R.layout.controller_dialog, (ViewGroup)findViewById(R.id.controller_dialog), false);
	        
				((TextView)dialogLayout.findViewById(R.id.name)).setText(name);
				((TextView)dialogLayout.findViewById(R.id.phonenumber)).setText(number);
				((TextView)dialogLayout.findViewById(R.id.hided_dialog_controller_id)).setText("" + _id);
				if ((permission & 1) != 0) 
					((Switch)dialogLayout.findViewById(R.id.permission_call_switch)).setChecked(true);
				if ((permission & 2) != 0) 
					((Switch)dialogLayout.findViewById(R.id.permission_open_switch)).setChecked(true);
				if ((permission & 4) != 0) 
					((Switch)dialogLayout.findViewById(R.id.permission_system_switch)).setChecked(true);
				if ((permission & 8) != 0) 
					((Switch)dialogLayout.findViewById(R.id.permission_gps_switch)).setChecked(true);
				if ((permission & 16) != 0) 
					((Switch)dialogLayout.findViewById(R.id.permission_alarm_switch)).setChecked(true);
				if ((permission & 32) != 0) 
					((Switch)dialogLayout.findViewById(R.id.permission_contact_switch)).setChecked(true);
				if ((permission & 64) != 0) 
					((Switch)dialogLayout.findViewById(R.id.permission_sms_switch)).setChecked(true);
				
				new AlertDialog.Builder(SettingActivity.this).setTitle("Edit controller").setPositiveButton("OK", editControllerClickListener).setNegativeButton("Delete", deleteControllerClickListener).setView(dialogLayout).show();
			
			}
		}
	};
	
	private void addController(String name, String number, int permission){
		ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("number", number);
        values.put("permission", permission);
        db.insert("controllers", null, values);
        flushControllers();
	}
	
	private void editController(int _id, String name, String number, int permission){
		String old_name;
		String old_number;
		int old_permission;
		
		Cursor cursor = db.query(true, "controllers", new String[] {"number", "name", "permission"}, "_id = (?)", new String[] {_id + ""}, null, null, null, null, null);
		if (cursor.moveToNext()) {
			old_name = cursor.getString(cursor.getColumnIndex("name"));
			old_number = cursor.getString(cursor.getColumnIndex("number"));
			old_permission = cursor.getInt(cursor.getColumnIndex("permission"));
		}
		else 
			return;
		boolean visible_change = false;
		if (old_name.equals(name) && old_number.equals(number)) {
			if (old_permission == permission)
				return;
		}
		else {
			visible_change = true;
		}
		
		ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("number", number);
        values.put("permission", permission);
        db.update("controllers", values, "_id = (?)", new String[] {_id + ""});
        
        if (visible_change)
        	flushControllers();
	}
	
	private void deleteController(int _id){
		db.delete("controllers", "_id = (?)", new String[] {_id + ""});
        flushControllers();
	}

	private List<Map<String, Object>> getData() {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Cursor cursor = db.query(true, "controllers", new String[] {"number", "name", "_id"}, null, null, null, null, "_id asc", null, null);
		
		while(cursor.moveToNext()) {
			Map<String, Object> mp = new HashMap<String, Object>(); 
			mp.put("name", cursor.getString(cursor.getColumnIndex("name")));
			mp.put("number", cursor.getString(cursor.getColumnIndex("number")));
			mp.put("_id", cursor.getInt(cursor.getColumnIndex("_id")) + "");
			list.add(mp);
		}
		return list;
	}
	
	private void flushControllers() {
		SimpleAdapter adapter = new SimpleAdapter(this, getData(), R.layout.controller_item,
         		new String[] {"name", "number", "_id"}, new int[] {R.id.controller_name, R.id.controller_number, R.id.hided_controller_id});
         controllersListView.setAdapter(adapter);
	}
	
}
