package com.smartworld.signinsystem;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class WaitActivity extends Activity {
	private static final String TAG = "WaitActivity";
	private static final int TAKE_PICTURE = 0;
	private static final int CROP_PICTURE = 1;
	private Uri bigPictureUri;
	private Uri smallPictureUri;
	private File bigPicture;
	private static final String BIG_PICTURE_LOCATION = "file:///sdcard/big_picture.jpg";
	private static final String SMALL_PICTURE_LOCATION = "file:///sdcard/small_picture.jpg";
//	private ImageView imageView;
	private WifiManager wifiManager;
	private ProgressBar pb;
	private Button button;
	private Thread thread;
	private ListView listView;
	private List<String> names;
	private MyAdapter adapter;
	private boolean smallPictureCanBeClicked = false;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wait);
		pb = (ProgressBar) findViewById(R.id.progressbar);
		button = (Button) findViewById(R.id.end);
		listView = (ListView) findViewById(R.id.listview);
		names = new ArrayList<String>();
		adapter = new MyAdapter(this,1,names);
		listView.setAdapter(adapter);
		createHotSpot();

//		smallPictureUri = Uri.parse(SMALL_PICTURE_LOCATION);
//		getPhoto();

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
				bigPictureUri = Uri.parse(BIG_PICTURE_LOCATION);
				File bigPicture = new File(bigPictureUri.getPath());
				try {
					bigPicture.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
				File smallPicture = new File(smallPictureUri.getPath());
				fileChannelCopy(smallPicture, bigPicture);
				cropImage(smallPictureUri, 400, 400, CROP_PICTURE);
				break;
			}
			case CROP_PICTURE: {
				Log.d("WaitActivity", "crop picture success");
				Bitmap bitmap = decodeUriAsBitmap(smallPictureUri);

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
		intent.putExtra("aspectX", 2);
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

	private void createHotSpot(){
		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		WifiApAdmin wifiAp = new WifiApAdmin(this);
		try {
			wifiAp.startWifiAp("SignInSystem", "signinsystem");
			while (!WifiApAdmin.isWifiApEnabled(this));
			//new WifiAdmin(this);
			pb.setVisibility(View.VISIBLE);
			button.setEnabled(true);
			thread = new Thread(new Runnable() {
				@Override
				public void run() {
					ServerSocket service = null;

					try {
						service = new ServerSocket(30000);
					} catch (IOException e) {
						e.printStackTrace();
					}

					while (true) {
						//等待客户端连接
						Socket socket = null;
						try {
							socket = service.accept();
							new Thread(new ServiceThread(socket)).start();
						} catch (IOException e) {
							e.printStackTrace();
						}

					}

				}
			});
			thread.start();


		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			Toast.makeText(this, "您的手机出现故障，请重启手机及本应用", Toast.LENGTH_LONG);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			Toast.makeText(this, "您的手机出现故障，请重启手机及本应用", Toast.LENGTH_LONG);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			Toast.makeText(this, "您的手机出现故障，请重启手机及本应用", Toast.LENGTH_LONG);
		}

	}

	public void end(View view){
		smallPictureCanBeClicked = true;
		WifiApAdmin.closeWifiAp(this);
		pb.setVisibility(View.GONE);
		button.setEnabled(false);
		button.setText("签到结束");
	}



	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			WifiApAdmin.closeWifiAp(this);
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	class ServiceThread implements Runnable {

		// 定义当前线程处理的Socket
		Socket s = null;
		DataInputStream is = null;
		DataOutputStream dos = null;
		String name;

		public ServiceThread(Socket s){
			this.s = s;
			try {
				is = new DataInputStream(s.getInputStream());
				dos = new DataOutputStream(s.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			try {
				getData();
				dos.writeInt(200);
				dos.flush();
				is.close();
				dos.close();
				s.close();
			} catch (IOException e) {
				e.printStackTrace();
			}


			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					//names.add(name);
					adapter.add(name);
					Log.d(TAG,"notifyAdapter");
					adapter.notifyDataSetChanged();

				}
			});

		}

		private void getData() throws IOException {

			name = is.readUTF();
			Log.d(TAG,"receive name:"+name);
			getPicture(name + "_small_picture");
			getPicture(name+"_big_picture");


		}
		private void getPicture(String name) throws IOException {
			int size = is.readInt(); //得到byte的长度
			byte[] buffer = new byte[size];
			int len = 0;
			while(len < size){
				len += is.read(buffer, len, size-len);
			}
			Bitmap bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);

			if(bitmap != null){
				File f = new File("/sdcard/SignInSystem/", name+".jpg");
				if (f.exists()) {
					f.delete();
				}
				FileOutputStream fos = new FileOutputStream(f);
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

				fos.flush();
				fos.close();

				Log.d(TAG,"get picture success");
			}


		}
	}

	private class MyAdapter extends ArrayAdapter<String>{

		private LayoutInflater inflater;


		public MyAdapter(Context context, int resource, List<String> objects) {
			super(context, resource, objects);
			inflater = LayoutInflater.from(context);

		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView == null){
				convertView = inflater.inflate(R.layout.row,parent,false);
			}
			ViewHolder holder = (ViewHolder) convertView.getTag();
			if (holder == null) {
				holder = new ViewHolder();
				holder.name = (TextView) convertView.findViewById(R.id.name);
				holder.time = (TextView) convertView.findViewById(R.id.time);
				holder.avatar = (ImageView) convertView.findViewById(R.id.avatar);
				convertView.setTag(holder);
			}

			final String name = getItem(position);
			holder.name.setText(name);

			 Log.d(TAG,"getView:"+name);

			File file = new File("/sdcard/SignInSystem/", name+"_small_picture.jpg");
			FileInputStream fileStream = null;
			try {
				fileStream = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			Bitmap bitmap = BitmapFactory.decodeStream(fileStream);

			holder.avatar.setImageBitmap(bitmap);
			holder.avatar.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(smallPictureCanBeClicked)
						startActivity(new Intent(WaitActivity.this,ShowBigPictureActivity.class).putExtra("NAME",name));
				}
			});

			holder.time.setText(new SimpleDateFormat("yyyy年MM月dd日 hh:mm").format(new Date()));

			return convertView;

		}


	}
	private static class ViewHolder {
		TextView time;
		TextView name;
		ImageView avatar;


	}


}
