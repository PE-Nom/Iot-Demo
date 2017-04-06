package iotDeviceServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import com.amazonaws.services.iot.client.AWSIotMqttClient;

import iotDemoMqttClient.MqttClient;
import java.util.Vector;
import org.apache.log4j.Logger;

public class IotDeviceServer {

	static Logger logger = Logger.getLogger(IotDeviceServer.class.getName());

	private Vector<IotDeviceSensorCommunicator> communicators;
	private MqttClient mqttClient;

	public IotDeviceServer( String bindAddr, int bindPort ) throws IOException {

		this.communicators = new Vector<IotDeviceSensorCommunicator>();
		
        InetSocketAddress sockAddr = new InetSocketAddress(bindAddr, bindPort);
        //create a socket channel and bind to local bind address
        AsynchronousServerSocketChannel serverSock =  AsynchronousServerSocketChannel.open().bind(sockAddr);
        //start to accept the connection from client
        serverSock.accept(serverSock, new onAcceptCompletionHandler() );
	}
	public IotDeviceServer( String bindAddr, int bindPort, MqttClient mqttClient ) throws IOException {

		this.mqttClient = mqttClient;
		this.communicators = new Vector<IotDeviceSensorCommunicator>();
		
        InetSocketAddress sockAddr = new InetSocketAddress(bindAddr, bindPort);
        //create a socket channel and bind to local bind address
        AsynchronousServerSocketChannel serverSock =  AsynchronousServerSocketChannel.open().bind(sockAddr);
        //start to accept the connection from client
        serverSock.accept(serverSock, new onAcceptCompletionHandler() );

	}

    /* ----------------------------------------
     * Accept ハンドラクラス
     */
    private class onAcceptCompletionHandler implements CompletionHandler<AsynchronousSocketChannel,AsynchronousServerSocketChannel> {
    	
        @Override
        public void completed(AsynchronousSocketChannel sockChannel, AsynchronousServerSocketChannel serverSock ) {
            //a connection is accepted, start to accept next connection
            serverSock.accept( serverSock, this );
            
            // Communicator　オブジェクト生成
        	IotDeviceSensorCommunicator communicator = new IotDeviceSensorCommunicator(IotDeviceServer.this,sockChannel, IotDeviceServer.this.mqttClient);
//        	IotDeviceSensorCommunicator communicator = new IotDeviceSensorCommunicator(IotDeviceServer.this,sockChannel);
            IotDeviceServer.this.communicators.add(communicator);
			/*
			 * TODO:　ハンドラのリスト化とAdd
			 */
            
            //start to read message from the client
            IotDeviceServer.logger.info("Start read");
            
        }

        @Override
        public void failed(Throwable exc, AsynchronousServerSocketChannel serverSock) {
        	IotDeviceServer.logger.info( "fail to accept a connection");
        }
    }
    
    /* ----------------------------------------
     * Communicator　Remove
     */ 
    public void removeCommunicator(IotDeviceSensorCommunicator communicator){
    	this.communicators.remove(communicator);
    }
}
