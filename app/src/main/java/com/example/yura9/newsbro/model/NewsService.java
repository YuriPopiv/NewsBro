package com.example.yura9.newsbro.model;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by yura9 on 3/14/2017.
 */

public interface NewsService {
    @GET("/v1/articles?")
    Call<NewsArray> getArticles(
            @Query("source") String source, @Query("sortBy") String sortBy, @Query("apiKey")String apiKey);
}
