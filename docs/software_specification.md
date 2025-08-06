# Android屋内測位アプリケーション ソフトウェア仕様書

**バージョン:** 1.0  
**日付:** 2025-08-05  
**作成者:** JetBrains Junie

## 1. はじめに

### 1.1 目的
本ドキュメントは、BLEビーコンとAndroidスマートフォンのセンサーを利用した高精度な屋内測位アプリケーションの開発に関するソフトウェア仕様を定義します。

### 1.2 対象読者
- アプリケーション開発者
- システム設計者
- テストエンジニア
- プロジェクト管理者

### 1.3 スコープ
本仕様書は以下の内容を含みます：
- システム概要と目標
- 機能要件
- 技術要件と制約
- システムアーキテクチャとコンポーネント
- アルゴリズムとデータ処理方法
- データ管理と保存
- テストと評価方法

## 2. システム概要

### 2.1 システムの目的
屋内環境において、GPSが利用できない場所でユーザーの位置を高精度に特定し、リアルタイムで表示するAndroidアプリケーションを開発する。

### 2.2 主要機能
- BLEビーコンからの信号受信と距離推定
- 複数のビーコン情報を用いた三角測量による位置計算
- スマートフォンセンサー（加速度計、ジャイロスコープ、地磁気センサー）を用いた歩行者自律航法（PDR）
- センサーフュージョンによる位置精度の向上
- 屋内マップ上でのユーザー位置のリアルタイム表示
- 位置精度の可視化
- バッテリー消費を最適化する動的スキャン制御

### 2.3 ユーザー
- 屋内施設の訪問者
- 施設管理者
- システム管理者

## 3. 機能要件

### 3.1 BLEビーコンスキャン
- **FR-1.1:** アプリケーションは周囲のBLEビーコンを定期的にスキャンする機能を提供すること
- **FR-1.2:** 各ビーコンのMACアドレス、RSSI値、タイムスタンプを記録すること
- **FR-1.3:** 一定時間（5秒以上）更新のないビーコンを無効とみなす機能を実装すること

### 3.2 距離推定
- **FR-2.1:** RSSIの値から各ビーコンまでの距離を推定する機能を提供すること
- **FR-2.2:** RSSIの揺らぎを抑制するためのフィルタリング機能を実装すること
- **FR-2.3:** 環境係数Nを調整可能にし、現場でのキャリブレーションを可能にすること

### 3.3 位置計算
- **FR-3.1:** 複数のビーコンからの距離情報を用いて、ユーザーの位置を計算する機能を提供すること
- **FR-3.2:** 最小二乗法などの堅牢なアルゴリズムを実装すること
- **FR-3.3:** ビーコン配置の幾何学的精度劣化（GDOP）を考慮した位置計算を行うこと

### 3.4 歩行者自律航法（PDR）
- **FR-4.1:** 加速度センサーを用いた歩行検出機能を実装すること
- **FR-4.2:** ジャイロセンサーを主体とした歩行方向推定機能を実装すること
- **FR-4.3:** 歩幅を動的に推定する機能を実装すること
- **FR-4.4:** PDRによる位置推定機能を提供すること

### 3.5 センサーフュージョン
- **FR-5.1:** BLE測位とPDRの結果を統合する機能を提供すること
- **FR-5.2:** 重み付け平均による滑らかな位置補正を実装すること
- **FR-5.3:** 測位信頼度に基づく動的な重み付けを行うこと

### 3.6 マップ表示
- **FR-6.1:** 屋内マップ上にユーザーの現在位置をリアルタイムで表示する機能を提供すること
- **FR-6.2:** 位置の不確実性を半透明の円で表現する機能を実装すること
- **FR-6.3:** マップの拡大・縮小・移動機能を提供すること

### 3.7 設定と構成管理
- **FR-7.1:** ビーコンの座標やマップ画像を外部JSONファイルから読み込む機能を提供すること
- **FR-7.2:** アプリケーションの各種パラメータ（スキャン間隔、フィルタ設定など）を設定できる機能を提供すること
- **FR-7.3:** ビーコンの死活監視機能を実装すること

### 3.8 デバッグと分析
- **FR-8.1:** センサーデータとBLEスキャン結果をログファイルに記録する機能を提供すること
- **FR-8.2:** 記録したログファイルを再生し、測位アルゴリズムをテストできる機能を提供すること
- **FR-8.3:** デバッグ情報を画面上にオーバーレイ表示する機能を提供すること

## 4. 技術要件と制約

### 4.1 プラットフォーム
- **TR-1.1:** Android 8.0（API レベル 26）以上をサポートすること
- **TR-1.2:** Kotlin言語を使用して開発すること
- **TR-1.3:** Android Jetpack（特にLifecycle、ViewModel、LiveData）を活用すること

