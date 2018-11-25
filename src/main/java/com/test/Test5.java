package com.test;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
public class Test5 {
    public static Queue<String> content = new LinkedList<String>();
    public static int  index = 0;
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
    public static List<String>  getAbsHref(){
        List<String>  hrefArr = new ArrayList<String>();
        try {
            hrefArr.add("https://beijing.zbj.com/wzkf/e.html");
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

    public static class MyThread extends Thread{
        List<String>   pageIndices;
        public MyThread( List<String>  pageIndices){
            this.pageIndices = pageIndices;
        }
        @Override
        public void run() {
            Document document;
            Elements brandName;
            try {
                for(int hrefNumber = 0;hrefNumber<pageIndices.size();hrefNumber++) {
                    document = Jsoup.connect(pageIndices.get(hrefNumber)).get();
                    brandName = document.select("span.shop-info-base-name.text-overflow");
                    for (Element name : brandName) {
                        System.out.println(getId() + " " + name.text() + "  "+ "pool中个数"+Thread.activeCount());
                        content.offer(name.text());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }catch(Exception e){
                // 处理 Class.forName 错误
                e.printStackTrace();
            }
        }
    }

    public static class dbThread extends Thread {

        public static Queue<String> content;
        public static ThreadGroup threadGroup;
        public dbThread(Queue<String> content,ThreadGroup threadGroup) {
            this.content = content;
            this.threadGroup = threadGroup;
        }

        @Override
        public void run() {
            Connection con = getConn();
            while ((threadGroup.activeCount()!= 0) ||
                    (threadGroup.activeCount()== 0 && content.peek() != null)){
                try {
                    String insertSql = "insert into brandName (id,name) values (?,?)";
                    PreparedStatement ps = con.prepareStatement(insertSql);
                    if (content.peek() == null) {
                        currentThread().sleep(10);
                    } else {
                        PreparedStatement lock = con.prepareStatement("lock tables brandName write");
                        PreparedStatement unlock = con.prepareStatement("unlock tables");
                        lock.executeQuery();
                        ps.setString(2, content.poll());
                        ps.setString(1, String.valueOf(index));
                        ps.executeUpdate();
                        index++;
                        unlock.executeQuery();
                        System.out.println(getId() + "          "+ "pool中个数"+threadGroup.activeCount());
                    }
                } catch (SQLException se) {
                    // 处理 JDBC 错误
                    se.printStackTrace();
                } catch (Exception e) {
                    // 处理 Class.forName 错误
                    e.printStackTrace();
                }
            }
        }
    }
    public static void main(String[] args) {
        List<String>  pageIndices = getAbsHref();
        Thread[] pool = new Thread[4];
        ThreadGroup threadGroup = new ThreadGroup("test-group");
//        for(int i = 0;i<pageIndices.size();i++) {
//                System.out.println(pageIndices.get(i));
//        }
        for(int i=0;i<pool.length;i++){
            pool[i] = new MyThread(pageIndices.subList(i*25, i*25+25));
            pool[i] = new Thread(threadGroup, pool[i]);
            pool[i].start();
        }
        Thread[] db = new Thread[4];
        for(int j=0;j<db.length;j++){
            db[j] = new dbThread(content,threadGroup);
            db[j].start();
        }
    }
}
