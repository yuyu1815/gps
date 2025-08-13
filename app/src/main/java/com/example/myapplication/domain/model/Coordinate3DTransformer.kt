package com.example.myapplication.domain.model

/**
 * 3D座標変換器
 * - 物理座標（メートル）↔ 画面座標（ピクセル）
 * - フロア間座標変換
 * - 座標系の正規化
 */
class Coordinate3DTransformer(
    private var map: IndoorMap3D? = null,
    private var viewportWidth: Float = 0f,
    private var viewportHeight: Float = 0f,
    private var zoomLevel: Float = 1f,
    private var panOffsetX: Float = 0f,
    private var panOffsetY: Float = 0f,
    private var panOffsetZ: Float = 0f
) {
    /**
     * 3Dマップを設定
     */
    fun setMap(map: IndoorMap3D) {
        this.map = map
    }
    
    /**
     * ビューポートサイズを設定
     */
    fun setViewport(width: Float, height: Float) {
        this.viewportWidth = width
        this.viewportHeight = height
    }
    
    /**
     * ズームレベルを設定
     */
    fun setZoom(zoom: Float) {
        this.zoomLevel = zoom.coerceIn(0.1f, 10f)
    }
    
    /**
     * パンオフセットを設定
     */
    fun setPanOffset(offsetX: Float, offsetY: Float, offsetZ: Float = 0f) {
        this.panOffsetX = offsetX
        this.panOffsetY = offsetY
        this.panOffsetZ = offsetZ
    }
    
    /**
     * 物理座標（メートル）を画面座標（ピクセル）に変換
     */
    fun metersToScreen3D(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        val map = this.map ?: return Triple(0f, 0f, 0f)
        
        // メートルをマップピクセルに変換
        val (mapX, mapY) = map.metersToPixels(x, y)
        
        // Z座標をピクセルに変換（高さスケール）
        val mapZ = (z - map.baseHeight) * map.pixelsPerMeter
        
        // ズームとパンを適用して画面座標を取得
        val screenX = mapX * zoomLevel + panOffsetX + 
                     (viewportWidth - map.widthMeters * map.pixelsPerMeter * zoomLevel) / 2
        val screenY = mapY * zoomLevel + panOffsetY + 
                     (viewportHeight - map.heightMeters * map.pixelsPerMeter * zoomLevel) / 2
        val screenZ = mapZ * zoomLevel + panOffsetZ
        
        return Triple(screenX, screenY, screenZ)
    }
    
    /**
     * 画面座標（ピクセル）を物理座標（メートル）に変換
     */
    fun screenToMeters3D(screenX: Float, screenY: Float, screenZ: Float): Triple<Float, Float, Float> {
        val map = this.map ?: return Triple(0f, 0f, 0f)
        
        // ビューポート中央、パン、ズームを調整
        val mapX = (screenX - panOffsetX - 
                   (viewportWidth - map.widthMeters * map.pixelsPerMeter * zoomLevel) / 2) / zoomLevel
        val mapY = (screenY - panOffsetY - 
                   (viewportHeight - map.heightMeters * map.pixelsPerMeter * zoomLevel) / 2) / zoomLevel
        val mapZ = (screenZ - panOffsetZ) / zoomLevel
        
        // マップピクセルをメートルに変換
        val (metersX, metersY) = map.pixelsToMeters(mapX, mapY)
        val metersZ = mapZ / map.pixelsPerMeter + map.baseHeight
        
        return Triple(metersX, metersY, metersZ)
    }
    
    /**
     * フロア間座標変換
     */
    fun transformBetweenFloors(fromFloor: Int, toFloor: Int, position: Coordinate3D): Coordinate3D {
        val floorHeight = 3.0f // 標準的なフロア高さ（メートル）
        val heightDifference = (toFloor - fromFloor) * floorHeight
        
        return position.copy(
            z = position.z + heightDifference,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * 座標が画面に表示可能かチェック
     */
    fun isVisibleOnScreen3D(x: Float, y: Float, z: Float): Boolean {
        val (screenX, screenY, screenZ) = metersToScreen3D(x, y, z)
        return screenX >= 0 && screenX <= viewportWidth && 
               screenY >= 0 && screenY <= viewportHeight &&
               screenZ >= -1000 && screenZ <= 1000 // Z軸の表示範囲
    }
    
    /**
     * 表示可能な3D領域を取得
     */
    fun getVisible3DArea(): Triple<Pair<Float, Float>, Pair<Float, Float>, Pair<Float, Float>> {
        val topLeft = screenToMeters3D(0f, 0f, 0f)
        val bottomRight = screenToMeters3D(viewportWidth, viewportHeight, 0f)
        val topLeftZ = screenToMeters3D(0f, 0f, -1000f)
        val bottomRightZ = screenToMeters3D(0f, 0f, 1000f)
        
        return Triple(
            Pair(topLeft.first, topLeft.second),
            Pair(bottomRight.first, bottomRight.second),
            Pair(topLeftZ.third, bottomRightZ.third)
        )
    }
    
    /**
     * 特定の3D座標にビューを中央配置
     */
    fun centerOn3D(x: Float, y: Float, z: Float) {
        val map = this.map ?: return
        
        // メートルをマップピクセルに変換
        val (mapX, mapY) = map.metersToPixels(x, y)
        val mapZ = (z - map.baseHeight) * map.pixelsPerMeter
        
        // このポイントに中央配置するためのパンオフセットを計算
        panOffsetX = viewportWidth / 2 - mapX * zoomLevel
        panOffsetY = viewportHeight / 2 - mapY * zoomLevel
        panOffsetZ = -mapZ * zoomLevel
    }
    
    /**
     * 現在のズームレベルでのスケール係数を取得
     */
    fun getScaleFactor(): Float = zoomLevel
}
