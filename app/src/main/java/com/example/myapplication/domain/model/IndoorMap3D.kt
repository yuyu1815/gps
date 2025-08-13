package com.example.myapplication.domain.model

/**
 * 3D屋内マップ
 */
data class IndoorMap3D(
    /**
     * マップの一意識別子
     */
    val id: String,
    
    /**
     * マップ名
     */
    val name: String,
    
    /**
     * マップの幅（メートル）
     */
    val widthMeters: Float,
    
    /**
     * マップの高さ（メートル）
     */
    val heightMeters: Float,
    
    /**
     * マップの深さ（メートル）- 高さ方向のサイズ
     */
    val depthMeters: Float,
    
    /**
     * メートルあたりのピクセル数
     */
    val pixelsPerMeter: Float,
    
    /**
     * マップ画像ファイルのパス
     */
    val imagePath: String,
    
    /**
     * フロア番号
     */
    val floor: Int,
    
    /**
     * 基準高さ（メートル）
     */
    val baseHeight: Float,
    
    /**
     * 3D壁のリスト
     */
    val walls: List<Wall3D> = emptyList(),
    
    /**
     * 3D POIのリスト
     */
    val pointsOfInterest: List<PointOfInterest3D> = emptyList()
) {
    /**
     * 物理座標（メートル）をマップ画像上のピクセル座標に変換
     */
    fun metersToPixels(x: Float, y: Float): Pair<Float, Float> {
        return Pair(x * pixelsPerMeter, y * pixelsPerMeter)
    }
    
    /**
     * マップ画像上のピクセル座標を物理座標（メートル）に変換
     */
    fun pixelsToMeters(x: Float, y: Float): Pair<Float, Float> {
        return Pair(x / pixelsPerMeter, y / pixelsPerMeter)
    }
    
    /**
     * 物理座標（メートル）がマップ境界内にあるかチェック
     */
    fun isWithinBounds(x: Float, y: Float, z: Float): Boolean {
        return x >= 0 && x <= widthMeters && 
               y >= 0 && y <= heightMeters &&
               z >= baseHeight && z <= baseHeight + depthMeters
    }
    
    /**
     * 2D境界チェック（Z軸を無視）
     */
    fun isWithinBounds2D(x: Float, y: Float): Boolean {
        return x >= 0 && x <= widthMeters && y >= 0 && y <= heightMeters
    }
}

/**
 * 3D壁または障害物を表す
 */
data class Wall3D(
    /**
     * 開始点のX座標（メートル）
     */
    val startX: Float,
    
    /**
     * 開始点のY座標（メートル）
     */
    val startY: Float,
    
    /**
     * 開始点のZ座標（メートル）
     */
    val startZ: Float,
    
    /**
     * 終了点のX座標（メートル）
     */
    val endX: Float,
    
    /**
     * 終了点のY座標（メートル）
     */
    val endY: Float,
    
    /**
     * 終了点のZ座標（メートル）
     */
    val endZ: Float,
    
    /**
     * 壁の高さ（メートル）
     */
    val height: Float = 3.0f,
    
    /**
     * 壁の厚さ（メートル）
     */
    val thickness: Float = 0.2f
) {
    /**
     * 壁の長さを計算
     */
    fun length(): Float {
        val dx = endX - startX
        val dy = endY - startY
        val dz = endZ - startZ
        return Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
    }
    
    /**
     * 2D壁に変換（Z軸を無視）
     */
    fun to2D(): Wall {
        return Wall(startX, startY, endX, endY)
    }
}

/**
 * 3D POI（Point of Interest）を表す
 */
data class PointOfInterest3D(
    /**
     * POIの一意識別子
     */
    val id: String,
    
    /**
     * POI名
     */
    val name: String,
    
    /**
     * X座標（メートル）
     */
    val x: Float,
    
    /**
     * Y座標（メートル）
     */
    val y: Float,
    
    /**
     * Z座標（メートル）
     */
    val z: Float,
    
    /**
     * POIのタイプ
     */
    val type: PoiType3D,
    
    /**
     * 追加情報
     */
    val description: String = "",
    
    /**
     * POIの高さ（メートル）
     */
    val height: Float = 0f,
    
    /**
     * POIの幅（メートル）
     */
    val width: Float = 0f,
    
    /**
     * POIの奥行き（メートル）
     */
    val depth: Float = 0f
) {
    /**
     * 2D POIに変換（Z軸を無視）
     */
    fun to2D(): PointOfInterest {
        return PointOfInterest(id, name, x, y, type.to2D(), description)
    }
    
    /**
     * 3D座標に変換
     */
    fun toCoordinate3D(): Coordinate3D {
        return Coordinate3D(x, y, z, 0f)
    }
}

/**
 * 3D POIタイプ
 */
enum class PoiType3D {
    ROOM,
    ENTRANCE,
    EXIT,
    ELEVATOR,
    STAIRS,
    ESCALATOR,
    RESTROOM,
    CAFE,
    SHOP,
    OFFICE,
    MEETING_ROOM,
    CONFERENCE_ROOM,
    LOBBY,
    CORRIDOR,
    OTHER;
    
    /**
     * 2D POIタイプに変換
     */
    fun to2D(): PoiType {
        return when (this) {
            ROOM -> PoiType.ROOM
            ENTRANCE -> PoiType.ENTRANCE
            EXIT -> PoiType.EXIT
            ELEVATOR -> PoiType.ELEVATOR
            STAIRS -> PoiType.STAIRS
            RESTROOM -> PoiType.RESTROOM
            CAFE -> PoiType.CAFE
            SHOP -> PoiType.SHOP
            else -> PoiType.OTHER
        }
    }
}
