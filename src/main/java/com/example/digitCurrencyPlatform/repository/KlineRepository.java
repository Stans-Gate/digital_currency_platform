package com.example.digitCurrencyPlatform.repository;

import com.example.digitCurrencyPlatform.model.Kline;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KlineRepository {
    // find all kline data stored in the db
    @Select("SELECT * FROM kline_data")
    @Results({
            @Result(property = "openTime", column = "open_time"),
            @Result(property = "closeTime", column = "close_time"),
            @Result(property = "openPrice", column = "open_price"),
            @Result(property = "closePrice", column = "close_price"),
            @Result(property = "highPrice", column = "high_price"),
            @Result(property = "lowPrice", column = "low_price"),
            @Result(property = "volume", column = "volume"),
            @Result(property = "numberOfTrades", column = "number_of_trades")

    })
    List<Kline> findAll();

    // Use time range to find
    @Select("SELECT * FROM kline_data " +
            "WHERE symbol = #{symbol} AND open_time >= #{openTime} AND close_time <= #{closeTime} LIMIT #{limit}")
    @Results({
            @Result(property = "openTime", column = "open_time"),
            @Result(property = "closeTime", column = "close_time"),
            @Result(property = "openPrice", column = "open_price"),
            @Result(property = "closePrice", column = "close_price"),
            @Result(property = "highPrice", column = "high_price"),
            @Result(property = "lowPrice", column = "low_price"),
            @Result(property = "volume", column = "volume"),
            @Result(property = "numberOfTrades", column = "number_of_trades")
    })
    List<Kline> retrieveKlineDataWithStartAndEndTime(@Param("symbol") String symbol, @Param("openTime") long openTime,
                                                     @Param("closeTime") long closeTime, @Param("limit") int limit);


    // delete a row of kline data using the symbol and openTime
    @Delete("DELETE FROM kline_data WHERE symbol = #{symbol} AND open_time = #{openTime}")
    public void deleteBySymbolAndOpenTime(@Param("symbol") String symbol, @Param("openTime") long openTime);

    // insert a row of kline data
    @Insert("INSERT INTO kline_data(symbol, open_time, close_time, " +
            "open_price, close_price, high_price, low_price, volume, number_of_trades)" +
            "VALUES (#{symbol}, #{openTime}, #{closeTime}, " +
            "#{openPrice}, #{closePrice}, #{highPrice}, #{lowPrice}, #{volume}, " +
            "#{numberOfTrades})")
    public int insert(Kline kline);

    // batch insert
    @Insert({
            "<script>",
            "INSERT INTO kline_data (symbol, open_time, close_time, open_price, close_price, high_price, " +
                    "low_price, volume, number_of_trades) VALUES",
            "<foreach collection='klines' item='kline' separator=','>",
            "(#{kline.symbol}, #{kline.openTime}, #{kline.closeTime}, #{kline.openPrice}, #{kline.closePrice}, " +
                    "#{kline.highPrice}, #{kline.lowPrice}, #{kline.volume}, #{kline.numberOfTrades})",
            "</foreach>",
            "</script>"
    })
    void batchInsert(@Param("klines") List<Kline> klines);

    // retrieve

}
