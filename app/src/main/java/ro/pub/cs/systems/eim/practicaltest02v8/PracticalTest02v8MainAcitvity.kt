package ro.pub.cs.systems.eim.practicaltest02v8

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.http.GET

class PracticalTest02v8MainAcitvity : AppCompatActivity() {
    private lateinit var resultTextView: TextView
    private lateinit var fetchButton: Button
    private lateinit var inputText: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_practical_test02v8_main)

        resultTextView = findViewById(R.id.resultText)
        fetchButton = findViewById(R.id.requestButton)
        inputText = findViewById(R.id.currencyInput)
        var secondActivityButton: Button = findViewById(R.id.navigateButton)

        fetchButton.setOnClickListener {
            val currency = inputText.text.toString()
            // get from cache
            val cache = BitcoinPriceCache(this)
            val lastUpdated = cache.getLastUpdated()
            val lastUpdatedTimestamp = cache.getLastUpdatedTimestamp()
            val usdRate = cache.getUsdRate()
            val eurRate = cache.getEurRate()

            if (currency == "USD") {
                if (usdRate != null) {
                    Log.d("[CACHE]", "1 BTC = $usdRate USD at $lastUpdated")
                    resultTextView.text = "1 BTC = $usdRate USD at $lastUpdated"
                } else {
                    fetchBitcoinPrice(this)
                }
            } else if (currency == "EUR") {
                if (eurRate != null) {
                    Log.d("[CACHE]", "1 BTC = $eurRate EUR at $lastUpdated")
                    resultTextView.text = "1 BTC = $eurRate EUR at $lastUpdated"
                } else {
                    fetchBitcoinPrice(this)
                }
            } else {
                Log.d("[ERROR]", "Invalid currency")
            }
        }

        // navigate to second activity
        secondActivityButton.setOnClickListener {
            val intent = Intent(this, SecondActivity::class.java)
            startActivity(intent)
        }

        startAutoUpdate(this)
    }

    interface CoinDeskApi {
        @GET("v1/bpi/currentprice/EUR.json")
        fun getCurrentBitcoinPrice(): Call<BitcoinPriceResponse>
    }

    object RetrofitClient {
        private const val BASE_URL = "https://api.coindesk.com/"

        val instance: retrofit2.Retrofit by lazy {
            retrofit2.Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build()
        }
    }

    data class BitcoinPriceResponse(
        val bpi: Bpi,
        val time: Time
    )

    data class Bpi(
        val USD: CurrencyInfo,
        val EUR: CurrencyInfo
    )

    data class CurrencyInfo(
        val code: String,
        val rate: String,
        val description: String
    )
    data class Time(
        val updated: String
    )

    class BitcoinPriceCache(context: Context) {
        private val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("BitcoinPriceCache", Context.MODE_PRIVATE)

        fun saveRates(usdRate: String?, eurRate: String?, lastUpdated: String?) {
            sharedPreferences.edit().apply {
                putString("usdRate", usdRate)
                putString("eurRate", eurRate)
                putString("lastUpdated", lastUpdated)
                putLong("lastUpdatedTimestamp", System.currentTimeMillis())
                apply()
            }


        }

        fun getUsdRate(): String? = sharedPreferences.getString("usdRate", null)

        fun getEurRate(): String? = sharedPreferences.getString("eurRate", null)

        fun getLastUpdated(): String? = sharedPreferences.getString("lastUpdated", null)

        fun getLastUpdatedTimestamp(): Long = sharedPreferences.getLong("lastUpdatedTimestamp", 0L)
    }

    fun startAutoUpdate(context: Context) {
        val handler = Handler(Looper.getMainLooper())
        val cache = BitcoinPriceCache(context)

        val updateRunnable = object : Runnable {
            override fun run() {
                fetchBitcoinPrice(context)
                handler.postDelayed(this,  10000) // Re-run every 10 secs
            }
        }

        handler.post(updateRunnable)
    }

    fun fetchBitcoinPrice(context: Context) {
        val api = RetrofitClient.instance.create(CoinDeskApi::class.java)
        val cache = BitcoinPriceCache(context)

        api.getCurrentBitcoinPrice().enqueue(object : retrofit2.Callback<BitcoinPriceResponse> {
            override fun onResponse(
                call: retrofit2.Call<BitcoinPriceResponse>,
                response: retrofit2.Response<BitcoinPriceResponse>
            ) {
                if (response.isSuccessful) {
                    val bitcoinPriceResponse = response.body()
                    val usdRate = bitcoinPriceResponse?.bpi?.USD?.rate
                    val eurRate = bitcoinPriceResponse?.bpi?.EUR?.rate
                    val lastUpdated = bitcoinPriceResponse?.time?.updated

                    cache.saveRates(usdRate, eurRate, lastUpdated)
                    Log.d("[CACHE UPDATED]", "1 BTC = $usdRate USD, $eurRate EUR at $lastUpdated")
                } else {
                    Log.d("[ERROR]", "Failed to fetch Bitcoin price")
                }
            }

            override fun onFailure(call: retrofit2.Call<BitcoinPriceResponse>, t: Throwable) {
                Log.d("[ERROR]", "Eroare: ${t.message}")
            }
        })
    }

}