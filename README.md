# Handy
Android Client

Handy is an Android client allowing people use SMS instructions to control remote Android devices. 

It can receive SMS instructions to make dial, send SMS, get GPS location, control system (turn on/off camera flash just like torch app, change volume, change screen lightness, and etc.), open applications, open url, set alarmclock, add/delete contacts and etc.

One typical usage scenario is that install Handy on old people's Android phones, then children of those old people can send SMS instruction to those phones, help their parents accomplish some   jobs, such as setting alarmclock and add contacts.


---

Usage: [instructions]

- Make Call: ~call {phone number}$ 

		e.g. ~call 10010$
	
- Get GPS Location: ~gps$

- System Settings:
	
	Turn On Camera Flash: ~system flash +$
	
	Turn Off Camera Flash: ~system flash -$
	
	Turn Screen to Maximum Lightness: ~system light 2$
	
	Turn Screen to Auto Lightness: ~system light 1$
	
	Turn Screen to Minimum Lightness: ~system light 0$
	
	Turn System to Maximum Volume: ~system volume 2$
	
	Turn System to Auto Volume: ~system volume 1$
	
	Turn System to Minimum Volume: ~system volume 0$
	
- Edit Contacts:

	Add Contact: ~contact add {contact number} {contact name}$
	
		e.g. ~contact add 18512345678 张三$
	
	Delete Contact: ~contact delete {contact number}$
	
		e.g. ~contact delete 18512345678$
	
- Open URL: ~open {URL}$
	
		e.g. ~open http://blog.tpircsboy.com/$
	
	URL should begin with http or https 
	
- Open Applications:
	
	Open QQ: ~open QQ$
	
	Open WeChat: ~open weixin$
	
	Open Weibo: ~open weibo$
	
	Open Taobao: ~open taobao$
	
- Set AlarmClock

	Add AlarmClock: ~alarm {hour}:{minute} {title (can be empty)}$
	
		e.g. ~alarm 8:13 Morning$
		e.g. ~alarm 12:45$
	
		
Add "1" behind "$" to force client to send job status message back
		
		e.g. ~call 18512345678$1 
	
		if the call is made successfully, client will send success message back
	
		otherwise, it will send fail message back
		
		e.g. ~contact add 18512345678 张三$1

	
 	
	 
	