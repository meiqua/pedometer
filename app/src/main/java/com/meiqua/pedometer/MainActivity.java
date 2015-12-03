package com.meiqua.pedometer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart;
import org.achartengine.chart.LineChart;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.lang.reflect.Array;

import app.akexorcist.bluetotohspp.library.*;

public class MainActivity extends AppCompatActivity {

    BluetoothSPP bt;
    MenuItem AverageItem=null;
    MenuItem FIRfilerItem=null;

    public int pedometer=0;
    public int PedometerCounterLag=0;//last step
    final int refreshTime=10;
    public boolean PedoFlag=false;
    final int amplitude=1000;
    //refresh pedometer for refreshTime/(100HZ/FenPin)

    public int counter = 0;
    public int counterLag=0;

    public int fenPin=0;
    final int fenPinConst=4;

    final int size = 500;
    final int half = size / 2;
    public int[] Array0 = new int[size];
    public int[] Array1 = new int[size];

    public int[] FilterArray0 = new int[size];
    public int[] FilterArray1 = new int[size];
    final int filterOrder=6;

final double[] FilterParams={-0.038863591290741892,
        -0.071472482469963419,0.28886359129074191
        ,0.58926375876501835,0.28886359129074191
        ,-0.071472482469963419,-0.038863591290741892};
    public boolean FilterFlag=false;
    public boolean updatePedometerFlag=false;
    //number of params must equal to order+1

    public int[] AverageArray0 = new int[size];
    public int[] AverageArray1 = new int[size];
    final int averageOrder=6;
    //to keep pace with filter,real averageNumber=averageOrder+1
    public boolean AverageFlag=false;

    public boolean updateFlag=false;
    public int chartFlag=0;

    private GraphicalView mChart;

