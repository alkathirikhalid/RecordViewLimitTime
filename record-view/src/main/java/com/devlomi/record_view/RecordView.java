package com.devlomi.record_view;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;

import io.supercharge.shimmerlayout.ShimmerLayout;

/**
 * Created by Devlomi on 24/08/2017.
 */

public class RecordView extends RelativeLayout {

    public static final int DEFAULT_CANCEL_BOUNDS = 8; //8dp
    private ImageView smallBlinkingMic, basketImg;
    private Chronometer counterTime;
    private TextView slideToCancel;
    private ShimmerLayout slideToCancelLayout;
    private ImageView arrow;
    private TextView mStopView;
    private float initialX, basketInitialY, difX = 0;
    private float cancelBounds = DEFAULT_CANCEL_BOUNDS;
    private long startTime, elapsedTime = 0;
    private Context context;
    private OnRecordListener recordListener;
    private boolean isSwiped, isLessThanSecondAllowed = false;
    private boolean isSoundEnabled = true;
    private int RECORD_START = R.raw.record_start;
    private int RECORD_FINISHED = R.raw.record_finished;
    private int RECORD_ERROR = R.raw.record_error;
    private MediaPlayer player;
    private AnimationHelper animationHelper;
    private RecordLockView recordLockView;
    private RecordButton recordButton;
    private Animation animJump, animJumpFast;

    private float lastX, lastY;
    private float firstX, firstY;

    public RecordView(Context context) {
        super(context);
        this.context = context;
        init(context, null, -1, -1);
    }