### 4.2 ハードウェア要件
- **TR-2.1:** BLEをサポートするAndroidデバイスで動作すること
- **TR-2.2:** 加速度センサー、ジャイロスコープ、地磁気センサーを搭載したデバイスで完全機能を提供すること
- **TR-2.3:** センサーが一部欠けている場合は、利用可能なセンサーのみで動作する縮小機能を提供すること

### 4.3 パフォーマンス
- **TR-3.1:** バッテリー消費を最小限に抑えるため、静止検出時にはセンサーリスナーとBLEスキャンを間欠動作させること
- **TR-3.2:** 位置更新は最低でも1秒に1回の頻度で行うこと
- **TR-3.3:** アプリケーションのメモリ使用量は100MB以下に抑えること

### 4.4 セキュリティ
- **TR-4.1:** ユーザーの位置情報は端末内でのみ処理し、外部に送信する場合は明示的な同意を得ること
- **TR-4.2:** ログファイルには個人を特定できる情報を含めないこと
- **TR-4.3:** Bluetoothの権限要求は適切に行い、ユーザーに目的を説明すること

## 5. システムアーキテクチャ

### 5.1 全体アーキテクチャ
アプリケーションはMVVMアーキテクチャパターンに従い、以下の主要コンポーネントで構成されます：

1. **UI層**
   - Activity/Fragment: ユーザーインターフェースを提供
   - ViewModel: UIとデータモデルの橋渡し役
   - LiveData: データの変更をUIに通知

2. **ドメイン層**
   - UseCase: ビジネスロジックを実装
   - Repository: データソースへのアクセスを抽象化

3. **データ層**
   - Repository実装: データの取得と保存を担当
   - DataSource: ローカルデータベース、センサー、BLEスキャナーなどのデータソース

### 5.2 主要コンポーネント

#### 5.2.1 BLEスキャナーコンポーネント
- BLEスキャンの開始/停止を制御
- スキャン結果の処理とフィルタリング
- ビーコンの有効期限管理

#### 5.2.2 距離推定コンポーネント
- RSSIフィルタリング
- 距離計算アルゴリズム
- キャリブレーションパラメータの管理

#### 5.2.3 位置計算コンポーネント
- 三角測量アルゴリズム
- 最小二乗法による位置推定
- GDOP計算と精度評価

#### 5.2.4 PDRコンポーネント
- 歩行検出
- 方向推定
- 歩幅推定
- 相対位置追跡

#### 5.2.5 センサーフュージョンコンポーネント
- BLE測位とPDRの統合
- 重み付け計算
- 位置の平滑化

#### 5.2.6 マップ管理コンポーネント
- マップ画像の読み込みと表示
- 座標変換（物理座標⇔画面座標）
- ビーコン配置の管理

#### 5.2.7 データロギングコンポーネント
- センサーデータの記録
- BLEスキャン結果の記録
- ログファイルの管理と再生

### 5.3 データフロー
1. BLEスキャナーがビーコン信号を受信
2. 距離推定コンポーネントがRSSIから距離を計算
3. 位置計算コンポーネントが複数のビーコン距離から位置を推定
4. PDRコンポーネントがセンサーデータから相対位置変化を計算
5. センサーフュージョンコンポーネントがBLE測位とPDR結果を統合
6. 統合された位置情報がViewModelを通じてUIに反映
7. 同時にデータロギングコンポーネントが全データを記録

## 6. アルゴリズムとデータ処理

### 6.1 RSSIフィルタリング
```kotlin
// 移動平均フィルタの実装例
private val rssiHistory = mutableMapOf<String, MutableList<Int>>()
private const val RSSI_HISTORY_SIZE = 5

fun getFilteredRssi(address: String, newRssi: Int): Double {
    val history = rssiHistory.getOrPut(address) { mutableListOf() }
    history.add(newRssi)
    if (history.size > RSSI_HISTORY_SIZE) {
        history.removeAt(0)
    }
    return history.average()
}
```

### 6.2 距離計算
```kotlin
// RSSIから距離を計算する関数
fun calculateDistance(rssi: Double, txPower: Int, environmentalFactor: Double): Double {
    return Math.pow(10.0, (txPower - rssi) / (10 * environmentalFactor))
}
```

### 6.3 位置計算（最小二乗法）
最小二乗法による位置計算のアルゴリズムを実装します。N個のビーコン (xi, yi) からの距離 di の観測方程式 (x - xi)^2 + (y - yi)^2 = di^2 を連立させ、全体の誤差が最小になるような (x, y) を求めます。

