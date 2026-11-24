# Universal Remote — mevcut proje ve doğrulama notları

Bu paket Android Studio ile açılmak üzere hazırlanmış bir Android/Kotlin proje kaynağıdır. Kurulabilir APK değildir.

## Bu çalışma turunda yapılan gerçek kod değişiklikleri
- TV güç, yön, OK, Home, Back, ses, kanal, kaynak ve sessiz kontrolleri `sendProfileCommand` üzerinden TV marka + model profiline bağlandı.
- Klima güç, sıcaklık, mod, fan, Swing, Eco, Sleep ve Timer kontrolleri klima marka + model profiline bağlandı.
- Önemli durum düzeltmesi: Klima sıcaklığı/modu/fan/toggle arayüz durumu, komut başarıyla gönderilmedikçe artık değiştirilmez. Böylece profil yokken ekranda klima gerçekten değişmiş gibi gösterilmez.
- TV ve klima için ayrı marka/model alanları kullanılıyor; komut yönlendirmesi diğer sekmenin marka/model bilgisini kullanmıyor.
- Eksik profil veya komut anahtarı olduğunda IR gönderilmiyor.
- Ekrandaki açıklama, profil bulunduğunda komut gönderilebileceğini doğru ifade edecek şekilde düzeltildi.

## Profil komut anahtarları
TV: `TV_POWER`, `TV_UP`, `TV_DOWN`, `TV_LEFT`, `TV_RIGHT`, `TV_OK`, `TV_HOME`, `TV_BACK`, `TV_VOL_DOWN`, `TV_VOL_UP`, `TV_CH_DOWN`, `TV_CH_UP`, `TV_SOURCE`, `TV_MUTE`.

Klima: `AC_POWER`, `AC_TEMP_16` … `AC_TEMP_30`, `AC_MODE_COOL`, `AC_MODE_HEAT`, `AC_MODE_DRY`, `AC_MODE_AUTO`, `AC_FAN_AUTO`, `AC_FAN_LOW`, `AC_FAN_MEDIUM`, `AC_FAN_HIGH`, `AC_SWING_ON/OFF`, `AC_ECO_ON/OFF`, `AC_SLEEP_ON/OFF`, `AC_TIMER_ON/OFF`.

## Doğrulama durumu
- `IrProfile.kt` bağımsız Kotlin derleyicisiyle derlendi; bu turda boş model, geçersiz frekans, bozuk darbe dizisi, geçerli profil kaydı, büyük/küçük harf duyarsız eşleşme ve yanlış model eşleşmesi dahil 7 kontrol geçti.
- Geçersiz frekans, boş model, bozuk IR dizisi, geçerli profil kaydı ve büyük/küçük harf duyarsız tam eşleşme için 6 kontrol geçti.
- Tam Android uygulaması bu ortamda derlenemedi: Android SDK (`android.jar`) ve Gradle komutu mevcut değil.
- Paket içinde doğrulanmış üretici/model IR kodları yoktur. Bu nedenle gerçek TV/klima kontrolü, cihaza uygun doğrulanmış profil eklenmeden çalışmaz.
- Wi-Fi keşfi ve marka özel ağ protokolleri uygulanmış değildir.
- APK ve fiziksel cihaz testi yapılmamıştır.

## Geliştirici notu
IR kodları tahmin edilerek eklenmemelidir. Her profil, tam model için güvenilir biçimde doğrulanmış taşıyıcı frekansı ve işaret/boşluk dizileri içermelidir. Klima protokolleri çoğunlukla tam durum çerçevesi gerektirebilir; yalnızca tek bir tuş kodu gibi varsayılmamalıdır.


## Model bazlı IR profil dosyası

Uygulama açılışta `app/src/main/assets/ir_profiles.json` dosyasını okur. Şema örneği:

```json
{
  "schemaVersion": 1,
  "profiles": [
    {
      "brand": "ÖRNEK_MARKA",
      "model": "TAM_MODEL_KODU",
      "frequencyHz": 38000,
      "commands": {
        "TV_POWER": [9000, 4500, 560, 560]
      }
    }
  ]
}
```

Yukarıdaki sayılar yalnızca JSON biçimini göstermek için örnektir; gerçek cihaza ait doğrulanmış IR zamanlamaları değildir ve çalışır profil olarak kullanılmamalıdır. `profiles` dosyası varsayılan olarak boştur. Yalnızca üretici dokümanı, güvenilir protokol verisi veya ölçülmüş cihaz testiyle doğrulanmış marka/model/komut verileri eklenmelidir. Her komut dizisi çift sayıda pozitif mikrosaniye darbe/boşluk değeri içermeli; taşıyıcı frekansı 30–60 kHz aralığında olmalıdır.


## 2026-11-24 profil güvenliği güncellemesi

- IR profil kayıt defteri eşzamanlı erişime karşı korundu; aynı marka/model ikinci kez sessizce üzerine yazılmaz.
- Profil komut anahtarları, frekans ve darbe süreleri doğrulanır.
- JSON yükleyici bozuk/tek sayılı/boş veya sayısal olmayan komut dizisini profil bazında reddeder.
- Profil bulunmayan cihazlara yine sinyal gönderilmez.

