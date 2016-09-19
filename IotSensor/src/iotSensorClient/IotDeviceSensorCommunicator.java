package iotSensorClient;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import iotSensorMessage.SensorInfo;
import iotSensor.IotSensorDummy;

public class IotDeviceSensorCommunicator {

	static Logger logger = Logger.getLogger(IotDeviceSensorCommunicator.class.getName());

	static final int UnknownSensorID = -1;
	private AsynchronousSocketChannel sockChannel;
	private Timer readTimer;
	private AtomicInteger timeoutCnt = new AtomicInteger( 0 );
	private boolean sessionAlive;
	private onReadCompletionHandler readHandler;
	private onWriteCompletionHandler writeHandler;
	private Object timerlock = new Object();
	private IotSensorDummy sensor;
	private int sensorId;
	private IotSensorClient client;

	public IotDeviceSensorCommunicator(IotSensorDummy sensor, IotSensorClient client,AsynchronousSocketChannel sockChannel){
		this.sockChannel = sockChannel;
		this.client = client;
		this.sensor = sensor;
		this.sensorId = this.sensor.getDataModel().getChannel();
		this.readHandler = new onReadCompletionHandler(sockChannel);
		this.writeHandler = new onWriteCompletionHandler(sockChannel);
		this.sessionAlive = true;
	}

	/* ----------------------------------------
	 * Read�@�n���h���N���X
	 */
	private class onReadCompletionHandler implements CompletionHandler<Integer, AsynchronousSocketChannel>{

		private AtomicInteger messageRead;
		private ByteBuffer buf;
		private AsynchronousSocketChannel sockChannel;

		/*
		 * TODO:�@�R���X�g���N�^������ IotDemoSensor�I�u�W�F�N�g��ǉ�
		 */
		public onReadCompletionHandler(AsynchronousSocketChannel sockChannel){
			this.messageRead = new AtomicInteger(0);
			this.buf = ByteBuffer.allocate(512);
			this.sockChannel = sockChannel;
		}

		public void startRead() {
			//read message from client
			this.buf.clear();
			try{
				this.sockChannel.read( this.buf, this.sockChannel, this);
			}catch(Exception e){
				IotDeviceSensorCommunicator.logger.info("Exception @ startRead:" + e.toString());
			}
		}

