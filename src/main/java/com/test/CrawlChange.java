package com.test;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.LinkedList;
public class CrawlChange {
    public static LinkedList<String> getAbsHref(){
        LinkedList<String> absHrefArr = new LinkedList();
        String basicPage = "https://beijing.zbj.com/wzkf/";
        String url;
        for (int i = 0; i < 100; i++){
                if (i == 0) {
                    url = basicPage + "e.html";
                }
                else {
                    url = basicPage + "ek" + String.valueOf(i*60) + ".html";
                }
                absHrefArr.offer(url);
        }
        return absHrefArr;
    }
    public static class MyThread extends Thread {
        LinkedList<String>  subAbsHrefQueue;
        LinkedList<String>  absHref;
        ThreadGroup threadGroup;
        public MyThread(LinkedList  subAbsHrefQueue, LinkedList  absHref,ThreadGroup threadGroup) {
            this.subAbsHrefQueue = subAbsHrefQueue;
            this.absHref = absHref;
            this.threadGroup = threadGroup;
        }
        @Override
        public void run() {
            Document document;
            Elements brandName;
            try {
                while (absHref.peek() != null){
                    if (subAbsHrefQueue.peek() == null)
                    {
                        subAbsHrefQueue.offer(absHref.poll());
                    }
                    else
                    {
                        document = Jsoup.connect(subAbsHrefQueue.poll()).get();
                        brandName = document.select("span.shop-info-base-name.text-overflow");
                        for (Element name : brandName) {
                            System.out.println(getId() + " " + name.text() + "  "+ "pool中个数"+threadGroup.activeCount());
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static void main(String args[]) {
        Thread[] pool = new Thread[20];
        LinkedList<String> absHref = getAbsHref();
        LinkedList<String> queue = new LinkedList<String>();
        ThreadGroup threadGroup = new ThreadGroup("test-group");
        for (int i = 0; i < pool.length; i++){
            pool[i] = new MyThread(queue,absHref,threadGroup);
            pool[i] = new Thread(threadGroup, pool[i]);
            pool[i].start();
        }
    }
}
