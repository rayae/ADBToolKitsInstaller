package cn.bavelee.donatedialog;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.widget.ImageView;
import android.widget.Toast;

import java.net.URISyntaxException;
import java.net.URLEncoder;

/**
 * Created by Bave on 2018/1/5.
 */

public class DonateToMe {
    public static void show(final Context context) {
        if (context == null) return;
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch (i) {
                    case DialogInterface.BUTTON_NEGATIVE:
                        // wechat
                        showSaveQRCodeDialog(context, R.drawable.wechat_money_revised);
                        break;
                    case DialogInterface.BUTTON_POSITIVE:
                        // alipay
                        if (haveInstalledAlipay(context)) {
                            jumpToAlipyScreen(context);
                        } else {
                            showSaveQRCodeDialog(context, R.drawable.alipay_money_revised);
                        }
                        break;
                    case DialogInterface.BUTTON_NEUTRAL:
                        break;
                }
            }
        };
        new AlertDialog.Builder(context).setView(R.layout.layout_donate_dialog)
                .setTitle(R.string.title_donate_dialog_donate_methods)
                .setNeutralButton(android.R.string.no, onClickListener)
                .setPositiveButton(R.string.title_donate_dialog_alipay, onClickListener)
                .setNegativeButton(R.string.title_donate_dialog_wechat, onClickListener)
                .show();

    }

    private static void showSaveQRCodeDialog(Context context, int resId) {
        final ImageView imageView = new ImageView(context);
        imageView.setImageResource(resId);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        new AlertDialog.Builder(context).setView(imageView)
                .setNeutralButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.title_donate_dialog_save_qr_code, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        saveImage(imageView);
                    }
                })
                .show();
    }

    private static void saveImage(ImageView imageView) {
        final String[] PERMISSIONS_STORAGE;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            PERMISSIONS_STORAGE = new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        } else {
            PERMISSIONS_STORAGE = new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
        int permission = ActivityCompat.checkSelfPermission(imageView.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    (Activity) imageView.getContext(),
                    PERMISSIONS_STORAGE,
                    1
            );
        }
        imageView.setDrawingCacheEnabled(true);
        Bitmap bitmap = imageView.getDrawingCache();
        MediaStore.Images.Media.insertImage(imageView.getContext().getContentResolver(), bitmap, "donate", "thanks");
        Toast.makeText(imageView.getContext(), R.string.text_donate_dialog_qr_code_saved, Toast.LENGTH_SHORT).show();
        imageView.setDrawingCacheEnabled(false);
    }

    public static boolean haveInstalledAlipay(Context context) {
        try {
            return context.getPackageManager().getPackageInfo("com.eg.android.AlipayGphone", PackageManager.GET_ACTIVITIES) != null;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void jumpToAlipyScreen(Context context) {
        String qrcode = URLEncoder.encode("HTTPS://QR.ALIPAY.COM/FKX05494PUYB5GFV1VNXAD");
        String url = "alipayqr://platformapi/startapp?saId=10000007&clientVersion=3.7.0.0718&qrcode=" + qrcode + "%3F_s%3Dweb-other&_t=" + System.currentTimeMillis();
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(intent);
    }

}
