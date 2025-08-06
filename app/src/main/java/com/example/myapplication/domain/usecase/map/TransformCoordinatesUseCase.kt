package com.example.myapplication.domain.usecase.map

import com.example.myapplication.data.repository.IMapRepository
import com.example.myapplication.domain.model.CoordinateTransformer
import com.example.myapplication.domain.model.IndoorMap
import com.example.myapplication.domain.usecase.UseCase
import timber.log.Timber

/**
 * Use case for transforming coordinates between different coordinate systems.
 * This use case handles transformations between physical (meters) and screen (pixels) coordinates.
 */
class TransformCoordinatesUseCase(
    private val mapRepository: IMapRepository
) : UseCase<TransformCoordinatesUseCase.Params, TransformCoordinatesUseCase.Result> {
    
    // Singleton transformer instance to maintain state
    private val transformer = CoordinateTransformer()
    
    override suspend fun invoke(params: Params): Result {
        // Get the active map or the specified map
        val map = params.map ?: mapRepository.getActiveMap()
        
        if (map == null) {
            Timber.w("No map available for coordinate transformation")
            return Result(success = false)
        }
        
        // Update transformer with current parameters
        transformer.setMap(map)
        
        // Update viewport if provided
        if (params.viewportWidth != null && params.viewportHeight != null) {
            transformer.setViewport(params.viewportWidth, params.viewportHeight)
        }
        
        // Update zoom if provided
        if (params.zoomLevel != null) {
            transformer.setZoom(params.zoomLevel)
        }
        
        // Update pan offset if provided
        if (params.panOffsetX != null && params.panOffsetY != null) {
            transformer.setPanOffset(params.panOffsetX, params.panOffsetY)
        }
        
        // Perform the requested transformation
        return when (params.transformationType) {
            TransformationType.METERS_TO_SCREEN -> {
                if (params.metersX == null || params.metersY == null) {
                    Timber.w("Missing meter coordinates for transformation")
                    Result(success = false)
                } else {
                    val (screenX, screenY) = transformer.metersToScreen(params.metersX, params.metersY)
                    Result(
                        success = true,
                        screenX = screenX,
                        screenY = screenY
                    )
                }
            }
            
            TransformationType.SCREEN_TO_METERS -> {
                if (params.screenX == null || params.screenY == null) {
                    Timber.w("Missing screen coordinates for transformation")
                    Result(success = false)
                } else {
                    val (metersX, metersY) = transformer.screenToMeters(params.screenX, params.screenY)
                    Result(
                        success = true,
                        metersX = metersX,
                        metersY = metersY
                    )
                }
            }
            
            TransformationType.GET_VISIBLE_AREA -> {
                val (topLeft, bottomRight) = transformer.getVisibleArea()
                Result(
                    success = true,
                    visibleAreaMinX = topLeft.first,
                    visibleAreaMinY = topLeft.second,
                    visibleAreaMaxX = bottomRight.first,
                    visibleAreaMaxY = bottomRight.second
                )
            }
            
            TransformationType.CENTER_ON_POSITION -> {
                if (params.metersX == null || params.metersY == null) {
                    Timber.w("Missing meter coordinates for centering")
                    Result(success = false)
                } else {
                    transformer.centerOn(params.metersX, params.metersY)
                    Result(success = true)
                }
            }
        }
    }
    
    /**
     * Types of coordinate transformations.
     */
    enum class TransformationType {
        METERS_TO_SCREEN,
        SCREEN_TO_METERS,
        GET_VISIBLE_AREA,
        CENTER_ON_POSITION
    }
    
    /**
     * Parameters for the TransformCoordinatesUseCase.
     *
     * @param transformationType Type of transformation to perform
     * @param map Map to use for transformation (uses active map if null)
     * @param viewportWidth Width of the viewport in pixels
     * @param viewportHeight Height of the viewport in pixels
     * @param zoomLevel Zoom level
     * @param panOffsetX Pan offset in X direction
     * @param panOffsetY Pan offset in Y direction
     * @param metersX X coordinate in meters (for METERS_TO_SCREEN or CENTER_ON_POSITION)
     * @param metersY Y coordinate in meters (for METERS_TO_SCREEN or CENTER_ON_POSITION)
     * @param screenX X coordinate in screen pixels (for SCREEN_TO_METERS)
     * @param screenY Y coordinate in screen pixels (for SCREEN_TO_METERS)
     */
    data class Params(
        val transformationType: TransformationType,
        val map: IndoorMap? = null,
        val viewportWidth: Float? = null,
        val viewportHeight: Float? = null,
        val zoomLevel: Float? = null,
        val panOffsetX: Float? = null,
        val panOffsetY: Float? = null,
        val metersX: Float? = null,
        val metersY: Float? = null,
        val screenX: Float? = null,
        val screenY: Float? = null
    )
    
    /**
     * Result of the coordinate transformation.
     *
     * @param success Whether the transformation was successful
     * @param metersX X coordinate in meters (for SCREEN_TO_METERS)
     * @param metersY Y coordinate in meters (for SCREEN_TO_METERS)
     * @param screenX X coordinate in screen pixels (for METERS_TO_SCREEN)
     * @param screenY Y coordinate in screen pixels (for METERS_TO_SCREEN)
     * @param visibleAreaMinX Minimum X coordinate of visible area in meters (for GET_VISIBLE_AREA)
     * @param visibleAreaMinY Minimum Y coordinate of visible area in meters (for GET_VISIBLE_AREA)
     * @param visibleAreaMaxX Maximum X coordinate of visible area in meters (for GET_VISIBLE_AREA)
     * @param visibleAreaMaxY Maximum Y coordinate of visible area in meters (for GET_VISIBLE_AREA)
     */
    data class Result(
        val success: Boolean,
        val metersX: Float? = null,
        val metersY: Float? = null,
        val screenX: Float? = null,
        val screenY: Float? = null,
        val visibleAreaMinX: Float? = null,
        val visibleAreaMinY: Float? = null,
        val visibleAreaMaxX: Float? = null,
        val visibleAreaMaxY: Float? = null
    )
}