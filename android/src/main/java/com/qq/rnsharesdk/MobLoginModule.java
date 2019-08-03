package com.qq.rnsharesdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.mob.tools.log.NLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.framework.PlatformDb;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.onekeyshare.OnekeyShare;
import cn.sharesdk.onekeyshare.OnekeyShareTheme;
import cn.sharesdk.tencent.qq.QQ;
import cn.sharesdk.wechat.friends.Wechat;

/**
 * Created by cc on 2017/1/29.
 */

public class MobLoginModule extends ReactContextBaseJavaModule implements PlatformActionListener {
    private Context mContext;
    private Promise mPromise;

    public MobLoginModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.mContext = reactContext.getApplicationContext();
    }

    @Override
    public String getName() {
        return "MobLogin";
    }

    @Override
    public void initialize() {
        super.initialize();
        //初始化Mob
        ShareSDK.initSDK(mContext);

        try{

            Log.i("sharesdk", "===================================================");
            Log.i("sharesdk",  mContext.getExternalFilesDir("").getPath());
            Log.i("sharesdk",  mContext.getExternalFilesDir("").toString());

            String toPath = mContext.getExternalFilesDir("").getPath();

            doCopy(mContext, "pic", toPath);
        }
        catch (Exception ex){
            Log.i("sharesdk", "===================================================");
            Log.i("sharesdk",  ex.getMessage());
        }

    }

    @ReactMethod
    public void loginWithQQ(Promise promise) {
        Platform qq = ShareSDK.getPlatform(QQ.NAME);
        if(qq.isAuthValid()){
            qq.removeAccount(true);
        }
        qq.setPlatformActionListener(this);
        qq.showUser(null);
        mPromise = promise;
    }

    @ReactMethod
    public void loginWithWeChat(Promise promise) {
        Platform wechat = ShareSDK.getPlatform(Wechat.NAME);
        if(wechat.isAuthValid()){
            wechat.removeAccount(true);
        }
        wechat.setPlatformActionListener(this);
        wechat.showUser(null);
        mPromise = promise;
    }

    @ReactMethod
    public void showShare(String title, String text, String url, String imageUrl) {

        Log.i("sharesdk", "===================================================");
        Log.i("sharesdk", title);
        Log.i("sharesdk", text);
        Log.i("sharesdk", url);
        Log.i("sharesdk", imageUrl);
        OnekeyShare oks = new OnekeyShare();
        oks.setSilent(true);
        //ShareSDK快捷分享提供两个界面第一个是九宫格 CLASSIC  第二个是SKYBLUE
        oks.setTheme(OnekeyShareTheme.CLASSIC);
        // 令编辑页面显示为Dialog模式
        oks.setDialogMode();
        // 在自动授权时可以禁用SSO方式
        oks.disableSSOWhenAuthorize();
        // title标题，印象笔记、邮箱、信息、微信、人人网、QQ和QQ空间使用
        oks.setTitle(title);
        // titleUrl是标题的网络链接，仅在Linked-in,QQ和QQ空间使用
        oks.setTitleUrl(url);
        // text是分享文本，所有平台都需要这个字段
        oks.setText(text);
        //分享网络图片，新浪微博分享网络图片需要通过审核后申请高级写入接口，否则请注释掉测试新浪微博
        oks.setImageUrl(imageUrl);

        if(imageUrl.isEmpty()){
            Log.i("sharesdk", "===== imageUrl is empty ==============================================");
            //String logoPath = "file:///android_asset/logo.png";

            String logoPath = mContext.getExternalFilesDir("").getPath()+"/logo.png";
            Log.i("sharesdk", "=============================================="+logoPath);
            oks.setImagePath(logoPath);
        }
        
        // url仅在微信（包括好友和朋友圈）中使用
        oks.setUrl(url);
        // 启动分享GUI
        oks.show(mContext);
    }


    @Override
    public void onComplete(Platform platform, int action, HashMap<String, Object> hashMap) {
        if (action == Platform.ACTION_USER_INFOR) {
            PlatformDb platDB = platform.getDb();
            WritableMap map = Arguments.createMap();
            map.putString("token", platDB.getToken());
            map.putString("user_id", platDB.getUserId());
            map.putString("user_name", platDB.getUserName());
            map.putString("user_gender", platDB.getUserGender());
            map.putString("user_icon", platDB.getUserIcon());
            mPromise.resolve(map);
        }
    }

    @Override
    public void onError(Platform platform, int i, Throwable throwable) {
        mPromise.reject("LoginError", throwable.getMessage());
    }

    @Override
    public void onCancel(Platform platform, int i) {

    }


    public static void doCopy(Context context, String assetsPath, String desPath) throws IOException {
        String[] srcFiles = context.getAssets().list(assetsPath);//for directory
        Log.d("sharesdk", "----------- do copy");
        Log.d("sharesdk", assetsPath);
        Log.i("sharesdk", srcFiles.length+"");
        for (String srcFileName : srcFiles) {
            String outFileName = desPath + File.separator + srcFileName;
            String inFileName = assetsPath + File.separator + srcFileName;
            if (assetsPath.equals("")) {// for first time
                inFileName = srcFileName;
            }
            Log.e("sharesdk","========= assets: "+ assetsPath+"  filename: "+srcFileName +" infile: "+inFileName+" outFile: "+outFileName);
            try {
                InputStream inputStream = context.getAssets().open(inFileName);
                copyAndClose(inputStream, new FileOutputStream(outFileName));
            } catch (IOException e) {//if directory fails exception
                Log.i("sharesdk",e.getMessage());
                e.printStackTrace();
                new File(outFileName).mkdir();
                doCopy(context,inFileName, outFileName);
            }
        }
    }

    private static void closeQuietly(OutputStream out){
        try{
            if(out != null) out.close();;
        }catch (IOException ex){
            ex.printStackTrace();
        }
    }

    private static void closeQuietly(InputStream is){
        try{
            if(is != null){
                is.close();
            }
        }catch (IOException ex){
            ex.printStackTrace();
        }
    }

    private static void copyAndClose(InputStream is, OutputStream out) throws IOException{
        copy(is,out);
        closeQuietly(is);
        closeQuietly(out);
    }

    private static void copy(InputStream is, OutputStream out) throws IOException{
        byte[] buffer = new byte[1024];
        int n = 0;
        while(-1 != (n = is.read(buffer))){
            out.write(buffer,0,n);
        }
    }
}
