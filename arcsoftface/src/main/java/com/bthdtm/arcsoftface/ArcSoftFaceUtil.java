package com.bthdtm.arcsoftface;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.Log;

import com.arcsoft.facedetection.AFD_FSDKEngine;
import com.arcsoft.facedetection.AFD_FSDKError;
import com.arcsoft.facedetection.AFD_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKMatching;
import com.guo.android_extend.image.ImageConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 徐杨 on 2018/3/19.
 * 邮箱：544066591@qq.com
 */

public class ArcSoftFaceUtil {

    private static Context context;
    private static String appid;
    private static String fd_key;
    private static String fr_key;

    public ArcSoftFaceUtil(Context context, String appid) {
        ArcSoftFaceUtil.context = context;
        ArcSoftFaceUtil.appid = appid;
    }

    private static boolean isOdd(int val) {
        return (val & 0x01) != 0;
    }

    private static byte[] Bitmap2Byte(Bitmap mBitmap) {
        Rect src = new Rect();
        src.set(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        byte[] data = new byte[mBitmap.getWidth() * mBitmap.getHeight() * 3 / 2];
        ImageConverter convert = new ImageConverter();
        convert.initial(mBitmap.getWidth(), mBitmap.getHeight(), ImageConverter.CP_PAF_NV21);
        if (convert.convert(mBitmap, data)) {
            //convert ok
        }
        convert.destroy();
        return data;
    }

    private static Bitmap scaleBitmap(Bitmap origin, int newWidth, int newHeight) {
        if (origin == null) {
            return null;
        }
        int height = origin.getHeight();
        int width = origin.getWidth();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);// 使用后乘
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (!origin.isRecycled()) {
            origin.recycle();
        }
        return newBM;
    }

    public void setFD_KEY(String fd_key) {
        ArcSoftFaceUtil.fd_key = fd_key;
    }

    public void setFR_KEY(String fr_key) {
        ArcSoftFaceUtil.fr_key = fr_key;
    }

