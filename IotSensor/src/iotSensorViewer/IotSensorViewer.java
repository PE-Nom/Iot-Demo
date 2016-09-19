package iotSensorViewer;

import java.util.Map;
import java.util.Iterator;
import java.awt.Color;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.axis.ValueAxis;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.experimental.chart.swt.ChartComposite;

import iotSensor.*;
import iotSensorDataModel.*;

public class IotSensorViewer implements IotSensorDataModelListener, IotSensorListener{
	
	static Logger logger = Logger.getLogger(IotSensorViewer.class.getName());

	private int xscaleRange = 180;
	private int channel;
	private Shell shell;
	private IotSensorDataModel dataModel;
	private IotSensorDummy sensor;
	private ChartComposite chartComp;
	private Color[] colors = {Color.RED, Color.CYAN, Color.GREEN};
	private XYSeriesCollection data;
	private XYSeries sig;
	private Button sensorLoadBtn;
	private Button startSampBtn;
	private Button startMonitorBtn;
	private Text txt;
	private CLabel sensorLoadingStatuslbl;
	private CLabel samplingStatuslbl;
	private CLabel monitorStatuslbl;
	private boolean monitorStatus = false;
	
	private Map<String,Object> arrivalData;
	private XYPlot xyPlot;
	private XYItemRenderer renderer;
	private ValueAxis xscale;
	private ValueAxis yscale;
	
	public IotSensorViewer(IotSensorDummy sensor, Shell shell){
		this.sensor = sensor;
		this.dataModel = sensor.getDataModel();
		this.channel = this.dataModel.getChannel();
		this.shell = shell;
		this.data = new XYSeriesCollection();
	    createTimeSerieseChart();
	    createLoadSensorButton();
	    createSensorLoadingStatusLabel();
	    createStartSamplingButton();
	    createSamplingStatusLabel();
	    createStartMonitorButton();
	    createMonitoringStatusLabel();
	    createTextBox();
	    this.sensor.addListener(this);
	    startMonitor();
	}

	//　時系列グラフチャート
	private void createTimeSerieseChart(){

		// 擬似センサデータの設定
		this.sig = new XYSeries("Sensor Sig.");
		this.data.addSeries(sig);
		
		//　グラフ作成
	    JFreeChart chart = 
	    		ChartFactory.createXYLineChart(
	    				"Pseudo Sensor"+this.channel+" Signal",
	    				"points",
	    				"value",
	    				data,
	    				PlotOrientation.VERTICAL, //* グラフ描画方向(HORIZONTAL)
	    				true,
	    				false,
	    				false);
	    
	    chart.setBackgroundPaint(Color.LIGHT_GRAY);

	    this.xyPlot = chart.getXYPlot();
	    this.xyPlot.setBackgroundPaint(Color.BLACK); // 背景を黒く
	    this.xyPlot.setDomainGridlinePaint(Color.white); //* 縦区切り線 ...by awt
	    this.xyPlot.setRangeGridlinePaint(Color.white); //* 横区切り線 ...by awt
		this.renderer = xyPlot.getRenderer();
		this.renderer.setSeriesPaint(0, colors[this.channel]); //* グラフ線の色
		
		this.yscale = xyPlot.getRangeAxis();
		this.yscale.setRange(-1.1,1.1);
		this.xscale = xyPlot.getDomainAxis();
		this.xscale.setRange(0,xscaleRange);
	
	    GridData grid = new GridData();
	    grid.verticalSpan = 4;
	    grid.verticalAlignment = GridData.FILL;
	    grid.horizontalAlignment = GridData.FILL;
	    grid.widthHint=1024;
	    
	    // コンテキストメニューなしで構築
		this.chartComp = new ChartComposite(this.shell,SWT.NONE,chart,false,false,false,false,false);
		this.chartComp.setLayoutData(grid);
		// マウス操作でのズームイン／アウト　disable
		this.chartComp.setDomainZoomable(false);
		this.chartComp.setRangeZoomable(false);
		
	}

