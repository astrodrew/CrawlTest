package com.test;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

public class CrawlChange {
    public static ArrayBlockingQueue<String> getAbsHref(){
        ArrayBlockingQueue<String> absHrefArr = new ArrayBlockingQueue(300);
        try {
            absHrefArr.offer("https://beijing.zbj.com/wzkf/e.html");
            Document document = Jsoup.connect("https://beijing.zbj.com/wzkf/e.html").get();
            Elements brandName = document.select("a.pagination-next"); //只有一个下一页
            String absHref;
            for(int i = 1; i < 100; i++){
                absHref = brandName.attr("abs:href");
                absHrefArr.offer(absHref);
                document = Jsoup.connect(absHref).get();
                brandName = document.select("a.pagination-next");
                //System.out.println(absHref);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return absHrefArr;
    }

    public static class MyThread extends Thread {
        ArrayBlockingQueue<String>  subAbsHrefQueue;
        ArrayBlockingQueue<String>  absHref;
        public MyThread(ArrayBlockingQueue  subAbsHrefQueue, ArrayBlockingQueue  absHref) {
            this.subAbsHrefQueue = subAbsHrefQueue;
            this.absHref = absHref;
        }
        @Override
        public void run() {
            Document document;
            Elements brandName;
            try{
                while(absHref.peek() != null){
                    if (subAbsHrefQueue.peek() == null)
                    {
                        subAbsHrefQueue.offer(absHref.poll());
                    }
                    else
                    {
                        document = Jsoup.connect(subAbsHrefQueue.poll()).get();
                        brandName = document.select("span.shop-info-base-name.text-overflow");
                        for (Element name : brandName) {
                            System.out.println(getId() + " " + name.text() + "  "+ "pool中个数"+Thread.activeCount());
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public static void main(String args[]) {
            ArrayBlockingQueue<String> absHref = getAbsHref();
            Thread[] pool = new Thread[3];
            ArrayBlockingQueue<String> queue1 = new ArrayBlockingQueue<String>(1);
            ArrayBlockingQueue<String> queue2 = new ArrayBlockingQueue<String>(1);
            ArrayBlockingQueue<String> queue3 = new ArrayBlockingQueue<String>(1);
            ThreadGroup threadGroup = new ThreadGroup("test-group");
            pool[0] = new MyThread(queue1,absHref);
            pool[1] = new MyThread(queue2,absHref);
            pool[2] = new MyThread(queue3,absHref);
            for(int i = 0; i < pool.length; i++){
                pool[i] = new Thread(threadGroup, pool[i]);
                pool[i].start();
            }
        }
    }
}
