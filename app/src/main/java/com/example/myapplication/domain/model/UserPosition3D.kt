package com.example.myapplication.domain.model

/**
 * 3Dユーザー位置
 */
data class UserPosition3D(
    /**
     * X座標（メートル）
     */
    val x: Float,
    
    /**
     * Y座標（メートル）
     */
    val y: Float,
    
    /**
     * Z座標（メートル）- 高さ情報
     */
    val z: Float,
    
    /**
     * 位置精度（メートル）
     */
    val accuracy: Float,
    
    /**
     * タイムスタンプ（ミリ秒）
     */
    val timestamp: Long = System.currentTimeMillis(),
    
    /**
     * 位置データのソース
     */
    val source: PositionSource = PositionSource.UNKNOWN,
    
    /**
     * 信頼度（0.0-1.0）
     */
    val confidence: Float = 0.5f,
    
    /**
     * フロア番号
     */
    val floor: Int = 1,
    
    /**
     * X軸の不確実性（標準偏差、メートル）
     */
    val sigmaX: Float? = null,
    
    /**
     * Y軸の不確実性（標準偏差、メートル）
     */
    val sigmaY: Float? = null,
    
    /**
     * Z軸の不確実性（標準偏差、メートル）
     */
    val sigmaZ: Float? = null,
    
    /**
     * 方位の不確実性（標準偏差、ラジアン）
     */
    val sigmaTheta: Float? = null,
    
    /**
     * 方位角（度、0-360）
     */
    val heading: Float? = null,
    
    /**
     * 移動速度（メートル/秒）
     */
    val velocity: Float? = null
) {
    /**
     * 2Dユーザー位置に変換（Z軸を無視）
     */
    fun to2D(): UserPosition {
        return UserPosition(
            x = x,
            y = y,
            accuracy = accuracy,
            timestamp = timestamp,
            source = source,
            confidence = confidence,
            sigmaX = sigmaX,
            sigmaY = sigmaY,
            sigmaTheta = sigmaTheta
        )
    }
    
    /**
     * 3D座標に変換
     */
    fun toCoordinate3D(): Coordinate3D {
        return Coordinate3D(
            x = x,
            y = y,
            z = z,
            accuracy = accuracy,
            timestamp = timestamp
        )
    }
    
    /**
     * 別の3D位置との距離を計算
     */
    fun distanceTo(other: UserPosition3D): Float {
        val dx = this.x - other.x
        val dy = this.y - other.y
        val dz = this.z - other.z
        return Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
    }
    
    /**
     * 2D距離を計算（Z軸を無視）
     */
    fun distance2DTo(other: UserPosition3D): Float {
        val dx = this.x - other.x
        val dy = this.y - other.y
        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
    
    /**
     * 高さ差を計算
     */
    fun heightDifference(other: UserPosition3D): Float {
        return this.z - other.z
    }
    
    /**
     * この位置が別の位置から有意に移動したかチェック
     */
    fun hasMovedSignificantly(other: UserPosition3D, threshold: Float = 0.5f): Boolean {
        if (!isValid() || !other.isValid()) {
            return false
        }
        return distanceTo(other) > threshold
    }
    
    /**
     * 2D移動をチェック（Z軸を無視）
     */
    fun hasMovedSignificantly2D(other: UserPosition3D, threshold: Float = 0.5f): Boolean {
        if (!isValid() || !other.isValid()) {
            return false
        }
        return distance2DTo(other) > threshold
    }
    
    /**
     * 別の位置からの移動方向を計算
     */
    fun movementDirectionFrom(other: UserPosition3D): Float? {
        if (!isValid() || !other.isValid()) {
            return null
        }
        val dx = this.x - other.x
        val dy = this.y - other.y
        return Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }
    
    /**
     * 別の位置との重み付き平均位置を計算
     */
    fun weightedAverageTo(other: UserPosition3D, weight: Float): UserPosition3D {
        val w = weight.coerceIn(0f, 1f)
        val newX = this.x * (1 - w) + other.x * w
        val newY = this.y * (1 - w) + other.y * w
        val newZ = this.z * (1 - w) + other.z * w
        val newAccuracy = this.accuracy * (1 - w) + other.accuracy * w
        val newConfidence = (this.confidence * (1 - w) + other.confidence * w).coerceIn(0f, 1f)
        
        return UserPosition3D(
            x = newX,
            y = newY,
            z = newZ,
            accuracy = newAccuracy,
            timestamp = System.currentTimeMillis(),
            source = PositionSource.FUSION,
            confidence = newConfidence,
            floor = this.floor
        )
    }
    
    /**
     * 位置が有効かチェック
     */
    fun isValid(): Boolean {
        return !x.isNaN() && !y.isNaN() && !z.isNaN() && 
               !x.isInfinite() && !y.isInfinite() && !z.isInfinite() &&
               accuracy < Float.MAX_VALUE
    }
    
    /**
     * フロア間移動を検出
     */
    fun isFloorChange(other: UserPosition3D): Boolean {
        return this.floor != other.floor
    }
    
    /**
     * 上昇/下降を検出
     */
    fun isElevationChange(other: UserPosition3D, threshold: Float = 0.5f): Boolean {
        return Math.abs(this.z - other.z) > threshold
    }
    
    companion object {
        /**
         * 無効な位置を作成
         */
        fun invalid(): UserPosition3D {
            return UserPosition3D(
                x = Float.NaN,
                y = Float.NaN,
                z = Float.NaN,
                accuracy = Float.MAX_VALUE,
                confidence = 0f,
                source = PositionSource.UNKNOWN
            )
        }
        
        /**
         * 原点位置を作成
         */
        fun origin(): UserPosition3D {
            return UserPosition3D(
                x = 0f,
                y = 0f,
                z = 0f,
                accuracy = 0f,
                confidence = 1f,
                source = PositionSource.GROUND_TRUTH
            )
        }
        
        /**
         * 2D位置から3D位置を作成
         */
        fun from2D(position2D: UserPosition, z: Float = 0f, floor: Int = 1): UserPosition3D {
            return UserPosition3D(
                x = position2D.x,
                y = position2D.y,
                z = z,
                accuracy = position2D.accuracy,
                timestamp = position2D.timestamp,
                source = position2D.source,
                confidence = position2D.confidence,
                floor = floor,
                sigmaX = position2D.sigmaX,
                sigmaY = position2D.sigmaY,
                sigmaTheta = position2D.sigmaTheta
            )
        }
    }
}
