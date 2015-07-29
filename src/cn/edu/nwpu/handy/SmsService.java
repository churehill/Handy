package cn.edu.nwpu.handy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlarmManager;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsManager;
import android.util.Log;

public class SmsService extends Service{
	
	static final public String UPDATE_RECORD = "cn.edu.nwpu.handy.UPDATE_RECORD";
	
	final private Uri SMS_INBOX = Uri.parse("content://sms/");
	
	static final private Map<String, String> appIntentMap;
	static {
		Map<String, String> tmpMap = new HashMap<String, String>();
		tmpMap.put("QQ", "com.tencent.mobileqq");
		tmpMap.put("weixin", "com.tencent.mm");
		tmpMap.put("weibo", "com.sina.weibo");
		appIntentMap = Collections.unmodifiableMap(tmpMap);
	}
	
	private LocalBroadcastManager broadcaster;
    private SQLiteDatabase db;
	private int lastID = 0;
	private SmsObserver smsObserver; 
	
	private LocationManager locationManager;
	private Camera camera = null;

	
	@Override
	public void onCreate() {
		db = openOrCreateDatabase("data", MODE_PRIVATE, null);
		
        smsObserver = new SmsObserver(this, smsHandler);
        getContentResolver().registerContentObserver(SMS_INBOX, true, smsObserver);
        
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        
        broadcaster = LocalBroadcastManager.getInstance(this);
        
	}
	
	@Override  
    public int onStartCommand(Intent intent, int flags, int startId) {  
		Log.d("FDEBUG", "service start");
		// systemWorker("flash", true);
//		proceedWorkflow("~system flash +$", "18591974818");
//		new Thread(new Runnable() {
//			
//			@Override
//			public void run() {
//				try {
//					Thread.sleep(1000);
//					Log.d("FDEBUG", "flash -");
//					proceedWorkflow("~system flash -$", "18591974818");
//				}
//				catch (Exception e) {
//					e.printStackTrace();
//				}
//				
//			}
//		}).start();
		//openWorker("http://www.baidu.com/");
		//openWorker("weibo");
		//contactWorker("add", "18591974819", "本人++");
		Log.d("FDEBUG", "name: " + ContactsContract.Data.DISPLAY_NAME);
        return super.onStartCommand(intent, flags, startId);  
    }  
      
    @Override  
    public void onDestroy() {  
    	getContentResolver().unregisterContentObserver(smsObserver);
        super.onDestroy();  
    }  
  
    @Override  
    public IBinder onBind(Intent intent) {  
        return null;  
    }  
    
    private void sendUpdateBroadcast() {
    	Intent intent = new Intent(UPDATE_RECORD);
    	broadcaster.sendBroadcast(intent);
    }
    
    private Handler smsHandler = new Handler() {  
        //这里可以进行回调的操作  
        //TODO  
  
    };  
    
    private List<String> getControllers() {
    	Cursor cursor = db.query(true, "controllers", new String[] {"number"}, null, null, null, null, null, null, null);
    	List<String> list = new ArrayList<String>();
    	
    	while (cursor.moveToNext()) {
    		list.add(cursor.getString(0));
    	}
    	return list;
    }
    
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
        String where = "(" + tmp + ")" +" AND type = 1 AND _id > " + lastID +" AND date >  "  
                + (System.currentTimeMillis() - 10 * 1000);
//        String where = tmp;
    	Log.d("FDEBUG", "hedou");
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
            
            ContentValues values = new ContentValues();
            values.put("number", number);
            values.put("content", body);
            db.insert("records", null, values);
            
            proceedWorkflow(body, number);
            
