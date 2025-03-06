package com.example.backgammon

import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat

class BackgammonBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    // משתנים עבור התמונות של הקוביות
    private  var dice1: Drawable = ContextCompat.getDrawable(context, R.drawable.dice_1)!!
    private  var dice2: Drawable = ContextCompat.getDrawable(context, R.drawable.dice_1)!!

    // ערכים אקראיים עבור הקוביות
    private var dice1Value = 0
    private var dice2Value = 0
    fun loadDiceImages(context: Context) {
        dice1 = when (dice1Value) {
            1 -> ContextCompat.getDrawable(context, R.drawable.dice_1)!!
            2 -> ContextCompat.getDrawable(context, R.drawable.dice_2)!!
            3 -> ContextCompat.getDrawable(context, R.drawable.dice_3)!!
            4 -> ContextCompat.getDrawable(context, R.drawable.dice_4)!!
            5 -> ContextCompat.getDrawable(context, R.drawable.dice_5)!!
            else -> ContextCompat.getDrawable(context, R.drawable.dice_6)!!
        }

        dice2 = when (dice2Value) {
            1 -> ContextCompat.getDrawable(context, R.drawable.dice_1)!!
            2 -> ContextCompat.getDrawable(context, R.drawable.dice_2)!!
            3 -> ContextCompat.getDrawable(context, R.drawable.dice_3)!!
            4 -> ContextCompat.getDrawable(context, R.drawable.dice_4)!!
            5 -> ContextCompat.getDrawable(context, R.drawable.dice_5)!!
            else -> ContextCompat.getDrawable(context, R.drawable.dice_6)!!
        }
    }
    // פונקציה להגרלת ערכים חדשים
    private fun rollDice() {

        dice1Value = (1..6).random()  // הגרלת ערך אקראי לקוביה 1
        dice2Value = (1..6).random()  // הגרלת ערך אקראי לקוביה 2
        Toast.makeText(context,"${dice1Value}, ${dice2Value}, dice",Toast.LENGTH_SHORT).show()
        loadDiceImages(context)  // טוען את התמונות החדשות
        movePosition(dice1Value,dice2Value,0)

        invalidate()  // עדכון הציור
    }



    private val boardPaint = Paint().apply {
        color = Color.rgb(139, 69, 19) // חום כהה ללוח
        style = Paint.Style.FILL
    }

    private val trianglePaint = Paint().apply {
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val trianglePath = Path()

    private val checkerPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val checkerBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.BLACK
        isAntiAlias = true
    }

    // מערך המייצג את מצב הלוח - מספר החיילים בכל עמודה והצבע שלהם
    private val boardState = Array(26) { position ->
        when (position) {
            0 -> Pair(2, true)    // 2 חיילים לבנים בעמדה 1
            5 -> Pair(5, false)   // 5 חיילים שחורים בעמדה 6
            7 -> Pair(3, false)   // 3 חיילים שחורים בעמדה 8
            11 -> Pair(5, true)   // 5 חיילים לבנים בעמדה 12
            12 -> Pair(5, false)  // 5 חיילים שחורים בעמדה 13
            16 -> Pair(3, true)   // 3 חיילים לבנים בעמדה 17
            18 -> Pair(5, true)   // 5 חיילים לבנים בעמדה 19
            23 -> Pair(2, false)  // 2 חיילים שחורים בעמדה 24
            else -> Pair(0, true) // עמדה ריקה
        }
    }

    private var selectedCheckerPosition: Int? = null // מיקום החייל שנבחר להזזה

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val padding = 0.0f // שוליים של 2%
        val drawable: Drawable = dice1
        val drawable2: Drawable = dice2

        // מיקום לציור (X, Y)
        val x = 2730f
        val y = 560f
        val x1 = 2500f
        val y1 = 560f

        // ציור ה-Drawable על ה-Canvas

        // צייר את הרקע של הלוח
        canvas.drawRect(padding, padding, width - padding, height - padding, boardPaint)

        val boardWidth = width - (padding * 2)
        val boardHeight = height - (padding * 2)
        val triangleWidth = boardWidth / 12 // 6 משולשים בכל חצי לוח
        val triangleHeight = (boardHeight - boardHeight * 0.15f) / 2 // גובה המשולש עם מרווח במרכז

        // צייר משולשים בחלק התחתון
        for (i in 0..11) {
            trianglePath.reset()
            trianglePaint.color = if (i % 2 == 0) Color.RED else Color.BLACK

            val startX = padding + (i * triangleWidth)
            trianglePath.moveTo(startX, height - padding)
            trianglePath.lineTo(startX + (triangleWidth / 2), height - padding - triangleHeight)
            trianglePath.lineTo(startX + triangleWidth, height - padding)
            trianglePath.close()
            canvas.drawPath(trianglePath, trianglePaint)
        }

        // צייר משולשים בחלק העליון
        for (i in 0..11) {
            trianglePath.reset()
            trianglePaint.color = if (i % 2 == 0) Color.BLACK else Color.RED

            val startX = padding + (i * triangleWidth)
            trianglePath.moveTo(startX, padding)
            trianglePath.lineTo(startX + (triangleWidth / 2), padding + triangleHeight)
            trianglePath.lineTo(startX + triangleWidth, padding)
            trianglePath.close()
            canvas.drawPath(trianglePath, trianglePaint)
        }

        // צייר את המסגרת וקו האמצע
        canvas.drawRect(padding, padding, width - padding, height - padding, borderPaint)
        canvas.drawLine(width / 2, padding, width / 2, height - padding, borderPaint)

        // צייר את החיילים
        drawCheckers(canvas, width, height, padding, triangleWidth)
        drawable.setBounds(x.toInt(), y.toInt(), (x + drawable.intrinsicWidth).toInt(), (y + drawable.intrinsicHeight).toInt())
        drawable.draw(canvas)
        drawable2.setBounds(x1.toInt(), y1.toInt(), (x1 + drawable.intrinsicWidth).toInt(), (y1 + drawable.intrinsicHeight).toInt())
        drawable2.draw(canvas)
    }

    private fun drawCheckers(canvas: Canvas, width: Float, height: Float, padding: Float, triangleWidth: Float) {
        val checkerRadius = triangleWidth * 0.2f

        for (i in 0..25) {
            val (count, isWhite) = boardState[i]
            if (count > 0) {
                val x = if (i < 12) {
                    width - padding - (i * triangleWidth + triangleWidth / 2)
                } else {
                    if (i>23)
                    {
                        width / 2
                    }
                    else {
                        padding + ((i - 12) * triangleWidth + triangleWidth / 2)
                    }
                }

                for (j in 0 until count) {
                    val y = if (i < 12) {
                        height - padding - (j + 1) * checkerRadius * 1.8f
                    } else {
                        if (i==24){
                            height - padding - (j + 1) * checkerRadius * 1.8f
                        }
                        else{
                            padding + (j + 1) * checkerRadius * 1.8f

                        }
                    }

                    // צייר את החייל
                    checkerPaint.color = if (isWhite) Color.WHITE else Color.BLACK
                    canvas.drawCircle(x, y, checkerRadius, checkerPaint)
                    canvas.drawCircle(x, y, checkerRadius, checkerBorderPaint)

                    // הוסף אפקט הדגשה לחייל
                    checkerPaint.color = if (isWhite) Color.LTGRAY else Color.DKGRAY
                    canvas.drawCircle(x, y - checkerRadius * 0.1f, checkerRadius * 0.85f, checkerPaint)
                }
            }
        }
    }

    private fun movePosition(dice1Value: Int, dice2Value: Int, fromPosition: Int, ) {
        var positionOne= boardState[fromPosition+dice1Value]
        var positionTwo= boardState[fromPosition+dice2Value]
        var positionThree= boardState[fromPosition+dice1Value+dice2Value]
        val threePosition = Array<Int>(3, { -1 })
        Log.d(TAG,"options 1: ${positionOne.toString()},${positionTwo.toString()},${positionThree.toString()}")

        if (positionOne.first < 2 || boardState[fromPosition].second == positionOne.second) {
            threePosition[0] = fromPosition+dice1Value
        }
        if (positionTwo.first < 2 || boardState[fromPosition].second == positionTwo.second) {
            threePosition[1] = fromPosition+dice2Value
        }
        if (positionThree.first < 2 || boardState[fromPosition].second == positionThree.second) {
            threePosition[2] = fromPosition+dice1Value+dice2Value
        }
        Log.d(TAG,"options:${threePosition[0].toString()}, ${threePosition[1].toString()} , ${threePosition[2].toString()}")

    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // זיהוי אם לחצת על חייל
                Toast.makeText(context,"${y}, ${x}",Toast.LENGTH_SHORT).show()
                if(y<565||y>800) {
                    val clickedPosition = getPositionFromTouch(x, y)
                    handleTouch(clickedPosition)
                }
                else{
                    if(x>2400&&x<2970){
                        rollDice()
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                // אם יש חייל שנבחר, הזז אותו
                selectedCheckerPosition?.let { selectedPosition ->
                    val targetPosition = getPositionFromTouch(x, y)
                    moveChecker(selectedPosition, targetPosition)
                }
            }
        }
        return true
    }

    private fun handleTouch(position: Int) {
        // בודק אם יש חייל בעמדה
        val (count, _) = boardState[position]
        if (count > 0) {
            selectedCheckerPosition = position // בחר את החייל להזזה
        }
    }

    private fun getPositionFromTouch(x: Float, y: Float): Int {
        // המרת קואורדינטות ל-position
        val triangleWidth = (width - 2 * 0.0f) / 12
        val boardHeight = height - 2 * 0.0f
        val positionIndex = ((x - 0.0f) / triangleWidth).toInt()

        return if (y > boardHeight / 2) {
            11 - positionIndex
        } else {
            12 + positionIndex
        }
    }

    private fun moveChecker(fromPosition: Int, toPosition: Int) {
        val (count, isWhite) = boardState[fromPosition]
        if (count > 0) {
            // הסר את החייל מהמיקום הנבחר
            //Toast.makeText(context,"${count}",Toast.LENGTH_SHORT).show()
            //Toast.makeText(context,"${boardState[fromPosition].first}",Toast.LENGTH_SHORT).show()
            // הוסף אותו למיקום החדש
            if (isWhite)
            {
                if(boardState[toPosition+1].second!=isWhite){
                    if(boardState[toPosition+1].first<2) {
                        boardState[fromPosition] = Pair(count-1, isWhite)
                        if (boardState[toPosition+1].first==1) {
                            boardState[24] = Pair(boardState[24].first + 1, !isWhite)
                        }
                        boardState[toPosition + 1] = Pair(1, isWhite)
                    }
                    else{
                        Toast.makeText(context,"You cant go their you stupid little bit...",Toast.LENGTH_SHORT).show()
                    }
                }
                else{
                    boardState[fromPosition] = Pair(count-1, isWhite)

                    val newCount = boardState[toPosition+1].first + 1
                    boardState[toPosition+1] = Pair(newCount, isWhite)
                }

            }
            else{
                if(boardState[toPosition-1].second!=isWhite){
                    if(boardState[toPosition-1].first<2) {
                        boardState[fromPosition] = Pair(count-1, isWhite)
                        if (boardState[toPosition-1].first==1) {
                            boardState[25] = Pair(boardState[25].first + 1, !isWhite)
                        }
                        boardState[toPosition - 1] = Pair(1, isWhite)
                    }
                    else{
                        Toast.makeText(context,"You cant go their you stupid little bit...",Toast.LENGTH_SHORT).show()
                    }
                }
                else{
                    boardState[fromPosition] = Pair(count-1, isWhite)

                    val newCount = boardState[toPosition-1].first + 1
                    boardState[toPosition-1] = Pair(newCount, isWhite)
                }

            }
            invalidate() // עדכון הציור
        }
        selectedCheckerPosition = null
    }
}
