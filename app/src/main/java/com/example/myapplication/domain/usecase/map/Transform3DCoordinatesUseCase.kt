package com.example.myapplication.domain.usecase.map

import com.example.myapplication.data.repository.IMapRepository
import com.example.myapplication.domain.model.Coordinate3D
import com.example.myapplication.domain.model.Coordinate3DTransformer
import com.example.myapplication.domain.model.IndoorMap3D
import com.example.myapplication.domain.usecase.UseCase
import timber.log.Timber

/**
 * 3D座標変換ユースケース
 */
class Transform3DCoordinatesUseCase(
    private val mapRepository: IMapRepository
) : UseCase<Transform3DCoordinatesUseCase.Params, Transform3DCoordinatesUseCase.Result> {
    
    // シングルトン変換器インスタンス
    private val transformer = Coordinate3DTransformer()
    
    override suspend fun invoke(params: Params): Result {
        // アクティブマップまたは指定されたマップを取得
        val map = params.map ?: mapRepository.getActiveMap3D(params.floor)
        
        if (map == null) {
            Timber.w("3D座標変換用のマップが利用できません")
            return Result(success = false)
        }
        
        // 変換器に現在のパラメータを更新
        transformer.setMap(map)
        
        // ビューポートが提供されている場合は更新
        if (params.viewportWidth != null && params.viewportHeight != null) {
            transformer.setViewport(params.viewportWidth, params.viewportHeight)
        }
        
        // ズームが提供されている場合は更新
        if (params.zoomLevel != null) {
            transformer.setZoom(params.zoomLevel)
        }
        
        // パンオフセットが提供されている場合は更新
        if (params.panOffsetX != null && params.panOffsetY != null) {
            transformer.setPanOffset(params.panOffsetX, params.panOffsetY, params.panOffsetZ ?: 0f)
        }
        
        // 要求された変換を実行
        return when (params.operation) {
            TransformOperation.METERS_TO_SCREEN -> {
                val (screenX, screenY, screenZ) = transformer.metersToScreen3D(
                    params.x, params.y, params.z
                )
                Result(
                    success = true,
                    screenX = screenX,
                    screenY = screenY,
                    screenZ = screenZ
                )
            }
            TransformOperation.SCREEN_TO_METERS -> {
                val (metersX, metersY, metersZ) = transformer.screenToMeters3D(
                    params.screenX, params.screenY, params.screenZ
                )
                Result(
                    success = true,
                    metersX = metersX,
                    metersY = metersY,
                    metersZ = metersZ
                )
            }
            TransformOperation.TRANSFORM_BETWEEN_FLOORS -> {
                val coordinate3D = params.coordinate3D ?: return Result(success = false)
                val transformedPosition = transformer.transformBetweenFloors(
                    params.fromFloor, params.toFloor, coordinate3D
                )
                Result(
                    success = true,
                    transformedCoordinate = transformedPosition
                )
            }
            TransformOperation.CHECK_VISIBILITY -> {
                val isVisible = transformer.isVisibleOnScreen3D(params.x, params.y, params.z)
                Result(
                    success = true,
                    isVisible = isVisible
                )
            }
            TransformOperation.GET_VISIBLE_AREA -> {
                val visibleArea = transformer.getVisible3DArea()
                Result(
                    success = true,
                    visibleArea = visibleArea
                )
            }
            TransformOperation.CENTER_ON -> {
                transformer.centerOn3D(params.x, params.y, params.z)
                Result(success = true)
            }
        }
    }
    
    /**
     * パラメータクラス
     */
    data class Params(
        val operation: TransformOperation,
        val map: IndoorMap3D? = null,
        val floor: Int = 1,
        val x: Float = 0f,
        val y: Float = 0f,
        val z: Float = 0f,
        val screenX: Float = 0f,
        val screenY: Float = 0f,
        val screenZ: Float = 0f,
        val viewportWidth: Float? = null,
        val viewportHeight: Float? = null,
        val zoomLevel: Float? = null,
        val panOffsetX: Float? = null,
        val panOffsetY: Float? = null,
        val panOffsetZ: Float? = null,
        val fromFloor: Int = 1,
        val toFloor: Int = 1,
        val coordinate3D: Coordinate3D? = null
    )
    
    /**
     * 結果クラス
     */
    data class Result(
        val success: Boolean,
        val screenX: Float = 0f,
        val screenY: Float = 0f,
        val screenZ: Float = 0f,
        val metersX: Float = 0f,
        val metersY: Float = 0f,
        val metersZ: Float = 0f,
        val transformedCoordinate: Coordinate3D? = null,
        val isVisible: Boolean = false,
        val visibleArea: Triple<Pair<Float, Float>, Pair<Float, Float>, Pair<Float, Float>>? = null
    )
    
    /**
     * 変換操作の種類
     */
    enum class TransformOperation {
        METERS_TO_SCREEN,      // メートル座標を画面座標に変換
        SCREEN_TO_METERS,      // 画面座標をメートル座標に変換
        TRANSFORM_BETWEEN_FLOORS, // フロア間座標変換
        CHECK_VISIBILITY,      // 画面表示可能性チェック
        GET_VISIBLE_AREA,      // 表示可能領域取得
        CENTER_ON             // 指定座標に中央配置
    }
}
