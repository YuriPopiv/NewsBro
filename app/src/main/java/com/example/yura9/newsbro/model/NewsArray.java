package com.example.yura9.newsbro.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

/**
 * Created by yura9 on 3/14/2017.
 */


@JsonIgnoreProperties(ignoreUnknown = true)
public class NewsArray extends RealmObject{

    @JsonProperty("source")
    public String source;

    @JsonProperty("articles")
    public RealmList<NewsArticle> articles;

}
