package com.test;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import static com.test.CrawlChange.getAbsHref;
import static com.test.Test5.getConn;

public class Test6 {
    public static LinkedList<String>  absHref = getAbsHref();
    public static LinkedList<String>  content = new LinkedList<String>();
    public static int  index = 0;
    public static CountDownLatch  countDownLatch = new CountDownLatch(10);
    public static class MyThread extends Thread{
        LinkedList<String>  subAbsHrefQueue;
        LinkedList<String>  absHref;
        ThreadGroup threadGroup;
        public MyThread(LinkedList subAbsHrefQueue, LinkedList absHref, ThreadGroup threadGroup){
            this.subAbsHrefQueue = subAbsHrefQueue;
            this.absHref = absHref;
            this.threadGroup = threadGroup;
        }
        @Override
        public void run(){
            Document document;
            Elements brandName;
            try {
                while (! absHref.isEmpty()){
                    String next = subAbsHrefQueue.poll();
                    if (next != null){
                        document = Jsoup.connect(next).get();
                        brandName = document.select("span.shop-info-base-name.text-overflow");
                        for (Element name : brandName){
                            System.out.println(getId() + "\t" + name.text() + CrawlChange.MyThread.activeCount());
                            content.offer(name.text());
                        }
                    }
                    else {
                        subAbsHrefQueue.offer(absHref.poll());
                    }
                }
            } catch (IOException e){
                e.printStackTrace();
            } finally {
                countDownLatch.countDown();
               /* System.out.println(countDownLatch.getCount());*/
            }
        }
    }
    public static class dbThread extends Thread {
        public static LinkedList<String> content;
        public static ThreadGroup threadGroup;
        public dbThread(LinkedList<String> content,ThreadGroup threadGroup) {
            this.content = content;
            this.threadGroup = threadGroup;
        }
        @Override
        public void run() {
            Connection con = getConn();
            while ((countDownLatch.getCount()!= 0) ||
                    (countDownLatch.getCount()== 0 && (! content.isEmpty()))){
                try {
                        String insertSql = "insert into brandName (id,name) values (?,?)";
                        PreparedStatement ps = con.prepareStatement(insertSql);
                        String next = content.poll();
                        if (next != null) {
                            PreparedStatement lock = con.prepareStatement("lock tables brandName write");
                            PreparedStatement unlock = con.prepareStatement("unlock tables");
                            lock.executeQuery();
                            ps.setString(2, next);
                            ps.setString(1, String.valueOf(index));
                            ps.executeUpdate();
                            index++;
                            unlock.executeQuery();
                            System.out.println(getId() + "\t"+ "pool中个数" + countDownLatch.getCount());
                        } else {
                            currentThread().sleep(10);
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
    public static void  main(String args[]){
        Thread[] pool = new Thread[10];
        LinkedList<String> absHref = getAbsHref();
        LinkedList<String> queue = new LinkedList<String>();
        ThreadGroup threadGroup = new ThreadGroup("test-group");
        for (int i = 0; i < pool.length; i++){
            pool[i] = new MyThread(queue,absHref,threadGroup);
            pool[i] = new Thread(threadGroup, pool[i]);
            pool[i].start();
        }
        Thread[] db = new Thread[10];
        for(int j = 0;j < db.length; j++){
            db[j] = new dbThread(content,threadGroup);
            db[j].start();
        }
    }
}
