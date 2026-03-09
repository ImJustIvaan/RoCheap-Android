package com.imjustivaan.rocheap
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    // REPLACE THIS with your RoCheap Place ID later
    private val ROCHEAP_PLACE_ID = "1234567890" 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            handleSharedText(intent)
        } else {
            finish() 
        }
    }

    private fun handleSharedText(intent: Intent) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        
        if (sharedText != null) {
            val urlRegex = Regex("(https?://\\S+)")
            val match = urlRegex.find(sharedText)
            
            if (match != null) {
                Toast.makeText(this, "RoCheap: Resolving link...", Toast.LENGTH_SHORT).show()
                resolveRobloxLink(match.value)
            } else {
                Toast.makeText(this, "No valid link found.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun resolveRobloxLink(shortUrl: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val connection = URL(shortUrl).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false 
                connection.connect()

                val responseCode = connection.responseCode
                var finalUrl = shortUrl

                if (responseCode in 300..399) {
                    finalUrl = connection.getHeaderField("Location") ?: shortUrl
                }

                val idRegex = Regex("/(?:catalog|bundles)/(\\d+)")
                val idMatch = idRegex.find(finalUrl)

                if (idMatch != null) {
                    val itemId = idMatch.groupValues[1]
                    withContext(Dispatchers.Main) {
                        copyToClipboardAndLaunch(itemId)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Could not find Item ID.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Network error.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun copyToClipboardAndLaunch(itemId: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Roblox Item ID", itemId)
        clipboard.setPrimaryClip(clip)
        
        Toast.makeText(this, "Copied ID: $itemId", Toast.LENGTH_SHORT).show()

        try {
            val robloxIntent = Intent(Intent.ACTION_VIEW)
            robloxIntent.data = Uri.parse("roblox://placeId=$ROCHEAP_PLACE_ID")
            startActivity(robloxIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Roblox app not found!", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
