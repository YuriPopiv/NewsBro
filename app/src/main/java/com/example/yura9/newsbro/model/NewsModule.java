package com.example.yura9.newsbro.model;

import io.realm.annotations.RealmModule;

/**
 * Created by yura9 on 3/15/2017.
 */


@RealmModule(library = true, classes = {NewsArray.class, NewsArticle.class})
public class NewsModule {
}