### 6.4 歩行検出
```kotlin
// 歩行検出アルゴリズムの実装例
private var lastPeakTime: Long = 0
private var lastValleyTime: Long = 0
private var isAscending = false
private var lastValue = 0f
private const val MIN_STEP_INTERVAL_MS = 250 // 最小歩行間隔（0.25秒）
private const val MAX_STEP_INTERVAL_MS = 2000 // 最大歩行間隔（2秒）
private const val THRESHOLD_PEAK = 12.0f // ピーク検出しきい値
private const val THRESHOLD_VALLEY = 8.0f // 谷検出しきい値

fun detectStep(value: Float, timestamp: Long): Boolean {
    var stepDetected = false
    
    if (value > lastValue && !isAscending) {
        isAscending = true
    } else if (value < lastValue && isAscending) {
        isAscending = false
        
        // 谷を検出
        if (lastValue < THRESHOLD_VALLEY) {
            lastValleyTime = timestamp
        }
    }
    
    // ピークを検出
    if (isAscending && value > THRESHOLD_PEAK && lastValue <= THRESHOLD_PEAK) {
        val timeSinceLastPeak = timestamp - lastPeakTime
        val timeSinceLastValley = timestamp - lastValleyTime
        
        // 適切な時間間隔で、かつ谷の後にピークが来た場合に歩行と判定
        if (timeSinceLastPeak > MIN_STEP_INTERVAL_MS && 
            timeSinceLastPeak < MAX_STEP_INTERVAL_MS &&
            timeSinceLastValley < timeSinceLastPeak) {
            stepDetected = true
            lastPeakTime = timestamp
        }
    }
    
    lastValue = value
    return stepDetected
}
```

### 6.5 方向推定（相補フィルタ）
```kotlin
// 相補フィルタによる方向推定の実装例
private var currentHeading = 0.0f // 現在の方位（ラジアン）
private var lastTimestamp: Long = 0
private const val ALPHA = 0.98f // ジャイロの重み（0.0〜1.0）

fun updateHeading(gyroZ: Float, compassHeading: Float, timestamp: Long) {
    if (lastTimestamp == 0L) {
        lastTimestamp = timestamp
        return
    }
    
    val dt = (timestamp - lastTimestamp) * 1.0e-9f // nsからsへ変換
    lastTimestamp = timestamp
    
    // ジャイロによる方位変化の計算
    val gyroHeadingChange = gyroZ * dt
    
    // 相補フィルタによる統合
    currentHeading = ALPHA * (currentHeading + gyroHeadingChange) + (1 - ALPHA) * compassHeading
    
    // 0〜2πの範囲に正規化
    currentHeading = normalizeAngle(currentHeading)
}

private fun normalizeAngle(angle: Float): Float {
    var result = angle
    while (result < 0) result += 2 * Math.PI.toFloat()
    while (result >= 2 * Math.PI) result -= 2 * Math.PI.toFloat()
    return result
}
```

### 6.6 センサーフュージョン
```kotlin
// BLE測位とPDRの結果を統合する関数
fun fusePositions(blePosition: Point, pdrPosition: Point, bleConfidence: Float): Point {
    // 信頼度に基づく重み付け
    val alpha = constrain(bleConfidence, 0.0f, 1.0f)
    
    // 重み付け平均による位置の統合
    val fusedX = (1 - alpha) * pdrPosition.x + alpha * blePosition.x
    val fusedY = (1 - alpha) * pdrPosition.y + alpha * blePosition.y
    
    return Point(fusedX, fusedY)
}

private fun constrain(value: Float, min: Float, max: Float): Float {
    return Math.max(min, Math.min(max, value))
}
```

## 7. データ管理と保存

### 7.1 設定データ
アプリケーションの設定データはSharedPreferencesに保存します。

```kotlin
// 設定の保存例
fun saveSettings(context: Context, scanInterval: Long, environmentalFactor: Double) {
    val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    prefs.edit()
        .putLong("scan_interval", scanInterval)
        .putFloat("environmental_factor", environmentalFactor.toFloat())
        .apply()
}

// 設定の読み込み例
fun loadSettings(context: Context): Pair<Long, Double> {
    val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    val scanInterval = prefs.getLong("scan_interval", 1000) // デフォルト1秒
    val environmentalFactor = prefs.getFloat("environmental_factor", 2.0f).toDouble() // デフォルト2.0
    return Pair(scanInterval, environmentalFactor)
}
```

### 7.2 マップとビーコン構成
マップとビーコンの構成情報はJSONファイルで管理します。

```json
{
  "map_name": "Office_Floor_1",
  "image_file": "floor1.png",
  "width_meters": 50.0,
  "height_meters": 30.0,
  "beacons": [
    { "mac_address": "00:11:22:33:44:AA", "x": 2.5, "y": 5.0, "tx_power": -59 },
    { "mac_address": "00:11:22:33:44:BB", "x": 45.0, "y": 15.0, "tx_power": -61 },
    { "mac_address": "00:11:22:33:44:CC", "x": 25.0, "y": 28.0, "tx_power": -60 }
  ]
}
```