	// SensorLoadingボタンが押下された時のイベント処理
	private class sensorLoadingBtnListener implements SelectionListener{
		public sensorLoadingBtnListener(){
		}
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			if(IotSensorViewer.this.sensor.getLoadingStatus()){
				IotSensorViewer.this.sensor.unloading();
			}else{
				IotSensorViewer.this.sensor.loading();
			}
		}
	}
	
	/*
	 * IotSensorlListener のイベントリスナー
	 */
	public void loadingStatusChanged(boolean status){
		this.shell.getDisplay().asyncExec(new UpdateloadingStatusChangedChagedCmd());
	}
	
	private class UpdateloadingStatusChangedChagedCmd implements Runnable{
		public UpdateloadingStatusChangedChagedCmd(){
			
		}
		public void run(){
			if(IotSensorViewer.this.sensor.getLoadingStatus()){
				IotSensorViewer.this.sensorLoadBtn.setText("UNLOAD");
				IotSensorViewer.this.sensorLoadingStatuslbl.setText("Loaded!");
				IotSensorViewer.this.sensorLoadingStatuslbl.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
				IotSensorViewer.this.sensorLoadingStatuslbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
			}else{
				IotSensorViewer.this.sensorLoadBtn.setText("LOAD");
				IotSensorViewer.this.sensorLoadingStatuslbl.setText("Unloaded!");
				IotSensorViewer.this.sensorLoadingStatuslbl.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
				IotSensorViewer.this.sensorLoadingStatuslbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			}
		}
	}

	//　Monitor Startボタンが押下された時のイベント処理
	private class startMonitoringBtnListener implements SelectionListener{
		public startMonitoringBtnListener(){
		}
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			if(IotSensorViewer.this.monitorStatus){
				IotSensorViewer.this.stopMonitor();
			}else{
				IotSensorViewer.this.startMonitor();
			}
		}
	}

	private void startMonitor(){
		if(!this.monitorStatus){
			this.dataModel.addListener(this);
			this.monitorStatus = true;
			this.startMonitorBtn.setText("STOP MONITOR");
			this.monitorStatuslbl.setText("Now Monitoring!");
			this.monitorStatuslbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
			this.monitorStatuslbl.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		}
	}
	private void stopMonitor(){
		if( this.monitorStatus ){
			this.monitorStatus = false;
			this.dataModel.removeListener(this);
			this.startMonitorBtn.setText("START MONITOR");
			this.monitorStatuslbl.setText("Monitor Stopped!");
			this.monitorStatuslbl.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
			this.monitorStatuslbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		}
	}
	
	// Sampling Startボタンが押下されたときのイベント処理
	//　プロトタイプでは、インジケータの位置を変化させる。
	private class startSamplingBtnListener implements SelectionListener{
		public startSamplingBtnListener(){
		}
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			if(IotSensorViewer.this.sensor.getSamplingStatus()){
				IotSensorViewer.this.stopSampling();
			}else{
				IotSensorViewer.this.startSampling();
			}
		}
	}
	private void startSampling(){
		this.sensor.startSampling();
	}
	private void stopSampling(){
		this.sensor.stopSampling();
	}

	// Window 終了ボタンの処理
	public void fin(){
		this.stopSampling();
		this.stopMonitor();
	}

	/*
	 * IotSensorDataModelListener のイベントリスナー
	 */
	public void updateSampleingData(Map<String,Object> arrivalData){
		this.arrivalData = arrivalData;
		// UIスレッドへ更新処理を委譲
		//　UIスレッド以外からは画面の更新はできない
		this.shell.getDisplay().asyncExec(new UpdateDataCmd(this.arrivalData));
	}
	
	// UIスレッドで実行される更新処理の実体
	private class UpdateDataCmd implements Runnable{
		Map<String,Object> addData;
		public UpdateDataCmd(Map<String,Object> addData){
			this.addData = addData;
		}
		public void run(){
			XYDataItem newData = new XYDataItem(Double.valueOf(this.addData.get("No.").toString()),
					 							Double.valueOf(this.addData.get("data").toString()));
			IotSensorViewer.this.sig.add(newData);
			if( IotSensorViewer.this.xscale.getUpperBound() < IotSensorViewer.this.sig.getMaxX() )
			{
				// 先頭データの削除
				if( xscaleRange < IotSensorViewer.this.sig.getItemCount()){
					IotSensorViewer.this.sig.remove(0);
				}
				IotSensorViewer.this.xscale.setRange(IotSensorViewer.this.sig.getMaxX() - xscaleRange,
													 IotSensorViewer.this.sig.getMaxX());
			}
			IotSensorViewer.this.txt.append("No."      + this.addData.get("No.").toString() + ", " +
										 	"data :" + this.addData.get("data").toString() + "\n");
		}
	}
	
	/*
	 * IotSensorlListener のイベントリスナー
	 */
	public void samplingStatusChanged(boolean status){
		this.shell.getDisplay().asyncExec(new UpdateSamplingStatusChagedCmd());
	}
	
	private class UpdateSamplingStatusChagedCmd implements Runnable{
		public UpdateSamplingStatusChagedCmd(){
			
		}
		public void run(){
			if(IotSensorViewer.this.sensor.getSamplingStatus()){
				IotSensorViewer.this.startSampBtn.setText("STOP SENSOR");
				IotSensorViewer.this.samplingStatuslbl.setText("Sensor Started!");
				IotSensorViewer.this.samplingStatuslbl.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
				IotSensorViewer.this.samplingStatuslbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED));
			}else{
				IotSensorViewer.this.startSampBtn.setText("START SENSOR");
				IotSensorViewer.this.samplingStatuslbl.setText("Sensor Stopped!");
				IotSensorViewer.this.samplingStatuslbl.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
				IotSensorViewer.this.samplingStatuslbl.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			}
		}
	}

	// Sensor ロード・アンロードボタン
	private void createLoadSensorButton(){
	    /*
	     * Button Widgetのセット
	     */
	    GridData grid = new GridData();
	    grid.verticalAlignment = GridData.FILL;
	    grid.horizontalAlignment = GridData.FILL;
	    grid.widthHint=120;
	    
	    this.sensorLoadBtn = new Button(this.shell, SWT.BORDER);
		if(this.sensor.getLoadingStatus()){
			this.sensorLoadBtn.setText("UNLOAD");
		}else{
			this.sensorLoadBtn.setText("LOAD");
		}
	    this.sensorLoadBtn.setLayoutData(grid);
	    this.sensorLoadBtn.addSelectionListener(new sensorLoadingBtnListener());
	}
	// Sensor ロード・アンロード状態表示ラベル
	private void createSensorLoadingStatusLabel(){
	    /*
	     * Label Widgetのセット
	     */
	    GridData grid = new GridData();
	    grid.verticalAlignment = GridData.FILL;
	    grid.horizontalAlignment = GridData.FILL;
	    grid.widthHint=120;
	    
	    this.sensorLoadingStatuslbl = new CLabel(this.shell, SWT.CENTER|SWT.BORDER|SWT.SINGLE);
		if(this.sensor.getLoadingStatus()){
		    this.sensorLoadingStatuslbl.setText("Sensor loaded!");
		}else{
		    this.sensorLoadingStatuslbl.setText("Sensor unloaded!");
		}
	    this.sensorLoadingStatuslbl.setLayoutData(grid);
	}
	
	// サンプリング開始・停止ボタン
	private void createStartSamplingButton(){
	    /*
	     * Button Widgetのセット
	     */
	    GridData grid = new GridData();
	    grid.verticalAlignment = GridData.FILL;
	    grid.horizontalAlignment = GridData.FILL;
	    grid.widthHint=120;
	    
	    this.startSampBtn = new Button(this.shell, SWT.BORDER);
		if(this.sensor.getSamplingStatus()){
			this.startSampBtn.setText("STOP SENSOR");
		}else{
			this.startSampBtn.setText("START SENSOR");
		}
	    this.startSampBtn.setLayoutData(grid);
	    this.startSampBtn.addSelectionListener(new startSamplingBtnListener());
	}
	// サンプリング状態表示ラベル
	private void createSamplingStatusLabel(){
	    /*
	     * Label Widgetのセット
	     */
	    GridData grid = new GridData();
	    grid.verticalAlignment = GridData.FILL;
	    grid.horizontalAlignment = GridData.FILL;
	    grid.widthHint=120;
	    
	    this.samplingStatuslbl = new CLabel(this.shell, SWT.CENTER|SWT.BORDER|SWT.SINGLE);
		if(this.sensor.getSamplingStatus()){
		    this.samplingStatuslbl.setText("Sensor started!");
		}else{
		    this.samplingStatuslbl.setText("Sensor stopped!");
		}
	    this.samplingStatuslbl.setLayoutData(grid);
	}

	// モニタ開始・停止ボタン
	private void createStartMonitorButton(){
	    /*
	     * Button Widgetのセット
	     */
	    GridData grid = new GridData();
	    grid.verticalAlignment = GridData.FILL;
	    grid.horizontalAlignment = GridData.FILL;
	    grid.widthHint=120;
	    
	    this.startMonitorBtn = new Button(this.shell, SWT.BORDER);
		this.startMonitorBtn.setText("START MONITOR");
	    this.startMonitorBtn.setLayoutData(grid);
	    this.startMonitorBtn.addSelectionListener(new startMonitoringBtnListener());
	}
	// モニタ状態表示ラベル
	private void createMonitoringStatusLabel(){
	    /*
	     * Label Widgetのセット
	     */
	    GridData grid = new GridData();
	    grid.verticalAlignment = GridData.FILL;
	    grid.horizontalAlignment = GridData.FILL;
	    grid.widthHint=120;
	    
	    this.monitorStatuslbl = new CLabel(this.shell, SWT.CENTER|SWT.BORDER);
		if(this.monitorStatus){
		    this.monitorStatuslbl.setText("Monitor stopped!");
		}else{
		    this.monitorStatuslbl.setText("Now Monitoring!");
		}
	    this.monitorStatuslbl.setLayoutData(grid);
	}

	// モニタログ表示リスト
	private void createTextBox(){
	    /*
	     * TextBox Widgetのセット
	     */
	    GridData grid = new GridData();
	    grid.horizontalSpan = 2;
	    grid.heightHint = 240;
	    grid.widthHint=240;

	    // 単一行テキストボックスを作成
	    this.txt = new Text(shell,SWT.MULTI|SWT.BORDER|SWT.V_SCROLL);
	    this.txt.setLayoutData(grid);
	}
	
}