    private XYSeries visitsSeries ;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bt = new BluetoothSPP(this);
        bt.setupService();
        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            public void onDataReceived(byte[] data, String message) {
                TextView textView = (TextView) findViewById(R.id.textView);
                TextView pedoView=(TextView)findViewById(R.id.pedometer);
                Model model = new Model();
                fenPin++;
                if (fenPin%fenPinConst==0)
                {
                    model.parseData(message, counter);
                    textView.setText(model.toString());
                    counter++;
                    save(model);
                    if (FilterFlag){
                        FIRfilter();
                        if (AverageFlag){
                            AverageFilter();
                        }
                    }
                    if (PedoFlag)
                    Pedometer();
                    fenPin=0;
                    if (updatePedometerFlag){
                        pedoView.setText("step:  "+Integer.toString(pedometer));
                        updatePedometerFlag=false;
                    }
                }

            }
        });
        // Setting up chart
        setupChart();
        // Start plotting chart

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        AverageItem=menu.findItem(R.id.Average);
        FIRfilerItem=menu.findItem(R.id.FIR);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.connect) {
            if (item.getTitle().equals("disconnect")) {
                bt.disconnect();
                bt.send("Stop", false);
                item.setTitle("connect");
            } else {
                bt.setDeviceTarget(BluetoothState.DEVICE_OTHER);
                Intent intent = new Intent(getApplicationContext(), DeviceList.class);
                startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
                item.setTitle("disconnect");
            }

        }
        if (id == R.id.start) {
            bt.send("Start", false);
            visitsSeries.clear();
            chartFlag++;
            if (chartFlag>1)
                chartFlag=2;
            if (chartFlag==1){
                new ChartTask().execute();
                //make the task only execute once
                PedoFlag=true;
            }
            pedometer=0;
        }

        if (id == R.id.FIR) {
            if (FilterFlag){
                FilterFlag=false;
                AverageItem.setEnabled(false);
                FIRfilerItem.setTitle("FIR");
            }else {
                FilterFlag=true;
                AverageItem.setEnabled(true);
                FIRfilerItem.setTitle("FIRok");
            }
        }

        if (id == R.id.Average){
            if (AverageFlag){
                AverageFlag=false;
                AverageItem.setTitle("ave");
            }else {
                AverageFlag=true;
                AverageItem.setTitle("aveOK");
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK)
                bt.connect(data);
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_ANDROID);
                //        setup();

            } else {
                // Do something if user doesn't choose any device (Pressed back)
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bt.disconnect();
    }

    private void save(Model model) {
        if (counter == size)
            counter = 0;
        if (counter < half) {
            Array0[counter] = model.getaZ();
            Array1[counter + half] = model.getaZ();
        } else if (counter >= half) {
            Array0[counter] = model.getaZ();
            Array1[counter - half] = model.getaZ();
        }
    }

    private class ChartTask extends AsyncTask<Void, String, Void> {
        // Generates data in a non-ui thread
        @Override
        protected Void doInBackground(Void... params) {
            for (;;){
                int i = 0;
                int updateNum=counter-counterLag;
                if (updateNum<0)
                    updateNum=updateNum+size;
                int[] Array=readForUpdate(updateNum);
                try {
                    do{
                        String [] values = new String[1];
                        values[0]=Array[i]+"";
                        publishProgress(values);
                        i++;
                    } while(i<updateNum);
                    counterLag=counter;
                    Thread.sleep(150);
                    updatePedometerFlag=true;
                    updateFlag=true;
                } catch (Exception e) {

                }
            }
         //   return null;
        }
        // Plotting generated data in the graph
        @Override
        protected void onProgressUpdate(String... values) {
            if(visitsSeries!=null)
            {
               // visitsSeries.remove();
                visitsSeries.add(visitsSeries.getItemCount(), Integer.parseInt(values[0]));
            }
            if (updateFlag)
            mChart.repaint();
        }
    }

    private void setupChart() {

        XYMultipleSeriesDataset dataset;
         XYSeriesRenderer visitsRenderer;
         XYMultipleSeriesRenderer multiRenderer;
         visitsSeries = new XYSeries("Acc-Time");

        // Creating a dataset to hold each series
           dataset = new XYMultipleSeriesDataset();
        // Adding Visits Series to the dataset
         dataset.addSeries(visitsSeries);

        // Creating XYSeriesRenderer to customize visitsSeries
        visitsRenderer = new XYSeriesRenderer();
        visitsRenderer.setColor(Color.BLUE);
        visitsRenderer.setPointStyle(PointStyle.CIRCLE);
        visitsRenderer.setFillPoints(true);
        visitsRenderer.setLineWidth(2);
        visitsRenderer.setDisplayChartValues(false);

        // Creating a XYMultipleSeriesRenderer to customize the whole chart
         multiRenderer = new XYMultipleSeriesRenderer();

        multiRenderer.setChartTitle("ACC-Time Chart");
        multiRenderer.setXTitle("Time");
        multiRenderer.setYTitle("Acc");

 //       multiRenderer.setZoomButtonsVisible(true);

        multiRenderer.setXAxisMin(0);
        multiRenderer.setXAxisMax(size);
        multiRenderer.setYAxisMin(-20000);
        multiRenderer.setYAxisMax(20000);
        multiRenderer.setLabelsTextSize(20);


        multiRenderer.setShowGrid(true);



        // Adding visitsRenderer to multipleRenderer
        // Note: The order of adding dataseries to dataset and renderers to multipleRenderer
        // should be same
        multiRenderer.addSeriesRenderer(visitsRenderer);

        // Getting a reference to LinearLayout of the MainActivity Layout
        LinearLayout chartContainer = (LinearLayout) findViewById(R.id.chart_container);

       mChart = (GraphicalView) ChartFactory.getLineChartView(this, dataset, multiRenderer);
      //  mChart = (GraphicalView) ChartFactory.getBarChartView(getBaseContext(), dataset, multiRenderer, BarChart.Type.DEFAULT);

        // Adding the Line Chart to the LinearLayout
        chartContainer.addView(mChart);
    }
    private  int[] readForUpdate(int num){
        int[] array=new int[num];
        if (counterLag<half){
            for (int i=0;i<num;i++){
                if (FilterFlag){
                    if (AverageFlag) {
                        array[i]=AverageArray0[counterLag+i];
                    }else {
                        array[i]=FilterArray0[counterLag+i];
                    }
                }else {
                    array[i]=Array0[counterLag+i];
                }

            }
        }else if (counterLag>half){
            for (int i=0;i<num;i++){
                if (FilterFlag){
                    if (AverageFlag) {
                        array[i]=AverageArray1[counterLag-half+i];
                    }else {
                        array[i]=FilterArray1[counterLag-half+i];
                    }
                }else {
                    array[i]=Array1[counterLag-half+i];
                }
            }
        }
        return array;
    }

    private void FIRfilter(){
          int filterCounterLag=counter-filterOrder;
        if (filterCounterLag<0)
            filterCounterLag=filterCounterLag+size-1;
            if (counter < half) {
                double sum=0;
                for (int i=0;i<filterOrder+1;i++) {
                    if (filterCounterLag<half)
                        sum=sum+FilterParams[i]*Array0[filterCounterLag+i];
                    else if (filterCounterLag>=half)
                        sum=sum+FilterParams[i]*Array1[filterCounterLag+i-half];
                }
                FilterArray0[counter]=(int)sum;
                FilterArray1[counter+half]=(int)sum;
            } else if (counter >= half) {
                double sum=0;
                for (int i=0;i<filterOrder+1;i++) {
                    if (filterCounterLag<half)
                        sum=sum+FilterParams[i]*Array0[filterCounterLag+i];
                    else if (filterCounterLag>=half)
                        sum=sum+FilterParams[i]*Array1[filterCounterLag+i-half];
                }
                FilterArray0[counter]=(int)sum;
                FilterArray1[counter-half]=(int)sum;
            }

    }
    private void AverageFilter(){
        int filterCounterLag=counter-averageOrder;
        if (filterCounterLag<0)
            filterCounterLag=filterCounterLag+size-1;
        if (counter < half) {
            float sum=0;
            int underSum=0;
            for (int i=0;i<averageOrder+1;i++) {
                if (filterCounterLag<half)
                    sum=sum+(averageOrder+1-i)*FilterArray0[filterCounterLag+i];
                else if (filterCounterLag>=half)
                    sum=sum+(averageOrder+1-i)*FilterArray1[filterCounterLag+i-half];
                  underSum=underSum+averageOrder+1-i;
            }
            sum=sum/underSum;
            AverageArray0[counter]=(int)sum;
            AverageArray1[counter+half]=(int)sum;
        } else if (counter >= half) {
            float sum=0;
            int underSum=0;
            for (int i=0;i<averageOrder+1;i++) {
                if (filterCounterLag<half)
                    sum=sum+(averageOrder+1-i)*FilterArray0[filterCounterLag+i];
                else if (filterCounterLag>=half)
                    sum=sum+(averageOrder+1-i)*FilterArray1[filterCounterLag+i-half];
                underSum=underSum+averageOrder+1-i;
            }
            sum=sum/underSum;
            AverageArray0[counter]=(int)sum;
            AverageArray1[counter-half]=(int)sum;
        }
    }
    private void Pedometer(){
        if (counter%refreshTime==0){
       //     int stepCounter=PedometerCounterLag;
            int step=0;
            int length=0;
            int start=PedometerCounterLag;
            int end=counter;
            int[] TempArray0 = Array0;
            int[] TempArray1 = Array1;
            if (FilterFlag){
                if (AverageFlag){
                    TempArray0 = AverageArray0;
                    TempArray1 = AverageArray1;
                }else {
                    TempArray0 = FilterArray0;
                    TempArray1 = FilterArray1;
                }
            }

            if(end>start){
                length=end-start+1;//include end point
            }else if (end<start){
                length=end-start+size+1;//include end point
            }
            if (length>0){
                int[] array=new int[length];
                for (int i=0;i<length;i++){

                    if (start+i<half){
                        array[i]=TempArray0[start+i];
                    }else if (start+i>=half){
                        if (start+i>size-1){
                            array[i]=TempArray0[start+i-size];
                        }else
                            array[i]=TempArray1[start+i-half];
                    }
                }
                for (int i=1;i<length;i++){
                    //from i=i will ensure no point cover or miss
                    if (array[i-1]<amplitude){
                    if (array[i]>amplitude){
                            step++;
                           Log.i("step", "Pedometer: "+start+"  "+end);
                        }
                    }
                }
            //    Log.i("step", "Pedometer-stepCounter: "+stepCounter);
            }
//            if (stepCounter>size-1)
//                stepCounter=stepCounter-size;
            pedometer=pedometer+step;
            PedometerCounterLag=counter;
        }
    }
}
