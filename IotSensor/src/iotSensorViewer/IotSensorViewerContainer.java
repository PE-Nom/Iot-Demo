package iotSensorViewer;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import iotSensor.*;
import iotSensorDataModel.*;

import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellAdapter;

public class IotSensorViewerContainer {

	static Logger logger = Logger.getLogger(IotSensorViewerContainer.class.getName());

	private Shell	shell;
	private Display	display;
	private ArrayList<IotSensorViewer> grp = new ArrayList<IotSensorViewer>();
	
	private IotSensorDataModelContainer dataModelContainer;
	private IotSensorContainer sensors;
	
	public IotSensorViewerContainer(IotSensorContainer sensors){
		this.sensors = sensors;
		this.dataModelContainer = sensors.getDataModelContainer();
		setupComponent(this.dataModelContainer.getChannels());
	}
	
	private void setupComponent(int channels){
		this.display = new Display ();
		this.shell = new Shell(this.display,SWT.TITLE|SWT.MIN|SWT.MAX|SWT.CLOSE);
		this.shell.setSize(1024, 768);
		this.shell.setText("MonitorView Sample1");

		this.shell.setLayout(new GridLayout(3,false));

		this.shell.addShellListener(new ShellAdapter(){
			public void shellClosed(ShellEvent event) {
				Iterator<IotSensorViewer> it = grp.iterator();
				while(it.hasNext()){
					IotSensorViewer chartGroup = (IotSensorViewer)it.next();
					chartGroup.fin();
				}
				// asyncExec　イベントキュー内のIotDemoSampleChartGroupオブジェクトがなくなるまで waitする
				//　そうしないとdisplay.disposeでExceptionが発生する。
				while(IotSensorViewerContainer.this.display.readAndDispatch()){}
				event.doit = true;
				return;
			}
		});
		
		Iterator<IotSensorDummy> it = sensors.getSensors().iterator();
		while(it.hasNext()){
			IotSensorDummy sensor = it.next();
			IotSensorViewer chart = new IotSensorViewer(sensor, this.shell);
			this.grp.add(chart);
		}
		IotSensorViewerContainer.logger.info("setup Complete");
	}
	
	public void dispatch(){
	    this.shell.pack();
	    this.shell.open();
	    while (!this.shell.isDisposed ()){
	      if (!this.display.readAndDispatch ()){
	    	  this.display.sleep ();
	      }
	    }
	    this.display.dispose ();
	}
}