		/*
		 * Read Event
		 */
		@Override
		public void completed(Integer result, AsynchronousSocketChannel sockChannel  ) {
			this.messageRead.getAndIncrement();
			int size = this.buf.position();
			if(0 < size){
				ByteBuffer mes = ByteBuffer.allocate(this.buf.position());
				mes.put(this.buf.array(),this.buf.arrayOffset(),this.buf.position());
				String message = new String(mes.array());
				IotDeviceSensorCommunicator.logger.info( "id:"+IotDeviceSensorCommunicator.this.sensorId+
									", Read  message:" + message + " @ " + this.messageRead.toString() );
				this.buf.flip();
            	/*
            	 * ��M���b�Z�[�W�p�[�X�i�񓯊��ׁ̈A�A�����Ď�M����ꍇ������̂Łh}"���Z�p���[�^�Ƃ��ĕ������A"}"���ēx�ǉ����Čʂ̃��b�Z�[�W�𒊏o����j
            	 */
            	ArrayList<String> messages = new ArrayList<String>(Arrays.asList(message.split("}")));
            	Iterator<String> messageIt = messages.iterator();
            	while(messageIt.hasNext()){
            		String m = messageIt.next()+"}";
	            	ObjectMapper mapper = new ObjectMapper();
	    			try{
	    				SensorInfo info = mapper.readValue(m,SensorInfo.class);
	    				if( info.type.equals("CMD") && info.val.equals("START") ){
	    					/*
	    					 * TODO: sensor�@�N���X�̃X�^�[�g����
	    					 */
	    					IotDeviceSensorCommunicator.this.sensor.startSampling();
	    					info.setType("RESP");
	    					info.setVal("START OK");
	    					IotDeviceSensorCommunicator.this.Write(info.type,info.num,info.val);
	    				}
	    				else if( info.type.equals("CMD") && info.val.equals("STOP") ){
	    					/*
	    					 * TODO: sensor�@�N���X�̃X�g�b�v����
	    					 */
	    					IotDeviceSensorCommunicator.this.sensor.stopSampling();
	    					info.setType("RESP");
	    					info.setVal("STOP OK");
	    					IotDeviceSensorCommunicator.this.Write(info.type,info.num,info.val);
	    				}
	    				else if( info.type.equals("CMD") && info.val.equals("STAT") ){
	    					/*
	    					 * TODO: sensor�@�N���X�̏�ԓǏo��
	    					 */
	    					String sensorStat = "";
	    					if(IotDeviceSensorCommunicator.this.sensor.getSamplingStatus()){
	    						sensorStat = "Started";
	    					}
	    					else{
	    						sensorStat = "Stopped";
	    					}
	    					info.setType("RESP");
	    					info.setVal(sensorStat);
	    					IotDeviceSensorCommunicator.this.Write(info.type,info.num,info.val);
	    				}
	    				/*
	    				 * PING�v����M
	    				 */
	    				else if( info.type.equals("CMD") && info.val.equals("PING")){
	    					info.setType("RESP");
	    					info.setVal("PING OK");
	    					IotDeviceSensorCommunicator.this.Write(info.type,info.num,info.val);
	    				}
	    				/*
	    				 * PING������M
	    				 */
	    				else if( info.type.equals("RESP") && info.val.equals("PING OK")){
	    				}
	    				else{
	    				}
	    			}catch(Exception e){
	    				IotDeviceSensorCommunicator.logger.info("Exception occured @ mapper.readValue : "+e.toString());
	    			}
            	}
			}
			else{
				try{
					IotDeviceSensorCommunicator.this.close();
				}catch(Exception e){
					IotDeviceSensorCommunicator.logger.info("Exception @ sockChannel.close() onReadCompletionHandler.completed()");
				}
			}
        	IotDeviceSensorCommunicator.this.restartTimer();
            /*
             * ReadPendingException���������ׁA�펞���[�h������B
             * (���[�h�̊����C�x���g�����Ŏ���Read�������Ă���)
             */
        	startRead();
		}

		@Override
		public void failed(Throwable exc, AsynchronousSocketChannel sockChannel ) {
			IotDeviceSensorCommunicator.logger.info( "fail to read message from client");
			try{
				IotDeviceSensorCommunicator.this.close();
			}catch(Exception e){
				IotDeviceSensorCommunicator.logger.info("Exception @ onReadCompletionHandler.failed:" + e.toString());
			}
		}
	}

	/* ----------------------------------------
	 * Write �n���h���N���X
	 */
	private class onWriteCompletionHandler implements CompletionHandler<Integer, AsynchronousSocketChannel>{

    	private AtomicInteger messageWritten;
       	private ByteBuffer buf;
       	private AsynchronousSocketChannel sockChannel;
    	private Semaphore sem;
        private String message;
        private ObjectMapper mapper;
        private SensorInfo cmdMessage;
        
    	public onWriteCompletionHandler(AsynchronousSocketChannel sockChannel){
    		this.messageWritten = new AtomicInteger( 0 );
    		this.buf = ByteBuffer.allocate(512);
    		this.sockChannel = sockChannel;
            this.sem = new Semaphore(1);
            this.mapper = new ObjectMapper();
            this.cmdMessage = new SensorInfo(IotDeviceSensorCommunicator.this.sensorId,"","","");
    	}

        public void write(int id,String type,String num, String val){
            this.startWrite(id,type,num,val);
        }
        
        private void startWrite(int id,String type,String num,String val) {
    		/*
    		 * ���M�r������ׁ̈A������sem�����b�N����
    		 * (���d���M�ɂ���O����ׁ̈j
    		 */
        	this.semAcquire();
        	cmdMessage.setId(id);
        	cmdMessage.setType(type);
        	cmdMessage.setVal(val);
        	if(num==null){
            	cmdMessage.setNum(Integer.toString(this.messageWritten.getAndIncrement()));
        	}
        	else{
        		cmdMessage.setNum(num);
        	}
        	try{
            	this.message = this.mapper.writeValueAsString(cmdMessage);
	    	}catch(Exception e){
				IotDeviceSensorCommunicator.logger.info("Exception occured @ mapper.writeValueAsString : "+e.toString());
	    	}
        	this.buf.clear();
            this.buf.put(message.getBytes());
            this.buf.flip();
            this.sockChannel.write(this.buf, this.sockChannel, this);
        }

