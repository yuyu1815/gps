package com.example.myapplication.domain.model

/**
 * 3D座標系の定義
 * - X軸: 東西方向（東が正）
 * - Y軸: 南北方向（北が正）  
 * - Z軸: 高さ方向（上が正、メートル単位）
 */
data class Coordinate3D(
    val x: Float,  // メートル
    val y: Float,  // メートル
    val z: Float,  // メートル（高さ）
    val accuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 2D座標に変換（Z軸を無視）
     */
    fun to2D(): Pair<Float, Float> = Pair(x, y)
    
    /**
     * 別の3D座標との距離を計算
     */
    fun distanceTo(other: Coordinate3D): Float {
        val dx = this.x - other.x
        val dy = this.y - other.y
        val dz = this.z - other.z
        return Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
    }
    
    /**
     * 2D距離を計算（Z軸を無視）
     */
    fun distance2DTo(other: Coordinate3D): Float {
        val dx = this.x - other.x
        val dy = this.y - other.y
        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
    
    /**
     * 座標が有効かどうかをチェック
     */
    fun isValid(): Boolean {
        return !x.isNaN() && !y.isNaN() && !z.isNaN() && 
               !x.isInfinite() && !y.isInfinite() && !z.isInfinite()
    }
    
    companion object {
        /**
         * 無効な座標を作成
         */
        fun invalid(): Coordinate3D {
            return Coordinate3D(
                x = Float.NaN,
                y = Float.NaN,
                z = Float.NaN,
                accuracy = Float.MAX_VALUE
            )
        }
        
        /**
         * 原点座標を作成
         */
        fun origin(): Coordinate3D {
            return Coordinate3D(0f, 0f, 0f, 0f)
        }
    }
}
