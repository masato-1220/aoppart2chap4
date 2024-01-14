package com.example.aop_part2_chap4

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.green
import androidx.core.view.isVisible
import com.example.aop_part2_chap4.model.AppDatabase
import fastcampus.aop.part2.chapter4.model.History
import java.lang.NumberFormatException

class MainActivity : AppCompatActivity() {

    private val expressionTextView: TextView by lazy {
        findViewById<TextView>(R.id.expressionTextView)
    }

    private val resultTextView: TextView by lazy {
        findViewById<TextView>(R.id.resultTextView)
    }

    private val historyLayout: View by lazy {
        findViewById<View>(R.id.historyLayout)
    }

    private val historyLinearLayout: LinearLayout by lazy {
        findViewById<LinearLayout>(R.id.historyLinearLayout)
    }


    private var isOperator = false
    private var hasOperator = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "historyDB"
        ).build()
    }

    fun buttonClicked(v: View) {
        when (v.id) {
            R.id.button0 -> numberButtonClicked("0")
            R.id.button1 -> numberButtonClicked("1")
            R.id.button2 -> numberButtonClicked("2")
            R.id.button3 -> numberButtonClicked("3")
            R.id.button4 -> numberButtonClicked("4")
            R.id.button5 -> numberButtonClicked("5")
            R.id.button6 -> numberButtonClicked("6")
            R.id.button7 -> numberButtonClicked("7")
            R.id.button8 -> numberButtonClicked("8")
            R.id.button9 -> numberButtonClicked("9")
            R.id.buttonPlus -> operatorButtonClicked("+")
            R.id.buttonMinus -> operatorButtonClicked("-")
            R.id.buttonMulti -> operatorButtonClicked("X")
            R.id.buttonDivider -> operatorButtonClicked("/")
            R.id.buttonModulo -> operatorButtonClicked("%")
        }
    }

    private fun numberButtonClicked(number: String) {

        if (isOperator) {
            expressionTextView.append(" ")
        }

        isOperator = false

        val expressionText = expressionTextView.text.split(" ")
        if (expressionText.isNotEmpty() && expressionText.last().length >= 15) {
            Toast.makeText(this, "15文字まで入力できます", Toast.LENGTH_SHORT).show()
            return
        } else if (expressionText.last().isEmpty() && number == "0") {
            Toast.makeText(this, "0は先頭に入力することが出来ません", Toast.LENGTH_SHORT).show()
            return
        }

        expressionTextView.append(number)
        resultTextView.text = calculateExpression()

    }

    private fun operatorButtonClicked(operator: String) {
        if (expressionTextView.text.isEmpty()) {
            return
        }

        when {
            isOperator -> {
                val text = expressionTextView.text.toString()
                expressionTextView.text = text.dropLast(1) + operator
            }

            hasOperator -> {
                Toast.makeText(this, "演算子は一回のみ使用できます。", Toast.LENGTH_SHORT).show()
                return
            }

            else -> {
                expressionTextView.append(("$operator"))
            }
        }

        val ssb = SpannableStringBuilder(expressionTextView.text)
        ssb.setSpan(
            ForegroundColorSpan(getColor(R.color.green)),
            expressionTextView.text.length - 1,
            expressionTextView.text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        expressionTextView.text = ssb

        isOperator = true
        hasOperator = true
    }


    fun resultButtonClicked(v: View) {
        val expressionText = expressionTextView.text.split(" ")

        if (expressionTextView.text.isEmpty() || expressionText.size == 1) {
            return
        }

        if (expressionText.size != 3 && hasOperator) {
            Toast.makeText(this, "まだ完成していない数式です。", Toast.LENGTH_SHORT).show()
            return
        }

        if (expressionText[0].isNumber().not() || expressionText[2].isNumber().not()) {
            Toast.makeText(this, "エラーが発生しました。", Toast.LENGTH_SHORT).show()
            return
        }

        val expressinTexta = expressionTextView.text.toString()
        val resultText = calculateExpression()

        Thread(Runnable {
            db.historyDao().insertHistory(History(null, expressionText, resultText))
        }).start()

        resultTextView.text = ""
        expressionTextView.text = resultText

        isOperator = false
        hasOperator = false
    }

    private fun calculateExpression(): String {
        val expressionText = expressionTextView.text.split(" ")

        if (hasOperator.not() || expressionText.size != 3) {
            return ""
        } else if (!expressionText[0].isNumber() || !expressionText[2].isNumber()) {
            return ""
        }

        val exp1 = expressionText[0].toBigInteger()
        val exp2 = expressionText[2].toBigInteger()
        val op = expressionText[1]

        return when (op) {
            "+" -> (exp1 + exp2).toString()
            "-" -> (exp1 - exp2).toString()
            "x" -> (exp1 * exp2).toString()
            "/" -> (exp1 / exp2).toString()
            "%" -> (exp1 % exp2).toString()
            else -> ""
        }

    }

    fun clearButtonClicked(v: View) {
        expressionTextView.text = ""
        resultTextView.text = ""
        isOperator = false
        hasOperator = false
    }

    fun historyButtonClicked(v: View) {
        historyLayout.isVisible = true
        historyLinearLayout.removeAllViews()

        Thread(Runnable {
            db.historyDao().getAll().reversed().forEach {
                runOnUiThread {
                    val historyView = LayoutInflater.from(this).inflate(R.layout.history_now, null, false)
                    historyView.findViewById<TextView>(R.id.expressionTextView).text = it.expression
                    historyView.findViewById<TextView>(R.id.resultTextView).text = "= ${it.result}"

                    historyLinearLayout.addView(historyView)
                }
            }
        }).start()
    }

    fun closeHistoryButtonClicked(v: View) {
        historyLayout.isVisible = false
    }

    fun historyClearButtonClicked(v: View) {
        historyLinearLayout.removeAllViews()

        Thread(Runnable{
            db.historyDao().deleteAll()
        }).start()
    }
}


fun String.isNumber(): Boolean {
    return try {
        this.toBigInteger()
        true
    } catch (e: NumberFormatException) {
        false
    }
}