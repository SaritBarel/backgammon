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
import kotlin.math.log

class BackgammonBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    // משתנים עבור התמונות של הקוביות
    private  var dice1: Drawable = ContextCompat.getDrawable(context, R.drawable.dice_1)!!
    private  var dice2: Drawable = ContextCompat.getDrawable(context, R.drawable.dice_1)!!
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isWhiteTurn = true // התור מתחיל עם השחקן הלבן
    // ערכים אקראיים עבור הקוביות
    private var dice1Value = 0
    private var dice2Value = 0
    // משתנים למעקב אחרי המהלכים הזמינים
    private var availableMoves = mutableListOf<Int>()
    private var initialDiceRoll = false // האם הקוביות הוטלו בתחילת התור
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

    private fun startGame() {
        isWhiteTurn = true  // קבע מי מתחיל
        showCurrentTurn()
    }

    // פונקציה להגרלת ערכים חדשים
    private fun rollDice() {
        dice1Value = (1..6).random()
        dice2Value = (1..6).random()

        // אתחול המהלכים הזמינים
        availableMoves.clear()
        availableMoves.add(dice1Value)
        availableMoves.add(dice2Value)

        // אם יצאו שני מספרים זהים, מקבלים 4 מהלכים זהים (דאבלס)
        if (dice1Value == dice2Value) {
            availableMoves.add(dice1Value)
            availableMoves.add(dice1Value)
        }

        initialDiceRoll = true

        loadDiceImages(context)

        // הצג את המהלכים הזמינים
        val movesMessage = "קוביות: ${dice1Value}, ${dice2Value}, מהלכים זמינים: ${availableMoves.joinToString(", ")}"
        Toast.makeText(context, movesMessage, Toast.LENGTH_SHORT).show()

        invalidate()
    }

    private fun drawTurnIndicator(canvas: Canvas) {
        val indicatorPaint = Paint().apply {
            color = if (isWhiteTurn) Color.WHITE else Color.BLACK
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        // מיקום האינדיקטור - התאם לפי הצורך
        val x = width / 2f
        val y = 50f
        val radius = 30f

        canvas.drawCircle(x, y, radius, indicatorPaint)
        canvas.drawCircle(x, y, radius, borderPaint)
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

    private fun getValidMoves(fromPosition: Int, isWhite: Boolean): List<Int> {
        val validMoves = mutableListOf<Int>()

        // בדיקת כל אחד מהמהלכים האפשריים
        for (moveValue in availableMoves) {
            // חישוב המיקום החדש בהתאם לצבע השחקן
            val targetPosition = if (isWhite) fromPosition + moveValue else fromPosition - moveValue

            // וידוא שהמיקום בגבולות הלוח
            if (targetPosition in 0..23) {
                val targetSpot = boardState[targetPosition]

                // בדיקה האם המהלך חוקי
                if (targetSpot.first == 0) {
                    // המקום ריק - מהלך חוקי
                    validMoves.add(targetPosition)
                } else if (targetSpot.second == isWhite) {
                    // יש חיילים בצבע של השחקן - מהלך חוקי
                    validMoves.add(targetPosition)
                } else if (targetSpot.first == 1 && targetSpot.second != isWhite) {
                    // יש רק חייל אחד של היריב - אפשר להכות
                    validMoves.add(targetPosition)
                }
                // אם יש יותר מחייל אחד של היריב - המהלך לא חוקי
            }
        }

        return validMoves
    }

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

    private fun movePosition(dice1Value: Int, dice2Value: Int, fromPosition: Int,isWhite:Boolean ): Array<Int>{
        var positionOne = Pair(-1,false)
        var positionTwo = Pair(-1,false)
        var positionThree = Pair(-1,false)
        val threePosition = Array<Int>(3, { -1 })
        if(isWhite) {
            if(fromPosition + dice1Value>0) {
                positionOne = boardState[fromPosition + dice1Value]
            }
            else{
                positionOne = boardState[fromPosition]
            }
            if(fromPosition + dice2Value>0) {
                positionTwo = boardState[fromPosition + dice2Value]
            }
            else {
                positionTwo = boardState[fromPosition]
            }
            if(fromPosition + dice1Value - dice2Value>0) {
                positionThree = boardState[fromPosition + dice1Value + dice2Value]
            }
            else {
                positionThree= boardState[fromPosition]
            }
            if (positionOne.first < 2 || boardState[fromPosition].second == positionOne.second) {
                threePosition[0] = fromPosition+dice1Value
            }
            if (positionTwo.first < 2 || boardState[fromPosition].second == positionTwo.second) {
                threePosition[1] = fromPosition+dice2Value
            }
            if (positionThree.first < 2 || boardState[fromPosition].second == positionThree.second) {
                threePosition[2] = fromPosition+dice1Value+dice2Value
            }
        }
        else{
            if(fromPosition - dice1Value>0) {
                positionOne = boardState[fromPosition - dice1Value]
            }
            else{
                positionOne = boardState[fromPosition]
            }
            if(fromPosition - dice2Value>0) {
                positionTwo = boardState[fromPosition - dice2Value]
            }
            else {
                positionTwo = boardState[fromPosition]
            }
            if(fromPosition - dice1Value - dice2Value>0) {
                positionThree = boardState[fromPosition - dice1Value - dice2Value]
            }
            else {
                positionThree= boardState[fromPosition]
            }
            if (positionOne.first < 2 || boardState[fromPosition].second == positionOne.second) {
                threePosition[0] = fromPosition-dice1Value
            }
            if (positionTwo.first < 2 || boardState[fromPosition].second == positionTwo.second) {
                threePosition[1] = fromPosition-dice2Value
            }
            if (positionThree.first < 2 || boardState[fromPosition].second == positionThree.second) {
                threePosition[2] = fromPosition-dice1Value-dice2Value
            }
        }

        Log.d(TAG,"options 1: ${positionOne.toString()},${positionTwo.toString()},${positionThree.toString()}")


        Log.d(TAG,"options:${threePosition[0].toString()}, ${threePosition[1].toString()} , ${threePosition[2].toString()}")
        return threePosition
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = x
                lastTouchY = y

                // בדיקה אם לחצו על אזור הקוביות
                if (y >= 565 && y <= 800 && x >= 2400 && x < 2970) {
                    // רק אם אין מהלכים זמינים או לא הוטלו הקוביות בתור זה
                    if (availableMoves.isEmpty()) {
                        rollDice()
                    } else {
                        Toast.makeText(context, "יש להשלים את המהלכים הנוכחיים: ${availableMoves.joinToString(", ")}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // בחירת חייל
                    val clickedPosition = getPositionFromTouch(x, y)
                    handleTouch(clickedPosition)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                lastTouchX = x
                lastTouchY = y
            }
            MotionEvent.ACTION_UP -> {
                // רק אם יש חייל נבחר
                selectedCheckerPosition?.let { selectedPosition ->
                    val targetPosition = getPositionFromTouch(lastTouchX, lastTouchY)
                    if (targetPosition != selectedPosition) {  // וידוא שהמיקום השתנה
                        moveChecker(selectedPosition, targetPosition,movePosition(dice1Value,dice2Value,selectedPosition,boardState[selectedPosition].second))
                    }
                }
            }
        }
        return true
    }

    private fun handleTouch(position: Int) {
        val (count, isWhite) = boardState[position]

        // בדוק אם זה התור של השחקן ויש לו מהלכים זמינים
        if (count > 0 && isWhite == isWhiteTurn && availableMoves.isNotEmpty()) {
            selectedCheckerPosition = position

            // הצג את המהלכים האפשריים לחייל זה
            val validMoves = getValidMoves(position, isWhite)
            if (validMoves.isNotEmpty()) {
                Toast.makeText(context, "מהלכים אפשריים: ${validMoves.joinToString(", ")}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "אין מהלכים אפשריים לחייל זה", Toast.LENGTH_SHORT).show()
            }
        } else if (count > 0 && isWhite != isWhiteTurn) {
            // הודעה שזה לא התור של השחקן
            Toast.makeText(context, "לא התור שלך", Toast.LENGTH_SHORT).show()
        } else if (availableMoves.isEmpty() && initialDiceRoll) {
            // אין מהלכים זמינים אבל כבר הטלנו את הקוביות
            Toast.makeText(context, "אין לך מהלכים זמינים, לחץ על הקוביות להטלה מחדש", Toast.LENGTH_SHORT).show()
        } else if (availableMoves.isEmpty()) {
            // אין מהלכים זמינים כי טרם הטלנו את הקוביות
            Toast.makeText(context, "הטל את הקוביות תחילה", Toast.LENGTH_SHORT).show()
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

    private fun moveChecker(fromPosition: Int, toPosition: Int,options:Array<Int>) {
        Log.d(TAG,"to ${toPosition}}")
        if (options.contains(toPosition)) {
            val (count, isWhite) = boardState[fromPosition]
            if (count > 0) {
                // כל הקוד הקיים שלך להזזת החייל...

                // אחרי שההזזה הסתיימה בהצלחה, הוסף רק את זה:
                switchTurn()

                invalidate() // עדכון הציור
            }
            selectedCheckerPosition = null
        }

        if (options.contains(toPosition)){

            val (count, isWhite) = boardState[fromPosition]
            if (count > 0) {
                // הסר את החייל מהמיקום הנבחר
                //Toast.makeText(context,"${count}",Toast.LENGTH_SHORT).show()
                //Toast.makeText(context,"${boardState[fromPosition].first}",Toast.LENGTH_SHORT).show()
                // הוסף אותו למיקום החדש
                if (isWhite)
                {
                    if(boardState[toPosition].second!=isWhite){
                        if(boardState[toPosition].first<2) {
                            boardState[fromPosition] = Pair(count-1, isWhite)
                            if (boardState[toPosition].first==1) {
                                boardState[24] = Pair(boardState[24].first + 1, !isWhite)
                            }
                            boardState[toPosition ] = Pair(1, isWhite)
                        }
                        else{
                            Toast.makeText(context,"You cant go their you stupid little bit...",Toast.LENGTH_SHORT).show()
                        }
                    }
                    else{
                        boardState[fromPosition] = Pair(count-1, isWhite)

                        val newCount = boardState[toPosition].first + 1
                        boardState[toPosition] = Pair(newCount, isWhite)
                    }

                }
                else{
                    if(boardState[toPosition].second!=isWhite){
                        if(boardState[toPosition].first<2) {
                            boardState[fromPosition] = Pair(count-1, isWhite)
                            if (boardState[toPosition].first==1) {
                                boardState[25] = Pair(boardState[25].first + 1, !isWhite)
                            }
                            boardState[toPosition ] = Pair(1, isWhite)
                        }
                        else{
                            Toast.makeText(context,"You cant go their you stupid little bit...",Toast.LENGTH_SHORT).show()
                        }
                    }
                    else{
                        boardState[fromPosition] = Pair(count-1, isWhite)

                        val newCount = boardState[toPosition].first + 1
                        boardState[toPosition] = Pair(newCount, isWhite)
                    }

                }
                invalidate() // עדכון הציור
            }
            selectedCheckerPosition = null
        }
    }
    private fun showCurrentTurn() {
        val message = if (isWhiteTurn) "תור השחקן הלבן" else "תור השחקן השחור"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun switchTurn() {
        isWhiteTurn = !isWhiteTurn
        availableMoves.clear()  // נקה את המהלכים הזמינים
        showCurrentTurn()
    }

}