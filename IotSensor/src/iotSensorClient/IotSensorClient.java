package iotSensorClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Map;

import org.apache.log4j.Logger;

import iotSensor.IotSensorDummy;
import iotSensor.IotSensorListener;
import iotSensorDataModel.IotSensorDataModelListener;

public class IotSensorClient implements IotSensorListener,IotSensorDataModelListener{

	static Logger logger = Logger.getLogger(IotSensorClient.class.getName());

	private AsynchronousSocketChannel sockChannel = null;
	private IotDeviceSensorCommunicator communicator = null;
	private String host = null;
	private int port;
	private IotSensorDummy sensor;
	int sensorId;

    public IotSensorClient( String host, int port, IotSensorDummy sensor ) throws IOException {
        this.host = host;
        this.port = port;
        this.sensor = sensor;
        this.sensorId = this.sensor.getDataModel().getChannel();
    }
    
    /*
     * Listener　メソッド追加
     *  IotDemoSensorListener#samplingStatusChanged()
     *  IotDemoSensorListener#loadingStatusChanged()
     */
    public void samplingStatusChanged(boolean status){
    	if(this.communicator!=null){
        	if(status){
            	this.communicator.Write("STAT", null, "Started");
        	}else{
            	this.communicator.Write("STAT", null, "Stopped");
        	}
    	}
    }
    public void loadingStatusChanged(boolean status){
    	if(status){
    		if(this.communicator==null){
    			this.connect();
        	}
    	}
   		else{
    		if(this.communicator!=null){
    			this.close();
        	}
    	}
    }
    
    public void updateSampleingData(Map<String,Object> x){
    	if(this.communicator!=null){
        	this.communicator.Write("DATA", x.get("No.").toString(), x.get("data").toString() );
    	}
    }
    
    /* ----------------------------------------
     * connect
     */
    private void connect(){
    	try{
            //create a socket channel
            this.sockChannel = AsynchronousSocketChannel.open();
            //try to connect to the server side
            this.sockChannel.connect( new InetSocketAddress(host, port), this.sockChannel, new onConnectCompletionHandler());
    	}catch( IOException e){
    		IotSensorClient.logger.info("AsynchronousSocke open faile");
    	}
    }
    
    /* ----------------------------------------
     * connect ハンドラクラス
     */
    private class onConnectCompletionHandler implements CompletionHandler<Void, AsynchronousSocketChannel >{

    	public onConnectCompletionHandler(){
    	}
    	
    	@Override
    	public void completed(Void result, AsynchronousSocketChannel channel ) {
    		/*
    		 * connect成功でセッション確立
    		 */
    		IotSensorClient.this.communicator = new IotDeviceSensorCommunicator(IotSensorClient.this.sensor,IotSensorClient.this,channel);
        	IotSensorClient.this.communicator.Read();
        	IotSensorClient.this.communicator.Write("CMD", null, "REGIST");
    	}

    	@Override
    	public void failed(Throwable exc, AsynchronousSocketChannel channel) {
    		IotSensorClient.logger.info( "fail to connect to server");
    	}
    }

    /* ----------------------------------------
	 * Close
	 */
	public boolean isSessionAlive(){
		boolean session;
		if(this.communicator != null ){
			session = this.communicator.isSessionAlive();
		}
		else {
			session = false;
		}
		return session;
	}

	public void removeCommunicator(){
		this.communicator = null;
	}
	
	public void close(){
		if(this.communicator!=null){
			this.communicator.close();
		}
	}
}
