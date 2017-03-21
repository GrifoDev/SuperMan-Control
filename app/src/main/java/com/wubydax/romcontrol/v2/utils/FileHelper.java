package com.wubydax.romcontrol.v2.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Created by Lars on 13.03.2017.
 */



public class FileHelper {

    private final static String TAG = FileHelper.class.getName();
    Process p;


    public static void copyFileToTemp(String cmd, Process p) {

        try {
            DataOutputStream outs = new DataOutputStream(p.getOutputStream());
            outs.writeBytes(cmd);
            outs.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void copyFileToRoot(String cmd, Process p) {

        try {
            DataOutputStream outs = new DataOutputStream(p.getOutputStream());
            outs.writeBytes("mount -o rw,remount /system\n");
            outs.writeBytes(cmd);
            outs.writeBytes("mount -o ro,remount /system\n");
            outs.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static String readFile(File inputFile) {
        String content;
        try {

            FileInputStream fileInputStream = new FileInputStream(inputFile);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();


            while ((content = bufferedReader.readLine()) != null) {
                stringBuilder.append(content + System.getProperty("line.separator"));
            }

            fileInputStream.close();
            inputStreamReader.close();
            bufferedReader.close();

            content = stringBuilder.toString();


        } catch (FileNotFoundException ex) {
            content = "File not Found - " + ex.getMessage();
            Log.d(TAG, ex.getMessage());
            return content;
        } catch (IOException ex) {
            content = "IO exception - " + ex.getMessage();
            Log.d(TAG, ex.getMessage());
            return content;
        }
        return content;
    }

    public static void investInput(String input, File tempFile) {

        if (input.contains("<CscFeature_SystemUI_ConfigOverrideDataIcon>LTE</CscFeature_SystemUI_ConfigOverrideDataIcon>")) {
            return;
        } else if (input.contains("<CscFeature_SystemUI_ConfigOverrideDataIcon>4G</CscFeature_SystemUI_ConfigOverrideDataIcon>")) {
            return;
        } else {
            try {
                input = input.replaceAll("(?m)^[ \t]*\r?\n", "");
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(input);

                stringBuilder.insert(input.length() - 39, "<CscFeature_SystemUI_ConfigOverrideDataIcon>4G</CscFeature_SystemUI_ConfigOverrideDataIcon>\n");

                FileOutputStream fileOutputStream = new FileOutputStream(tempFile, false);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
                outputStreamWriter.append(stringBuilder.toString());


                outputStreamWriter.close();
                fileOutputStream.flush();
                fileOutputStream.close();

                return;
            } catch (FileNotFoundException ex) {
                Log.d(TAG, ex.getMessage());
                ex.printStackTrace();
            } catch (IOException ex) {
                Log.d(TAG, ex.getMessage());
                ex.printStackTrace();
            }

        }
    }
    public static void saveFile(String input, File tempFile) {
        try {

            //Removing empty lines
            input = input.replaceAll("(?m)^[ \t]*\r?\n", "");


            FileOutputStream fileOutputStream = new FileOutputStream(tempFile, false);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            outputStreamWriter.append(input);


            outputStreamWriter.close();
            fileOutputStream.flush();
            fileOutputStream.close();

            return;
        } catch (FileNotFoundException ex) {
            Log.d(TAG, ex.getMessage());
            ex.printStackTrace();
        } catch (IOException ex) {
            Log.d(TAG, ex.getMessage());
            ex.printStackTrace();
        }
    }
}
