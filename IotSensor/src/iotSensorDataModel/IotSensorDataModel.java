package iotSensorDataModel;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.lang.Math;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class IotSensorDataModel extends Thread{

	static Logger logger = Logger.getLogger(IotSensorDataModel.class.getName());

	private String jsonFilePath = null;
	private List<Object> dataList = null;
	private List<IotSensorDataModelListener> obs = new ArrayList<IotSensorDataModelListener>();
	private int channel;
	private int samplingIndicator = 0;
	private double signalValue = 0.0;
	private double xn0=0.0;
	private double yn1=0.0;
	private double yn2=0.0;
	private double yn3=0.0;
	
	public IotSensorDataModel(String filePath, int channel){
		this.jsonFilePath=filePath;
		this.channel = channel;
/*		try{
			readJsonArraySample();
		}
		catch(Exception e){
			IotSensorDataModel.logger.info("Exception Occure :" + e.toString());
		}
*/
	}

	// JSON 配列は List<Object> にマッピングされる
/*	private List<Object> readJsonArray(File json)
			throws JsonMappingException, JsonParseException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(json, new TypeReference<List<Object>>(){});
	}

	private void readJsonArraySample()
			throws JsonMappingException, JsonParseException, IOException {
		if( this.jsonFilePath != null ){
			File json = new File(this.jsonFilePath);
			// JSON 配列は List
			this.dataList = readJsonArray(json);
		}
		else{
			IotSensorDataModel.logger.info("Invalid JSON File Path!");
		}
	}
*/
	public String getJsonFilePath(){
		return this.jsonFilePath;
	}

	public int getSize(){
		int size = 0;
		if( this.dataList != null ){
			size = this.dataList.size();
		}
		return size;
	}
	
	public List<Object> getData(){
		return this.dataList;
	}
	
	public Map<String, Object> getData(int n){
		Map<String, Object> x = null;
		if( this.dataList != null ){
			x = (Map<String, Object>) this.dataList.get(n);
		}
		return x;
	}
	
	public int getChannel(){
		return this.channel;
	}
	
	public synchronized void addListener(IotSensorDataModelListener listner){
		obs.add(listner);
	}
	public synchronized void removeListener(IotSensorDataModelListener listener) {
		obs.remove(listener);
	}

	public int getIndicatorPosition(){
		return this.samplingIndicator;
	}
	
	private synchronized void notifyUpdateSampleData(){
		Map<String,Object> x = new HashMap<String,Object>();
		x.put("No.", this.samplingIndicator);
		x.put("data", this.signalValue);
		Iterator<IotSensorDataModelListener> it = obs.iterator();
		while(it.hasNext()){
			IotSensorDataModelListener listener = (IotSensorDataModelListener)it.next();
			listener.updateSampleingData(x);
		}
	}
	public void updateSampleData(){
		if( this.channel == 0){
			// 乱数
			this.signalValue = Math.random()*2-1;
		}
		else if( this.channel == 1 ){
			//　sin波
			this.signalValue = Math.sin(this.samplingIndicator*Math.PI/15);
		}
		else{
			// インパルス応答
			this.signalValue = Math.tan(this.samplingIndicator*Math.PI/45);
			if( this.samplingIndicator%20==0 ){
				this.xn0 = 1.0;
			}
			else{
				this.xn0 = 0.0;
			}
			this.signalValue = this.xn0 - 0.5*(this.xn0 - this.yn1)
										+ 0.38*(this.xn0 - this.yn2)
										- 0.03*(this.xn0 - this.yn3);
			this.yn3 = this.yn2;
			this.yn2 = this.yn1;
			this.yn1 = this.signalValue;
		}
		notifyUpdateSampleData();
		this.samplingIndicator++;
	}
}