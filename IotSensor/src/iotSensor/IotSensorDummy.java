package iotSensor;

import java.util.ArrayList;
import java.util.List;

import iotSensorDataModel.IotSensorDataModel;

import java.util.Iterator;

public class IotSensorDummy extends Thread {
	
	private boolean started = false;
	private IotSensorDataModel dataModel;
	private boolean samplingStatus = false;
	private boolean loadingStatus = false;
	private List<IotSensorListener> obs = new ArrayList<IotSensorListener>();

	public IotSensorDummy(IotSensorDataModel dataModel){
		this.dataModel = dataModel;
	}
	
	public IotSensorDataModel getDataModel(){
		return this.dataModel;
	}
	
	public boolean getSamplingStatus(){
		return this.samplingStatus;
	}
	public boolean getLoadingStatus(){
		return this.loadingStatus;
	}
	
	public synchronized void addListener(IotSensorListener listner){
		obs.add(listner);
	}
	public synchronized void removeListener(IotSensorListener listener) {
		obs.remove(listener);
	}
	
	public void run(){
		this.started =true;
		while(started){
			this.dataModel.updateSampleData();
			try{
				Thread.yield();
				Thread.sleep(1000);
			}catch(Exception e){}
		}
	}
	
	private void terminate(){
		this.started = false;
	}

	//　Load・Unload
	private synchronized void notyfyLoadingStatusChange(){
		Iterator<IotSensorListener> it = obs.iterator();
		while(it.hasNext()){
			IotSensorListener listener = it.next();
			listener.loadingStatusChanged(this.loadingStatus);
		}
	}
	
	public void loading(){
		if(!this.loadingStatus){
			this.loadingStatus = true;
			this.notyfyLoadingStatusChange();
		}
	}
	public void unloading(){
		if(this.loadingStatus){
			this.loadingStatus = false;
			this.notyfyLoadingStatusChange();
		}
	}
	
	//　Start・Stop
	private synchronized void notyfySamplingStatusChange(){
		Iterator<IotSensorListener> it = obs.iterator();
		while(it.hasNext()){
			IotSensorListener listener = it.next();
			listener.samplingStatusChanged(this.samplingStatus);
		}
	}

	public void startSampling(){
		if(!this.samplingStatus){
			this.samplingStatus = true;
			this.notyfySamplingStatusChange();
			new Thread(this).start();
		}
	}
	
	public void stopSampling(){
		if(this.samplingStatus){
			this.samplingStatus = false;
			this.terminate();
			try{
				this.join(1000);
			}catch(InterruptedException e){}
			this.notyfySamplingStatusChange();
		}
	}
}
