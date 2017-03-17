package com.example.yura9.newsbro;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.yura9.newsbro.model.NewsArray;
import com.example.yura9.newsbro.model.NewsArticle;
import com.example.yura9.newsbro.model.NewsModule;
import com.example.yura9.newsbro.model.NewsService;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.Sort;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class NewsIntentService extends IntentService {

    public static String API = "5a1e47972cfb41488b8c3c0f4217f1f8";
    private NewsService mNewsService;
    private SimpleDateFormat iData = new SimpleDateFormat("yyyy-MM-d'T'HH:mm:ss'Z'");
    private SimpleDateFormat oData = new SimpleDateFormat("MM-dd-yyyy");

    public NewsIntentService(){
        super("NewsIntentService");

        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(JacksonConverterFactory.create())
                .baseUrl("https://newsapi.org/")
                .build();
        mNewsService = retrofit.create(NewsService.class);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        while (true){
            synchronized (this){
                try {
                    loadNews(API);
                    wait(60000);//delay for check news every 1 min
                    //for hour data usage about 3MB and 25MB RAM
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }

    public void loadNews(final String apiKey){
        final Call<NewsArray> newsCall =
                mNewsService.getArticles("bbc-news", "top", apiKey);
        newsCall.enqueue(new Callback<NewsArray>() {
            @Override
            public void onResponse(Call<NewsArray> call, Response<NewsArray> response) {
                final NewsArray body = response.body();
                if (body.articles.isEmpty()){
                    return;
                }
                saveNews( body.articles);
            }

            @Override
            public void onFailure(Call<NewsArray> call, Throwable t) {
                //Log.d("NewsBro", "Error loading data");
            }
        });
    }

    public void saveNews(final List<NewsArticle> articles){
        final Realm realm = Realm.getDefaultInstance();

        RealmResults<NewsArticle> results = realm.where(NewsArticle.class).findAll();
        long maxTimeStamp = 0;
        if (!results.isEmpty()){
            maxTimeStamp = results.max("timeStamp").longValue();
        }

        for (NewsArticle a : articles){
            if (a.getPublishedAt() != null) {
                Date pubDate = iData.parse(a.getPublishedAt(), new ParsePosition(0));
                a.setTimeStamp(pubDate.getTime());
                a.setPublishedAt(oData.format(pubDate));
            }
            else {
                a.setTimeStamp(0);
            }
            if (maxTimeStamp!=0 && a.getTimeStamp() > maxTimeStamp)//check latest news
                showNotification(a.getTitle());
        }

        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealmOrUpdate(articles);
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                realm.close();
            }
        }, new Realm.Transaction.OnError() {
            @Override
            public void onError(Throwable error) {
                realm.close();
            }
        });
    }

    public void showNotification(String text){
        NotificationCompat.Builder builder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_action_sub)
                .setContentTitle("NewsBro")
                .setVibrate(new long[]{500, 500, 1000, 500, 1000})
                .setContentText(text);

        Intent resIntent = new Intent(this, MainActivity.class);
        Bundle args = new Bundle();
        args.putBoolean("subscription", true);
        resIntent.putExtras(args);

        resIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);//run the same activity without creating new instance

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resIntent);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, resIntent, 0);
        builder.setContentIntent(pendingIntent);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
