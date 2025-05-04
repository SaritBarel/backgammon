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
            Toast.makeText(context, "יצא דאבל! יש לך 4 מהלכים", Toast.LENGTH_SHORT).show()
        }

        initialDiceRoll = true
        // איפוס מספר המהלכים בתור
        currentTurnMoves = 0
        moveHistory.clear()
        
        loadDiceImages(context)

        // הצג את המהלכים הזמינים
        val movesMessage = "קוביות: ${dice1Value}, ${dice2Value}"
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
        color = Color.rgb(101, 67, 33) // צבע חום כהה יותר למסגרת
        style = Paint.Style.STROKE
        strokeWidth = 15f // הגדלת עובי המסגרת הפנימית
    }

    private val outerBorderPaint = Paint().apply {
        color = Color.rgb(59, 38, 19) // צבע חום כהה מאוד למסגרת החיצונית
        style = Paint.Style.STROKE
        strokeWidth = 25f // הגדלת עובי המסגרת החיצונית
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
    data class Move(val from: Int, val to: Int, val eatenChecker: Pair<Int, Boolean>?, val usedDice: Int)

    private val moveHistory = mutableListOf<Move>()
    private var waitForDoneButton = false
    private var currentTurnMoves = 0 // משתנה חדש למעקב אחר מספר המהלכים בתור הנוכחי

    // מערך המייצג את מצב הלוח - מספר החיילים בכל עמודה והצבע שלהם
    private val boardState = Array(29) { position ->
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
    private fun inHome(isWhite: Boolean): Boolean {
        // בדיקה שאין חיילים בבר (מחוץ למשחק)
        val barPosition = if (isWhite) 25 else 24
        if (boardState[barPosition].first > 0 && boardState[barPosition].second == isWhite) {
            return false
        }

        if (isWhite) {
            // בדיקה ששחקן לבן - כל החיילים שלו נמצאים ב-6 המשבצות האחרונות (18-23)
            for (i in 0..17) {
                if (boardState[i].first > 0 && boardState[i].second == isWhite) {
                    Log.d("Backgammon", "נמצא חייל לבן מחוץ לבית בעמדה $i")
                    return false
                }
            }
            Log.d("Backgammon", "כל החיילים הלבנים בבית")
            return true
        } else {
            // בדיקה ששחקן שחור - כל החיילים שלו נמצאים ב-6 המשבצות הראשונות (0-5)
            for (i in 6..23) {
                if (boardState[i].first > 0 && boardState[i].second == isWhite) {
                    Log.d("Backgammon", "נמצא חייל שחור מחוץ לבית בעמדה $i")
                    return false
                }
            }
            Log.d("Backgammon", "כל החיילים השחורים בבית")
            return true
        }
    }

    private fun getValidMoves(fromPosition: Int, isWhite: Boolean): List<Int> {
        val validMoves = mutableListOf<Int>()
        val canBearOff = inHome(isWhite)

        // בדיקה אם יש חיילים בבר
        val barPosition = if (isWhite) 25 else 24
        if (boardState[barPosition].first > 0 && boardState[barPosition].second == isWhite) {
            // אם יש חיילים בבר, אפשר להזיז רק מהבר
            if (fromPosition != barPosition) {
                Toast.makeText(context, "חייב להכניס קודם את החיילים מהבר", Toast.LENGTH_SHORT).show()
                return validMoves
            }
            
            // חישוב עמדות אפשריות לכניסה מהבר
            return getValidBarEntries(isWhite)
        }

        if (canBearOff) {
            // הוצאת חיילים לשחקן לבן
            if (isWhite && fromPosition >= 18) {
                for (moveValue in availableMoves) {
                    if (fromPosition + moveValue >= 24) {
                        validMoves.add(99)
                    } else if (isLastFarthestChecker(fromPosition, isWhite)) {
                        validMoves.add(99)
                    }
                }
            }
            // הוצאת חיילים לשחקן שחור
            else if (!isWhite && fromPosition <= 5) {
                for (moveValue in availableMoves) {
                    if (fromPosition - moveValue < 0) {
                        validMoves.add(99)
                    } else if (isLastFarthestChecker(fromPosition, isWhite)) {
                        validMoves.add(99)
                    }
                }
            }
        }

        // בדיקת מהלכים רגילים
        for (moveValue in availableMoves) {
            val targetPosition = if (isWhite) fromPosition + moveValue else fromPosition - moveValue
            if (targetPosition in 0..23) {
                val targetSpot = boardState[targetPosition]
                if (targetSpot.first <= 1 || targetSpot.second == isWhite) {
                    validMoves.add(targetPosition)
                }
            }
        }

        // בדיקת מהלך משולב של שתי הקוביות (אם הן שונות)
        if (dice1Value != dice2Value && availableMoves.contains(dice1Value) && availableMoves.contains(dice2Value)) {
            val combinedValue = dice1Value + dice2Value
            val targetPosition = if (isWhite) fromPosition + combinedValue else fromPosition - combinedValue
            
            if (targetPosition in 0..23) {
                val targetSpot = boardState[targetPosition]
                if (targetSpot.first <= 1 || targetSpot.second == isWhite) {
                    validMoves.add(targetPosition)
                }
            }
        }

        return validMoves
    }

    // פונקציה חדשה למציאת עמדות הכניסה האפשריות מהבר
    private fun getValidBarEntries(isWhite: Boolean): List<Int> {
        val validEntries = mutableListOf<Int>()
        
        // הקביעה של הרבע הראשון תלויה בצבע השחקן
        val entryPositions = if (isWhite) {
            // עבור שחקן לבן, הרבע הראשון הוא עמדות 0-5
            (0..5)
        } else {
            // עבור שחקן שחור, הרבע הראשון הוא עמדות 18-23
            (18..23)
        }
        
        // עבור כל ערך קובייה, בדוק אם העמדה המתאימה פנויה או יש בה רק חייל אחד של היריב
        for (diceValue in availableMoves) {
            val targetPosition = if (isWhite) {
                diceValue - 1  // עבור לבן, מתחילים מ-0 (עמדה 1 במשחק)
            } else {
                24 - diceValue  // עבור שחור, סופרים מהסוף
            }
            
            // ודא שהעמדה בתוך הרבע הראשון
            if (targetPosition in entryPositions) {
                val (count, spotColor) = boardState[targetPosition]
                
                // העמדה פנויה או יש בה רק חייל אחד של היריב
                if (count == 0 || (count == 1 && spotColor != isWhite)) {
                    validEntries.add(targetPosition)
                }
            }
        }
        
        // בדיקת מהלך משולב של שתי הקוביות גם בכניסה מהבר
        if (dice1Value != dice2Value && availableMoves.contains(dice1Value) && availableMoves.contains(dice2Value)) {
            val combinedValue = dice1Value + dice2Value
            val targetPosition = if (isWhite) {
                combinedValue - 1  // עבור לבן
            } else {
                24 - combinedValue  // עבור שחור
            }
            
            // ודא שהעמדה בתוך הרבע הראשון
            if (targetPosition in entryPositions) {
                val (count, spotColor) = boardState[targetPosition]
                
                // העמדה פנויה או יש בה רק חייל אחד של היריב
                if (count == 0 || (count == 1 && spotColor != isWhite)) {
                    validEntries.add(targetPosition)
                }
            }
        }
        
        // אם אין עמדות תקפות, הודע לשחקן
        if (validEntries.isEmpty()) {
            Toast.makeText(context, "אין אפשרויות כניסה מהבר, התור עובר", Toast.LENGTH_SHORT).show()
            // לוגיקה לסיום התור כשאין אפשרויות יכולה להיות כאן
        }
        
        return validEntries
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val padding = 30.0f // הגדלת השוליים ל-30 פיקסלים
        val drawable: Drawable = dice1
        val drawable2: Drawable = dice2

        // מיקום לציור (X, Y)
        val x = 2730f
        val y = 560f
        val x1 = 2500f
        val y1 = 560f

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

        // צייר את המסגרת הפנימית
        canvas.drawRect(padding, padding, width - padding, height - padding, borderPaint)
        
        // צייר את המסגרת החיצונית
        canvas.drawRect(padding - 10, padding - 10, width - padding + 10, height - padding + 10, outerBorderPaint)
        
        // צייר קו האמצע
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
        val barWidth = triangleWidth
        val barCenterX = width / 2

        for (i in 0..27) {  // הגדלתי את הטווח כדי לכלול גם את עמדות 26-27 (החיילים שיצאו)
            val (count, isWhite) = boardState[i]
            if (count > 0) {
                var x: Float
                
                if (i == 24 || i == 25) {
                    // עמדות הבר
                    x = barCenterX
                } else if (i < 12) {
                    x = width - padding - (i * triangleWidth + triangleWidth / 2)
                } else if (i < 24) {
                    x = padding + ((i - 12) * triangleWidth + triangleWidth / 2)
                } else {
                    // עמדות 26-27 (החיילים שיצאו)
                    x = if (i == 26) padding * 2 else width - padding * 2  // סידור בצדדים
                }

                for (j in 0 until count) {
                    var y: Float
                    
                    if (i == 24) {
                        // בר שחור - למטה
                        y = height - padding - (j + 1) * checkerRadius * 1.8f
                    } else if (i == 25) {
                        // בר לבן - למעלה
                        y = padding + (j + 1) * checkerRadius * 1.8f
                    } else if (i == 26 || i == 27) {
                        // חיילים שיצאו - בצד
                        y = height / 2 + (j - count/2) * checkerRadius * 1.5f
                    } else if (i < 12) {
                        y = height - padding - (j + 1) * checkerRadius * 1.8f
                    } else {
                        y = padding + (j + 1) * checkerRadius * 1.8f
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
                    // אם מחכים ללחיצה על DONE, לא לאפשר הטלת קוביות
                    if (waitForDoneButton) {
                        Toast.makeText(context, "יש ללחוץ על DONE לסיום התור הנוכחי", Toast.LENGTH_SHORT).show()
                        return true
                    }
                    
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
                    // בדוק אם השחקן מנסה לשים את החייל על המסגרת (מחוץ ללוח)
                    if (isOnBorder(lastTouchX, lastTouchY)) {
                        val isWhite = boardState[selectedPosition].second
                        // רישום מצב הלוח לדיבוג
                        logBoardState()
                        // בדוק אם השחקן רשאי להוציא חיילים (כל החיילים שלו בבית)
                        if (canBearOff(selectedPosition, isWhite)) {
                            // הפעל את הלוגיקה של הוצאת חייל
                            val validMoves = getValidMoves(selectedPosition, isWhite)
                            if (validMoves.contains(99)) {
                                // קוד 99 מסמל אפשרות להוציא חייל
                                moveChecker(selectedPosition, 99, validMoves)
                            } else {
                                Toast.makeText(context, "לא ניתן להוציא חייל זה", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "לא ניתן להוציא חיילים כל עוד לא כל החיילים שלך בבית", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val targetPosition = getPositionFromTouch(lastTouchX, lastTouchY)
                        if (targetPosition != selectedPosition) {  // וידוא שהמיקום השתנה
                            moveChecker(selectedPosition, targetPosition, getValidMoves(selectedPosition, boardState[selectedPosition].second))
                        }
                    }
                }
            }
        }
        return true
    }

    // פונקציה חדשה לבדיקה אם הנקודה נמצאת על המסגרת
    private fun isOnBorder(x: Float, y: Float): Boolean {
        val width = width.toFloat()
        val height = height.toFloat()
        val padding = 30.0f

        // בדיקה אם הנקודה נמצאת מחוץ לאזור המשחק אך בתוך המסך
        return (x < padding || x > width - padding || y < padding || y > height - padding) &&
               (x >= 0 && x <= width && y >= 0 && y <= height)
    }

    // פונקציה לבדיקה אם ניתן להוציא חייל - משופרת
    private fun canBearOff(position: Int, isWhite: Boolean): Boolean {
        // הוספת לוג לצורך דיבוג
        Log.d("Backgammon", "בדיקת הוצאת חייל: עמדה $position, צבע ${if (isWhite) "לבן" else "שחור"}")
        
        // בדיקה שהשחקן בבית
        val inHomeStatus = inHome(isWhite)
        Log.d("Backgammon", "סטטוס בבית: $inHomeStatus")
        if (!inHomeStatus) {
            Toast.makeText(context, "לא כל החיילים בבית", Toast.LENGTH_SHORT).show()
            return false
        }

        // בדיקה שהחייל נמצא במיקום מתאים להוצאה
        if (isWhite && position < 18) {
            Log.d("Backgammon", "חייל לבן לא במיקום מתאים להוצאה")
            return false
        }
        if (!isWhite && position > 5) {
            Log.d("Backgammon", "חייל שחור לא במיקום מתאים להוצאה")
            return false
        }

        // אין צורך לבדוק אם זה החייל הרחוק ביותר - נבדוק בזמן הוצאה אם יש
        // מספר מתאים בקוביות
        Log.d("Backgammon", "אפשר להוציא את החייל")
        return true
    }

    private fun handleTouch(position: Int) {
        // בדוק אם זה התור של השחקן ויש מהלכים זמינים
        val barPosition = if (isWhiteTurn) 25 else 24
        
        // בדוק אם יש חיילים בבר
        if (boardState[barPosition].first > 0 && boardState[barPosition].second == isWhiteTurn) {
            // אם יש חיילים בבר, חייבים לשחק אותם קודם
            if (position != barPosition) {
                Toast.makeText(context, "חייב להכניס קודם את החיילים מהבר", Toast.LENGTH_SHORT).show()
                return
            }
            selectedCheckerPosition = barPosition
            val validMoves = getValidMoves(barPosition, isWhiteTurn)
            if (validMoves.isEmpty()) {
                if (availableMoves.isNotEmpty()) {
                    // אין אפשרויות כניסה אך יש ערכי קוביות - עבור לשחקן הבא
                    Toast.makeText(context, "אין אפשרויות כניסה, התור עובר", Toast.LENGTH_SHORT).show()
                    waitForDoneButton = true
                }
            }
            return
        }
        
        val (count, isWhite) = boardState[position]

        // בדיקות רגילות
        if (count > 0 && isWhite == isWhiteTurn && availableMoves.isNotEmpty()) {
            selectedCheckerPosition = position

            // הצג את המהלכים האפשריים לחייל זה
            val validMoves = getValidMoves(position, isWhite)
            if (validMoves.isEmpty()) {
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
    
    // עדכון המציאת עמדה מנקודת מגע כדי לתמוך בבר
    private fun getPositionFromTouch(x: Float, y: Float): Int {
        // המרת קואורדינטות ל-position
        val triangleWidth = (width - 2 * 0.0f) / 12
        val boardHeight = height - 2 * 0.0f
        
        // בדיקה אם הנגיעה באזור הבר
        val centerX = width / 2f
        val barWidth = triangleWidth
        
        // אם הנגיעה באזור המרכזי של הלוח (הבר)
        if (x >= centerX - barWidth/2 && x <= centerX + barWidth/2) {
            return if (y > boardHeight / 2) {
                24  // בר של השחורים (למטה)
            } else {
                25  // בר של הלבנים (למעלה)
            }
        }
        
        val positionIndex = ((x - 0.0f) / triangleWidth).toInt()

        return if (y > boardHeight / 2) {
            11 - positionIndex
        } else {
            12 + positionIndex
        }
    }

    private fun moveChecker(fromPosition: Int, toPosition: Int, options: List<Int>) {
        val isWhite = boardState[fromPosition].second
        val isBearingOff = (options.contains(99) && toPosition == 99) || 
                           (options.contains(99) && ((isWhite && toPosition >= 24) || (!isWhite && toPosition < 0)))

        // בדיקה אם זה מהלך של כניסה מהבר
        val isBarEntry = (fromPosition == 25 && isWhite) || (fromPosition == 24 && !isWhite)

        if (options.contains(toPosition) || isBearingOff) {
            val (count, isWhite) = boardState[fromPosition]
            if (count > 0) {
                if (isBearingOff) {
                    // חישוב המרחק עבור הוצאת חייל
                    val moveDistance = if (isWhite) {
                        24 - fromPosition
                    } else {
                        fromPosition + 1
                    }

                    // בדיקה אם יש מספר מדויק בקוביות
                    val exactMove = availableMoves.find { it == moveDistance }
                    // בדיקה אם יש מספר גדול יותר בקוביות
                    val largerMove = availableMoves.filter { it > moveDistance }.minOrNull()

                    if (exactMove != null) {
                        // יש מספר מדויק - משתמשים בו
                        availableMoves.remove(exactMove)
                        Log.d("Backgammon", "משתמש במהלך מדויק $exactMove להוצאת חייל")
                        
                        // שמירת המהלך להיסטוריה
                        moveHistory.add(Move(fromPosition, 99, null, exactMove))
                        currentTurnMoves++
                    } else if (largerMove != null && isLastFarthestChecker(fromPosition, isWhite)) {
                        // אין מספר מדויק, אבל יש מספר גדול יותר, וזה החייל הרחוק ביותר
                        availableMoves.remove(largerMove)
                        Log.d("Backgammon", "משתמש במהלך גדול יותר $largerMove להוצאת חייל")
                        
                        // שמירת המהלך להיסטוריה
                        moveHistory.add(Move(fromPosition, 99, null, largerMove))
                        currentTurnMoves++
                    } else {
                        // אין אפשרות להוציא את החייל
                        Toast.makeText(context, "לא ניתן להוציא חייל זה - אין מספר מתאים בקוביות", Toast.LENGTH_SHORT).show()
                        Log.d("Backgammon", "לא ניתן להוציא את החייל - אין מספר מתאים בקוביות (בדיוק או גדול יותר)")
                        return
                    }

                    // הוצאת החייל
                    boardState[fromPosition] = Pair(count - 1, isWhite)

                    // הוספת חייל ל"חיילים שיצאו"
                    val bearOffPosition = if (isWhite) 26 else 27
                    boardState[bearOffPosition] = Pair(boardState[bearOffPosition].first + 1, isWhite)
                    Log.d("Backgammon", "חייל הוצא בהצלחה מעמדה $fromPosition")

                    // בדיקת ניצחון
                    if (isAllBornOff(isWhite)) {
                        gameOver(isWhite)
                    }
                } else {
                    // טיפול במהלך רגיל
                    val moveDistance = if (isBarEntry) {
                        // חישוב המרחק עבור כניסה מהבר
                        if (isWhite) {
                            toPosition + 1  // עבור לבן, העמדה 0 היא 1 במשחק
                        } else {
                            24 - toPosition  // עבור שחור, סופרים מהסוף
                        }
                    } else {
                        // חישוב רגיל למהלך
                        Math.abs(toPosition - fromPosition)
                    }
                    
                    var usedDiceValue = 0
                    var eatenChecker: Pair<Int, Boolean>? = null
                    
                    // מצא את ערך הקובייה המתאים למהלך
                    if (availableMoves.contains(moveDistance)) {
                        availableMoves.remove(moveDistance)
                        usedDiceValue = moveDistance
                    } else if (dice1Value == dice2Value) {
                        // במקרה של דאבל
                        val baseMove = dice1Value
                        val movesUsed = moveDistance / baseMove

                        if (moveDistance % baseMove == 0 && movesUsed <= availableMoves.size) {
                            // מסיר את מספר המהלכים שנוצלו
                            for (i in 0 until movesUsed) {
                                availableMoves.removeAt(0)
                                // שמירת המהלך להיסטוריה - בדאבל נשמור כל צעד בנפרד
                                if (i > 0) {
                                    moveHistory.add(Move(fromPosition, toPosition, null, baseMove))
                                    currentTurnMoves++
                                }
                            }
                            usedDiceValue = baseMove
                        } else {
                            Toast.makeText(context, "מהלך לא חוקי", Toast.LENGTH_SHORT).show()
                            return
                        }
                    } else if (moveDistance == dice1Value + dice2Value && availableMoves.contains(dice1Value) && availableMoves.contains(dice2Value)) {
                        // מהלך המשלב את שתי הקוביות יחד
                        availableMoves.remove(dice1Value)
                        availableMoves.remove(dice2Value)
                        usedDiceValue = dice1Value + dice2Value
                        
                        // במקרה של שימוש בשתי קוביות, נשמור שני מהלכים בהיסטוריה
                        moveHistory.add(Move(fromPosition, toPosition, null, dice1Value))
                        currentTurnMoves++
                    } else {
                        Toast.makeText(context, "מהלך לא חוקי", Toast.LENGTH_SHORT).show()
                        return
                    }

                    // מבצע את המהלך עצמו
                    if (toPosition >= 0 && toPosition <= 23) {
                        // בדיקה אם במקום היעד יש חייל בודד של היריב
                        if (boardState[toPosition].first == 1 && boardState[toPosition].second != isWhite) {
                            // זכור את החייל שנאכל
                            eatenChecker = Pair(toPosition, boardState[toPosition].second)
                            
                            // אכילת חייל - העברה לבר
                            val barPosition = if (boardState[toPosition].second) 25 else 24
                            boardState[barPosition] = Pair(boardState[barPosition].first + 1, boardState[toPosition].second)

                            // הורדת החייל מהמקור והצבתו ביעד
                            boardState[fromPosition] = Pair(count - 1, isWhite)
                            boardState[toPosition] = Pair(1, isWhite)
                        } else if (boardState[toPosition].first == 0 || boardState[toPosition].second == isWhite) {
                            // מהלך רגיל - הורדת החייל מהמקור והוספתו ליעד
                            boardState[fromPosition] = Pair(count - 1, isWhite)
                            boardState[toPosition] = Pair(boardState[toPosition].first + 1, isWhite)
                        } else {
                            // לא ניתן לבצע מהלך כי יש יותר מחייל אחד של היריב
                            Toast.makeText(context, "לא ניתן לבצע מהלך זה", Toast.LENGTH_SHORT).show()
                            return
                        }
                    }
                    
                    // הוסף את המהלך להיסטוריה
                    moveHistory.add(Move(fromPosition, toPosition, eatenChecker, usedDiceValue))
                    currentTurnMoves++
                }

                // בדיקה אם נגמרו כל המהלכים האפשריים
                if (availableMoves.isEmpty()) {
                    waitForDoneButton = true
                    Toast.makeText(context, "מהלכים הושלמו, לחץ על DONE לסיום התור", Toast.LENGTH_SHORT).show()
                } else if (dice1Value == dice2Value) {
                    // הצג כמה מהלכים נותרו במקרה של דאבל
                    Toast.makeText(context, "נותרו עוד ${availableMoves.size} מהלכים", Toast.LENGTH_SHORT).show()
                }

                invalidate()
            }
            selectedCheckerPosition = null
        } else {
            Toast.makeText(context, "מהלך לא חוקי", Toast.LENGTH_SHORT).show()
        }
    }
    // פונקציה לבדיקה אם כל החיילים יצאו
    private fun isAllBornOff(isWhite: Boolean): Boolean {
        // בדיקה שאין יותר חיילים על הלוח
        for (i in 0..23) {
            if (boardState[i].first > 0 && boardState[i].second == isWhite) {
                return false
            }
        }

        // בדיקה שאין חיילים בבר
        val barPosition = if (isWhite) 25 else 24
        if (boardState[barPosition].first > 0 && boardState[barPosition].second == isWhite) {
            return false
        }

        return true
    }

    // פונקציה לטיפול בסיום משחק
    private fun gameOver(winnerIsWhite: Boolean) {
        val message = if (winnerIsWhite) "השחקן הלבן ניצח!" else "השחקן השחור ניצח!"
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        // כאן אפשר להוסיף קוד נוסף שיקרה בסיום המשחק
    }    private fun showCurrentTurn() {
        val message = if (isWhiteTurn) "תור השחקן הלבן" else "תור השחקן השחור"
    }

    private fun switchTurn() {
        isWhiteTurn = !isWhiteTurn
        availableMoves.clear()  // נקה את המהלכים הזמינים
        moveHistory.clear()  // נקה את היסטוריית המהלכים של התור הקודם
        currentTurnMoves = 0 // איפוס מספר המהלכים בתור
        initialDiceRoll = false  // איפוס לתחילת תור חדש
        showCurrentTurn()
    }
    // פונקציה לבדיקה אם יש חיילים רחוקים יותר
    private fun hasCheckersFartherBack(position: Int, isWhite: Boolean): Boolean {
        if (isWhite) {
            // עבור שחקן לבן, בדוק אם יש חיילים לפני העמדה הנוכחית
            for (i in 0 until position) {
                if (boardState[i].first > 0 && boardState[i].second == isWhite) {
                    return true
                }
            }
        } else {
            // עבור שחקן שחור, בדוק אם יש חיילים אחרי העמדה הנוכחית
            for (i in position + 1..23) {
                if (boardState[i].first > 0 && boardState[i].second == isWhite) {
                    return true
                }
            }
        }
        return false
    }
    private fun isLastFarthestChecker(position: Int, isWhite: Boolean): Boolean {
        Log.d("Backgammon", "בודק אם חייל בעמדה $position הוא הרחוק ביותר. צבע: ${if (isWhite) "לבן" else "שחור"}")
        
        if (isWhite) {
            // עבור שחקן לבן, בדוק רק אם יש חיילים לבנים במיקומים פנימיים יותר (0-17)
            // כשנמצאים בבית (עמדות 18-23), בדיקה אחרת
            if (position >= 18) {
                // בודקים אם יש חיילים לבנים בעמדות נמוכות יותר מהעמדה הנוכחית
                for (i in 18 until position) {
                    if (boardState[i].first > 0 && boardState[i].second == isWhite) {
                        Log.d("Backgammon", "נמצא חייל לבן בעמדה $i שרחוק יותר מעמדה $position")
                        return false
                    }
                }
                // אם הגענו לכאן, זה אומר שאין חיילים בעמדות נמוכות יותר בתוך הבית
                Log.d("Backgammon", "עמדה $position היא העמדה הנמוכה ביותר עם חיילים לבנים בבית")
                return true
            } else {
                // מחוץ לבית - לא ניתן להוציא
                return false
            }
        } else {
            // עבור שחקן שחור, בדוק רק אם יש חיילים שחורים במיקומים פנימיים יותר (6-23)
            // כשנמצאים בבית (עמדות 0-5), בדיקה אחרת
            if (position <= 5) {
                // בודקים אם יש חיילים שחורים בעמדות גבוהות יותר מהעמדה הנוכחית
                for (i in position + 1..5) {
                    if (boardState[i].first > 0 && boardState[i].second == isWhite) {
                        Log.d("Backgammon", "נמצא חייל שחור בעמדה $i שרחוק יותר מעמדה $position")
                        return false
                    }
                }
                // אם הגענו לכאן, זה אומר שאין חיילים בעמדות גבוהות יותר בתוך הבית
                Log.d("Backgammon", "עמדה $position היא העמדה הגבוהה ביותר עם חיילים שחורים בבית")
                return true
            } else {
                // מחוץ לבית - לא ניתן להוציא
                return false
            }
        }
    }

    private fun logBoardState() {
        Log.d("Backgammon", "מצב הלוח:")
        for (i in 0..27) {
            if (boardState[i].first > 0) {
                Log.d("Backgammon", "עמדה $i: ${boardState[i].first} חיילים, צבע: ${if (boardState[i].second) "לבן" else "שחור"}")
            }
        }
    }

    // פונקציה חדשה להחזרת מהלך אחורה
    fun undoLastMove() {
        if (moveHistory.isEmpty()) {
            Toast.makeText(context, "אין מהלכים לביטול", Toast.LENGTH_SHORT).show()
            return
        }

        // בדיקה שלא ניסו לבטל יותר מהמהלכים שבוצעו בתור הנוכחי
        if (currentTurnMoves <= 0) {
            Toast.makeText(context, "לא ניתן לבטל יותר מהלכים בתור זה", Toast.LENGTH_SHORT).show()
            return
        }

        // קח את המהלך האחרון
        val lastMove = moveHistory.removeAt(moveHistory.size - 1)
        Log.d("Backgammon", "מבטל מהלך: מעמדה ${lastMove.from} לעמדה ${lastMove.to}")

        // החזר את החייל למצב הקודם
        val isWhite = if (lastMove.to == 99) boardState[lastMove.from].second else boardState[lastMove.to].second

        // פחות חייל במיקום החדש
        if (lastMove.to == 99) { // אם זו הייתה הוצאת חייל
            val bearOffPosition = if (isWhite) 26 else 27
            boardState[bearOffPosition] = Pair(boardState[bearOffPosition].first - 1, isWhite)
        } else {
            boardState[lastMove.to] = Pair(boardState[lastMove.to].first - 1, isWhite)
        }
        
        // יותר חייל במיקום המקורי
        boardState[lastMove.from] = Pair(boardState[lastMove.from].first + 1, isWhite)
        
        // אם זה היה מהלך שאכל חייל
        lastMove.eatenChecker?.let { (position, color) ->
            val barPosition = if (color) 25 else 24
            // פחות חייל בבר
            boardState[barPosition] = Pair(boardState[barPosition].first - 1, color)
            // יותר חייל במיקום המקורי
            boardState[position] = Pair(boardState[position].first + 1, color)
        }

        // החזר את ערך הקובייה ששימשה למהלך
        availableMoves.add(lastMove.usedDice)
        
        // הפחת את מספר המהלכים בתור הנוכחי
        currentTurnMoves--
        
        // הצג כמה מהלכים עוד ניתן לבטל
        if (currentTurnMoves > 0) {
            Toast.makeText(context, "ניתן לבטל עוד $currentTurnMoves מהלכים", Toast.LENGTH_SHORT).show()
        }
        
        // אם כבר הסתיים התור, לא צריך לחכות לDONE
        if (waitForDoneButton && availableMoves.size > 0) {
            waitForDoneButton = false
        }

        invalidate()
    }

    // פונקציה לסיום התור
    fun finishTurn() {
        // בדיקה אם יש חיילים בבר ואין להם מהלכים אפשריים
        val barPosition = if (isWhiteTurn) 25 else 24
        if (boardState[barPosition].first > 0 && boardState[barPosition].second == isWhiteTurn) {
            val validMoves = getValidBarEntries(isWhiteTurn)
            if (validMoves.isEmpty() && waitForDoneButton) {
                // אין אפשרויות כניסה מהבר - מעבר לשחקן הבא מאושר
                Log.d("Backgammon", "אין אפשרות להכניס חיילים מהבר - סיום תור")
                switchTurn()
                waitForDoneButton = false
                invalidate()
                return
            } else if (validMoves.isNotEmpty()) {
                Toast.makeText(context, "חייב להכניס את החיילים מהבר קודם", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (!waitForDoneButton) {
            if (availableMoves.isNotEmpty()) {
                val message = if (dice1Value == dice2Value) {
                    "יש לך עוד ${availableMoves.size} מהלכים להשלים (דאבל)"
                } else {
                    "יש לך עוד מהלכים להשלים: ${availableMoves.joinToString(", ")}"
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                return
            } else if (!initialDiceRoll) {
                Toast.makeText(context, "יש להטיל קוביות תחילה", Toast.LENGTH_SHORT).show()
                return
            }
        }

        Log.d("Backgammon", "סיום תור של שחקן ${if (isWhiteTurn) "לבן" else "שחור"}")
        switchTurn()
        waitForDoneButton = false
        invalidate()
    }
}