    public RecordView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(context, attrs, -1, -1);
    }

    public RecordView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init(context, attrs, defStyleAttr, -1);
    }

    public RecordView setRecordLockView(RecordLockView recordLockView) {
        this.recordLockView = recordLockView;
        hideViews(true);
        return this;
    }

    public RecordView setRecordButton(RecordButton recordButton) {
        this.recordButton = recordButton;
        return this;
    }


    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        View view = View.inflate(context, R.layout.record_view_layout, null);
        addView(view);


        ViewGroup viewGroup = (ViewGroup) view.getParent();
        viewGroup.setClipChildren(false);

        arrow = view.findViewById(R.id.arrow);
        slideToCancel = view.findViewById(R.id.slide_to_cancel);
        smallBlinkingMic = view.findViewById(R.id.glowing_mic);
        counterTime = view.findViewById(R.id.counter_tv);
        basketImg = view.findViewById(R.id.basket_img);
        slideToCancelLayout = view.findViewById(R.id.shimmer_layout);
        mStopView = view.findViewById(R.id.stopView);

        animJump = AnimationUtils.loadAnimation(getContext(),
                R.anim.jump);
        animJumpFast = AnimationUtils.loadAnimation(getContext(),
                R.anim.jump_fast);

        hideViews(true);

        if (attrs != null && defStyleAttr == -1 && defStyleRes == -1) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RecordView,
                    defStyleAttr, defStyleRes);


            int slideArrowResource = typedArray.getResourceId(R.styleable.RecordView_slide_to_cancel_arrow, -1);
            String slideToCancelText = typedArray.getString(R.styleable.RecordView_slide_to_cancel_text);
            int slideMarginRight = (int) typedArray.getDimension(R.styleable.RecordView_slide_to_cancel_margin_right, 30);
            int counterTimeColor = typedArray.getColor(R.styleable.RecordView_counter_time_color, -1);
            int arrowColor = typedArray.getColor(R.styleable.RecordView_slide_to_cancel_arrow_color, -1);
            String stopText = typedArray.getString(R.styleable.RecordView_stop_text);

            int cancelBounds = typedArray.getDimensionPixelSize(R.styleable.RecordView_slide_to_cancel_bounds, -1);

            if (cancelBounds != -1)
                setCancelBounds(cancelBounds, false);//don't convert it to pixels since it's already in pixels


            if (slideArrowResource != -1) {
                Drawable slideArrow = AppCompatResources.getDrawable(getContext(), slideArrowResource);
                arrow.setImageDrawable(slideArrow);
            }

            if (slideToCancelText != null)
                slideToCancel.setText(slideToCancelText);

            if (counterTimeColor != -1)
                setCounterTimeColor(counterTimeColor);


            if (arrowColor != -1)
                setSlideToCancelArrowColor(arrowColor);

            if (stopText != null)
                mStopView.setText(stopText);

            setMarginRight(slideMarginRight, true);

            typedArray.recycle();
        }


        animationHelper = new AnimationHelper(context, basketImg, smallBlinkingMic);

        mStopView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                isLocked = false;
                long time = System.currentTimeMillis() - startTime;
                //if the time was less than one second then do not start basket animation
                if (isLessThanOneSecond(time)) {
                    hideViews(true);
                    animationHelper.clearAlphaAnimation(false);


                    animationHelper.onAnimationEnd();

                } else {
                    hideViews(false);
                    animationHelper.animateBasket(basketInitialY);
                }

                if (recordButton != null)
                    animationHelper.moveRecordButtonAndSlideToCancelBack(recordButton, slideToCancelLayout, initialX, difX);

                counterTime.stop();
                slideToCancelLayout.stopShimmerAnimation();
                isSwiped = true;


                animationHelper.setStartRecorded(false);

                if (recordListener != null)
                    recordListener.onCancel();
            }
        });

    }


    private void hideViews(boolean hideSmallMic) {
        firstX = 0;
        firstY = 0;
        lastX = 0;
        lastY = 0;
        isLocked = false;
        slideToCancelLayout.setVisibility(GONE);
        counterTime.setVisibility(GONE);
        mStopView.setVisibility(GONE);
        if (hideSmallMic)
            smallBlinkingMic.setVisibility(GONE);
        if (recordLockView != null) {
            recordLockView.getmLayoutLock().setVisibility(View.GONE);
            recordLockView.getmLayoutLock().setTranslationY(0);
            recordLockView.getmImageViewLockArrow().clearAnimation();
            recordLockView.getmImageViewLock().clearAnimation();
        }
        if (recordButton != null && recordButton.getImageResource() != -1) {
            recordButton.setTheImageResource(recordButton.getImageResource());
        }
    }

    private void showViews() {
        mStopView.setVisibility(GONE);
        slideToCancelLayout.setVisibility(VISIBLE);
        smallBlinkingMic.setVisibility(VISIBLE);
        counterTime.setVisibility(VISIBLE);
        if (recordLockView != null) {
            recordLockView.getmLayoutLock().setVisibility(View.VISIBLE);
            recordLockView.getmImageViewLockArrow().clearAnimation();
            recordLockView.getmImageViewLock().clearAnimation();
            recordLockView.getmImageViewLockArrow().startAnimation(animJumpFast);
            recordLockView.getmImageViewLock().startAnimation(animJump);
        }
    }

    private void showLockedViews() {
        slideToCancelLayout.setVisibility(GONE);
        mStopView.setVisibility(VISIBLE);
        recordButton.stopScale();
        if (recordLockView != null) {
            recordLockView.getmLayoutLock().setVisibility(View.GONE);
            recordLockView.getmLayoutLock().setTranslationY(0);
            recordLockView.getmImageViewLockArrow().clearAnimation();
            recordLockView.getmImageViewLock().clearAnimation();
        }
        if (recordButton != null) {
            if (recordButton.getLockSendResource() != -1) {
                recordButton.setTheImageResource(recordButton.getLockSendResource());
            }
            animationHelper.moveRecordButtonAndSlideToCancelBack(recordButton, slideToCancelLayout, initialX, difX);
        }

    }


    private boolean isLessThanOneSecond(long time) {
        return time <= 1000;
    }


    private void playSound(int soundRes) {

        if (isSoundEnabled) {
            if (soundRes == 0)
                return;

            try {
                player = new MediaPlayer();
                AssetFileDescriptor afd = context.getResources().openRawResourceFd(soundRes);
                if (afd == null) return;
                player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                player.prepare();
                player.start();
                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.release();
                    }

                });
                player.setLooping(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }


    protected void onActionDown(RecordButton recordBtn, MotionEvent motionEvent) {

        if (isLocked)
            return;

        if (recordListener != null)
            recordListener.onStart();

        if (firstX == 0) {
            firstX = motionEvent.getRawX();
        }

        if (firstY == 0) {
            firstY = motionEvent.getRawY();
        }

        animationHelper.setStartRecorded(true);
        animationHelper.resetBasketAnimation();
        animationHelper.resetSmallMic();


        recordBtn.startScale();
        slideToCancelLayout.startShimmerAnimation();

        initialX = recordBtn.getX();

        basketInitialY = basketImg.getY() + 90;

        playSound(RECORD_START);

        showViews();

        animationHelper.animateSmallMicAlpha();
        counterTime.setBase(SystemClock.elapsedRealtime());
        startTime = System.currentTimeMillis();
        counterTime.start();
        isSwiped = false;

    }

    boolean isLocked = false;

    protected void onActionMove(RecordButton recordBtn, MotionEvent motionEvent) {

        long time = System.currentTimeMillis() - startTime;

        float motionX = Math.abs(firstX - motionEvent.getRawX());
        float motionY = Math.abs(firstY - motionEvent.getRawY());

        if (motionY > motionX && (firstY - lastY > 50)) {
            isLocked = true;
        } else {
            isLocked = false;
        }

        if (!isSwiped) {

            //Swipe To Cancel
            if (slideToCancelLayout.getX() != 0 && slideToCancelLayout.getX() <= counterTime.getRight() + cancelBounds) {

                //if the time was less than one second then do not start basket animation
                if (isLessThanOneSecond(time)) {
                    hideViews(true);
                    animationHelper.clearAlphaAnimation(false);


                    animationHelper.onAnimationEnd();

                } else {
                    hideViews(false);
                    animationHelper.animateBasket(basketInitialY);
                }

                animationHelper.moveRecordButtonAndSlideToCancelBack(recordBtn, slideToCancelLayout, initialX, difX);

                counterTime.stop();
                slideToCancelLayout.stopShimmerAnimation();
                isSwiped = true;


                animationHelper.setStartRecorded(false);

                if (recordListener != null)
                    recordListener.onCancel();


            } else {


                //if statement is to Prevent Swiping out of bounds
                if (motionEvent.getRawX() < initialX) {
                    recordBtn.animate()
                            .x(motionEvent.getRawX())
                            .setDuration(0)
                            .start();


                    if (difX == 0)
                        difX = (initialX - slideToCancelLayout.getX());


                    slideToCancelLayout.animate()
                            .x(motionEvent.getRawX() - difX)
                            .setDuration(0)
                            .start();


                }


            }

        }

        lastX = motionEvent.getRawX();
        lastY = motionEvent.getRawY();
    }

    public void onActionUp(RecordButton recordBtn) {

        if (isLocked && mStopView.getVisibility() != View.VISIBLE) {
            showLockedViews();
            return;
        }

        elapsedTime = System.currentTimeMillis() - startTime;

        if (!isLessThanSecondAllowed && isLessThanOneSecond(elapsedTime) && !isSwiped) {
            if (recordListener != null)
                recordListener.onLessThanSecond();

            animationHelper.setStartRecorded(false);

            playSound(RECORD_ERROR);


        } else {
            if (recordListener != null && !isSwiped)
                recordListener.onFinish(elapsedTime);

            animationHelper.setStartRecorded(false);


            if (!isSwiped)
                playSound(RECORD_FINISHED);

        }


        //if user has swiped then do not hide SmallMic since it will be hidden after swipe Animation
        hideViews(!isSwiped);


        if (!isSwiped)
            animationHelper.clearAlphaAnimation(true);

        if (recordBtn != null)
            animationHelper.moveRecordButtonAndSlideToCancelBack(recordBtn, slideToCancelLayout, initialX, difX);
        counterTime.stop();
        slideToCancelLayout.stopShimmerAnimation();


    }


    private void setMarginRight(int marginRight, boolean convertToDp) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) slideToCancelLayout.getLayoutParams();
        if (convertToDp) {
            layoutParams.rightMargin = (int) DpUtil.toPixel(marginRight, context);
        } else
            layoutParams.rightMargin = marginRight;

        slideToCancelLayout.setLayoutParams(layoutParams);
    }


    public void setOnRecordListener(OnRecordListener recrodListener) {
        this.recordListener = recrodListener;
    }

    public void setOnBasketAnimationEndListener(OnBasketAnimationEnd onBasketAnimationEndListener) {
        animationHelper.setOnBasketAnimationEndListener(onBasketAnimationEndListener);
    }

    public void setSoundEnabled(boolean isEnabled) {
        isSoundEnabled = isEnabled;
    }

    public void setLessThanSecondAllowed(boolean isAllowed) {
        isLessThanSecondAllowed = isAllowed;
    }

    public void setSlideToCancelText(String text) {
        slideToCancel.setText(text);
    }

    public void setSlideToCancelTextColor(int color) {
        slideToCancel.setTextColor(color);
    }

    public void setSmallMicColor(int color) {
        smallBlinkingMic.setColorFilter(color);
    }

    public void setSmallMicIcon(int icon) {
        smallBlinkingMic.setImageResource(icon);
    }

    public void setSlideMarginRight(int marginRight) {
        setMarginRight(marginRight, true);
    }


    public void setCustomSounds(int startSound, int finishedSound, int errorSound) {
        //0 means do not play sound
        RECORD_START = startSound;
        RECORD_FINISHED = finishedSound;
        RECORD_ERROR = errorSound;
    }

    public float getCancelBounds() {
        return cancelBounds;
    }

    public void setCancelBounds(float cancelBounds) {
        setCancelBounds(cancelBounds, true);
    }

    //set Chronometer color
    public void setCounterTimeColor(int color) {
        counterTime.setTextColor(color);
    }

    public void setSlideToCancelArrowColor(int color) {
        arrow.setColorFilter(color);
    }


    private void setCancelBounds(float cancelBounds, boolean convertDpToPixel) {
        float bounds = convertDpToPixel ? DpUtil.toPixel(cancelBounds, context) : cancelBounds;
        this.cancelBounds = bounds;
    }

}


