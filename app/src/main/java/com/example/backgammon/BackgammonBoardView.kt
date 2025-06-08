package com.example.backgammon

import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import kotlin.math.log
import android.os.Handler
import android.os.Looper

class BackgammonBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0

) : View(context, attrs, defStyleAttr) {
    // ─────────────  צעד 2: פונקציה bs()  ─────────────
    /** מחזירה Pair(count, isWhite) או null אם האינדקס מחוץ לטווח 0-28 */
    private fun bs(i: Int): Pair<Int, Boolean>? =
        boardState.getOrNull(i)

    // משתנים עבור התמונות של הקוביות
    private  var dice1: Drawable = ContextCompat.getDrawable(context, R.drawable.dice_1)!!
    private  var dice2: Drawable = ContextCompat.getDrawable(context, R.drawable.dice_1)!!
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isWhiteTurn = true // התור מתחיל עם השחקן הלבן
    // משתנה לשם השחקן הנוכחי
    private var currentPlayerName: String = ""
    // ערכים אקראיים עבור הקוביות
    private var dice1Value = 0
    private var dice2Value = 0
    // משתנים למעקב אחרי המהלכים הזמינים
    private var availableMoves = mutableListOf<Int>()
    private var initialDiceRoll = false // האם הקוביות הוטלו בתחילת התור
    // מערך שמכיל את האזורים הגרפיים של כל משולש
    private val triangleAreas = Array(24) { RectF() }
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
        if (availableMoves.isNotEmpty()) {
            Toast.makeText(context, "יש להשלים קודם את המהלכים הנוכחיים", Toast.LENGTH_SHORT).show()
            return
        }

        dice1Value = (1..6).random()
        dice2Value = (1..6).random()
        initialDiceRoll = true
        waitForDoneButton = false

        // יצירת רשימת מהלכים זמינים
        if (dice1Value == dice2Value) {
            // דאבל - 4 מהלכים זהים
            availableMoves = mutableListOf(dice1Value, dice1Value, dice1Value, dice1Value)
            Toast.makeText(context, "דאבל! יש לך 4 מהלכים של $dice1Value", Toast.LENGTH_SHORT).show()
        } else {
            // קוביות שונות - 2 מהלכים
            availableMoves = mutableListOf(dice1Value, dice2Value)
        }

        // לוג מפורט להבנת מצב המשחק
        Log.d("Backgammon", "הטלת קוביות: $dice1Value, $dice2Value")
        Log.d("Backgammon", "מהלכים זמינים: $availableMoves")
        Log.d("Backgammon", "תור השחקן: ${if (isWhiteTurn) "לבן" else "שחור"}")

        // בדיקה אוטומטית לסיום תור אם אין מהלכים כלל
        Handler(Looper.getMainLooper()).postDelayed({
            val barPosition = if (isWhiteTurn) 25 else 24
            val hasBarPieces = boardState[barPosition].first > 0 && boardState[barPosition].second == isWhiteTurn
            
            if (hasBarPieces) {
                // יש חיילים בבר - בדוק אם יש אפשרויות כניסה
                val validBarMoves = getValidBarEntriesForDice(isWhiteTurn, availableMoves)
                if (validBarMoves.isEmpty()) {
                    Log.d("Backgammon", "אין אפשרויות כניסה מהבר - מעבר אוטומטי לשחקן הבא")
                    Toast.makeText(context, "אין אפשרויות כניסה מהבר - מעבר לשחקן הבא", Toast.LENGTH_LONG).show()
                    Handler(Looper.getMainLooper()).postDelayed({
                        switchTurn()
                        updateFirebase()
                        invalidate()
                    }, 2000) // המתנה של שנייתיים לפני מעבר תור
                    return@postDelayed
                }
            } else {
                // אין חיילים בבר - בדוק מהלכים רגילים
                val hasAnyValidMoves = playerHasAnyValidMoves(isWhiteTurn, availableMoves)
                if (!hasAnyValidMoves) {
                    Log.d("Backgammon", "אין מהלכים חוקיים זמינים - מעבר אוטומטי לשחקן הבא")
                    Toast.makeText(context, "אין מהלכים חוקיים זמינים - מעבר לשחקן הבא", Toast.LENGTH_LONG).show()
                    Handler(Looper.getMainLooper()).postDelayed({
                        switchTurn()
                        updateFirebase()
                        invalidate()
                    }, 2000) // המתנה של שנייתיים לפני מעבר תור
                    return@postDelayed
                }
            }
        }, 500) // המתנה קצרה לאחר הטלת הקוביות

        loadDiceImages(context)
        invalidate()
        updateFirebase()
    }

    // פונקציה מחודשת לבדיקה אוטומטית אם צריך לדלג על התור
    private fun checkForAutoSkipTurn() {
        val barPosition = if (isWhiteTurn) 25 else 24
        
        Log.d("AutoSkipTurn", "בדיקת דילוג אוטומטי: שחקן ${if (isWhiteTurn) "לבן" else "שחור"}")
        Log.d("AutoSkipTurn", "חיילים בבר: ${boardState[barPosition].first}, צבע: ${boardState[barPosition].second}")
        Log.d("AutoSkipTurn", "קוביות זמינות: $availableMoves")
        
        // בדוק אם השחקן הנוכחי יש לו חיילים בבר
        if (boardState[barPosition].first > 0 && boardState[barPosition].second == isWhiteTurn) {
            Log.d("AutoSkipTurn", "שחקן בבר - בודק אפשרויות כניסה")
            
            // בדוק אם יש אפשרויות כניסה עם הקוביות הנוכחיות
            val validBarEntries = getValidBarEntriesForDice(isWhiteTurn, availableMoves)
            
            if (validBarEntries.isEmpty()) {
                // אין אפשרויות כניסה - דלג על התור אוטומטית
                val playerName = if (isWhiteTurn) "הלבן" else "השחור"
                Toast.makeText(context, "שחקן $playerName בבר ואין אפשרויות כניסה. התור עובר אוטומטית.", Toast.LENGTH_LONG).show()
                
                // דחיית המעבר תור קצת כדי שהשחקן יראה את ההודעה
                postDelayed({
                    switchTurn()
                    updateFirebase()
                    invalidate()
                }, 2500) // דחיית 2.5 שניות
                return
            } else {
                Log.d("AutoSkipTurn", "יש אפשרויות כניסה מהבר: $validBarEntries")
                return
            }
        }
        
        // אם אין חיילים בבר, בדוק אם יש בכלל מהלכים אפשריים
        val hasAnyValidMoves = playerHasAnyValidMoves(isWhiteTurn, availableMoves)
        
        if (!hasAnyValidMoves) {
            // אין מהלכים אפשריים כלל - דלג על התור אוטומטית
            val playerName = if (isWhiteTurn) "הלבן" else "השחור"
            val diceText = if (dice1Value == dice2Value) {
                "דאבל ${dice1Value}"
            } else {
                "${dice1Value}, ${dice2Value}"
            }
            Toast.makeText(context, "שחקן $playerName אין לו מהלכים אפשריים עם $diceText. התור עובר אוטומטית.", Toast.LENGTH_LONG).show()
            
            // דחיית המעבר תור קצת כדי שהשחקן יראה את ההודעה
            postDelayed({
                switchTurn()
                updateFirebase()
                invalidate()
            }, 2500) // דחיית 2.5 שניות
        } else {
            Log.d("AutoSkipTurn", "שחקן יכול לבצע מהלכים")
        }
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
    data class Move(val from: Int, val to: Int, val eatenChecker: Pair<Int, Boolean>?, val usedDiceValues: List<Int>)

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
            24 -> Pair(0, false)   // בר של השחור (תמיד שחור)
            25 -> Pair(0, true)    // בר של הלבן (תמיד לבן)
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
                if (boardState[i].first > 0 && !boardState[i].second) {
                    Log.d("Backgammon", "נמצא חייל שחור מחוץ לבית בעמדה $i")
                    return false
                }
            }
            Log.d("Backgammon", "כל החיילים השחורים בבית")
            return true
        }
    }

    // פונקציית עזר לחישוב מהלכים חוקיים עבור קובייה בודדת
    private fun getValidSingleDiceMoves(fromPosition: Int, isWhite: Boolean, diceValue: Int, canBearOffGlobal: Boolean): List<Int> {
        val singleValidMoves = mutableListOf<Int>()
        val targetPosition = if (isWhite) fromPosition + diceValue else fromPosition - diceValue

        if (targetPosition in 0..23) { // מהלך רגיל על הלוח
            val targetSpot = boardState[targetPosition]
            if (targetSpot.first <= 1 || targetSpot.second == isWhite) {
                singleValidMoves.add(targetPosition)
            }
        } else if (canBearOffGlobal) { // פוטנציאל להוצאת כלי
            // האם היעד מגיע בדיוק לקצה הלוח או עובר אותו?
            val reachesOrPassesEnd = if (isWhite) (fromPosition + diceValue >= 24) else (fromPosition - diceValue < 0)
            if (reachesOrPassesEnd) {
                // אם החייל הוא הרחוק ביותר, או שהקובייה מגיעה בדיוק ליציאה או מעבר לה
                 if (isLastFarthestChecker(fromPosition, isWhite) ||
                    (isWhite && fromPosition + diceValue >= 24 && fromPosition < 24) || // חייב להיות בתוך הבית כדי להשתמש בקוביה גדולה יותר מהמרחק המדויק
                    (!isWhite && fromPosition - diceValue < 0 && fromPosition >= 0) ) {
                    
                    // וודא שהחייל נמצא באזור הבית שלו
                    val inPlayerHome = if (isWhite) (fromPosition >= 18 && fromPosition <= 23) else (fromPosition >= 0 && fromPosition <= 5)
                    if (inPlayerHome) {
                         singleValidMoves.add(99) // 99 מסמל הוצאת כלי
                    }
                }
            }
        }
        return singleValidMoves
    }

    private fun getValidDoubleMoves(fromPosition: Int, isWhite: Boolean, d: Int, numDiceAvailable: Int, canBearOffGlobal: Boolean): List<Int> {
        val validMoves = mutableListOf<Int>()

        for (numDiceInChain in 1..numDiceAvailable) {
            var pathIsValidSoFar = true

            // Check intermediate steps for chains longer than 1d
            for (stepNum in 1 until numDiceInChain) { // stepNum = 1, 2, ... (numDiceInChain - 1)
                val intermediateLandingPos = if (isWhite) fromPosition + (stepNum * d) else fromPosition - (stepNum * d)
                if (!isValidIntermediateStep(intermediateLandingPos, isWhite)) {
                    pathIsValidSoFar = false
                    break // This chain is broken at this intermediate step
                }
            }

            if (!pathIsValidSoFar) {
                // If an intermediate step for this chain length failed, don't check its final landing.
                continue // Try the next numDiceInChain value
            }

            // If all intermediate steps (if any) were valid, now check the final landing spot of the full chain
            val finalTargetPosition = if (isWhite) fromPosition + (numDiceInChain * d) else fromPosition - (numDiceInChain * d)

            if (finalTargetPosition in 0..23) { // Lands on the board
                val targetSpot = boardState[finalTargetPosition]
                if (targetSpot.first <= 1 || targetSpot.second == isWhite) {
                    validMoves.add(finalTargetPosition)
                }
            } else if (canBearOffGlobal) { // Potential to bear off
                val inPlayerHomeForThisChecker = if (isWhite) (fromPosition in 18..23) else (fromPosition in 0..5)
                
                if (inPlayerHomeForThisChecker) { 
                    val isExactBearOff = (isWhite && finalTargetPosition == 24) || (!isWhite && finalTargetPosition == -1)
                    val isOverBearOff = (isWhite && finalTargetPosition > 24) || (!isWhite && finalTargetPosition < -1)

                    if (isExactBearOff) {
                        validMoves.add(99)
                    } else if (isOverBearOff) {
                        if (isLastFarthestChecker(fromPosition, isWhite)) {
                            validMoves.add(99)
                        }
                    }
                }
            }
        }
        return validMoves.distinct().toMutableList()
    }

    private fun getValidMovesForDice(fromPosition: Int, isWhite: Boolean, diceToConsider: List<Int>): List<Int> {
        val validMovesOutput = mutableListOf<Int>()
        val canExecuteBearOff = inHome(isWhite)

        // 1. Handle bar entries first - this is a special case for 'fromPosition'
        val barPositionCurrentPlayer = if (isWhite) 25 else 24
        if (boardState[barPositionCurrentPlayer].first > 0 && boardState[barPositionCurrentPlayer].second == isWhite) {
            if (fromPosition != barPositionCurrentPlayer) {
                // Trying to move a checker on board while having checkers on bar
                Toast.makeText(context, "חייב להכניס קודם את החיילים מהבר", Toast.LENGTH_SHORT).show()
                return mutableListOf()
            }
            // If fromPosition IS the bar, delegate to getValidBarEntriesForDice
            return getValidBarEntriesForDice(isWhite, diceToConsider)
        }

        // 2. If no dice have been rolled (initial state) or no dice left to consider
        if (dice1Value == 0 || diceToConsider.isEmpty()) {
            return mutableListOf()
        }

        // 3. Handle Doubles
        if (dice1Value == dice2Value) {
            val d = dice1Value
            val countOfDoubleDiceStillAvailable = diceToConsider.count { it == d }
            if (countOfDoubleDiceStillAvailable > 0) {
                 validMovesOutput.addAll(getValidDoubleMoves(fromPosition, isWhite, d, countOfDoubleDiceStillAvailable, canExecuteBearOff))
            }
        } else { 
            // 4. Handle Non-Doubles
            // 4a. Moves with dice1Value alone (if available in diceToConsider)
            if (diceToConsider.contains(dice1Value)) {
                validMovesOutput.addAll(getValidSingleDiceMoves(fromPosition, isWhite, dice1Value, canExecuteBearOff))
            }
            // 4b. Moves with dice2Value alone (if available in diceToConsider)
            if (diceToConsider.contains(dice2Value)) {
                validMovesOutput.addAll(getValidSingleDiceMoves(fromPosition, isWhite, dice2Value, canExecuteBearOff))
            }

            // 4c. Combined move (d1 then d2, or d2 then d1) - if both dice are available in diceToConsider
            if (diceToConsider.contains(dice1Value) && diceToConsider.contains(dice2Value)) {
                val d1 = dice1Value
                val d2 = dice2Value

                // Path 1: fromPosition -> use d1 -> use d2
                val intermediatePos1 = if (isWhite) fromPosition + d1 else fromPosition - d1
                if (isValidIntermediateStep(intermediatePos1, isWhite)) {
                    validMovesOutput.addAll(getValidSingleDiceMoves(intermediatePos1, isWhite, d2, canExecuteBearOff))
                }

                // Path 2: fromPosition -> use d2 -> use d1
                val intermediatePos2 = if (isWhite) fromPosition + d2 else fromPosition - d2
                if (isValidIntermediateStep(intermediatePos2, isWhite)) {
                    validMovesOutput.addAll(getValidSingleDiceMoves(intermediatePos2, isWhite, d1, canExecuteBearOff))
                }
            }
        }
        
        return validMovesOutput.distinct().toMutableList()
    }

    // פונקציית עזר לבדיקת חוקיות משבצת ביניים
    private fun isValidIntermediateStep(position: Int, isWhite: Boolean): Boolean {
        if (position < 0 || position > 23) return false // מחוץ לגבולות הלוח
        val targetSpot = boardState[position]
        // משבצת ביניים יכולה להיות פנויה, או של השחקן, או עם חייל בודד של היריב (ש"נדרוך" עליו בדרך)
        return targetSpot.first == 0 || targetSpot.second == isWhite || targetSpot.first == 1
    }

    // פונקציה למציאת עמדות הכניסה האפשריות מהבר
    private fun getValidBarEntriesForDice(isWhite: Boolean, diceToConsider: List<Int>): List<Int> {
        val validEntries = mutableListOf<Int>()
        Log.d("BarEntries", "בדיקת כניסות מהבר: שחקן ${if (isWhite) "לבן" else "שחור"}, קוביות: $diceToConsider")
        
        if (diceToConsider.isEmpty()) {
            Log.d("BarEntries", "אין קוביות לבדיקה")
            return validEntries
        }

        val barPosition = if (isWhite) 25 else 24
        Log.d("BarEntries", "מיקום בר: $barPosition, חיילים בבר: ${boardState[barPosition].first}, צבע: ${boardState[barPosition].second}")
        
        if (boardState[barPosition].first == 0 || boardState[barPosition].second != isWhite) {
            Log.d("BarEntries", "אין חיילים בבר לשחקן הנוכחי")
            return validEntries
        }

        val uniqueDiceValues = diceToConsider.distinct()
        Log.d("BarEntries", "ערכי קוביות ייחודיים: $uniqueDiceValues")

        for (diceValue in uniqueDiceValues) {
            val entryPosition = if (isWhite) 24 - diceValue else diceValue - 1
            Log.d("BarEntries", "בודק קובייה $diceValue, מיקום כניסה: $entryPosition")

            if (entryPosition in 0..23) {
                val targetSpot = boardState[entryPosition]
                Log.d("BarEntries", "מיקום $entryPosition: ${targetSpot.first} חיילים, צבע: ${targetSpot.second}")
                
                if (targetSpot.first <= 1 || targetSpot.second == isWhite) {
                    validEntries.add(entryPosition)
                    Log.d("BarEntries", "כניסה חוקית למיקום $entryPosition")
                } else {
                    Log.d("BarEntries", "כניסה לא חוקית למיקום $entryPosition - יותר מחייל אחד של היריב")
                }
            } else {
                Log.d("BarEntries", "מיקום כניסה $entryPosition מחוץ לטווח")
            }
        }
        
        Log.d("BarEntries", "כניסות חוקיות נמצאו: $validEntries")
        return validEntries.distinct().toMutableList()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val outerPadding = 120.0f // מסגרת חיצונית עבה יותר

        // הקטנת הקוביות
        val diceScale = 0.7f // מקדם הקטנה לקוביות
        val diceWidth = dice1.intrinsicWidth * diceScale
        val diceHeight = dice1.intrinsicHeight * diceScale

        // מיקום הקוביות במסגרת העליונה
        val gapBetweenDice = 10f
        val totalDiceWidth = diceWidth * 2 + gapBetweenDice
        val diceStartX = width / 2 - totalDiceWidth / 2

        val dice1X = diceStartX
        val dice2X = diceStartX + diceWidth + gapBetweenDice
        val diceY = outerPadding / 2 - diceHeight / 2 // מיקום הקוביות במרכז המסגרת העליונה

        // צייר את המסגרת החיצונית העבה
        outerBorderPaint.apply {
            color = Color.rgb(59, 38, 19) // צבע חום כהה מאוד למסגרת החיצונית
            style = Paint.Style.FILL
            strokeWidth = 0f
        }
        canvas.drawRect(0f, 0f, width, height, outerBorderPaint)

        // צייר את הרקע של הלוח הפנימי
        canvas.drawRect(outerPadding, outerPadding, width - outerPadding, height - outerPadding, boardPaint)

        val boardWidth = width - (outerPadding * 2)
        val boardHeight = height - (outerPadding * 2)
        val triangleWidth = boardWidth / 12 // 6 משולשים בכל חצי לוח
        val triangleHeight = (boardHeight - boardHeight * 0.15f) / 2 // גובה המשולש עם מרווח במרכז

        // שמירה של האזורים הגרפיים של המשולשים
        // תחתון (0-11)
        for (i in 0..11) {
            val startX = outerPadding + (i * triangleWidth)
            val endX = startX + triangleWidth
            val topY = height - outerPadding - triangleHeight
            val bottomY = height - outerPadding
            triangleAreas[11 - i].set(startX, topY, endX, bottomY)
        }
        // עליון (12-23)
        for (i in 0..11) {
            val startX = outerPadding + (i * triangleWidth)
            val endX = startX + triangleWidth
            val topY = outerPadding
            val bottomY = outerPadding + triangleHeight
            triangleAreas[12 + i].set(startX, topY, endX, bottomY)
        }

        // צייר משולשים בחלק התחתון
        for (i in 0..11) {
            trianglePath.reset()
            trianglePaint.color = if (i % 2 == 0) Color.RED else Color.BLACK

            val startX = outerPadding + (i * triangleWidth)
            trianglePath.moveTo(startX, height - outerPadding)
            trianglePath.lineTo(startX + (triangleWidth / 2), height - outerPadding - triangleHeight)
            trianglePath.lineTo(startX + triangleWidth, height - outerPadding)
            trianglePath.close()
            canvas.drawPath(trianglePath, trianglePaint)
        }

        // צייר משולשים בחלק העליון
        for (i in 0..11) {
            trianglePath.reset()
            trianglePaint.color = if (i % 2 == 0) Color.BLACK else Color.RED

            val startX = outerPadding + (i * triangleWidth)
            trianglePath.moveTo(startX, outerPadding)
            trianglePath.lineTo(startX + (triangleWidth / 2), outerPadding + triangleHeight)
            trianglePath.lineTo(startX + triangleWidth, outerPadding)
            trianglePath.close()
            canvas.drawPath(trianglePath, trianglePaint)
        }

        // צייר את המסגרת הפנימית
        borderPaint.apply {
            color = Color.rgb(101, 67, 33) // צבע חום כהה יותר למסגרת הפנימית
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        canvas.drawRect(outerPadding, outerPadding, width - outerPadding, height - outerPadding, borderPaint)

        // צייר קו האמצע
        canvas.drawLine(width / 2, outerPadding, width / 2, height - outerPadding, borderPaint)

        // צייר את החיילים
        drawCheckers(canvas, width, height, outerPadding, triangleWidth)

        // צייר את הקוביות
        dice1.setBounds(
            dice1X.toInt(),
            diceY.toInt(),
            (dice1X + diceWidth).toInt(),
            (diceY + diceHeight).toInt()
        )
        dice1.draw(canvas)

        dice2.setBounds(
            dice2X.toInt(),
            diceY.toInt(),
            (dice2X + diceWidth).toInt(),
            (diceY + diceHeight).toInt()
        )
        dice2.draw(canvas)
    }


    private fun drawCheckers(canvas: Canvas, width: Float, height: Float, padding: Float, triangleWidth: Float) {
        val checkerRadius = triangleWidth * 0.2f
        val barWhite = 25
        val barBlack = 24
        val bearOffWhite = 26
        val bearOffBlack = 27
        val barAreaTop = padding + checkerRadius
        val barAreaBottom = height - padding - checkerRadius
        val barAreaHeight = barAreaBottom - barAreaTop
        for (i in 0..27) {
            val (count, isWhite) = boardState[i]
            if (count > 0) {
                var x: Float
                var y: Float
                if (i == barBlack) {
                    // בר שחור - עמודה אנכית בצד שמאל באזור החום
                    x = padding * 0.6f
                    val spacing = if (count > 1) barAreaHeight / (count) else 0f
                    for (j in 0 until count) {
                        y = barAreaTop + j * spacing
                        checkerPaint.color = Color.BLACK
                        canvas.drawCircle(x, y, checkerRadius, checkerPaint)
                        canvas.drawCircle(x, y, checkerRadius, checkerBorderPaint)
                        checkerPaint.color = Color.DKGRAY
                        canvas.drawCircle(x, y - checkerRadius * 0.1f, checkerRadius * 0.85f, checkerPaint)
                    }
                    continue
                } else if (i == barWhite) {
                    // בר לבן - עמודה אנכית בצד ימין באזור החום
                    x = width - padding * 0.6f
                    val spacing = if (count > 1) barAreaHeight / (count) else 0f
                    for (j in 0 until count) {
                        y = barAreaTop + j * spacing
                        checkerPaint.color = Color.WHITE
                        canvas.drawCircle(x, y, checkerRadius, checkerPaint)
                        canvas.drawCircle(x, y, checkerRadius, checkerBorderPaint)
                        checkerPaint.color = Color.LTGRAY
                        canvas.drawCircle(x, y - checkerRadius * 0.1f, checkerRadius * 0.85f, checkerPaint)
                    }
                    continue
                } else if (i == bearOffWhite) {
                    // חיילים לבנים שיצאו - בצד ימין
                    x = width - padding / 2
                    for (j in 0 until count) {
                        y = height / 2 + (j - count/2) * checkerRadius * 1.2f
                        checkerPaint.color = Color.WHITE
                        canvas.drawCircle(x, y, checkerRadius, checkerPaint)
                        canvas.drawCircle(x, y, checkerRadius, checkerBorderPaint)
                        checkerPaint.color = Color.LTGRAY
                        canvas.drawCircle(x, y - checkerRadius * 0.1f, checkerRadius * 0.85f, checkerPaint)
                    }
                    continue
                } else if (i == bearOffBlack) {
                    // חיילים שחורים שיצאו - בצד שמאל
                    x = padding / 2
                    for (j in 0 until count) {
                        y = height / 2 + (j - count/2) * checkerRadius * 1.2f
                        checkerPaint.color = Color.BLACK
                        canvas.drawCircle(x, y, checkerRadius, checkerPaint)
                        canvas.drawCircle(x, y, checkerRadius, checkerBorderPaint)
                        checkerPaint.color = Color.DKGRAY
                        canvas.drawCircle(x, y - checkerRadius * 0.1f, checkerRadius * 0.85f, checkerPaint)
                    }
                    continue
                }
                // ציור רגיל של חיילים על הלוח
                if (i < 12) {
                    x = width - padding - (i * triangleWidth + triangleWidth / 2)
                    for (j in 0 until count) {
                        y = height - padding - (j + 1) * checkerRadius * 1.8f
                        checkerPaint.color = if (isWhite) Color.WHITE else Color.BLACK
                        canvas.drawCircle(x, y, checkerRadius, checkerPaint)
                        canvas.drawCircle(x, y, checkerRadius, checkerBorderPaint)
                        checkerPaint.color = if (isWhite) Color.LTGRAY else Color.DKGRAY
                        canvas.drawCircle(x, y - checkerRadius * 0.1f, checkerRadius * 0.85f, checkerPaint)
                    }
                } else if (i < 24) {
                    x = padding + ((i - 12) * triangleWidth + triangleWidth / 2)
                    for (j in 0 until count) {
                        y = padding + (j + 1) * checkerRadius * 1.8f
                        checkerPaint.color = if (isWhite) Color.WHITE else Color.BLACK
                        canvas.drawCircle(x, y, checkerRadius, checkerPaint)
                        canvas.drawCircle(x, y, checkerRadius, checkerBorderPaint)
                        checkerPaint.color = if (isWhite) Color.LTGRAY else Color.DKGRAY
                        canvas.drawCircle(x, y - checkerRadius * 0.1f, checkerRadius * 0.85f, checkerPaint)
                    }
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
                            val validMoves = getValidMovesForDice(selectedPosition, isWhite, availableMoves)
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
                            moveChecker(selectedPosition, targetPosition, getValidMovesForDice(selectedPosition, boardState[selectedPosition].second, availableMoves))
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
        val padding = 60.0f

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
        // אם הלחיצה מחוץ לאזורים החוקיים, התעלם
        if (position == -1) {
            selectedCheckerPosition = null // בטל בחירת חייל אם נלחץ מחוץ ללוח
            invalidate()
            return
        }

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
            val validMoves = getValidBarEntriesForDice(isWhiteTurn, availableMoves)
            if (validMoves.isEmpty()) {
                Toast.makeText(context, "אין אפשרויות כניסה מהבר", Toast.LENGTH_SHORT).show()
            }
            return
        }
        
        val (count, isWhite) = boardState[position]

        // בדיקות רגילות
        if (count > 0 && isWhite == isWhiteTurn && availableMoves.isNotEmpty()) {
            selectedCheckerPosition = position

            // הצג את המהלכים האפשריים לחייל זה
            val validMoves = getValidMovesForDice(position, isWhite, availableMoves)
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
    /** ממפה נקודת-מגע לאינדקס משבצת 0-23, 24=Bar שחור, 25=Bar לבן, -1=חוץ */
    private fun getPositionFromTouch(x: Float, y: Float): Int {
        val w          = width.toFloat()
        val h          = height.toFloat()
        val outer      = 120f                       // אותו padding שבו השתמשת
        val boardW     = w - outer * 2
        val triW       = boardW / 12
        val checkerR   = triW * 0.2f                // רדיוס משוער של חייל
        val barSideX   = outer * 0.6f               // מקום ציור ה-Bar בצד

        /* 0. מחוץ למסגרת? */
        if (x < 0 || x > w || y < 0 || y > h) return -1

        /* 1. Bar צדדי – שחור (שמאל) */
        if (x in (barSideX - checkerR)..(barSideX + checkerR) &&
            y in outer..(h - outer)) {
            return 24                                // bar שחור
        }

        /* 2. Bar צדדי – לבן (ימין) */
        if (x in (w - barSideX - checkerR)..(w - barSideX + checkerR) &&
            y in outer..(h - outer)) {
            return 25                                // bar לבן
        }

        /* 3. לוח פנימי – משולשים */
        // (אם תרצי להשאיר גם Bar באמצע – אפשר, אבל קודם נזהה משולשים)
        for (i in 0..23) {
            if (triangleAreas[i].contains(x, y)) return i
        }

        return -1
    }



    private fun moveChecker(fromPosition: Int, toPosition: Int, options: List<Int>) {
        Log.d("BackgammonDebug", "moveChecker: from=$fromPosition, to=$toPosition, opts=$options, dice=[$dice1Value,$dice2Value], avail=$availableMoves")

        /* 1. נגיעה במסגרת? */
        if (toPosition == -1) {
            Toast.makeText(context, "גרור למשבצת חוקית", Toast.LENGTH_SHORT).show()
            return
        }

        /* 2. נתונים על המשבצת שממנה זזים (גישה בטוחה) */
        val srcPair   = bs(fromPosition) ?: return      // אם null – לא עושים כלום
        val count     = srcPair.first
        val isWhite   = srcPair.second

        /* 3. פלג ניתוקים למקרים מיוחדים */
        val isBearingOff = (options.contains(99) && toPosition == 99) ||
                (options.contains(99) && ((isWhite && toPosition >= 24) ||
                        (!isWhite && toPosition < 0)))

        val isBarEntry   = (fromPosition == 25 && isWhite) ||
                (fromPosition == 24 && !isWhite)

        /* 4. האם המהלך מותר עקרונית? */
        if (!options.contains(toPosition) && !isBearingOff) {
            Toast.makeText(context, "מהלך לא חוקי", Toast.LENGTH_SHORT).show()
            return
        }

        /* 5. יש חייל להזיז? */
        if (count <= 0) return

        /* ============ 5A. הוצאת חייל (Bearing-off) ============ */
        if (isBearingOff) {
            val moveDistance = if (isWhite) 24 - fromPosition else fromPosition + 1
            val exactMove    = availableMoves.find { it == moveDistance }
            val largerMove   = availableMoves.filter { it > moveDistance }.minOrNull()

            when {
                exactMove != null -> {
                    availableMoves.remove(exactMove)
                    moveHistory.add(Move(fromPosition, 99, null, listOf(exactMove)))
                }
                largerMove != null && isLastFarthestChecker(fromPosition, isWhite) -> {
                    availableMoves.remove(largerMove)
                    moveHistory.add(Move(fromPosition, 99, null, listOf(largerMove)))
                }
                else -> {
                    Toast.makeText(context, "אין מספר מתאים בקוביות", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            boardState[fromPosition] = Pair(count - 1, isWhite)         // מוציא מהמקור
            val bearOffPos = if (isWhite) 26 else 27
            boardState[bearOffPos] = Pair(boardState[bearOffPos].first + 1, isWhite)

            if (isAllBornOff(isWhite)) gameOver(isWhite)

            afterMoveCommon()
            return
        }

        /* ============ 5B. חישוב מרחק (מהלך רגיל / כניסה מה-Bar) ============ */
        val moveDistance = when {
            isBarEntry && isWhite      -> 24 - toPosition
            isBarEntry && !isWhite     -> toPosition + 1
            else                       -> kotlin.math.abs(toPosition - fromPosition)
        }

        /* 6. בדיקה וצריכת הקוביות המתאימות */
        val usedDice = mutableListOf<Int>()
        when {
            availableMoves.contains(moveDistance) -> {
                availableMoves.remove(moveDistance)
                usedDice += moveDistance
            }
            dice1Value != dice2Value &&
                    moveDistance == dice1Value + dice2Value &&
                    availableMoves.contains(dice1Value) &&
                    availableMoves.contains(dice2Value) -> {          // שילוב שתי קוביות
                availableMoves.remove(dice1Value)
                availableMoves.remove(dice2Value)
                usedDice += dice1Value
                usedDice += dice2Value
            }
            dice1Value == dice2Value &&
                    moveDistance % dice1Value == 0 -> {               // דאבל
                val need = moveDistance / dice1Value
                val have = availableMoves.count { it == dice1Value }
                if (need > have) {
                    Toast.makeText(context, "אין מספיק קוביות דאבל", Toast.LENGTH_SHORT).show()
                    return
                }
                repeat(need) {
                    availableMoves.remove(dice1Value)
                    usedDice += dice1Value
                }
            }
            else -> {
                Toast.makeText(context, "מרחק לא תואם לקוביות", Toast.LENGTH_SHORT).show()
                return
            }
        }

        /* 7. ביצוע המהלך עצמו */
        var eatenChecker: Pair<Int, Boolean>? = null
        if (toPosition in 0..23) {
            val dstPair = bs(toPosition) ?: Pair(0, isWhite)  // יעד ריק אם null

            when {
                dstPair.first == 1 && dstPair.second != isWhite -> {  // אכילה
                    eatenChecker = Pair(toPosition, dstPair.second)
                    val barPos   = if (dstPair.second) 25 else 24
                    boardState[barPos] = Pair(boardState[barPos].first + 1, dstPair.second)
                    boardState[toPosition] = Pair(1, isWhite)
                }
                dstPair.first == 0 || dstPair.second == isWhite -> {
                    boardState[toPosition] = Pair(dstPair.first + 1, isWhite)
                }
                else -> {
                    Toast.makeText(context, "מהלך נחסם", Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }

        boardState[fromPosition] = Pair(count - 1, isWhite)
        moveHistory.add(Move(fromPosition, toPosition, eatenChecker, usedDice))
        afterMoveCommon()
    }

    /* ───── פונקציית-עזר אחרי כל מהלך ───── */
    private fun afterMoveCommon() {
        currentTurnMoves++
        if (availableMoves.isEmpty()) {
            waitForDoneButton = true
            Toast.makeText(context, "מהלכים הושלמו, לחץ DONE", Toast.LENGTH_SHORT).show()
        }
        updateFirebase()
        invalidate()
    }
    /* ─────────────────────────────────────── */
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
        waitForDoneButton = false // איפוס מצב DONE
        selectedCheckerPosition = null // איפוס בחירת חייל
        Log.d("Backgammon", "מעבר תור ל${if (isWhiteTurn) "לבן" else "שחור"}")
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
                if (boardState[i].first > 0 && !boardState[i].second) {
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
                    if (boardState[i].first > 0 && !boardState[i].second) {
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
        availableMoves.addAll(lastMove.usedDiceValues)

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

    // פונקציה לסנכרון מצב המשחק עם Firebase
    fun syncWithFirebase(snapshot: DataSnapshot) {
        snapshot.child("dice1").getValue(Int::class.java)?.let {
            dice1Value = it
        }
        snapshot.child("dice2").getValue(Int::class.java)?.let {
            dice2Value = it
        }

        snapshot.child("currentTurn").getValue(String::class.java)?.let {
            isWhiteTurn = it == "white"
        }

        val boardStateData = snapshot.child("boardState")
        if (boardStateData.exists()) {
            val newBoard = boardStateData.children.mapIndexed { index, snapshot ->
                val isWhite = snapshot.child("isWhite").getValue(Boolean::class.java) ?: true
                val count = snapshot.child("count").getValue(Int::class.java) ?: 0
                Pair(count, isWhite)
            }

            for (i in newBoard.indices) {
                if (i < boardState.size) {
                    boardState[i] = newBoard[i]
                }
            }
        }

        loadDiceImages(context)
        invalidate()
    }

    // מעדכן את מצב המשחק ב-Firebase אחרי מהלך
    private fun updateFirebase() {
        val boardStateMap = boardState.mapIndexed { index, pair ->
            "position_$index" to mapOf(
                "count" to pair.first,
                "isWhite" to pair.second
            )
        }.toMap()

        MultiplayerManager.updateGameState(
            mapOf(
                "dice1" to dice1Value,
                "dice2" to dice2Value,
                "currentTurn" to if (isWhiteTurn) "white" else "black",
                "boardState" to boardStateMap
            )
        )
    }

    // פונקציה לסיום התור עם סנכרון Firebase
    fun finishTurn() {
        Log.d("Backgammon", "finishTurn called - isWhiteTurn: $isWhiteTurn, availableMoves: $availableMoves, dice1Value: $dice1Value, dice2Value: $dice2Value")

        // בדיקה ראשונה: אם לא הטילו קוביות
        if (!initialDiceRoll) {
            Toast.makeText(context, "יש להטיל קוביות תחילה", Toast.LENGTH_SHORT).show()
            return
        }

        // בדיקה שנייה: אם יש חיילים בבר - הם חייבים להיכנס קודם
        val barPosition = if (isWhiteTurn) 25 else 24
        if (boardState[barPosition].first > 0 && boardState[barPosition].second == isWhiteTurn) {
            val validBarMoves = getValidBarEntriesForDice(isWhiteTurn, availableMoves)
            if (validBarMoves.isNotEmpty()) {
                Toast.makeText(context, "חייב להכניס את החיילים מהבר קודם", Toast.LENGTH_SHORT).show()
                return
            } else {
                // אין אפשרויות כניסה מהבר - סיום תור אוטומטי מותר
                Log.d("Backgammon", "אין אפשרות להכניס חיילים מהבר - סיום תור אוטומטי")
                switchTurn()
                updateFirebase()
                invalidate()
                return
            }
        }

        // בדיקה שלישית: אם יש עוד מהלכים אפשריים
        if (availableMoves.isNotEmpty()) {
            val hasValidMoves = playerHasAnyValidMoves(isWhiteTurn, availableMoves)

            if (hasValidMoves) {
                // יש מהלכים אפשריים - השחקן צריך לבצע אותם או ללחוץ "Done" שוב לאישור סיום מוקדם
                if (!waitForDoneButton) {
                    waitForDoneButton = true
                    val remainingMovesText = if (dice1Value == dice2Value) {
                        "יש לך עוד ${availableMoves.size} מהלכים דאבל (${dice1Value}). לחץ 'סיום' שוב אם אין מהלכים חוקיים."
                    } else {
                        "יש לך עוד מהלכים: ${availableMoves.joinToString(", ")}. לחץ 'סיום' שוב אם אין מהלכים חוקיים."
                    }
                    Toast.makeText(context, remainingMovesText, Toast.LENGTH_LONG).show()
                    return
                } else {
                    // לחצו "Done" פעמיים - מאפשרים סיום תור גם עם מהלכים שנותרו
                    Log.d("Backgammon", "שחקן בחר לסיים תור למרות מהלכים זמינים")
                    waitForDoneButton = false
                }
            } else {
                // אין מהלכים אפשריים למרות קוביות זמינות - סיום תור אוטומטי
                Log.d("Backgammon", "אין מהלכים אפשריים עם קוביות זמינות - סיום תור אוטומטי")
            }
        }

        // כל הבדיקות עברו - סיים תור
        Log.d("Backgammon", "סיום תור של שחקן ${if (isWhiteTurn) "לבן" else "שחור"}")
        switchTurn()
        waitForDoneButton = false
        updateFirebase()
        invalidate()
    }

    // פונקציה חדשה לבדיקה אם לשחקן יש מהלכים חוקיים כלשהם
    private fun playerHasAnyValidMoves(isPlayerWhite: Boolean, currentDice: List<Int>): Boolean {
        if (currentDice.isEmpty()) {
            return false // אין קוביות, אין מהלכים
        }

        // בדוק קודם את עמדת הבר של השחקן
        val barPosition = if (isPlayerWhite) 25 else 24
        if (boardState[barPosition].first > 0 && boardState[barPosition].second == isPlayerWhite) {
            val barMoves = getValidBarEntriesForDice(isPlayerWhite, currentDice)
            if (barMoves.isNotEmpty()) {
                return true // יש מהלכים מהבר
            }
            // אם אין מהלכים מהבר והשחקן על הבר, הוא לא יכול לבצע מהלכים אחרים
            return false 
        }

        // עבור על כל המשבצות הרגילות
        for (position in 0..23) {
            if (boardState[position].first > 0 && boardState[position].second == isPlayerWhite) {
                val possibleMoves = getValidMovesForDice(position, isPlayerWhite, currentDice)
                if (possibleMoves.isNotEmpty()) {
                    return true // נמצא מהלך חוקי
                }
            }
        }
        return false // לא נמצאו מהלכים חוקיים
    }

    // פונקציה להגדרת השם של השחקן הנוכחי
    fun setCurrentPlayerName(playerName: String) {
        currentPlayerName = playerName
        // השחקן תמיד יתחיל כלבן
        isWhiteTurn = true
        invalidate()
    }
    
    // פונקציה לקבלת השם של השחקן הנוכחי
    fun getCurrentPlayerName(): String {
        return currentPlayerName
    }
}

