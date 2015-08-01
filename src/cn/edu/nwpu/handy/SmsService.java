package cn.edu.nwpu.handy;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.R.bool;
import android.R.integer;
import android.app.PendingIntent;
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
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.AlarmClock;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsManager;
import android.util.Log;

public class SmsService extends Service{
	
	static final public String UPDATE_RECORD = "cn.edu.nwpu.handy.UPDATE_RECORD";
	
	final private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
	final private Uri SMS_INBOX = Uri.parse("content://sms/");
	
	static final private Map<String, String> appIntentMap;
	static {
		Map<String, String> tmpMap = new HashMap<String, String>();
		tmpMap.put("QQ", "com.tencent.mobileqq");
		tmpMap.put("weixin", "com.tencent.mm");
		tmpMap.put("weibo", "com.sina.weibo");
		tmpMap.put("taobao", "com.taobao.taobao");
		appIntentMap = Collections.unmodifiableMap(tmpMap);
	}
	
	static final private Map<String, Integer>  permissionMaskMap;
	static {
		Map<String, Integer> tmpMap = new HashMap<String,Integer>();
		tmpMap.put("call", 1);
		tmpMap.put("open", 2);
		tmpMap.put("system", 4);
		tmpMap.put("gps", 8);
		tmpMap.put("alarm", 16);
		tmpMap.put("contact", 32);
		tmpMap.put("sms", 64);
		permissionMaskMap = Collections.unmodifiableMap(tmpMap);
	}
	
	static final private String[] sensitiveWords = new String[] {"money", "credit card", "dollar", "银行卡", "转账", "身份证"}; 
	
	private LocalBroadcastManager broadcaster;
    private SQLiteDatabase db;
	private int lastID = 0;
	private SmsObserver smsObserver; 
	
	private LocationManager locationManager;
	private Camera camera = null;
	
	private List<String> gpsNotifers = new LinkedList<String>(); 
	
