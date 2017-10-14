package com.example.doityourself_.sqlitedatabase;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ArrayList<String> title = new ArrayList<>();
    ArrayList<String> url = new ArrayList<>();
    ArrayAdapter arrayAdapter;
    SQLiteDatabase articleDatabase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView)findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,title);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(),ArticleActivity.class);
                intent.putExtra("url",url.get(i));
                startActivity(intent);
            }
        });
        articleDatabase = this.openOrCreateDatabase("articles",MODE_PRIVATE,null);
        articleDatabase.execSQL("create table if not exists articles(id integer primary key, articleId integer, title varchar,url varchar)");
        updateListView();
        DownloadTask downloadTask = new DownloadTask();
        downloadTask.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");

    }

    public void updateListView()
    {
        Cursor cursor = articleDatabase.rawQuery("select*from articles",null);
        int titleIndex = cursor.getColumnIndex("title");
        int urlIndex = cursor.getColumnIndex("url");

        if(cursor.moveToFirst())
        {
            title.clear();
            url.clear();
            do {
                title.add(cursor.getString(titleIndex));

                url.add(cursor.getString(urlIndex));
            }while (cursor.moveToNext());
        }
        arrayAdapter.notifyDataSetChanged();
    }

    public class DownloadTask extends AsyncTask<String,Void,String>
    {

        @Override
        protected String doInBackground(String... strings) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;
            try {
                url = new URL(strings[0]);
                urlConnection = (HttpURLConnection)url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(inputStream);
                int data = reader.read();
                while (data!=-1)
                {
                    char current = (char) data;
                    result+=current;
                    data = reader.read();

                }

                JSONArray jsonArray = new JSONArray(result);
                int numbersOfItem = 20;

                if(jsonArray.length()<20)
                {
                    numbersOfItem=jsonArray.length();
                }
                articleDatabase.execSQL("delete from articles");
                for(int a=0;a<numbersOfItem;a++)
                {
                    String articleId = jsonArray.getString(a);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/"+articleId+".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    inputStream=urlConnection.getInputStream();
                    reader = new InputStreamReader(inputStream);
                    data = reader.read();
                    String articleInfo = "";
                    while (data!=-1)
                    {
                        char current = (char) data;
                        articleInfo+=current;
                        data=reader.read();
                    }


                    JSONObject jsonObject = new JSONObject(articleInfo);
                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                        //Log.i("info",jsonObject.toString());

                        String articleTitle = jsonObject.getString("title");

                        String articleUrl = jsonObject.getString("url");
                        Log.i("info",articleUrl);
                        String sql = "insert into articles(articleid, title, url)values(?,?,?)";
                        SQLiteStatement sqLiteStatement = articleDatabase.compileStatement(sql);
                        sqLiteStatement.bindString(1,articleId);
                        sqLiteStatement.bindString(2,articleTitle);
                        sqLiteStatement.bindString(3,articleUrl);

                        sqLiteStatement.execute();

                    }


                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }
}