Bu güncelleme kaynak kod ve statik kontrollerle incelenmiştir. Bu ortamda Android SDK/Gradle bulunmadığından APK derlemesi ve cihaz testi yapılmış değildir. `ir_profiles.json` bilerek boş bırakılmıştır; gerçek model kodları doğrulanmadan eklenmeyecektir.

### Son profil yükleme güvenliği güncellemesi
- Profil dosyası artık önce bütünüyle doğrulanıyor; bir kayıt hatalıysa dosyadaki diğer profiller kısmen etkinleştirilmiyor.
- Profil kayıt defterine toplu ekleme atomik: mevcut kayıtla veya aynı dosyadaki başka kayıtla çakışma varsa hiçbir kayıt eklenmiyor.
- Bu değişiklikler cihaz protokol kodu eklemez; `ir_profiles.json` boş olduğu sürece gerçek TV/klima komutu gönderilemez.
- Bu çalışma ortamında Android SDK/Gradle bulunmadığından APK derlemesi yapılamadı. Android Studio üzerinde derleme ve fiziksel cihaz testi hâlâ gerekli.

## 2026-11-24 registry hardening
- `VerifiedIrProfiles.find()` now returns defensive copies of command arrays. This prevents callers from mutating the registry's internally stored IR patterns after lookup.
- Added `tools/ProfileRegistrySmokeTest.kt`, a JVM smoke test covering registration-copy isolation, lookup-copy isolation, case/whitespace-insensitive exact model lookup, duplicate rejection, and invalid command-key rejection.
- Verified locally with `kotlinc` and Java: PASS.
- Android APK build remains unverified: this environment has Java/Kotlin compiler but no Gradle executable or Android SDK/android.jar. No APK is claimed.

## Build preflight
Run `bash tools/check-build-environment.sh` before attempting an APK build. It checks Java, Gradle, and Android SDK Platform 35 and exits non-zero when any requirement is missing. This environment currently has Java 21 and Kotlin compiler, but no Gradle executable and no Android SDK `android.jar`; therefore APK generation cannot be completed here. The JVM registry smoke test is available as source at `tools/ProfileRegistrySmokeTest.kt` and the last verified run printed PASS.


## Android build configuration correction
- Updated Android Gradle Plugin from 8.5.2 to 8.6.1 to align the build plugin with the project's compileSdk 35 configuration.
- Updated the preflight script to report the Gradle 8.7+ requirement explicitly.
- This is a build-configuration correction only; no APK was produced in this environment because Gradle and Android SDK Platform 35 are absent.

## Reproducible local checks

Run the pure-Kotlin profile registry checks with:

```bash
./tools/run-profile-tests.sh
```

This compiles `IrProfile.kt` with the installed Kotlin compiler and runs the registry smoke tests. It does **not** compile the Android app or validate IR behavior on hardware.

Before building the APK, run:

```bash
./tools/check-build-environment.sh
```

The Android build requires JDK 17+, Gradle 8.7+, and Android SDK Platform 35. In the current work environment, Java 21 is present, but Gradle and Android SDK Platform 35 are absent; therefore `APK_BUILD_READY=NO` and no APK can be produced here yet.

## Cloud APK build workflow

A GitHub Actions workflow is included at `.github/workflows/android-apk.yml`. After this project is pushed to a GitHub repository, open **Actions → Build Android APK → Run workflow**. The workflow installs JDK 17, Android SDK Platform 35, Build Tools 35.0.0 and Gradle 8.7; runs the JVM profile tests; then attempts `assembleDebug`. On success, download the `UniversalRemote-debug-apk` artifact from that workflow run.

This workflow has been added to the source package but has **not** been executed from this environment. It is a route to a reproducible remote build, not proof that the Android app compiles. Any build errors reported by GitHub Actions must be fixed before the APK is considered ready. The debug APK will still contain an empty `ir_profiles.json` and therefore will not control real appliances until verified model-specific IR data is supplied.

## Güncellenmiş CI derleme iş akışı

`.github/workflows/android-apk.yml` artık Android SDK lisans kabulünü, platform-tools kurulumunu, gerçek Gradle/SDK sürüm kontrolünü ve başarısız derlemede tanılama raporu yüklemeyi içerir. GitHub Actions üzerinde henüz çalıştırılmadı; dolayısıyla bu düzenleme derlemenin başarılı olduğunu kanıtlamaz.

### APK almak için gereken son kullanıcı adımı

1. ZIP'i bilgisayarda açın ve içindeki proje klasörünü GitHub'da oluşturduğunuz boş depoya yükleyin.
2. GitHub deposunda **Actions → Build Android APK → Run workflow** seçin (veya `main` dalına push edin).
3. İş akışı yeşil tamamlanırsa çalıştırmanın **Artifacts** bölümünden `UniversalRemote-debug-apk` dosyasını indirin.
4. APK'yı Android telefona aktarın. Android'in bilinmeyen kaynaklardan yükleme iznini yalnızca bu dosya için verin.

Bu APK derlense bile `ir_profiles.json` varsayılan olarak boştur. Bu nedenle uygulama kurulabilir olsa da doğrulanmış model profili eklenene kadar TV/klima kumandası olarak gerçek sinyal gönderemez. Wi-Fi cihaz keşfi de bu pakette uygulanmış değildir.
