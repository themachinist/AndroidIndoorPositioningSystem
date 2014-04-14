package com.example.nwen404P1;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by sriram on 12/04/14.
 */
public class APLister extends Activity implements View.OnClickListener {
    private static final int showX = 300;
    Map<String, Double> macToLevel;
    Map<String, Integer> macToFreq;
    TextView text;
    List<String> macs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listap);

        text = (TextView) findViewById(R.id.text);

        final Handler h = new Handler();

        final WifiManager mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        macToFreq = new HashMap<String, Integer>();
        macToLevel = new HashMap<String, Double>();

        final double factor = 0.96;

        final Button button = (Button) findViewById(R.id.clear);
        button.setOnClickListener(this);


        macs = new LinkedList<String>();

        Runnable r = new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                mainWifi.startScan();

                List<String> macsFound = new LinkedList<String>();

                for (ScanResult scanResult : mainWifi.getScanResults()) {
                    if (!macToLevel.containsKey(scanResult.BSSID)) {
                        macToLevel.put(scanResult.BSSID, (double) scanResult.level);
                    } else {
                        double prev = macToLevel.get(scanResult.BSSID);
                        macToLevel.put(scanResult.BSSID, factor * prev + (1.0 - factor) * (double) scanResult.level);
                    }

                    if (!macs.contains(scanResult.BSSID)) {
                        macs.add(scanResult.BSSID);
                    }

                    macToFreq.put(scanResult.BSSID, scanResult.frequency);
                    macsFound.add(scanResult.BSSID);
                }

                updateResults();


                h.postDelayed(this, 50);
            }
        };

        r.run();

    }

    public void updateResults() {
        String results = "";
        int remaining = showX;
        for (String mac : macs) {
            if (remaining-- == 0) break;

            DecimalFormat f = new DecimalFormat("###.00");

            double distance = 0.0;
            //String[] name = WifiNames.getMacToName().get(mac);
            CottonAP filter = new CottonAP();

            if (filter.filter(mac,2)) {
                distance = strengthToDistance(macToLevel.get(mac), 1000000.0 * macToFreq.get(mac));

                results += filter.getAPByMac(mac).getDescription() + " : " + mac + " : " + f.format(macToLevel.get(mac)) +
                        " (== " + f.format(distance) + " m)" + 1000000.0 * macToFreq.get(mac);
            }
        }
        text.setText(results);
    }

    private double strengthToDistance(double level, double freq) {
        final double a = -0.07363796;
        final double b = -2.52218124;


        final double C = 299792458.0;
        final double ROUTER_HEIGHT = 2.5;

        double n = Math.max(2, a * level + b);
        level = -level;
        double wavelength = C / freq;
        double FSPL = 20.0 * Math.log10(4.0 * Math.PI / wavelength);
        double directDistanceM = Math.pow(10, (level - FSPL) / (10.0 * n));

        directDistanceM = Math.max(directDistanceM, ROUTER_HEIGHT);

        double distancePlaneM = Math.sqrt(Math.pow(directDistanceM, 2) - Math.pow(ROUTER_HEIGHT, 2));
        return distancePlaneM;

    }


    @Override
    public void onClick(View v) {
        macToLevel.clear();
        macToFreq.clear();
        macs.clear();
        text.clearComposingText();

    }
}