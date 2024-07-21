import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.daljeet.xplayer.MainActivity
import com.daljeet.xplayer.R

class SplashScreen : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)


        val intent = Intent(this@SplashScreen, MainActivity::class.java)
        startActivity(intent)


    }
}