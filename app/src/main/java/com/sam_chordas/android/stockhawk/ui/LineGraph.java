package com.sam_chordas.android.stockhawk.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class LineGraph extends Activity {

    LineSet mlineSet;
    String mSymbol;
    int result_count=0;
    LineChartView chartView;
    int num_of_days= -15;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);

        chartView= ((LineChartView)findViewById(R.id.linechart));
        mlineSet= new LineSet();

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras == null) {
                mSymbol= null;
            } else {
                mSymbol= extras.getString("symbol");
            }
        } else {
            mSymbol= (String) savedInstanceState.getSerializable("symbol");
        }

        new FetchGraphData().execute(mSymbol);
    }

    public class FetchGraphData extends AsyncTask<String,Void,Void>
    {
        public ProgressDialog pd;
        String detailsJsonString;
        String urlStringBuilder = new String();
        String mDateStart,mDateEnd;

        void getDatafromJson(String jsonString)throws JSONException
        {
            final String QUERY_JSON="query";
            final String COUNT_JSON="count";
            final String RESULTS_JSON="results";
            final String QUOTE_JSON="quote";
            final String DATE_JSON="Date";
            final String CLOSE_JSON="Close";

            if(jsonString==null)
            {
                Log.v("gettingdata","null data");
                return;
            }
            Log.v("debug",jsonString);
            JSONObject query= (new JSONObject(jsonString)).getJSONObject(QUERY_JSON);
            result_count= query.getInt(COUNT_JSON);
            JSONObject results= query.getJSONObject(RESULTS_JSON);
            JSONArray quotes= results.getJSONArray(QUOTE_JSON);

            for(int i=0;i<result_count;++i)
            {
                JSONObject day_entry= quotes.getJSONObject(i);
                String date_entry = day_entry.getString(DATE_JSON);
                float price= Float.parseFloat(day_entry.getString(CLOSE_JSON));
                mlineSet.addPoint(date_entry,price);
            }
        }


        @Override
        protected Void doInBackground(String... params) {


            if (params==null || params.length == 0 || params[0].equals("")) {
                return null;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            detailsJsonString = null;
            //moviescount=0;
            try {
                urlStringBuilder= urlStringBuilder + ("https://query.yahooapis.com/v1/public/yql?q=");
                urlStringBuilder= urlStringBuilder + ("Select%20*%20from%20yahoo.finance.historicaldata%20where%20symbol%20%3D%27");
                urlStringBuilder= urlStringBuilder + (URLEncoder.encode(mSymbol, "UTF-8"));
                urlStringBuilder= urlStringBuilder + ("%27%20and%20startDate%20%3D%20%27");
                urlStringBuilder= urlStringBuilder + (URLEncoder.encode(mDateStart, "UTF-8"));
                urlStringBuilder= urlStringBuilder + ("%27%20and%20endDate%20%3D%20%27");
                urlStringBuilder= urlStringBuilder + (URLEncoder.encode(mDateEnd, "UTF-8"));
                urlStringBuilder= urlStringBuilder + ("%27&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                        + "org%2Falltableswithkeys");
                urlStringBuilder= urlStringBuilder.replace("+","%20");
                URL url= new URL(urlStringBuilder);
                Log.v("built uri", urlStringBuilder);

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                detailsJsonString = buffer.toString();
            } catch (IOException e) {
                Log.e("LineGraph", "Error ", e);
                // If the code didn't successfully get the movies data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("LineGraph", "Error closing stream", e);
                    }
                }
            }
            Log.v("message",detailsJsonString);
            try {
                getDatafromJson(detailsJsonString);
            } catch (JSONException e) {
                Log.e("LineGraph", e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }
        protected void onPreExecute() {
            super.onPreExecute();
            pd=new ProgressDialog(LineGraph.this);
            pd.setMessage("Loading...");
            pd.show();

            Calendar start_date,end_date;
            start_date= Calendar.getInstance();
            end_date= Calendar.getInstance();
            start_date.add(Calendar.DATE,num_of_days);
            SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd");

            mDateStart= s.format(new Date(start_date.getTimeInMillis()));
            mDateEnd= s.format(new Date(end_date.getTimeInMillis()));

            Log.v("Date", mDateEnd+mDateStart);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            chartView.addData(mlineSet);
            chartView.show();
            if(pd!=null)
            {
                pd.dismiss();
            }
        }
    }

}
