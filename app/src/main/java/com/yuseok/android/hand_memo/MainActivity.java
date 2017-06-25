package com.yuseok.android.hand_memo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.SeekBar;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    FrameLayout layout;
    RadioGroup color;       // 색상조절 옵션
    SeekBar stoke;          // 두께조절 옵션

    Board board;            // 그림판
    ImageView imageView;    // 캡쳐한 이미지를 썸네일로 화면에 표시

    int opt_brush_color = Color.BLACK;
    float opt_brush_width = 10f;
    int mode = Brush.MODE_DRAW;

    // 캡쳐한 이미지를 저장하는 변수
    Bitmap captured = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 그림판이 들어가는 프레임 레이아웃
        layout = (FrameLayout) findViewById(R.id.layout);

        // 색상 선택
        color = (RadioGroup) findViewById(R.id.color);
        color.setOnCheckedChangeListener(checkColorListener);

        // 두께 선택
        stoke = (SeekBar) findViewById(R.id.seekBar);
        stoke.setProgress(10);
        stoke.setOnSeekBarChangeListener(thickChangeListener);

        // 썸네일 이미지뷰
        imageView = (ImageView) findViewById(R.id.imageView);

        // 저장버튼
        findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 드로잉 캐쉬를 지워주고
                layout.destroyDrawingCache();
                // 다시 만들고
                layout.buildDrawingCache();
                // 레이아웃의 그려진 내용을 Bitmap 형태로 가져온다,
                captured = layout.getDrawingCache();
                // 캡쳐한 이미지를 썸네일에 보여준다.
                imageView.setImageBitmap(captured);
            }
        });

        // 1. 그림판을 새로 생성한다.
        board = new Board(getBaseContext());
        // 2. 생성된 보드를 화면에 세팅한다.
        layout.addView(board);
        // 3. 기본 브러쉬 세팅
        setBrush(opt_brush_color, opt_brush_width, mode);
    }

    // captureBoard

    /*
        컬러와 두꼐는 조절 할 떄마다 새로운 브러쉬를 생성하여 그림판에 담는다.
        * 사용하지 않은 브러쉬는 그냥 버려진다. *
     */

    // 컬러 옵션값 조절
    private void setBrushColor(int colorType) {
        opt_brush_color = colorType;
        setBrush(opt_brush_color, opt_brush_width, mode);
    }

    // 두께 옵션값 조절
    private void setBrushStroke(float width) {
        opt_brush_width = width;
        setBrush(opt_brush_color, opt_brush_width, mode);
    }

    // 현재 설정된 옵션값을 사용하여 브러쉬를 새로 생성하고 그림판에 담는다.
    private void setBrush(int color, float width, int mode) {
        Brush brush = Brush.newInstance(color, width, mode);
        board.setBrush(brush);
    }

    /*
        컬러 리스너
     */
    RadioGroup.OnCheckedChangeListener checkColorListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
            switch (checkedId) {
                case R.id.rBlack:
                    mode = Brush.MODE_DRAW;
                    setBrushColor(Color.BLACK);
                    break;
                case R.id.rBlue:
                    mode = Brush.MODE_DRAW;
                    setBrushColor(Color.BLUE);
                    break;
                case R.id.rGreen:
                    mode = Brush.MODE_DRAW;
                    setBrushColor(Color.GREEN);
                    break;
                case R.id.rRed:
                    mode = Brush.MODE_DRAW;
                    setBrushColor(Color.RED);
                    break;
                case R.id.rEraser:
                    mode = Brush.MODE_ERASE;
                    setBrushColor(Color.TRANSPARENT);
                    break;
            }
        }
    };

    /*
        선굵기 리스너
     */
    SeekBar.OnSeekBarChangeListener thickChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            opt_brush_width = progress + 1; // seekbar가 0부터 시작하므로 1을 더해준다.
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        // 터치가 종료되었을때만 값을 세팅해준다.
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            setBrushStroke(opt_brush_width);
        }
    };

    /*
        그림판
     */
    class Board extends View {
        Paint paint;
        Paint erase;
        List<Brush> brushes;
        Brush current_brush;
        Path current_path;

        // 브러쉬의 속성값 변경 여부를 판단하기 위한 플래그 > 브러쉬의 속성값이 바뀌면 Path를 다시 생성한다.
        boolean newBrush = true;

        public Board(Context context) {
            super(context);
            setPaint();
            setErase();
            brushes = new ArrayList<>();
        }

        // 처음 한번만 기본 페인트 속성을 설정해둔다.
        private void setPaint() {
            // Paint 의 기본속성만 적용해 두고, color 와 두께는 Brush에서 가져다가 그린다.
            paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setAntiAlias(true);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setXfermode(null);
        }

        /*
            지워지는 효과를 위해서 XferMode 를 사용한다.
            new PorterDuffXfermode(PorterDuff.Mode.CLEAR)
         */
        // 지우개 설정
        private void setErase() {
            erase = new Paint();
            erase.setColor(Color.TRANSPARENT);
            erase.setStyle(Paint.Style.STROKE);
            erase.setAntiAlias(true);
            erase.setStrokeJoin(Paint.Join.ROUND);
            erase.setStrokeCap(Paint.Cap.ROUND);
            erase.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        }

        // 브러쉬를 새로 생성한다.
        public void setBrush(Brush brush) {
            current_brush = brush;
            newBrush = true;
        }

        // Path를 새로 생성한다.
        private void createPath() {
            if (newBrush) { // 브러쉬가 변경되었을 때만 Path를 생성해준다.
                current_path = new Path();
                current_brush.addPath(current_path);
                brushes.add(current_brush);

                newBrush = false;  // Path가 생성되면 브러쉬에 path가 적용되었다고 알려준다.
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            // xFerMode 에서 투명값을 적용하기 위해 LayerType 을 설정해준다
            setLayerType(LAYER_TYPE_HARDWARE, null);

            for (Brush brush : brushes) {
                // 브러쉬에서 속성값을 꺼내서 Paint 에 반영한다.
                // 지우개 설정
                if (brush.erase) {
                    erase.setStrokeWidth(brush.stroke);
                    canvas.drawPath(brush.path, erase);
                    // 그리기 설정
                } else {
                    //setLayerType(LAYER_TYPE_NONE, paint);
                    paint.setStrokeWidth(brush.stroke);
                    paint.setColor(brush.color);
                    canvas.drawPath(brush.path, paint);
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            // 내가 터치한 좌표를 꺼낸다
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                // 터치가 시작되면 Path 를 생성하고 현재 지정된 브러쉬와 함께 저장소에 담아둔다.
                case MotionEvent.ACTION_DOWN:
                    createPath();
                    current_path.moveTo(x, y); // 이전점과 현재점 사이를 그리지 않고 이동한다.
                    break;
                case MotionEvent.ACTION_MOVE:
                    current_path.lineTo(x, y); // 바로 이전점과 현재점사이에 줄을 그어준다.
                    break;
                case MotionEvent.ACTION_UP:
                    // none
                    break;
            }

            // 화면을 갱신해서 위에서 그린 Path를 반영해 준다.
            invalidate();

            // 리턴 false 일 경우 touch 이벤트를 연속해서 발생시키지 않는다.
            // 즉, 드래그시 onTouchEvent 가 호출되지 않는다
            return true;
        }
    }
}


class Brush {
    // 그리기 모드 설정
    public final static int MODE_DRAW = 100;
    public final static int MODE_ERASE = 200;

    Path path;
    int color;
    float stroke;

    // 지우개 설정값 추가
    boolean erase = false;

    public static Brush newInstance(int color, float width, int mode) {
        Brush brush = new Brush();
        brush.color = color;
        brush.stroke = width;
        switch (mode) {
            case MODE_DRAW:
                brush.erase = false;
                break;
            case MODE_ERASE:
                brush.erase = true;
                break;
        }
        return brush;
    }

    public void addPath(Path path) {
        this.path = path;
    }
}
