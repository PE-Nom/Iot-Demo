package iotDeviceDemo;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import com.amazonaws.services.cognitoidentity.model.Credentials;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotException;

import iotDemoCredentialSample.IotDemoCredentials;
import iotDeviceServer.IotDeviceServer;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.apache.log4j.Logger;

public class IotDeviceDemo {
	
	static Logger logger = Logger.getLogger(IotDeviceDemo.class.getName());

	/* ----------------------------------------
	 *  main
	 */
	public static void main( String...args ) {

		String identity_pool_id = null;
		String clientEndpoint = null;
		String clientId		  = "UUID";							// ユニークな文字列であればOK。最終的にはUUIDで生成する。

		DOMConfigurator.configure(".\\log4j.xml");
		BasicConfigurator.configure();

		Properties properties = new Properties();
		try{
			InputStream fileInputStream = new FileInputStream(new File("credential.properties"));
			properties.load(fileInputStream);
			identity_pool_id = properties.getProperty("identity_pool_id");
			clientEndpoint = properties.getProperty("client_entpoint");
			logger.info("identity_pool_id ="+identity_pool_id);
			logger.info("clientEnfpoint = "+clientEndpoint);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		AWSIotMqttClient awsIotClient;
		
		/*
		 * AWS Iot に接続する　Mqtt　クライアント構築
		 */
    	IotDemoCredentials demoCredentials
    				= new IotDemoCredentials(identity_pool_id);				// 未認証ユーザで一時的な認証情報を生成する。
    	Credentials credential		= demoCredentials.getCredentials();		// 一時的な認証情報を得る。
        String awsAccessKeyId 		= credential.getAccessKeyId();			// 認証情報からアクセスキーを取得する。
        String awsSecretAccessKey	= credential.getSecretKey();			// 認証情報からシークレットキーを取得する。
        String sessionToken			= credential.getSessionToken();			// 認証情報をからセッショントークンを取得する。

        if (awsAccessKeyId != null && awsSecretAccessKey != null) {
        	awsIotClient = new AWSIotMqttClient(clientEndpoint, clientId, awsAccessKeyId, awsSecretAccessKey,sessionToken);
        	try{
                awsIotClient.connect();
        	}catch(AWSIotException e){
        		System.out.println("AWSIoException @ main :"+e.toString());
        	}
	        /*
	         * IotSensorからの接続待ち Listenソケット生成
	         */
			try{
			 	IotDeviceServer server = new IotDeviceServer("127.0.0.1", 3575, awsIotClient);
//			 	IotDeviceServer server = new IotDeviceServer("127.0.0.1", 3575);
			}catch(IOException e){
				logger.info("Exception @ main" + e.toString());
			}
			logger.info("start");
			while(true);
        }
        else{
        	logger.info("AWSIotMqttClient Construct faillure!! @ main");
        }
	}
}
