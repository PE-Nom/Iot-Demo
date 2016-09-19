package iotSensorDataModel;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Vector;

import org.apache.log4j.Logger;

import java.util.Properties;


public class IotSensorDataModelContainer {
	
	static Logger logger = Logger.getLogger(IotSensorDataModelContainer.class.getName());

	private int channels;
	private Properties config;
	private Vector<IotSensorDataModel> jsonTesters;

	public IotSensorDataModelContainer(){
		this.config = new Properties();
		try{
			InputStream fileInputStream = new FileInputStream(new File("config.properties"));
			this.config.load(fileInputStream);
			this.channels = Integer.parseInt(this.config.getProperty("channels"));
			this.readProperties();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void readProperties(){
		this.jsonTesters = new Vector<IotSensorDataModel>();
		for(int i=0; i<this.channels; i++){
			this.jsonTesters.add(new IotSensorDataModel((String)this.config.getProperty("ch"+i),i));
		}
	}
	
	public int getChannels(){
		return this.channels;
	}

	public Vector<IotSensorDataModel> getDataModels(){
		return this.jsonTesters;
	}
	
	public IotSensorDataModel getDataModel(int channel){
		if(channel < this.jsonTesters.size()){
			return this.jsonTesters.get(channel);
		}
		else{
			return null;
		}
		
	}
}