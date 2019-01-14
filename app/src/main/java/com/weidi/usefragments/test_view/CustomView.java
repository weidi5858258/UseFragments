package com.weidi.usefragments.test_view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by root on 19-1-14.
 */

public class CustomView extends View {

    private Paint mPaint = new Paint();
    private Path mPath = new Path();

    public CustomView(Context context) {
        super(context);
        setPaint();
    }

    public CustomView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setPaint();
    }

    public CustomView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int
            defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 设置当前View的背景色(三选一)
        // canvas.drawColor(Color.parseColor("#88880000"));
        // canvas.drawRGB(100, 200, 100);
        canvas.drawARGB(50, 100, 200, 100);

        // 绘制文字
        canvas.drawText("2019您好!", 50, 100, mPaint);

        // 两个关键点坐标: 左上 右下
        canvas.drawRect(100, 100, 500, 500, mPaint);
        // drawRect(Rect  rect, Paint paint)
        // drawRect(RectF rect, Paint paint)

        // 绘制一个圆(圆心坐标和半径的单位都是像素)
        canvas.drawCircle(300, 300, 180, mPaint);

        // 画椭圆(left, top, right, bottom是这个椭圆的左、上、右、下四个边界点的坐标)
        // 只能绘制横着的或者竖着的椭圆，不能绘制斜的
        canvas.drawOval(400, 50, 700, 200, mPaint);
        // drawOval(RectF rect, Paint paint)

        // 画线(startX, startY, stopX, stopY 分别是线的起点和终点坐标)
        // 由于直线不是封闭图形，所以setStyle(style)对直线没有影响
        canvas.drawLine(200, 200, 800, 500, mPaint);

        // 批量画线
        float[] points = new float[]{
                20, 20, 120, 20,
                70, 20, 70, 120,
                20, 120, 120, 120,
                150, 20, 250, 20,
                150, 20, 150, 120,
                250, 20, 250, 120,
                150, 120, 250, 120};
        canvas.drawLines(points, mPaint);

        // 画圆角矩形(left, top, right, bottom 是四条边的坐标，rx 和 ry 是圆角的横向半径和纵向半径)
        canvas.drawRoundRect(100, 100, 500, 300, 50, 50, mPaint);
        // drawRoundRect(RectF rect, float rx, float ry, Paint paint)

        // 填充模式
        mPaint.setStyle(Paint.Style.FILL);
        // 绘制扇形
        canvas.drawArc(200, 100, 800, 500,
                -110, 100,
                true, mPaint);
        // 绘制弧形
        canvas.drawArc(200, 100, 800, 500,
                20, 140,
                false, mPaint);
        // 画线模式
        mPaint.setStyle(Paint.Style.STROKE);
        // 绘制不封口的弧形
        canvas.drawArc(200, 100, 800, 500,
                180, 60,
                false, mPaint);

        // 画自定义图形
        canvas.drawPath(mPath, mPaint);

        // 画点
        mPaint.setStrokeWidth(20);
        // SQUARE或BUTT画出来是方形的点
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawPoint(600, 600, mPaint);

        // 批量画点
        points = new float[]{0, 0, 50, 50, 50, 100, 100, 50, 100, 100, 150, 50, 150, 100};
        // 绘制四个点：(50, 50) (50, 100) (100, 50) (100, 100)
        canvas.drawPoints(
                points,
                2 /* 跳过两个数,即前两个0 */,
                8 /* 一共绘制8个数(4个点) */,
                mPaint);
    }

    private void setPaint() {
        // 设置绘制模式(填充)
        // mPaint.setStyle(Paint.Style.FILL);
        // 设置绘制模式(镂空)
        mPaint.setStyle(Paint.Style.STROKE);
        // 设置线条宽度(在STROKE和FILL_AND_STROKE模式下有效)
        mPaint.setStrokeWidth(1);
        // 设置颜色
        mPaint.setColor(Color.GREEN);
        // 设置文字大小
        mPaint.setTextSize(100);
        // 设置抗锯齿开关
        mPaint.setAntiAlias(true);
    }

    private void setPath() {
        // 使用path对图形进行描述（这段描述代码不必看懂）
        mPath.addArc(200, 200, 400, 400,
                -225, 225);
        mPath.arcTo(400, 200, 600, 400,
                -180, 225, false);
        mPath.lineTo(400, 542);
    }

}

/***
 drawArc() 是使用一个椭圆来描述弧形的。
 left, top, right, bottom 描述的是这个弧形所在的椭圆；
 startAngle 是弧形的起始角度（x 轴的正向，即正右的方向，是 0 度的位置；顺时针为正角度，逆时针为负角度），
 sweepAngle 是弧形划过的角度；
 useCenter 表示是否连接到圆心，如果不连接到圆心，就是弧形，
 如果连接到圆心，就是扇形。

 drawPath(Path path, Paint paint) 画自定义图形
 drawPath(path) 这个方法是通过描述路径的方式来绘制图形的，它的 path 参数就是用来描述图形路径的对象。
 */