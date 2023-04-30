// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2020 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

package com.tencent.yolov5ncnn;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends Activity
{
    private static final int SELECT_IMAGE = 1;

    private ImageView imageView;
    private Bitmap bitmap = null;
    private Bitmap yourSelectedImage = null;

    private YoloV5Ncnn yolov5ncnn = new YoloV5Ncnn();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        boolean ret_init = yolov5ncnn.Init(getAssets());
        if (!ret_init)
        {
            Log.e("MainActivity", "yolov5ncnn Init failed");
        }

        imageView = (ImageView) findViewById(R.id.imageView);

        Button buttonImage = (Button) findViewById(R.id.buttonImage);
        buttonImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent i = new Intent(Intent.ACTION_PICK);
                i.setType("image/*");
                startActivityForResult(i, SELECT_IMAGE);
            }
        });

        Button buttonDetect = (Button) findViewById(R.id.buttonDetect);
        buttonDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (yourSelectedImage == null)
                    return;

                YoloV5Ncnn.Obj[] objects = yolov5ncnn.Detect(yourSelectedImage, false);
                showObjects_Detect(objects);
            }
        });

        Button buttonDetectGPU = (Button) findViewById(R.id.buttonDetectGPU);
        buttonDetectGPU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (yourSelectedImage == null)
                    return;

                YoloV5Ncnn.Obj[] objects = yolov5ncnn.Detect(yourSelectedImage, true);

                showObjects_Detect(objects);
            }
        });

        Button buttonClassDetectCPU = (Button) findViewById(R.id.buttonClassDetectCPU);
        buttonClassDetectCPU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (yourSelectedImage == null)
                    return;

                YoloV5Ncnn.Obj[] objects = yolov5ncnn.Class(yourSelectedImage, false);

                showObjects_Class(objects);
            }
        });

        Button buttonClassDetectGPU = (Button) findViewById(R.id.buttonClassDetectGPU);
        buttonClassDetectGPU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (yourSelectedImage == null)
                    return;

                YoloV5Ncnn.Obj[] objects = yolov5ncnn.Class(yourSelectedImage, true);

                showObjects_Class(objects);
        }

    });
    }

    /**
     * 显示检测到的目标物体在图像中的位置，并将结果显示在ImageView上
     * 如果未检测到任何目标，则直接显示原始图像
     * @param objects 检测到的目标对象数组，可以为空
     */
    private void showObjects_Detect(YoloV5Ncnn.Obj[] objects)
    {
        if (objects == null)
        {
            // 如果未检测到任何目标，则直接将原始图像设置为ImageView的位图
            imageView.setImageBitmap(bitmap);
            return;
        }

        // 在位图上绘制检测到的目标框和文本信息
        Bitmap rgba = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        // 定义目标框的颜色
        final int[] colors = new int[] {
            Color.rgb( 0,  255, 0),
            Color.rgb( 255,  0, 0)
        };

        Canvas canvas = new Canvas(rgba);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);

        Paint textbgpaint = new Paint();
        textbgpaint.setColor(Color.WHITE);
        textbgpaint.setStyle(Paint.Style.FILL);

        Paint textpaint = new Paint();
        textpaint.setColor(Color.BLACK);
        textpaint.setTextSize(10);
        textpaint.setTextAlign(Paint.Align.LEFT);

        for (int i = 0; i < objects.length; i++)
        {
            // 设置当前目标框的颜色
            paint.setColor(colors[objects[i].label.equals("benign")  ? 0 : 1]);

            // 在画布上绘制目标框
            canvas.drawRect(objects[i].x, objects[i].y, objects[i].x + objects[i].w, objects[i].y + objects[i].h, paint);

            // 在目标框内部绘制文本信息
            {
                String text = objects[i].label + " = " + String.format("%.1f", objects[i].prob * 100) + "%";

                // 计算文本的宽度和高度
                float text_width = textpaint.measureText(text);
                float text_height = - textpaint.ascent() + textpaint.descent();

                // 确定文本在图像上的位置
                float x = objects[i].x;
                float y = objects[i].y - text_height;
                if (y < 0)
                    y = 0;
                if (x + text_width > rgba.getWidth())
                    x = rgba.getWidth() - text_width;

                // 在画布上绘制带背景的文本框
                canvas.drawRect(x, y, x + text_width, y + text_height, textbgpaint);

                // 在文本框中绘制文本
                canvas.drawText(text, x, y - textpaint.ascent(), textpaint);
            }
        }

        imageView.setImageBitmap(rgba);
    }

    private void showObjects_Class(YoloV5Ncnn.Obj[] objects)
    {
        if (objects == null)
        {
            // 如果未检测到任何目标，则直接将原始图像设置为ImageView的位图
            imageView.setImageBitmap(bitmap);
            return;
        }

        // 在位图上绘制检测到的目标框和文本信息
        Bitmap rgba = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        // 定义目标框的颜色
        final int[] colors = new int[] {
                Color.rgb( 0,  255, 0),
                Color.rgb( 255,  0, 0)
        };

        Canvas canvas = new Canvas(rgba);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);

        Paint textbgpaint = new Paint();
        textbgpaint.setColor(Color.WHITE);
        textbgpaint.setStyle(Paint.Style.FILL);

        Paint textpaint = new Paint();
        textpaint.setColor(Color.BLACK);
        textpaint.setTextSize(10);
        textpaint.setTextAlign(Paint.Align.LEFT);

        for (int i = 0; i < objects.length; i++)
        {
            // 设置当前目标框的颜色
            paint.setColor(colors[(objects[i].label.equals("BIRADS-1")||objects[i].label.equals("BIRADS-2")||objects[i].label.equals("BIRADS-3"))? 0 : 1]);

            // 在画布上绘制目标框
            canvas.drawRect(objects[i].x, objects[i].y, objects[i].x + objects[i].w, objects[i].y + objects[i].h, paint);

            // 在目标框内部绘制文本信息
            {
                String text = objects[i].label + " = " + String.format("%.1f", objects[i].prob * 100) + "%";

                // 计算文本的宽度和高度
                float text_width = textpaint.measureText(text);
                float text_height = - textpaint.ascent() + textpaint.descent();

                // 确定文本在图像上的位置
                float x = objects[i].x;
                float y = objects[i].y - text_height;
                if (y < 0)
                    y = 0;
                if (x + text_width > rgba.getWidth())
                    x = rgba.getWidth() - text_width;

                // 在画布上绘制带背景的文本框
                canvas.drawRect(x, y, x + text_width, y + text_height, textbgpaint);

                // 在文本框中绘制文本
                canvas.drawText(text, x, y - textpaint.ascent(), textpaint);
            }
        }

        imageView.setImageBitmap(rgba);
    }

    @Override
    // 处理从相册返回的图像数据
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        // 如果结果为“成功”，并且数据不为空（即已选择图像）
        if (resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();

            try
            {
                if (requestCode == SELECT_IMAGE) { // 检查请求码以确保它是我们发送的
                    bitmap = decodeUri(selectedImage); // 调用解码函数并获得位图

                    yourSelectedImage = bitmap.copy(Bitmap.Config.ARGB_8888, true); // 创建一个可修改副本以供进一步处理

                    imageView.setImageBitmap(bitmap); // 在ImageView中显示位图
                }
            }
            catch (FileNotFoundException e) // 如果无法找到文件则捕获异常并记录日志
            {
                Log.e("MainActivity", "FileNotFoundException");
                return;
            }
        }
    }

    // 解码图像并应用缩小比例以适应指定大小
    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException
    {
        // 获取图像的实际大小
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // 定义新的目标尺寸
        final int REQUIRED_SIZE = 640;

        // 确定正确的缩放比例。应该是2的幂
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
               || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // 应用缩放比例并解码图像
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);

        // 根据EXIF信息旋转图像
        int rotate = 0;
        try
        {
            ExifInterface exif = new ExifInterface(getContentResolver().openInputStream(selectedImage));
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
        }
        catch (IOException e) // 如果无法读取EXIF数据则捕获异常并记录日志
        {
            Log.e("MainActivity", "ExifInterface IOException");
        }

        // 根据旋转角度创建一个矩阵并将其应用于位图
        Matrix matrix = new Matrix();
        matrix.postRotate(rotate);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

}
