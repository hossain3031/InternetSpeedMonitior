package com.samsolution.internetspeedmonitior;

//Hex color code opacity
       /*100% — FF
        95% — F2
        90% — E6
        85% — D9
        80% — CC
        75% — BF
        70% — B3
        65% — A6
        60% — 99
        55% — 8C
        50% — 80
        45% — 73
        40% — 66
        35% — 59
        30% — 4D
        25% — 40
        20% — 33
        15% — 26
        10% — 1A
        5% — 0D
        0% — 00*/

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.samsolution.internetspeedmonitior.test.HttpDownloadTest;
import com.samsolution.internetspeedmonitior.test.HttpUploadTest;
import com.samsolution.internetspeedmonitior.test.PingTest;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    static int position = 0;
    static int lastPosition = 0;
    GetSpeedTestHostsHandler getSpeedTestHostsHandler = null;
    HashSet<String> tempBlackList;

    PieChart pieChart;
    ArrayList<Entry> noOfValue = new ArrayList<Entry>();
    ArrayList<String> status = new ArrayList<String>();
    float ping = 0, download = 0, upload = 0;

    @Override
    public void onResume() {
        super.onResume();

        getSpeedTestHostsHandler = new GetSpeedTestHostsHandler();
        getSpeedTestHostsHandler.start();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button startButton = (Button) findViewById(R.id.startButton);
        final DecimalFormat dec = new DecimalFormat("#.##");
        startButton.setText("Test");

        tempBlackList = new HashSet<>();

        getSpeedTestHostsHandler = new GetSpeedTestHostsHandler();
        getSpeedTestHostsHandler.start();

        //Pie Chart init
        pieChart = findViewById(R.id.piechart);

        //*********************************************************************//
        //Value Addition
        /*noOfValue.add(new Entry(945f, 0));
        noOfValue.add(new Entry(1040f, 1));
        noOfValue.add(new Entry(1133f, 2));*/


        //*********************************************************************//


        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startButton.setEnabled(false);
                PieChart piechart = findViewById(R.id.piechart);

                ping = 0;
                download = 0;
                upload = 0;
                status.clear();
                noOfValue.clear();
                piechart.clear();

                //Restart test icin eger baglanti koparsa
                if (getSpeedTestHostsHandler == null) {
                    getSpeedTestHostsHandler = new GetSpeedTestHostsHandler();
                    getSpeedTestHostsHandler.start();
                }

                new Thread(new Runnable() {
                    //RotateAnimation rotate;
                    //ImageView barImageView = (ImageView) findViewById(R.id.barImageView);
                    TextView pingTextView = (TextView) findViewById(R.id.pingTextView);
                    TextView downloadTextView = (TextView) findViewById(R.id.downloadTextView);
                    TextView uploadTextView = (TextView) findViewById(R.id.uploadTextView);

                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.i(TAG, "run: Test Button Clicked 1");
                                startButton.setText("Selecting best server based on ping...");
                                Log.i(TAG, "run: Test Button Clicked 2");
                            }
                        });

                        //Get egcodes.speedtest hosts
                        int timeCount = 600; //1min
                        while (!getSpeedTestHostsHandler.isFinished()) {
                            timeCount--;
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                            }
                            if (timeCount <= 0) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "No Connection...", Toast.LENGTH_LONG).show();
                                        startButton.setEnabled(true);
                                        startButton.setTextSize(16);
                                        startButton.setText("Restart Test");
                                    }
                                });
                                getSpeedTestHostsHandler = null;
                                return;
                            }
                        }

                        //Find closest server
                        HashMap<Integer, String> mapKey = getSpeedTestHostsHandler.getMapKey();
                        HashMap<Integer, List<String>> mapValue = getSpeedTestHostsHandler.getMapValue();
                        double selfLat = getSpeedTestHostsHandler.getSelfLat();
                        double selfLon = getSpeedTestHostsHandler.getSelfLon();
                        double tmp = 19349458;
                        double dist = 0.0;
                        int findServerIndex = 0;
                        for (int index : mapKey.keySet()) {
                            if (tempBlackList.contains(mapValue.get(index).get(5))) {
                                continue;
                            }

                            Location source = new Location("Source");
                            source.setLatitude(selfLat);
                            source.setLongitude(selfLon);

                            List<String> ls = mapValue.get(index);
                            Location dest = new Location("Dest");
                            dest.setLatitude(Double.parseDouble(ls.get(0)));
                            dest.setLongitude(Double.parseDouble(ls.get(1)));

                            double distance = source.distanceTo(dest);
                            if (tmp > distance) {
                                tmp = distance;
                                dist = distance;
                                findServerIndex = index;
                            }
                        }
                        String uploadAddr = mapKey.get(findServerIndex);
                        final List<String> info = mapValue.get(findServerIndex);
                        final double distance = dist;

                        if (info == null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    startButton.setTextSize(12);
                                    startButton.setText("There was a problem in getting Host Location. Try again later.");
                                }
                            });
                            return;
                        }
                        /*[Distance: %s km]    */
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                startButton.setTextSize(13);
                                startButton.setText(String.format("Host Location: %s ", info.get(2), new DecimalFormat("#.##")));
                            }
                        });

                        //Init Ping graphic
                        final LinearLayout chartPing = (LinearLayout) findViewById(R.id.chartPing);
                        XYSeriesRenderer pingRenderer = new XYSeriesRenderer();
                        XYSeriesRenderer.FillOutsideLine pingFill = new XYSeriesRenderer.FillOutsideLine(XYSeriesRenderer.FillOutsideLine.Type.BOUNDS_ALL);
                        pingFill.setColor(Color.parseColor("#667CB342"));     // 40 opacity Green
                        pingRenderer.addFillOutsideLine(pingFill);
                        pingRenderer.setDisplayChartValues(false);
                        pingRenderer.setShowLegendItem(false);
                        pingRenderer.setColor(Color.parseColor("#99FFFF00"));
                        pingRenderer.setLineWidth(5);
                        final XYMultipleSeriesRenderer multiPingRenderer = new XYMultipleSeriesRenderer();
                        multiPingRenderer.setXLabels(0);
                        multiPingRenderer.setYLabels(0);
                        multiPingRenderer.setZoomEnabled(false);
                        multiPingRenderer.setXAxisColor(Color.parseColor("#647488"));
                        multiPingRenderer.setYAxisColor(Color.parseColor("#2F3C4C"));
                        multiPingRenderer.setPanEnabled(true, true);
                        multiPingRenderer.setZoomButtonsVisible(false);
                        multiPingRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
                        multiPingRenderer.addSeriesRenderer(pingRenderer);

                        //Init Download graphic
                        final LinearLayout chartDownload = (LinearLayout) findViewById(R.id.chartDownload);
                        XYSeriesRenderer downloadRenderer = new XYSeriesRenderer();
                        XYSeriesRenderer.FillOutsideLine downloadFill = new XYSeriesRenderer.FillOutsideLine(XYSeriesRenderer.FillOutsideLine.Type.BOUNDS_ALL);
                        downloadFill.setColor(Color.parseColor("#6603A9F4"));   //Blue 40 % Opacity
                        downloadRenderer.addFillOutsideLine(downloadFill);
                        downloadRenderer.setDisplayChartValues(false);
                        downloadRenderer.setColor(Color.parseColor("#99FFFF00"));
                        downloadRenderer.setShowLegendItem(false);
                        downloadRenderer.setLineWidth(5);
                        final XYMultipleSeriesRenderer multiDownloadRenderer = new XYMultipleSeriesRenderer();
                        multiDownloadRenderer.setXLabels(0);
                        multiDownloadRenderer.setYLabels(0);
                        multiDownloadRenderer.setZoomEnabled(false);
                        multiDownloadRenderer.setXAxisColor(Color.parseColor("#647488"));
                        multiDownloadRenderer.setYAxisColor(Color.parseColor("#2F3C4C"));
                        multiDownloadRenderer.setPanEnabled(false, false);
                        multiDownloadRenderer.setZoomButtonsVisible(false);
                        multiDownloadRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
                        multiDownloadRenderer.addSeriesRenderer(downloadRenderer);

                        //Initialization Upload graphic
                        final LinearLayout chartUpload = (LinearLayout) findViewById(R.id.chartUpload);
                        XYSeriesRenderer uploadRenderer = new XYSeriesRenderer();
                        XYSeriesRenderer.FillOutsideLine uploadFill = new XYSeriesRenderer.FillOutsideLine(XYSeriesRenderer.FillOutsideLine.Type.BOUNDS_ALL);
                        uploadFill.setColor(Color.parseColor("#66F44336"));  //Red 40% Opacity
                        uploadRenderer.addFillOutsideLine(uploadFill);
                        uploadRenderer.setDisplayChartValues(false);
                        uploadRenderer.setColor(Color.parseColor("#99FFFF00"));
                        uploadRenderer.setShowLegendItem(false);
                        uploadRenderer.setLineWidth(5);
                        final XYMultipleSeriesRenderer multiUploadRenderer = new XYMultipleSeriesRenderer();
                        multiUploadRenderer.setXLabels(0);
                        multiUploadRenderer.setYLabels(0);
                        multiUploadRenderer.setZoomEnabled(false);
                        multiUploadRenderer.setXAxisColor(Color.parseColor("#647488"));
                        multiUploadRenderer.setYAxisColor(Color.parseColor("#2F3C4C"));
                        multiUploadRenderer.setPanEnabled(false, false);
                        multiUploadRenderer.setZoomButtonsVisible(false);
                        multiUploadRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
                        multiUploadRenderer.addSeriesRenderer(uploadRenderer);

                        //Reset value, graphics
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pingTextView.setText("0 ms");
                                chartPing.removeAllViews();
                                downloadTextView.setText("0 Mbps");
                                chartDownload.removeAllViews();
                                uploadTextView.setText("0 Mbps");
                                chartUpload.removeAllViews();
                            }
                        });
                        final List<Double> pingRateList = new ArrayList<>();
                        final List<Double> downloadRateList = new ArrayList<>();
                        final List<Double> uploadRateList = new ArrayList<>();
                        boolean pingTestStarted = false;
                        boolean pingTestFinished = false;
                        boolean downloadTestStarted = false;
                        boolean downloadTestFinished = false;
                        boolean uploadTestStarted = false;
                        boolean uploadTestFinished = false;

                        //Initialization Test
                        final PingTest pingTest = new PingTest(info.get(6).replace(":8080", ""), 6);
                        Log.i(TAG, "run:000 " + pingTest.toString());
                        final HttpDownloadTest downloadTest = new HttpDownloadTest(uploadAddr.replace(uploadAddr.split("/")[uploadAddr.split("/").length - 1], ""));
                        Log.i(TAG, "run:111 " + downloadTest.toString());
                        final HttpUploadTest uploadTest = new HttpUploadTest(uploadAddr);
                        Log.i(TAG, "run:222 " + uploadTest.toString());

                        //Tests
                        while (true) {
                            if (!pingTestStarted) {
                                pingTest.start();
                                pingTestStarted = true;
                            }
                            if (pingTestFinished && !downloadTestStarted) {
                                downloadTest.start();
                                downloadTestStarted = true;
                            }
                            if (downloadTestFinished && !uploadTestStarted) {
                                uploadTest.start();
                                uploadTestStarted = true;
                            }

                            //Ping Test
                            if (pingTestFinished) {
                                //Failure
                                if (pingTest.getAvgRtt() == 0) {
                                    System.out.println("Ping error...");
                                } else {
                                    //Success
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Log.i(TAG, "run: Success " + dec.format(pingTest.getAvgRtt()) + " ms...");
                                            pingTextView.setText(dec.format(pingTest.getAvgRtt()) + " ms");
                                            ping = Float.parseFloat(dec.format(pingTest.getAvgRtt()));


                                        }
                                    });
                                }
                            } else {
                                pingRateList.add(pingTest.getInstantRtt());

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        pingTextView.setText(dec.format(pingTest.getInstantRtt()) + " ms");
                                    }
                                });

                                //Update chart
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Creating an  XYSeries for Income
                                        XYSeries pingSeries = new XYSeries("");
                                        pingSeries.setTitle("");

                                        int count = 0;
                                        List<Double> tmpLs = new ArrayList<>(pingRateList);
                                        for (Double val : tmpLs) {
                                            pingSeries.add(count++, val);
                                        }

                                        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
                                        dataset.addSeries(pingSeries);

                                        GraphicalView chartView = ChartFactory.getLineChartView(getBaseContext(), dataset, multiPingRenderer);
                                        chartPing.addView(chartView, 0);
                                    }
                                });
                            }

                            //Download Test
                            if (pingTestFinished) {
                                if (downloadTestFinished) {
                                    //Failure
                                    if (downloadTest.getFinalDownloadRate() == 0) {
                                        Log.i(TAG, "run: Failed" + downloadTest.getInstantDownloadRate());
                                        System.out.println("Download error...");
                                    } else {
                                        //Success
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                downloadTextView.setText(dec.format(downloadTest.getFinalDownloadRate()) + " Mbps");
                                                //Log.i(TAG, "run: Success " + dec.format(downloadTest.getFinalDownloadRate()) + " Mbps");

                                            }
                                        });
                                    }
                                } else {
                                    //Calc position
                                    double downloadRate = downloadTest.getInstantDownloadRate();
                                    Log.i(TAG, "run: upload speed: " + downloadTest.getInstantDownloadRate());  // Continuously Updating the upload speed value
                                    downloadRateList.add(downloadRate);
                                    position = getPositionByRate(downloadRate);
                                    download = Float.parseFloat(dec.format(downloadTest.getFinalDownloadRate()));

                                    runOnUiThread(new Runnable() {

                                        @Override
                                        public void run() {
                                            //rotate = new RotateAnimation(lastPosition, position, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                                            //rotate.setInterpolator(new LinearInterpolator());
                                            //rotate.setDuration(100);
                                            // barImageView.startAnimation(rotate);
                                            downloadTextView.setText(dec.format(downloadTest.getInstantDownloadRate()) + " Mbps");
                                            Log.i(TAG, "run: download position " + dec.format(downloadTest.getInstantDownloadRate()) + " Mbps Final");
                                            /*float pingValue = Float.parseFloat(dec.format(uploadTest.getInstantUploadRate()));
                                            noOfValue.add(new Entry(pingValue, 1));*/
                                        }
                                    });
                                    lastPosition = position;
                                    Log.i(TAG, "run: download last position " + dec.format(downloadTest.getInstantDownloadRate()) + " Mbps last Final");

                                    //Update chart
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // Creating an  XYSeries for Income
                                            XYSeries downloadSeries = new XYSeries("");
                                            downloadSeries.setTitle("");

                                            List<Double> tmpLs = new ArrayList<>(downloadRateList);
                                            int count = 0;
                                            for (Double val : tmpLs) {
                                                downloadSeries.add(count++, val);
                                            }

                                            XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
                                            dataset.addSeries(downloadSeries);

                                            GraphicalView chartView = ChartFactory.getLineChartView(getBaseContext(), dataset, multiDownloadRenderer);
                                            chartDownload.addView(chartView, 0);
                                        }
                                    });

                                }
                            }

                            //Upload Test
                            if (downloadTestFinished) {
                                if (uploadTestFinished) {
                                    //Failure
                                    if (uploadTest.getFinalUploadRate() == 0) {
                                        System.out.println("Upload error...");
                                    } else {
                                        //Success
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                uploadTextView.setText(dec.format(uploadTest.getFinalUploadRate()) + " Mbps");
                                                //Log.i(TAG, "run: Success " + dec.format(uploadTest.getFinalUploadRate()) + " Mbps uploaded 395 Line");  //Final Status
                                            }
                                        });
                                    }
                                } else {
                                    //Calc position
                                    double uploadRate = uploadTest.getInstantUploadRate();
                                    uploadRateList.add(uploadRate);
                                    position = getPositionByRate(uploadRate);

                                    runOnUiThread(new Runnable() {

                                        @Override
                                        public void run() {
                                            // rotate = new RotateAnimation(lastPosition, position, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                                            // rotate.setInterpolator(new LinearInterpolator());
                                            // rotate.setDuration(100);
                                            // barImageView.startAnimation(rotate);
                                            uploadTextView.setText(dec.format(uploadTest.getInstantUploadRate()) + " Mbps");
                                            Log.i(TAG, "run: " + dec.format(uploadTest.getInstantUploadRate()) + " Mbps updating");
                                            upload = Float.parseFloat(dec.format(uploadTest.getInstantUploadRate()));
                                        }

                                    });
                                    lastPosition = position;

                                    //Update chart
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // Creating an  XYSeries for Income
                                            XYSeries uploadSeries = new XYSeries("");
                                            uploadSeries.setTitle("");

                                            int count = 0;
                                            List<Double> tmpLs = new ArrayList<>(uploadRateList);
                                            for (Double val : tmpLs) {
                                                if (count == 0) {
                                                    val = 0.0;
                                                }
                                                uploadSeries.add(count++, val);
                                            }

                                            XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
                                            dataset.addSeries(uploadSeries);

                                            GraphicalView chartView = ChartFactory.getLineChartView(getBaseContext(), dataset, multiUploadRenderer);
                                            chartUpload.addView(chartView, 0);
                                        }
                                    });

                                }
                            }


                            //Test bitti
                            if (pingTestFinished && downloadTestFinished && uploadTest.isFinished()) {
                                break;
                            }

                            if (pingTest.isFinished()) {
                                pingTestFinished = true;
                            }
                            if (downloadTest.isFinished()) {
                                downloadTestFinished = true;
                            }
                            if (uploadTest.isFinished()) {
                                uploadTestFinished = true;
                            }

                            if (pingTestStarted && !pingTestFinished) {
                                try {
                                    Thread.sleep(300);
                                } catch (InterruptedException e) {
                                }
                            } else {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                }
                            }
                        }

                        //Thread bitiminde button
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                noOfValue.add(new Entry(ping, 0));
                                noOfValue.add(new Entry(download, 1));
                                noOfValue.add(new Entry(upload, 2));

                                PieDataSet dataSet = new PieDataSet(noOfValue, "Internt Score");
                                dataSet.setValueTextColor(Color.WHITE);

                                status.add("");
                                status.add("");
                                status.add("");

                                PieData data = new PieData(status, dataSet);
                                data.setValueTextColor(Color.WHITE);
                                pieChart.setData(data);
                                dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
                                pieChart.animateXY(3000, 3000);

                                startButton.setEnabled(true);
                                startButton.setTextSize(16);
                                startButton.setText("Test Again");
                            }
                        });
                    }
                }).start();
            }
        });

    }


    public int getPositionByRate(double rate) {
        if (rate <= 1) {
            return (int) (rate * 30);

        } else if (rate <= 10) {
            return (int) (rate * 6) + 30;

        } else if (rate <= 30) {
            return (int) ((rate - 10) * 3) + 90;

        } else if (rate <= 50) {
            return (int) ((rate - 30) * 1.5) + 150;

        } else if (rate <= 100) {
            return (int) ((rate - 50) * 1.2) + 180;
        }

        return 0;
    }

}
