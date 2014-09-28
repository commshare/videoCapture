package cn.itcast.h264test;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
 
public class MainActivity extends Activity implements Callback, Runnable{
 
    private static final String TAG = "ZBCamera";
    //��ʼ��MediaRecorder
    private MediaRecorder mMediaRecorder = null;
    private int videoWidth = 640;//320;
    private int videoHeight = 480;//240;
    private int videoRate = 15;
    
    private Camera camera = null;
	private static String SDCARD_PATH=Environment.getExternalStorageDirectory().getPath();

    private String fd = "data/data/cn.itcast.h264test/h264.3gp";
    private File file =null;
    
    private PowerManager.WakeLock wl;
    private Button mBtnStartRecordVideo;
    private Button mBtnStopRecordVideo; 
    private Button mBtnStartCamera; 
    private Button mBtnStopCamera; 
    private Button mBtnAutoRecord;
    
    private boolean mIsRecording=false;
    
    private AbstractTimer mAutoRecordTimer;  
    private Handler mHandler;
	private final static int AUTO_RECORD_ONE_MINUTES = 0x0001;
	private static int count=5;
    
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        InitSurfaceView();			//��ʼ�����Ž���
        InitMediaSharePreference();	//��ʼ�����Լ�¼��
        initData();
        
        //��Դ����
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //SCREEN_DIM_WAKE_LOCK����CPU ��ת����������Ļ��ʾ���п����ǻҵģ�����رռ��̵�
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "net.majorkernelpanic.spydroid.wakelock");
        //PARTIAL_WAKE_LOCK:����CPU ��ת����Ļ�ͼ��̵��п����ǹرյġ�
    }
 
    private void initData() {
		// TODO Auto-generated method stub
    	mAutoRecordTimer= new SingleSecondTimer(this);
    	mHandler=new Handler(){

			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				//super.handleMessage(msg);
				switch(msg.what){
				case AUTO_RECORD_ONE_MINUTES:
					if(count>0)
					{
						Log.e(TAG, "----- count["+count+"]----------");
						recordEveryMinute();
						count--;
					}else{
						mAutoRecordTimer.stopTimer();
						mBtnAutoRecord.setEnabled(true);
					}
					break;
				default:
					break;
				}
			}
    		
    	};
		mAutoRecordTimer.setHandler(mHandler, AUTO_RECORD_ONE_MINUTES);
	}

    private boolean mIsFirstStart=true;
	protected void recordEveryMinute() {
		// TODO Auto-generated method stub
		if(mIsFirstStart)
		{
			Log.e(TAG,"first record");
			startRecordVideo();
			mIsFirstStart=false;
		
		}
		else
		{
			stopRecordVideo();
			startRecordVideo();
		}
	}

	private void initView() {
		// TODO Auto-generated method stub
    	mBtnStartRecordVideo=(Button)findViewById(R.id.btn_VideoStart);
    	mBtnStopRecordVideo=(Button)findViewById(R.id.btn_VideoStop);
    	mBtnStartCamera=(Button)findViewById(R.id.btn_CameraStart);
    	mBtnStopCamera=(Button)findViewById(R.id.btn_CameraStop);
    	mBtnAutoRecord=(Button)findViewById(R.id.btn_AutoRecord);

		mBtnStartRecordVideo.setOnClickListener(click);
    	mBtnStopRecordVideo.setOnClickListener(click);
    	mBtnStartRecordVideo.setEnabled(false);
    	mBtnStopRecordVideo.setEnabled(false);
    	mBtnStartCamera.setOnClickListener(click);
    	mBtnStopCamera.setOnClickListener(click);
    	mBtnAutoRecord.setOnClickListener(click);
    	mBtnStopCamera.setEnabled(false);
    	mBtnStartCamera.setEnabled(false);
	}

    private View.OnClickListener click=new OnClickListener(){

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch(v.getId()){
				case R.id.btn_VideoStart:
					startRecordVideo();
					break;
				case R.id.btn_VideoStop:
					stopRecordVideo();
					break;
				case R.id.btn_CameraStart:
					startCamera();
					break;
				case R.id.btn_CameraStop:
					//Ӧ���ͷ�������Դ
					stopCamera();
					break;
				case R.id.btn_AutoRecord:
					autoRecord();
					break;
				default:
					break;
			}
		}    	
    };

	protected void stopCamera() {
		// TODO Auto-generated method stub
//		closeCamera();
//		mBtnStopCamera.setEnabled(false);
		
		//�ͷ�������Դ
		mIsRecording=false;
		mMediaRecorderRecording=false;
		
		releaseMediaRecorder();
		mBtnStartCamera.setEnabled(true);
    	mBtnStartRecordVideo.setEnabled(false);
    	mBtnStopRecordVideo.setEnabled(false);

	}

	protected void autoRecord() {
		// TODO Auto-generated method stub
		Log.e(TAG,"auto record staretd...");
		mAutoRecordTimer.startTimer();
		//�ȿ���ʱ����Ȼ��ʼ¼��
		//startRecordVideo();
		mBtnAutoRecord.setEnabled(false);
	}

	protected void startCamera0() {
		// TODO Auto-generated method stub
    	//������ͷ����ʼ��¼�񻷾�
        boolean ret=initializeVideo();

        if(!ret)
        {
        	return;
        }
        mMediaRecorderRecording = true;
        //
		Toast.makeText(MainActivity.this, "��ʼ��¼��ok", 0).show();

    	//����¼����
		mBtnStartCamera.setEnabled(false);
    	mBtnStartRecordVideo.setEnabled(true);
    	//���Թر�����ͷ��
    	mBtnStopCamera.setEnabled(true);
	}
	private boolean mIsInitCamera=false;

	boolean initCamera(){
		if(mSurfaceHolder == null) 
		{
			return false;
		}
		createCamera();
		mIsInitCamera=true;
		return true;
	}
	protected void startCamera() {
		// TODO Auto-generated method stub
    	//������ͷ����ʼ��¼�񻷾�
        boolean ret=initCamera();

        if(!ret)
        {
        	return;
        }
        mMediaRecorderRecording = true;
        //
		Toast.makeText(MainActivity.this, "��ʼ��¼��ok", 0).show();

    	//����¼����
		mBtnStartCamera.setEnabled(false);
    	mBtnStartRecordVideo.setEnabled(true);
    	//���Թر�����ͷ��
    	mBtnStopCamera.setEnabled(true);
	}
	protected void startCamera01() {
		// TODO Auto-generated method stub
    	//������ͷ����ʼ��¼�񻷾�
        boolean ret=initializeVideo();

        if(!ret)
        {
        	return;
        }
        mMediaRecorderRecording = true;
        //
		Toast.makeText(MainActivity.this, "��ʼ��¼��ok", 0).show();

    	//����¼����
		mBtnStartCamera.setEnabled(false);
    	mBtnStartRecordVideo.setEnabled(true);
    	//���Թر�����ͷ��
    	mBtnStopCamera.setEnabled(true);
	}

    private boolean mIsFirstInitRecorder=true; 
    private boolean mIsReuseCamera=false;
    
   private  void createCamera(){
    	//�������������
        if (camera == null)
		{
        	camera = Camera.open();
            Camera.Parameters parameters = camera.getParameters();
            //�������Ҫ��
            camera.setDisplayOrientation(90);	//����ͷ��ת
            
            camera.setParameters(parameters); 

            //��ʼԤ�� ���� ���� Camera.startPreview() ��ʼ��ʾʵʱ�����档
            
            //��������media�����á�move it to recorder create
            //unlockCamera();
		}
    }
   	private void unlockCamera() {
	// TODO Auto-generated method stub
        //Ϊɶ���ʱ��Ҫ�����أ�
        camera.unlock();
}

	private
   	void createRecorder(){
        if(mMediaRecorder == null) 
        {
            mMediaRecorder = new MediaRecorder();
        } 
   	}
    
    private boolean initializeVideo()
    {
    	 if(mSurfaceHolder == null) 
         {
             return false;
         }
         createCamera();
        //move it to startCamera()
//        //��������Ϊ��
//        mMediaRecorderRecording = true;
        
        if(mMediaRecorder == null) 
        {
            mMediaRecorder = new MediaRecorder();
        } 
        else 
        {
        	//���˵�һ���ã���ҪResetһ��
            mMediaRecorder.reset();
        }
         
        
        
        //������������һ��ɣ�
        if(mIsFirstInitRecorder)
        {
        	if(camera!=null){
        		mMediaRecorder.setCamera(camera);
        		Log.e(TAG,"set camera to recorder");
    	    	mIsFirstInitRecorder=false;
    	    	mIsReuseCamera=true;
    	    	
    	    	//�ı����
        		mIsFirstRecord=false;
        	}else
        		Log.e(TAG,"camera null,cannot set camera to recorder");

        }
        
       return configRecorder();
        
        //not use this one
        //createH264OutFile();
        //add by me 
//        createMP4OutFile();
//        doRecord();
         
       //make it return at setRecorder()
        //return true;
    }

	
	
	
	private boolean mIsFirstRecord=true;
	private void startRecordVideo0() {
		Log.e(TAG,"start record video ");
		// TODO Auto-generated method stub

        if(mMediaRecorderRecording) //�����Ѿ�׼����
        {
        	//NOT USE THIS NOW
        	//getH264();
        	

      
           // RecordH264();
        	//����Ҫ���³�ʼ��һ��,����Ӧ�ò���������������ͷ��
        	//ȷ����һ�β��ܽ��룬֮�󶼿��Խ�ȥ
        	if(!mIsFirstRecord)
        	{
        		Log.e(TAG,"call reconnectCamera");
        		reconnectCamera();
        	
        	}
        	//setRecorder();//���ȱ��reset��
        	boolean ret=initializeVideo();//�������£�Ӧ�ÿ�����
        	if(!ret)
        	{
        		Log.e(TAG,"init error !");
        		return;
        	}else
        		Log.e(TAG,"init ok");

        	//Ȼ����������ļ���ִ��¼��
        	doRecord();
        	mIsRecording=true; //��������¼����
    		Toast.makeText(MainActivity.this, "��ʼ¼����", 0).show();

        }
        if(mMediaRecorderRecording){
        	mBtnStopRecordVideo.setEnabled(true);
        }
	}
	private void startRecordVideo() {
		Log.e(TAG,"start record video ");
		if(mIsFirstRecord){
			 if(mIsInitCamera) //�����Ѿ�׼����
		      {
		          createRecorder();
		          
		          //move it to configRecorder
		         //mMediaRecorder.setCamera(camera);
		      }
			 mIsFirstRecord=false;
		}else{
    		//Log.e(TAG,"call reconnectCamera");
    		//resetRecorder();
    		//unlockCamera();
    		
    		/*
    		 *  ����� recording ������ reconnect() �� re-acquire and re-lock the camera.
    		 * */
    		//reconnectCamera();
		}
		configRecorder();
		doRecord();
    	mIsRecording=true; //��������¼����
		Toast.makeText(MainActivity.this, "��ʼ¼����", 0).show();
        if(mMediaRecorderRecording){
        	mBtnStopRecordVideo.setEnabled(true);
        }
//		// TODO Auto-generated method stub
//        if(mIsInitCamera) //�����Ѿ�׼����
//        {
//        	//NOT USE THIS NOW
//        	//getH264();
//        	
//
//      
//           // RecordH264();
//        	//����Ҫ���³�ʼ��һ��,����Ӧ�ò���������������ͷ��
//        	//ȷ����һ�β��ܽ��룬֮�󶼿��Խ�ȥ
//        	if(!mIsFirstRecord)
//        	{
//        		Log.e(TAG,"call reconnectCamera");
//        		reconnectCamera();
//        	
//        	}
//        	//setRecorder();//���ȱ��reset��
//        	boolean ret=initializeVideo();//�������£�Ӧ�ÿ�����
//        	if(!ret)
//        	{
//        		Log.e(TAG,"init error !");
//        		return;
//        	}else
//        		Log.e(TAG,"init ok");
//
//        	//Ȼ����������ļ���ִ��¼��
//        	doRecord();
//        	mIsRecording=true; //��������¼����
//    		Toast.makeText(MainActivity.this, "��ʼ¼����", 0).show();
//
//        }
//        if(mMediaRecorderRecording){
//        	mBtnStopRecordVideo.setEnabled(true);
//        }
	}
	//�ر�һ��mediarecorder֮��Ҫlock camera���´δ�mediarecorder��ʱ���ٽ�������set��recorder��������ok�ˡ�
	protected void stopRecordVideo() {
		// TODO Auto-generated method stub
        if(mMediaRecorderRecording)
        {
           // releaseMediaRecorder();             
           
            //NOT USE THIS
            //closeH264();
        	if(mIsRecording)
        	{
        		//Log.e(TAG,"stop recorder");
        		stopRecorder();
        		mIsRecording=false;        	
        	}
          
        	//��ʱ����δ��ʼ����flag��Ӧ����ֹͣ��¼�ơ�
        	//  mMediaRecorderRecording = false;
           // resetRecorder();
            //������ͷ�recoder������Դ������Ϊnull
            releaseRecorder();
            if(camera == null){
            	Log.e(TAG,"after release recoder,camera IS null");
            }else
            	Log.e(TAG,"after release recoder,camera is NOT  null");
         
            //ֹͣ¼��ʱ����סcamera��4.0֮�󣬺�����Բ�����
            lockCamera();

        }
		Toast.makeText(MainActivity.this, "�Ѿ��ر�¼��", 0).show();

	}
	private void lockCamera() {
		// TODO Auto-generated method stub
		if(camera != null)
			camera.lock();
	}

	private void reconnectCamera() {
		Log.e(TAG,"reconnectCamera");
        if( camera != null ) { // just to be safe
    		try {
    			Log.e(TAG,"make camera reconnect");
				camera.reconnect();
		     //   this.startCameraPreview();
			}
    		catch (IOException e) {
        	//	if( MyDebug.LOG )
        			Log.e(TAG, "###failed to reconnect to camera");
				e.printStackTrace();
	    	   // showToast(null, R.string.fail_connect_camera);
	    	    closeCamera();
			}
		}else
			Log.e(TAG,"reconnectCamera:camera is null now!!!!");
        Log.e(TAG,"start preview");
        camera.startPreview();
	}
	//�ͷ�MediaRecorder��Դ,�������ر�������ͷ
    private void releaseMediaRecorder()
    {
    	Log.e(TAG,"release all ");
        if(mMediaRecorder != null) 
        {
         //   if(mMediaRecorderRecording) 
            {
            	if(mIsRecording)
            	{
            		Log.e(TAG,"1 stop recorder");
            		stopRecorder();
            	
            	}
            }
            resetRecorder();
            releaseRecorder();            
        }  
        closeCamera();
    }
     
    private void stopRecorder() {
		// TODO Auto-generated method stub
    	if(mMediaRecorder!=null)
    	{
    		mMediaRecorder.stop();
    		Log.e(TAG,"stop recorder");

    	}
    	else
    		Log.e(TAG,"mMediaRecorder is null ,could not stop");
	}

	private void releaseRecorder() {
		// TODO Auto-generated method stub
    	if(mMediaRecorder!=null)
    	{
    		Log.e(TAG,"release recorder");
    		mMediaRecorder.release();
            mMediaRecorder = null;
    	}    
    	else
    		Log.e(TAG,"mMediaRecorder is null ,could not release");
	}

	//�������в�������϶����а�
	private void resetRecorder() {
		// TODO Auto-generated method stub
    	if(mMediaRecorder!=null)
    	{
    		mMediaRecorder.reset();
    		Log.e(TAG,"reset recorder");
    	}else
    		Log.e(TAG,"null,could not reset recorder");
	}

	private void closeCamera() {
		Log.e(TAG,"release camera");

		// TODO Auto-generated method stub
        //�ȹر�¼��Ȼ��ر�����ͷ
        if (camera != null)
		{
        	camera.release();
        	camera=null;
		}
	}



	//��ʼ��SurfaceView
    private SurfaceView mSurfaceView;
    private void InitSurfaceView() 
    {
        mSurfaceView = (SurfaceView) this.findViewById(R.id.surface_camera);
        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceView.setVisibility(View.VISIBLE);
    }
     
    //��ʼ������¼mdat��ʼλ�õĲ���
    SharedPreferences sharedPreferences;
    private final String mediaShare = "media";
    private void InitMediaSharePreference() 
    {
        sharedPreferences = this.getSharedPreferences(mediaShare, MODE_PRIVATE);       
    }
 
 
    private SurfaceHolder mSurfaceHolder;
    private boolean mMediaRecorderRecording = false;
    private boolean mSurfaceCreated=false;
     
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
    }
 
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) 
    {
        mSurfaceHolder = holder;
        if(!mSurfaceCreated)
        {
        	mSurfaceCreated=true;
        	//���Դ�����ͷ��
        	mBtnStartCamera.setEnabled(true);
        }
    }
 
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
         
    }
     

    private boolean configRecorder() {
		// TODO Auto-generated method stub
    	if(mSurfaceHolder == null) 
    	{
    		return false;
    	}
//    	if(mMediaRecorder == null) 
//    	{
//    		mMediaRecorder = new MediaRecorder();
//    	} 
//    	else 
//    	{
//    		//���˵�һ���ã���ҪResetһ��
//    		mMediaRecorder.reset();
//    	}
    	createRecorder();//ȷ��mMediaRecorder��Ϊ��

    	//add this
    	//unlockCamera();
    	//ÿ�ζ�set��
    	unlockCamera();
    	mMediaRecorder.setCamera(camera);

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);//     //.THREE_GPP);
        mMediaRecorder.setVideoFrameRate(videoRate);	//¼����ת
        mMediaRecorder.setVideoSize(videoWidth, videoHeight);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
    	// ������Ƶ�ı����ʽ
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        //�����Ƿŵ���ͷȥ
//        if(mSurfaceHolder == null) 
//        {
//            return false;
//        }
        //ò�������camera��ͷҲ�����ƵĹ��ܰ�
        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
        mMediaRecorder.setMaxDuration(0);
        mMediaRecorder.setMaxFileSize(0);
        return true;
	}

	private void doRecord() {
    	//move it here to produce more files
    	createMP4OutFile();
		// TODO Auto-generated method stub
        try 
        {
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        } 
        catch (IllegalStateException e)
        {
            e.printStackTrace();
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
            //�ͷ�������Դ
            releaseMediaRecorder();
        }
	}

	private void createMP4OutFile() {
		// TODO Auto-generated method stub
    	
    	//copy from YMCX
		//File 
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String index = "";
        String filename=SDCARD_PATH+ File.separator
		+ "ZB_VID_" + timeStamp + index + ".mp4";
        Log.e(TAG,"cur filename["+filename+"]");
    	file = new File(filename);
		if (file.exists()) {
			// ����ļ����ڣ�ɾ��������ʾ���뱣֤�豸��ֻ��һ��¼���ļ�
			file.delete();
		}
		mMediaRecorder.setOutputFile(file.getAbsolutePath());
	}





    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
 
    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        if(mMediaRecorderRecording)
        {
            releaseMediaRecorder();
             
//            try 
//            {
//                lss.close();
//                receiver.close();
//                sender.close();
//            } 
//            catch (IOException e) 
//            {
//                Log.e(TAG, e.toString());
//            }
             //closeH264();
            
            mMediaRecorderRecording = false;
        }
        finish();
    }
    
  //step 7 do start
  	public void onStart() 
  	{
  		super.onStart();
  		// Lock screen
  		wl.acquire();
  	}
  	
  	public void onStop() {
  		super.onStop();
  		wl.release();
  	}
    
    /////////////////////////////////////////////////////////////
    //��ʼ��LocalServerSocket LocalSocket
    LocalServerSocket lss;
    LocalSocket receiver, sender;
     
    private void InitLocalSocket()
    {
        try 
        {
            lss = new LocalServerSocket("H264");
            receiver = new LocalSocket();
             
            receiver.connect(new LocalSocketAddress("H264"));
            receiver.setReceiveBufferSize(500000);
            receiver.setSendBufferSize(50000);
             
            sender = lss.accept();
            sender.setReceiveBufferSize(500000);
            sender.setSendBufferSize(50000);
             
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            this.finish();
            return;
        }
         
    }

  	
  	
	//��ʼ¼�������߳�
    private void startVideoRecording() 
    {
        new Thread(this).start();
    }
     
  	
  	
	private void RecordH264() {
		// TODO Auto-generated method stub
		startVideoRecording();
	}

	private void getH264() {
		// TODO Auto-generated method stub
        InitLocalSocket();		//��ʼ�����ݽӿ�
        getSPSAndPPS();
	}

	private void closeH264() {
		// TODO Auto-generated method stub
        try 
        {
            lss.close();
            receiver.close();
            sender.close();
        } 
        catch (IOException e) 
        {
            Log.e(TAG, e.toString());
        }
	}
	private void createH264OutFile() {
		// TODO Auto-generated method stub
        if(SPS==null)//spsδ֪
        {
            mMediaRecorder.setOutputFile(fd);	//¼���ļ�Ϊ��¼������Sps
        }
        else
        {
            mMediaRecorder.setOutputFile(sender.getFileDescriptor());
        }
	}
    
    private final int MAXFRAMEBUFFER = 20480;//20K
    private byte[] h264frame = new byte[MAXFRAMEBUFFER];
    private final byte[] head = new byte[]{0x00,0x00,0x00,0x01};
    private RandomAccessFile file_test;
    public void run() 
    {
        try {
             
            if(SPS == null)
            {
                Log.e(TAG, "Rlease MediaRecorder and get SPS and PPS");
                Thread.sleep(1000);
                //�ͷ�MediaRecorder��Դ
                releaseMediaRecorder();
                //���Ѳɼ�����Ƶ�����л�ȡSPS��PPS
                findSPSAndPPS();
                //�ҵ������³�ʼ��MediaRecorder
                initializeVideo();
            }          
             
            DataInputStream dataInput = new DataInputStream(receiver.getInputStream());
            //�ȶ�ȡftpy box and mdat box, Ŀ����skip ftpy and mdat data,(decisbe by phone)
            dataInput.read(h264frame, 0, StartMdatPlace);
             
            try 
            {
                File file = new File(Environment.getExternalStorageDirectory(), "encoder.h264");
                if (file.exists())
                    file.delete();
                file_test = new RandomAccessFile(file, "rw");
            } 
            catch (Exception ex) 
            {
                Log.v("System.out", ex.toString());
            }
            file_test.write(head);
            file_test.write(SPS);//write sps
             
            file_test.write(head);
            file_test.write(PPS);//write pps
             
            int h264length =0;
             
            while(mMediaRecorderRecording) 
            {
                h264length = dataInput.readInt();
                //Log.e(TAG, "h264 length :" + h264length);
//              int number=0 , num=0;
//              int frame_size = 1024;
//              file_test.write(head);
//              while(number<h264length)
//              {
//                  int lost=h264length-number;
//                  num = dataInput.read(h264frame,0,frame_size<lost?frame_size:lost);
//                  Log.d(TAG,String.format("H264 %d,%d,%d", h264length,number,num));
//                  number+=num;
//                  file_test.write(h264frame, 0, num);
//              }
                ReadSize(h264length, dataInput);
                 
                byte[] h264 = new byte[h264length];
                System.arraycopy(h264frame, 0, h264, 0, h264length);
                 
                file_test.write(head);
                file_test.write(h264);//write selice
            }
              
            file_test.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
         
    }
     
    private void ReadSize(int h264length,DataInputStream dataInput) throws IOException, InterruptedException{
        int read = 0;
        int temp = 0;
        while(read<h264length)
        {
            temp= dataInput.read(h264frame, read, h264length-read);
            //Log.e(TAG, String.format("h264frame %d,%d,%d", h264length,read,h264length-read));
            if(temp==-1)
            {
                Log.e(TAG, "no data get wait for data coming.....");
                Thread.sleep(2000);
                continue;
            }
            read += temp;
        }
    }
     
    //�� fd�ļ����ҵ�SPS And PPS
    private byte[] SPS;
    private byte[] PPS;
    private int StartMdatPlace = 0;
    private void findSPSAndPPS() throws Exception
    {
        File file = new File(fd);
        FileInputStream fileInput = new FileInputStream(file);
         
        int length = (int)file.length();
        byte[] data = new byte[length];
         
        fileInput.read(data);
         
        final byte[] mdat = new byte[]{0x6D,0x64,0x61,0x74};
        final byte[] avcc = new byte[]{0x61,0x76,0x63,0x43};
         
        for(int i=0 ; i<length; i++){
            if(data[i] == mdat[0] && data[i+1] == mdat[1] && data[i+2] == mdat[2] && data[i+3] == mdat[3]){
                StartMdatPlace = i+4;//find mdat
                break;
            }
        }
        Log.e(TAG, "StartMdatPlace:"+StartMdatPlace);
        //��¼��xml�ļ���
        String mdatStr = String.format("mdata_%d%d.mdat",videoWidth,videoHeight);
        Editor editor = sharedPreferences.edit();
        editor.putInt(mdatStr, StartMdatPlace);
        editor.commit();
         
        for(int i=0 ; i<length; i++){
            if(data[i] == avcc[0] && data[i+1] == avcc[1] && data[i+2] == avcc[2] && data[i+3] == avcc[3]){
                int sps_start = i+3+7;//����i+3ָ��avcc��c���ټ�7����6λAVCDecoderConfigurationRecord����
                 
                //sps length and sps data
                byte[] sps_3gp = new byte[2];//sps length
                sps_3gp[1] = data[sps_start];
                sps_3gp[0] = data[sps_start + 1];
                int sps_length = bytes2short(sps_3gp);
                Log.e(TAG, "sps_length :" + sps_length);
                 
                sps_start += 2;//skip length
                SPS = new byte[sps_length];
                System.arraycopy(data, sps_start, SPS, 0, sps_length);
                //save sps
                FileOutputStream file_out = MainActivity.this.openFileOutput(
                        String.format("%d%d.sps",videoWidth,videoHeight),
                        Context.MODE_PRIVATE);
                file_out.write(SPS);
                file_out.close();
                 
                //pps length and pps data
                int pps_start = sps_start + sps_length + 1;
                byte[] pps_3gp =new byte[2];
                pps_3gp[1] = data[pps_start];
                pps_3gp[0] =data[pps_start+1];
                int pps_length = bytes2short(pps_3gp);
                Log.e(TAG, "PPS LENGTH:"+pps_length);
                 
                pps_start+=2;
                 
                PPS = new byte[pps_length];
                System.arraycopy(data, pps_start, PPS,0,pps_length);
                 
                 
                //Save PPS
                file_out = MainActivity.this.openFileOutput(
                        String.format("%d%d.pps",videoWidth,videoHeight),
                        Context.MODE_PRIVATE);
                file_out.write(PPS);
                file_out.close();
                break;
            }
        }
         
    }
     

 

    //���㳤��
    public short bytes2short(byte[] b)
    {
                short mask=0xff;
                short temp=0;
                short res=0;
                for(int i=0;i<2;i++)
                {
                    res<<=8;
                    temp=(short)(b[1-i]&mask);
                    res|=temp;
                }
                return res;
    }
    File dir;
    //�����ļ��м��ļ� 
    public void CreateText()
    { 
 	   	//�ж�SD���Ƿ����
 	   	if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
 	   	{
 	   		File ldDownloadDir = new File(Environment.getExternalStorageDirectory(), "LDDownload01"); 
 	   		// �������ͼƬ��Ӧ�ó���ж�غ󻹴��ڡ����ܱ�����Ӧ�ó�����
 	   		// ��˱���λ�������
 	           // ��������ڵĻ����򴴽��洢Ŀ¼
 	           if ( !ldDownloadDir.exists())
 	           { 
 	               if (! ldDownloadDir.mkdirs())
 	               { 
 	                   Log.e(TAG, "�����ļ���ʧ��"); 
 	                   Toast.makeText(MainActivity.this, "�����ļ���ʧ��", Toast.LENGTH_SHORT).show();
 	                   return; 
 	               } 
 	           } 
 	           
 	       	dir = new File(ldDownloadDir.getAbsolutePath()+ "/image" ); 
 	       	if (!dir.exists()) 
 	        { 
 	              try 
 	              { 
 	                  //��ָ�����ļ����д����ļ� 
 	                  dir.createNewFile(); 
 	              }
 	              catch (Exception e)
 	              { 
 	            	  Log.e(TAG, e.toString());
 	            	  
 	              } 
 	        } 
 	
 	   	}
 	   	else
 	   	{
 	   		Toast.makeText(MainActivity.this, "Sd��������", 1).show();
 	   	}
 	   	
    }
    
  //���Ѵ������ļ���д������ 
    public void print(byte[] str, int num)
    { 
 	   if (dir == null)
 	   {
 			CreateText();
 	   }
 	   
        FileWriter fw = null; 
        BufferedWriter bw = null; 
        try 
        { 
     	   FileOutputStream os = new FileOutputStream(dir, true);
     	   os.write(str,0, num);
     	   os.close();
        } 
        catch (IOException e)
        { 
            // TODO Auto-generated catch block 
            e.printStackTrace(); 
            try 
            { 
                bw.close(); 
                fw.close(); 
            } 
            catch (IOException e1) 
            { 
                // TODO Auto-generated catch block 
         	   Log.i(TAG, e1.toString());
            } 
            catch (Exception e1)
            {
         	   Log.i(TAG, e1.toString());
            }
            
        } 
    } 
    
   //�õ����в�����SPS��ͼ�������PPS,����Ѿ��洢�ڱ���
   private void getSPSAndPPS()
   {
   	//��ȡ�����ļ��е�����
       StartMdatPlace = sharedPreferences.getInt( String.format("mdata_%d%d.mdat", videoWidth, videoHeight), -1);
        
       //���ݴ���
       if(StartMdatPlace != -1) 
       {
           byte[] temp = new byte[100];
           try 
           {
               FileInputStream file_in = MainActivity.this.openFileInput(
                       String.format("%d%d.sps", videoWidth,videoHeight));
                
               int index = 0;
               int read=0;
               while(true)
               {
                   read = file_in.read(temp,index,10);
                   if(read==-1) break;
                   else index += read;
               }
               Log.e(TAG, "sps length:"+index);
               SPS = new byte[index];
               System.arraycopy(temp, 0, SPS, 0, index);
                               
               file_in.close();
                
               index =0;
               //read PPS
               file_in = MainActivity.this.openFileInput(
                       String.format("%d%d.pps", videoWidth,videoHeight));
               while(true)
               {
                   read = file_in.read(temp,index,10);
                   if(read==-1) break;
                   else index+=read;
               }
               Log.e(TAG, "pps length:"+index);
               PPS = new byte[index];
               System.arraycopy(temp, 0, PPS, 0, index);
           } 
           catch (FileNotFoundException e)
           {
               //e.printStackTrace();
               Log.e(TAG, e.toString());
           } 
           catch (IOException e) 
           {
               //e.printStackTrace();
               Log.e(TAG, e.toString());
           }
       } 
       else 
       {
           SPS = null;
           PPS = null;
       }
   }
      
}