        // WritePendingException�@����̔r������
      	private void semAcquire(){
      		try{
      			sem.acquire();
      		}catch(Exception e){
      			IotDeviceSensorCommunicator.logger.info("Exception @ sem.acquire() : " + e.toString());
      		}
      	}
      	
      	private void semRelease(){
      		try{
      			sem.release();
      		}catch(Exception e){
      			IotDeviceSensorCommunicator.logger.info("Exception @ sem.acquire() : " + e.toString());
      		}
      	}
    	
    	public void completed(Integer result, AsynchronousSocketChannel channel ) {
            IotDeviceSensorCommunicator.logger.info( "id:"+IotDeviceSensorCommunicator.this.sensorId+
            					", Write message:" + this.message + " @ " + this.messageWritten.get());
        	/*
        	 * ���M�r������ׁ̈A������sem���������B
        	 * (���d���M�ɂ���O����ׁ̈j
        	 */
            this.semRelease();
        }

        @Override
        public void failed(Throwable exc, AsynchronousSocketChannel channel) {
        	if(IotDeviceSensorCommunicator.this.isSessionAlive()){
        		IotDeviceSensorCommunicator.logger.info( "fail to write message to server : " + exc.toString());
        	}
        	/*
        	 * ���M�r������ׁ̈A������sem���������B
        	 * (���d���M�ɂ���O����ׁ̈j
        	 * (Fail�ł�������Ă����Ȃ��ƃv���O����������I���ł��Ȃ��̂Œ��Ӂj
        	 */
        	this.semRelease();
        }
	}

	/* ----------------------------------------
	 * Read �^�C���A�E�g�n���h��
	 */
	public void startTimer(){
		this.readTimer = new Timer();
		this.readTimer.schedule(new readTimeoutHandler(this.timeoutCnt),5000);
	}
	public void stopTimer(){
		if(this.readTimer!=null){
			this.readTimer.cancel();
			this.readTimer = null;
		}
	}
	public void restartTimer(){
		/*
		 * �Z���T��~���Z���T�J�n�̃^�C�~���O��
		 * �^�C���A�E�g���m�ɂ�� restart ��
		 * �Z���T����M�C�x���g(onReadCompletedHandler.completed) �ɂ�� restart
		 * ���d���� already cancelled exception ����������^�C�~���O������B
		 * �����������邽�߂�restart�������N���e�B�J���Z�N�V�����ł������Ă���
		 */
		synchronized(this.timerlock){
			this.stopTimer();
			this.startTimer();
		}
	}
	private class readTimeoutHandler extends TimerTask{
		/*
		 * Read Timeout Handler
		 */
		private AtomicInteger timeoutCnt;
		public readTimeoutHandler(AtomicInteger timeoutCnt){
			this.timeoutCnt = timeoutCnt;
		}
		public void run(){
			this.timeoutCnt.getAndIncrement();
			IotDeviceSensorCommunicator.this.Write("CMD",null,"PING");
			IotDeviceSensorCommunicator.this.restartTimer();
		}
	}

	/* ----------------------------------------
	 * Read Operation
	 */
	public void Read(){
		/*
		 * ��M�^�C���A�E�g�ݒ�
		 */
		this.startTimer();
		this.readHandler.startRead();
	}
	/* ----------------------------------------
	 * Write Operation
	 */
	public void Write(String type,String num,String val){
		this.writeHandler.write(this.sensorId,type,num,val);
	}

	/* ----------------------------------------
	 * Close
	 */
	public boolean isSessionAlive(){
		return this.sessionAlive;
	}

	public void close(){
		if(this.isSessionAlive()){
			try{
				this.sockChannel.close();
				this.sessionAlive = false;
				IotDeviceSensorCommunicator.this.client.removeCommunicator();
			}catch(Exception e){
				IotDeviceSensorCommunicator.logger.info("Close Exceptio Occured" + e.toString());
			}
		}
	}
}
