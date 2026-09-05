package com.smxy.myapplication.network;

import com.smxy.myapplication.model.Record;
import com.smxy.myapplication.model.Schedule;

import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;
import java.util.Map;

public interface ApiService {
    // ========== 用户相关 ==========
    // 注册
    @POST("api/register")
    Call<ApiResponse> register(@Body Map<String, String> params);

    // 登录
    @POST("api/login")
    Call<LoginResponse> login(@Body Map<String, String> params);

    // 检查用户名是否存在
    @GET("api/check-username")
    Call<CheckUsernameResponse> checkUsername(@Query("username") String username);

    // 获取用户信息
    @GET("api/user/info")
    Call<UserInfoResponse> getUserInfo(@Header("Authorization") String token);

    // 重置密码
    @POST("reset-password")
    Call<ApiResponse> resetPassword(@Body Map<String, String> params);

    // ========== 记账记录 API ==========

    // 获取所有记账记录
    @GET("api/records")
    Call<RecordsResponse> getRecords(@Header("Authorization") String authorization);

    // 获取指定账本的记账记录
    @GET("api/records")
    Call<RecordsResponse> getRecords(@Header("Authorization") String authorization, @Query("ledger_id") int ledgerId);

    // 添加记账记录
    @POST("api/records")
    Call<ApiResponse> addRecord(@Header("Authorization") String authorization, @Body Map<String, Object> record);

    // 获取收支统计
    @GET("api/statistics")
    Call<StatisticsResponse> getStatistics(
            @Header("Authorization") String authHeader,
            @Query("start_date") String startDate,
            @Query("end_date") String endDate
    );

    // 获取指定账本的收支统计
    @GET("api/statistics")
    Call<StatisticsResponse> getStatistics(
            @Header("Authorization") String authHeader,
            @Query("start_date") String startDate,
            @Query("end_date") String endDate,
            @Query("ledger_id") int ledgerId
    );

    // 获取月度统计
    @GET("api/statistics/monthly")
    Call<MonthlyStatisticsResponse> getMonthlyStatistics(
            @Header("Authorization") String authHeader,
            @Query("year") String year
    );

    // 获取指定账本的月度统计
    @GET("api/statistics/monthly")
    Call<MonthlyStatisticsResponse> getMonthlyStatistics(
            @Header("Authorization") String authHeader,
            @Query("year") String year,
            @Query("ledger_id") int ledgerId
    );

    // 获取分类统计
    @GET("api/statistics/categories")
    Call<CategoryStatisticsResponse> getCategoryStatistics(
            @Header("Authorization") String authHeader,
            @Query("start_date") String startDate,
            @Query("end_date") String endDate,
            @Query("category") String category
    );

    // 获取指定账本的分类统计
    @GET("api/statistics/categories")
    Call<CategoryStatisticsResponse> getCategoryStatistics(
            @Header("Authorization") String authHeader,
            @Query("start_date") String startDate,
            @Query("end_date") String endDate,
            @Query("category") String category,
            @Query("ledger_id") int ledgerId
    );

    // 获取所有分类类型
    @GET("api/statistics/types")
    Call<TypesResponse> getAccountTypes(@Header("Authorization") String authHeader);

    // ========== 账本相关 API ==========

    // 获取用户的所有账本
    @GET("api/ledgers")
    Call<LedgerResponse> getLedgers(@Header("Authorization") String authorization);

    // 创建新账本
    @POST("api/ledgers")
    Call<ApiResponse> createLedger(@Header("Authorization") String authorization, @Body Map<String, Object> ledgerData);

    // 更新账本信息
    @PUT("api/ledgers/{id}")
    Call<ApiResponse> updateLedger(@Header("Authorization") String authorization, @Path("id") int ledgerId, @Body Map<String, Object> ledgerData);

    // 删除账本
    @DELETE("api/ledgers/{id}")
    Call<ApiResponse> deleteLedger(@Header("Authorization") String authorization, @Path("id") int ledgerId);

    // 获取默认账本
    @GET("api/ledgers/default")
    Call<LedgerResponse> getDefaultLedger(@Header("Authorization") String authorization);

    // ========== 菜谱相关 API ==========

    // 获取菜谱列表
    @GET("api/recipes")
    Call<RecipesResponse> getRecipes(@Header("Authorization") String authorization);

    // 获取我的菜谱
    @GET("api/recipes/my-recipes")
    Call<RecipesResponse> getMyRecipes(@Header("Authorization") String authorization);

    // 添加菜谱
    @POST("api/recipes")
    Call<ApiResponse> addRecipe(@Header("Authorization") String authorization, @Body Map<String, Object> recipe);

    // 删除菜谱
    @DELETE("api/recipes/{id}")
    Call<ApiResponse> deleteRecipe(@Header("Authorization") String authorization, @Path("id") int id);

    // 收藏菜谱
    @POST("api/recipes/{id}/favorite")
    Call<ApiResponse> favoriteRecipe(@Header("Authorization") String authorization, @Path("id") int id);

    // 取消收藏
    @DELETE("api/recipes/{id}/favorite")
    Call<ApiResponse> unfavoriteRecipe(@Header("Authorization") String authorization, @Path("id") int id);

    // 获取收藏的菜谱
    @GET("api/recipes/favorites/list")
    Call<FavoritesResponse> getFavoriteRecipes(@Header("Authorization") String authorization);

    // 获取菜系列表
    @GET("api/cuisines")
    Call<CuisineResponse> getCuisines(@Header("Authorization") String token);

    // ========== 日程相关 API ==========

    // 获取日程列表 - 使用 Map 参数
    @GET("api/schedules")
    Call<ScheduleResponse> getSchedules(@Header("Authorization") String authorization,
                                        @QueryMap Map<String, String> params);

    // 添加日程 - 返回 ScheduleResponse
    @POST("api/schedules")
    Call<ScheduleResponse> addSchedule(@Header("Authorization") String authorization,
                                       @Body Map<String, Object> schedule);

    // 更新日程状态 - 返回 ScheduleResponse
    @PUT("api/schedules/{id}/status")
    Call<ScheduleResponse> updateScheduleStatus(@Header("Authorization") String authorization,
                                                @Path("id") int id,
                                                @Body Map<String, Object> status);

    // 删除日程 - 返回 ScheduleResponse
    @DELETE("api/schedules/{id}")
    Call<ScheduleResponse> deleteSchedule(@Header("Authorization") String authorization,
                                          @Path("id") int id);

    // ========== 便签相关 API ==========

    @GET("api/notes")
    Call<NoteResponse> getNotes(@Header("Authorization") String authToken);

    @POST("api/notes")
    Call<NoteResponse> addNote(@Header("Authorization") String authToken,
                               @Body Map<String, String> body);

    @PUT("api/notes/{id}")
    Call<NoteResponse> updateNote(@Header("Authorization") String authToken,
                                  @Path("id") int id,
                                  @Body Map<String, String> body);

    @DELETE("api/notes/{id}")
    Call<NoteResponse> deleteNote(@Header("Authorization") String authToken,
                                  @Path("id") int id);
}