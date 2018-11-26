package com.test;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
public class CrawlChange{
    public static LinkedList<String> getAbsHref(){
        LinkedList<String> absHrefArr = new LinkedList();
        String basicPage = "https://beijing.zbj.com/wzkf/";
        String url;
        for (int i = 0; i < 100; i++){
                url = basicPage + "ek" + String.valueOf(i*60) + ".html";
                absHrefArr.offer(url);
        }
        return absHrefArr;
    }
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
                            System.out.println(getId() + "\t" + name.text());
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
            }
        }
    }
    public static CountDownLatch countDownLatch;
    public static void main(String args[]){
        Thread[] pool = new Thread[20];
        countDownLatch = new CountDownLatch(20); // 初始化容量，
        LinkedList<String> absHref = getAbsHref();
        LinkedList<String> queue = new LinkedList<String>();
        ThreadGroup threadGroup = new ThreadGroup("test-group");
        for (int i = 0; i < pool.length; i++){
            pool[i] = new MyThread(queue,absHref,threadGroup);
            pool[i] = new Thread(threadGroup, pool[i]);
            pool[i].start();
        }
        try {
            countDownLatch.await();   //等待子线程全部执行结束（等待CountDownLatch计数变为0）
            System.out.println("ok");
        } catch (InterruptedException e){
                e.printStackTrace();
                System.exit(0);
        }
    }
}
