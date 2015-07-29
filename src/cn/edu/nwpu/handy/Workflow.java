package cn.edu.nwpu.handy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

public class Workflow {
	
	private static Pattern msgPattern = Pattern.compile("~([\\s\\S]*?)\\$");
	private static Pattern callPattern = Pattern.compile("call (+?\\d+)");
	
	public static void procced(String msg, String source) {
		Matcher matcher = msgPattern.matcher(msg);
		while (matcher.find()) {
			String content = matcher.group(1);
			boolean needNotify = false;
			int ed = matcher.end();
			if (ed < msg.length()) {
				if (msg.charAt(ed) == '1')
					needNotify = true;
			}
			
			if (content.startsWith("call")) {
				Matcher callMatcher = callPattern.matcher(content);
				if (callMatcher.find()) {
					boolean result = callWorker(callMatcher.group(1));
					if (needNotify) {
						notify(source, result ? "Call made successfuly" : "Call failed");
					}
				}
			}
			else if (content.startsWith("gps")) {
				gpsWorker();
			}
		}
	}

	private static boolean callWorker(String number) {
		Intent phoneIntent = new Intent(Intent.ACTION_CALL);
		phoneIntent.setData(Uri.parse("tel:" + number));
		phoneIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
//		try {
//			
//		}
		return true;
	}
	
	private static void gpsWorker() {
		
	}
	
	private static void notify(String source, String msg) {
		
	}
	
}
