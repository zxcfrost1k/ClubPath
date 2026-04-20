package com.example.guessplayer

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.example.guessplayer.databinding.ActivityMainBinding
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import androidx.cardview.widget.CardView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.guessplayer.fragments.FragmentForChapter1
import com.example.guessplayer.fragments.FragmentForChapter2
import com.example.guessplayer.fragments.FragmentForChapter3
import com.example.guessplayer.fragments.FragmentForChapter4
import com.example.guessplayer.fragments.FragmentForChapter5
import com.example.guessplayer.fragments.FragmentsForChaptersPager
import androidx.core.view.isVisible
import kotlin.math.abs


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    // текст
    lateinit var balanceText: TextView
    lateinit var userLevelText: TextView
    // лейауты
    private lateinit var uniformWindow: RelativeLayout
    private lateinit var hiddenButtonsContainer: LinearLayout
    // кард-вью
    private lateinit var settingsWindow: CardView
    private lateinit var trophyWindow: CardView
    private lateinit var askWindow: CardView
    private lateinit var newsWindow: CardView
    // списки/словари
    private var gamerNowProgress = mapOf<String, Int>()
    // кнопки
    private lateinit var buttonResetProgress: Button
    // кнопки-картинки
    private lateinit var buttonSupport: ImageButton
    private lateinit var buttonSettings: ImageButton
    private lateinit var buttonTrophy: ImageButton
    private lateinit var buttonAsk: ImageButton
    private lateinit var buttonNews: ImageButton
    // булевые
    private var isExpanded = false
    // коллбэки
    private var settingsBackCallback: OnBackInvokedCallback? = null

    companion object {
        private const val MAX_LEVEL_CHAPTER_1 = 12
    }

    private val getResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                updateBalanceFromFile()
            }
        }

    // <<<ИНИЦИАЛИЗАТОР>>>

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideNavigationBar()

        setContentView(R.layout.activity_main)

        setupBinding()
        setupViewPager()

        balanceText = findViewById(R.id.balanceTextView)
        userLevelText = findViewById(R.id.userLevelTextView)

        uniformWindow = findViewById(R.id.uniformWindowRelative)
        settingsWindow = findViewById(R.id.settingsCardView)
        trophyWindow = findViewById(R.id.trophyCardView)
        askWindow = findViewById(R.id.askCardView)
        newsWindow = findViewById(R.id.newsCardView)

        hiddenButtonsContainer = findViewById(R.id.hiddenButtonsContainer)

        buttonSupport = findViewById(R.id.buttonSupport)

        buttonSettings = findViewById(R.id.buttonSettings)
        buttonTrophy = findViewById(R.id.buttonTrophyCollection)
        buttonAsk = findViewById(R.id.buttonAsk)
        buttonNews = findViewById(R.id.buttonNews)

        buttonResetProgress = findViewById(R.id.buttonResetProgress)

        createProgressFile()
        gamerNowProgress = readProgressFile()

        showBalance()
        showUserLevel()

        prepareSupportMenuAnimation()

        setupUniformWindowTouchListener()

        setupClickListeners()
    }

    /// <<<SETUPS>>>

    // сборка макета
    private fun setupBinding() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    // сборка фрагментов
    private fun setupViewPager() {
        val fragments = listOf(
            FragmentForChapter1(),
            FragmentForChapter2(),
            FragmentForChapter3(),
            FragmentForChapter4(),
            FragmentForChapter5()
        )

        val adapter = FragmentsForChaptersPager(
            fragments,
            supportFragmentManager,
            lifecycle
        )

        binding.pager.adapter = adapter

        binding.pager.setPageTransformer { page, position ->
            val absPosition = abs(position)
            page.alpha = 1 - absPosition
        }
    }

    // слушатель универсального окна
    @SuppressLint("ClickableViewAccessibility")
    private fun setupUniformWindowTouchListener() {
        uniformWindow.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val x = event.rawX.toInt()
                val y = event.rawY.toInt()

                val visibleCards = listOf(
                    settingsWindow to R.id.settingsCardView,
                    trophyWindow to R.id.trophyCardView,
                    askWindow to R.id.askCardView,
                    newsWindow to R.id.newsCardView
                ).filter { it.first.isVisible }

                val isInsideAnyWindow = visibleCards.any { (cardView, _) ->
                    val location = IntArray(2)
                    cardView.getLocationOnScreen(location)
                    val viewX = location[0]
                    val viewY = location[1]

                    x >= viewX && x <= viewX + cardView.width &&
                            y >= viewY && y <= viewY + cardView.height
                }

                if (!isInsideAnyWindow) {
                    hideUniformWindow()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    // слушатель support menu
    private fun setupClickListeners() {
        buttonSupport.setOnClickListener {
            if (isExpanded) {
                collapseMenu()
            } else {
                expandMenu()
            }
        }

        buttonSettings.setOnClickListener(::showSettingsWindow)
        buttonTrophy.setOnClickListener(::showTrophyWindow)
        buttonAsk.setOnClickListener(::showAskWindow)
        buttonNews.setOnClickListener(::showNewsWindow)

        buttonResetProgress.setOnClickListener(::resetProgress)
    }

    // <<<ОБРАБОТЧИКИ НАЖАТИЯ КНОПКИ ДЛЯ ПЕРЕХОДА К ГЛАВАМ>>>

    // переход к первой главе
    @Suppress("DEPRECATION")
    fun goToChapter1(view: View){
        var delay: Long = 100

        if (!uniformWindow.isVisible) {
            val chapter1Level = gamerNowProgress["chapter1"] ?: 0

            if (chapter1Level < MAX_LEVEL_CHAPTER_1) {
                if (hiddenButtonsContainer.isVisible) {
                    delay = 200
                    collapseMenu()
                }

                buttonSupport.postDelayed({
                    launchChapter(1)

                    overridePendingTransition(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out)
                }, delay)
            } else {
                showMessage("Chapter I is already finished")
            }
        }
    }

    // переход ко второй главе
    fun goToChapter2(view: View) {
    }

    // переход к третьей главе
    fun goToChapter3(view: View) {
    }

    // переход к четвертой главе
    fun goToChapter4(view: View) {
    }

    // переход к пятой главе
    fun goToChapter5(view: View) {
    }

    private fun launchChapter(chapterNumber: Int) {
        val intent = Intent(this, ChapterDefaultActivity::class.java).apply {
            putExtra("currentChapter",
                chapterNumber)
            putExtra("currentLvl",
                gamerNowProgress["chapter$chapterNumber"] ?: 0)
            putExtra("filenameForFootballPlayersClubs",
                "clubs_chapter_$chapterNumber.txt")
            putExtra("filenameForFootballPlayersTransferYears",
                "years_chapter_$chapterNumber.txt")
            putExtra("filenameForgetFootballPlayersNames",
                "players_chapter_$chapterNumber.txt")
            putExtra("filenameForGameProgress",
                "game_progress.txt")
        }
        getResult.launch(intent)
    }

    // <<<ОТОБРАЖЕНИЕ И ВНЕШНИЙ ВИД ОТДЕЛЬНЫХ ЭЛЕМЕНТОВ>>>

    // настройка нав. панели
    private fun hideNavigationBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, window.decorView)

        controller.hide(WindowInsetsCompat.Type.navigationBars())
        controller.show(WindowInsetsCompat.Type.statusBars())

        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    // отображение баланса
    fun showBalance() {
        val nowBalance = gamerNowProgress["balance"] ?: 0
        balanceText.text = resources.getString(R.string.balance_format,
            nowBalance)
    }

    fun showUserLevel() {
        val nowUserLevel = gamerNowProgress["user_level"] ?: 0
        userLevelText.text = resources.getString(R.string.user_level_format,
            nowUserLevel)
    }

    // отображение toast-сообщения
    fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    // <<<АНИМАЦИИ>>>

    // Подготовка анимации support menu
    private fun prepareSupportMenuAnimation() {
        // Скрываем контейнер
        hiddenButtonsContainer.apply {
            alpha = 0f
            visibility = View.GONE
        }

        listOf(buttonSettings, buttonTrophy, buttonAsk, buttonNews).forEach { button ->
            button.apply {
                translationY = 100f
                alpha = 0f
            }
        }
    }

    // появление support menu
    private fun expandMenu() {
        if (isExpanded) return

        isExpanded = true

        ObjectAnimator.ofFloat(buttonSupport,
            View.ROTATION, 0f, 180f).apply {
            duration = 300
            start()
        }

        hiddenButtonsContainer.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(180)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    val buttons = listOf(
                        buttonSettings,
                        buttonTrophy,
                        buttonAsk,
                        buttonNews)
                    buttons.forEachIndexed { index, button ->
                        button.postDelayed({
                            if (isExpanded) {
                                button.animate()
                                    .translationY(0f)
                                    .alpha(1f)
                                    .setDuration(400)
                                    .setInterpolator(OvershootInterpolator())
                                    .start()
                            }
                        }, (index * 100).toLong())
                    }
                }
                .start()
        }
    }

    // скрытие support menu
    private fun collapseMenu() {
        if (!isExpanded) return

        isExpanded = false

        ObjectAnimator.ofFloat(buttonSupport,
            View.ROTATION, 180f, 0f).apply {
            duration = 300
            start()
        }

        val buttons = listOf(buttonSettings, buttonTrophy, buttonAsk, buttonNews)
        buttons.reversed().forEachIndexed { index, button ->
            button.postDelayed({
                if (!isExpanded) {
                    button.animate()
                        .translationY(100f)
                        .alpha(0f)
                        .setDuration(300)
                        .setInterpolator(AccelerateInterpolator())
                        .start()
                }
            }, (index * 50).toLong())
        }

        hiddenButtonsContainer.postDelayed({
            if (!isExpanded) {
                hiddenButtonsContainer.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .setInterpolator(AccelerateInterpolator())
                    .withEndAction {
                        hiddenButtonsContainer.visibility = View.GONE
                    }
                    .start()
            }
        }, 150)
    }

    // появление универсального окна
    private fun showUniformWindow() {
        uniformWindow.apply {
            visibility = View.VISIBLE
            alpha = 0f

            animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }

    // появление определенного окна
    private fun showWindow(window: CardView) {
        showUniformWindow()
        window.apply {
            visibility = View.VISIBLE
            alpha = 0f

            animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }
    fun showSettingsWindow(view: View) = showWindow(settingsWindow)
    fun showTrophyWindow(view: View) = showWindow(trophyWindow)
    fun showAskWindow(view: View) = showWindow(askWindow)
    fun showNewsWindow(view: View) = showWindow(newsWindow)

    // скрытие универсального окна
    private fun hideUniformWindow() {
        uniformWindow.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                uniformWindow.apply {
                    visibility = View.GONE
                    alpha = 1f

                    settingsWindow.visibility = View.GONE
                    trophyWindow.visibility = View.GONE
                    askWindow.visibility = View.GONE
                    newsWindow.visibility = View.GONE
                }
            }
            .start()
    }

    // <<<РАБОТА С ФАЙЛАМИ>>>

    // создание файла с прогрессом игрока
    private fun createProgressFile() {
        val fileName = "game_progress.txt"
        val defaultContent = """
            chapter1 0
            chapter2 0
            chapter3 0
            balance 0
        """.trimIndent()

        try {
            val files = fileList()
            if (!files.contains(fileName)) {
                openFileOutput(fileName, MODE_PRIVATE).use { output ->
                    output.write(defaultContent.toByteArray())
                }
                Log.d("FileCreate", "File $fileName is created")
            } else {
                Log.d("FileCreate", "File $fileName already created")
            }
        } catch (e: Exception) {
            Log.e("FileCreate", "Error with creation file $fileName", e)
        }
    }

    // чтение файла с прогрессом игрока
    fun readProgressFile(): Map<String, Int> {
        val progressMap = mutableMapOf<String, Int>()
        try {
            openFileInput("game_progress.txt").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size == 2) {
                        val key = parts[0]
                        val value = parts[1].toIntOrNull() ?: 0
                        progressMap[key] = value
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FileRead", "Error with reading file game_progress.txt", e)
        }
        return progressMap
    }


    // обнуление файла с прогрессом игрока
    fun resetProgress(view: View) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Reset progress")
            .setMessage("Are you sure you want to discard all game progress? " +
                    "All achievements will be lost.")
            .setPositiveButton("Reset") { _, _ ->
                performResetProgress()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // пересоздание файла с прогрессом игрока
    private fun performResetProgress() {
        try {
            val resetContent = """
            chapter1 0
            chapter2 0
            chapter3 0
            balance 0
            user_level 0
        """.trimIndent()

            openFileOutput("game_progress.txt", MODE_PRIVATE).use { output ->
                output.write(resetContent.toByteArray())
            }

            gamerNowProgress = readProgressFile()

            showBalance()
            showUserLevel()
            showMessage("Progress reset successfully")
            hideUniformWindow()

            Log.d("ResetProgress", "Progress has been reset successfully")

        } catch (e: Exception) {
            Log.e("ResetProgress", "Error resetting progress", e)
            showMessage("Error resetting progress")
        }
    }

    // обновление баланса из файла с прогрессом игрока
    private fun updateBalanceFromFile() {
        gamerNowProgress = readProgressFile()
        showBalance()
    }

    private fun updateUserLevelFromFile() {
        gamerNowProgress = readProgressFile()
        showUserLevel()
    }

    // <<<СТАРТЕР>>>

    override fun onResume() {
        super.onResume()
        updateBalanceFromFile()
        updateUserLevelFromFile()
    }

    // <<<ДЕСТРУКТОР>>>

    override fun onDestroy() {
        super.onDestroy()
        settingsBackCallback?.let {
            onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it)
        }
    }
}
