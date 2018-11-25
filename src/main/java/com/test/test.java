package com.test;

import java.io.IOException;

import javafx.application.Application;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.Properties;

import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;



public class test {
        public static Connection getConn() {
            Parameters params = new Parameters();
            FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                    new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                            .configure(params.properties()
                                    .setFileName("user.properties"));
            String driver = "com.mysql.jdbc.Driver";
             Connection conn = null;
        try {
                Class.forName(driver); //classLoader,加载对应驱动
                Configuration config = builder.getConfiguration();
                String url = config.getString("url");
                String username = config.getString("name");
                String password = config.getString("password");
                conn = (Connection) DriverManager.getConnection(url, username, password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        catch(ConfigurationException cex)
        {
            cex.printStackTrace();
        }
        return conn;
    }

    public static ArrayList<String> getAbsHref(){
        ArrayList<String> hrefArr = new   ArrayList<String>();
        try {
            hrefArr.add( "https://beijing.zbj.com/wzkf/e.html");
            Document document = Jsoup.connect("https://beijing.zbj.com/wzkf/e.html").get();
            Elements brandName = document.select("a.pagination-next"); //只有一个下一页
            String absHref;
            for(int i = 1;i<100;i++){

                absHref = brandName.attr("abs:href");
                hrefArr.add(absHref);
                document = Jsoup.connect(absHref).get();
                brandName = document.select("a.pagination-next");
//                System.out.println(absHref);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return hrefArr;
    }
    static String creatSql = "CREATE TABLE brandName " +
                             "(id INTEGER not NULL, " +
                                " name VARCHAR(255), " +
                                " PRIMARY KEY ( id ))DEFAULT CHARSET=utf8";
    public static void main(String[] args) {
        Connection con=getConn();
        ArrayList<String> absHref = getAbsHref();
        try {
            if(!con.isClosed())
                System.out.println("Succeeded connecting to the Database!");
            //2.创建statement类对象，用来执行SQL语句！！
            Statement statement = con.createStatement();
            statement.executeUpdate(creatSql);
            String insertSql="insert into brandName (id,name) values (?,?)";
            PreparedStatement ps=con.prepareStatement(insertSql);
            Document document;
            Elements brandName;
            int i = 0;
            for(int hrefNumber = 0;hrefNumber<100;hrefNumber++) {
                document = Jsoup.connect(absHref.get(hrefNumber)).get();
                brandName = document.select("span.shop-info-base-name.text-overflow");
                for (Element name : brandName) {
                    System.out.println(name.text());
                    ps.setString(2,name.text());
                    ps.setString(1, String.valueOf(i));
                    ps.executeUpdate();
                    i = i+1;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }catch(SQLException se){
            // 处理 JDBC 错误
            se.printStackTrace();
        }catch(Exception e){
            // 处理 Class.forName 错误
            e.printStackTrace();
        }
    }
}


