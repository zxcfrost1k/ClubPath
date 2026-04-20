package com.example.guessplayer

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.example.guessplayer.chapter_tools.FootballClub
import com.example.guessplayer.chapter_tools.FootballClubAdapter
import com.example.guessplayer.chapter_tools.GetInfoAboutPlayer
import com.example.guessplayer.chapter_tools.ResourceMapOfClubs
import kotlin.random.Random
import kotlin.text.replace
import kotlin.text.uppercase


open class ChapterDefaultActivity: AppCompatActivity() {
    // кнопки
    lateinit var buttonToNextLevel: Button
    lateinit var buttonToSelection: ImageButton
    lateinit var buttonToSelection1: Button
    lateinit var buttonToSelection2: Button
    lateinit var buttonToSelection3: Button
    private lateinit var toggleLoanButton: ToggleButton
    
    // текст
    lateinit var balanceText: TextView
    lateinit var userLevelText: TextView

    // лейауты
    lateinit var successWindow: RelativeLayout // структура окна успешного ввода слова
    lateinit var selectionWindow: RelativeLayout // структура окна селекции
    lateinit var structureOfLetterButtons: LinearLayout // структура букв-кнопок
    // структура некликабельных кнопок для "визуализации" загаданного слова
    lateinit var structureOfButtonsForVisualization: LinearLayout
    lateinit var structureOfButtonsForInputField: LinearLayout // структура "поля ввода"
    private lateinit var recyclerView: RecyclerView

    // скроллеры
    private lateinit var smoothScroller: LinearSmoothScroller

    // адаптеры
    private lateinit var footballClubAdapter: FootballClubAdapter

    // списки и словари
    private val listOfFreePositionsOnTop = Array(MAX_LETTER_BUTTONS) { 0 }
    private val listOfFreePositionsOnLow = Array(MAX_LETTER_BUTTONS) { 1 }
    // список кнопок-букв в "поле ввода"
    private val listOfLetterButtonsForInputField = ArrayList<Button>()
    val listOfLetterButtons = ArrayList<Button>() // список букв-кнопок
    var listOfFootballPlayersNames = ArrayList<String>() // список всех имен
    lateinit var listOfFootballPlayersClubs: List<List<String>> // список клубов
    lateinit var listOfFootballPlayersTransferYears: List<List<String>> // список дат трансферов
    private lateinit var listOfFootballClub: ArrayList<FootballClub>

    // коллбэки
    private var selectionBackCallback: OnBackInvokedCallback? = null

    // числа
    companion object {
        private const val MAX_LETTER_BUTTONS = 17
        private const val MAX_LEVEL = 12

        private const val DEFAULT_WIDTH = 35
        private const val DEFAULT_HEIGHT = 55

        private const val SMALLER_WIDTH = 28
        private const val SMALLER_HEIGHT = 48

        private const val SMALLEST_WIDTH = 21
        private const val SMALLEST_HEIGHT = 45
    }
    var currentLevel = 0
    var currentBalance = 0
    var currentUserLevel = 0

    // объекты класса
    val rmoc = ResourceMapOfClubs()
    val resourceMapOfClubs = rmoc.resourceMapOfClubs

    // <<<ИНИЦИАЛИЗАТОР>>>

    @SuppressLint("MissingInflatedId") // скип проверки на существование всех элементов
    override fun onCreate(savedInstanceState: Bundle?) { // основной метод жизненного цикла окна
        super.onCreate(savedInstanceState)

        hideNavigationBar()

        setContentView(R.layout.activity_default_chapter)
        window.setBackgroundDrawable(null)

        val intent = intent
        val currentChapter =
            intent.getIntExtra("currentChapter", 0) // номер действующей главы
        val filenameForFootballPlayersClubs =
            intent.getStringExtra("filenameForFootballPlayersClubs") ?: "null"
        val filenameForFootballPlayersTransferYears =
            intent.getStringExtra("filenameForFootballPlayersTransferYears") ?: "null"
        val filenameForGetFootballPlayersNames =
            intent.getStringExtra("filenameForgetFootballPlayersNames") ?: "null"
        val filenameForGameProgress =
            intent.getStringExtra("filenameForGameProgress") ?: "null"

        smoothScroller = object : LinearSmoothScroller(this) {
            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return 200f / displayMetrics.densityDpi
            }
        }

