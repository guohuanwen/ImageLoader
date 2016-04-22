package com.mylibrary;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by bigwen on 2016/4/20.
 */
public class BwThreadPool {

    private static volatile BwThreadPool bwThreadPool;
    private ThreadPoolExecutor mThreadPoolExecutor;
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int TIME = 1;
    private LinkedBlockingQueue<Runnable> mLinkedBlockingQueue;

    public static BwThreadPool getInstance() {
        if (bwThreadPool == null) {
            synchronized (BwThreadPool.class) {
                if (bwThreadPool == null) {
                    bwThreadPool = new BwThreadPool();
                }
            }
        }
        return bwThreadPool;
    }

    private BwThreadPool() {
        mLinkedBlockingQueue = new LinkedBlockingQueue<>();
        mThreadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, TIME, TimeUnit.DAYS, mLinkedBlockingQueue);
    }

    public void add(Runnable r){
        mThreadPoolExecutor.execute(r);
    }

}



