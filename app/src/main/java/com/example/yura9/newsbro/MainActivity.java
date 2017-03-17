package com.example.yura9.newsbro;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Context;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.yura9.newsbro.model.NewsArticle;
import com.example.yura9.newsbro.model.NewsModule;
import com.facebook.stetho.Stetho;
import com.uphyca.stetho_realm.RealmInspectorModulesProvider;

import co.moonmonkeylabs.realmrecyclerview.RealmRecyclerView;
import io.realm.Realm;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.RealmViewHolder;
import io.realm.Sort;

public class MainActivity extends AppCompatActivity {

    private final static String SUB = "subscription";
    private boolean subscription;
    private boolean fromNotification;
    private boolean service;

    private Realm realm;
    private RealmRecyclerView realmRecyclerView;
    private NewsAdapter mNewsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        //checking launch activity from notification
        Bundle bundle = getIntent().getExtras();
        if (bundle != null){
            fromNotification = bundle.getBoolean(SUB);
        }
        if (savedInstanceState != null){
            subscription = savedInstanceState.getBoolean(SUB);
        }
        if (subscription && fromNotification){
            updateView();
        }

        //For Database Inspection
        Stetho.initializeWithDefaults(this);
        Stetho.initialize(
                Stetho.newInitializerBuilder(this)
                        .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                        .enableWebKitInspector(RealmInspectorModulesProvider.builder(this).build())
                        .build());

        Realm.init(getApplicationContext());
        realmRecyclerView = (RealmRecyclerView) findViewById(R.id.realm_recycle_view);

    }

    public void updateView(){
        Realm.setDefaultConfiguration(getRealmConfig());
        realm = Realm.getDefaultInstance();
        RealmResults<NewsArticle> results = realm.where(NewsArticle.class).findAllAsync().sort("timeStamp", Sort.DESCENDING);
        mNewsAdapter = new NewsAdapter(MainActivity.this, results, true, true);
        realmRecyclerView.setAdapter(mNewsAdapter);

        /*RealmChangeListener listener = new RealmChangeListener() {
            @Override
            public void onChange(Object element) {

            }
        };
        results.addChangeListener(listener);*/
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SUB, subscription);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem item = menu.findItem(R.id.subscribe);
        updateMenuItems(item);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.subscribe:
                if (subscription && service){//cancel subscription
                    subscription = false;
                    service = false;
                    stopService(new Intent(this, NewsIntentService.class));
                    Toast.makeText(this, "Subscription cancelled", Toast.LENGTH_LONG).show();
                }
                else {
                    subscription = true;
                    service = true;
                    startSubscription();
                    Toast.makeText(this, "Thanks for subscription!", Toast.LENGTH_LONG).show();
                }
                updateMenuItems(item);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void startSubscription(){
        resetRealm();
        updateView();
        Intent intent = new Intent(this, NewsIntentService.class);
        startService(intent);
        service = true;
    }

    public void updateMenuItems(MenuItem item){
        if (subscription){
            item.setTitle(R.string.menu_item_unsubscribe);
            item.setIcon(R.drawable.ic_action_sub_off);
        }else {
            item.setTitle(R.string.menu_item_subscribe);
            item.setIcon(R.drawable.ic_action_sub);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (realm!= null)
            realm.close();
        realm = null;
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private RealmConfiguration getRealmConfig(){
        return new RealmConfiguration.Builder()
                .modules(Realm.getDefaultModule(), new NewsModule())
                .build();

    }

    private void resetRealm(){
        if (realm ==null) Realm.deleteRealm(getRealmConfig());
    }


    public class NewsAdapter extends RealmBasedRecyclerViewAdapter<NewsArticle, NewsAdapter.ViewHolder>{

        public class ViewHolder extends RealmViewHolder{
            public ImageView image;
            public TextView title;
            public TextView author;
            public TextView date;
            public TextView story;

            public ViewHolder(LinearLayout container){
                super(container);
                this.image = (ImageView) container.findViewById(R.id.image);
                this.title = (TextView) container.findViewById(R.id.title);
                this.author = (TextView) container.findViewById(R.id.author);
                this.date = (TextView) container.findViewById(R.id.date);
                this.story = (TextView) container.findViewById(R.id.story);
            }
        }

        public NewsAdapter(Context context, RealmResults<NewsArticle> realmResults, boolean aUpdating, boolean animResult){
            super(context, realmResults, aUpdating, animResult);
        }

        @Override
        public ViewHolder onCreateRealmViewHolder(ViewGroup viewGroup, int i) {
            View v  = inflater.inflate(R.layout.item_view, viewGroup, false);
            ViewHolder vh = new ViewHolder((LinearLayout) v);
            return vh;
        }

        @Override
        public void onBindRealmViewHolder(ViewHolder viewHolder, int i) {
            final NewsArticle newsArticle = realmResults.get(i);
            viewHolder.title.setText(newsArticle.getTitle());
            viewHolder.author.setText(newsArticle.getAuthor());
            viewHolder.date.setText(newsArticle.getPublishedAt());
            viewHolder.story.setText(newsArticle.getDescription());
            if (newsArticle.getUrlToImage() != null) {
                Glide.with(MainActivity.this).load(newsArticle.getUrlToImage()).into(viewHolder.image);
            }
        }
    }
}