        enablePredictiveBackGesture()

        initializeObjects() // инициализация объектов активности

        initializeResources(
            filenameForGetFootballPlayersNames,
            filenameForFootballPlayersClubs,
            filenameForFootballPlayersTransferYears,
            filenameForGameProgress,
            currentChapter
        ) // инициализация ресурсов активности

        setLetterButtons()

        for (i in 0 until listOfLetterButtons.size) {
            resizeButton(listOfLetterButtons[i])
        }

        showBalance()
        showUserLevel()

        setupClickListeners() // отслеживание кликов
        setupSelectionWindowTouchListener()
        setupLoanFilterButton()
    }

    // <<<ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ИНИЦИАЛИЗАТОРА>>>

    // инициализация ресурсов активности
    fun initializeResources(
        filenameForGetFootballPlayersNames: String,
        filenameForFootballPlayersClubs: String,
        filenameForFootballPlayersTransferYears: String,
        filenameForGameProgress: String,
        currentChapter: Int
    ) {
        listOfFootballPlayersNames =
            GetInfoAboutPlayer.getFootballPlayersNames(this,
                filenameForGetFootballPlayersNames) as ArrayList<String>
        listOfFootballPlayersClubs =
            GetInfoAboutPlayer.getFootballPlayersClubs(this,
                filenameForFootballPlayersClubs)
        listOfFootballPlayersTransferYears =
            GetInfoAboutPlayer.getFootballPlayersTransferYears(this,
                filenameForFootballPlayersTransferYears)

        currentLevel =
            GetInfoAboutPlayer.getCurrentLevelFromFile(this,
                filenameForGameProgress, currentChapter)

        currentBalance = GetInfoAboutPlayer.getBalanceFromFile(this,
            filenameForGameProgress)

        currentUserLevel = GetInfoAboutPlayer.getUserLevelFromFile(this,
            filenameForGameProgress)

        listOfFootballClub = ArrayList()
        addDataToListOfFootballClub()

        footballClubAdapter = FootballClubAdapter(listOfFootballClub)
        recyclerView.adapter = footballClubAdapter

        recyclerView.post {
            smoothScroller.targetPosition = recyclerView.adapter?.itemCount?.minus(1) ?: 0
            (recyclerView.layoutManager as LinearLayoutManager).startSmoothScroll(smoothScroller)
        }
    }

    // инициализация объектов активности
    fun initializeObjects() {
        recyclerView = findViewById(R.id.recyclerview)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this,
            RecyclerView.HORIZONTAL, false)

        val buttonLettersIds = listOf(
            R.id.button1, R.id.button2, R.id.button3, R.id.button4, R.id.button5,
            R.id.button6, R.id.button7, R.id.button8, R.id.button9, R.id.button10,
            R.id.button11, R.id.button12, R.id.button13, R.id.button14, R.id.button15,
            R.id.button16, R.id.button17
        )
        listOfLetterButtons.addAll(buttonLettersIds.map { findViewById(it) })

        buttonToNextLevel = findViewById(R.id.buttonNext)
        buttonToSelection = findViewById(R.id.buttonSelection)
        toggleLoanButton = findViewById(R.id.toggleLoanButton)

        balanceText = findViewById(R.id.balanceTextView)
        userLevelText = findViewById(R.id.userLevelTextView)

        structureOfLetterButtons = findViewById(R.id.linearLayoutLow)
        structureOfButtonsForVisualization = findViewById(R.id.linearLayoutUp)
        structureOfButtonsForInputField = findViewById(R.id.linearLayoutUpTop)

        successWindow = findViewById(R.id.successWindowRelative)
        selectionWindow = findViewById(R.id.selectionWindowRelative)

        buttonToSelection1 = findViewById(R.id.buttonSelection1)
        buttonToSelection2 = findViewById(R.id.buttonSelection2)
        buttonToSelection3 = findViewById(R.id.buttonSelection3)
    }

    // заполнение списка с логотипами фут. клубов
    fun addDataToListOfFootballClub() {
        for (i in 0 until listOfFootballPlayersClubs[currentLevel].size) {
            val drawableId: Int = resourceMapOfClubs[listOfFootballPlayersClubs[currentLevel][i]]!!
            listOfFootballClub.add(
                FootballClub(
                    drawableId,
                    listOfFootballPlayersTransferYears[currentLevel][i]
                )
            )
        }
    }

    // заполнение списка с логотипами фут. клубов
    fun addDataToList() {
        for (i in 0 until listOfFootballPlayersClubs[currentLevel].size) {
            val drawableId: Int =
                rmoc.resourceMapOfClubs[listOfFootballPlayersClubs[currentLevel][i]]!!
            listOfFootballClub.add(
                FootballClub(
                    drawableId,
                    listOfFootballPlayersTransferYears[currentLevel][i]
                )
            )
        }
    }

    // установка букв-кнопок
    fun setLetterButtons() {
        createInputField()
        val allLetters = generateStringForLetterButtons()

        repeat(MAX_LETTER_BUTTONS) { i ->
            listOfLetterButtons[i].text = allLetters[i].toString()
        }
    }

    // создание кнопки для визуализации "поля ввода"
    fun createInputField() {
        val name = listOfFootballPlayersNames[currentLevel]
        var width = DEFAULT_WIDTH
        var height = DEFAULT_HEIGHT
        var size = 2

        if (name.length in 10..<13) {
            width = SMALLER_WIDTH
            height = SMALLER_HEIGHT
            size = 1
        }
        else if (name.length > 12) {
            width = SMALLEST_WIDTH
            height = SMALLEST_HEIGHT
            size = 0
        }

        for (i in 0 until name.length) {
            // создание кнопки со своим стилем под определенный символ
            if (name[i] == ' ') {
                createInputButton(0, i, width, height, size)
            }
            else if (name[i] == '-') {
                createInputButton(1, i, width, height, size)
            }
            else {
                createInputButton(2, i, width, height, size)
            }
        }
        setupClickListeners()
    }

    // заполнение структуры "визуализации"
    fun createInputButton(indicatorForStyle: Int,
                                  index: Int, width: Int, height: Int, size: Int) {
        val newButton = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                width.dpToPx(),
                height.dpToPx(),
            ).apply {
                setMargins(
                    2.dpToPx(), 2.dpToPx(),
                    2.dpToPx(), 2.dpToPx()
                )
            }
            when (indicatorForStyle) {
                2 -> setBackgroundResource(R.drawable.stl_transparent_20)
                0 -> {
                    if (index < listOfFreePositionsOnTop.size) {
                        listOfFreePositionsOnTop[index] = 1
                    }
                    setBackgroundResource(R.drawable.btn_input_enter)
                }
                else -> {
                    if (index < listOfFreePositionsOnTop.size) {
                        listOfFreePositionsOnTop[index] = 1
                    }
                    setBackgroundResource(R.drawable.btn_hyphen)
                }
            }
        }

        structureOfButtonsForVisualization.addView(newButton)
        newButton.isClickable = false

        if (indicatorForStyle == 0){
            newButton.text = " "
        }
        else if (indicatorForStyle == 1){
            newButton.text = "—"
            newButton.textSize = 21.0f
        }

        when (size) {
            0 -> newButton.textSize = 18.0f
        }

        createInvisibleButton(width, height, indicatorForStyle)
    }

    // создание кнопки-заготовки для "поля ввода"
    fun createInvisibleButton(width: Int, height: Int, cont: Int) {
        val newButton = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                width.dpToPx(),
                height.dpToPx(),
            ).apply {
                setMargins(
                    2.dpToPx(), 2.dpToPx(),
                    2.dpToPx(), 2.dpToPx()
                )
            }
            setBackgroundResource(R.drawable.btn_letters)
            setTextAppearance(R.style.buttonLetter)
        }
        structureOfButtonsForInputField.addView(newButton)
        listOfLetterButtonsForInputField.add(newButton)

        newButton.isClickable = false
        newButton.visibility = View.INVISIBLE

        if (cont == 0){
            newButton.text = " "
        }
        else if (cont == 1){
            newButton.text = "—"
            newButton.textSize = 21.0f
        }
    }

    // генерация строки из рандомных букв
    fun generateStringForLetterButtons(): String {
        val neededLetters = listOfFootballPlayersNames[currentLevel]
            .replace(" ", "")
            .replace("-", "")
            .uppercase()
        val lenToGenerate = MAX_LETTER_BUTTONS - neededLetters.length

        val allLetters = shuffleString(
            generateRandomLettersWithFrequency(lenToGenerate) + neededLetters
        )

        return allLetters
    }

    // перетасовка строки
    fun shuffleString(input: String): String {
        val chars = input.toMutableList()
        for (i in chars.size - 1 downTo 1) {
            val j = Random.nextInt(i + 1)
            val temp = chars[i]
            chars[i] = chars[j]
            chars[j] = temp
        }

        return chars.joinToString("")
    }

    // генерация рандомных букв с учетом частности повторения
    fun generateRandomLettersWithFrequency(count: Int,
                                           excludeLetters: String = ""): String {
        if (count <= 0) return ""

        val letters = StringBuilder()
        val letterFrequency = mapOf(
            'E' to 12.7, 'T' to 9.1, 'A' to 8.2, 'O' to 7.5, 'I' to 7.0,
            'N' to 6.7, 'S' to 6.3, 'H' to 6.1, 'R' to 6.0, 'D' to 4.3,
            'L' to 4.0, 'C' to 2.8, 'U' to 2.8, 'M' to 2.4, 'W' to 2.4,
            'F' to 2.2, 'G' to 2.0, 'Y' to 2.0, 'P' to 1.9, 'B' to 1.5,
            'V' to 1.0, 'K' to 0.8, 'J' to 0.15, 'X' to 0.15, 'Q' to 0.10, 'Z' to 0.07
        )

        val availableLetters = letterFrequency.keys.filter { !excludeLetters.contains(it) }

        // список букв с учетом частотности
        val weightedLetters = mutableListOf<Char>()
        for (letter in availableLetters) {
            val frequency = letterFrequency[letter] ?: 1.0
            val count = (frequency * 2).toInt()
            repeat(count) {
                weightedLetters.add(letter)
            }
        }

        // перемешиваем список перед выбором
        weightedLetters.shuffle()

        // выбор случайных букв
        repeat(count) {
            if (weightedLetters.isNotEmpty()) {
                val randomChar = weightedLetters.removeAt(0)
                letters.append(randomChar)
            }
        }

        return letters.toString()
    }

    // изменение размера кнопок-букв
    fun resizeButton(button: Button) {
        button.layoutParams = LinearLayout.LayoutParams(
            DEFAULT_WIDTH.dpToPx(), DEFAULT_HEIGHT.dpToPx()
        ).apply {
            setMargins(
                4.dpToPx(), 4.dpToPx(),
                4.dpToPx(), 4.dpToPx()
            )
        }
    }

    // <<<SETUPS>>>

    // настройка кнопки-переключателя
    private fun setupLoanFilterButton() {
        footballClubAdapter.filterLoans(false)

        toggleLoanButton.setOnCheckedChangeListener { _, isChecked ->
            footballClubAdapter.filterLoans(isChecked)

            if (isChecked) {
                recyclerView.post {
                    val lastPosition = footballClubAdapter.itemCount - 1
                    if (lastPosition >= 0) {
                        smoothScroller.targetPosition = lastPosition
                        (recyclerView.layoutManager as LinearLayoutManager).startSmoothScroll(smoothScroller)
                    }
                }
            } else {
                val hiddenCount = footballClubAdapter.getHiddenCount()
                if (hiddenCount > 0) {
                    Toast.makeText(
                        this@ChapterDefaultActivity,
                        "Hidden $hiddenCount loan transfers",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // обработчики нажатий
    fun setupClickListeners() {
        repeat(MAX_LETTER_BUTTONS) { i ->
            listOfLetterButtons[i].setOnClickListener {
                handleLetterButtonClick(listOfLetterButtons[i], i) }
        }

        repeat(listOfLetterButtonsForInputField.size) { i ->
                listOfLetterButtonsForInputField[i].setOnClickListener {
                    handleLetterButtonClick(listOfLetterButtonsForInputField[i], i)
                }
        }

        buttonToNextLevel.setOnClickListener { hideRL() }
    }

    // обработчик нажатия на букву-кнопку
    fun handleLetterButtonClick(button: Button, index: Int) {
        // проверка на клик (из структуры букв-кнопок или из структуры "поля ввода"?)
        if (listOfLetterButtons.contains(button)) {
            // проверка на загруженность "поля ввода"
            if (listOfFootballPlayersNames[currentLevel].length > listOfFreePositionsOnTop.sum()) {
                moveButtonToUp(button, index) // "перемещает" букву-кнопку в "поле ввода"
            }
        }
        else {
            moveButtonToLow(button) // "перемещает" букву-кнопку из "поля ввода"
        }
    }

    // слушатель окна селекции
    @SuppressLint("ClickableViewAccessibility")
    private fun setupSelectionWindowTouchListener() {
        selectionWindow.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val x = event.rawX.toInt()
                val y = event.rawY.toInt()

                val cardView = selectionWindow.findViewById<CardView>(R.id.selectionCardView)

                val location = IntArray(2)
                cardView.getLocationOnScreen(location)
                val viewX = location[0]
                val viewY = location[1]

                val isInside = x >= viewX && x <= viewX + cardView.width &&
                        y >= viewY && y <= viewY + cardView.height

                if (!isInside) {
                    hideSelectionWindow()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    // "перемещает" букву-кнопку в "поле ввода"
    fun moveButtonToUp(button: Button, index: Int) {
        val checkIn: Int = checkFreePosition()
        if (checkIn != -1) {
            button.visibility = View.INVISIBLE
            button.isClickable = false

            val buttonUp = structureOfButtonsForInputField.getChildAt(checkIn)
            if (buttonUp is Button) {
                buttonUp.text = button.text
                buttonUp.isClickable = true
                buttonUp.visibility = View.VISIBLE
            }

            val buttonUnderUp = structureOfButtonsForVisualization.getChildAt(checkIn)
            if (buttonUnderUp is Button) {
                buttonUnderUp.visibility = View.INVISIBLE
            }

            // занимает позицию
            if (checkIn < listOfFreePositionsOnTop.size) {
                listOfFreePositionsOnTop[checkIn] = 1
            }
            if (index < listOfFreePositionsOnLow.size) {
                listOfFreePositionsOnLow[index] = 0
            }

            if (checkPositions() != -1) {
                currentBalance += 15
                if ((currentLevel + 1) % 3 == 0) {
                    currentUserLevel += 1
                }

                showRL()
                showBalance()
                showUserLevel()

                val currentChapter = intent.getIntExtra("currentChapter", 1)
                GetInfoAboutPlayer.updateProgressInFile(this,
                    currentLevel + 1, currentChapter,
                    currentBalance, currentUserLevel)
            }
        }
    }

    // "перемещает" букву-кнопку из "поля ввода"
    fun moveButtonToLow(button: Button) {
        button.visibility = View.INVISIBLE

        var buttonLow: Button? = button
        for (i in 0 until MAX_LETTER_BUTTONS) {
            if ((button.text == listOfLetterButtons[i].text) &&
                (listOfFreePositionsOnLow[i] == 0)) {

                buttonLow = listOfLetterButtons[i]
                listOfFreePositionsOnLow[i] = 1
                break
            }
        }

        button.text = ""
        buttonLow?.apply {
            visibility = View.VISIBLE
            isClickable = true
        }

        val position = structureOfButtonsForInputField.indexOfChild(button)
        if (position != -1) {
            listOfFreePositionsOnTop[position] = 0
        }

        val buttonUnderUp = structureOfButtonsForVisualization.getChildAt(position)
        if (buttonUnderUp is Button) {
            buttonUnderUp.visibility = View.VISIBLE
        }
    }

    // <<<СИСТЕМНЫЕ ФУНКЦИИ>>>

    // возвращает ближайшую к началу свободную позицию
    fun checkFreePosition(): Int {
        repeat(listOfFreePositionsOnTop.size) { i ->
            if (listOfFreePositionsOnTop[i] == 0) {
                return i
            }
        }
        return -1
    }

    // проверка на правильный ввод
    fun checkPositions(): Int {
        val nowName = listOfFootballPlayersNames[currentLevel].uppercase()

        for (i in 0 until nowName.length) {
            val child = structureOfButtonsForInputField.getChildAt(i)
            if (child is Button) {
                if (child.text != "—" && child.text != " ") {
                    if (child.text != nowName[i].toString()) {
                        return -1
                    }
                }
            }
        }
        return 1
    }

    // очистка окна активности
    fun clearWindow() {
        structureOfButtonsForVisualization.removeAllViews()

        for (i in 0 until structureOfButtonsForInputField.childCount) {
            val child = structureOfButtonsForInputField.getChildAt(i)
            if (child is Button) {
                moveButtonToLow(child)
            }
        }

        structureOfButtonsForInputField.removeAllViews()
    }

    // переход букв-кнопок в некликабельный режим
    fun blockLetterButtons() {
        repeat(MAX_LETTER_BUTTONS) { i ->
            if (i < listOfLetterButtons.size) {
                listOfLetterButtons[i].isClickable = false
            }
        }

        for (i in 0 until structureOfButtonsForInputField.childCount) {
            val button = structureOfButtonsForInputField.getChildAt(i)
            if (button is Button) {
                button.isClickable = false
            }
        }
    }

    // переход букв-кнопок в кликабельный режим
    fun unblockAllButtons() {
        repeat(MAX_LETTER_BUTTONS) { i ->
            if (i < listOfLetterButtons.size) {
                listOfLetterButtons[i].isClickable = true
            }
        }

        for (i in 0 until structureOfButtonsForInputField.childCount) {
            val button = structureOfButtonsForInputField.getChildAt(i)
            if (button is Button) {
                button.isClickable = true
            }
        }
    }

    // конвертирование dp в px
    fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    // <<<АНИМАЦИИ>>>

    // переход в видимый режим окна успешного ввода
    fun showRL() {
        if (currentLevel >= MAX_LEVEL - 1) {
            endOfTheChapter()
            return
        }

        buttonToNextLevel.isClickable = true
        blockLetterButtons()

        successWindow.visibility = View.VISIBLE
        successWindow.alpha = 0f

        successWindow.animate() // анимирование появление
            .alpha(1f)
            .setDuration(300)
            .setListener(null)
            .start()
    }

    // переход в невидимый режим окна успешного ввода
    private fun hideRL() {
        if (currentLevel >= MAX_LEVEL) {
            endOfTheChapter()
            return
        }

        buttonToNextLevel.isClickable = false
        unblockAllButtons()

        currentLevel++
        clearWindow()
        setLetterButtons()
        reCreate()

        successWindow.animate() // анимирование исчезновения
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                successWindow.apply {
                    visibility = View.GONE
                    alpha = 1f
                }
            }
            .start()
    }

    // появление окна селекции
    fun showSelectionWindow(view: View) {
        selectionWindow.apply {
            visibility = View.VISIBLE
            alpha = 0f

            animate()
                .alpha(1f)
                .setDuration(300)
                .withStartAction {
                    buttonToSelection.isClickable = false
                    blockLetterButtons()
                }
                .start()
        }
    }

    // скрытие окна селекции
    fun hideSelectionWindow() {
        selectionWindow.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                selectionWindow.apply {
                    visibility = View.GONE
                    alpha = 1f
                    buttonToSelection.isClickable = true
                    unblockAllButtons()
                }
            }
            .start()
    }

    private fun enablePredictiveBackGesture() {
        onBackInvokedDispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT
        ) {
            // Анимация при свайпе назад
            finishAfterTransition()
        }
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
        balanceText.text = resources.getString(R.string.balance_format, currentBalance)
    }

    // отображение уровня игрока
    fun showUserLevel() {
        userLevelText.text = resources.getString(R.string.user_level_format, currentUserLevel)
    }

    // <<<СЕЛЕКЦИЯ>>>

    fun selection1(view: View) {
    }

    fun selection2(view: View) {
    }

    fun selection3(view: View) {
    }

    // <<<КОНЕЦ ГЛАВЫ>>>
    fun endOfTheChapter() {
        val message = "Chapter passed"

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()

        findViewById<View>(android.R.id.content).postDelayed({
            finish()
        }, 500)
    }

    // <<<РЕСТАРТЕР>>>
    fun reCreate() {
        listOfFootballClub = ArrayList()
        addDataToList()

        footballClubAdapter = FootballClubAdapter(listOfFootballClub)
        recyclerView.adapter = footballClubAdapter
        footballClubAdapter.filterLoans(false)
        toggleLoanButton.isChecked = false

        recyclerView.post {
            smoothScroller.targetPosition = recyclerView.adapter?.itemCount?.minus(1) ?: 0
            (recyclerView.layoutManager as LinearLayoutManager).startSmoothScroll(smoothScroller)
        }
    }

    // <<<ДЕСТРУКТОР>>>

    override fun onDestroy() {
        super.onDestroy()
        selectionBackCallback?.let {
            onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it)
        }
    }
}
