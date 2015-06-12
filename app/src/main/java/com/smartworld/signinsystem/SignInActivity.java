package com.smartworld.signinsystem;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class SignInActivity extends Activity {

	private static final String TAG = "SignInActivity";
	private static final int TAKE_PICTURE = 0;
	private static final int CROP_PICTURE = 1;
	private Uri bigPictureUri;
	private Uri smallPictureUri;
	private File bigPicture;
	private File smallPicture;
	private static final String DIR = "/sdcard/SignInSystem";
	private static final String BIG_PICTURE_LOCATION = "/sdcard/SignInSystem/big_picture.jpg";
	private static final String SMALL_PICTURE_LOCATION = "/sdcard/SignInSystem/small_picture.jpg";
	private ProgressDialog pd;
	private WifiAdmin wifiAdmin;
	private String name;
	private TextView textView;
	private BroadcastReceiver scanReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			unregisterReceiver(scanReceiver);
			receiveScanResult();
		}
	};

	private BroadcastReceiver connectReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			Log.d(TAG,"DetailState:"+info.getDetailedState());
			if (info.getDetailedState().equals(NetworkInfo.DetailedState.CONNECTED)){
				unregisterReceiver(connectReceiver);
				isConnected();
			}
			//isConnected();
		}
	};

	private void isConnected() {
		Log.d("SignInSystem","wifiConnected");
		name = getIntent().getStringExtra("NAME");
		int ipAddr;
		ipAddr = wifiAdmin.getServerAddr();
		Log.d(TAG,"ipAddr:"+ipAddr);
		String ipStr = int2str(ipAddr);
		Log.d(TAG,"ipStr:"+ipStr);

		Log.d(TAG,"myIP:"+int2str(wifiAdmin.getIPAddress()));
		new Thread(new ClientThread(ipStr)).start();
	}

	private void receiveScanResult(){
		List<ScanResult> wifiList = wifiAdmin.getWifiList();
		int size = wifiList.size();
		Log.d(TAG,"WIFIList size:"+size);
		for (ScanResult wifi : wifiList){
			Log.d(TAG,"wifi SSID:"+wifi.SSID);
			if(wifi.SSID.equals("SignInSystem"))
				break;
			size--;
		}
		if(size == 0) {
			failToFindServer();
		} else {
			wifiAdmin.addNetwork(wifiAdmin.createWifiInfo("SignInSystem", "signinsystem", WifiAdmin.TYPE_WPA));
			registerReceiver(connectReceiver,new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
		}

	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sign_in);
		textView = (TextView) findViewById(R.id.textview);

		File dir = new File(DIR);
		if (!dir.exists())
			dir.mkdirs();

		smallPicture = new File(SMALL_PICTURE_LOCATION);
		smallPictureUri = Uri.fromFile(smallPicture);
		bigPicture = new File(BIG_PICTURE_LOCATION);
		bigPictureUri = Uri.fromFile(bigPicture);
		getPhoto();
		//signIn();
	}


	private void getPhoto() {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//action is capture
		intent.putExtra(MediaStore.EXTRA_OUTPUT, smallPictureUri);
		startActivityForResult(intent, TAKE_PICTURE);
	}

	private void choosePicture(){
		Intent intent = new Intent();
                /* 开启Pictures画面Type设定为image */
		intent.setType("image/*");
                /* 使用Intent.ACTION_GET_CONTENT这个Action */
		intent.setAction(Intent.ACTION_GET_CONTENT);
                /* 取得相片后返回本画面 */
		startActivityForResult(intent, 2);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		switch (requestCode) {
			case TAKE_PICTURE: {
				//bigPictureUri = Uri.parse(BIG_PICTURE_LOCATION);
				//bigPicture = new File(bigPictureUri.getPath());

				fileChannelCopy(smallPicture, bigPicture);
				cropImage(smallPictureUri, 180, 180, CROP_PICTURE);
				break;
			}
			case CROP_PICTURE: {
				Log.d("WaitActivity", "crop picture success");
				//Bitmap bitmap = decodeUriAsBitmap(smallPictureUri);
				signIn();
//				imageView.setImageBitmap(bitmap);
				break;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void cropImage(Uri uri, int outputX, int outputY, int requestCode) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(uri, "image/*");
		intent.putExtra("crop", "true");
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		intent.putExtra("outputX", outputX);
		intent.putExtra("outputY", outputY);
		intent.putExtra("scale", true);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
		intent.putExtra("return-data", false);
		intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
		intent.putExtra("noFaceDetection", true); // no face detection
		startActivityForResult(intent, requestCode);
	}

	public void fileChannelCopy(File s, File t) {
		FileInputStream fi = null;
		FileOutputStream fo = null;
		FileChannel in = null;
		FileChannel out = null;
		try {
			fi = new FileInputStream(s);
			fo = new FileOutputStream(t);
			in = fi.getChannel();// 得到对应的文件通道
			out = fo.getChannel();// 得到对应的文件通道
			in.transferTo(0, in.size(), out);// 连接两个通道，并且从in通道读取，然后写入out通道
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fi.close();
				in.close();
				fo.close();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private Bitmap decodeUriAsBitmap(Uri uri) {
		Bitmap bitmap = null;
		try {
			bitmap = BitmapFactory.decodeStream(getContentResolver()
					.openInputStream(uri));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		return bitmap;
	}

	private void signIn(){
		Log.d("SignInSystem","start signing in");
		pd =  new ProgressDialog(SignInActivity.this);

		pd.setCanceledOnTouchOutside(false);

		pd.setMessage("正在签到，请耐心等待。。。");
		pd.show();

		WifiApAdmin.closeWifiAp(this);
		wifiAdmin = new WifiAdmin(SignInActivity.this);
		wifiAdmin.openWifi();

		int time = 0;
		while(wifiAdmin.checkState()!= WifiManager.WIFI_STATE_ENABLED){
			Log.d(TAG, String.valueOf(wifiAdmin.checkState()));
			time++;
		}
		Log.d(TAG, String.valueOf(time));
		Log.d(TAG, "openWifi success");
		wifiAdmin.startScan();
		registerReceiver(scanReceiver,new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

	}



	private void failToFindServer() {
		Log.d(TAG,"failToFindServer");
		pd.dismiss();
		Toast.makeText(SignInActivity.this, "签到失败，不在签到地点", Toast.LENGTH_LONG);
		textView.setText("签到失败，请到达签到点再打开该软件进行签到");
		textView.setVisibility(View.VISIBLE);
	}

	/**
	 * 输入int 得到String类型的ip地址
	 * @param i
	 * @return
	 */
	private String int2str(int i){

		return (i & 0xFF)+ "." + ((i >> 8 ) & 0xFF) + "." + ((i >> 16 ) & 0xFF) +"."+((i >> 24 ) & 0xFF );

	}

	class ClientThread implements Runnable{
		// 定义当前线程处理的Socket
		String serverIP;
		Socket s = null;
		DataInputStream is = null;
		DataOutputStream dos = null;

		public ClientThread(String ipStr){
			serverIP = ipStr;

		}

		@Override
		public void run() {
			try {
				try {
					s = new Socket(serverIP, 30000);
					is = new DataInputStream(s.getInputStream());
					dos = new DataOutputStream(s.getOutputStream());
				} catch (IOException e) {
					e.printStackTrace();
				}

				sendData();

				int code = is.readInt();
				if(code == 200) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							pd.dismiss();
							textView.setVisibility(View.VISIBLE);
						}
					});
				}
				is.close();
				dos.close();
				s.close();
			} catch (IOException e) {
				e.printStackTrace();
			}


		}

		private void sendData() throws IOException {

			dos.writeUTF(name);
			dos.flush();
			sendPicture("small_picture");
			sendPicture("big_picture");


		}
		private void sendPicture(String filename) throws IOException {
			Log.d(TAG,"start sending the picture:"+filename);
			FileInputStream fileStream;
			if(filename.equals("small_picture"))
				fileStream = new FileInputStream(smallPicture);
			else
				fileStream = new FileInputStream(bigPicture);

			Bitmap bitmap = BitmapFactory.decodeStream(fileStream);

			//发送给客户端

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
			Log.i("SignInSystem", "baos.size() " + baos.size());
			dos.writeInt(baos.size());

			byte[] bytes = baos.toByteArray();
			dos.write(bytes);

			dos.flush();
			Log.d(TAG,"finish sending the picture:"+filename);


		}
	}


}
