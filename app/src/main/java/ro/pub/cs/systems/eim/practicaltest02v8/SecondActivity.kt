package ro.pub.cs.systems.eim.practicaltest02v8

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class SecondActivity: AppCompatActivity() {
    private lateinit var resultTextView: TextView
    private lateinit var fetchButton: Button
    private lateinit var op1Input: EditText
    private lateinit var op2Input: EditText
    private lateinit var operatorInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.second_activity)

        resultTextView = findViewById(R.id.resultTextView)
        fetchButton = findViewById(R.id.calculateButton)
        op1Input = findViewById(R.id.t1Input)
        op2Input = findViewById(R.id.t2Input)
        operatorInput = findViewById(R.id.operationInput)

        fetchButton.setOnClickListener {
            val op1 = op1Input.text.toString()
            val op2 = op2Input.text.toString()
            val operator = operatorInput.text.toString()

            fetchCalculation(this, operator, op1.toInt(), op2.toInt())
        }
    }

    interface CalculatorApi {
        @GET("expr/expr_get.php")
        fun calculate(
            @Query("operation") operation: String,
            @Query("t1") t1: Int,
            @Query("t2") t2: Int
        ): Call<CalculatorResponse>
    }

    data class CalculatorResponse(
        val result: Double
    )

    object RetrofitClient {
        private const val BASE_URL = "http://10.40.5.83:8080/"

        val instance: Retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }

    fun fetchCalculation(context: Context, operation: String, t1: Int, t2: Int) {
        val api = RetrofitClient.instance.create(CalculatorApi::class.java)

        api.calculate(operation, t1, t2).enqueue(object : Callback<CalculatorResponse> {

            override fun onResponse(
                call: Call<CalculatorResponse>,
                response: Response<CalculatorResponse>
            ) {
                Log.d("[ ]", response.body()?.toString() ?: "No Response Body")
                if (response.isSuccessful) {
                    val result = response.body()?.result
                    Log.d("", "Operation: $operation, Result: $result")
                    resultTextView.text = result.toString()
                } else {
                    Log.d("[ERROR]", "Failed to fetch calculation")
                }
            }

            override fun onFailure(call: Call<CalculatorResponse>, t: Throwable) {
                Log.d("[ERROR]", "Error: ${t.message}")
            }
        })
    }

}