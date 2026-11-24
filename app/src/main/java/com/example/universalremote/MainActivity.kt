package com.example.universalremote

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.hardware.ConsumerIrManager
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private val bg = Color.rgb(13, 18, 32)
    private val panel = Color.rgb(28, 38, 58)
    private val accent = Color.rgb(88, 104, 240)
    private val prefs by lazy { getSharedPreferences("remote_prefs", Context.MODE_PRIVATE) }
    private var tab = "TV"
    private var tvBrand = "Marka seç"
    private var acBrand = "Marka seç"
    private var tvModel = ""
    private var acModel = ""
    private val model get() = if (tab == "TV") tvModel else acModel
    private val brand get() = if (tab == "TV") tvBrand else acBrand
    private var temp = 24
    private var mode = "Soğutma"
    private var fan = "Auto"
    private var swingEnabled = false
    private var ecoEnabled = false
    private var sleepEnabled = false
    private var timerEnabled = false
    private var ir: ConsumerIrManager? = null
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ir = getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        val profileLoad = IrProfileLoader.loadFromAssets(this)
        tab = prefs.getString("tab", "TV") ?: "TV"
        tvBrand = prefs.getString("tvBrand", "Marka seç") ?: "Marka seç"
        acBrand = prefs.getString("acBrand", "Marka seç") ?: "Marka seç"
        tvModel = prefs.getString("tvModel", "") ?: ""
        acModel = prefs.getString("acModel", "") ?: ""
        temp = prefs.getInt("temp", 24).coerceIn(16, 30)
        mode = prefs.getString("mode", "Soğutma") ?: "Soğutma"
        fan = prefs.getString("fan", "Auto") ?: "Auto"
        swingEnabled = prefs.getBoolean("swingEnabled", false)
        ecoEnabled = prefs.getBoolean("ecoEnabled", false)
        sleepEnabled = prefs.getBoolean("sleepEnabled", false)
        timerEnabled = prefs.getBoolean("timerEnabled", false)
        render()
        if (profileLoad.loaded > 0 || profileLoad.rejected > 0 || profileLoad.error != null) {
            Toast.makeText(this, "IR profilleri: ${profileLoad.loaded} yüklendi, ${profileLoad.rejected} reddedildi${profileLoad.error?.let { " • $it" } ?: ""}", Toast.LENGTH_LONG).show()
        }
    }

    private fun save() {
        prefs.edit().putString("tab", tab).putString("tvBrand", tvBrand).putString("acBrand", acBrand).putString("tvModel", tvModel).putString("acModel", acModel).putInt("temp", temp)
            .putString("mode", mode).putString("fan", fan).putBoolean("swingEnabled", swingEnabled)
            .putBoolean("ecoEnabled", ecoEnabled).putBoolean("sleepEnabled", sleepEnabled)
            .putBoolean("timerEnabled", timerEnabled).apply()
    }

    private fun render() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg); setPadding(dp(14), dp(10), dp(14), dp(10)) }
        root.addView(txt("UNIVERSAL REMOTE", 22f, Color.WHITE, true), full())
        root.addView(txt("TV • KLİMA • TEK UYGULAMA", 12f, Color.LTGRAY, true), full())
        root.addView(txt("Komutlar yalnızca doğrulanmış model profili varsa gönderilir", 11f, Color.rgb(255, 190, 120), true), full())
        status = txt(irStatus(), 12f, Color.rgb(255, 210, 120), true).apply { setPadding(dp(6), dp(8), dp(6), dp(8)) }
        root.addView(status, full())
        root.addView(btn("⚡ TEK TUŞ KONTROL", accent) { setupInfo() }, fixed(50))
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tabs.addView(btn("📺 TV", if (tab == "TV") accent else panel) { tab = "TV"; save(); render() }, weight())
        tabs.addView(btn("❄ KLİMA", if (tab == "KLİMA") accent else panel) { tab = "KLİMA"; save(); render() }, weight())
        root.addView(tabs, fixed(48))
        root.addView(txt("MARKA", 12f, Color.LTGRAY, false), full())
        val tvBrands = arrayOf("Marka seç", "Samsung", "LG", "Sony", "Philips", "Vestel", "Arçelik", "Beko", "TCL", "Xiaomi", "Hisense", "Panasonic", "Diğer")
        val acBrands = arrayOf("Marka seç", "Arçelik", "Beko", "Vestel", "Daikin", "Mitsubishi", "LG", "Samsung", "Bosch", "Toshiba", "Panasonic", "Diğer")
        val spinner = Spinner(this)
        val choices = if (tab == "TV") tvBrands else acBrands
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, choices)
        spinner.setSelection(choices.indexOf(brand).coerceAtLeast(0))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, position: Int, id: Long) { if (brand != choices[position]) { if (tab == "TV") tvBrand = choices[position] else acBrand = choices[position]; save() } }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        root.addView(spinner, fixed(48))
        root.addView(txt("MODEL / MODEL NUMARASI", 12f, Color.LTGRAY, false), full())
        val modelInput = EditText(this).apply {
            setSingleLine(true)
            hint = "Örn. cihaz model numarası"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setBackgroundColor(panel)
            textSize = 14f
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setText(model)
        }
        modelInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (tab == "TV") tvModel = s?.toString()?.trim() ?: "" else acModel = s?.toString()?.trim() ?: ""
                save()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        root.addView(modelInput, fixed(52))
        val profileHint = if (model.isBlank()) "Model girilmedi. Marka seçimi gerçek komut profili için yeterli değildir." else "Model kaydedildi: $model • Bu model için doğrulanmış komut profili henüz bulunmayabilir."
        root.addView(txt(profileHint, 10.5f, Color.rgb(255, 190, 120), true), full())
        val scroller = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(6), 0, dp(6)) }
        if (tab == "TV") tvPanel(body) else acPanel(body)
        scroller.addView(body); root.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(txt("Not: Marka seçimi tek başına cihaz kodunu doğrulamaz. Bilinmeyen profilde sinyal gönderilmez.", 10.5f, Color.GRAY, true), full())
        setContentView(root)
    }


    /** Sends a raw IR sequence only when a verified device profile supplies it.
     * Raw values are intentionally not guessed from a brand name. */
    private fun sendVerifiedIr(frequencyHz: Int, pattern: IntArray, actionLabel: String): Boolean {
        val manager = ir
        if (manager == null || !manager.hasIrEmitter()) {
            unavailable("IR donanımı yok: $actionLabel")
            return false
        }
        if (frequencyHz !in 30000..60000 || pattern.isEmpty() || pattern.size % 2 != 0 || pattern.any { it <= 0 }) {
            unavailable("Geçersiz/eksik IR profili: $actionLabel")
            return false
        }
        val supported = try {
            manager.carrierFrequencies.any { frequencyHz in it.minFrequency..it.maxFrequency }
        } catch (_: Exception) { false }
        if (!supported) {
            unavailable("Telefon IR frekans aralığı bu profile uygun değil: $actionLabel")
            return false
        }
        return try {
            manager.transmit(frequencyHz, pattern)
            Toast.makeText(this, "IR komutu gönderildi: $actionLabel", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Toast.makeText(this, "IR gönderimi başarısız: ${e.message ?: "bilinmeyen hata"}", Toast.LENGTH_LONG).show()
            false
        }
    }

    /** Dispatch only an exact, registered profile command. Brand-only selection is not enough. */
    private fun sendProfileCommand(deviceBrand: String, model: String, actionKey: String, actionLabel: String): Boolean {
        // Explicit brand is required so AC dispatch cannot accidentally use the TV selection.
        val profile = VerifiedIrProfiles.find(deviceBrand, model)
        val pattern = profile?.commands?.get(actionKey)
        if (profile == null || pattern == null) {
            unavailable("$actionLabel (bu marka/model için doğrulanmış kod yok)")
            return false
        }
        return sendVerifiedIr(profile.frequencyHz, pattern, actionLabel)
    }

    private fun setupInfo() {
        val hasIr = ir?.hasIrEmitter() == true
        val ranges = try {
            ir?.carrierFrequencies?.joinToString("\n") { "• ${it.minFrequency}–${it.maxFrequency} Hz" }
        } catch (_: Exception) { null }
        val details = if (hasIr && !ranges.isNullOrBlank()) "\n\nTelefonun bildirdiği IR frekans aralıkları:\n$ranges" else ""
        val message = if (hasIr) "Telefonunda IR vericisi var. ${brand} seçimi yapılsa bile model/protokol kodu doğrulanmadıkça sinyal gönderilmez. Otomatik marka seçimi tek başına yeterli değildir.$details" else "Bu telefonda dahili IR vericisi bulunamadı. IR kumandası için harici IR köprüsü gerekir. Wi-Fi kontrolü yalnızca desteklenen akıllı cihazlarda kullanılabilir."
        AlertDialog.Builder(this).setTitle("IR / Tek tuş tanılama").setMessage(message).setPositiveButton("Anladım", null).show()
    }


    private fun tvCommand(key: String, label: String) {
        if (tvBrand == "Marka seç" || tvModel.isBlank()) {
            unavailable("$label: önce TV markası ve tam model numarası girin")
            return
        }
        sendProfileCommand(tvBrand, tvModel, key, label)
    }

    private fun acCommand(key: String, label: String): Boolean {
        if (acBrand == "Marka seç" || acModel.isBlank()) {
            unavailable("$label: önce klima markası ve tam model numarası girin")
            return false
        }
        return sendProfileCommand(acBrand, acModel, key, label)
    }

    private fun tvPanel(c: LinearLayout) {
        c.addView(btn("⏻ TV GÜÇ", Color.rgb(180, 55, 68)) { tvCommand("TV_POWER", "TV güç") }, fixed(54))
        c.addView(txt("YÖN KONTROLÜ", 11f, Color.LTGRAY, true), full())
        c.addView(row(btn("▲", panel) { tvCommand("TV_UP", "Yukarı") }, spacer(), btn("HOME", panel) { tvCommand("TV_HOME", "Ana ekran") }), fixed(54))
        c.addView(row(btn("◀", panel) { tvCommand("TV_LEFT", "Sol") }, btn("OK", accent) { tvCommand("TV_OK", "Onay") }, btn("▶", panel) { tvCommand("TV_RIGHT", "Sağ") }), fixed(54))
        c.addView(row(btn("▼", panel) { tvCommand("TV_DOWN", "Aşağı") }, spacer(), btn("BACK", panel) { tvCommand("TV_BACK", "Geri") }), fixed(54))
        c.addView(row(btn("VOL −", panel) { tvCommand("TV_VOL_DOWN", "Ses azalt") }, btn("VOL +", panel) { tvCommand("TV_VOL_UP", "Ses artır") }), fixed(54))
        c.addView(row(btn("CH −", panel) { tvCommand("TV_CH_DOWN", "Kanal azalt") }, btn("CH +", panel) { tvCommand("TV_CH_UP", "Kanal artır") }), fixed(54))
        c.addView(row(btn("SOURCE", panel) { tvCommand("TV_SOURCE", "Kaynak") }, btn("MUTE", panel) { tvCommand("TV_MUTE", "Sessiz") }), fixed(54))
    }

    private fun acPanel(c: LinearLayout) {
        c.addView(btn("⏻ KLİMA GÜÇ", Color.rgb(180, 55, 68)) { acCommand("AC_POWER", "Klima güç") }, fixed(54))
        c.addView(txt("${temp}°C", 44f, Color.WHITE, true).apply { setPadding(0, dp(12), 0, dp(12)) }, full())
        c.addView(row(
            btn("−", panel) { if (temp > 16) { val target = temp - 1; if (acCommand("AC_TEMP_$target", "Sıcaklık $target derece")) { temp = target; save(); render() } } },
            btn("+", panel) { if (temp < 30) { val target = temp + 1; if (acCommand("AC_TEMP_$target", "Sıcaklık $target derece")) { temp = target; save(); render() } } }
        ), fixed(58))
        c.addView(txt("MOD: $mode", 12f, Color.LTGRAY, true), full())
        c.addView(row("❄ Soğutma", "☀ Isıtma", "💧 Nem alma", "AUTO"), fixed(50))
        c.addView(txt("FAN: $fan", 12f, Color.LTGRAY, true), full())
        c.addView(row("Auto", "Düşük", "Orta", "Yüksek"), fixed(50))
        c.addView(txt("EK KONTROLLER", 11f, Color.LTGRAY, true), full())
        c.addView(row(
            if (swingEnabled) "Swing ✓" else "Swing",
            if (ecoEnabled) "Eco ✓" else "Eco",
            if (sleepEnabled) "Sleep ✓" else "Sleep",
            if (timerEnabled) "Timer ✓" else "Timer"
        ), fixed(50))
    }

    private fun row(vararg labels: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
        labels.forEach { label -> addView(btn(label, panel) { selectAc(label) }, weight()) }
    }
    private fun selectAc(choice: String) {
        val commandKey = when (choice) {
            "❄ Soğutma" -> "AC_MODE_COOL"
            "☀ Isıtma" -> "AC_MODE_HEAT"
            "💧 Nem alma" -> "AC_MODE_DRY"
            "AUTO" -> "AC_MODE_AUTO"
            "Auto", "Düşük", "Orta", "Yüksek" -> "AC_FAN_" + when (choice) { "Auto" -> "AUTO"; "Düşük" -> "LOW"; "Orta" -> "MEDIUM"; else -> "HIGH" }
            "Swing", "Swing ✓" -> if (!swingEnabled) "AC_SWING_ON" else "AC_SWING_OFF"
            "Eco", "Eco ✓" -> if (!ecoEnabled) "AC_ECO_ON" else "AC_ECO_OFF"
            "Sleep", "Sleep ✓" -> if (!sleepEnabled) "AC_SLEEP_ON" else "AC_SLEEP_OFF"
            "Timer", "Timer ✓" -> if (!timerEnabled) "AC_TIMER_ON" else "AC_TIMER_OFF"
            else -> null
        }
        if (commandKey == null || !acCommand(commandKey, choice)) return
        when (choice) {
            "❄ Soğutma" -> mode = "Soğutma"
            "☀ Isıtma" -> mode = "Isıtma"
            "💧 Nem alma" -> mode = "Nem alma"
            "AUTO" -> mode = "Auto"
            "Auto", "Düşük", "Orta", "Yüksek" -> fan = choice
            "Swing", "Swing ✓" -> swingEnabled = !swingEnabled
            "Eco", "Eco ✓" -> ecoEnabled = !ecoEnabled
            "Sleep", "Sleep ✓" -> sleepEnabled = !sleepEnabled
            "Timer", "Timer ✓" -> timerEnabled = !timerEnabled
        }
        save(); render()
    }
    private fun row(vararg views: View) = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; views.forEach { addView(it, weight()) } }
    private fun spacer() = View(this).apply { alpha = 0f }
    private fun btn(caption: String, color: Int, action: () -> Unit) = Button(this).apply { text = caption; setTextColor(Color.WHITE); setBackgroundColor(color); textSize = 12f; setOnClickListener { action() } }
    private fun txt(text: String, size: Float, color: Int, centered: Boolean) = TextView(this).apply { this.text = text; setTextColor(color); textSize = size; gravity = if (centered) Gravity.CENTER else Gravity.START; setPadding(dp(5), dp(5), dp(5), dp(5)) }
    private fun unavailable(action: String) { Toast.makeText(this, "$action: doğrulanmış cihaz profili bulunmadı; sinyal gönderilmedi.", Toast.LENGTH_SHORT).show() }
    private fun irStatus() = if (ir?.hasIrEmitter() == true) "IR algılandı • doğrulanmış cihaz kodu gerekli" else "IR yok • desteklenen Wi-Fi cihazları / harici IR köprüsü"
    private fun full() = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(3) }
    private fun fixed(h: Int) = LinearLayout.LayoutParams(-1, dp(h)).apply { bottomMargin = dp(3) }
    private fun weight() = LinearLayout.LayoutParams(0, dp(50), 1f).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