### 7.3 ログデータ
センサーデータとBLEスキャン結果はCSVファイルに記録します。

```kotlin
// ログファイルへの書き込み例
fun logData(timestamp: Long, sensorData: SensorData, bleData: List<BleData>) {
    val logFile = File(getExternalFilesDir(null), "positioning_log_${System.currentTimeMillis()}.csv")
    
    if (!logFile.exists()) {
        // ヘッダー行を書き込む
        logFile.writeText("Timestamp,AccX,AccY,AccZ,GyroX,GyroY,GyroZ,MagX,MagY,MagZ,BleAddress,BleRssi\n")
    }
    
    // センサーデータの書き込み
    val sensorLine = "$timestamp,${sensorData.accX},${sensorData.accY},${sensorData.accZ}," +
                     "${sensorData.gyroX},${sensorData.gyroY},${sensorData.gyroZ}," +
                     "${sensorData.magX},${sensorData.magY},${sensorData.magZ}"
    
    // BLEデータがある場合は各行に書き込み、なければ空欄
    if (bleData.isEmpty()) {
        logFile.appendText("$sensorLine,,\n")
    } else {
        for (ble in bleData) {
            logFile.appendText("$sensorLine,${ble.address},${ble.rssi}\n")
        }
    }
}
```

## 8. テストと評価

### 8.1 単体テスト
各コンポーネントの機能を個別にテストします。

- BLEスキャナーコンポーネントのテスト
- 距離推定アルゴリズムのテスト
- 位置計算アルゴリズムのテスト
- PDRコンポーネントのテスト
- センサーフュージョンアルゴリズムのテスト

### 8.2 統合テスト
複数のコンポーネントを組み合わせた統合テストを実施します。

- BLEスキャンから距離推定、位置計算までの一連の流れのテスト
- PDRとセンサーフュージョンの統合テスト
- UIとバックエンドロジックの統合テスト

### 8.3 フィールドテスト
実際の環境でのテストを実施し、システムの精度と性能を評価します。

#### 8.3.1 グラウンドトゥルース（正解経路）の準備
床にメジャーで線を引き、その上を歩くなどして、時刻ごとの正確な位置がわかるテストデータを作成します。

#### 8.3.2 評価指標
アプリが推定した経路と正解経路を比較し、RMSE（二乗平均平方根誤差）を計算します。

```
RMSE = √(1/n * Σ((xi - x̂i)² + (yi - ŷi)²))
```

ここで、(xi, yi)は正解位置、(x̂i, ŷi)は推定位置、nはサンプル数です。

#### 8.3.3 キャリブレーション
- TxPower: 各ビーコンから1m離れた場所でRSSIを測定し、その平均値を求めます。
- 環境係数N: 現場で既知の距離を歩き、推定距離と一致するように調整します。

## 9. 運用とメンテナンス

### 9.1 ビーコンの死活監視
ビーコンの電池切れや故障を検出するため、最後に検出してから24時間以上経過したビーコンを「応答なし」と判定し、管理者に通知します。

### 9.2 再キャリブレーションのタイミング
以下の場合に再キャリブレーションを実施します：

- 物理的なレイアウト変更: オフィスの大規模な模様替え、新しいパーテーションや大型什器の設置など
- パフォーマンスの定量的劣化: RMSEが初期のベースラインから20%以上悪化した場合
- 定期メンテナンス: 半年に一度の定期的な精度チェック

### 9.3 バージョンアップと機能拡張
アプリケーションの継続的な改善のため、以下の計画を立てます：

- バグ修正とパフォーマンス最適化
- 新しいセンサーフュージョンアルゴリズムの導入
- ユーザーフィードバックに基づくUI/UX改善
- 新しいAndroid APIへの対応

## 10. 付録

### 10.1 用語集
- **BLE (Bluetooth Low Energy)**: 低消費電力のBluetooth規格
- **RSSI (Received Signal Strength Indicator)**: 受信信号強度指標
- **PDR (Pedestrian Dead Reckoning)**: 歩行者自律航法
- **GDOP (Geometric Dilution of Precision)**: 幾何学的精度劣化
- **センサーフュージョン**: 複数のセンサーデータを統合する技術

### 10.2 参考文献
- Android Developer Documentation
- Bluetooth Core Specification
- Indoor Positioning Systems: A Survey (論文)
- Sensor Fusion Algorithms for Orientation Estimation (論文)

### 10.3 改訂履歴
- バージョン 1.0 (2025-08-05): 初版作成