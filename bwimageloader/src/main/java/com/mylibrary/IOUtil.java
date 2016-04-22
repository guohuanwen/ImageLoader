package com.mylibrary;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by bigwen on 2016/4/20.
 */
public class IOUtil {

    public static void close(BufferedInputStream bufferedInputStream){
        if (bufferedInputStream == null){
            return;
        }
        try {
            bufferedInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void close(OutputStream bufferedInputStream){
        if (bufferedInputStream == null){
            return;
        }
        try {
            bufferedInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void close(InputStream bufferedInputStream){
        if (bufferedInputStream == null){
            return;
        }
        try {
            bufferedInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void close(BufferedOutputStream bufferedInputStream){
        if (bufferedInputStream == null){
            return;
        }
        try {
            bufferedInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void close(FileInputStream bufferedInputStream){
        if (bufferedInputStream == null){
            return;
        }
        try {
            bufferedInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