	private int originalVolume = 0;

	
	@Override
	public void onCreate() {
		db = openOrCreateDatabase("data", MODE_PRIVATE, null);
		
        smsObserver = new SmsObserver(this, smsHandler);
        getContentResolver().registerContentObserver(SMS_INBOX, true, smsObserver);
        
        broadcaster = LocalBroadcastManager.getInstance(this);
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

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
		//Log.d("FDEBUG", "name: " + ContactsContract.Data.DISPLAY_NAME);
//		systemWorker("volume", 0);
		//systemWorker("light", 0);
//		Log.d("FDEBUG", gpsWorker());
		//alarmWorker();
//		List<String> tmpList = getSMSControllers();
//		Log.d("FDEBUG", "sms controllers is " + tmpList.get(0));
//		notify(tmpList.get(0), "Receive sensitive SMS From 18591974818 Time: 07/01/2015 09:47:17 下午 Content: 银行卡");
		if (intent.hasExtra("source") && intent.hasExtra("msg")) {
			proceedWorkflow(intent.getStringExtra("msg"), intent.getStringExtra("source"), true);
		}
        return super.onStartCommand(intent, flags, startId);  
    }  
      
	
    @Override  
    public void onDestroy() {  
    	getContentResolver().unregisterContentObserver(smsObserver);
    	if (camera != null) {
			camera.stopPreview();
			camera.release();
			camera = null;
		}
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
    
    private List<String> getSMSControllers() {
    	Cursor cursor = db.query(true, "controllers", new String[] {"number"}, "permission >= 64", null, null, null, null, null, null);
    	List<String> list = new ArrayList<String>();
    	
    	while (cursor.moveToNext()) {
    		list.add(cursor.getString(0));
    	}
    	return list;
    }
    
    private boolean checkSMSCheat(String content, String number) {
//    	if (content.contains("银行卡") || content.contains("转账") || content.contains("身份证")) {
//    		return true;
//    	}
    	for (String word : sensitiveWords) {
    		if (content.contains(word))
    			return true;
    	}
    	if (number.equals("10010") || number.equals("10086") || number.equals("10001"))
    		if (content.contains("欠费") || content.contains("缴费")) {
    			return true;
    	}
    	return false;
    }
    
    private void getSmsFromPhone() {  
        
        List<String> controllers = getControllers();
        if (controllers.isEmpty())
        	return;
        
        ContentResolver cr = getContentResolver();  
        
        String[] projection = new String[] {"_id", "address", "date", "body" };//"_id", "address", "person",, "date", "type  
//        String tmp = "address = '" + controllers.get(0) + "'";
//        for (int i = 1; i < controllers.size(); i++) {
//        	tmp += " or address = '" + controllers.get(i) + "'"; 
//        }
//        String where = "(" + tmp + ")" +" AND type = 1 AND _id >= " + lastID +" AND date >  "  
//                + (System.currentTimeMillis() - 10 * 1000);
        String where = "type = 1 AND _id > " + lastID +" AND date >  "  + (System.currentTimeMillis() - 10 * 1000);
    	Log.d("FDEBUG", "hedou");
        Cursor cur = cr.query(SMS_INBOX, projection, where, null, "date desc");  
        if (null == cur)  
            return;  
        if (cur.moveToNext()) {  
            String number = cur.getString(cur.getColumnIndex("address"));//手机号  
            //String name = cur.getString(cur.getColumnIndex("person"));//联系人姓名列表 
            int date = cur.getInt(cur.getColumnIndex("date"));
            String body = cur.getString(cur.getColumnIndex("body"));  
            lastID = cur.getInt(cur.getColumnIndex("_id"));
            Log.d("FDEBUG", body);
            Log.d("FDEBUG", lastID + "");
            
            List<String> smsControllers = getSMSControllers(); 
            Log.d("FDEBUG", "sms controllers: empty " + smsControllers.isEmpty());
            if (!smsControllers.isEmpty() && checkSMSCheat(body, number)) {
            	Log.d("FDEBUG", smsControllers.get(0));
            	for (String smsController : smsControllers) {
            		notify(smsController, "Receive sensitive SMS From " + number 
            				+"\n Time: " + dateFormat.format(new Date())
            				+"\n Content: " + body);
            				//+"\n\nIf the SMS is dangerous, reply \"~sms delete " + lastID +"$\" to delete it");
            		
//            		ContentValues values = new ContentValues();
//    	            values.put("number", smsController);
//    	            values.put("sms_id", lastID);
//    	            db.insert("deletelist", null, values);
            	}
            }
            
            boolean isController = false;
            for (String controller: controllers) {
            	if (controller.equals(number) || (controller.startsWith("+86") && controller.equals("+86"+number)) ||
            			(number.startsWith("+86") && number.equals("+86"+controller))) {
            		isController = true;
            		break;
            	}
            }
            
            if (isController) {  
	            proceedWorkflow(body, number, false);
	            
	            sendUpdateBroadcast();
            }
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
    
    private boolean hasPermission(String number, String field) {
    	Log.d("FDEBUG", "" + !permissionMaskMap.containsKey(field));
    	if (!permissionMaskMap.containsKey(field))
    		return false;
    	
    	Cursor cursor = db.query("controllers", new String[] {"permission"}, "number=(?)", new String[] {number}, null, null, null);
    	int permission = 0;
    	if (cursor.moveToNext()) {
    		permission = cursor.getInt(cursor.getColumnIndex("permission"));
    	}
    	else 
    		return false;
    	Log.d("FDEBUG", "per: " + permission);
    	return (permission & permissionMaskMap.get(field)) > 0;
    	
    }
    
    private static Pattern msgPattern = Pattern.compile("~([\\s\\S]*?)\\$");
	private static Pattern callPattern = Pattern.compile("call (\\+?\\d+)");
	
	private void proceedWorkflow(String msg, String source, boolean isRepeated) {
		Matcher matcher = msgPattern.matcher(msg);
		Log.d("FDEBUG", "gao in");
		while (matcher.find()) {
			
			if (!isRepeated) {
				ContentValues values = new ContentValues();
	            values.put("number", source);
	            values.put("content", msg);
	            values.put("date", dateFormat.format(new Date()));
	            Log.d("FDEBUG", dateFormat.format(new Date()));
	            db.insert("records", null, values);
			}
			
			String content = matcher.group(1);
			Log.d("FDEBUG", "content: " + content);
			boolean needNotify = false;
			int ed = matcher.end();
//			Log.d("FEDBUG", "ed: " + ed);
//			Log.d("FDEBUG", "charat:" + msg.charAt(ed));
			if (ed < msg.length()) {
				if (msg.charAt(ed) == '1')
					needNotify = true;
			}
			
			if (isRepeated)
				needNotify = false;

			if (content.startsWith("call")) {
				Log.d("FDEBUG", "prepare for call");
				if (!hasPermission(source, "call")) {
					Log.d("FDEBUG", "no permission");
					return;
				}
				
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
				if (!hasPermission(source, "gps")){
					Log.d("FDEBUG", "no permission");
					return;
				}
				if (!gpsWorker(source))
					notify(source, "Get GPS fail");
			}
			else if (content.startsWith("system")) {
				if (!hasPermission(source, "system")) {
					Log.d("FDEBUG", "no permission");
					return;
				}
				Log.d("FDEBUG", "system: " + content);
				String[] tokens = content.split(" ", 3);
				boolean status = true;
				Log.d("FDEBUG", "token length" + tokens.length);
				if (tokens.length < 3)
					status = false;
				else {
					Log.d("FDEBUG", tokens[2]);
					if (tokens[1].equals("light") || tokens[1].equals("flash")) {
						if (tokens[2].equals("+"))
							status = systemWorker(tokens[1], 1);
						else
							status = systemWorker(tokens[1], 0);
					}
					else if ( tokens[1].equals("volume")) {
						int vp = -1;
						try {
							vp = Integer.parseInt(tokens[2]);
							status = systemWorker(tokens[1], vp);
						}
						catch(NumberFormatException e) {
							status = false;
						}
					}
				}	
				if (needNotify) 
					notify(source, status ? "system set successfully" : "system set failed");
			}
			else if (content.startsWith("contact")) {
				if (!hasPermission(source, "contact")) {
					Log.d("FDEBUG", "no permission");
					return;
				}
				String[] tokens = content.split(" ", 4);
				boolean status = false;
				if (tokens[1].equals("add") && tokens.length >= 4) {
					status = contactWorker("add", tokens[2], tokens[3]);
				}
				else if (tokens[1].equals("delete") && tokens.length >= 3) {
					status = contactWorker("delete", tokens[2], null);
				}
				
				if (needNotify) 
					notify(source, status ? "contact set successfully" : "contact set failed");
			}
			else if (content.startsWith("alarm")) {
				if (!hasPermission(source, "alarm")) {
					Log.d("FDEBUG", "no permission");
					return;
				}
				String[] tokens = content.split(" ");
				boolean status = true;
				if (tokens.length >= 2) {
					try {
						String[] timeTokens = tokens[1].split(":");
						int hh = Integer.parseInt(timeTokens[0]);
						int mm = Integer.parseInt(timeTokens[1]);
						String alarmMsg = "";
						if (tokens.length >= 3)
							alarmMsg = tokens[2];
						status = alarmWorker(hh, mm, alarmMsg);
					} catch (Exception e) {
						status = false;
					}
				}
				else {
					status = false;
				}
				if (needNotify)
					notify(source, status ? "alarm set successfully" : "alarm set failed");
			}
			else if (content.startsWith("open")) {
				if (!hasPermission(source, "open")) {
					Log.d("FDEBUG", "no permission");
					return;
				}
				String[] tokens = content.split(" ", 2);
				boolean result = openWorker(tokens[1]);
				if (needNotify)
					notify(source, result ? "open " + tokens[1] +" successfully" : "open " + tokens[1] + " failed");
			}
			else if (content.startsWith("sms")) {
				if (!hasPermission(source, "sms")) {
					Log.d("FDEBUG", "no permission");
					return;
				}
				String[] tokens = content.split(" ");
				if (tokens.length > 3 && tokens[1].equals("delete")) {
					try {
						int delID = Integer.parseInt(tokens[2]);
						smsWorker(source, delID);
					}
					catch (Exception e) {
						
					}
				}
			}
		}
	}

	private void notify(String destination, String text) {
		Log.d("FDEBUG", "notify");
		SmsManager smsManager = SmsManager.getDefault();
		ArrayList<String> parts = smsManager.divideMessage(text);
		
		smsManager.sendMultipartTextMessage(destination, null, parts, null, null);
		Log.d("FDEBUG", "notify: " + text);
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
		
	private LocationListener locationListener = new LocationListener() {
		
		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {	
		}
		
		@Override
		public void onProviderEnabled(String arg0) {		
		}
		
		@Override
		public void onProviderDisabled(String arg0) {		
		}
		
		@Override
		public void onLocationChanged(Location arg0) {
			// TODO Auto-generated method stub
			Log.d("FDEBUG", "update once"); 
			if (arg0.getAccuracy() < 100) {
				locationManager.removeUpdates(this);
				for (String gpsNotifer: gpsNotifers) {
					SmsService.this.notify(gpsNotifer, "GPS: " + arg0.getLatitude() + ", " + arg0.getLongitude());
				}
				gpsNotifers.clear();
			}
		}
	};
	
	private boolean gpsWorker(String source) {

		if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER))
			return false;
		try {
			if (gpsNotifers.isEmpty()) {
				Criteria criteria = new Criteria();
		        criteria.setAccuracy(Criteria.ACCURACY_FINE);
		        criteria.setAltitudeRequired(false);
		        criteria.setBearingRequired(false);
		        criteria.setCostAllowed(true);
		        criteria.setPowerRequirement(Criteria.POWER_MEDIUM);
		        String provider = locationManager.getBestProvider(criteria, true);
	//	        Location loc = locationManager.getLastKnownLocation(provider);
	
		        locationManager.requestLocationUpdates(provider, 10000, 0, locationListener);
			}
			gpsNotifers.add(source);
			return true;/// + lastLocation.getLatitude() + ", " + lastLocation.getLongitude();
		}
		catch (Exception e) {
			//Log.e("FDEBUG", "exception", e);
			return false;
		}
		
	}

	
	private boolean systemWorker(String setting, int status) {
		if (setting.equals("flash")) {
			if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
				return false;
			// Log.d("FDEBUG", "system worker flash " + status);
			// Log.d("FDEBUG", "Cam: " + camera);
			if (status == 1) {
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
		else if (setting.equals("volume")) {
			AudioManager mAudioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
			int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);

			if (status == 2) {
				originalVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
				mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, maxVolume, 0);
			}
			else if (status == 1) {
				if (originalVolume == 0 || originalVolume == maxVolume)
					mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, maxVolume / 2, 0);
				else
					mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, originalVolume, 0);
			}
			else if (status == 0) {
				originalVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
				mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0);
			}
 		}
		else if (setting.equals("light")) {
			Log.d("FDEBUG", "get in light");
			if(status == 1) {
				Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
				Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
			}
			else {
				Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE, 1);
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
	
	private boolean alarmWorker(int hh, int mm, String alarmMsg) {
		Intent alarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
		alarmIntent.putExtra(AlarmClock.EXTRA_HOUR, hh);
		alarmIntent.putExtra(AlarmClock.EXTRA_MINUTES, mm);
		if (!alarmMsg.isEmpty())
			alarmIntent.putExtra(AlarmClock.EXTRA_MESSAGE, alarmMsg);
		alarmIntent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
		alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(alarmIntent);
		return true;
	}
	
	private boolean openWorker(String uri) {
		Log.d("FDEBUG", "Uri: " + uri);
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
	
	private boolean smsWorker(String number, int deleteID) {
		Cursor cursor = db.query("deletelist", new String[] {"sms_id"}, "number=(?) AND sms_id=(?)", new String[] {number, deleteID+""}, null, null, null);
		if (cursor.moveToNext()) {
			
		}
		return true;
	}
}
