package iotDemoMqttClient;

import com.amazonaws.services.cognitoidentity.model.Credentials;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotTopic;
import com.amazonaws.services.iot.client.AWSIotException;

import iotDemoCredentialSample.IotDemoCredentials;
import iotDeviceDemo.IotDeviceDemo;

import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class IotDemoMqttClient {

	static Logger logger = Logger.getLogger(IotDeviceDemo.class.getName());

	private String identity_pool_id;
	private String clientEndpoint;
	private String clientId		  = "UUID";		// ユニークな文字列であればOK。最終的にはUUIDで生成する。
	private IotDemoCredentials demoCredentials = null;
	private Credentials credential = null;
	private AWSIotMqttClient awsIotClient;
	private AWSIotTopic topic;
	private ArrayList<AWSIotTopic> topics = new ArrayList<AWSIotTopic>();
	
	public IotDemoMqttClient(String identity_pool_id, String clientEndpoint){
		this.identity_pool_id = identity_pool_id;
		this.clientEndpoint = clientEndpoint;
	}

	private class resetTimerElapse extends TimerTask {
		public void run(){
			try{
				resetConnection();
				logger.info(String.format("resetConnectio success"));
			}catch(AWSIotException e){
				logger.info("reseConnection failure @ IotDemoMqttClient : " + e.getMessage());
			}
		}
	}
	
	synchronized public void resetConnection() throws AWSIotException{
		if( this.awsIotClient!=null){
			disconnect();
			connect();
			subscribe();
		}
	}

	public void connect() throws AWSIotException {
		/*
		 * AWS Iot に接続する　Mqtt　クライアント構築
		 */
		if( this.demoCredentials == null ){
	    	this.demoCredentials = new IotDemoCredentials(identity_pool_id);	// 未認証ユーザで一時的な認証情報を生成する。
	    	this.credential		 = this.demoCredentials.getCredentials();		// 一時的な認証情報を得る。
		}
		
        String awsAccessKeyId 		= this.credential.getAccessKeyId();			// 認証情報からアクセスキーを取得する。
        String awsSecretAccessKey	= this.credential.getSecretKey();			// 認証情報からシークレットキーを取得する。
        String sessionToken			= this.credential.getSessionToken();		// 認証情報をからセッショントークンを取得する。
        
        if (awsAccessKeyId != null && awsSecretAccessKey != null) {
        	this.awsIotClient = new AWSIotMqttClient(this.clientEndpoint, this.clientId, awsAccessKeyId, awsSecretAccessKey,sessionToken);
       		this.awsIotClient.connect();
       		Timer resetTimer  = new Timer(true);
       		resetTimer.schedule(new resetTimerElapse(),60000);
        }
	}
	
	public void disconnect() throws AWSIotException {
		if( this.awsIotClient != null ){
			this.awsIotClient.disconnect();
			this.awsIotClient = null;
		}
	}
	
	synchronized public void publish(AWSIotMessage message) throws AWSIotException {
		if( this.awsIotClient != null ){
	        this.awsIotClient.publish(message);
		}
	}
	
	synchronized public void subscribe(AWSIotTopic topic) throws AWSIotException {
		this.topics.add(topic);
		logger.info("add topic");
		this.awsIotClient.subscribe(topic,true);
	}
	
	synchronized private void subscribe() throws AWSIotException {
		if( this.awsIotClient != null ){
			Iterator<AWSIotTopic> topicite = topics.iterator();
			while(topicite.hasNext()){
				AWSIotTopic topic = topicite.next();
				logger.info("subscribe topic");
				this.awsIotClient.subscribe(topic, true);
			}
		}
	}
	synchronized public void removeSubscriber(AWSIotTopic topic){
		this.topics.remove(topic);
		logger.info("remove topic");
	}
}