    public AFR_FSDKFace getFeature(Bitmap bitmap) {
        if (TextUtils.isEmpty(appid) || TextUtils.isEmpty(fr_key)) return null;
        if (isOdd(bitmap.getWidth())) {
            bitmap = scaleBitmap(bitmap, bitmap.getWidth() + 1, bitmap.getHeight());
        }
        if (isOdd(bitmap.getHeight())) {
            bitmap = scaleBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight() + 1);
        }
        AFR_FSDKEngine engine = new AFR_FSDKEngine();//用来存放提取到的人脸信息, face_1 是注册的人脸，face_2 是要识别的人脸
        AFR_FSDKFace face = new AFR_FSDKFace();
        AFR_FSDKError error = engine.AFR_FSDK_InitialEngine(appid, fr_key);
        Log.d("com.arcsoft", "AFR_FSDK_InitialEngine = " + error.getCode());//输入的 data 数据为 NV21 格式(如 Camera 里 NV21 格式的 preview 数据);人脸坐标一般使用人 脸检测返回的 Rect 传入;人脸角度请按照人脸检测引擎返回的值传入。
        byte[] faceData1 = Bitmap2Byte(bitmap);
        if (getFace(faceData1, bitmap) == null) {
            return null;
        } else {
            error = engine.AFR_FSDK_ExtractFRFeature(faceData1, bitmap.getWidth(), bitmap.getHeight(), AFR_FSDKEngine.CP_PAF_NV21, getFace(faceData1, bitmap).getRect(), AFR_FSDKEngine.AFR_FOC_0, face);
            Log.d("com.arcsoft", "Face=" + face.getFeatureData()[0] + "," + face.getFeatureData()[1] + "," + face.getFeatureData()[2] + "," + error.getCode());
            return face;
        }
    }

    public float compare(AFR_FSDKFace face1, AFR_FSDKFace face2) {
        if (TextUtils.isEmpty(appid) || TextUtils.isEmpty(fr_key)) return 0;
        AFR_FSDKEngine engine = new AFR_FSDKEngine();//用来存放提取到的人脸信息, face_1 是注册的人脸，face_2 是要识别的人脸
        AFR_FSDKError error = engine.AFR_FSDK_InitialEngine(appid, fr_key);
        Log.d("com.arcsoft", "AFR_FSDK_InitialEngine = " + error.getCode());//输入的 data 数据为 NV21 格式(如 Camera 里 NV21 格式的 preview 数据);人脸坐标一般使用人 脸检测返回的 Rect 传入;人脸角度请按照人脸检测引擎返回的值传入。
        AFR_FSDKMatching score = new AFR_FSDKMatching();//score 用于存放人脸对比的相似度值
        error = engine.AFR_FSDK_FacePairMatching(face1, face2, score);
        Log.d("com.arcsoft", "AFR_FSDK_FacePairMatching=" + error.getCode());
        Log.d("com.arcsoft", "Score:" + score.getScore());
        error = engine.AFR_FSDK_UninitialEngine();//销毁人脸识别引擎
        Log.d("com.arcsoft", "AFR_FSDK_UninitialEngine : " + error.getCode());
        return score.getScore();
    }

    public float compare(Bitmap aBitmap, Bitmap bBitmap) {
        if (TextUtils.isEmpty(appid) || TextUtils.isEmpty(fr_key)) return 0;
        if (isOdd(aBitmap.getWidth())) {
            aBitmap = scaleBitmap(aBitmap, aBitmap.getWidth() + 1, aBitmap.getHeight());
        }
        if (isOdd(aBitmap.getHeight())) {
            aBitmap = scaleBitmap(aBitmap, aBitmap.getWidth(), aBitmap.getHeight() + 1);
        }
        if (isOdd(bBitmap.getWidth())) {
            bBitmap = scaleBitmap(bBitmap, bBitmap.getWidth() + 1, bBitmap.getHeight());
        }
        if (isOdd(bBitmap.getHeight())) {
            bBitmap = scaleBitmap(bBitmap, bBitmap.getWidth(), bBitmap.getHeight() + 1);
        }

        AFR_FSDKEngine engine = new AFR_FSDKEngine();//用来存放提取到的人脸信息, face_1 是注册的人脸，face_2 是要识别的人脸
        AFR_FSDKFace face1 = new AFR_FSDKFace();
        AFR_FSDKFace face2 = new AFR_FSDKFace();
        AFR_FSDKError error = engine.AFR_FSDK_InitialEngine(appid, fr_key);
        Log.d("com.arcsoft", "AFR_FSDK_InitialEngine = " + error.getCode());//输入的 data 数据为 NV21 格式(如 Camera 里 NV21 格式的 preview 数据);人脸坐标一般使用人 脸检测返回的 Rect 传入;人脸角度请按照人脸检测引擎返回的值传入。

        byte[] faceData1 = Bitmap2Byte(aBitmap);
        byte[] faceData2 = Bitmap2Byte(bBitmap);

        if (getFace(faceData1, aBitmap) == null) {
            return 0;
        } else if (getFace(faceData2, bBitmap) == null) {
            return 0;
        } else {
            error = engine.AFR_FSDK_ExtractFRFeature(faceData1, aBitmap.getWidth(), aBitmap.getHeight(), AFR_FSDKEngine.CP_PAF_NV21, getFace(faceData1, aBitmap).getRect(), AFR_FSDKEngine.AFR_FOC_0, face1);
            Log.d("com.arcsoft", "Face=" + face1.getFeatureData()[0] + "," + face1.getFeatureData()[1] + "," + face1.getFeatureData()[2] + "," + error.getCode());
            error = engine.AFR_FSDK_ExtractFRFeature(faceData2, bBitmap.getWidth(), bBitmap.getHeight(), AFR_FSDKEngine.CP_PAF_NV21, getFace(faceData2, bBitmap).getRect(), AFR_FSDKEngine.AFR_FOC_0, face2);
            Log.d("com.arcsoft", "Face=" + face2.getFeatureData()[0] + "," + face2.getFeatureData()[1] + "," + face2.getFeatureData()[2] + "," + error.getCode());
            AFR_FSDKMatching score = new AFR_FSDKMatching();//score 用于存放人脸对比的相似度值
            error = engine.AFR_FSDK_FacePairMatching(face1, face2, score);
            Log.d("com.arcsoft", "AFR_FSDK_FacePairMatching=" + error.getCode());
            Log.d("com.arcsoft", "Score:" + score.getScore());
            error = engine.AFR_FSDK_UninitialEngine();//销毁人脸识别引擎
            Log.d("com.arcsoft", "AFR_FSDK_UninitialEngine : " + error.getCode());
            return score.getScore();
        }
    }

    public List<AFD_FSDKFace> getFaceList(Bitmap bitmap) {
        if (TextUtils.isEmpty(appid) || TextUtils.isEmpty(fd_key)) return null;
        AFD_FSDKEngine engine = new AFD_FSDKEngine();// 用来存放检测到的人脸信息列表
        List<AFD_FSDKFace> result = new ArrayList<>();//初始化人脸检测引擎，使用时请替换申请的 APPID 和 SDKKEY
        AFD_FSDKError err = engine.AFD_FSDK_InitialFaceEngine(appid, fd_key, AFD_FSDKEngine.AFD_OPF_0_HIGHER_EXT, 16, 5);
        Log.d("com.arcsoft", "AFD_FSDK_InitialFaceEngine = " + err.getCode());//输入的 data 数据为 NV21 格式(如 Camera 里 NV21 格式的 preview 数据)，其中 height 不能为奇数，人脸检测返回结果保存在 result。
        err = engine.AFD_FSDK_StillImageFaceDetection(Bitmap2Byte(bitmap), bitmap.getWidth(), bitmap.getHeight(), AFD_FSDKEngine.CP_PAF_NV21, result);
        Log.d("com.arcsoft", "AFD_FSDK_StillImageFaceDetection =" + err.getCode());
        Log.d("com.arcsoft", "Face=" + result.size());
        err = engine.AFD_FSDK_UninitialFaceEngine();//销毁人脸检测引擎
        Log.d("com.arcsoft", "AFD_FSDK_UninitialFaceEngine =" + err.getCode());
        return result;
    }

    private AFD_FSDKFace getFace(byte[] data, Bitmap bitmap) {
        if (TextUtils.isEmpty(appid) || TextUtils.isEmpty(fd_key)) return null;
        AFD_FSDKEngine engine = new AFD_FSDKEngine();// 用来存放检测到的人脸信息列表
        List<AFD_FSDKFace> result = new ArrayList<AFD_FSDKFace>();//初始化人脸检测引擎，使用时请替换申请的 APPID 和 SDKKEY
        AFD_FSDKError err = engine.AFD_FSDK_InitialFaceEngine(appid, fd_key, AFD_FSDKEngine.AFD_OPF_0_HIGHER_EXT, 16, 5);
        Log.d("com.arcsoft", "AFD_FSDK_InitialFaceEngine = " + err.getCode());//输入的 data 数据为 NV21 格式(如 Camera 里 NV21 格式的 preview 数据)，其中 height 不能为奇数，人脸检测返回结果保存在 result。
        err = engine.AFD_FSDK_StillImageFaceDetection(data, bitmap.getWidth(), bitmap.getHeight(), AFD_FSDKEngine.CP_PAF_NV21, result);
        Log.d("com.arcsoft", "AFD_FSDK_StillImageFaceDetection =" + err.getCode());
        Log.d("com.arcsoft", "Face=" + result.size());
        AFD_FSDKFace mFace = null;
        for (AFD_FSDKFace face : result) {
            Log.d("com.arcsoft", "Face:" + face.toString());
            mFace = face;
        }
        err = engine.AFD_FSDK_UninitialFaceEngine();//销毁人脸检测引擎
        Log.d("com.arcsoft", "AFD_FSDK_UninitialFaceEngine =" + err.getCode());
        return mFace;
    }
}