            sendUpdateBroadcast();
        }  
        cur.close();
        Log.d("FDEBUG", "wocao");
    } 
    
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
    
    private static Pattern msgPattern = Pattern.compile("~([\\s\\S]*?)\\$");
	private static Pattern callPattern = Pattern.compile("call (\\+?\\d+)");
	
	private void proceedWorkflow(String msg, String source) {
		Matcher matcher = msgPattern.matcher(msg);
		Log.d("FDEBUG", "gao in");
		while (matcher.find()) {
			String content = matcher.group(1);
			Log.d("FDEBUG", "content: " + content);
			boolean needNotify = false;
			int ed = matcher.end();
			if (ed < msg.length()) {
				if (msg.charAt(ed) == '1')
					needNotify = true;
			}
			
			if (content.startsWith("call")) {
				Log.d("FDEBUG", "prepare for call");
				Matcher callMatcher = callPattern.matcher(content);
				if (callMatcher.find()) {
					boolean result = callWorker(callMatcher.group(1));
					Log.d("FEBUG", "" + needNotify);
					if (needNotify) {
						notify(source, result ? "Call made successfully" : "Call failed");
					}
				}
			}
			else if (content.startsWith("gps")) {
				notify(source, gpsWorker());
			}
			else if (content.startsWith("system")) {
				Log.d("FDEBUG", "system: " + content);
				String[] tokens = content.split(" ", 3);
				boolean status = true;
				if (tokens.length < 3)
					status = false;
				else {
					Log.d("FDEBUG", tokens[2]);
					if (tokens[1].equals("light") || tokens[1].equals("volume") || tokens[1].equals("flash")) {
						if (tokens[2].equals("+"))
							status = systemWorker(tokens[1], true);
						else
							status = systemWorker(tokens[1], false);
					}
				}	
				if (needNotify) 
					notify(source, status ? "system set successfully" : "system set failed");
			}
			else if (content.startsWith("contact")) {
				String[] tokens = content.split(" ", 4);
				if (tokens[1].equals("add") && tokens.length >= 4) {
					contactWorker("add", tokens[2], tokens[3]);
				}
				else if (tokens[1].equals("delete") && tokens.length >= 3) {
					contactWorker("delete", tokens[2], null);
				}
			}
			else if (content.startsWith("alarm")) {
				
			}
			else if (content.startsWith("open")) {
				String[] tokens = content.split(" ", 2);
				boolean result = openWorker(tokens[1]);
				if (needNotify)
					notify(source, result ? "open " + tokens[1] +" successfully" : "open " + tokens[1] + " failed");
			}
		}
	}

	private boolean callWorker(String number) {
		Log.d("FDEBUG", "get in call");
		Intent phoneIntent = new Intent(Intent.ACTION_CALL);
		phoneIntent.setData(Uri.parse("tel:" + number));
		phoneIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		try {
			startActivity(phoneIntent);
		}
		catch (android.content.ActivityNotFoundException ex) {
			return false;
		}
		return true;
	}
	
	private String gpsWorker() {
		if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER))
			return "GPS is disabled";
		try {
			Criteria criteria = new Criteria();
	        criteria.setAccuracy(Criteria.ACCURACY_FINE);
	        criteria.setAltitudeRequired(false);
	        criteria.setBearingRequired(false);
	        criteria.setCostAllowed(true);
	        criteria.setPowerRequirement(Criteria.POWER_MEDIUM);
	        String provider = locationManager.getBestProvider(criteria, true);
	        Location loc = locationManager.getLastKnownLocation(provider);
			return "GPS: " + loc.getLatitude() + ", " + loc.getLongitude();
		}
		catch (Exception e) {
			return "get GPS fail";
		}
	}
	
	private void notify(String destination, String text) {
		Log.d("FDEBUG", "notify");
		SmsManager.getDefault().sendTextMessage(destination, null, text, null, null);
		Log.d("FDEBUG", "notify: " + text);
	}
	
	private boolean systemWorker(String setting, boolean status) {
		if (setting.equals("flash")) {
			if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
				return false;
			// Log.d("FDEBUG", "system worker flash " + status);
			// Log.d("FDEBUG", "Cam: " + camera);
			if (status) {
				// Log.d("FBEBUG", "begin flash");
				camera = Camera.open();
				Parameters p = camera.getParameters();
				p.setFlashMode(Parameters.FLASH_MODE_TORCH);
				camera.setParameters(p);
				camera.startPreview();
			}
			else {
				if (camera != null) {
					camera.stopPreview();
					camera.release();
					camera = null;
				}
			}
		}
		return true;
	}
	
	private boolean contactWorker(String action, String number, String name) {
		if (action.equals("add")) {
			ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
			int rawContactInsertIndex = ops.size();
			ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
	                .withValue(RawContacts.ACCOUNT_TYPE, null)
	                .withValue(RawContacts.ACCOUNT_NAME, null)
	                .build());
			ops.add(ContentProviderOperation
                    .newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID,rawContactInsertIndex)
                    .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(StructuredName.DISPLAY_NAME, name) // Name of the person
                    .build());
			ops.add(ContentProviderOperation
                    .newInsert(Data.CONTENT_URI)
                    .withValueBackReference(
                            ContactsContract.Data.RAW_CONTACT_ID,   rawContactInsertIndex)
                    .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    .withValue(Phone.NUMBER, number) // Number of the person
                    .withValue(Phone.TYPE, Phone.TYPE_MOBILE).build()); // Type of mobile number   
			try {
				ContentProviderResult[] res = getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
				if (res != null && res[0] != null) {
					Log.d("FDEBUG", "URI add contact: " + res[0].uri);
					return true;
				}
				else {
					return false;
				}
			}
			catch (RemoteException e){
				return false;
			}
			catch (OperationApplicationException e) {
				return false;
			}
		}
		else if (action.equals("delete")) {
			Log.d("FDEBUG", "get in delete");
			Cursor cursor = getContentResolver().query(Data.CONTENT_URI, new String[] {Data.RAW_CONTACT_ID}, Data.HAS_PHONE_NUMBER + " > 0 AND " + Phone.NUMBER + " = ? ", new String[] {number}, null);
			if (cursor.moveToNext()) {
				int rawID = cursor.getInt(cursor.getColumnIndex(Data.RAW_CONTACT_ID));
				Log.d("FDEBUG", "id: " + rawID); 
				getContentResolver().delete(RawContacts.CONTENT_URI, RawContacts._ID + "= ?", new String[] {"" + rawID});
				return true;
			}
			else {
				return false;
			}
		}
		return true;
	}
	
	private boolean alarmWorker() {
		return true;
	}
	
	private boolean openWorker(String uri) {
		if (uri.startsWith("http")) {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
			browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(browserIntent);
			return true;
		}
		if (appIntentMap.containsKey(uri)) {
			try {
				Intent appIntent = getPackageManager().getLaunchIntentForPackage(appIntentMap.get(uri));
				if (appIntent == null)
					return false;
				appIntent.addCategory(Intent.CATEGORY_LAUNCHER);
				appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(appIntent);
				return true;
			}
			catch(ActivityNotFoundException e) {
				Log.d("FDEBUG", "unable to open " + appIntentMap.get(uri));
				return false;
			}
		}
		return false;
	}
}
